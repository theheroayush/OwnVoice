import unittest
import ctypes
from core.injector import CursorInjector
from tests.test_helpers import InMemoryConfigManager

user32 = ctypes.windll.user32

class TestF09FocusRestoration(unittest.TestCase):
    """
    Feature 9: Caret & Search Box Focus Restoration
    Requirement: Remove SetFocus from container HWND; use AttachThreadInput + SetForegroundWindow to keep child caret
    """

    def setUp(self):
        self.config = InMemoryConfigManager()
        self.injector = CursorInjector(self.config)

    def test_refocus_target_with_none_hwnd(self):
        """Verify refocus_target returns False or handles None last_target_hwnd gracefully."""
        self.injector.last_target_hwnd = None
        res = self.injector.refocus_target()
        self.assertFalse(bool(res))

    def test_refocus_target_with_zero_hwnd(self):
        """Verify refocus_target returns False or handles 0 HWND gracefully."""
        self.injector.last_target_hwnd = 0
        res = self.injector.refocus_target()
        self.assertFalse(bool(res))

    def test_refocus_target_with_invalid_dead_hwnd(self):
        """Verify refocus_target handles an invalid non-existent HWND without raising unhandled exceptions."""
        self.injector.last_target_hwnd = 0xDEADBEEF
        try:
            res = self.injector.refocus_target()
            self.assertFalse(bool(res))
        except Exception as e:
            self.fail(f"refocus_target raised unexpected exception on dead HWND: {e}")

    def test_refocus_desktop_window_does_not_crash(self):
        """Verify refocusing Desktop HWND operates cleanly."""
        desktop_hwnd = user32.GetDesktopWindow()
        self.injector.last_target_hwnd = desktop_hwnd
        try:
            self.injector.refocus_target()
        except Exception as e:
            self.fail(f"refocus_target failed on valid Desktop window: {e}")

    def test_is_window_validation(self):
        """Verify user32.IsWindow correctly differentiates valid vs invalid window handles."""
        desktop_hwnd = user32.GetDesktopWindow()
        self.assertTrue(bool(user32.IsWindow(desktop_hwnd)))
        self.assertFalse(bool(user32.IsWindow(0)))
        self.assertFalse(bool(user32.IsWindow(99999999)))

if __name__ == "__main__":
    unittest.main()
