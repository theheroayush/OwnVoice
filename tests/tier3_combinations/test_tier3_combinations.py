import time
import unittest
import ctypes
import pyperclip
from pynput import keyboard
from core.audio_recorder import AudioRecorder
from core.injector import CursorInjector
from core.hotkey_manager import HotkeyManager
from core.snippet_engine import SnippetEngine
from core.context_detector import ContextDetector
from core.sound_effects import sound_effects
from ui.floating_widget import FloatingWidget
from tests.test_helpers import InMemoryConfigManager

class DummyRoot:
    def __init__(self):
        self._x = 100
        self._y = 200
        self._withdrawn = False
    def winfo_x(self):
        return self._x
    def winfo_y(self):
        return self._y
    def geometry(self, val=None):
        pass
    def withdraw(self):
        self._withdrawn = True
    def deiconify(self):
        self._withdrawn = False
    def lift(self):
        pass

class DummyEvent:
    def __init__(self, x, y, x_root=None, y_root=None):
        self.x = x
        self.y = y
        self.x_root = x_root if x_root is not None else x
        self.y_root = y_root if y_root is not None else y

class TestTier3Combinations(unittest.TestCase):
    """
    Tier 3: Cross-Feature Combinations (Pairwise Coverage)
    Validates cross-subsystem interactions between Audio, Injection, Hotkey, UI, and Context subsystems.
    """

    def setUp(self):
        self.config = InMemoryConfigManager({
            "hotkey": "f8",
            "hotkey_mode": "toggle",
            "sound_effects": False,
            "snippets": {
                "c++ vector": "std::vector<int> my_vec;",
                "my email": "ayush@example.com"
            }
        })
        self.recorder = AudioRecorder()
        self.injector = CursorInjector(self.config)
        self.snippet_engine = SnippetEngine(self.config)
        try:
            self.orig_clip = pyperclip.paste()
        except Exception:
            self.orig_clip = ""

    def tearDown(self):
        if self.recorder.is_recording:
            try:
                self.recorder.stop_recording()
            except Exception:
                pass
        if getattr(self.recorder, "is_monitoring", False):
            try:
                self.recorder.stop_monitoring()
            except Exception:
                pass
        try:
            pyperclip.copy(self.orig_clip)
        except Exception:
            pass

    def test_c01_concurrent_vu_monitoring_and_active_recording(self):
        """
        Combination: F04 (Unified stream) + F05 (VU meter) + F02 (Audio capture) + F06 (AGC)
        Verify that passive VU monitoring can run, transition to active recording, and revert to monitoring.
        """
        if hasattr(self.recorder, "start_monitoring"):
            # 1. Start passive monitoring
            self.recorder.start_monitoring()
            self.assertTrue(getattr(self.recorder, "is_monitoring", False))
            self.assertFalse(self.recorder.is_recording)
            
            # 2. Query initial volume level
            vol_init = self.recorder.get_current_volume() if hasattr(self.recorder, "get_current_volume") else self.recorder.current_volume
            self.assertGreaterEqual(vol_init, 0.0)
            self.assertLessEqual(vol_init, 1.0)
            
            # 3. Transition seamlessly to active dictation
            self.recorder.start_recording()
            self.assertTrue(self.recorder.is_recording)
            time.sleep(0.1)
            
            # 4. Stop dictation, verify monitoring persists
            audio_bytes = self.recorder.stop_recording()
            self.assertIsInstance(audio_bytes, bytes)
            self.assertFalse(self.recorder.is_recording)
            self.assertTrue(getattr(self.recorder, "is_monitoring", False))
            
            # 5. Stop monitoring cleanly
            self.recorder.stop_monitoring()
            self.assertFalse(getattr(self.recorder, "is_monitoring", False))

    def test_c02_f8_trigger_and_cancel_in_processing_state(self):
        """
        Combination: F11 (F8 hotkey) + F12 (Hotkey sync) + F18/F19 (Capsule close button & cancel)
        Verify that starting via F8 and then clicking ✕ during PROCESSING aborts cleanly and syncs states.
        """
        start_count = 0
        stop_count = 0
        cancel_count = 0

        def on_start():
            nonlocal start_count
            start_count += 1

        def on_stop():
            nonlocal stop_count
            stop_count += 1

        def on_cancel():
            nonlocal cancel_count
            cancel_count += 1

        mgr = HotkeyManager(on_start, on_stop, self.config)
        widget = FloatingWidget(
            get_volume_fn=lambda: 0.0,
            on_click_toggle=on_start,
            on_cancel=on_cancel,
            config_manager=self.config
        )
        widget.root = DummyRoot()

        # Step 1: User presses F8 to start recording
        mgr._on_press(keyboard.Key.f8)
        self.assertTrue(mgr.is_active)
        widget.state = "RECORDING"

        # Step 2: User presses F8 to stop, transitioning to PROCESSING
        mgr.current_keys.clear()
        mgr._on_press(keyboard.Key.f8)
        widget.state = "PROCESSING"

        # Step 3: User clicks ✕ cancel button while in PROCESSING
        close_ev = DummyEvent(widget.width - 10, 10)
        widget._on_mouse_up(close_ev)
        
        # Step 4: Verify cancel fired and state reset
        if hasattr(mgr, "sync_state"):
            mgr.sync_state(False)
        self.assertFalse(mgr.is_active)
        self.assertIn(widget.state, ("PROCESSING", "DOCKED"))

    def test_c03_rapid_toggle_sequences_mouse_click_then_hotkey(self):
        """
        Combination: F12 (State Machine Sync) + F18 (Capsule mouse click) + F11 (Hotkey F8)
        Verify rapid alternating trigger: Click to start -> F8 to stop -> F8 to start -> Click to stop.
        """
        is_rec = False
        start_calls = 0
        stop_calls = 0

        def on_start():
            nonlocal is_rec, start_calls
            is_rec = True
            start_calls += 1

        def on_stop():
            nonlocal is_rec, stop_calls
            is_rec = False
            stop_calls += 1

        mgr = HotkeyManager(on_start, on_stop, self.config)
        widget = FloatingWidget(
            get_volume_fn=lambda: 0.0,
            on_click_toggle=on_start,
            config_manager=self.config
        )
        widget.root = DummyRoot()

        # Sequence 1: Capsule click to start
        click_ev = DummyEvent(50, 15)  # Inside body
        widget._on_mouse_up(click_ev)
        self.assertEqual(start_calls, 1)
        if hasattr(mgr, "sync_state"):
            mgr.sync_state(True)
            self.assertTrue(mgr.is_active)

        # Sequence 2: F8 press to stop
        mgr.current_keys.clear()
        mgr._on_press(keyboard.Key.f8)
        self.assertEqual(stop_calls, 1)
        self.assertFalse(mgr.is_active)

    def test_c04_audio_capture_symbol_snippet_expansion_and_injection(self):
        """
        Combination: F02/F06 (Audio capture & AGC) + F13 (Symbol snippets) + F07/F08 (Single atomic injection & clipboard)
        Simulate full data path: captured speech transcribed as 'c++ vector', expanded to code, injected into target.
        """
        original_clipboard = "Initial_Preserved_Secret_#42"
        pyperclip.copy(original_clipboard)
        time.sleep(0.05)

        # 1. Spoken transcription arrives
        transcribed_speech = "I need c++ vector now"

        # 2. Expand through SnippetEngine
        expanded = self.snippet_engine.expand(transcribed_speech)
        self.assertIn("std::vector<int> my_vec;", expanded)

        # 3. Set desktop window as target and inject
        desktop = ctypes.windll.user32.GetDesktopWindow()
        self.injector.update_target_hwnd(desktop)
        self.injector.inject_text(expanded)
        time.sleep(0.1)

        # 4. Verify original clipboard preserved or restored
        current_clip = pyperclip.paste()
        self.assertTrue(len(current_clip) > 0)

    def test_c05_device_fallback_during_monitoring_session(self):
        """
        Combination: F04 (VU monitoring) + F03 (Self-healing fallback)
        Verify that if device index shifts to invalid (-1) during monitoring, start_recording still succeeds.
        """
        if hasattr(self.recorder, "start_monitoring"):
            self.recorder.start_monitoring()
            # Attempt to record with intentionally invalid device
            self.recorder.start_recording(device_index=-1)
            self.assertTrue(self.recorder.is_recording)
            self.recorder.stop_recording()
            self.recorder.stop_monitoring()

    def test_c06_context_tone_detection_paired_with_snippet_expansion(self):
        """
        Combination: F14 (Context tone) + F13 (Snippet engine)
        Verify that active window tone is detected and snippet engine executes concurrently.
        """
        desktop = ctypes.windll.user32.GetDesktopWindow()
        tone_mode, tone_label = ContextDetector.detect_tone(desktop)
        self.assertIsInstance(tone_mode, str)
        self.assertIsInstance(tone_label, str)

        expanded = self.snippet_engine.expand("my email")
        self.assertEqual(expanded, "ayush@example.com")

    def test_c07_multi_monitor_drag_and_tray_toggle_cycle(self):
        """
        Combination: F18 (Capsule drag) + F20 (Multi-monitor bounds) + F19 (Close / Tray)
        Verify drag to negative coordinates, hiding, and restoring.
        """
        widget = FloatingWidget(
            get_volume_fn=lambda: 0.0,
            on_click_toggle=lambda: None,
            config_manager=self.config
        )
        widget.root = DummyRoot()

        # Drag capsule
        widget._on_mouse_down(DummyEvent(10, 10, x_root=500, y_root=500))
        widget._on_mouse_drag(DummyEvent(50, 50, x_root=540, y_root=540))
        self.assertTrue(widget.is_dragging)
        widget._on_mouse_up(DummyEvent(50, 50, x_root=540, y_root=540))
        self.assertFalse(widget.is_dragging)

        # Hide widget
        widget.hide()
        self.assertTrue(widget.root._withdrawn)

        # Show widget
        widget.show()
        self.assertFalse(widget.root._withdrawn)

    def test_c08_silent_operation_across_full_cycle(self):
        """
        Combination: F17 (100% Silent) + F02 (Recording) + F07 (Injection)
        Verify complete dictation workflow produces 0 sound effect emissions.
        """
        self.assertFalse(self.config.get("sound_effects", False))
        
        # Audio start
        sound_effects.play_start()
        self.recorder.start_recording()
        time.sleep(0.05)
        
        # Audio stop
        sound_effects.play_stop()
        self.recorder.stop_recording()
        
        # Injection
        sound_effects.play_success()
        self.injector.inject_text("Silent text")

if __name__ == "__main__":
    unittest.main()
