import unittest
import ctypes

user32 = ctypes.windll.user32

SM_XVIRTUALSCREEN = 76
SM_YVIRTUALSCREEN = 77
SM_CXVIRTUALSCREEN = 78
SM_CYVIRTUALSCREEN = 79

class TestF20MultiMonitorDpi(unittest.TestCase):
    """
    Feature 20: Multi-Monitor Bounds Clamping & DPI Awareness
    Requirement: Clamp capsule to virtual desktop metrics (SM_XVIRTUALSCREEN); enable Per-Monitor V2 DPI awareness
    """

    def test_virtual_screen_metrics_retrievable(self):
        """Verify Win32 GetSystemMetrics returns valid virtual desktop metrics."""
        vx = user32.GetSystemMetrics(SM_XVIRTUALSCREEN)
        vy = user32.GetSystemMetrics(SM_YVIRTUALSCREEN)
        vw = user32.GetSystemMetrics(SM_CXVIRTUALSCREEN)
        vh = user32.GetSystemMetrics(SM_CYVIRTUALSCREEN)
        
        self.assertGreater(vw, 0, "Virtual screen width must be > 0")
        self.assertGreater(vh, 0, "Virtual screen height must be > 0")
        # vx and vy can be negative in multi-monitor setups
        self.assertIsInstance(vx, int)
        self.assertIsInstance(vy, int)

    def test_coordinate_clamping_logic_within_virtual_screen(self):
        """Verify clamping formula keeps capsule within virtual desktop boundaries."""
        vx = user32.GetSystemMetrics(SM_XVIRTUALSCREEN)
        vy = user32.GetSystemMetrics(SM_YVIRTUALSCREEN)
        vw = user32.GetSystemMetrics(SM_CXVIRTUALSCREEN)
        vh = user32.GetSystemMetrics(SM_CYVIRTUALSCREEN)
        
        pill_w = 165
        pill_h = 34
        
        # Test extreme off-screen coordinates
        far_left = vx - 1000
        far_right = vx + vw + 1000
        far_top = vy - 1000
        far_bottom = vy + vh + 1000
        
        clamped_left = max(vx, min(vx + vw - pill_w, far_left))
        clamped_right = max(vx, min(vx + vw - pill_w, far_right))
        clamped_top = max(vy, min(vy + vh - pill_h, far_top))
        clamped_bottom = max(vy, min(vy + vh - pill_h, far_bottom))
        
        self.assertEqual(clamped_left, vx)
        self.assertEqual(clamped_right, vx + vw - pill_w)
        self.assertEqual(clamped_top, vy)
        self.assertEqual(clamped_bottom, vy + vh - pill_h)

    def test_negative_monitor_coordinate_support(self):
        """Verify clamping logic supports negative X coordinate for left-sided secondary displays."""
        vx = -1920
        vw = 3840
        pill_w = 165
        
        target_x = -500  # On secondary monitor to the left
        clamped_x = max(vx, min(vx + vw - pill_w, target_x))
        self.assertEqual(clamped_x, -500)

    def test_dpi_awareness_api_available(self):
        """Verify Win32 SetProcessDpiAwarenessContext or SetProcessDPIAware is present on system."""
        has_v2 = hasattr(user32, "SetProcessDpiAwarenessContext")
        has_v1 = hasattr(user32, "SetProcessDPIAware")
        self.assertTrue(has_v2 or has_v1, "System must provide Win32 DPI awareness APIs")

    def test_primary_screen_vs_virtual_screen_relationship(self):
        """Verify virtual screen width is greater than or equal to primary screen width."""
        primary_w = user32.GetSystemMetrics(0)
        virtual_w = user32.GetSystemMetrics(SM_CXVIRTUALSCREEN)
        self.assertGreaterEqual(virtual_w, primary_w)

if __name__ == "__main__":
    unittest.main()
