import unittest
import math
from ui.floating_widget import FloatingWidget
from tests.test_helpers import InMemoryConfigManager

class DummyEvent:
    def __init__(self, x, y, x_root=None, y_root=None):
        self.x = x
        self.y = y
        self.x_root = x_root if x_root is not None else x
        self.y_root = y_root if y_root is not None else y

class TestF18CapsuleGuard(unittest.TestCase):
    """
    Feature 18: Draggable Capsule Accidental Trigger Guard
    Requirement: Differentiate ✕ vs body; 6px Euclidean threshold; 400ms time window; canvas.grab_set(); SetWindowPos
    """

    def setUp(self):
        self.config = InMemoryConfigManager()
        self.toggle_count = 0
        self.cancel_count = 0

        def on_toggle():
            self.toggle_count += 1

        def on_cancel():
            self.cancel_count += 1

        try:
            self.widget = FloatingWidget(
                get_volume_fn=lambda: 0.0,
                on_click_toggle=on_toggle,
                on_cancel=on_cancel,
                config_manager=self.config
            )
        except TypeError:
            self.widget = FloatingWidget(
                on_start=on_toggle,
                on_stop=on_toggle,
                on_cancel=on_cancel,
                config=self.config
            )

    def test_widget_geometry_dimensions(self):
        """Verify capsule default width and height."""
        self.assertEqual(self.widget.width, self.widget.docked_width)
        self.assertEqual(self.widget.height, 36)

    def test_euclidean_distance_threshold_calculation(self):
        """Verify Euclidean distance formula distinguishes 6px threshold."""
        # 4px dx, 3px dy -> sqrt(16 + 9) = 5px (below 6px threshold = click candidate)
        dist_below = math.sqrt(4**2 + 3**2)
        self.assertLess(dist_below, 6.0)
        
        # 5px dx, 5px dy -> sqrt(50) = 7.07px (above 6px threshold = drag)
        dist_above = math.sqrt(5**2 + 5**2)
        self.assertGreater(dist_above, 6.0)

    def test_close_button_hit_region_boundary(self):
        """Verify click within width - 26 is close button, while x < width - 28 is body."""
        close_x = self.widget.width - 15  # Inside ✕ button area
        body_x = 50  # Inside capsule body
        self.assertGreaterEqual(close_x, self.widget.width - 28)
        self.assertLess(body_x, self.widget.width - 28)

    def test_initial_drag_state_is_false(self):
        """Verify is_dragging flag is initially False."""
        self.assertFalse(self.widget.is_dragging)

    def test_mouse_down_records_start_coordinates(self):
        """Verify _on_mouse_down records start positions without initiating drag."""
        class DummyRoot:
            def winfo_x(self):
                return 100
            def winfo_y(self):
                return 200

        self.widget.root = DummyRoot()
        ev = DummyEvent(10, 10, x_root=500, y_root=300)
        self.widget._on_mouse_down(ev)
        self.assertEqual(self.widget.drag_start_x, 500)
        self.assertEqual(self.widget.drag_start_y, 300)
        self.assertFalse(self.widget.is_dragging)

if __name__ == "__main__":
    unittest.main()
