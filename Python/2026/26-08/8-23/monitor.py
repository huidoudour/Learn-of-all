import re
import time
import random
import threading
import warnings
import xml.etree.ElementTree as ET
from datetime import datetime
from typing import Callable, Optional
from dataclasses import dataclass, field

import requests
from bs4 import BeautifulSoup, XMLParsedAsHTMLWarning
from log import log

# 部分监控页可能是 XML（如 maven-metadata），避免误导性的“用 HTML 解析 XML”警告刷屏
warnings.filterwarnings("ignore", category=XMLParsedAsHTMLWarning)


@dataclass
class VersionInfo:
    version: str
    url: str
    detected_at: str = field(default_factory=lambda: datetime.now().strftime("%Y-%m-%d %H:%M:%S"))


class PageMonitor:
    def __init__(self, url: str, name: str, parse_func: Callable, fetch_url: str = None):
        self.url = url
        self.name = name
        self.parse_func = parse_func
        self.fetch_url = fetch_url or url
        self.last_version: Optional[VersionInfo] = None

    def _request_with_retry(self, url: str, headers: dict, retries: int = 3) -> requests.Response:
        """带退避重试的 GET 请求，缓解 Cloudflare 等反爬导致的 403/429 间歇性失败。"""
        last_err: Optional[Exception] = None
        retry_statuses = (403, 429, 502, 503, 504)
        for attempt in range(retries):
            try:
                response = requests.get(url, headers=headers, timeout=30, allow_redirects=True)
                if response.status_code in retry_statuses:
                    # 403/429/5xx 通常是一时性的服务器/反爬拦截，记录后进入重试
                    last_err = requests.HTTPError(f"{response.status_code} {response.reason}", response=response)
                else:
                    response.raise_for_status()
                    return response
            except requests.HTTPError:
                # 非重试类 HTTP 错误（如 404）不重试，直接抛给上层
                if last_err is None:
                    raise
            except Exception as e:
                last_err = e

            wait = 2 * (attempt + 1) + random.uniform(0, 1)
            log(f"[{self.name}] 第 {attempt + 1}/{retries} 次请求失败({last_err})，{wait:.1f}s 后重试")
            if attempt < retries - 1:
                time.sleep(wait)
        raise last_err if last_err else requests.RequestException("请求失败")

    def check(self) -> Optional[VersionInfo]:
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
            "Accept-Encoding": "gzip, deflate, br",
            "Connection": "keep-alive",
            "Cache-Control": "no-cache",
            "Pragma": "no-cache",
            "Upgrade-Insecure-Requests": "1",
            "Sec-Fetch-Site": "none",
            "Sec-Fetch-Mode": "navigate",
            "Sec-Fetch-User": "?1",
            "Sec-Fetch-Dest": "document",
            "Sec-Ch-Ua": '\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"',
            "Sec-Ch-Ua-Mobile": "?0",
            "Sec-Ch-Ua-Platform": '\"Windows\"',
        }
        try:
            response = self._request_with_retry(self.fetch_url, headers)
            result = self.parse_func(response.text, self.url)
            if result is None:
                log(f"[{self.name}] 解析失败: 未找到版本信息")
            return result
        except Exception as e:
            log(f"[{self.name}] 检查失败: {e}")
            return None


def parse_gradle_snapshots(html: str, url: str) -> Optional[VersionInfo]:
    soup = BeautifulSoup(html, "html.parser")
    links = soup.find_all("a")
    versions = []
    for link in links:
        href = link.get("href", "")
        match = re.search(r"(gradle-\d+\.\d+(?:\.\d+)?(?:-rc-\d+|-milestone-\d+)?-\d{14}\+\d{4}-(?:bin|all)\.zip)", href)
        if match:
            versions.append((match.group(1), href))

    if not versions:
        return None

    def version_key(v):
        ver = v[0]
        match = re.search(r"gradle-(\d+\.\d+(?:\.\d+)?(?:-rc-\d+|-milestone-\d+)?)", ver)
        if match:
            base_ver = match.group(1)
            parts = re.split(r"[-.]", base_ver)
            result = []
            for p in parts:
                if p.isdigit():
                    result.append((0, int(p)))
                else:
                    result.append((1, p))
            return result
        return [(0, 0)]

    versions.sort(key=version_key, reverse=True)
    latest = versions[0]
    return VersionInfo(version=latest[0], url=url + latest[1])


def parse_mvnrepository(html: str, url: str) -> Optional[VersionInfo]:
    try:
        metadata_url = "https://dl.google.com/android/maven2/androidx/compose/ui/ui-tooling-preview/maven-metadata.xml"
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        }
        response = requests.get(metadata_url, headers=headers, timeout=30)
        response.raise_for_status()
        return parse_maven_metadata(response.text, url)
    except Exception as e:
        log(f"[Compose UI Tooling Preview] 请求失败: {e}")

    return None


