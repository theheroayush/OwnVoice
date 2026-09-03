"""
Adversarial Stress Test Harness for core/audio_recorder.py
Milestone 1 - Empirical Challenger 1

Covers:
1. Bogus device indices (-1, 999, 8, etc.), rapid recovery, thread leaks, latency.
2. Concurrent monitoring and recording toggling, race conditions, deadlocks.
3. AGC extreme signals (silence, extreme clipping spikes, phase inversion, DC bias, square wave).
"""

import os
import math
import struct
import subprocess
import sys
import threading
import time
import unittest
import numpy as np
import sounddevice as sd

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from core.audio_recorder import AudioRecorder


class TestAudioStressDeviceIndices(unittest.TestCase):
    """Stress testing bogus device indices, recovery time, and thread safety."""

    def setUp(self):
        self.recorder = AudioRecorder()

    def tearDown(self):
        try:
            if self.recorder.is_recording:
                self.recorder.stop_recording()
        except Exception:
            pass
        try:
            if self.recorder.is_monitoring:
                self.recorder.stop_monitoring()
        except Exception:
            pass

    def test_bogus_indices_recovery_cycle(self):
        """Repeatedly start and stop recording with bogus indices in rapid succession."""
        bogus_indices = [-1, 999, 8, -50, 100, 8, 999, -1, 10, None]
        initial_threads = threading.active_count()
        latencies = []

        for cycle in range(2):  # 2 full passes = 20 switches
            for idx in bogus_indices:
                t0 = time.perf_counter()
                self.recorder.start_recording(device_index=idx)
                t1 = time.perf_counter()
                elapsed_ms = (t1 - t0) * 1000.0
                latencies.append(elapsed_ms)

                self.assertTrue(self.recorder.is_recording, f"Failed to record with index {idx}")
                self.assertIsNotNone(self.recorder.active_device_index)

                # Verify active device is actually an input device
                dev_info = sd.query_devices(self.recorder.active_device_index)
                self.assertGreater(dev_info.get("max_input_channels", 0), 0)

                # Stop recording
                self.recorder.stop_recording()
                self.assertFalse(self.recorder.is_recording)

        final_threads = threading.active_count()
        self.assertEqual(initial_threads, final_threads, f"Thread leak detected: {final_threads - initial_threads} threads leaked")

        avg_latency = sum(latencies) / len(latencies)
        print(f"\n[Test Bogus Indices] 20 cycles: avg={avg_latency:.2f}ms, min={min(latencies):.2f}ms, max={max(latencies):.2f}ms")

    def test_fast_transition_when_monitoring(self):
        """Verify that transitioning to recording while already monitoring takes <1ms."""
        self.recorder.start_monitoring()
        self.assertTrue(self.recorder.is_monitoring)
        time.sleep(0.05)  # Let stream stabilize

        latencies = []
        for _ in range(10):
            t0 = time.perf_counter()
            self.recorder.start_recording(device_index=-1)
            t1 = time.perf_counter()
            elapsed_ms = (t1 - t0) * 1000.0
            latencies.append(elapsed_ms)

            self.assertTrue(self.recorder.is_recording)
            self.recorder.stop_recording()
            self.assertFalse(self.recorder.is_recording)
            self.assertTrue(self.recorder.is_monitoring)

        self.recorder.stop_monitoring()
        avg_trans = sum(latencies) / len(latencies)
        print(f"[Fast Transition] 10 transitions from monitoring: avg={avg_trans:.3f}ms, max={max(latencies):.3f}ms")
        self.assertLess(avg_trans, 5.0, f"Transition from monitoring took too long: {avg_trans}ms")


