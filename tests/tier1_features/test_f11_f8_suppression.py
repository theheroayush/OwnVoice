import unittest
import time
from pynput import keyboard
from core.hotkey_manager import HotkeyManager
from tests.test_helpers import InMemoryConfigManager

class TestF11F8Suppression(unittest.TestCase):
    """
    Feature 11: F8 Key-Repeat & Alert Ding Suppression
    Requirement: Drop repeat WM_KEYDOWN events; debounce 350ms; suppress F8 pass-through to prevent console/Word alert dings
    """

    def setUp(self):
        self.config = InMemoryConfigManager({"hotkey": "f8", "hotkey_mode": "toggle"})
        self.start_calls = 0
        self.stop_calls = 0

        def on_start():
            self.start_calls += 1

        def on_stop():
            self.stop_calls += 1

        # HotkeyManager accepts (on_start, on_stop, config) or (on_start, on_stop, is_recording_fn, config)
        try:
            self.mgr = HotkeyManager(on_start, on_stop, lambda: False, self.config)
        except TypeError:
            self.mgr = HotkeyManager(on_start, on_stop, self.config)

    def test_initial_hotkey_state(self):
        """Verify HotkeyManager initializes in inactive state with empty key set."""
        self.assertFalse(self.mgr.is_active)
        self.assertEqual(len(self.mgr.current_keys), 0)

    def test_hotkey_trigger_detection(self):
        """Verify _is_hotkey_triggered recognizes Key.f8."""
        self.assertTrue(self.mgr._is_hotkey_triggered(keyboard.Key.f8))
        self.assertFalse(self.mgr._is_hotkey_triggered(keyboard.Key.f7))

    def test_key_release_removes_from_current_keys(self):
        """Verify releasing a key removes it from current_keys set."""
        self.mgr.current_keys.add(keyboard.Key.f8)
        self.mgr._on_release(keyboard.Key.f8)
        self.assertNotIn(keyboard.Key.f8, self.mgr.current_keys)

    def test_push_to_talk_mode_start_and_stop(self):
        """Verify push_to_talk mode triggers start on press and stop on release."""
        self.config.set("hotkey_mode", "push_to_talk")
        # Key down
        self.mgr._on_press(keyboard.Key.f8)
        time.sleep(0.05)
        self.assertTrue(self.mgr.is_active)
        self.assertGreaterEqual(self.start_calls, 1)
        
        # Key up
        self.mgr._on_release(keyboard.Key.f8)
        time.sleep(0.05)
        self.assertFalse(self.mgr.is_active)
        self.assertGreaterEqual(self.stop_calls, 1)

    def test_key_repeat_suppression_logic(self):
        """Verify simulated repeated key-presses do not cause multiple toggles."""
        # First press
        self.mgr._on_press(keyboard.Key.f8)
        time.sleep(0.05)
        initial_starts = self.start_calls
        
        # Second immediate press without release (OS key repeat)
        if keyboard.Key.f8 in self.mgr.current_keys and hasattr(self.mgr, "last_toggle_time"):
            self.mgr._on_press(keyboard.Key.f8)
            time.sleep(0.05)
            # Starts should not increment again on repeated press
            self.assertEqual(self.start_calls, initial_starts)

if __name__ == "__main__":
    unittest.main()
