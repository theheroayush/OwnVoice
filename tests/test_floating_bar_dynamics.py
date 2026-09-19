import unittest
from ui.floating_widget import FloatingWidget
from tests.test_helpers import InMemoryConfigManager

class TestFloatingBarDynamics(unittest.TestCase):
    """
    Validates all interactive elements, dynamic states, vector mic,
    and Note Taker integration of the new Dynamic Floating Bar.
    """

    def setUp(self):
        self.config = InMemoryConfigManager()
        self.dictate_toggled = False
        self.note_toggled = False
        self.cancelled = False

        def on_dictate():
            self.dictate_toggled = True

        def on_note():
            self.note_toggled = True

        def on_cancel():
            self.cancelled = True

        self.widget = FloatingWidget(
            get_volume_fn=lambda: 0.75,
            on_click_toggle=on_dictate,
            on_click_note_toggle=on_note,
            on_cancel=on_cancel,
            config_manager=self.config
        )

    def test_initial_geometry_and_docked_state(self):
        """Verify initial dimensions and docked width."""
        self.assertEqual(self.widget.width, 240)
        self.assertEqual(self.widget.height, 36)
        self.assertEqual(self.widget.state, "DOCKED")
        self.assertEqual(self.widget.docked_width, 240)

    def test_element_hit_detection(self):
        """Verify _get_element_at accurately partitions mic, dictate, note, hotkey, and close regions."""
        w = self.widget.width  # 240

        # Mic & studio icon region (< 36)
        self.assertEqual(self.widget._get_element_at(15, 18), "mic")

        # Dictate pill region (36 <= x < 108)
        self.assertEqual(self.widget._get_element_at(60, 18), "dictate")

        # Note chip region (114 <= x < 180)
        self.assertEqual(self.widget._get_element_at(140, 18), "note")

        # Hotkey keycap region (186 <= x < 212)
        self.assertEqual(self.widget._get_element_at(200, 18), "hotkey")

        # Close button region (x >= 216)
        self.assertEqual(self.widget._get_element_at(230, 18), "close")
        self.assertEqual(self.widget._get_element_at(w - 10, 18), "close")

    def test_note_chip_click_triggers_note_callback(self):
        """Clicking on the Note chip calls on_click_note_toggle."""
        class MockEvent:
            def __init__(self, x, y):
                self.x = x
                self.y = y
                self.x_root = x
                self.y_root = y

        # Click at x=140 (Note chip)
        self.widget._on_mouse_up(MockEvent(140, 18))
        self.assertTrue(self.note_toggled)
        self.assertFalse(self.dictate_toggled)

    def test_mic_click_triggers_dictation_callback(self):
        """Clicking on the Mic / Dictate calls on_click_toggle."""
        class MockEvent:
            def __init__(self, x, y):
                self.x = x
                self.y = y
                self.x_root = x
                self.y_root = y

        # Click at x=20 (Mic body)
        self.widget._on_mouse_up(MockEvent(20, 18))
        self.assertTrue(self.dictate_toggled)
        self.assertFalse(self.note_toggled)

    def test_recording_state_note_mode_vs_dictate_mode(self):
        """Verify recording state correctly sets is_note_mode and expands width."""
        # Dictation recording
        self.widget.show_recording(context_label="VS Code", is_note_mode=False)
        self.assertEqual(self.widget.state, "RECORDING")
        self.assertFalse(self.widget.is_note_mode)
        self.assertEqual(self.widget.context_label, "VS Code")
        self.assertGreaterEqual(self.widget.width, 200)

        # Note Taker recording
        self.widget.show_recording(context_label="Note Mode", is_note_mode=True)
        self.assertEqual(self.widget.state, "RECORDING")
        self.assertTrue(self.widget.is_note_mode)
        self.assertGreaterEqual(self.widget.width, 200)

    def test_processing_state_note_mode(self):
        """Verify processing state stores note mode flag."""
        self.widget.show_processing(is_note_mode=True)
        self.assertEqual(self.widget.state, "PROCESSING")
        self.assertTrue(self.widget.is_note_mode)

    def test_dock_resets_note_mode(self):
        """Verify docking resets state and note mode flag."""
        self.widget.show_recording(is_note_mode=True)
        self.widget.dock()
        self.assertEqual(self.widget.state, "DOCKED")
        self.assertFalse(self.widget.is_note_mode)
        self.assertEqual(self.widget.width, self.widget.docked_width)

if __name__ == "__main__":
    unittest.main()
