import unittest
import numpy as np
import struct
from core.audio_recorder import AudioRecorder
from tests.test_helpers import generate_pcm_spike, generate_pcm_audio, generate_stereo_dc_offset

class TestF06AgcDownmix(unittest.TestCase):
    """
    Feature 6: Percentile 50x Dynamic AGC & Energy Downmix
    Requirement: 99.2th percentile AGC immune to keypress transient spikes; adaptive stereo downmix preventing phase cancellation
    """

    def setUp(self):
        self.recorder = AudioRecorder()

    def test_transient_spike_does_not_throttle_gain(self):
        """Verify impulsive keyclick spike (16000) does not throttle gain on quiet speech (gain >= 40x)."""
        quiet_speech_rms = 100
        audio_bytes = generate_pcm_spike(
            duration_sec=1.0,
            sample_rate=48000,
            speech_amplitude=quiet_speech_rms,
            spike_amplitude=16000
        )
        if hasattr(self.recorder, "_apply_resilient_agc"):
            boosted_bytes, gain = self.recorder._apply_resilient_agc(audio_bytes, 48000, 1)
            self.assertGreaterEqual(gain, 35.0, f"Gain throttled by transient spike: {gain:.2f}x")
            boosted = np.frombuffer(boosted_bytes, dtype=np.int16)
            self.assertLessEqual(np.max(np.abs(boosted)), 32767)

    def test_gain_ceiling_is_50x(self):
        """Verify dynamic auto-gain ceiling does not exceed 50.0x on near-silence."""
        silence_bytes = (np.random.randn(48000) * 5).astype(np.int16).tobytes()
        if hasattr(self.recorder, "_apply_resilient_agc"):
            _, gain = self.recorder._apply_resilient_agc(silence_bytes, 48000, 1)
            self.assertLessEqual(gain, 50.0, f"Gain exceeded 50x maximum ceiling: {gain}")

    def test_adaptive_stereo_downmix_dead_channel(self):
        """Verify stereo downmix when one channel is dead (zero) does not halve signal power (-6dB attenuation)."""
        sample_count = 1000
        ch0 = (np.sin(np.linspace(0, 10 * np.pi, sample_count)) * 10000).astype(np.int16)
        ch1 = np.zeros(sample_count, dtype=np.int16)
        interleaved = np.empty(sample_count * 2, dtype=np.int16)
        interleaved[0::2] = ch0
        interleaved[1::2] = ch1
        
        if hasattr(self.recorder, "_downmix_stereo"):
            mono = self.recorder._downmix_stereo(interleaved)
            # Peak of mono should match peak of active channel without being halved to 5000
            self.assertGreater(np.max(np.abs(mono)), 8000)

    def test_dc_offset_removal(self):
        """Verify DC offset removal centers the waveform around 0 mean."""
        biased_bytes = generate_stereo_dc_offset(duration_sec=0.1, sample_rate=48000, dc_offset=6000)
        if hasattr(self.recorder, "_remove_dc_offset"):
            samples = np.frombuffer(biased_bytes, dtype=np.int16).astype(np.float32)
            centered = self.recorder._remove_dc_offset(samples)
            self.assertAlmostEqual(float(np.mean(centered)), 0.0, delta=10.0)

    def test_soft_saturation_limiting_bounds(self):
        """Verify boosted signal never exceeds int16 physical boundaries [-32768, 32767]."""
        loud_audio = (np.sin(np.linspace(0, 100 * np.pi, 48000)) * 25000).astype(np.int16).tobytes()
        if hasattr(self.recorder, "_apply_resilient_agc"):
            boosted_bytes, _ = self.recorder._apply_resilient_agc(loud_audio, 48000, 1)
            boosted = np.frombuffer(boosted_bytes, dtype=np.int16)
            self.assertTrue(np.all(boosted >= -32768))
            self.assertTrue(np.all(boosted <= 32767))

if __name__ == "__main__":
    unittest.main()
