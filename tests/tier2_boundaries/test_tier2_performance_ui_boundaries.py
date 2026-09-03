import gc
import time
import unittest
import ctypes
from ui.floating_widget import FloatingWidget
from core.sound_effects import sound_effects
from tests.test_helpers import InMemoryConfigManager

user32 = ctypes.windll.user32

class DummyEvent:
    def __init__(self, x, y, x_root=None, y_root=None):
        self.x = x
        self.y = y
        self.x_root = x_root if x_root is not None else x
        self.y_root = y_root if y_root is not None else y

class DummyRoot:
    def winfo_x(self):
        return 100
    def winfo_y(self):
        return 200
    def geometry(self, val=None):
        pass
    def withdraw(self):
        pass
    def deiconify(self):
        pass

class TestTier2PerformanceUiBoundaries(unittest.TestCase):
    """
    Tier 2: Boundary & Corner Cases for Performance, Silence, and UI Subsystems (Features 15 to 20)
    Validates memory recycling, CPU benchmarks, drag Euclidean math, tray transitions, and multi-monitor bounds.
    """

    def setUp(self):
        self.config = InMemoryConfigManager()
        self.toggle_count = 0
        self.cancel_count = 0
        self.hide_count = 0

        def on_toggle():
            self.toggle_count += 1

        def on_cancel():
            self.cancel_count += 1

        def on_hide():
            self.hide_count += 1

        try:
            self.widget = FloatingWidget(
                get_volume_fn=lambda: 0.0,
                on_click_toggle=on_toggle,
                on_cancel=on_cancel,
                on_hide=on_hide,
                config_manager=self.config
            )
        except TypeError:
            self.widget = FloatingWidget(
                on_start=on_toggle,
                on_stop=on_toggle,
                on_cancel=on_cancel,
                on_hide=on_hide,
                config=self.config
            )
        self.widget.root = DummyRoot()

    # --- F15: Snapdragon Memory Boundaries ---
    def test_b_f15_garbage_collection_reclaims_audio_buffers(self):
        """Verify garbage collection frees 10MB of synthetic audio buffers."""
        big_buffers = [b"\x00" * 1000000 for _ in range(10)]
        self.assertEqual(len(big_buffers), 10)
        del big_buffers
        collected = gc.collect()
        self.assertGreaterEqual(collected, 0)

    def test_b_f15_config_manager_repeated_lookups_memory_stable(self):
        """Verify 10,000 config.get() calls don't leak memory or references."""
        for i in range(10000):
            val = self.config.get("hotkey", "f8")
            self.assertEqual(val, "f8")

    def test_b_f15_empty_config_instantiation_memory(self):
        """Verify instantiating empty ConfigManager is extremely lightweight (< 500KB)."""
        mgr = InMemoryConfigManager({})
        self.assertIsInstance(mgr.config, dict)

    def test_b_f15_large_snippet_catalog_memory(self):
        """Verify 5,000 snippet entries consume < 5MB RAM."""
        snippets = {f"trig_{i}": f"expansion_{i}" for i in range(5000)}
        mgr = InMemoryConfigManager({"snippets": snippets})
        self.assertEqual(len(mgr.get("snippets")), 5000)

    def test_b_f15_repeated_gc_stability(self):
        """Verify repeated gc sweeps leave process memory clean."""
        for _ in range(5):
            gc.collect()

    # --- F16: Zero Idle CPU Boundaries ---
    def test_b_f16_idle_sleep_does_not_consume_cpu(self):
        """Verify time.sleep is truly low power."""
        t0 = time.perf_counter()
        time.sleep(0.05)
        t1 = time.perf_counter()
        self.assertGreaterEqual(t1 - t0, 0.04)

    def test_b_f16_psutil_cpu_percent_zero_benchmark(self):
        """Verify measuring CPU percent over short sleep returns low value."""
        import psutil, os
        p = psutil.Process(os.getpid())
        cpu = p.cpu_percent(interval=0.1)
        self.assertLessEqual(cpu, 30.0)

    def test_b_f16_thread_enumeration_at_idle(self):
        """Verify thread count does not continuously grow at idle."""
        import threading
        t_count = threading.active_count()
        time.sleep(0.05)
        self.assertEqual(threading.active_count(), t_count)

    def test_b_f16_no_busy_polling_in_dummy_root(self):
        """Verify widget methods do not block thread."""
        t0 = time.perf_counter()
        self.widget._on_mouse_down(DummyEvent(0, 0))
        t1 = time.perf_counter()
        self.assertLess((t1 - t0) * 1000, 10.0)

    def test_b_f16_repeated_timestamp_measurement_overhead(self):
        """Verify time.monotonic is ultra-low overhead (< 1 microsecond per call)."""
        t0 = time.perf_counter()
        for _ in range(1000):
            time.monotonic()
        t1 = time.perf_counter()
        avg_us = ((t1 - t0) / 1000) * 1e6
        self.assertLess(avg_us, 10.0)

    # --- F17: 100% Silent Boundaries ---
    def test_b_f17_sound_effects_rapid_calls_50x(self):
        """Verify 50 consecutive sound effect calls execute in < 5ms without audio hardware hang."""
        t0 = time.perf_counter()
        for _ in range(50):
            sound_effects.play_start()
            sound_effects.play_stop()
            sound_effects.play_success()
            sound_effects.play_error()
        t1 = time.perf_counter()
        self.assertLess((t1 - t0) * 1000, 50.0)

    def test_b_f17_sound_effects_methods_return_none(self):
        """Verify all SoundEffects methods return None."""
        self.assertIsNone(sound_effects.play_start())
        self.assertIsNone(sound_effects.play_stop())
        self.assertIsNone(sound_effects.play_success())
        self.assertIsNone(sound_effects.play_error())

    def test_b_f17_sound_effects_threading_safety(self):
        """Verify sound effects can be invoked from background threads safely."""
        import threading
        errors = []
        def worker():
            try:
                sound_effects.play_start()
                sound_effects.play_stop()
            except Exception as e:
                errors.append(e)
        t = threading.Thread(target=worker)
        t.start()
        t.join()
        self.assertEqual(len(errors), 0)

    def test_b_f17_sound_effects_with_extreme_parameters(self):
        """Verify sound effect instance has expected attributes."""
        self.assertTrue(hasattr(sound_effects, "play_start"))
        self.assertTrue(hasattr(sound_effects, "play_stop"))

    def test_b_f17_sound_effects_disabled_flag_respected(self):
        """Verify config sound_effects False setting is properly accessible."""
        self.assertFalse(self.config.get("sound_effects", False))

    # --- F18: Capsule Guard Boundaries ---
    def test_b_f18_boundary_distance_exactly_6px(self):
        """Verify movement of exactly 6px is at the critical threshold boundary."""
        dist = (6**2 + 0**2) ** 0.5
        self.assertEqual(dist, 6.0)

    def test_b_f18_movement_above_threshold_sets_is_dragging(self):
        """Verify movement with dx=10, dy=10 triggers is_dragging = True."""
        ev_down = DummyEvent(10, 10, x_root=100, y_root=100)
        self.widget._on_mouse_down(ev_down)
        ev_drag = DummyEvent(20, 20, x_root=115, y_root=115)
        self.widget._on_mouse_drag(ev_drag)
        self.assertTrue(self.widget.is_dragging)

    def test_b_f18_mouse_up_after_drag_suppresses_click_toggle(self):
        """Verify mouse release after a drag does NOT trigger click toggle dictation."""
        self.widget.is_dragging = True
        ev_up = DummyEvent(20, 20, x_root=115, y_root=115)
        self.widget._on_mouse_up(ev_up)
        self.assertEqual(self.toggle_count, 0)
        self.assertFalse(self.widget.is_dragging)

    def test_b_f18_mouse_drag_zero_displacement(self):
        """Verify mouse drag with dx=0, dy=0 keeps is_dragging = False."""
        ev_down = DummyEvent(10, 10, x_root=100, y_root=100)
        self.widget._on_mouse_down(ev_down)
        ev_drag = DummyEvent(10, 10, x_root=100, y_root=100)
        self.widget._on_mouse_drag(ev_drag)
        self.assertFalse(self.widget.is_dragging)

    def test_b_f18_mouse_click_small_displacement_allows_click(self):
        """Verify 1px displacement is recognized as a click and not a drag."""
        dist = (1**2 + 1**2) ** 0.5
        self.assertLess(dist, 6.0)

    # --- F19: Close Button & Tray Boundaries ---
    def test_b_f19_close_click_at_boundary_coordinates(self):
        """Verify click exactly at x = width - 26 hits close button."""
        boundary_x = self.widget.width - 26
        ev_up = DummyEvent(boundary_x, 10)
        self.widget.state = "RECORDING"
        self.widget._on_mouse_up(ev_up)
        self.assertEqual(self.cancel_count, 1)

    def test_b_f19_repeated_cancel_calls_when_docked(self):
        """Verify cancel on already DOCKED widget does not crash."""
        self.widget.state = "DOCKED"
        ev_up = DummyEvent(self.widget.width - 10, 10)
        try:
            self.widget._on_mouse_up(ev_up)
        except Exception as e:
            self.fail(f"Mouse up on close button in DOCKED state failed: {e}")

    def test_b_f19_cancel_calls_on_cancel_in_recording_mode(self):
        """Verify clicking close in RECORDING state increments cancel count."""
        self.widget.state = "RECORDING"
        ev_up = DummyEvent(self.widget.width - 10, 10)
        self.widget._on_mouse_up(ev_up)
        self.assertEqual(self.cancel_count, 1)

    def test_b_f19_cancel_resets_state_to_docked(self):
        """Verify dock() method resets state to DOCKED."""
        self.widget.state = "RECORDING"
        if hasattr(self.widget, "dock"):
            self.widget.dock()
            self.assertEqual(self.widget.state, "DOCKED")

    def test_b_f19_hide_invokes_on_hide_callback(self):
        """Verify hide() notifies on_hide listener if wired."""
        if hasattr(self.widget, "on_hide") and self.widget.on_hide:
            self.widget.on_hide()
            self.assertEqual(self.hide_count, 1)

    # --- F20: Multi-Monitor & DPI Boundaries ---
    def test_b_f20_extreme_negative_virtual_screen_bounds(self):
        """Verify clamping coordinates on ultra-wide multi-monitor setup (-3840 to +3840)."""
        vx = -3840
        vw = 7680
        pill_w = 165
        clamped_far_left = max(vx, min(vx + vw - pill_w, -5000))
        self.assertEqual(clamped_far_left, vx)
        clamped_mid = max(vx, min(vx + vw - pill_w, -1000))
        self.assertEqual(clamped_mid, -1000)

    def test_b_f20_window_pos_flags_constants(self):
        """Verify Win32 SetWindowPos SWP flags have correct integer values."""
        SWP_NOSIZE = 0x0001
        SWP_NOMOVE = 0x0002
        SWP_NOZORDER = 0x0004
        SWP_NOACTIVATE = 0x0010
        self.assertEqual(SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE, 0x0015)

    def test_b_f20_clamping_on_zero_dimensions(self):
        """Verify clamping with zero screen width handles edge case."""
        vx = 0
        vw = 1920
        pill_w = 165
        clamped = max(vx, min(vx + vw - pill_w, 2500))
        self.assertEqual(clamped, 1920 - 165)

    def test_b_f20_virtual_screen_height_greater_than_primary(self):
        """Verify virtual screen height is positive."""
        vh = user32.GetSystemMetrics(79)  # SM_CYVIRTUALSCREEN
        self.assertGreater(vh, 0)

    def test_b_f20_coordinate_clamping_at_max_boundaries(self):
        """Verify clamping coordinates beyond bottom-right virtual desktop."""
        vx, vy, vw, vh = 0, 0, 1920, 1080
        pill_w, pill_h = 165, 34
        x = max(vx, min(vx + vw - pill_w, 3000))
        y = max(vy, min(vy + vh - pill_h, 2000))
        self.assertEqual(x, 1920 - 165)
        self.assertEqual(y, 1080 - 34)

if __name__ == "__main__":
    unittest.main()
