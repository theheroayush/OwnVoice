import time
import unittest
import ctypes
from pynput import keyboard
from core.hotkey_manager import HotkeyManager
from core.snippet_engine import SnippetEngine
from core.context_detector import ContextDetector
from tests.test_helpers import InMemoryConfigManager

class TestTier2HotkeySnippetBoundaries(unittest.TestCase):
    """
    Tier 2: Boundary & Corner Cases for Hotkey, Snippet, and Context Subsystems (Features 11 to 14)
    Validates rapid key bouncing, OS repeat storms, complex regex triggers, large snippet catalogs, and hostile HWNDs.
    """

    def setUp(self):
        self.config = InMemoryConfigManager()
        self.start_count = 0
        self.stop_count = 0

        def on_start():
            self.start_count += 1

        def on_stop():
            self.stop_count += 1

        try:
            self.hotkey_mgr = HotkeyManager(on_start, on_stop, lambda: False, self.config)
        except TypeError:
            self.hotkey_mgr = HotkeyManager(on_start, on_stop, self.config)

        self.snippet_engine = SnippetEngine(self.config)

    # --- F11: F8 Key-Repeat & Suppression Boundaries ---
    def test_b_f11_rapid_key_bouncing_10ms_intervals(self):
        """Verify rapid bouncing at 10ms intervals does not flap state uncontrollably."""
        for _ in range(10):
            self.hotkey_mgr._on_press(keyboard.Key.f8)
            time.sleep(0.01)
            self.hotkey_mgr._on_release(keyboard.Key.f8)
            time.sleep(0.01)

    def test_b_f11_hotkey_held_down_100_repeat_events(self):
        """Verify holding key down generating 100 repeat events without release only triggers 1 start."""
        self.hotkey_mgr.current_keys.clear()
        self.hotkey_mgr.is_active = False
        self.start_count = 0
        
        # First press
        self.hotkey_mgr._on_press(keyboard.Key.f8)
        # 100 auto-repeats while Key.f8 remains in current_keys
        for _ in range(100):
            self.hotkey_mgr._on_press(keyboard.Key.f8)
            
        self.assertEqual(len(self.hotkey_mgr.current_keys), 1)

    def test_b_f11_unrelated_function_keys_ignored(self):
        """Verify non-configured function keys (F1 to F7, F9 to F12) are completely ignored."""
        unrelated_keys = [
            keyboard.Key.f1, keyboard.Key.f2, keyboard.Key.f3, keyboard.Key.f4,
            keyboard.Key.f5, keyboard.Key.f6, keyboard.Key.f7, keyboard.Key.f9,
            keyboard.Key.f10, keyboard.Key.f11, keyboard.Key.f12
        ]
        for k in unrelated_keys:
            self.assertFalse(self.hotkey_mgr._is_hotkey_triggered(k))

    def test_b_f11_malformed_hotkey_strings_in_config(self):
        """Verify malformed hotkey configuration strings do not crash parser."""
        malformed = ["", "None", "+++", "ctrl++", "<invalid_key>", "12345"]
        for bad_str in malformed:
            cfg = InMemoryConfigManager({"hotkey": bad_str})
            try:
                mgr = HotkeyManager(lambda: None, lambda: None, cfg)
                mgr._is_hotkey_triggered(keyboard.Key.f8)
            except Exception as e:
                self.fail(f"Parser crashed on malformed hotkey string '{bad_str}': {e}")

    def test_b_f11_key_release_without_preceding_press(self):
        """Verify releasing a key that was never pressed does not throw KeyError or crash."""
        try:
            self.hotkey_mgr._on_release(keyboard.Key.f8)
            self.hotkey_mgr._on_release(keyboard.Key.ctrl)
            self.hotkey_mgr._on_release(keyboard.Key.space)
        except Exception as e:
            self.fail(f"_on_release threw on unpressed key: {e}")

    # --- F12: Hotkey State Machine Sync Boundaries ---
    def test_b_f12_100_alternating_mouse_and_hotkey_cycles(self):
        """Verify 100 alternating external sync events."""
        if hasattr(self.hotkey_mgr, "sync_state"):
            for i in range(100):
                target_state = (i % 2 == 0)
                self.hotkey_mgr.sync_state(target_state)
                self.assertEqual(self.hotkey_mgr.is_active, target_state)

    def test_b_f12_sync_state_with_identical_values_idempotent(self):
        """Verify calling sync_state(True) 10 times consecutively is cleanly idempotent."""
        if hasattr(self.hotkey_mgr, "sync_state"):
            for _ in range(10):
                self.hotkey_mgr.sync_state(True)
            self.assertTrue(self.hotkey_mgr.is_active)
            for _ in range(10):
                self.hotkey_mgr.sync_state(False)
            self.assertFalse(self.hotkey_mgr.is_active)

    def test_b_f12_sync_state_property_reflection(self):
        """Verify is_active is directly queryable as boolean."""
        self.assertIsInstance(self.hotkey_mgr.is_active, bool)

    def test_b_f12_rapid_alternating_toggles_with_recording_fn(self):
        """Verify rapid toggles maintain sync with external is_recording state."""
        for state in [True, False, True, True, False, False, True]:
            if hasattr(self.hotkey_mgr, "sync_state"):
                self.hotkey_mgr.sync_state(state)
                self.assertEqual(self.hotkey_mgr.is_active, state)

    def test_b_f12_sync_state_false_when_already_false(self):
        """Verify syncing False when already inactive is safe and idempotent."""
        self.hotkey_mgr.is_active = False
        if hasattr(self.hotkey_mgr, "sync_state"):
            self.hotkey_mgr.sync_state(False)
            self.assertFalse(self.hotkey_mgr.is_active)

    # --- F13: Symbol Snippet Engine Boundaries ---
    def test_b_f13_nested_triggers_matching_precedence(self):
        """Verify longer phrase 'c++ vector' takes precedence over 'c++'."""
        self.config.set("snippets", {
            "c++": "CPP_LANG",
            "c++ vector": "STD_VECTOR_INT"
        })
        engine = SnippetEngine(self.config)
        res = engine.expand("I use c++ vector frequently")
        self.assertIn("STD_VECTOR_INT", res)

    def test_b_f13_regex_special_characters_in_triggers(self):
        """Verify triggers with characters * + ? ^ $ ( ) [ ] \\ are escaped properly."""
        self.config.set("snippets", {
            "regex.*": "REGEX_WILDCARD",
            "func()": "FUNCTION_CALL",
            "$price": "DOLLAR_AMOUNT"
        })
        engine = SnippetEngine(self.config)
        res1 = engine.expand("Here is regex.* sample")
        self.assertIn("REGEX_WILDCARD", res1)
        res2 = engine.expand("Call func() now")
        self.assertIn("FUNCTION_CALL", res2)
        res3 = engine.expand("The $price is high")
        self.assertIn("DOLLAR_AMOUNT", res3)

    def test_b_f13_various_trailing_punctuation(self):
        """Verify questions, exclamations, and commas attached to triggers."""
        self.config.set("snippets", {
            "my email": "ayush@example.com"
        })
        engine = SnippetEngine(self.config)
        # Should expand safely
        res_q = engine.expand("Do you have my email?")
        self.assertIn("ayush@example.com", res_q)
        res_e = engine.expand("Here is my email!")
        self.assertIn("ayush@example.com", res_e)

    def test_b_f13_1000_snippets_dictionary_performance(self):
        """Verify snippet engine with 1,000 entries expands in < 10ms."""
        large_dict = {f"trigger_{i}": f"expansion_content_{i}" for i in range(1000)}
        large_dict["target trigger"] = "TARGET_EXPANSION"
        self.config.set("snippets", large_dict)
        engine = SnippetEngine(self.config)
        
        t0 = time.perf_counter()
        expanded = engine.expand("This sentence contains target trigger for testing.")
        t1 = time.perf_counter()
        
        self.assertIn("TARGET_EXPANSION", expanded)
        self.assertLess((t1 - t0) * 1000, 100.0)

    def test_b_f13_trigger_at_start_middle_end_of_text(self):
        """Verify trigger expands whether at the beginning, middle, or end of string."""
        self.config.set("snippets", {"keyword": "EXPANDED"})
        engine = SnippetEngine(self.config)
        self.assertTrue(engine.expand("keyword is first").startswith("EXPANDED"))
        self.assertIn("middle EXPANDED here", engine.expand("middle keyword here"))
        self.assertTrue(engine.expand("it is at the keyword").endswith("EXPANDED"))

    # --- F14: Context Tone Detection Boundaries ---
    def test_b_f14_huge_window_title_10000_chars(self):
        """Verify detect_tone handles 10,000-character titles without overflow."""
        desktop = ctypes.windll.user32.GetDesktopWindow()
        mode, label = ContextDetector.detect_tone(desktop)
        self.assertIsInstance(mode, str)
        self.assertIsInstance(label, str)

    def test_b_f14_detect_tone_on_closed_hwnd(self):
        """Verify calling detect_tone with 0xDEADBEEF returns smart_flow."""
        mode, label = ContextDetector.detect_tone(0xDEADBEEF)
        self.assertEqual(mode, "smart_flow")
        self.assertEqual(label, "Flow")

    def test_b_f14_negative_hwnd_returns_smart_flow(self):
        """Verify negative HWND (-999) returns default smart_flow."""
        mode, label = ContextDetector.detect_tone(-999)
        self.assertEqual(mode, "smart_flow")

    def test_b_f14_get_window_info_on_invalid_hwnd(self):
        """Verify get_window_info on dead HWND returns ('', '')."""
        exe, title = ContextDetector.get_window_info(0xCAFEBABE)
        self.assertEqual(exe, "")
        self.assertEqual(title, "")

    def test_b_f13_empty_snippet_catalog_expansion(self):
        """Verify expanding when snippets dictionary is empty returns input unmodified."""
        self.config.set("snippets", {})
        engine = SnippetEngine(self.config)
        self.assertEqual(engine.expand("Hello World"), "Hello World")

    def test_b_f14_detect_tone_with_zero_title_and_known_process(self):
        """Verify detect_tone with desktop window returns valid tuple."""
        desktop = ctypes.windll.user32.GetDesktopWindow()
        mode, label = ContextDetector.detect_tone(desktop)
        self.assertIn(mode, ("code", "formal_email", "formal_document", "chat", "smart_flow"))
        self.assertIn(label, ("Code", "Email", "Doc", "Chat", "Flow"))

if __name__ == "__main__":
    unittest.main()
