from flask import Flask, render_template, jsonify
from monitor import VersionMonitorApp, VersionInfo
from email_notifier import send_version_notification
from datetime import datetime
import threading

app = Flask(__name__)

version_history = []
current_versions = {}
monitor = None


def on_new_version(name: str, info: VersionInfo):
    old_version = current_versions.get(name, {}).get("version")
    entry = {
        "name": name,
        "version": info.version,
        "url": info.url,
        "time": info.detected_at,
    }
    version_history.insert(0, entry)
    current_versions[name] = entry
    print(f"通知: {name} 更新到 {info.version}")
    send_version_notification(name, old_version, info.version, info.url)


def init_monitor():
    global monitor
    monitor = VersionMonitorApp(interval=60, on_new_version=on_new_version)
    monitor.start()


@app.route("/")
def index():
    return render_template("index.html")


@app.route("/api/versions")
def get_versions():
    return jsonify(
        {
            "current": current_versions,
            "history": version_history[:50],
        }
    )


@app.route("/api/check-now")
def check_now():
    if monitor:
        for m in monitor.monitors:
            result = m.check()
            if result:
                if m.last_version and result.version != m.last_version.version:
                    on_new_version(m.name, result)
                elif not m.last_version:
                    current_versions[m.name] = {
                        "name": m.name,
                        "version": result.version,
                        "url": result.url,
                        "time": result.detected_at,
                    }
                m.last_version = result
        return jsonify({"status": "checked"})
    return jsonify({"status": "monitor not running"})


if __name__ == "__main__":
    init_monitor()
    app.run(host="127.0.0.1", port=5000, debug=False)