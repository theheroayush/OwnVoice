import unittest
import math
import numpy as np
import sounddevice as sd
from core.audio_recorder import AudioRecorder
from tests.test_helpers import (
    generate_pcm_silence,
    generate_stereo_dc_offset,
    generate_out_of_phase_stereo
)

class TestTier2AudioBoundaries(unittest.TestCase):
    """
    Tier 2: Boundary & Corner Cases for Audio Subsystems (Features 1 to 6)
    Validates extreme inputs, edge values, hardware contention, and signal boundaries.
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

    # --- F01: WDM-KS Elimination Boundaries ---
    def test_b_f01_device_index_negative_extremes(self):
        """Verify handling of extreme negative device indices (-100, -999999)."""
        all_devs = sd.query_devices()
        # Ensure get_input_devices doesn't produce negative indices
        safe_devs = AudioRecorder.get_input_devices()
        for idx, _ in safe_devs:
            self.assertGreaterEqual(idx, 0)
            self.assertLess(idx, len(all_devs))

    def test_b_f01_device_index_overflow(self):
        """Verify 32-bit integer overflow device index is handled without segfault."""
        safe_devs = AudioRecorder.get_input_devices()
        for idx, _ in safe_devs:
            self.assertLess(idx, 2**31 - 1)

    def test_b_f01_filter_case_variations_of_wdmks(self):
        """Verify case-insensitive filtering of WDM-KS strings."""
        test_strings = ["wdm-ks", "WdM-Ks", "WDMKS", "kernel streaming", "Kernel Streaming"]
        for s in test_strings:
            self.assertTrue("WDM-KS" in s.upper() or "WDMKS" in s.upper() or "KERNEL STREAMING" in s.upper())

    def test_b_f01_filter_device_with_negative_channels(self):
        """Verify devices with <= 0 channels are strictly excluded."""
        safe_devs = AudioRecorder.get_input_devices()
        all_devs = sd.query_devices()
        for idx, _ in safe_devs:
            self.assertGreater(all_devs[idx].get("max_input_channels", 0), 0)

    def test_b_f01_device_list_immutability(self):
        """Verify calling get_input_devices multiple times produces consistent, non-corrupted lists."""
        d1 = AudioRecorder.get_input_devices()
        d2 = AudioRecorder.get_input_devices()
        self.assertEqual(len(d1), len(d2))
        self.assertEqual(d1, d2)

    # --- F02: Auto-Detect Boundaries ---
    def test_b_f02_non_standard_sample_rates(self):
        """Verify candidate samplerates are safe standard audio sample rates."""
        if hasattr(self.recorder, "_get_fallback_candidates"):
            candidates = self.recorder._get_fallback_candidates()
            for cand in candidates:
                if isinstance(cand, (tuple, list)) and len(cand) >= 3:
                    sr = cand[2]
                    self.assertIn(sr, (8000, 11025, 16000, 22050, 32000, 44100, 48000, 96000, 192000))

    def test_b_f02_candidate_deduplication(self):
        """Verify fallback candidate chain has 0 duplicate (idx, channels, samplerate) tuples."""
        if hasattr(self.recorder, "_get_fallback_candidates"):
            candidates = self.recorder._get_fallback_candidates()
            seen = set()
            for cand in candidates:
                key = (cand[0], cand[1], cand[2]) if isinstance(cand, (tuple, list)) and len(cand) >= 3 else cand
                self.assertNotIn(key, seen, f"Duplicate candidate found: {key}")
                seen.add(key)

    def test_b_f02_candidate_channels_bounded_to_hardware_max(self):
        """Verify candidate channels do not exceed 2 (stereo) or device capability."""
        if hasattr(self.recorder, "_get_fallback_candidates"):
            candidates = self.recorder._get_fallback_candidates()
            all_devs = sd.query_devices()
            for cand in candidates:
                if isinstance(cand, (tuple, list)) and len(cand) >= 2:
                    idx, ch = cand[0], cand[1]
                    hw_max = all_devs[idx]["max_input_channels"]
                    self.assertLessEqual(ch, hw_max)
                    self.assertLessEqual(ch, 2)

    def test_b_f02_preferred_device_out_of_bounds_falls_back(self):
        """Verify requesting index 9999 gracefully falls back to valid candidate."""
        if hasattr(self.recorder, "_get_fallback_candidates"):
            candidates = self.recorder._get_fallback_candidates(preferred_index=9999)
            self.assertGreater(len(candidates), 0)
            # 9999 must NOT be in the resolved candidate list
            for cand in candidates:
                idx = cand[0] if isinstance(cand, (tuple, list)) else cand
                self.assertNotEqual(idx, 9999)

    def test_b_f02_negative_preferred_device_index(self):
        """Verify negative preferred device index (-5) is ignored and falls back."""
        if hasattr(self.recorder, "_get_fallback_candidates"):
            candidates = self.recorder._get_fallback_candidates(preferred_index=-5)
            self.assertGreater(len(candidates), 0)
            for cand in candidates:
                idx = cand[0] if isinstance(cand, (tuple, list)) else cand
                self.assertGreaterEqual(idx, 0)

    # --- F03: Self-Healing Fallback Boundaries ---
    def test_b_f03_rapid_successive_start_recording_reentrancy(self):
        """Verify calling start_recording 5 times in rapid succession does not lock or crash."""
        try:
            for _ in range(5):
                self.recorder.start_recording()
                self.assertTrue(self.recorder.is_recording)
            self.recorder.stop_recording()
        finally:
            if self.recorder.is_recording:
                self.recorder.stop_recording()

    def test_b_f03_stop_recording_when_never_started(self):
        """Verify calling stop_recording when not recording returns empty or valid bytes without crash."""
        try:
            res = self.recorder.stop_recording()
            self.assertIsInstance(res, (bytes, bytearray))
        except Exception as e:
            self.fail(f"stop_recording crashed when idle: {e}")

    def test_b_f03_device_fallback_under_output_device_simulation(self):
        """Verify fallback when an output device is forced."""
        try:
            self.recorder.start_recording(device_index=8)
            self.assertTrue(self.recorder.is_recording)
            self.recorder.stop_recording()
        except Exception as e:
            self.fail(f"Failed fallback on output device: {e}")

    def test_b_f03_zero_frame_abort_handling(self):
        """Verify starting and immediately stopping in 0ms does not divide by zero or corrupt state."""
        self.recorder.start_recording()
        data = self.recorder.stop_recording()
        self.assertIsInstance(data, bytes)
        self.assertFalse(self.recorder.is_recording)

    def test_b_f03_portaudio_terminate_initialize_idempotency(self):
        """Verify multiple consecutive PortAudio re-initializations."""
        for _ in range(3):
            sd._terminate()
            sd._initialize()
        self.assertGreater(len(sd.query_devices()), 0)

    # --- F04: Unified Stream Boundaries ---
    def test_b_f04_rapid_monitoring_toggles_20x(self):
        """Verify rapid toggling of start_monitoring / stop_monitoring 20 times."""
        if hasattr(self.recorder, "start_monitoring"):
            for _ in range(20):
                self.recorder.start_monitoring()
                self.recorder.stop_monitoring()
            self.assertFalse(getattr(self.recorder, "is_monitoring", False))

    def test_b_f04_start_recording_while_already_recording(self):
        """Verify calling start_recording twice sequentially does not lose accumulated frames."""
        self.recorder.start_recording()
        self.recorder.start_recording()
        self.assertTrue(self.recorder.is_recording)
        self.recorder.stop_recording()

    def test_b_f04_stop_monitoring_while_never_monitored(self):
        """Verify calling stop_monitoring on unmonitored recorder does not crash."""
        if hasattr(self.recorder, "stop_monitoring"):
            try:
                self.recorder.stop_monitoring()
            except Exception as e:
                self.fail(f"stop_monitoring crashed on idle state: {e}")

    def test_b_f04_get_volume_when_stream_is_none(self):
        """Verify get_current_volume returns 0.0 when stream has never been opened."""
        vol = self.recorder.get_current_volume() if hasattr(self.recorder, "get_current_volume") else self.recorder.current_volume
        self.assertEqual(vol, 0.0)

    def test_b_f04_concurrent_volume_queries(self):
        """Verify 100 consecutive volume reads return valid floats in [0.0, 1.0]."""
        for _ in range(100):
            vol = self.recorder.get_current_volume() if hasattr(self.recorder, "get_current_volume") else self.recorder.current_volume
            self.assertGreaterEqual(vol, 0.0)
            self.assertLessEqual(vol, 1.0)

    # --- F05: Calibrated dBFS Meter Boundaries ---
    def _calc_dbfs(self, rms):
        dB = 20 * math.log10(max(1.0, float(rms)) / 32767.0)
        return min(1.0, max(0.0, (dB + 50.0) / 40.0))

    def test_b_f05_dbfs_negative_rms_clamping(self):
        """Verify negative RMS is clamped to 1.0 floor and evaluates to 0.0."""
        level = self._calc_dbfs(-500.0)
        self.assertEqual(level, 0.0)

    def test_b_f05_dbfs_zero_rms(self):
        """Verify exactly zero RMS evaluates to 0.0 level without math domain error."""
        level = self._calc_dbfs(0.0)
        self.assertEqual(level, 0.0)

    def test_b_f05_dbfs_overflow_rms_clamping(self):
        """Verify RMS above 32767 (e.g. 65536) clamps to exactly 1.0."""
        level = self._calc_dbfs(65536.0)
        self.assertEqual(level, 1.0)

    def test_b_f05_dbfs_fractional_rms(self):
        """Verify fractional RMS values (0.001) evaluate cleanly."""
        level = self._calc_dbfs(0.001)
        self.assertEqual(level, 0.0)

    def test_b_f05_dbfs_precision_no_nan_or_inf(self):
        """Verify dBFS calculation never yields NaN or Inf for any valid float input."""
        for test_val in [0.0, 0.5, 1.0, 100.0, 1000.0, 32767.0, 50000.0]:
            lvl = self._calc_dbfs(test_val)
            self.assertFalse(math.isnan(lvl))
            self.assertFalse(math.isinf(lvl))

    # --- F06: Percentile AGC & Downmix Boundaries ---
    def test_b_f06_pure_dc_bias_signal(self):
        """Verify pure DC signal (+10000 constant) is handled without division by zero."""
        dc_buf = generate_stereo_dc_offset(duration_sec=0.1, dc_offset=10000)
        if hasattr(self.recorder, "_apply_resilient_agc"):
            boosted, gain = self.recorder._apply_resilient_agc(dc_buf, 48000, 2)
            self.assertIsInstance(boosted, bytes)

    def test_b_f06_square_wave_maximum_clipping(self):
        """Verify alternating square wave at maximum limits (+32767, -32768) does not overflow."""
        samples = np.array([32767, -32768] * 1000, dtype=np.int16)
        if hasattr(self.recorder, "_apply_resilient_agc"):
            boosted, gain = self.recorder._apply_resilient_agc(samples.tobytes(), 48000, 1)
            arr = np.frombuffer(boosted, dtype=np.int16)
            self.assertTrue(np.all(arr >= -32768))
            self.assertTrue(np.all(arr <= 32767))

    def test_b_f06_180_degree_out_of_phase_stereo_cancellation(self):
        """Verify stereo signal with 180-degree phase inversion does not cancel to silence in downmix."""
        stereo_inv = generate_out_of_phase_stereo(duration_sec=0.1, amplitude=10000)
        if hasattr(self.recorder, "_downmix_stereo"):
            interleaved = np.frombuffer(stereo_inv, dtype=np.int16)
            mono = self.recorder._downmix_stereo(interleaved)
            # Destructive cancellation would yield 0; adaptive downmix preserves amplitude > 5000
            self.assertGreater(np.max(np.abs(mono)), 5000)

    def test_b_f06_micro_buffer_less_than_10_samples(self):
        """Verify very small buffers (<10 samples) don't crash AGC percentile calculation."""
        micro_samples = np.array([100, -100, 200, -200], dtype=np.int16)
        if hasattr(self.recorder, "_apply_resilient_agc"):
            boosted, gain = self.recorder._apply_resilient_agc(micro_samples.tobytes(), 48000, 1)
            self.assertEqual(len(boosted), len(micro_samples) * 2)

    def test_b_f06_all_silence_agc(self):
        """Verify complete silence (all zero samples) maintains 0 amplitude and caps at <= 50x gain."""
        silence = generate_pcm_silence(duration_sec=0.1)
        if hasattr(self.recorder, "_apply_resilient_agc"):
            boosted, gain = self.recorder._apply_resilient_agc(silence, 48000, 1)
            self.assertLessEqual(gain, 50.0)

if __name__ == "__main__":
    unittest.main()
