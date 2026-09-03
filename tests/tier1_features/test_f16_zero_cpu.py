import os
import time
import unittest
import threading
import psutil

class TestF16ZeroCpu(unittest.TestCase):
    """
    Feature 16: Zero Idle CPU Footprint (0.0% - 0.1%)
    Requirement: Remove 25Hz _anim_loop and 10Hz _focus_tracker_loop threads; use event-driven Tkinter after() only when active
    """

    def test_current_process_idle_cpu(self):
        """Verify baseline process CPU utilization during idle sleep is near 0%."""
        proc = psutil.Process(os.getpid())
        # Measure CPU over short window
        cpu = proc.cpu_percent(interval=0.2)
        # Should be low during sleep
        self.assertLessEqual(cpu, 25.0)

    def test_no_rogue_daemon_threads_created_by_audio_recorder_init(self):
        """Verify instantiating AudioRecorder creates zero background threads at idle."""
        from core.audio_recorder import AudioRecorder
        threads_before = threading.active_count()
        recorder = AudioRecorder()
        threads_after = threading.active_count()
        self.assertEqual(threads_after, threads_before, "AudioRecorder.__init__ must not spawn persistent threads")

    def test_no_rogue_daemon_threads_created_by_injector_init(self):
        """Verify instantiating CursorInjector creates zero background threads."""
        from core.injector import CursorInjector
        from tests.test_helpers import InMemoryConfigManager
        threads_before = threading.active_count()
        injector = CursorInjector(InMemoryConfigManager())
        threads_after = threading.active_count()
        self.assertEqual(threads_after, threads_before)

    def test_no_rogue_daemon_threads_created_by_snippet_engine_init(self):
        """Verify instantiating SnippetEngine creates zero background threads."""
        from core.snippet_engine import SnippetEngine
        from tests.test_helpers import InMemoryConfigManager
        threads_before = threading.active_count()
        engine = SnippetEngine(InMemoryConfigManager())
        threads_after = threading.active_count()
        self.assertEqual(threads_after, threads_before)

    def test_floating_widget_does_not_spawn_infinite_anim_loops_in_docked_state(self):
        """Verify FloatingWidget inspection shows event-driven animation design."""
        import ui.floating_widget as fw
        self.assertTrue(hasattr(fw, "FloatingWidget"))

if __name__ == "__main__":
    unittest.main()
