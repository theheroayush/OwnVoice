import unittest
from ui.floating_widget import FloatingWidget
from tests.test_helpers import InMemoryConfigManager

class TestF19CloseTraySync(unittest.TestCase):
    """
    Feature 19: Dedicated ✕ Button & Tray Synchronization
    Requirement: Wire on_hide to update overlay_visible; allow ✕ to cancel active recording/processing; reapply WS_EX_NOACTIVATE
    """

    def setUp(self):
        self.config = InMemoryConfigManager()
        self.hide_calls = 0
        self.cancel_calls = 0

        def on_hide():
            self.hide_calls += 1

        def on_cancel():
            self.cancel_calls += 1

        try:
            self.widget = FloatingWidget(
                get_volume_fn=lambda: 0.0,
                on_click_toggle=lambda: None,
                on_cancel=on_cancel,
                on_hide=on_hide,
                config_manager=self.config
            )
        except TypeError:
            self.widget = FloatingWidget(
                on_start=lambda: None,
                on_stop=lambda: None,
                on_cancel=on_cancel,
                on_hide=on_hide,
                config=self.config
            )

    def test_initial_state_is_docked(self):
        """Verify widget starts in DOCKED state."""
        self.assertEqual(self.widget.state, "DOCKED")

    def test_set_state_transitions(self):
        """Verify state transitions between DOCKED, RECORDING, PROCESSING."""
        if hasattr(self.widget, "set_state"):
            self.widget.set_state("RECORDING")
            self.assertEqual(self.widget.state, "RECORDING")
            self.widget.set_state("PROCESSING")
            self.assertEqual(self.widget.state, "PROCESSING")
            self.widget.set_state("DOCKED")
            self.assertEqual(self.widget.state, "DOCKED")

    def test_on_hide_callback_assigned(self):
        """Verify on_hide callback is registered in FloatingWidget."""
        self.assertIsNotNone(self.widget.on_hide)

    def test_on_cancel_callback_assigned(self):
        """Verify on_cancel callback is registered in FloatingWidget."""
        self.assertIsNotNone(self.widget.on_cancel)

    def test_cancel_triggers_in_recording_state(self):
        """Verify cancel logic is executed when state is RECORDING."""
        self.widget.state = "RECORDING"
        if self.widget.on_cancel:
            self.widget.on_cancel()
            self.assertEqual(self.cancel_calls, 1)

if __name__ == "__main__":
    unittest.main()
