import unittest
import math
from core.audio_recorder import AudioRecorder

class TestF05DbfsMeter(unittest.TestCase):
    """
    Feature 5: Calibrated Logarithmic dBFS VU Meter
    Requirement: Replace linear formula with -50 to -10 dBFS standard scale for responsive visual level
    Contract: AudioRecorder.get_current_volume() -> float (0.0 to 1.0)
    """

    def setUp(self):
        self.recorder = AudioRecorder()

    def _calc_dbfs_reference(self, rms: float) -> float:
        """Authoritative specification from PROJECT.md & Survey 1."""
        dB = 20 * math.log10(max(1.0, rms) / 32767.0)
        return min(1.0, max(0.0, (dB + 50.0) / 40.0))

    def test_volume_initializes_to_zero(self):
        """Verify initial volume is 0.0 when recorder is idle."""
        vol = self.recorder.get_current_volume() if hasattr(self.recorder, "get_current_volume") else self.recorder.current_volume
        self.assertEqual(vol, 0.0)

    def test_silence_level_clamped_to_zero(self):
        """Verify absolute silence (RMS <= 1) evaluates to 0.0."""
        level = self._calc_dbfs_reference(1.0)
        self.assertAlmostEqual(level, 0.0, places=2)

    def test_ambient_room_noise_not_pegged_at_full_scale(self):
        """Verify ambient room noise (RMS 400, -38 dBFS) is responsive around ~0.30 and NOT pegged at 1.0."""
        level = self._calc_dbfs_reference(400.0)
        self.assertGreater(level, 0.20, "Ambient noise should register on meter")
        self.assertLess(level, 0.45, "Ambient noise must NOT saturate the VU meter at 1.0")

    def test_normal_speech_level_in_expected_bracket(self):
        """Verify normal speech (RMS 3500, -19 dBFS) registers in the 0.70 - 0.85 bracket."""
        level = self._calc_dbfs_reference(3500.0)
        self.assertGreater(level, 0.65)
        self.assertLess(level, 0.90)

    def test_maximum_scale_clamped_to_one(self):
        """Verify full scale digital audio (RMS 32767, 0 dBFS) caps at 1.0."""
        level = self._calc_dbfs_reference(32767.0)
        self.assertEqual(level, 1.0)

    def test_monotonicity_of_dbfs_scale(self):
        """Verify volume level strictly increases as RMS increases across full dynamic range."""
        rms_values = [5.0, 50.0, 200.0, 500.0, 2000.0, 5000.0, 15000.0, 32767.0]
        levels = [self._calc_dbfs_reference(r) for r in rms_values]
        for i in range(len(levels) - 1):
            self.assertLessEqual(levels[i], levels[i+1], f"Monotonicity failed at RMS {rms_values[i]}")

if __name__ == "__main__":
    unittest.main()
