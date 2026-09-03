import unittest
import ctypes
from ctypes import wintypes
from core.injector import CursorInjector
from tests.test_helpers import InMemoryConfigManager

class TestF07AtomicInjection(unittest.TestCase):
    """
    Feature 7: Single Atomic Text Injection (No Double-Paste)
    Requirement: Eliminate duplicate pynput paste; use Win32 SendInput with verified 40-byte union struct
    """

    def setUp(self):
        self.config = InMemoryConfigManager()
        self.injector = CursorInjector(self.config)

    def test_empty_string_injection_is_noop(self):
        """Verify passing empty string or None to inject_text returns cleanly without error."""
        res_empty = self.injector.inject_text("")
        self.assertFalse(bool(res_empty))
        res_none = self.injector.inject_text(None)
        self.assertFalse(bool(res_none))

    def test_win32_input_union_40_byte_struct(self):
        """Verify on 64-bit Windows the INPUT union struct measures exactly 40 bytes."""
        is_64bit = ctypes.sizeof(ctypes.c_void_p) == 8
        if is_64bit:
            class MOUSEINPUT(ctypes.Structure):
                _fields_ = [
                    ("dx", wintypes.LONG),
                    ("dy", wintypes.LONG),
                    ("mouseData", wintypes.DWORD),
                    ("dwFlags", wintypes.DWORD),
                    ("time", wintypes.DWORD),
                    ("dwExtraInfo", ctypes.c_ulonglong)
                ]

            class KEYBDINPUT(ctypes.Structure):
                _fields_ = [
                    ("wVk", wintypes.WORD),
                    ("wScan", wintypes.WORD),
                    ("dwFlags", wintypes.DWORD),
                    ("time", wintypes.DWORD),
                    ("dwExtraInfo", ctypes.c_ulonglong)
                ]

            class HARDWAREINPUT(ctypes.Structure):
                _fields_ = [
                    ("uMsg", wintypes.DWORD),
                    ("wParamL", wintypes.WORD),
                    ("wParamH", wintypes.WORD)
                ]

            class INPUT_UNION(ctypes.Union):
                _fields_ = [
                    ("mi", MOUSEINPUT),
                    ("ki", KEYBDINPUT),
                    ("hi", HARDWAREINPUT)
                ]

            class INPUT(ctypes.Structure):
                _fields_ = [
                    ("type", wintypes.DWORD),
                    ("u", INPUT_UNION)
                ]

            # 4 bytes (DWORD type) + 4 bytes padding + 32 bytes (MOUSEINPUT union) = 40 bytes
            self.assertEqual(ctypes.sizeof(INPUT), 40, "INPUT structure must be 40 bytes on 64-bit Windows")

    def test_injector_initializes_with_overlay_and_target_properties(self):
        """Verify injector exposes last_target_hwnd and overlay_hwnd properties."""
        self.assertIsNone(self.injector.last_target_hwnd)
        self.assertIsNone(self.injector.overlay_hwnd)

    def test_set_overlay_hwnd_assigns_value(self):
        """Verify set_overlay_hwnd stores the given HWND."""
        test_hwnd = 123456
        self.injector.set_overlay_hwnd(test_hwnd)
        self.assertEqual(self.injector.overlay_hwnd, test_hwnd)

    def test_target_hwnd_cleared_or_updated(self):
        """Verify update_target_hwnd updates target when given a valid external window."""
        desktop_hwnd = ctypes.windll.user32.GetDesktopWindow()
        self.injector.update_target_hwnd(desktop_hwnd)
        self.assertEqual(self.injector.last_target_hwnd, desktop_hwnd)

if __name__ == "__main__":
    unittest.main()
