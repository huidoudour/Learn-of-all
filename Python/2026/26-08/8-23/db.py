import os
import sqlite3
from contextlib import closing

DB_FILE = os.path.join(os.path.dirname(__file__), "monitors.db")


def _conn() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_FILE)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    with closing(_conn()) as conn:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS custom_monitors (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                url TEXT NOT NULL,
                parse_type TEXT NOT NULL DEFAULT 'generic',
                fetch_url TEXT,
                created_at TEXT DEFAULT (datetime('now', 'localtime'))
            )
            """
        )
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS version_state (
                name TEXT PRIMARY KEY,
                version TEXT NOT NULL,
                url TEXT,
                updated_at TEXT
            )
            """
        )
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS version_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                version TEXT NOT NULL,
                url TEXT,
                detected_at TEXT
            )
            """
        )
        conn.commit()


def list_monitors() -> list:
    with closing(_conn()) as conn:
        rows = conn.execute(
            "SELECT * FROM custom_monitors ORDER BY id"
        ).fetchall()
        return [dict(r) for r in rows]


def get_monitor(mid: int):
    with closing(_conn()) as conn:
        row = conn.execute(
            "SELECT * FROM custom_monitors WHERE id = ?", (mid,)
        ).fetchone()
        return dict(row) if row else None


def add_monitor(name: str, url: str, parse_type: str, fetch_url: str | None = None) -> int:
    with closing(_conn()) as conn:
        cur = conn.execute(
            "INSERT INTO custom_monitors (name, url, parse_type, fetch_url) VALUES (?,?,?,?)",
            (name, url, parse_type, fetch_url),
        )
        conn.commit()
        return cur.lastrowid


def remove_monitor(mid: int):
    with closing(_conn()) as conn:
        conn.execute("DELETE FROM custom_monitors WHERE id = ?", (mid,))
        conn.commit()


# ---- 版本状态 / 历史记录（替代 state.json） ----

def load_current_versions() -> dict:
    """读取每个监控项目当前版本，返回 {name: {name, version, url, time}}。"""
    with closing(_conn()) as conn:
        rows = conn.execute(
            "SELECT name, version, url, updated_at FROM version_state"
        ).fetchall()
    return {
        r["name"]: {
            "name": r["name"],
            "version": r["version"],
            "url": r["url"],
            "time": r["updated_at"],
        }
        for r in rows
    }


def load_version_history(limit: int = 50) -> list:
    """读取版本历史（最新在前），返回 [{name, version, url, time}]。"""
    with closing(_conn()) as conn:
        rows = conn.execute(
            "SELECT name, version, url, detected_at FROM version_history ORDER BY id DESC LIMIT ?",
            (limit,),
        ).fetchall()
    return [
        {"name": r["name"], "version": r["version"], "url": r["url"], "time": r["detected_at"]}
        for r in rows
    ]


def save_current_version(name: str, version: str, url: str, time: str):
    """写入/更新某个监控项目的当前版本。"""
    with closing(_conn()) as conn:
        conn.execute(
            """
            INSERT INTO version_state (name, version, url, updated_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(name) DO UPDATE SET
                version=excluded.version,
                url=excluded.url,
                updated_at=excluded.updated_at
            """,
            (name, version, url, time),
        )
        conn.commit()


def add_version_history(name: str, version: str, url: str, time: str):
    """往版本历史里追加一条记录。"""
    with closing(_conn()) as conn:
        conn.execute(
            "INSERT INTO version_history (name, version, url, detected_at) VALUES (?, ?, ?, ?)",
            (name, version, url, time),
        )
        conn.commit()


def prune_version_state(active_names: list):
    """删除不在监控列表中的孤儿当前版本记录。"""
    with closing(_conn()) as conn:
        if active_names:
            placeholders = ",".join("?" * len(active_names))
            conn.execute(
                f"DELETE FROM version_state WHERE name NOT IN ({placeholders})",
                active_names,
            )
        else:
            conn.execute("DELETE FROM version_state")
        conn.commit()


def prune_version_history(active_names: list):
    """删除不在监控列表中的孤儿历史记录。"""
    with closing(_conn()) as conn:
        if active_names:
            placeholders = ",".join("?" * len(active_names))
            conn.execute(
                f"DELETE FROM version_history WHERE name NOT IN ({placeholders})",
                active_names,
            )
        else:
            conn.execute("DELETE FROM version_history")
        conn.commit()
