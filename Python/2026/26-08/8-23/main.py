import os
from flask import Flask, render_template, jsonify, request
from dotenv import load_dotenv
from monitor import VersionMonitorApp, VersionInfo, PageMonitor, PARSE_FUNCTIONS, google_maven_metadata_url
from email_notifier import send_batch_notification
from log import log
import atexit
import signal
import sys
import threading
import db

load_dotenv()

app = Flask(__name__)
app.config["TEMPLATES_AUTO_RELOAD"] = True

version_history = []
current_versions = {}
monitor = None
state_lock = threading.RLock()
notify_lock = threading.RLock()
pending_notifications = []


def load_state():
    global current_versions, version_history
    with state_lock:
        try:
            current_versions = db.load_current_versions()
            version_history = db.load_version_history(50)
        except Exception as e:
            log(f"[加载] 读取数据库失败: {e}")
            current_versions = {}
            version_history = []


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
        version_history[:] = version_history[:50]
        current_versions[name] = entry
        try:
            db.save_current_version(name, info.version, info.url, info.detected_at)
            db.add_version_history(name, info.version, info.url, info.detected_at)
        except Exception as e:
            log(f"[{name}] 写入数据库失败: {e}")

    # 有旧版本说明是真更新，先加入待发送队列，等本轮所有监控项检查完再合并成一封邮件
    if old_version:
        with notify_lock:
            pending_notifications.append(
                {
                    "name": name,
                    "old_version": old_version,
                    "new_version": info.version,
                    "url": info.url,
                }
            )
        log(f"通知: {name} 更新到 {info.version} (已加入待发送队列)")
    else:
        log(f"[{name}] 首次记录版本: {info.version}")


def flush_pending_notifications():
    """把缓存中的更新通知合并成一封邮件发出，并清空缓存。"""
    with notify_lock:
        if not pending_notifications:
            return
        batch = list(pending_notifications)
        pending_notifications.clear()
    if send_batch_notification(batch):
        log(f"[邮件] 已合并发送 {len(batch)} 条更新通知")


def is_valid_url(url) -> bool:
    return isinstance(url, str) and url.strip().lower().startswith(("http://", "https://"))


def build_monitor_from_row(row):
    url = row.get("url") or ""
    if not is_valid_url(url):
        log(f"[{row.get('name')}] 跳过：链接格式无效 {url!r}")
        return None
    parse_func = PARSE_FUNCTIONS.get(row.get("parse_type"))
    if not parse_func:
        log(f"[{row.get('name')}] 跳过：未知解析方式 {row.get('parse_type')}")
        return None
    fetch_url = row.get("fetch_url") or None
    # google-maven 未指定备用地址时，自动改用 Google 官方元数据源，规避 Cloudflare 拦截
    if row.get("parse_type") == "google-maven" and not fetch_url:
        fetch_url = google_maven_metadata_url(url)
    return PageMonitor(
        url,
        row["name"],
        parse_func,
        fetch_url=fetch_url,
    )


def init_monitor():
    global monitor
    db.init_db()
    load_state()
    all_monitors = []
    for row in db.list_monitors():
        pm = build_monitor_from_row(row)
        if pm:
            all_monitors.append(pm)
    monitor_interval = int(os.getenv("MONITOR_INTERVAL", "60"))
    monitor = VersionMonitorApp(
        interval=monitor_interval,
        on_new_version=on_new_version,
        on_batch_end=flush_pending_notifications,
        monitors=all_monitors,
    )

    # 清理数据库中已不在监控列表里的孤儿数据（历史与当前版本保持同步）
    active_names = [m.name for m in monitor.monitors]
    with state_lock:
        for name in list(current_versions.keys()):
            if name not in active_names:
                del current_versions[name]
        version_history[:] = [e for e in version_history if e.get("name") in active_names]
    db.prune_version_state(active_names)
    db.prune_version_history(active_names)

    for m in monitor.monitors:
        if m.name in current_versions:
            saved = current_versions[m.name]
            m.last_version = VersionInfo(
                version=saved["version"],
                url=saved["url"],
                detected_at=saved.get("time", ""),
            )
            log(f"[{m.name}] 恢复上次版本: {saved['version']}")
    monitor.start()


@app.route("/")
def index():
    return render_template("index.html")


@app.route("/admin")
def admin():
    return render_template("admin.html")


@app.route("/api/custom-monitors", methods=["GET"])
def get_custom_monitors():
    return jsonify(db.list_monitors())


