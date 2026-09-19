import os
import sys
import tempfile
import json
from pathlib import Path

APP_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(APP_DIR))

from core.note_store import NoteStore
from core.note_engine import NoteEngine
from core.selection_reader import SelectionReader
from config import config_manager

def test_note_store_crud():
    with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmpdir:
        db_path = Path(tmpdir) / "test_notes.db"
        store = NoteStore(db_path=db_path)

        # 1. Save note
        note_id = store.save_note(
            title="Q3 Roadmap Strategy",
            category="brain_dump",
            structured_content="### 💡 Executive Summary\nShip voice dictation.",
            raw_transcript="ship voice dictation this quarter",
            tags=["roadmap", "product"],
            source="desktop_hotkey"
        )
        assert note_id is not None
        assert len(note_id) > 10

        # 2. Get note
        note = store.get_note(note_id)
        assert note is not None
        assert note["title"] == "Q3 Roadmap Strategy"
        assert note["category"] == "brain_dump"
        assert "roadmap" in note["tags"]
        assert note["pinned"] is False

        # 3. Update note
        store.save_note(
            title="Q3 Roadmap Strategy (Updated)",
            category="brain_dump",
            structured_content="### 💡 Executive Summary\nShip voice dictation and scratchpad.",
            raw_transcript="ship voice dictation this quarter",
            tags=["roadmap", "product", "v2"],
            source="desktop_hotkey",
            note_id=note_id
        )
        updated = store.get_note(note_id)
        assert updated["title"] == "Q3 Roadmap Strategy (Updated)"
        assert "v2" in updated["tags"]

        # 4. Pin note
        ok = store.toggle_pin(note_id)
        assert ok is True
        pinned_note = store.get_note(note_id)
        assert pinned_note["pinned"] is True

        # 5. List notes
        notes = store.list_notes()
        assert len(notes) == 1
        assert notes[0]["id"] == note_id

        # 6. Delete note
        del_ok = store.delete_note(note_id)
        assert del_ok is True
        assert store.get_note(note_id) is None


def test_note_store_fts5_search():
    with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmpdir:
        db_path = Path(tmpdir) / "search_notes.db"
        store = NoteStore(db_path=db_path)

        store.save_note(
            title="Kubernetes Deployment Guide",
            category="memo",
            structured_content="Deploy pods using Helm charts and ingress controllers.",
            raw_transcript="deploy pods using helm charts",
            tags=["devops", "k8s"]
        )

        store.save_note(
            title="Stripe Payment Webhook",
            category="task_list",
            structured_content="- [ ] Verify signature\n- [ ] Idempotency key handling",
            raw_transcript="verify stripe signature",
            tags=["billing", "stripe"]
        )

        # Search by title keyword
        res1 = store.search_notes("Kubernetes")
        assert len(res1) == 1
        assert res1[0]["title"] == "Kubernetes Deployment Guide"

        # Search by content keyword
        res2 = store.search_notes("idempotency")
        assert len(res2) == 1
        assert res2[0]["title"] == "Stripe Payment Webhook"

        # Search non-matching
        res3 = store.search_notes("nonexistentword123")
        assert len(res3) == 0


def test_note_store_export_markdown():
    with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmpdir:
        db_path = Path(tmpdir) / "export_notes.db"
        store = NoteStore(db_path=db_path)

        note_id = store.save_note(
            title="Sprint Planning",
            category="meeting",
            structured_content="### 🎯 Decisions\nLaunch beta on Friday.",
            raw_transcript="we will launch beta on friday",
            tags=["sprint"]
        )

        dest_file = Path(tmpdir) / "Sprint_Planning.md"
        md = store.export_markdown(note_id, dest_path=dest_file)
        assert md is not None
        assert "title: \"Sprint Planning\"" in md
        assert "Launch beta on Friday." in md
        assert dest_file.exists()


def test_note_engine_offline_fallback():
    engine = NoteEngine(config_manager)
    res = engine._offline_structure(
        audio_wav_bytes=None,
        raw_text="we need to fix the authentication bug and deploy by tomorrow noon",
        category="task_list",
        selection_context=None,
        source="desktop"
    )

    assert res["title"] != ""
    assert res["category"] == "task_list"
    assert "authentication bug" in res["structured_content"]
    assert "Action Items" in res["structured_content"]


