from flask import Flask, render_template, jsonify
from monitor import VersionMonitorApp, VersionInfo
from email_notifier import send_version_notification
import threading
import json
import os

app = Flask(__name__)

STATE_FILE = os.path.join(os.path.dirname(__file__), "state.json")

version_history = []
current_versions = {}
monitor = None
state_lock = threading.RLock()


def load_state():
    global current_versions, version_history
    if os.path.exists(STATE_FILE):
        try:
            with open(STATE_FILE, "r", encoding="utf-8") as f:
                data = json.load(f)
                with state_lock:
                    current_versions = data.get("current_versions", {})
                    version_history = data.get("version_history", [])
        except Exception:
            pass


def save_state():
    try:
        with state_lock:
            snapshot = {
                "current_versions": current_versions,
                "version_history": version_history[:50],
            }
        with open(STATE_FILE, "w", encoding="utf-8") as f:
            json.dump(snapshot, f, ensure_ascii=False, indent=2)
    except Exception:
        pass


def on_new_version(name: str, info: VersionInfo):
    with state_lock:
        old_version = current_versions.get(name, {}).get("version")
        if old_version == info.version:
            return  # 已记录相同版本，避免重复处理/重复通知
        entry = {
            "name": name,
            "version": info.version,
            "url": info.url,
            "time": info.detected_at,
        }
        version_history.insert(0, entry)
        current_versions[name] = entry

    if old_version:
        print(f"通知: {name} 更新到 {info.version}")
        send_version_notification(name, old_version, info.version, info.url)
    else:
        print(f"[{name}] 首次记录版本: {info.version}")
    save_state()


def init_monitor():
    global monitor
    load_state()
    monitor = VersionMonitorApp(interval=60, on_new_version=on_new_version)
    for m in monitor.monitors:
        if m.name in current_versions:
            saved = current_versions[m.name]
            m.last_version = VersionInfo(
                version=saved["version"],
                url=saved["url"],
                detected_at=saved.get("time", ""),
            )
            print(f"[{m.name}] 恢复上次版本: {saved['version']}")
    monitor.start()


@app.route("/")
def index():
    return render_template("index.html")


@app.route("/api/versions")
def get_versions():
    with state_lock:
        return jsonify(
            {
                "current": current_versions,
                "history": version_history[:50],
            }
        )


@app.route("/api/check-now")
def check_now():
    if not monitor:
        return jsonify({"status": "monitor not running"})

    for m in monitor.monitors:
        result = m.check()
        if result:
            # 统一走 on_new_version：首次仅记录版本，有旧版本才通知
            if not m.last_version or result.version != m.last_version.version:
                on_new_version(m.name, result)
            m.last_version = result
    return jsonify({"status": "checked"})


if __name__ == "__main__":
    init_monitor()
    app.run(host="127.0.0.1", port=5000, debug=False)