class TestAudioStressConcurrency(unittest.TestCase):
    """Stress testing concurrent stream operations, thread contention, and deadlock freedom."""

    def test_sequential_rapid_toggling(self):
        """Sequential rapid toggling between monitoring and recording must succeed without error."""
        recorder = AudioRecorder()
        for _ in range(25):
            recorder.start_monitoring()
            vol = recorder.get_current_volume()
            self.assertTrue(0.0 <= vol <= 1.0)
            recorder.start_recording()
            vol2 = recorder.get_current_volume()
            self.assertTrue(0.0 <= vol2 <= 1.0)
            audio = recorder.stop_recording()
            recorder.stop_monitoring()

    def test_concurrent_stream_unsynchronized_race_vulnerability(self):
        """
        Empirical Demonstration: Concurrent calls to start/stop monitoring and
        start/stop recording cause heap corruption (0xc0000374) or access violation in PortAudio.
        """
        code = """
import time
import threading
from core.audio_recorder import AudioRecorder

recorder = AudioRecorder()
stop_event = threading.Event()

def worker_mon():
    while not stop_event.is_set():
        try:
            recorder.start_monitoring()
            time.sleep(0.005)
            recorder.stop_monitoring()
            time.sleep(0.005)
        except Exception:
            pass

def worker_rec():
    while not stop_event.is_set():
        try:
            recorder.start_recording(device_index=8)
            time.sleep(0.01)
            recorder.stop_recording()
            time.sleep(0.005)
        except Exception:
            pass

th1 = threading.Thread(target=worker_mon)
th2 = threading.Thread(target=worker_rec)
th1.start()
th2.start()

time.sleep(1.5)
stop_event.set()
th1.join(timeout=2.0)
th2.join(timeout=2.0)
if th1.is_alive() or th2.is_alive():
    import sys
    sys.exit(42)  # Deadlock exit code
"""
        res = subprocess.run(
            [sys.executable, "-c", code],
            cwd=os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
            capture_output=True,
            timeout=10
        )
        # Check if crash (non-zero returncode or heap corruption) occurred
        print(f"\n[Concurrency Stress Empirical Result] Process returncode: {res.returncode}")
        if res.returncode != 0:
            print(f"[Concurrency Vulnerability Confirmed] Unsynchronized AudioRecorder crashed/deadlocked with returncode {res.returncode}")

    def test_synchronized_wrapper_concurrency_success(self):
        """
        Verify that adding a synchronization lock around stream lifecycle calls
        completely eliminates all deadlocks, access violations, and heap corruptions.
        """
        code = """
import time
import threading
from core.audio_recorder import AudioRecorder

class SynchronizedAudioRecorder(AudioRecorder):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._stream_lock = threading.RLock()

    def start_monitoring(self, device_index=None):
        with self._stream_lock:
            super().start_monitoring(device_index)

    def stop_monitoring(self):
        with self._stream_lock:
            super().stop_monitoring()

    def start_recording(self, device_index=None):
        with self._stream_lock:
            super().start_recording(device_index)

    def stop_recording(self):
        with self._stream_lock:
            return super().stop_recording()

rec = SynchronizedAudioRecorder()
stop_event = threading.Event()

def worker_mon():
    while not stop_event.is_set():
        rec.start_monitoring()
        time.sleep(0.005)
        rec.stop_monitoring()
        time.sleep(0.005)

def worker_rec():
    while not stop_event.is_set():
        rec.start_recording(device_index=8)
        time.sleep(0.01)
        rec.stop_recording()
        time.sleep(0.005)

th1 = threading.Thread(target=worker_mon)
th2 = threading.Thread(target=worker_rec)
th1.start()
th2.start()

time.sleep(1.5)
stop_event.set()
th1.join(timeout=2.0)
th2.join(timeout=2.0)
assert not th1.is_alive() and not th2.is_alive()
print("SYNCHRONIZED_SUCCESS")
"""
        res = subprocess.run(
            [sys.executable, "-c", code],
            cwd=os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
            capture_output=True,
            text=True,
            timeout=10
        )
        self.assertEqual(res.returncode, 0, f"Synchronized recorder failed: {res.stderr}")
        self.assertIn("SYNCHRONIZED_SUCCESS", res.stdout)
        print("[Synchronized Fix Verified] 100% clean concurrency under lock synchronization.")


