import io
import wave
import math
import struct
import threading
import sounddevice as sd

class AudioRecorder:
    def __init__(self, sample_rate=48000):
        self.sample_rate = sample_rate
        self.is_recording = False
        self.frames = []
        self.stream = None
        self.lock = threading.Lock()
        self.current_volume = 0.0

    def _find_wasapi_device(self):
        try:
            for idx, dev in enumerate(sd.query_devices()):
                if dev.get("max_input_channels", 0) > 0:
                    hostapi = sd.query_hostapis(dev["hostapi"])["name"]
                    if "WASAPI" in hostapi:
                        return idx, int(dev.get("default_samplerate", 48000))
        except Exception:
            pass
        return 9, 48000

    def _callback(self, indata, frame_count, time_info, status):
        if self.is_recording:
            raw_bytes = bytes(indata)
            with self.lock:
                self.frames.append(raw_bytes)
            
            count = len(raw_bytes) // 2
            if count > 0:
                shorts = struct.unpack(f"<{count}h", raw_bytes[:count*2])
                sum_sq = sum(s * s for s in shorts)
                rms = math.sqrt(sum_sq / count)
                self.current_volume = min(1.0, float(rms * 0.02))

    def start_recording(self, device_index=None):
        with self.lock:
            if self.is_recording:
                return
            self.frames = []
            self.is_recording = True
            self.current_volume = 0.0

        if device_index is None:
            dev_idx, sr = self._find_wasapi_device()
        else:
            dev_idx = device_index
            try:
                sr = int(sd.query_devices(dev_idx).get("default_samplerate", 48000))
            except Exception:
                sr = 48000

        self.sample_rate = sr
        self.device_index = dev_idx

        # Open stream on WASAPI endpoint (2-channel stereo @ 48kHz)
        try:
            self.stream = sd.RawInputStream(
                samplerate=self.sample_rate,
                channels=2,
                dtype="int16",
                device=self.device_index,
                callback=self._callback,
                blocksize=2048
            )
            self.stream.start()
        except Exception:
            self.stream = sd.RawInputStream(
                samplerate=self.sample_rate,
                channels=1,
                dtype="int16",
                device=self.device_index,
                callback=self._callback,
                blocksize=2048
            )
            self.stream.start()

    def stop_recording(self) -> bytes:
        with self.lock:
            if not self.is_recording:
                return b""
            self.is_recording = False
            self.current_volume = 0.0

        if self.stream:
            try:
                self.stream.stop()
                self.stream.close()
            except Exception:
                pass
            self.stream = None

        with self.lock:
            if not self.frames:
                return b""
            raw_audio = b"".join(self.frames)

        total_samples = len(raw_audio) // 2
        if total_samples == 0:
            return b""

        shorts = struct.unpack(f"<{total_samples}h", raw_audio)
        
        # Downmix stereo to mono
        mono_samples = []
        for i in range(0, len(shorts) - 1, 2):
            mono_samples.append((shorts[i] + shorts[i+1]) // 2)

        if not mono_samples:
            mono_samples = list(shorts)

        peak_orig = max(abs(s) for s in mono_samples)
        
        # 50x Dynamic Auto-Gain Normalization
        if peak_orig > 2:
            target_peak = 24000
            gain = min(50.0, target_peak / max(1, peak_orig))
            boosted = [max(-32768, min(32767, int(s * gain))) for s in mono_samples]
        else:
            boosted = mono_samples

        wav_buf = io.BytesIO()
        with wave.open(wav_buf, "wb") as wf:
            wf.setnchannels(1)
            wf.setsampwidth(2)
            wf.setframerate(self.sample_rate)
            wf.writeframes(struct.pack(f"<{len(boosted)}h", *boosted))

        return wav_buf.getvalue()

    def get_current_volume(self) -> float:
        return self.current_volume

    @staticmethod
    def get_input_devices():
        devices = []
        try:
            for idx, dev in enumerate(sd.query_devices()):
                if dev.get("max_input_channels", 0) > 0:
                    hostapi = sd.query_hostapis(dev["hostapi"])["name"]
                    devices.append((idx, f"[{hostapi}] {dev['name']}"))
        except Exception:
            pass
        return devices
