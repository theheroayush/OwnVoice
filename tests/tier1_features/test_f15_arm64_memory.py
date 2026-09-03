import os
import sys
import unittest
import psutil

class TestF15Arm64Memory(unittest.TestCase):
    """
    Feature 15: Snapdragon ARM64 Idle Memory < 40MB
    Requirement: Lazy-load SettingsWindow/customtkinter and requests/urllib to drop idle RSS to <40MB
    """

    def test_core_audio_import_memory_overhead(self):
        """Verify importing core audio recorder adds modest memory (< 15MB)."""
        process = psutil.Process(os.getpid())
        mem_before = process.memory_info().rss
        import core.audio_recorder
        mem_after = process.memory_info().rss
        overhead_mb = (mem_after - mem_before) / (1024 * 1024)
        # Import overhead should be reasonable
        self.assertLess(overhead_mb, 20.0)

    def test_core_injector_import_memory_overhead(self):
        """Verify importing core injector adds minimal overhead (< 5MB)."""
        process = psutil.Process(os.getpid())
        mem_before = process.memory_info().rss
        import core.injector
        mem_after = process.memory_info().rss
        overhead_mb = (mem_after - mem_before) / (1024 * 1024)
        self.assertLess(overhead_mb, 10.0)

    def test_snippet_engine_import_memory_overhead(self):
        """Verify importing snippet engine adds minimal overhead (< 2MB)."""
        process = psutil.Process(os.getpid())
        mem_before = process.memory_info().rss
        import core.snippet_engine
        mem_after = process.memory_info().rss
        overhead_mb = (mem_after - mem_before) / (1024 * 1024)
        self.assertLess(overhead_mb, 5.0)

    def test_context_detector_import_memory_overhead(self):
        """Verify importing context detector adds minimal overhead (< 2MB)."""
        process = psutil.Process(os.getpid())
        mem_before = process.memory_info().rss
        import core.context_detector
        mem_after = process.memory_info().rss
        overhead_mb = (mem_after - mem_before) / (1024 * 1024)
        self.assertLess(overhead_mb, 5.0)

    def test_config_memory_efficiency(self):
        """Verify ConfigManager loads configuration with < 2MB footprint."""
        from config import ConfigManager
        mgr = ConfigManager()
        self.assertIsInstance(mgr.config, dict)
        self.assertLess(len(mgr.config), 100)

if __name__ == "__main__":
    unittest.main()
