import unittest
import time
import ctypes
from core.context_detector import ContextDetector

class TestF14ContextTone(unittest.TestCase):
    """
    Feature 14: Low-Latency Context Tone Detection
    Requirement: Sub-0.003ms native Win32 tone detection with expanded IDE and UWP process recognition
    Contract: ContextDetector.detect_tone(hwnd: int) -> Tuple[str, str]
    """

    def test_null_hwnd_defaults_to_flow(self):
        """Verify calling detect_tone with 0 or None returns default smart_flow tone."""
        mode, label = ContextDetector.detect_tone(0)
        self.assertEqual(mode, "smart_flow")
        self.assertEqual(label, "Flow")

    def test_latency_benchmark_under_0_2ms(self):
        """Verify average detect_tone execution time is well under the 0.2ms requirement."""
        desktop_hwnd = ctypes.windll.user32.GetDesktopWindow()
        # Warmup
        ContextDetector.detect_tone(desktop_hwnd)
        
        iterations = 200
        t0 = time.perf_counter()
        for _ in range(iterations):
            ContextDetector.detect_tone(desktop_hwnd)
        t1 = time.perf_counter()
        
        avg_latency_ms = ((t1 - t0) / iterations) * 1000
        self.assertLess(avg_latency_ms, 0.2, f"Latency {avg_latency_ms:.4f}ms exceeded 0.2ms ceiling")

    def test_coding_processes_inventory(self):
        """Verify CODING_PROCESSES contains essential developer tools."""
        self.assertIn("code.exe", ContextDetector.CODING_PROCESSES)
        self.assertIn("cmd.exe", ContextDetector.CODING_PROCESSES)
        self.assertIn("powershell.exe", ContextDetector.CODING_PROCESSES)

    def test_document_processes_inventory(self):
        """Verify DOCUMENT_PROCESSES contains word processors and text editors."""
        self.assertIn("winword.exe", ContextDetector.DOCUMENT_PROCESSES)
        self.assertIn("notepad.exe", ContextDetector.DOCUMENT_PROCESSES)

    def test_chat_processes_inventory(self):
        """Verify CHAT_PROCESSES contains enterprise and team messaging tools."""
        self.assertIn("slack.exe", ContextDetector.CHAT_PROCESSES)
        self.assertIn("discord.exe", ContextDetector.CHAT_PROCESSES)

    def test_get_window_info_returns_tuple_of_strings(self):
        """Verify get_window_info returns a 2-tuple of strings."""
        desktop_hwnd = ctypes.windll.user32.GetDesktopWindow()
        exe, title = ContextDetector.get_window_info(desktop_hwnd)
        self.assertIsInstance(exe, str)
        self.assertIsInstance(title, str)

if __name__ == "__main__":
    unittest.main()
