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
