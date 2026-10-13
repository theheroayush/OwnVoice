"""
Unit and integration tests for Milestone 2:
- Gemini Live 2.0-flash model update and fail-fast disconnect handling
- Zero fabricated productivity metrics on fresh installs (history.json / notes.db)
- NoteStore list_notes SQLite integration
- ProductivityView dynamic binding and empty state handling
- Process context telemetry persistence in history entries
- StyleView app_styles load/save wiring
"""
import unittest
import time
import datetime
import tempfile
import json
from pathlib import Path
import customtkinter as ctk

from core.gemini_live_client import GeminiLiveClient, DEFAULT_LIVE_MODEL
from core.note_store import NoteStore
from config import ConfigManager, DEFAULT_CONFIG
from tests.test_helpers import InMemoryConfigManager
from ui.views.productivity_view import ProductivityView
from ui.views.style_view import StyleView
from ui.views.history_view import HistoryView


class TestGeminiLiveModelAndFailFast(unittest.TestCase):
    def test_default_model_is_gemini_2_0_flash(self):
        """Verify the model was updated from invalid 3.5 to models/gemini-2.0-flash."""
        self.assertEqual(DEFAULT_LIVE_MODEL, "models/gemini-2.0-flash")
        client = GeminiLiveClient(api_key="mock_key")
        self.assertEqual(client.model, "models/gemini-2.0-flash")
        client.cancel()

    def test_fail_fast_on_handshake_or_disconnect(self):
        """
        Verify fail-fast behavior: when WebSocket connection drops or handshake fails,
        finish_and_get_transcript() MUST NOT stall for the full 2.0s timeout.
        """
        client = GeminiLiveClient(api_key="mock_invalid_key_999")
        # Connection handshake should fail immediately or within small timeout
        ok = client.start_session("System instruction", timeout=1.0)
        self.assertFalse(ok)
        self.assertFalse(client.is_connected())

        t_start = time.time()
        # Request transcript with 2.0s timeout: should return immediately (<0.15s)
        text, latency = client.finish_and_get_transcript(timeout=2.0)
        elapsed = time.time() - t_start

        self.assertEqual(text, "")
        self.assertLess(elapsed, 0.25, f"finish_and_get_transcript stalled for {elapsed:.3f}s instead of failing fast!")
        client.cancel()

    def test_fail_fast_on_active_abrupt_disconnect(self):
        """Simulate an active connection abruptly closing and verify immediate unblock."""
        client = GeminiLiveClient(api_key="mock_key")
        client._is_active = True
        client._error = "Connection dropped by peer"
        client._transcript_done.set()

        t_start = time.time()
        text, latency = client.finish_and_get_transcript(timeout=2.0)
        elapsed = time.time() - t_start

        self.assertLess(elapsed, 0.15)
        client.cancel()


