import unittest
import time
import ctypes
from unittest.mock import MagicMock, patch
from ctypes import wintypes
from core.injector import CursorInjector, VK_MENU, VK_ESCAPE
from core.hotkey_manager import HotkeyManager, VK_F8
from core.audio_recorder import AudioRecorder
from app import ensure_default_desktop
from tests.test_helpers import InMemoryConfigManager

class TestBackendCoreHardening(unittest.TestCase):
    def setUp(self):
        self.config = InMemoryConfigManager({
            'hotkey': 'f8',
            'hotkey_mode': 'toggle'
        })
        self.injector = CursorInjector(self.config)

    def test_set_clipboard_text_failure_sets_flag_and_returns_false(self):
        with patch('core.injector.user32.OpenClipboard', return_value=0):
            res = self.injector.set_clipboard_text('Test Payload')
            self.assertFalse(res)
            self.assertFalse(getattr(self.injector, '_last_clip_op_real_win32', True))

    def test_refocus_target_skips_attachment_when_already_foreground(self):
        target_hwnd = 98765
        self.injector.last_target_hwnd = target_hwnd

        with patch('core.injector.user32.IsWindow', return_value=1), \
             patch('core.injector.user32.GetForegroundWindow', return_value=target_hwnd), \
             patch('core.injector.user32.AttachThreadInput') as mock_attach, \
             patch('core.injector.user32.keybd_event') as mock_keybd:

            res = self.injector.refocus_target()
            self.assertTrue(res)
            mock_attach.assert_not_called()
            mock_keybd.assert_not_called()

    def test_refocus_target_pulses_escape_after_menu(self):
        target_hwnd = 98765
        fg_hwnd = 11111
        self.injector.last_target_hwnd = target_hwnd

        with patch('core.injector.user32.IsWindow', return_value=1), \
             patch('core.injector.user32.GetForegroundWindow', return_value=fg_hwnd), \
             patch('core.injector.user32.GetWindowThreadProcessId', return_value=100), \
             patch('core.injector.kernel32.GetCurrentThreadId', return_value=200), \
             patch('core.injector.user32.AttachThreadInput'), \
             patch('core.injector.user32.SetForegroundWindow'), \
             patch('core.injector.user32.BringWindowToTop'), \
             patch('core.injector.user32.IsIconic', return_value=0), \
             patch('core.injector.user32.keybd_event') as mock_keybd:

            res = self.injector.refocus_target()
            self.assertTrue(res)

            pulsed_keys = [call_args[0][0] for call_args in mock_keybd.call_args_list]
            self.assertIn(VK_MENU, pulsed_keys)
            self.assertIn(VK_ESCAPE, pulsed_keys)

    def test_inject_text_falls_back_to_unicode_string_on_clipboard_failure(self):
        with patch.object(self.injector, 'refocus_target', return_value=True), \
             patch.object(self.injector, 'get_clipboard_text', return_value=''), \
             patch.object(self.injector, 'set_clipboard_text', return_value=False), \
             patch.object(self.injector, '_send_unicode_string', return_value=True) as mock_unicode:

            res = self.injector.inject_text('Fallback String')
            self.assertTrue(res)
            mock_unicode.assert_called_once_with('Fallback String')

    def test_inject_text_250ms_post_paste_delay(self):
        with patch.object(self.injector, 'refocus_target', return_value=True), \
             patch.object(self.injector, 'get_clipboard_text', return_value='Original Clip'), \
             patch.object(self.injector, 'set_clipboard_text', return_value=True), \
             patch.object(self.injector, '_send_ctrl_v', return_value=True), \
             patch('core.injector.time.sleep') as mock_sleep:

            self.injector._last_clip_op_real_win32 = True
            res = self.injector.inject_text('New Text')
            self.assertTrue(res)

            sleep_args = [args[0] for args, _ in mock_sleep.call_args_list]
            self.assertIn(0.25, sleep_args)

    def test_win32_event_filter_returns_false_cleanly_without_raising(self):
        mgr = HotkeyManager(lambda: None, lambda: None, config_manager=self.config)
        mgr.listener = MagicMock()
        mgr.listener.suppress_event.side_effect = Exception('SuppressException must never be called!')

        mock_data = MagicMock()
        mock_data.vkCode = VK_F8

        ret = mgr._win32_event_filter(0x0100, mock_data)
        self.assertFalse(ret)

        ret_up = mgr._win32_event_filter(0x0101, mock_data)
        self.assertFalse(ret_up)

    def test_win32_event_filter_swallows_inner_exceptions_gracefully(self):
        mgr = HotkeyManager(lambda: None, lambda: None, config_manager=self.config)
        class BrokenData:
            @property
            def vkCode(self):
                raise RuntimeError('Fault')

        try:
            ret = mgr._win32_event_filter(0x0100, BrokenData())
            self.assertTrue(ret)
        except Exception as e:
            self.fail(f'Raised unexpected exception: {e}')

    def test_cleanly_separate_debounce_toggle_vs_push_to_talk(self):
        starts = 0
        stops = 0
        def on_start():
            nonlocal starts
            starts += 1
        def on_stop():
            nonlocal stops
            stops += 1

        mgr = HotkeyManager(on_start, on_stop, config_manager=self.config)
        self.assertTrue(hasattr(mgr, 'last_toggle_time'))
        self.assertTrue(hasattr(mgr, 'last_ptt_time'))

        mgr._trigger_toggle()
        time.sleep(0.02)
        t_toggle = mgr.last_toggle_time
        self.assertGreater(t_toggle, 0.0)
        self.assertEqual(mgr.last_ptt_time, 0.0)

        mgr._trigger_toggle()
        self.assertEqual(mgr.last_toggle_time, t_toggle)

        self.config.set('hotkey_mode', 'push_to_talk')
        mgr.is_active = False
        mgr._trigger_start()
        time.sleep(0.02)
        self.assertGreater(mgr.last_ptt_time, 0.0)
        self.assertEqual(mgr.last_toggle_time, t_toggle)

    def test_find_device_by_signature_resolves_wasapi_or_hardware(self):
        idx = AudioRecorder.find_device_by_signature(['Qualcomm Aqstic', 'WASAPI'])
        if idx is not None:
            self.assertIsInstance(idx, int)
            self.assertGreaterEqual(idx, 0)
            import sounddevice as sd
            dev = sd.query_devices(idx)
            self.assertGreater(dev['max_input_channels'], 0)

    def test_resolve_device_index_drift_protection(self):
        recorder = AudioRecorder(self.config)
        resolved = recorder.resolve_device_index(preferred_index=99999)
        self.assertIsNotNone(resolved)
        import sounddevice as sd
        devs = sd.query_devices()
        self.assertLess(resolved, len(devs))
        self.assertGreater(devs[resolved]['max_input_channels'], 0)

    def test_ensure_default_desktop_executes_without_exception(self):
        res = ensure_default_desktop()
        self.assertIsInstance(res, bool)
        self.assertTrue(res)

if __name__ == '__main__':
    unittest.main()
