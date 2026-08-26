from datetime import datetime


def log(*args, **kwargs):
    """带时间戳的日志输出，方便追踪每条行为发生的时间。"""
    ts = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    flush = kwargs.pop("flush", False)
    print(f"[{ts}]", *args, **kwargs, flush=flush)
