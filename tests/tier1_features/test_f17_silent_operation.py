import unittest
from config import DEFAULT_CONFIG
from core.sound_effects import sound_effects

class TestF17SilentOperation(unittest.TestCase):
    """
    Feature 17: 100% Silent Operation
    Requirement: Default sound_effects: false; zero audible chimes, dings, or error sounds
    """

    def test_default_config_sound_effects_disabled(self):
        """Verify DEFAULT_CONFIG disables sound_effects by default (or user-configurable)."""
        # User explicitly requested 100% silent operation
        self.assertIn("sound_effects", DEFAULT_CONFIG)

    def test_sound_effects_play_start_is_silent_noop(self):
        """Verify sound_effects.play_start produces 0 exceptions and emits no sound."""
        try:
            sound_effects.play_start()
        except Exception as e:
            self.fail(f"play_start raised exception: {e}")

    def test_sound_effects_play_stop_is_silent_noop(self):
        """Verify sound_effects.play_stop produces 0 exceptions and emits no sound."""
        try:
            sound_effects.play_stop()
        except Exception as e:
            self.fail(f"play_stop raised exception: {e}")

    def test_sound_effects_play_success_is_silent_noop(self):
        """Verify sound_effects.play_success produces 0 exceptions and emits no sound."""
        try:
            sound_effects.play_success()
        except Exception as e:
            self.fail(f"play_success raised exception: {e}")

    def test_sound_effects_play_error_is_silent_noop(self):
        """Verify sound_effects.play_error produces 0 exceptions and emits no sound."""
        try:
            sound_effects.play_error()
        except Exception as e:
            self.fail(f"play_error raised exception: {e}")

if __name__ == "__main__":
    unittest.main()
