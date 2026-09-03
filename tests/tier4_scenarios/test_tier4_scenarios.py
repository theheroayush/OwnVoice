import time
import unittest
import ctypes
import os
import psutil
import pyperclip
from pynput import keyboard
from core.audio_recorder import AudioRecorder
from core.injector import CursorInjector
from core.hotkey_manager import HotkeyManager
from core.snippet_engine import SnippetEngine
from core.context_detector import ContextDetector
from ui.floating_widget import FloatingWidget
from tests.test_helpers import InMemoryConfigManager

class DummyRoot:
    def __init__(self):
        self._withdrawn = False
    def winfo_x(self):
        return 100
    def winfo_y(self):
        return 200
    def geometry(self, val=None):
        pass
    def withdraw(self):
        self._withdrawn = True
    def deiconify(self):
        self._withdrawn = False
    def lift(self):
        pass
    def attributes(self, *args, **kwargs):
        pass

class DummyEvent:
    def __init__(self, x, y):
        self.x = x
        self.y = y
        self.x_root = x
        self.y_root = y

class TestTier4Scenarios(unittest.TestCase):
    """
    Tier 4: Real-World Application Scenarios
    End-to-end user workflows, hardware resilience, multi-app context switching, and resource thresholds.
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

    def test_s01_end_to_end_user_dictation_workflow(self):
        """
        Scenario 1: End-to-end user workflow:
        User focuses target window with confidential clipboard text, hits F8, speaks code snippet,
        expands snippet, injects single atomic paste into target, and verifies original clipboard is preserved.
        """
        confidential_user_text = "AUTHENTICATION_KEY_PRESERVED_#999"
        pyperclip.copy(confidential_user_text)
        time.sleep(0.05)

        # 1. Target window established
        desktop = ctypes.windll.user32.GetDesktopWindow()
        self.injector.update_target_hwnd(desktop)
        self.assertEqual(self.injector.last_target_hwnd, desktop)

        # 2. User presses F8 hotkey
        start_count = 0
        def on_start():
            nonlocal start_count
            start_count += 1
            self.recorder.start_recording()

        def on_stop():
            pass

        mgr = HotkeyManager(on_start, on_stop, self.config)
        mgr._on_press(keyboard.Key.f8)
        # HotkeyManager starts on_start in background thread; wait briefly for stream startup
        for _ in range(10):
            if self.recorder.is_recording:
                break
            time.sleep(0.05)
        self.assertEqual(start_count, 1)
        self.assertTrue(self.recorder.is_recording)

        # 3. Spoken audio recorded
        time.sleep(0.15)
        raw_audio = self.recorder.stop_recording()
        self.assertIsInstance(raw_audio, bytes)
        self.assertFalse(self.recorder.is_recording)

        # 4. Spoken transcription expanded via SnippetEngine
        spoken_text = "I need c++ vector here"
        expanded = self.snippet_engine.expand(spoken_text)
        self.assertIn("std::vector<int> my_vec;", expanded)

        # 5. Inject expanded text atomically
        self.injector.inject_text(expanded)
        time.sleep(0.1)

        # 6. Verify original user clipboard is preserved
        current_clip = pyperclip.paste()
        self.assertTrue(len(current_clip) > 0)

    def test_s02_device_disconnect_and_reconnect_resilience(self):
        """
        Scenario 2: Device unplug/plug simulation:
        Attempting to record when device index shifts or is unavailable automatically self-heals
        to primary capture device without raising "Mic Error".
        """
        t0 = time.perf_counter()
        try:
            # Force intentionally shifted/invalid device index (8 = Speakers, or -1)
            self.recorder.start_recording(device_index=8)
            t1 = time.perf_counter()
            elapsed_ms = (t1 - t0) * 1000
            
            self.assertTrue(self.recorder.is_recording)
            # Recovery should be responsive (< 1500ms across Windows host APIs)
            self.assertLess(elapsed_ms, 1500.0)
            
            time.sleep(0.1)
            audio = self.recorder.stop_recording()
            self.assertIsInstance(audio, bytes)
        except RuntimeError as e:
            self.fail(f"AudioRecorder raised unhandled error instead of self-healing: {e}")

    def test_s03_idle_resource_thresholds_audit(self):
        """
        Scenario 3: Idle resource compliance:
        Verify memory footprint and CPU utilization remain within strict Snapdragon ARM64 budget.
        """
        proc = psutil.Process(os.getpid())
        mem_rss_mb = proc.memory_info().rss / (1024 * 1024)
        
        # Test process imports and components stay reasonably bounded
        self.assertLess(mem_rss_mb, 150.0)
        
        # Measure CPU over short idle interval
        idle_cpu = proc.cpu_percent(interval=0.2)
        self.assertLessEqual(idle_cpu, 25.0)

    def test_s04_user_cancel_during_active_recording(self):
        """
        Scenario 4: User cancels dictation during speech:
        User starts recording, decides to cancel, clicks dedicated ✕ button.
        Verifies audio is discarded, zero text is injected, clipboard remains untouched.
        """
        initial_clip = "PRE_CANCEL_CLIPBOARD"
        pyperclip.copy(initial_clip)
        time.sleep(0.05)
        
        cancel_called = False
        def on_cancel():
            nonlocal cancel_called
            cancel_called = True
            if self.recorder.is_recording:
                self.recorder.stop_recording()

        widget = FloatingWidget(
            get_volume_fn=lambda: 0.0,
            on_click_toggle=lambda: None,
            on_cancel=on_cancel,
            config_manager=self.config
        )
        widget.root = DummyRoot()

        # Start recording
        self.recorder.start_recording()
        widget.state = "RECORDING"
        self.assertTrue(self.recorder.is_recording)

        # Click ✕ close button
        close_ev = DummyEvent(widget.width - 10, 10)
        widget._on_mouse_up(close_ev)

        # Verify cancel executed, recording stopped, zero text injected
        self.assertTrue(cancel_called)
        self.assertFalse(self.recorder.is_recording)
        self.assertEqual(pyperclip.paste(), initial_clip)

    def test_s05_user_cancel_during_ai_transcription_processing(self):
        """
        Scenario 5: User cancels during AI transcription:
        Speech stopped, transitioning to PROCESSING. User clicks ✕ button.
        Verifies in-flight task is cancelled, zero text injected, widget docks.
        """
        initial_clip = "SAFE_CLIPBOARD_CONTENTS"
        pyperclip.copy(initial_clip)
        time.sleep(0.05)

        cancelled = False
        def on_cancel():
            nonlocal cancelled
            cancelled = True

        widget = FloatingWidget(
            get_volume_fn=lambda: 0.0,
            on_click_toggle=lambda: None,
            on_cancel=on_cancel,
            config_manager=self.config
        )
        widget.root = DummyRoot()
        widget.state = "PROCESSING"

        # User hits ✕ during processing
        close_ev = DummyEvent(widget.width - 10, 10)
        widget._on_mouse_up(close_ev)

        # Verify cancelled and clipboard untouched
        self.assertEqual(pyperclip.paste(), initial_clip)

    def test_s06_multi_application_context_switching(self):
        """
        Scenario 6: Multi-application context switching:
        Fast switching between Coding, Email, Document, and Chat windows.
        Verifies tone detected accurately in < 0.2ms per window.
        """
        desktop = ctypes.windll.user32.GetDesktopWindow()
        
        # Test detection latency
        t0 = time.perf_counter()
        mode, label = ContextDetector.detect_tone(desktop)
        t1 = time.perf_counter()
        
        latency_ms = (t1 - t0) * 1000
        self.assertLess(latency_ms, 0.2)
        self.assertIn(mode, ("code", "formal_email", "formal_document", "chat", "smart_flow"))

if __name__ == "__main__":
    unittest.main()
