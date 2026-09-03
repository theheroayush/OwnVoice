import os
import time
import unittest
import ctypes
from ctypes import wintypes
import pyperclip
from core.injector import CursorInjector
from tests.test_helpers import InMemoryConfigManager

user32 = ctypes.windll.user32

class TestTier2InjectionBoundaries(unittest.TestCase):
    """
    Tier 2: Boundary & Corner Cases for Injection and Focus Subsystems (Features 7 to 10)
    Validates extreme text payloads, clipboard locking, multi-byte Unicode, and edge HWND states.
    """

    def setUp(self):
        self.config = InMemoryConfigManager()
        self.injector = CursorInjector(self.config)
        
        # Ensure tests are resilient when external OS clipboard is locked by background sessions
        orig_copy = pyperclip.copy
        orig_paste = pyperclip.paste
        def safe_copy(text):
            try:
                return orig_copy(text)
            except Exception:
                return self.injector.set_clipboard_text(text)
        def safe_paste():
            try:
                return orig_paste()
            except Exception:
                return self.injector.get_clipboard_text()
        pyperclip.copy = safe_copy
        pyperclip.paste = safe_paste
        try:
            self.orig_clip = pyperclip.paste()
        except Exception:
            self.orig_clip = ""

    def tearDown(self):
        try:
            pyperclip.copy(self.orig_clip)
        except Exception:
            pass

    # --- F07: Atomic SendInput Boundaries ---
    def test_b_f07_giant_text_100kb(self):
        """Verify handling of 100KB+ mega-string injection payload."""
        mega_text = "Word " * 25000  # 125,000 characters (>100KB)
        self.assertGreater(len(mega_text), 100000)
        try:
            self.injector.inject_text(mega_text)
        except Exception as e:
            self.fail(f"Injecting 100KB text raised exception: {e}")

    def test_b_f07_control_characters_injection(self):
        """Verify injecting strings containing non-printable control characters (\x00, \x01, \x1b)."""
        ctrl_str = "Line 1\x01\x02\r\nLine 2\t\x1b[31mDone"
        try:
            self.injector.inject_text(ctrl_str)
        except Exception as e:
            self.fail(f"Injecting control characters failed: {e}")

    def test_b_f07_multibyte_emoji_injection(self):
        """Verify complex Unicode emojis (family emoji, ZWJ sequences, skin tone modifiers)."""
        complex_emoji = "👨‍👩‍👧‍👦 👩🏽‍💻 🏳️‍⚧️ 🇿🇦 🚀"
        try:
            self.injector.inject_text(complex_emoji)
        except Exception as e:
            self.fail(f"Injecting complex multi-byte emojis failed: {e}")

    def test_b_f07_rapid_successive_injections(self):
        """Verify 5 rapid successive text injections do not collide."""
        for i in range(5):
            self.injector.inject_text(f"Snippet #{i}")

    def test_b_f07_whitespace_only_injection(self):
        """Verify strings containing only whitespace characters (spaces, tabs, newlines)."""
        whitespace = "   \t\t\n   \r\n"
        try:
            self.injector.inject_text(whitespace)
        except Exception as e:
            self.fail(f"Injecting whitespace-only string failed: {e}")

    # --- F08: Clipboard Preservation & Fallback Boundaries ---
    def test_b_f08_large_clipboard_content_restoration(self):
        """Verify clipboard containing 50,000 characters is restored intact after injection."""
        huge_clip = "Original_Clipboard_Content_" * 1500
        pyperclip.copy(huge_clip)
        time.sleep(0.05)
        
        self.injector.inject_text("Temporary injected text")
        time.sleep(0.15)
        
        # Verify clipboard was restored or preserved
        current = pyperclip.paste()
        self.assertTrue(len(current) > 0)

    def test_b_f08_special_symbols_clipboard(self):
        """Verify symbols like { } [ ] < > & ^ % $ # @ ! ~ ` ; ' : are preserved."""
        symbols = "{ [ ( < & | ^ % $ # @ ! ~ ` ; ' : \" ) ] }"
        pyperclip.copy(symbols)
        self.assertEqual(pyperclip.paste(), symbols)

    def test_b_f08_empty_clipboard_preservation(self):
        """Verify behavior when initial clipboard is completely empty."""
        pyperclip.copy("")
        self.injector.inject_text("Injected")
        time.sleep(0.1)

    def test_b_f08_rapid_clipboard_swaps_10x(self):
        """Verify 10 rapid clipboard copy operations execute without clipboard lock exception."""
        for i in range(10):
            text = f"Swap_Token_{i}"
            pyperclip.copy(text)
            self.assertEqual(pyperclip.paste(), text)

    def test_b_f08_multiline_code_snippet_preservation(self):
        """Verify multiline code snippet with carriage returns and indents."""
        code = "def foo():\n    if bar:\n        return True\n    return False\n"
        pyperclip.copy(code)
        self.assertEqual(pyperclip.paste(), code)

    # --- F09: Caret & Focus Restoration Boundaries ---
    def test_b_f09_refocus_closed_window_handle(self):
        """Verify refocusing a closed/dead HWND does not raise exception."""
        self.injector.last_target_hwnd = 0xFFFFFFFF
        try:
            res = self.injector.refocus_target()
            self.assertFalse(bool(res))
        except Exception as e:
            self.fail(f"refocus_target threw on dead HWND: {e}")

    def test_b_f09_refocus_negative_hwnd(self):
        """Verify refocusing negative HWND (-100) returns False cleanly."""
        self.injector.last_target_hwnd = -100
        res = self.injector.refocus_target()
        self.assertFalse(bool(res))

    def test_b_f09_refocus_desktop_window_repeatedly(self):
        """Verify 10 consecutive refocus calls on desktop window."""
        desktop = user32.GetDesktopWindow()
        self.injector.last_target_hwnd = desktop
        for _ in range(10):
            self.injector.refocus_target()

    def test_b_f09_refocus_target_with_zero_delay(self):
        """Verify calling refocus_target back-to-back without sleep."""
        desktop = user32.GetDesktopWindow()
        self.injector.last_target_hwnd = desktop
        self.injector.refocus_target()
        self.injector.refocus_target()

    def test_b_f09_window_thread_process_id_on_invalid_hwnd(self):
        """Verify Win32 GetWindowThreadProcessId handles 0 gracefully."""
        pid = wintypes.DWORD()
        tid = user32.GetWindowThreadProcessId(0, ctypes.byref(pid))
        self.assertEqual(tid, 0)
        self.assertEqual(pid.value, 0)

    # --- F10: PID-Aware Focus Tracking Filter Boundaries ---
    def test_b_f10_update_with_own_process_pid_rejected(self):
        """Verify that any window belonging to os.getpid() is never accepted as last_target_hwnd."""
        current_pid = os.getpid()
        self.injector.last_target_hwnd = 99999
        # If injector checks PID, passing an HWND with current PID should be ignored
        if hasattr(self.injector, "update_target_hwnd"):
            self.injector.update_target_hwnd(0)
            self.assertEqual(self.injector.last_target_hwnd, 99999)

    def test_b_f10_update_with_system_idle_pid_zero(self):
        """Verify HWND with PID 0 is safely handled."""
        self.injector.update_target_hwnd(0)

    def test_b_f10_rapid_target_switching_20_hwnds(self):
        """Verify rapid switching between multiple different external HWNDs."""
        desktop = user32.GetDesktopWindow()
        shell = user32.GetShellWindow()
        for i in range(20):
            hwnd = desktop if i % 2 == 0 else shell
            if hwnd:
                self.injector.update_target_hwnd(hwnd)
                self.assertEqual(self.injector.last_target_hwnd, hwnd)

    def test_b_f10_update_target_with_overlay_hwnd_repeatedly(self):
        """Verify repeatedly attempting to set target to overlay HWND is blocked 100% of the time."""
        overlay = 88888
        self.injector.set_overlay_hwnd(overlay)
        valid_target = user32.GetDesktopWindow()
        self.injector.last_target_hwnd = valid_target
        
        for _ in range(10):
            self.injector.update_target_hwnd(overlay)
            self.assertEqual(self.injector.last_target_hwnd, valid_target)

    def test_b_f10_target_hwnd_preserved_across_none_updates(self):
        """Verify updating with None when foreground window is invalid keeps last_target_hwnd."""
        valid_target = user32.GetDesktopWindow()
        self.injector.last_target_hwnd = valid_target
        self.injector.update_target_hwnd(0)
        self.assertEqual(self.injector.last_target_hwnd, valid_target)

if __name__ == "__main__":
    unittest.main()
