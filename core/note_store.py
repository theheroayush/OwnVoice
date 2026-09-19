import sqlite3
import json
import uuid
import datetime
from pathlib import Path
from contextlib import contextmanager
from typing import List, Dict, Any, Optional, Generator

DEFAULT_DB_PATH = Path(__file__).resolve().parent.parent / "notes.db"

class NoteStore:
    """
    Production-grade, zero-dependency SQLite persistence layer for OwnVoice notes.
    Features:
    - WAL (Write-Ahead Logging) mode for concurrent reader/writer safety.
    - FTS5 full-text indexing for instant multi-keyword searching across titles and content.
    - Automatic fallback if FTS5 module is not compiled into SQLite.
    - Tagging, categorization, pinning, and markdown exporting.
    - Deterministic connection lifecycle with contextmanager to eliminate Windows file locking.
    """

    def __init__(self, db_path: Optional[Path] = None):
        self.db_path = Path(db_path) if db_path else DEFAULT_DB_PATH
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self.has_fts5 = False
        self._init_db()

    @contextmanager
    def _connect(self) -> Generator[sqlite3.Connection, None, None]:
        conn = sqlite3.connect(str(self.db_path), timeout=10.0)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA journal_mode=WAL;")
        conn.execute("PRAGMA foreign_keys=ON;")
        try:
            yield conn
        finally:
            conn.close()

    def _init_db(self):
        with self._connect() as conn:
            cursor = conn.cursor()
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS notes (
                    id TEXT PRIMARY KEY,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    title TEXT NOT NULL,
                    category TEXT NOT NULL DEFAULT 'general',
                    structured_content TEXT NOT NULL,
                    raw_transcript TEXT NOT NULL,
                    tags TEXT DEFAULT '[]',
                    source TEXT DEFAULT 'desktop',
                    pinned INTEGER DEFAULT 0,
                    archived INTEGER DEFAULT 0
                )
            """)

            cursor.execute("CREATE INDEX IF NOT EXISTS idx_notes_created ON notes(created_at DESC)")
            cursor.execute("CREATE INDEX IF NOT EXISTS idx_notes_category ON notes(category)")
            cursor.execute("CREATE INDEX IF NOT EXISTS idx_notes_pinned ON notes(pinned)")

            # Check and initialize FTS5
            try:
                cursor.execute("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS notes_fts USING fts5(
                        title,
                        structured_content,
                        raw_transcript,
                        content='notes',
                        content_rowid='rowid'
                    )
                """)
                # Auto-sync triggers for FTS5
                cursor.execute("""
                    CREATE TRIGGER IF NOT EXISTS notes_ai AFTER INSERT ON notes BEGIN
                        INSERT INTO notes_fts(rowid, title, structured_content, raw_transcript)
                        VALUES (new.rowid, new.title, new.structured_content, new.raw_transcript);
                    END;
                """)
                cursor.execute("""
                    CREATE TRIGGER IF NOT EXISTS notes_ad AFTER DELETE ON notes BEGIN
                        INSERT INTO notes_fts(notes_fts, rowid, title, structured_content, raw_transcript)
                        VALUES ('delete', old.rowid, old.title, old.structured_content, old.raw_transcript);
                    END;
                """)
                cursor.execute("""
                    CREATE TRIGGER IF NOT EXISTS notes_au AFTER UPDATE ON notes BEGIN
                        INSERT INTO notes_fts(notes_fts, rowid, title, structured_content, raw_transcript)
                        VALUES ('delete', old.rowid, old.title, old.structured_content, old.raw_transcript);
                        INSERT INTO notes_fts(rowid, title, structured_content, raw_transcript)
                        VALUES (new.rowid, new.title, new.structured_content, new.raw_transcript);
                    END;
                """)
                self.has_fts5 = True
            except sqlite3.OperationalError:
                self.has_fts5 = False
            conn.commit()

    def save_note(
        self,
        title: str,
        category: str,
        structured_content: str,
        raw_transcript: str,
        tags: Optional[List[str]] = None,
        source: str = "desktop",
        note_id: Optional[str] = None
    ) -> str:
        """Saves a new note or updates an existing one."""
        nid = note_id or str(uuid.uuid4())
        now = datetime.datetime.now(datetime.timezone.utc).isoformat()
        tags_json = json.dumps(tags or [], ensure_ascii=False)

        with self._connect() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT id FROM notes WHERE id = ?", (nid,))
            existing = cursor.fetchone()

            if existing:
                cursor.execute("""
                    UPDATE notes
                    SET updated_at = ?,
                        title = ?,
                        category = ?,
                        structured_content = ?,
                        raw_transcript = ?,
                        tags = ?,
                        source = ?
                    WHERE id = ?
                """, (now, title.strip(), category.strip().lower(), structured_content.strip(),
                      raw_transcript.strip(), tags_json, source, nid))
            else:
                cursor.execute("""
                    INSERT INTO notes (
                        id, created_at, updated_at, title, category,
                        structured_content, raw_transcript, tags, source, pinned, archived
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                """, (nid, now, now, title.strip(), category.strip().lower(),
                      structured_content.strip(), raw_transcript.strip(), tags_json, source))
            conn.commit()
        return nid

    def get_note(self, note_id: str) -> Optional[Dict[str, Any]]:
        """Fetches a single note by ID."""
        with self._connect() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM notes WHERE id = ?", (note_id,))
            row = cursor.fetchone()
            if not row:
                return None
            return self._row_to_dict(row)

    def list_notes(
        self,
        category: Optional[str] = None,
        limit: int = 50,
        offset: int = 0,
        include_archived: bool = False
    ) -> List[Dict[str, Any]]:
        """Lists notes ordered by pinned status and creation date."""
        query = "SELECT * FROM notes WHERE 1=1"
        params: List[Any] = []

        if not include_archived:
            query += " AND archived = 0"
        if category:
            query += " AND category = ?"
            params.append(category.strip().lower())

        query += " ORDER BY pinned DESC, created_at DESC LIMIT ? OFFSET ?"
        params.extend([limit, offset])

        with self._connect() as conn:
            cursor = conn.cursor()
            cursor.execute(query, params)
            rows = cursor.fetchall()
            return [self._row_to_dict(r) for r in rows]

    def search_notes(self, query: str, limit: int = 20) -> List[Dict[str, Any]]:
        """Performs full-text search with automatic fallback to LIKE queries."""
        clean_q = query.strip()
        if not clean_q:
            return self.list_notes(limit=limit)

        with self._connect() as conn:
            cursor = conn.cursor()
            if self.has_fts5:
                safe_q = clean_q.replace('"', '""')
                terms = [f'"{t}"*' for t in safe_q.split() if t]
                match_expr = " ".join(terms) if terms else f'"{safe_q}"*'
                try:
                    cursor.execute("""
                        SELECT notes.* FROM notes
                        JOIN notes_fts ON notes.rowid = notes_fts.rowid
                        WHERE notes_fts MATCH ? AND notes.archived = 0
                        ORDER BY bm25(notes_fts) ASC, notes.created_at DESC
                        LIMIT ?
                    """, (match_expr, limit))
                    rows = cursor.fetchall()
                    return [self._row_to_dict(r) for r in rows]
                except sqlite3.OperationalError:
                    pass

            like_term = f"%{clean_q}%"
            cursor.execute("""
                SELECT * FROM notes
                WHERE (title LIKE ? OR structured_content LIKE ? OR raw_transcript LIKE ?)
                  AND archived = 0
                ORDER BY created_at DESC
                LIMIT ?
            """, (like_term, like_term, like_term, limit))
            rows = cursor.fetchall()
            return [self._row_to_dict(r) for r in rows]

    def delete_note(self, note_id: str) -> bool:
        """Permanently deletes a note."""
        with self._connect() as conn:
            cursor = conn.cursor()
            cursor.execute("DELETE FROM notes WHERE id = ?", (note_id,))
            conn.commit()
            return cursor.rowcount > 0

    def toggle_pin(self, note_id: str) -> bool:
        """Toggles pinned state of a note."""
        with self._connect() as conn:
            cursor = conn.cursor()
            cursor.execute("UPDATE notes SET pinned = (1 - pinned) WHERE id = ?", (note_id,))
            conn.commit()
            return cursor.rowcount > 0

    def export_markdown(self, note_id: str, dest_path: Optional[Path] = None) -> Optional[str]:
        """Exports a note to a clean Markdown file with YAML frontmatter."""
        note = self.get_note(note_id)
        if not note:
            return None

        tags_str = ", ".join(note.get("tags", []))
        md_content = f"""---
title: "{note['title']}"
category: {note['category']}
created_at: {note['created_at']}
tags: [{tags_str}]
source: {note['source']}
---

# {note['title']}

{note['structured_content']}

---
### 🎙️ Raw Spoken Transcript
> {note['raw_transcript']}
"""
        if dest_path:
            dest = Path(dest_path)
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_text(md_content, encoding="utf-8")
        return md_content

    def get_stats(self) -> Dict[str, Any]:
        """Returns aggregate note metrics."""
        with self._connect() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT COUNT(*) as total, SUM(CASE WHEN pinned=1 THEN 1 ELSE 0 END) as pinned FROM notes WHERE archived=0")
            row = cursor.fetchone()
            cursor.execute("SELECT category, COUNT(*) as count FROM notes WHERE archived=0 GROUP BY category")
            cats = {r["category"]: r["count"] for r in cursor.fetchall()}
            return {
                "total_notes": row["total"] if row else 0,
                "pinned_notes": row["pinned"] if row and row["pinned"] else 0,
                "categories": cats
            }

    @staticmethod
    def _row_to_dict(row: sqlite3.Row) -> Dict[str, Any]:
        d = dict(row)
        try:
            d["tags"] = json.loads(d.get("tags") or "[]")
        except Exception:
            d["tags"] = []
        d["pinned"] = bool(d.get("pinned", 0))
        d["archived"] = bool(d.get("archived", 0))
        return d
