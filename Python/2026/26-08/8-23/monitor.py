import re
import time
import threading
from datetime import datetime
from typing import Callable, Optional
from dataclasses import dataclass, field

import requests
from bs4 import BeautifulSoup


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


class VersionMonitorApp:
    def __init__(self, interval: int = 300, on_new_version: Optional[Callable] = None):
        self.interval = interval
        self.on_new_version = on_new_version
        self.monitors = [
            PageMonitor(
                "https://services.gradle.org/distributions-snapshots/",
                "Gradle Snapshots",
                parse_gradle_snapshots,
            ),
            PageMonitor(
                "https://mvnrepository.com/artifact/androidx.compose.ui/ui-tooling-preview/versions",
                "Compose UI Tooling Preview",
                parse_mvnrepository,
                fetch_url="https://dl.google.com/android/maven2/androidx/compose/ui/ui-tooling-preview/maven-metadata.xml",
            ),
            PageMonitor(
                "https://gradle.org/releases/",
                "Gradle Releases",
                parse_gradle_releases,
            ),
        ]
        self._running = False
        self._thread: Optional[threading.Thread] = None

    def start(self):
        if self._running:
            return
        self._running = True
        self._thread = threading.Thread(target=self._monitor_loop, daemon=True)
        self._thread.start()
        print("监控已启动")

    def stop(self):
        self._running = False
        if self._thread:
            self._thread.join(timeout=5)
        print("监控已停止")

    def _monitor_loop(self):
        while self._running:
            for monitor in self.monitors:
                if not self._running:
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

            time.sleep(self.interval)