def parse_gradle_releases(html: str, url: str) -> Optional[VersionInfo]:
    soup = BeautifulSoup(html, "html.parser")

    version_links = soup.find_all("a", href=re.compile(r"#\d+\.\d+"))
    versions = []
    for link in version_links:
        href = link.get("href", "")
        match = re.search(r"#(\d+\.\d+(?:\.\d+)?(?:-rc-\d+|-milestone-\d+)?)", href)
        if match:
            versions.append((match.group(1), href))

    if not versions:
        version_patterns = [
            r"(\d+\.\d+(?:\.\d+)?(?:-rc-\d+|-milestone-\d+))",
        ]
        for pattern in version_patterns:
            matches = re.findall(pattern, html)
            for m in matches:
                versions.append((m, f"#{m}"))

    if not versions:
        return None

    def version_key(v):
        parts = re.split(r"[-.]", v[0])
        result = []
        for p in parts:
            if p.isdigit():
                result.append((0, int(p)))
            else:
                result.append((1, p))
        return result

    versions.sort(key=version_key, reverse=True)
    latest = versions[0]
    return VersionInfo(version=latest[0], url=url + latest[1])


def parse_github_releases(html: str, url: str) -> Optional[VersionInfo]:
    """解析 GitHub releases 页面，取最新发布版本（含 pre-release）。

    GitHub 的 ``/releases`` 页面把“Latest”（最新正式版）盒子放在最前，
    而可能更新的 pre-release 排在它后面，直接取第一个 tag 会漏掉它们。
    这里遍历所有 release 条目，按各自发布时间（relative-time）倒序排序，
    取最新的一条（因此能命中 pre-release）。只解析 HTML，不依赖会限流的 API。
    """

    def release_key(item):
        # 让有发布时间(True)的排在没有时间(False)的前面，再按时间倒序
        dt = item[0]
        return (dt != "", dt)

    soup = BeautifulSoup(html, "html.parser")
    entries = []  # (datetime, tag)
    seen = set()
    for box in soup.select("div.Box"):
        link = box.select_one("a[href*='/releases/tag/']")
        if not link:
            continue
        rt = box.select_one("relative-time")
        if not rt:
            continue
        dt = rt.get("datetime", "")
        href = link.get("href", "")
        m = re.search(r"/releases/tag/([^/\s#?]+)", href)
        if not m:
            continue
        tag = m.group(1)
        if tag in seen:
            continue
        seen.add(tag)
        entries.append((dt, tag))

    if entries:
        entries.sort(key=release_key, reverse=True)
        tag = entries[0][1]
        return VersionInfo(version=tag, url=url.rstrip("/") + "/tag/" + tag)

    # 兜底：找不到发布时间时，按页面顺序取第一个 tag
    for link in soup.find_all("a", href=re.compile(r"/releases/tag/[^/\s#?]+")):
        href = link.get("href", "")
        m = re.search(r"/releases/tag/([^/\s#?]+)", href)
        if m:
            tag = m.group(1)
            return VersionInfo(version=tag, url=url.rstrip("/") + "/tag/" + tag)
    return None


def semantic_version_key(ver: str):
    """把版本号转成可比较的元组，如 '1.2.3' 和 '1.2.3-rc1'。"""
    ver = ver.strip().lstrip("vV").strip()
    m = re.match(r"^([0-9]+(?:\.[0-9]+)*)(.*)$", ver)
    if not m:
        return ((0, 0),)
    core, rest = m.group(1), m.group(2)
    core_parts = [int(x) for x in core.split(".")]
    while len(core_parts) < 3:
        core_parts.append(0)
    rest = rest.lstrip("-.")
    is_pre = 0
    pre_key = []
    if rest:
        rest = rest.split("+")[0]
        if rest:
            is_pre = 1
            for t in re.split(r"[-._]", rest):
                if t.isdigit():
                    pre_key.append((0, int(t)))
                else:
                    pre_key.append((1, t))
    return (tuple(core_parts), is_pre) + tuple(pre_key)


def parse_generic_version(html: str, url: str) -> Optional[VersionInfo]:
    """在页面中查找形如 1.2.3 / v1.2.3 / 1.2.3-rc1 的版本号，取语义化最高的一个。

    属于启发式解析，可能误抓页面中的其他数字，适合作为通用兜底。
    """
    pattern = re.compile(r"(?i)(?<![A-Za-z0-9])v?(\d+\.\d+(?:\.\d+)?(?:[-._][0-9A-Za-z]+)*)")
    candidates = set()
    for m in pattern.finditer(html):
        candidates.add(m.group(1))
    if not candidates:
        return None
    latest = max(candidates, key=semantic_version_key)
    return VersionInfo(version=latest, url=url)


