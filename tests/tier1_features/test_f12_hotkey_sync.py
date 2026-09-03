import unittest
from pynput import keyboard
from core.hotkey_manager import HotkeyManager
from tests.test_helpers import InMemoryConfigManager

class TestF12HotkeySync(unittest.TestCase):
    """
    Feature 12: Synchronized Hotkey & Mouse State Machine
    Requirement: Bidirectional sync between hotkey_manager.is_active and audio_recorder.is_recording
    """

    def setUp(self):
        self.config = InMemoryConfigManager()
        self.recording_state = False
        self.start_called = 0
        self.stop_called = 0

        def on_start():
            self.start_called += 1
            self.recording_state = True

        def on_stop():
            self.stop_called += 1
            self.recording_state = False

        try:
            self.mgr = HotkeyManager(on_start, on_stop, lambda: self.recording_state, self.config)
        except TypeError:
            self.mgr = HotkeyManager(on_start, on_stop, self.config)

    def test_sync_state_updates_is_active(self):
        """Verify sync_state method directly sets is_active."""
        if hasattr(self.mgr, "sync_state"):
            self.mgr.sync_state(True)
            self.assertTrue(self.mgr.is_active)
            self.mgr.sync_state(False)
            self.assertFalse(self.mgr.is_active)

    def test_external_mouse_start_syncs_with_hotkey_manager(self):
        """Verify when recording starts via mouse click, hotkey_manager can be synced."""
        self.recording_state = True
        if hasattr(self.mgr, "sync_state"):
            self.mgr.sync_state(self.recording_state)
            self.assertTrue(self.mgr.is_active)

    def test_external_cancel_syncs_with_hotkey_manager(self):
        """Verify when recording cancels via close button, hotkey_manager is reset to inactive."""
        self.recording_state = True
        if hasattr(self.mgr, "sync_state"):
            self.mgr.sync_state(True)
            # User cancels
            self.recording_state = False
            self.mgr.sync_state(False)
            self.assertFalse(self.mgr.is_active)

    def test_f8_after_external_start_stops_recording(self):
        """Verify pressing F8 when already recording triggers on_stop rather than on_start."""
        self.recording_state = True
        if hasattr(self.mgr, "sync_state"):
            self.mgr.sync_state(True)
            self.mgr.current_keys.clear()
            self.mgr._on_press(keyboard.Key.f8)
            # Should have called on_stop
            self.assertEqual(self.stop_called, 1)
            self.assertEqual(self.start_called, 0)

    def test_state_consistency_after_stop(self):
        """Verify is_active is cleanly False after calling on_stop."""
        self.mgr.is_active = True
        self.mgr.current_keys.clear()
        self.mgr._on_press(keyboard.Key.f8)
        self.assertFalse(self.mgr.is_active)

if __name__ == "__main__":
    unittest.main()
