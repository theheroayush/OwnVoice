import os
import unittest
import ctypes
from ctypes import wintypes
from core.injector import CursorInjector
from tests.test_helpers import InMemoryConfigManager

user32 = ctypes.windll.user32

class TestF10PidFocusFilter(unittest.TestCase):
    """
    Feature 10: PID-Aware Focus Tracking Filter
    Requirement: Discard os.getpid() HWNDs so clicking capsule or settings never steals target HWND
    """

    def setUp(self):
        self.config = InMemoryConfigManager()
        self.injector = CursorInjector(self.config)

    def test_zero_hwnd_is_ignored(self):
        """Verify passing 0 does not update last_target_hwnd."""
        self.injector.last_target_hwnd = 12345
        self.injector.update_target_hwnd(0)
        self.assertEqual(self.injector.last_target_hwnd, 12345)

    def test_overlay_hwnd_is_filtered_out(self):
        """Verify passing overlay_hwnd does not update last_target_hwnd."""
        overlay_hwnd = 54321
        self.injector.set_overlay_hwnd(overlay_hwnd)
        self.injector.last_target_hwnd = 99999
        self.injector.update_target_hwnd(overlay_hwnd)
        self.assertEqual(self.injector.last_target_hwnd, 99999)

    def test_own_process_id_detection(self):
        """Verify GetWindowThreadProcessId correctly retrieves the current process ID."""
        current_pid = os.getpid()
        # Find any window owned by this process (e.g. if one exists) or verify API mechanics
        pid_var = wintypes.DWORD()
        desktop_hwnd = user32.GetDesktopWindow()
        user32.GetWindowThreadProcessId(desktop_hwnd, ctypes.byref(pid_var))
        self.assertGreater(pid_var.value, 0)
        # Desktop PID should be Explorer/System, definitely not our python process
        self.assertNotEqual(pid_var.value, current_pid)

    def test_external_window_is_accepted(self):
        """Verify an external window (such as the desktop window) is accepted as target."""
        desktop_hwnd = user32.GetDesktopWindow()
        self.injector.update_target_hwnd(desktop_hwnd)
        self.assertEqual(self.injector.last_target_hwnd, desktop_hwnd)

    def test_target_hwnd_retained_on_invalid_updates(self):
        """Verify previously established valid target is retained after subsequent invalid attempts."""
        desktop_hwnd = user32.GetDesktopWindow()
        self.injector.update_target_hwnd(desktop_hwnd)
        # Attempt update with 0
        self.injector.update_target_hwnd(0)
        self.assertEqual(self.injector.last_target_hwnd, desktop_hwnd)
        # Attempt update with overlay
        if self.injector.overlay_hwnd:
            self.injector.update_target_hwnd(self.injector.overlay_hwnd)
            self.assertEqual(self.injector.last_target_hwnd, desktop_hwnd)

if __name__ == "__main__":
    unittest.main()
