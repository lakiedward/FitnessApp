import os
import hashlib
from datetime import datetime, timezone

from app.database.connection import get_db


def _read_sql(file_path: str) -> str:
    with open(file_path, "r", encoding="utf-8") as f:
        return f.read().lstrip("\ufeff")


def _split_statements(sql: str):
    # Simple splitter by ';' that tolerates newlines/comments and BOM
    # Assumes we don't define custom delimiters in these files
    parts = []
    buffer = []
    in_block_comment = False
    for raw in sql.splitlines():
        line = raw.lstrip("\ufeff")
        s = line.strip()
        if in_block_comment:
            if "*/" in line:
                in_block_comment = False
            continue
        if s.startswith("/*"):
            in_block_comment = not s.endswith("*/")
            continue
        # Skip full-line comments
        if s.startswith("--") or s == "":
            continue
        buffer.append(line)
        if line.rstrip().endswith(";"):
            parts.append("\n".join(buffer).strip())
            buffer = []
    if buffer:
        tail = "\n".join(buffer).strip()
        if tail:
            parts.append(tail)
    return parts


def _ensure_migrations_table():
    with get_db() as db:
        cur = db.cursor()
        cur.execute(
            """
            CREATE TABLE IF NOT EXISTS schema_migrations (
              id INT PRIMARY KEY AUTO_INCREMENT,
              filename VARCHAR(255) NOT NULL UNIQUE,
              checksum VARCHAR(64) NOT NULL,
              applied_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """
        )
        db.commit()


def _already_applied(filename: str, checksum: str) -> bool:
    with get_db() as db:
        cur = db.cursor()
        cur.execute(
            "SELECT checksum FROM schema_migrations WHERE filename = %s",
            (filename,),
        )
        row = cur.fetchone()
        if not row:
            return False
        return row[0] == checksum


def _record_applied(filename: str, checksum: str):
    with get_db() as db:
        cur = db.cursor()
        cur.execute(
            "INSERT INTO schema_migrations(filename, checksum, applied_at) VALUES (%s, %s, %s)",
            (filename, checksum, datetime.utcnow()),
        )
        db.commit()


def _hash(sql: str) -> str:
    return hashlib.sha256(sql.encode("utf-8")).hexdigest()


def collect_migration_files() -> list[str]:
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    candidates: list[str] = []

    # 1) BD_Fitness_App/init (skip 00_create_database.sql on managed DB)
    init_dir = os.path.join(repo_root, "BD_Fitness_App", "init")
    if os.path.isdir(init_dir):
        for name in sorted(os.listdir(init_dir)):
            if not name.lower().endswith(".sql"):
                continue
            if name.startswith("00_") or "create_database" in name.lower():
                continue
            candidates.append(os.path.join(init_dir, name))

    # 2) app/database/migrations
    mig_dir = os.path.join(repo_root, "app", "database", "migrations")
    if os.path.isdir(mig_dir):
        for name in sorted(os.listdir(mig_dir)):
            if name.lower().endswith(".sql"):
                candidates.append(os.path.join(mig_dir, name))

    return candidates


def apply_all():
    _ensure_migrations_table()
    files = collect_migration_files()
    for fp in files:
        try:
            sql = _read_sql(fp)
            checksum = _hash(sql)
            filename = os.path.basename(fp)
            if _already_applied(filename, checksum):
                continue

            statements = _split_statements(sql)
            if not statements:
                _record_applied(filename, checksum)
                continue

            with get_db() as db:
                cur = db.cursor()
                # Handle idempotent errors gracefully (table exists, duplicate column, etc.)
                import mysql.connector
                from mysql.connector import errorcode

                benign = {
                    errorcode.ER_TABLE_EXISTS_ERROR,        # 1050
                    errorcode.ER_DUP_FIELDNAME,            # 1060
                    errorcode.ER_DUP_KEYNAME,              # 1061
                    errorcode.ER_CANT_DROP_FIELD_OR_KEY,   # 1091
                    errorcode.ER_DUP_ENTRY,                # 1062
                    3757,                                  # functional index on JSON/BLOB (MySQL 8)
                }

                for stmt in statements:
                    # Defensive: ignore empty or USE statements without trailing ';'
                    s = stmt.strip()
                    if not s:
                        continue
                    try:
                        cur.execute(s)
                    except mysql.connector.Error as err:
                        msg = str(err).lower()
                        if err.errno in benign or "functional index" in msg:
                            print(f"  Skipping benign error [{err.errno}] for statement: {s[:120]}...")
                            continue
                        cur.close()
                        raise
                db.commit()
            _record_applied(filename, checksum)
        except Exception as e:
            # Surface error but do not stop other app startup flows by raising
            print(f"Migration failed for {fp}: {e}")


if __name__ == "__main__":
    apply_all()
