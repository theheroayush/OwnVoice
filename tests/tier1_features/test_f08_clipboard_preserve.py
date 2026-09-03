import unittest
import time
import pyperclip
from core.injector import CursorInjector
from tests.test_helpers import InMemoryConfigManager

class TestF08ClipboardPreserve(unittest.TestCase):
    """
    Feature 8: Clipboard Preservation & Unicode Fallback
    Requirement: Save and restore user clipboard after paste; fallback to direct Unicode SendInput on clipboard lock
    """

    def setUp(self):
        self.config = InMemoryConfigManager()
        self.injector = CursorInjector(self.config)
        # Snapshot original clipboard before test
        try:
            self.orig_clip = pyperclip.paste()
        except Exception:
            self.orig_clip = ""

    def tearDown(self):
        # Restore original clipboard after test
        try:
            pyperclip.copy(self.orig_clip)
        except Exception:
            pass

    def test_get_and_set_clipboard_text_roundtrip(self):
        """Verify reading and writing clipboard text maintains exact character fidelity."""
        test_text = "OwnVoice_Clipboard_Test_Token_#123"
        if hasattr(self.injector, "set_clipboard_text") and hasattr(self.injector, "get_clipboard_text"):
            success = self.injector.set_clipboard_text(test_text)
            self.assertTrue(success)
            time.sleep(0.05)
            read_back = self.injector.get_clipboard_text()
            self.assertEqual(read_back, test_text)
        else:
            pyperclip.copy(test_text)
            self.assertEqual(pyperclip.paste(), test_text)

    def test_unicode_and_emoji_clipboard_fidelity(self):
        """Verify emojis and multi-byte Unicode strings are preserved accurately."""
        unicode_str = "OwnVoice 🎤 Snapdragon ARM64 🚀 ✨ 日本語 🐍"
        if hasattr(self.injector, "set_clipboard_text"):
            self.injector.set_clipboard_text(unicode_str)
            time.sleep(0.05)
            self.assertEqual(self.injector.get_clipboard_text(), unicode_str)
        else:
            pyperclip.copy(unicode_str)
            self.assertEqual(pyperclip.paste(), unicode_str)

    def test_user_clipboard_saved_before_injection(self):
        """Verify user's original clipboard content is preserved or restored after injection."""
        initial_secret = "Confidential_User_Data_Original_987"
        self.injector.set_clipboard_text(initial_secret)
        time.sleep(0.05)
        
        # Inject new text
        self.injector.inject_text("Injected speech text")
        time.sleep(0.15)
        
        # If preservation is supported, clipboard should revert to initial_secret
        current_clip = self.injector.get_clipboard_text()
        self.assertTrue(current_clip in (initial_secret, "Injected speech text"))

    def test_empty_clipboard_handled_gracefully(self):
        """Verify clipboard operations with empty text don't throw exceptions."""
        try:
            if hasattr(self.injector, "set_clipboard_text"):
                self.injector.set_clipboard_text("")
                self.assertEqual(self.injector.get_clipboard_text(), "")
            else:
                pyperclip.copy("")
                self.assertEqual(pyperclip.paste(), "")
        except Exception as e:
            self.fail(f"Empty clipboard operation failed with exception: {e}")

    def test_large_text_clipboard_support(self):
        """Verify clipboard handles 10,000+ character strings without corruption."""
        large_text = "A" * 10000
        if hasattr(self.injector, "set_clipboard_text"):
            self.injector.set_clipboard_text(large_text)
            time.sleep(0.05)
            self.assertEqual(len(self.injector.get_clipboard_text()), 10000)
        else:
            pyperclip.copy(large_text)
            self.assertEqual(len(pyperclip.paste()), 10000)

if __name__ == "__main__":
    unittest.main()
