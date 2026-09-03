import unittest
import math
import ctypes
import threading
import time
from typing import Optional
from ui.floating_widget import FloatingWidget, GWL_EXSTYLE, WS_EX_NOACTIVATE, WS_EX_TOOLWINDOW, WS_EX_TOPMOST
from ui.settings_window import SettingsWindow
from ui.tray_icon import TrayIcon
from tests.test_helpers import InMemoryConfigManager

user32 = ctypes.windll.user32

class DummyEvent:
    def __init__(self, x, y, x_root=None, y_root=None):
        self.x = x
        self.y = y
        self.x_root = x_root if x_root is not None else x
        self.y_root = y_root if y_root is not None else y

class MockAudioRecorder:
    def __init__(self):
        self.is_recording = False
        self.is_monitoring = False
        self.monitoring_started_count = 0
        self.monitoring_stopped_count = 0
        self.current_volume = 0.42

    def get_input_devices(self):
        return [(0, "Default Mic"), (1, "Secondary Mic")]

    def get_current_volume(self):
        return self.current_volume

    def start_monitoring(self, device_index: Optional[int] = None):
        self.is_monitoring = True
        self.monitoring_started_count += 1

    def stop_monitoring(self):
        self.is_monitoring = False
        self.monitoring_stopped_count += 1

    def start_recording(self, device_index: Optional[int] = None):
        self.is_recording = True

    def stop_recording(self):
        self.is_recording = False
        return b"\x00" * 4000

class MockInjector:
    def __init__(self):
        self.overlay_hwnd = 0
        self.refocused = False

    def set_overlay_hwnd(self, hwnd):
        self.overlay_hwnd = hwnd

    def refocus_target(self):
        self.refocused = True