class TestHonestProductivityStats(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.db_path = Path(self.temp_dir.name) / "test_notes.db"
        self.note_store = NoteStore(db_path=self.db_path)
        self.config = InMemoryConfigManager()

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_zero_fabricated_metrics_on_empty_install(self):
        """
        Verify that on a fresh clean install with zero history and zero notes,
        no fabricated stats (such as 1284 words, 42 dictations) are returned.
        """
        stats = self.config.get_productivity_stats(note_store=self.note_store)
        self.assertEqual(stats["words_today"], 0, "words_today must be 0 on empty install!")
        self.assertEqual(stats["dictations_today"], 0, "dictations_today must be 0 on empty install!")
        self.assertEqual(stats["time_saved_min"], 0, "time_saved_min must be 0 on empty install!")
        self.assertEqual(stats["hours_saved_week"], 0.0, "hours_saved_week must be 0.0 on empty install!")

        # Verify default config has app_styles configured
        self.assertIn("app_styles", self.config.config)
        self.assertIn("WhatsApp", self.config.config["app_styles"])
        self.assertIn("Email", self.config.config["app_styles"])

    def test_genuine_stats_calculation_from_history(self):
        """Verify metrics are calculated strictly from real history entries."""
        now_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        self.config.add_history_entry({
            "timestamp": now_str,
            "text": "The quick brown fox jumps over the lazy dog",  # 9 words
            "mode": "smart_flow",
            "latency": 0.35,
            "app": "code.exe"
        })
        self.config.add_history_entry({
            "timestamp": now_str,
            "text": "Hello world from OwnVoice dictation",  # 5 words
            "mode": "code",
            "latency": 0.22,
            "app": "code.exe"
        })

        stats = self.config.get_productivity_stats(note_store=self.note_store)
        self.assertEqual(stats["words_today"], 14)
        self.assertEqual(stats["dictations_today"], 2)
        expected_saved_min = round(14 * 0.0183)
        self.assertEqual(stats["time_saved_min"], expected_saved_min)

    def test_sqlite_notes_integration_via_list_notes(self):
        """
        Verify NoteStore.list_notes() is invoked correctly without AttributeError
        (fixing the prior get_all_notes bug).
        """
        self.note_store.save_note(
            title="Meeting Action Items",
            category="general",
            structured_content="Review milestone 2 implementation details and run test suite",  # 9 words
            raw_transcript="review milestone 2 implementation details and run test suite",
            tags=["m2", "desktop"],
            source="desktop"
        )

        notes = self.note_store.list_notes(limit=10)
        self.assertEqual(len(notes), 1)

        stats = self.config.get_productivity_stats(note_store=self.note_store)
        self.assertEqual(stats["words_today"], 9)
        self.assertEqual(stats["dictations_today"], 1)


class TestProductivityViewDynamicBinding(unittest.TestCase):
    def setUp(self):
        self.root = ctk.CTk()
        self.root.withdraw()
        self.config = InMemoryConfigManager()
        self.temp_dir = tempfile.TemporaryDirectory()
        self.db_path = Path(self.temp_dir.name) / "test_notes.db"
        self.note_store = NoteStore(db_path=self.db_path)

    def tearDown(self):
        self.root.destroy()
        self.temp_dir.cleanup()

    def test_clean_empty_state_rendering(self):
        """Verify ProductivityView handles zero metrics honestly without fabricated static numbers."""
        view = ProductivityView(self.root, hub=None, config_manager=self.config, note_store=self.note_store)
        self.assertEqual(view.total_words, 0)
        self.assertEqual(view.total_dictations, 0)
        self.assertEqual(view.current_streak, 0)
        self.assertEqual(view.longest_streak, 0)

        # Time range filter switching
        for r in ["7 days", "30 days", "3 months", "All time"]:
            view._set_time_range(r)
            self.assertEqual(view.time_range, r)

    def test_live_data_binding(self):
        """Verify ProductivityView binds to real entries and computes metrics dynamically."""
        now_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        self.config.add_history_entry({
            "timestamp": now_str,
            "text": "This is a real user dictation in Slack",  # 8 words
            "mode": "chat",
            "app": "slack.exe"
        })
        view = ProductivityView(self.root, hub=None, config_manager=self.config, note_store=self.note_store)
        self.assertEqual(view.total_words, 8)
        self.assertEqual(view.total_dictations, 1)
        self.assertEqual(view.current_streak, 1)


class TestStyleViewAndTelemetryWiring(unittest.TestCase):
    def setUp(self):
        self.root = ctk.CTk()
        self.root.withdraw()
        self.config = InMemoryConfigManager()

    def tearDown(self):
        self.root.destroy()

    def test_style_view_app_styles_persistence(self):
        """Verify StyleView loads app_styles from config and persists changes dynamically."""
        style_view = StyleView(self.root, hub=None, config_manager=self.config)
        self.assertIn("WhatsApp", style_view.app_style_menus)

        # Simulate user changing WhatsApp dropdown
        style_view._on_app_style_changed("WhatsApp", "Casual")
        saved_styles = self.config.get("app_styles")
        self.assertEqual(saved_styles["WhatsApp"], "Casual")

        # Simulate user changing Code dropdown
        style_view._on_app_style_changed("Code", "Technical")
        saved_styles = self.config.get("app_styles")
        self.assertEqual(saved_styles["Code"], "Technical")

    def test_history_view_genuine_app_telemetry(self):
        """Verify HistoryView renders genuine app telemetry from history entry."""
        self.config.add_history_entry({
            "timestamp": "2026-10-13 14:00:00",
            "text": "Refactor auth endpoints in VS Code",
            "mode": "code",
            "latency": 0.41,
            "app": "code.exe"
        })
        hist_view = HistoryView(self.root, hub=None, config_manager=self.config)
        hist_view._load_items()
        self.assertIsNotNone(hist_view.selected_item)
        self.assertEqual(hist_view.selected_item["app"], "VS Code")
        self.assertEqual(hist_view.selected_item["icon"], "💻")
        self.assertEqual(hist_view.selected_item["latency"], 0.41)


if __name__ == "__main__":
    unittest.main()
