"""
Unit tests for OwnVoice Desktop Hub UI architecture & views.
Verifies all 7 views, sidebar navigation routing, vocabulary CRUD, snippet CRUD, and stats calculation.
"""
import unittest
import customtkinter as ctk
from tests.test_helpers import InMemoryConfigManager
from ui.settings_window import SettingsWindow
from core.snippet_engine import SnippetEngine
from core.note_store import NoteStore

class MockAudioRecorder:
    def __init__(self):
        self.is_recording = False
        self.is_monitoring = False
        self.volume = 0.55

    def get_input_devices(self):
        return [(0, "Realtek Audio"), (1, "USB Microphone")]

    def get_current_volume(self):
        return self.volume

    def start_monitoring(self, device_index=None):
        self.is_monitoring = True

    def stop_monitoring(self):
        self.is_monitoring = False

class MockAIEngine:
    def test_key(self, key):
        return (key == "VALID_KEY"), ("Valid Key" if key == "VALID_KEY" else "Invalid Key")

    def test_connection(self, key):
        return (key == "VALID_KEY"), "Connected to Gemini 3.6", 42.0

class TestDesktopHubUI(unittest.TestCase):
    def setUp(self):
        self.config = InMemoryConfigManager()
        self.ai = MockAIEngine()
        self.audio = MockAudioRecorder()
        self.snippet_engine = SnippetEngine(self.config)
        self.dictation_toggled = False

        def toggle_cb():
            self.dictation_toggled = True

        self.hub = SettingsWindow(
            config_manager=self.config,
            ai_engine=self.ai,
            audio_recorder=self.audio,
            snippet_engine=self.snippet_engine,
            on_toggle_dictation=toggle_cb
        )

    def tearDown(self):
        if self.hub and self.hub.is_open:
            self.hub._on_close()

    def test_hub_open_and_view_initialization(self):
        """Verify hub initializes all 8 modular views on show()."""
        self.hub.show()
        self.assertTrue(self.hub.is_open)
        self.assertIsNotNone(self.hub.window)
        
        expected_views = ["🏠 Home", "🕒 History", "📊 Productivity", "📖 Vocabulary", "⚡ Snippets", "Aa Style", "📱 Phone", "⚙️ Settings"]
        for v in expected_views:
            self.assertIn(v, self.hub.views)
            self.assertIsNotNone(self.hub.views[v])

    def test_navigation_routing(self):
        """Verify navigating across all tabs updates active_tab_name and highlights."""
        self.hub.show()

        tabs = ["🕒 History", "📊 Productivity", "📖 Vocabulary", "⚡ Snippets", "Aa Style", "📱 Phone", "⚙️ Settings", "🏠 Home"]
        for tab in tabs:
            self.hub.navigate_to(tab)
            self.assertEqual(self.hub.active_tab_name, tab)

    def test_home_view_interaction(self):
        """Verify HomeView CTA triggers on_toggle_dictation."""
        self.hub.show()
        self.hub.navigate_to("🏠 Home")
        home_view = self.hub.views["🏠 Home"]
        self.assertIsNotNone(home_view)

        # Trigger speak CTA
        home_view._handle_speak_cta()
        self.assertTrue(self.dictation_toggled)

    def test_productivity_view_rendering_and_filters(self):
        """Verify ProductivityView metric cards, time range filter, and chart canvas."""
        self.hub.show()
        self.hub.navigate_to("📊 Productivity")
        prod_view = self.hub.views["📊 Productivity"]
        self.assertIsNotNone(prod_view)
        self.assertIsNotNone(prod_view.chart_canvas)

        # Change range filter
        prod_view._set_time_range("30 days")
        self.assertEqual(prod_view.time_range, "30 days")

    def test_vocabulary_crud(self):
        """Verify adding, filtering, and deleting vocabulary terms."""
        self.hub.show()
        self.hub.navigate_to("📖 Vocabulary")
        vocab_view = self.hub.views["📖 Vocabulary"]

        # Add a word
        vocab_view.word_input.insert(0, "QuantumLeap")
        vocab_view.category_select.set("Technology")
        vocab_view.replacement_input.insert(0, "QuantumLeap")
        vocab_view._save_word()

        # Word should be saved in config
        terms = self.config.get_structured_vocabulary()
        self.assertTrue(any(t["term"] == "QuantumLeap" for t in terms))

        # Delete the word
        vocab_view._delete_term("QuantumLeap")
        terms_after = self.config.get_structured_vocabulary()
        self.assertFalse(any(t["term"] == "QuantumLeap" for t in terms_after))

    def test_snippets_crud(self):
        """Verify adding, selecting, editing, and deleting snippets."""
        self.hub.show()
        self.hub.navigate_to("⚡ Snippets")
        snip_view = self.hub.views["⚡ Snippets"]

        # Add new snippet
        snip_view._clear_form()
        snip_view.trigger_input.insert(0, "github profile")
        snip_view.title_input.insert(0, "GitHub")
        snip_view.category_select.set("Link")
        snip_view.expansion_text.insert("1.0", "https://github.com/theheroayush")
        snip_view._save_snippet()

        snippets = self.snippet_engine.get_structured_snippets()
        self.assertIn("github profile", snippets)
        self.assertEqual(snippets["github profile"]["category"], "Link")

        # Test expansion through SnippetEngine
        expanded = self.snippet_engine.expand("Check out my github profile please")
        self.assertIn("https://github.com/theheroayush", expanded)

        # Delete snippet
        snip_view.active_trigger = "github profile"
        snip_view._delete_active_snippet()
        snippets_after = self.snippet_engine.get_structured_snippets()
        self.assertNotIn("github profile", snippets_after)

    def test_style_view_mode_selection(self):
        """Verify changing tone mode and live preview updates."""
        self.hub.show()
        self.hub.navigate_to("Aa Style")
        style_view = self.hub.views["Aa Style"]

        style_view._select_mode("formal_email")
        self.assertEqual(self.config.get("dictation_mode"), "formal_email")

        style_view._select_mode("code")
        self.assertEqual(self.config.get("dictation_mode"), "code")

        # Test live preview style switching
        style_view._set_preview_style("Formal")
        self.assertEqual(style_view.active_preview_style, "Formal")
        self.assertIn("circulate", style_view.output_text_lbl.cget("text").lower())

    def test_history_view_selection_and_copy(self):
        """Verify selecting items in HistoryView populates inspector, metadata, and clipboard."""
        self.config.add_history_entry({"timestamp": "2026-09-19 12:00:00", "text": "Test dictation for history view", "mode": "smart_flow"})
        self.hub.show()
        self.hub.navigate_to("🕒 History")
        hist_view = self.hub.views["🕒 History"]

        hist_view._load_items()
        self.assertIsNotNone(hist_view.selected_item)
        self.assertEqual(hist_view.meta_labels["model_display"].cget("text"), "Google Gemini")

        hist_view._copy_selected()
        copied = self.hub.window.clipboard_get()
        self.assertIn("Test dictation for history view", copied)

if __name__ == "__main__":
    unittest.main()