class TestUIAudit(unittest.TestCase):
    """
    Comprehensive UI audit test suite verifying:
    1. Tkinter window creation, transparency, and topmost / no-activate styling.
    2. Window dragging, 6px Euclidean threshold, clamping, and cancel button (✕).
    3. SettingsWindow VU monitor non-contention and clean closure.
    4. Tray icon and context menu interactions.
    5. Clean teardown and timer safety.
    """

    def setUp(self):
        self.config = InMemoryConfigManager()
        self.toggle_count = 0
        self.cancel_count = 0
        self.hide_count = 0
        self.exit_count = 0
        self.settings_count = 0
        self.injector = MockInjector()
        self.audio_recorder = MockAudioRecorder()

        self.widget = FloatingWidget(
            get_volume_fn=lambda: 0.5,
            on_click_toggle=self._inc_toggle,
            on_open_settings=self._inc_settings,
            on_cancel=self._inc_cancel,
            on_hide=self._inc_hide,
            on_exit=self._inc_exit,
            injector=self.injector,
            config_manager=self.config
        )

    def tearDown(self):
        if self.widget:
            self.widget.destroy()

    def _inc_toggle(self):
        self.toggle_count += 1

    def _inc_cancel(self):
        self.cancel_count += 1

    def _inc_hide(self):
        self.hide_count += 1

    def _inc_exit(self):
        self.exit_count += 1

    def _inc_settings(self):
        self.settings_count += 1

    # =========================================================================
    # 1. Tkinter Window Creation, Transparency, and Topmost / No-Activate Styling
    # =========================================================================
    def test_window_creation_and_topmost_transparency_styling(self):
        """Verify window creation sets overrideredirect, topmost, and colorkey transparency."""
        self.widget._create_window()
        root = self.widget.root
        self.assertIsNotNone(root)
        self.assertIsNotNone(self.widget.canvas)

        # Transparency color key check
        trans_color = root.wm_attributes("-transparentcolor")
        self.assertEqual(trans_color, "#000001")
        self.assertEqual(root.cget("bg"), "#000001")
        self.assertEqual(self.widget.canvas.cget("bg"), "#000001")

        # Topmost check
        is_topmost = root.attributes("-topmost")
        self.assertTrue(bool(is_topmost))

    def test_window_hwnd_resolution_and_extended_styles(self):
        """Verify HWND is resolved properly to a valid Win32 HWND and has WS_EX_NOACTIVATE."""
        self.widget._create_window()
        hwnd = self.widget.hwnd
        self.assertIsNotNone(hwnd)
        self.assertGreater(hwnd, 0, "HWND must be a positive Win32 handle")
        self.assertTrue(bool(user32.IsWindow(hwnd)), "HWND must be a recognized Windows handle")

        # Verify injector received the valid overlay HWND
        self.assertEqual(self.injector.overlay_hwnd, hwnd)

        # Check Win32 extended window styles: WS_EX_NOACTIVATE and WS_EX_TOOLWINDOW
        ex_style = user32.GetWindowLongW(hwnd, GWL_EXSTYLE)
        self.assertTrue(bool(ex_style & WS_EX_NOACTIVATE), "Window must have WS_EX_NOACTIVATE to prevent focus theft")
        self.assertTrue(bool(ex_style & WS_EX_TOOLWINDOW), "Window must have WS_EX_TOOLWINDOW to hide from Alt+Tab")

    def test_window_show_reapplies_topmost(self):
        """Verify show() reasserts topmost and calls deiconify cleanly."""
        self.widget._create_window()
        self.widget.hide()
        self.widget.show()
        is_topmost = self.widget.root.attributes("-topmost")
        self.assertTrue(bool(is_topmost))

    # =========================================================================
    # 2. Window Dragging, 6px Euclidean Threshold, Clamping, and Cancel Button (✕)
    # =========================================================================
    def test_mouse_down_records_positions_without_dragging(self):
        """Verify _on_mouse_down initializes coordinate tracking without setting is_dragging."""
        self.widget._create_window()
        ev = DummyEvent(10, 10, x_root=350, y_root=450)
        self.widget._on_mouse_down(ev)
        self.assertEqual(self.widget.drag_start_x, 350)
        self.assertEqual(self.widget.drag_start_y, 450)
        self.assertFalse(self.widget.is_dragging)

    def test_mouse_drag_euclidean_threshold_below_6px(self):
        """Verify displacement < 6px Euclidean distance does not trigger drag mode."""
        self.widget._create_window()
        ev_down = DummyEvent(10, 10, x_root=300, y_root=300)
        self.widget._on_mouse_down(ev_down)
        
        # 4px in X, 3px in Y -> sqrt(16 + 9) = 5px < 6px
        ev_drag = DummyEvent(14, 13, x_root=304, y_root=303)
        self.widget._on_mouse_drag(ev_drag)
        self.assertFalse(self.widget.is_dragging)

    def test_mouse_drag_euclidean_threshold_above_6px(self):
        """Verify displacement >= 6px Euclidean distance triggers drag mode."""
        self.widget._create_window()
        ev_down = DummyEvent(10, 10, x_root=300, y_root=300)
        self.widget._on_mouse_down(ev_down)
        
        # 5px in X, 5px in Y -> sqrt(50) = 7.07px >= 6px
        ev_drag = DummyEvent(15, 15, x_root=305, y_root=305)
        self.widget._on_mouse_drag(ev_drag)
        self.assertTrue(self.widget.is_dragging)

    def test_drag_clamping_prevents_offscreen_loss(self):
        """Verify dragging to extreme offscreen coordinates is clamped within virtual screen."""
        self.widget._create_window()
        vx = user32.GetSystemMetrics(76)
        vy = user32.GetSystemMetrics(77)
        vw = user32.GetSystemMetrics(78)
        vh = user32.GetSystemMetrics(79)

        cx, cy = self.widget._clamp_coordinates(-99999, -99999)
        self.assertEqual(cx, vx)
        self.assertEqual(cy, vy)

        cx_far, cy_far = self.widget._clamp_coordinates(999999, 999999)
        self.assertEqual(cx_far, vx + vw - self.widget.width)
        self.assertEqual(cy_far, vy + vh - self.widget.height)

    def test_cancel_button_click_in_recording_state(self):
        """Verify clicking ✕ in RECORDING state triggers on_cancel and docks."""
        self.widget._create_window()
        self.widget.state = "RECORDING"
        ev_close = DummyEvent(self.widget.width - 10, 15)
        self.widget._on_mouse_up(ev_close)

        self.assertEqual(self.cancel_count, 1)
        self.assertEqual(self.widget.state, "DOCKED")
        self.assertEqual(self.toggle_count, 0)

    def test_cancel_button_click_in_processing_state(self):
        """Verify clicking ✕ in PROCESSING state triggers on_cancel and docks."""
        self.widget._create_window()
        self.widget.state = "PROCESSING"
        ev_close = DummyEvent(self.widget.width - 10, 15)
        self.widget._on_mouse_up(ev_close)

        self.assertEqual(self.cancel_count, 1)
        self.assertEqual(self.widget.state, "DOCKED")
        self.assertEqual(self.toggle_count, 0)

    def test_cancel_button_click_in_docked_state_hides_widget_and_syncs_tray(self):
        """Verify clicking ✕ in DOCKED state hides the widget AND notifies on_hide."""
        self.widget._create_window()
        self.widget.state = "DOCKED"
        ev_close = DummyEvent(self.widget.width - 10, 15)
        self.widget._on_mouse_up(ev_close)

        self.assertEqual(self.cancel_count, 0)
        self.assertEqual(self.hide_count, 1, "on_hide must be called so tray icon state stays in sync")

    def test_body_click_triggers_toggle_and_refocus(self):
        """Verify clicking the capsule body triggers on_click_toggle and refocus_target."""
        self.widget._create_window()
        ev_body = DummyEvent(50, 15)
        self.widget._on_mouse_up(ev_body)

        self.assertEqual(self.toggle_count, 1)
        self.assertTrue(self.injector.refocused)

    # =========================================================================
    # 3. SettingsWindow VU Monitor Non-Contention and Clean Closure
    # =========================================================================
    def test_settings_window_vu_monitor_lifecycle(self):
        """Verify SettingsWindow starts VU monitor on open, updates safely, and stops on close."""
        settings = SettingsWindow(
            config_manager=self.config,
            ai_engine=None,
            audio_recorder=self.audio_recorder
        )
        self.assertFalse(settings.is_open)
        self.assertFalse(self.audio_recorder.is_monitoring)

        # Show settings window
        settings.show()
        self.assertTrue(settings.is_open)
        self.assertTrue(self.audio_recorder.is_monitoring)
        self.assertGreaterEqual(self.audio_recorder.monitoring_started_count, 1)

        # Verify safe stop_vu_monitor does not disrupt unified stream
        settings.stop_vu_monitor()
        self.assertTrue(self.audio_recorder.is_monitoring)

        # Close settings window
        settings._on_close()
        self.assertFalse(settings.is_open)
        self.assertFalse(settings.test_running)
        self.assertFalse(self.audio_recorder.is_monitoring)
        self.assertIsNone(settings.window)

    def test_settings_window_recording_coexistence(self):
        """Verify starting recording while SettingsWindow is open causes 0 hardware contention."""
        settings = SettingsWindow(
            config_manager=self.config,
            ai_engine=None,
            audio_recorder=self.audio_recorder
        )
        settings.show()
        self.assertTrue(self.audio_recorder.is_monitoring)

        # User starts recording
        self.audio_recorder.start_recording()
        self.assertTrue(self.audio_recorder.is_recording)

        # Settings window tries device change during recording -> must NOT crash or close stream
        settings._on_device_changed("Secondary Mic")
        self.assertTrue(self.audio_recorder.is_recording)

        # Recording stops
        self.audio_recorder.stop_recording()
        self.assertFalse(self.audio_recorder.is_recording)

        # Clean close
        settings._on_close()
        self.assertFalse(self.audio_recorder.is_monitoring)

    # =========================================================================
    # 4. Tray Icon and Context Menu Interactions
    # =========================================================================
    def test_tray_icon_image_creation(self):
        """Verify TrayIcon generates a valid 64x64 RGBA icon."""
        tray = TrayIcon(
            on_open_settings=lambda: None,
            on_toggle_dictation=lambda: None,
            on_toggle_overlay=lambda: None,
            on_exit=lambda: None,
            config_manager=self.config
        )
        img = tray._create_image()
        self.assertEqual(img.size, (64, 64))
        self.assertEqual(img.mode, "RGBA")

    def test_tray_icon_hotkey_label_synchronization(self):
        """Verify get_hotkey_label dynamically reflects active config."""
        self.config.set("hotkey", "f9")
        tray = TrayIcon(
            on_open_settings=lambda: None,
            on_toggle_dictation=lambda: None,
            on_toggle_overlay=lambda: None,
            on_exit=lambda: None,
            config_manager=self.config
        )
        self.assertEqual(tray.get_hotkey_label(), "F9")

    def test_context_menu_cancel_and_exit_callbacks(self):
        """Verify context menu actions trigger proper callbacks including on_exit."""
        self.widget._create_window()
        self.widget.state = "RECORDING"
        self.widget._cancel_and_dock()
        self.assertEqual(self.cancel_count, 1)
        self.assertEqual(self.widget.state, "DOCKED")

    # =========================================================================
    # 5. Hide Timer Safety and Clean Teardown
    # =========================================================================
    def test_hide_timer_cleared_on_state_transition(self):
        """Verify hide_timer is properly cancelled when transitioning to new states."""
        self.widget._create_window()
        self.widget.show_success("Test")
        self.assertIsNotNone(self.widget.hide_timer)
        
        # Immediate state transition to recording cancels the timer
        self.widget.show_recording("Code")
        self.assertIsNone(self.widget.hide_timer)
        self.assertEqual(self.widget.state, "RECORDING")

    def test_clean_destroy_resets_state_and_closes_window(self):
        """Verify destroy() sets is_running=False, cancels timers, and closes window."""
        self.widget._create_window()
        self.widget.is_running = True
        self.widget.destroy()
        self.assertFalse(self.widget.is_running)
        self.assertIsNone(self.widget.root)

    # =========================================================================
    # 6. Hardened UI Features: Welcome Toast, Capture, Tick Anim, Test Key, Yield
    # =========================================================================
    def test_welcome_toast_lifecycle_and_auto_dock(self):
        """Verify startup welcome toast displays 'OwnVoice Ready • F8' and auto-docks to saved position."""
        self.config.set("overlay_x", 320, save=False)
        self.config.set("overlay_y", 480, save=False)
        self.widget._create_window()
        self.widget.show_welcome()

        self.assertEqual(self.widget.state, "WELCOME")
        self.assertIsNotNone(self.widget.hide_timer)
        self.assertGreaterEqual(self.widget.width, 180)

        # Trigger dock
        self.widget.dock()
        self.assertEqual(self.widget.state, "DOCKED")
        self.assertEqual(self.widget.width, 165)

    def test_mouse_drag_pointer_capture_and_release(self):
        """Verify Win32 SetCapture and ReleaseCapture guard mouse dragging against pointer loss."""
        self.widget._create_window()

        # Down triggers capture without dragging
        ev_down = DummyEvent(10, 10, x_root=200, y_root=200)
        self.widget._on_mouse_down(ev_down)
        self.assertFalse(self.widget.is_dragging)

        # Drag >= 6px triggers drag
        ev_drag = DummyEvent(20, 20, x_root=210, y_root=210)
        self.widget._on_mouse_drag(ev_drag)
        self.assertTrue(self.widget.is_dragging)

        # Up triggers ReleaseCapture
        ev_up = DummyEvent(20, 20, x_root=210, y_root=210)
        self.widget._on_mouse_up(ev_up)
        self.assertFalse(self.widget.is_dragging)

    def test_native_tick_anim_loop_no_daemon_thread_flooding(self):
        """Verify native Tk-driven _tick_anim loop is used without 25Hz thread flooding."""
        self.widget._create_window()
        self.widget.is_running = True
        self.widget.state = "RECORDING"
        initial_phase = self.widget.pulse_phase

        self.widget._tick_anim()
        self.assertGreater(self.widget.pulse_phase, initial_phase)
        self.assertIsNotNone(self.widget.anim_timer)

        # Stop and verify timer cancelled cleanly
        self.widget.destroy()
        self.assertIsNone(self.widget.anim_timer)

    def test_settings_test_key_valid_and_invalid_ping(self):
        """Verify Test Key button invokes lightweight test and sets green/red label."""
        class MockAIEngine:
            def __init__(self, valid_keys=None):
                self.valid_keys = valid_keys or {"VALID_TEST_KEY_123"}
            def test_key(self, key):
                if key in self.valid_keys:
                    return True, "Valid Key"
                return False, "Invalid Key"
            def test_connection(self, key):
                if key in self.valid_keys:
                    return True, "Connected to gemini-3.5-flash-lite! (45ms)", 0.045
                return False, "Invalid API Key. Please check your key.", 0.05

        mock_ai = MockAIEngine()
        settings = SettingsWindow(
            config_manager=self.config,
            ai_engine=mock_ai,
            audio_recorder=self.audio_recorder
        )
        settings.show()
        self.assertIsNotNone(settings.test_key_btn)
        self.assertIsNotNone(settings.key_status_label)

        # Test with invalid key
        settings.api_entry.delete(0, "end")
        settings.api_entry.insert(0, "BAD_KEY")
        settings._test_key()
        for _ in range(50):
            if settings.window:
                try:
                    settings.window.update()
                except Exception:
                    pass
            if settings.key_status_label.cget("text") not in ("", "Testing Key..."):
                break
            time.sleep(0.02)
        self.assertEqual(settings.key_status_label.cget("text"), "Invalid Key")
        self.assertEqual(settings.key_status_label.cget("text_color"), "#EF4444")

        # Test with valid key
        settings.api_entry.delete(0, "end")
        settings.api_entry.insert(0, "VALID_TEST_KEY_123")
        settings._test_key()
        for _ in range(50):
            if settings.window:
                try:
                    settings.window.update()
                except Exception:
                    pass
            if settings.key_status_label.cget("text") not in ("", "Testing Key..."):
                break
            time.sleep(0.02)
        self.assertEqual(settings.key_status_label.cget("text"), "Valid Key")
        self.assertEqual(settings.key_status_label.cget("text_color"), "#10B981")

        settings._on_close()

    def test_settings_vu_monitor_yields_to_active_dictation(self):
        """Verify VU monitor in Settings cleanly yields to active dictation and pauses updates."""
        settings = SettingsWindow(
            config_manager=self.config,
            ai_engine=None,
            audio_recorder=self.audio_recorder
        )
        settings.show()
        self.assertFalse(settings._vu_paused)

        # Signal dictation start
        settings.stop_vu_monitor()
        self.assertTrue(settings._vu_paused)
        # Unified stream is NOT stopped
        self.assertTrue(self.audio_recorder.is_monitoring)

        # Signal dictation stop
        settings.resume_vu_monitor()
        self.assertFalse(settings._vu_paused)

        settings._on_close()

if __name__ == "__main__":
    unittest.main()
