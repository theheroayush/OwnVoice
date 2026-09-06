import unittest
from unittest.mock import patch, MagicMock
import sys

from core.injector import CursorInjector
from core.context_detector import ContextDetector
from core.hotkey_manager import HotkeyManager
from core.bridge_server import BridgeServer

class TestCrossPlatformDesktop(unittest.TestCase):
    """
    Validates that OwnVoice core components execute cleanly across
    Windows, macOS, and Linux platforms without missing DLL or type errors.
    """

    def test_injector_instantiation_windows(self):
        with patch("sys.platform", "win32"):
            injector = CursorInjector()
            self.assertIsNotNone(injector)
            self.assertTrue(injector.refocus_target() in (True, False))

    def test_injector_instantiation_macos(self):
        with patch("sys.platform", "darwin"), patch("core.injector.IS_MACOS", True), patch("core.injector.IS_WINDOWS", False):
            injector = CursorInjector()
            self.assertIsNotNone(injector)
            self.assertTrue(injector.refocus_target())
            self.assertEqual(injector.get_clipboard_text(), "")
            # Verify set_clipboard_text fallback
            with patch("core.injector._mac_set_clipboard", return_value=True):
                self.assertTrue(injector.set_clipboard_text("Hello Mac"))

    def test_injector_instantiation_linux(self):
        with patch("sys.platform", "linux"), patch("core.injector.IS_LINUX", True), patch("core.injector.IS_WINDOWS", False):
            injector = CursorInjector()
            self.assertIsNotNone(injector)
            self.assertTrue(injector.refocus_target())
            self.assertEqual(injector.get_clipboard_text(), "")
            # Verify set_clipboard_text fallback
            with patch("core.injector._linux_set_clipboard", return_value=True):
                self.assertTrue(injector.set_clipboard_text("Hello Linux"))

    def test_context_detector_macos(self):
        with patch("sys.platform", "darwin"), patch("core.context_detector.IS_MACOS", True), patch("core.context_detector.IS_WINDOWS", False):
            with patch("core.context_detector._mac_get_active_app", return_value=("code", "main.py - OwnVoice")):
                mode, label = ContextDetector.detect_tone(1234)
                self.assertEqual(mode, "code")
                self.assertEqual(label, "Code")

            with patch("core.context_detector._mac_get_active_app", return_value=("slack", "General")):
                mode, label = ContextDetector.detect_tone(1234)
                self.assertEqual(mode, "chat")
                self.assertEqual(label, "Chat")

    def test_context_detector_linux(self):
        with patch("sys.platform", "linux"), patch("core.context_detector.IS_LINUX", True), patch("core.context_detector.IS_WINDOWS", False):
            with patch("core.context_detector._linux_get_active_app", return_value=("gnome-terminal", "")):
                mode, label = ContextDetector.detect_tone(1234)
                self.assertEqual(mode, "code")
                self.assertEqual(label, "Code")

    def test_hotkey_manager_listener_args_non_windows(self):
        with patch("sys.platform", "darwin"), patch("pynput.keyboard.Listener") as mock_listener:
            hm = HotkeyManager(on_start=lambda: None, on_stop=lambda: None)
            hm.start()
            self.assertTrue(mock_listener.called)
            kwargs = mock_listener.call_args[1]
            self.assertNotIn("win32_event_filter", kwargs)
            hm.stop()

    def test_bridge_server_runs_cross_platform(self):
        server = BridgeServer(port=8777, on_inject=lambda t: True)
        self.assertIsNotNone(server.local_ip)
        uri = server.get_pairing_uri()
        self.assertTrue(uri.startswith("ownvoice://pair?"))
        qr_img = server.generate_qr_image(120)
        self.assertIsNotNone(qr_img)
        self.assertEqual(len(server.current_pin), 6)

if __name__ == "__main__":
    unittest.main()
