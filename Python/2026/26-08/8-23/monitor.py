import re
import time
import threading
import warnings
from datetime import datetime
from typing import Callable, Optional
from dataclasses import dataclass, field

import requests
from bs4 import BeautifulSoup, XMLParsedAsHTMLWarning

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

    def check(self) -> Optional[VersionInfo]:
        try:
            headers = {
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
                "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
                "Accept-Encoding": "gzip, deflate, br",
                "Connection": "keep-alive",
                "Upgrade-Insecure-Requests": "1",
            }
            response = requests.get(self.fetch_url, headers=headers, timeout=30, allow_redirects=True)
            response.raise_for_status()
            result = self.parse_func(response.text, self.url)
            if result is None:
                print(f"[{self.name}] 解析失败: 未找到版本信息")
            return result
        except Exception as e:
            print(f"[{self.name}] 检查失败: {e}")
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
        soup = BeautifulSoup(response.text, "xml")
        latest = soup.find("latest")
        if latest and latest.text:
            return VersionInfo(version=latest.text, url=url)
        release = soup.find("release")
        if release and release.text:
            return VersionInfo(version=release.text, url=url)
        versions = soup.find_all("version")
        if versions:
            return VersionInfo(version=versions[-1].text, url=url)
    except Exception as e:
        print(f"[Compose UI Tooling Preview] 请求失败: {e}")

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
    """解析 GitHub releases，取最新发布版本（含 pre-release）。

    GitHub 的 ``/releases`` 页面会把“Latest”（最新正式版）置顶，而更新的
    pre-release 排在它之后，直接按页面顺序取第一个会漏掉 pre-release。
    因此优先使用官方 REST API：返回所有 release，按发布时间倒序取最新。
    仅在 API 不可用时回退到原 HTML 解析。
    """
    m = re.search(r"github\.com/([^/]+)/([^/]+)", url)
    if m:
        owner, repo = m.group(1), m.group(2)
        api_url = f"https://api.github.com/repos/{owner}/{repo}/releases"
        try:
            resp = requests.get(
                api_url,
                headers={"User-Agent": "Mozilla/5.0", "Accept": "application/vnd.github+json"},
                timeout=30,
            )
            if resp.status_code == 200:
                releases = resp.json()
                if isinstance(releases, list) and releases:
                    def released_at(r):
                        return r.get("published_at") or r.get("created_at") or ""
                    releases.sort(key=released_at, reverse=True)
                    latest = releases[0]
                    return VersionInfo(
                        version=latest.get("tag_name") or latest.get("name") or "",
                        url=latest.get("html_url") or url,
                    )
        except Exception as e:
            print(f"[GitHub API] 请求失败: {e}")

    # 兜底：沿用 HTML 解析（按页面顺序取第一个 tag）
    soup = BeautifulSoup(html, "html.parser")
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
    """解析 Maven 的 maven-metadata.xml，取 latest / release / 最后一个 version。"""
    soup = BeautifulSoup(html, "xml")
    latest = soup.find("latest")
    if latest and latest.text:
        return VersionInfo(version=latest.text, url=url)
    release = soup.find("release")
    if release and release.text:
        return VersionInfo(version=release.text, url=url)
    versions = soup.find_all("version")
    if versions:
        return VersionInfo(version=versions[-1].text, url=url)
    return None


# 预置的解析方式，供前端管理界面选择
PARSE_FUNCTIONS = {
    "generic": parse_generic_version,
    "github": parse_github_releases,
    "maven": parse_maven_metadata,
    "gradle-snapshots": parse_gradle_snapshots,
    "gradle-releases": parse_gradle_releases,
}


class VersionMonitorApp:
    def __init__(
        self,
        interval: int = 300,
        on_new_version: Optional[Callable] = None,
        monitors: Optional[list] = None,
    ):
        self.interval = interval
        self.on_new_version = on_new_version
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
        print("监控已启动")

    def stop(self):
        self._running = False
        self._stop_event.set()
        if self._thread:
            self._thread.join(timeout=5)
        print("监控已停止")

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
                        print(f"[{monitor.name}] 发现新版本: {current.version}")
                        if self.on_new_version:
                            self.on_new_version(monitor.name, current)
                    elif not monitor.last_version:
                        print(f"[{monitor.name}] 初始版本: {current.version}")
                        if self.on_new_version:
                            self.on_new_version(monitor.name, current)
                    monitor.last_version = current

            self._stop_event.wait(self.interval)