@app.route("/api/custom-monitors", methods=["POST"])
def add_custom_monitor():
    data = request.get_json(silent=True) or {}
    name = (data.get("name") or "").strip()
    url = (data.get("url") or "").strip()
    parse_type = (data.get("parse_type") or "generic").strip()
    fetch_url = (data.get("fetch_url") or "").strip() or None

    if not name or not url:
        return jsonify({"ok": False, "error": "名称和链接不能为空"}), 400
    if not is_valid_url(url):
        return jsonify({"ok": False, "error": "链接格式不正确，需以 http:// 或 https:// 开头"}), 400
    if parse_type not in PARSE_FUNCTIONS:
        return jsonify({"ok": False, "error": f"不支持的解析方式: {parse_type}"}), 400

    # google-maven 未填写备用地址时自动推导官方元数据源，避免 mvnrepository 的 403
    if parse_type == "google-maven" and not fetch_url:
        fetch_url = google_maven_metadata_url(url)

    try:
        mid = db.add_monitor(name, url, parse_type, fetch_url)
    except Exception as e:
        return jsonify({"ok": False, "error": str(e)}), 500

    if monitor:
        pm = PageMonitor(url, name, PARSE_FUNCTIONS[parse_type], fetch_url=fetch_url)
        monitor.add_monitor(pm)
    return jsonify({"ok": True, "id": mid})


@app.route("/api/custom-monitors/<int:mid>", methods=["PUT"])
def update_custom_monitor(mid):
    data = request.get_json(silent=True) or {}
    name = (data.get("name") or "").strip()
    url = (data.get("url") or "").strip()
    parse_type = (data.get("parse_type") or "generic").strip()
    fetch_url = (data.get("fetch_url") or "").strip() or None

    if not name or not url:
        return jsonify({"ok": False, "error": "名称和链接不能为空"}), 400
    if not is_valid_url(url):
        return jsonify({"ok": False, "error": "链接格式不正确，需以 http:// 或 https:// 开头"}), 400
    if parse_type not in PARSE_FUNCTIONS:
        return jsonify({"ok": False, "error": f"不支持的解析方式: {parse_type}"}), 400
    if parse_type == "google-maven" and not fetch_url:
        fetch_url = google_maven_metadata_url(url)

    row = db.get_monitor(mid)
    if not row:
        return jsonify({"ok": False, "error": "监控项不存在"}), 404

    try:
        db.update_monitor(mid, name, url, parse_type, fetch_url)
    except Exception as e:
        return jsonify({"ok": False, "error": str(e)}), 500

    # 同步运行中的监控：先移除旧项，再按新配置加入
    if monitor:
        if row:
            monitor.remove_monitor(row["name"])
        pm = PageMonitor(url, name, PARSE_FUNCTIONS[parse_type], fetch_url=fetch_url)
        monitor.add_monitor(pm)
    return jsonify({"ok": True})


@app.route("/api/custom-monitors/<int:mid>", methods=["DELETE"])
def remove_custom_monitor(mid):
    row = db.get_monitor(mid)
    db.remove_monitor(mid)
    if monitor and row:
        monitor.remove_monitor(row["name"])
    return jsonify({"ok": True})


@app.route("/api/test-parse", methods=["POST"])
def test_parse():
    data = request.get_json(silent=True) or {}
    url = (data.get("url") or "").strip()
    parse_type = (data.get("parse_type") or "generic").strip()
    fetch_url = (data.get("fetch_url") or "").strip() or None

    if not url:
        return jsonify({"ok": False, "error": "链接不能为空"}), 400
    if not is_valid_url(url):
        return jsonify({"ok": False, "error": "链接格式不正确，需以 http:// 或 https:// 开头"}), 400
    if parse_type not in PARSE_FUNCTIONS:
        return jsonify({"ok": False, "error": f"不支持的解析方式: {parse_type}"}), 400

    pm = PageMonitor(url, "测试", PARSE_FUNCTIONS[parse_type], fetch_url=fetch_url)
    result = pm.check()
    if result:
        return jsonify({"ok": True, "version": result.version, "url": result.url})
    return jsonify({"ok": False, "error": "未能解析出版本，请检查链接或解析方式"})


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

    with monitor._monitors_lock:
        snapshot = list(monitor.monitors)
    for m in snapshot:
        result = m.check()
        if result:
            # 统一走 on_new_version：首次仅记录版本，有旧版本才通知
            if not m.last_version or result.version != m.last_version.version:
                on_new_version(m.name, result)
            m.last_version = result
    # 本轮检查完成：合并同一轮检测到的多个更新为一封邮件
    flush_pending_notifications()
    return jsonify({"status": "checked"})


_shut_down = False


def shutdown():
    """安全退出：停止后台监控线程，幂等。"""
    global _shut_down
    if _shut_down:
        return
    _shut_down = True
    log("\n正在关闭...")
    flush_pending_notifications()
    if monitor:
        monitor.stop()


if __name__ == "__main__":
    init_monitor()
    atexit.register(shutdown)

    def _signal_handler(signum, frame):
        log(f"\n收到信号 {signum}，程序即将退出...")
        shutdown()
        sys.exit(0)

    signal.signal(signal.SIGINT, _signal_handler)
    signal.signal(signal.SIGTERM, _signal_handler)

    try:
        app.run(host="127.0.0.1", port=5000, debug=False)
    except KeyboardInterrupt:
        shutdown()
    finally:
        shutdown()