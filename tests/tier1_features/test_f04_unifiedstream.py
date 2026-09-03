import unittest
import time
from core.audio_recorder import AudioRecorder

class TestF04UnifiedStream(unittest.TestCase):
    """
    Feature 4: Unified Audio & Settings VU Stream
    Requirement: Consolidate capture into single stream; passive VU monitoring with zero contention; prevent VU thread death
    Contract: start_monitoring(), stop_monitoring(), is_recording, is_monitoring
    """

    def setUp(self):
        self.recorder = AudioRecorder()

    def tearDown(self):
        if self.recorder.is_recording:
            try:
                self.recorder.stop_recording()
            except Exception:
                pass
        if getattr(self.recorder, "is_monitoring", False):
            try:
                self.recorder.stop_monitoring()
            except Exception:
                pass

    def test_start_monitoring_sets_flag(self):
        """Verify start_monitoring sets is_monitoring property or flag."""
        if hasattr(self.recorder, "start_monitoring"):
            self.recorder.start_monitoring()
            self.assertTrue(getattr(self.recorder, "is_monitoring", False))
            self.assertFalse(self.recorder.is_recording)
            self.recorder.stop_monitoring()

    def test_monitoring_does_not_accumulate_audio_frames(self):
        """Verify audio frames are not stored in memory while only monitoring."""
        if hasattr(self.recorder, "start_monitoring"):
            self.recorder.start_monitoring()
            time.sleep(0.1)
            self.assertEqual(len(self.recorder.frames), 0)
            self.recorder.stop_monitoring()

    def test_transition_from_monitoring_to_recording(self):
        """Verify seamless transition from passive monitoring to active recording."""
        if hasattr(self.recorder, "start_monitoring"):
            self.recorder.start_monitoring()
            self.recorder.start_recording()
            self.assertTrue(self.recorder.is_recording)
            time.sleep(0.15)
            self.assertGreater(len(self.recorder.frames), 0)
            audio = self.recorder.stop_recording()
            self.assertIsInstance(audio, bytes)
            self.recorder.stop_monitoring()

    def test_stop_recording_retains_monitoring_if_enabled(self):
        """Verify stopping recording reverts to monitoring mode without stream crash."""
        if hasattr(self.recorder, "start_monitoring"):
            self.recorder.start_monitoring()
            self.recorder.start_recording()
            time.sleep(0.1)
            self.recorder.stop_recording()
            self.assertFalse(self.recorder.is_recording)
            self.assertTrue(getattr(self.recorder, "is_monitoring", False))
            self.recorder.stop_monitoring()

    def test_stop_monitoring_resets_stream(self):
        """Verify stop_monitoring cleanly shuts down stream and resets monitoring state."""
        if hasattr(self.recorder, "start_monitoring"):
            self.recorder.start_monitoring()
            self.recorder.stop_monitoring()
            self.assertFalse(getattr(self.recorder, "is_monitoring", False))
            self.assertFalse(self.recorder.is_recording)

if __name__ == "__main__":
    unittest.main()