def parse_maven_metadata(html: str, url: str) -> Optional[VersionInfo]:
    """解析 Maven 的 maven-metadata.xml，取 latest / release / 最后一个 version。

    使用标准库 xml.etree.ElementTree 解析，避免依赖未安装的 lxml 解析器。
    """
    try:
        root = ET.fromstring(html)
    except ET.ParseError as e:
        log(f"[{url}] XML 解析失败: {e}")
        return None

    def _text(tag: str) -> Optional[str]:
        # 使用 .// 递归查找（latest/release 嵌套在 <versioning> 内），等价于 BeautifulSoup 的 find()
        el = root.find(f".//{tag}")
        if el is not None and el.text:
            return el.text.strip()
        return None

    latest = _text("latest")
    if latest:
        return VersionInfo(version=latest, url=url)
    release = _text("release")
    if release:
        return VersionInfo(version=release, url=url)
    versions = root.findall(".//version")
    if versions:
        return VersionInfo(version=versions[-1].text.strip(), url=url)
    return None


GOOGLE_MAVEN_ROOT = "https://dl.google.com/android/maven2"


def google_maven_metadata_url(url: str) -> Optional[str]:
    """从 mvnrepository 页面 URL 或已有的 maven-metadata URL 推导 Google Maven 元数据地址。

    mvnrepository.com 受 Cloudflare 反爬保护，直接抓取易返回 403；
    改用 Google 官方源可稳定获取版本信息。
    例如:
      https://mvnrepository.com/artifact/androidx.sqlite/sqlite/versions
      -> https://dl.google.com/android/maven2/androidx/sqlite/sqlite/maven-metadata.xml
    """
    m = re.search(r"mvnrepository\.com/artifact/([^/]+)/([^/#?]+)", url)
    if m:
        group, artifact = m.group(1), m.group(2)
        return f"{GOOGLE_MAVEN_ROOT}/{group.replace('.', '/')}/{artifact}/maven-metadata.xml"
    # 已经是元数据 XML 或 Google 源地址则原样使用
    if "maven-metadata.xml" in url or url.startswith(GOOGLE_MAVEN_ROOT):
        return url
    return None


def parse_google_maven(html: str, url: str) -> Optional[VersionInfo]:
    """解析 Google Maven(AndroidX) 的 maven-metadata.xml，取最新版本。

    由 build_monitor_from_row 自动将 fetch_url 指向官方元数据地址，
    这里复用 maven-metadata 的解析逻辑。"""
    return parse_maven_metadata(html, url)


# 预置的解析方式，供前端管理界面选择
PARSE_FUNCTIONS = {
    "generic": parse_generic_version,
    "github": parse_github_releases,
    "maven": parse_maven_metadata,
    "google-maven": parse_google_maven,
    "gradle-snapshots": parse_gradle_snapshots,
    "gradle-releases": parse_gradle_releases,
}


class VersionMonitorApp:
    def __init__(
        self,
        interval: int = 300,
        on_new_version: Optional[Callable] = None,
        on_batch_end: Optional[Callable] = None,
        monitors: Optional[list] = None,
    ):
        self.interval = interval
        self.on_new_version = on_new_version
        self.on_batch_end = on_batch_end
        self._monitors_lock = threading.Lock()
        # 监控项由前端 + SQLite 统一管理，不再在此硬编码
        self.monitors = list(monitors) if monitors else []
        self._running = False
        self._stop_event = threading.Event()
        self._thread: Optional[threading.Thread] = None

    def add_monitor(self, monitor: PageMonitor):
        with self._monitors_lock:
            if not any(m.name == monitor.name for m in self.monitors):
                self.monitors.append(monitor)

    def remove_monitor(self, name: str) -> bool:
        with self._monitors_lock:
            original = len(self.monitors)
            self.monitors[:] = [m for m in self.monitors if m.name != name]
            return len(self.monitors) != original

    def start(self):
        if self._running:
            return
        self._running = True
        self._stop_event.clear()
        self._thread = threading.Thread(target=self._monitor_loop, daemon=True)
        self._thread.start()
        log("监控已启动")

    def stop(self):
        self._running = False
        self._stop_event.set()
        if self._thread:
            self._thread.join(timeout=5)
        log("监控已停止")

    def _monitor_loop(self):
        while not self._stop_event.is_set():
            with self._monitors_lock:
                snapshot = list(self.monitors)
            for monitor in snapshot:
                if self._stop_event.is_set():
                    break
                current = monitor.check()
                if current:
                    if monitor.last_version and current.version != monitor.last_version.version:
                        log(f"[{monitor.name}] 发现新版本: {current.version}")
                        if self.on_new_version:
                            self.on_new_version(monitor.name, current)
                    elif not monitor.last_version:
                        log(f"[{monitor.name}] 初始版本: {current.version}")
                        if self.on_new_version:
                            self.on_new_version(monitor.name, current)
                    monitor.last_version = current

            # 本轮所有监控项检查完毕，触发批量通知：把同一轮检测到的多个更新合并成一封邮件
            if self.on_batch_end:
                self.on_batch_end()

            self._stop_event.wait(self.interval)