def test_note_engine_json_extraction():
    engine = NoteEngine(config_manager)

    # JSON wrapped in markdown code fence
    markdown_wrapped = """```json
{
  "title": "Design System Refactor",
  "category": "brain_dump",
  "tags": ["ui", "design"],
  "summary": "Revamp design system tokens.",
  "structured_content": "### 💡 Executive Summary\\nUse tailwind tokens.",
  "raw_transcript": "revamp tokens"
}
```"""
    parsed = engine._extract_json(markdown_wrapped)
    assert parsed is not None
    assert parsed["title"] == "Design System Refactor"
    assert "ui" in parsed["tags"]


def test_selection_reader_safe_execution():
    # Calling get_selected_text should safely execute without raising an unhandled exception
    sel = SelectionReader.get_selected_text(max_wait_ms=30)
    # sel may be None or string if text was selected, but must not crash
    assert sel is None or isinstance(sel, str)


def test_bridge_server_endpoints():
    import urllib.request
    import time
    from core.bridge_server import BridgeServer

    with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmpdir:
        db_path = Path(tmpdir) / "bridge_test.db"
        store = NoteStore(db_path=db_path)
        engine = NoteEngine(config_manager)
        server = BridgeServer(
            port=8799,
            note_store=store,
            note_engine=engine
        )
        server.start()
        time.sleep(0.4)
        try:
            # 1. Test /notes (empty)
            req = urllib.request.Request("http://127.0.0.1:8799/notes")
            with urllib.request.urlopen(req, timeout=3.0) as resp:
                data = json.loads(resp.read().decode())
                assert data["success"] is True
                assert data["notes"] == []

            # 2. Test /note/create
            payload = json.dumps({
                "text": "Meeting with client discussed pricing agreed on 5k tier",
                "category": "meeting",
                "inject": False,
                "source": "android"
            }).encode()
            post_req = urllib.request.Request("http://127.0.0.1:8799/note/create", data=payload, headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(post_req, timeout=15.0) as resp:
                res = json.loads(resp.read().decode())
                assert res["success"] is True
                assert "note" in res
                assert res["note"]["source"] == "android"

            # 3. Test /notes (now contains the note)
            req = urllib.request.Request("http://127.0.0.1:8799/notes")
            with urllib.request.urlopen(req, timeout=3.0) as resp:
                data = json.loads(resp.read().decode())
                assert data["success"] is True
                assert len(data["notes"]) == 1

            # 4. Test /notes/search
            search_req = urllib.request.Request("http://127.0.0.1:8799/notes/search?q=pricing")
            with urllib.request.urlopen(search_req, timeout=3.0) as resp:
                data = json.loads(resp.read().decode())
                assert data["success"] is True
                assert len(data["notes"]) == 1

            # 5. Test /context/selection
            req = urllib.request.Request("http://127.0.0.1:8799/context/selection")
            with urllib.request.urlopen(req, timeout=3.0) as resp:
                data = json.loads(resp.read().decode())
                assert data["success"] is True
                assert "selection" in data
        finally:
            server.stop()


if __name__ == "__main__":
    print("Running OwnVoice Note Architecture Tests...")
    test_note_store_crud()
    print("  [PASS] test_note_store_crud")
    test_note_store_fts5_search()
    print("  [PASS] test_note_store_fts5_search")
    test_note_store_export_markdown()
    print("  [PASS] test_note_store_export_markdown")
    test_note_engine_offline_fallback()
    print("  [PASS] test_note_engine_offline_fallback")
    test_note_engine_json_extraction()
    print("  [PASS] test_note_engine_json_extraction")
    test_selection_reader_safe_execution()
    print("  [PASS] test_selection_reader_safe_execution")
    test_bridge_server_endpoints()
    print("  [PASS] test_bridge_server_endpoints")
    print("ALL 7 TESTS PASSED SUCCESSFULLY! 100% GREEN.")

