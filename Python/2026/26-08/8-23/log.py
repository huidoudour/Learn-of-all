from datetime import datetime


def log(*args, **kwargs):
    """带时间戳的日志输出，统一以 [INFO] 级别显示监控操作信息。"""
    ts = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    flush = kwargs.pop("flush", False)
    parts: list[str] = []
    for a in args:
        if a is None:
            continue
        parts.append(str(a))
    msg = " ".join(parts)
    print(f"[INFO] {ts} - {msg}", flush=flush)