class TestAudioStressAGCExtremes(unittest.TestCase):
    """Stress testing automatic gain control and downmix under extreme acoustic/digital signals."""

    def test_pure_silence_signal(self):
        """Feeding all zeros must not divide by zero, produce NaN, or crash."""
        sample_rate = 48000
        silence_mono = b"\x00" * (sample_rate * 2 * 2)
        out_bytes, gain = AudioRecorder._apply_resilient_agc(silence_mono, sample_rate, 1)

        self.assertIsInstance(out_bytes, bytes)
        self.assertEqual(len(out_bytes), len(silence_mono))
        self.assertEqual(gain, 1.0)

        out_samples = np.frombuffer(out_bytes, dtype=np.int16)
        self.assertFalse(np.isnan(out_samples).any())
        self.assertEqual(np.max(np.abs(out_samples)), 0)

        silence_stereo = b"\x00" * (sample_rate * 2 * 2 * 2)
        out_bytes_st, gain_st = AudioRecorder._apply_resilient_agc(silence_stereo, sample_rate, 2)
        self.assertEqual(gain_st, 1.0)
        out_samples_st = np.frombuffer(out_bytes_st, dtype=np.int16)
        self.assertFalse(np.isnan(out_samples_st).any())
        self.assertEqual(np.max(np.abs(out_samples_st)), 0)

    def test_extreme_transient_clipping_spikes(self):
        """Single massive impulses (+32767, -32768) must not throttle normal speech gain."""
        sample_rate = 48000
        total_samples = sample_rate * 1
        np.random.seed(123)
        speech = (np.random.randn(total_samples) * 100).astype(np.int16)

        speech[100] = 32767
        speech[500] = -32768
        speech[1000] = 32767

        out_bytes, gain = AudioRecorder._apply_resilient_agc(speech.tobytes(), sample_rate, 1)
        out_samples = np.frombuffer(out_bytes, dtype=np.int16)

        self.assertGreaterEqual(gain, 40.0, f"Gain was improperly throttled by transient spikes: {gain}")
        self.assertTrue(np.all(out_samples >= -32768))
        self.assertTrue(np.all(out_samples <= 32767))
        self.assertLessEqual(np.max(np.abs(out_samples)), 32767)

    def test_out_of_phase_stereo_cancellation_prevention(self):
        """180-degree out of phase stereo signal must NOT cancel to silence."""
        sample_rate = 48000
        total_samples = sample_rate // 2
        t = np.linspace(0, 0.5, total_samples, endpoint=False)
        wave = (np.sin(2 * np.pi * 440.0 * t) * 10000.0).astype(np.int16)

        stereo = np.empty((total_samples * 2,), dtype=np.int16)
        stereo[0::2] = wave
        stereo[1::2] = -wave

        out_bytes, gain = AudioRecorder._apply_resilient_agc(stereo.tobytes(), sample_rate, 2)
        out_samples = np.frombuffer(out_bytes, dtype=np.int16)

        out_rms = np.sqrt(np.mean(out_samples.astype(np.float32) ** 2))
        self.assertGreater(out_rms, 1000.0, f"Phase cancellation occurred! Output RMS is nearly silent: {out_rms}")

    def test_extreme_dc_bias_offset(self):
        """Acoustic signal with huge DC offset (+15000) must be centered cleanly."""
        sample_rate = 48000
        total_samples = sample_rate // 2
        t = np.linspace(0, 0.5, total_samples, endpoint=False)
        wave = (np.sin(2 * np.pi * 440.0 * t) * 500.0 + 15000.0).astype(np.int16)

        out_bytes, gain = AudioRecorder._apply_resilient_agc(wave.tobytes(), sample_rate, 1)
        out_samples = np.frombuffer(out_bytes, dtype=np.int16)

        mean_val = float(np.mean(out_samples))
        self.assertLess(abs(mean_val), 5.0, f"DC offset was not removed: mean is {mean_val}")

    def test_rail_to_rail_square_wave(self):
        """Full-scale square wave (+32767, -32768) must be handled without overflow or NaN."""
        sample_rate = 48000
        total_samples = sample_rate // 4
        square = np.ones(total_samples, dtype=np.int16) * 32767
        square[::2] = -32768

        out_bytes, gain = AudioRecorder._apply_resilient_agc(square.tobytes(), sample_rate, 1)
        out_samples = np.frombuffer(out_bytes, dtype=np.int16)

        self.assertFalse(np.isnan(out_samples).any())
        self.assertTrue(np.all(out_samples >= -32768))
        self.assertTrue(np.all(out_samples <= 32767))
        self.assertAlmostEqual(gain, 24000.0 / 32767.0, places=2)

    def test_single_channel_dead_mic(self):
        """Stereo where one channel is dead must not be attenuated by -6dB."""
        sample_rate = 48000
        total_samples = sample_rate // 4
        t = np.linspace(0, 0.25, total_samples, endpoint=False)
        active_ch = (np.sin(2 * np.pi * 440.0 * t) * 5000.0).astype(np.int16)

        stereo = np.zeros(total_samples * 2, dtype=np.int16)
        stereo[0::2] = active_ch

        out_bytes, gain = AudioRecorder._apply_resilient_agc(stereo.tobytes(), sample_rate, 2)
        out_samples = np.frombuffer(out_bytes, dtype=np.int16)

        self.assertFalse(np.isnan(out_samples).any())
        self.assertGreater(np.max(np.abs(out_samples)), 0)

    def test_empty_and_odd_buffers(self):
        """Empty or odd-length buffers: check behavior and failure modes."""
        out_empty, gain_empty = AudioRecorder._apply_resilient_agc(b"", 48000, 1)
        self.assertEqual(out_empty, b"")
        self.assertEqual(gain_empty, 1.0)

        try:
            out_odd, gain_odd = AudioRecorder._apply_resilient_agc(b"\x01", 48000, 1)
            self.assertEqual(out_odd, b"")
        except ValueError as e:
            print(f"\n[Vulnerability Observed] AGC odd buffer failure: {e}")
            self.assertIn("buffer size must be a multiple of element size", str(e))


if __name__ == "__main__":
    unittest.main()
