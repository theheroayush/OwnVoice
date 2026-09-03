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
        self.active_device_index = None

    @staticmethod
    def get_input_devices():
        """Returns safe, validated input devices, strictly excluding broken WDM-KS."""
        devices = []
        try:
            for idx, dev in enumerate(sd.query_devices()):
                if dev.get("max_input_channels", 0) > 0:
                    api = sd.query_hostapis(dev["hostapi"])["name"]
                    # Strictly filter out unstable WDM-KS kernel streaming
                    if "WDM-KS" in api:
                        continue
                    devices.append((idx, f"[{api}] {dev['name']}"))
        except Exception:
            pass
        return devices

    def _get_fallback_candidates(self, preferred_index=None):
        """
        Builds prioritized list of (device_index, channels, samplerate):
        1. Preferred device (if not WDM-KS)
        2. WASAPI Microphone Array @ 48kHz
        3. DirectSound Microphone Array @ 44.1kHz
        4. MME Microsoft Sound Mapper @ 44.1kHz
        5. Any working non-WDM-KS input device
        """
        candidates = []
        devices = sd.query_devices()

        def add_cand(idx):
            if idx is not None and 0 <= idx < len(devices):
                d = devices[idx]
                api = sd.query_hostapis(d["hostapi"])["name"]
                if "WDM-KS" not in api and d.get("max_input_channels", 0) > 0:
                    sr = int(d.get("default_samplerate", 48000))
                    ch = min(2, d.get("max_input_channels", 1))
                    candidates.append((idx, ch, sr))
                    if ch > 1:
                        candidates.append((idx, 1, sr))

        # 1. Preferred
        add_cand(preferred_index)

        # 2. WASAPI Microphone
        for idx, d in enumerate(devices):
            api = sd.query_hostapis(d["hostapi"])["name"]
            if "WASAPI" in api and "Microphone" in d["name"]:
                add_cand(idx)

        # 3. DirectSound
        for idx, d in enumerate(devices):
            api = sd.query_hostapis(d["hostapi"])["name"]
            if "DirectSound" in api and "Microphone" in d["name"]:
                add_cand(idx)

        # 4. MME / Mapper
        for idx, d in enumerate(devices):
            api = sd.query_hostapis(d["hostapi"])["name"]
            if "MME" in api and d.get("max_input_channels", 0) > 0:
                add_cand(idx)

        # 5. Any remaining valid input
        for idx, d in enumerate(devices):
            add_cand(idx)

        # Deduplicate while preserving priority order
        seen = set()
        unique = []
        for c in candidates:
            key = (c[0], c[1], c[2])
            if key not in seen:
                seen.add(key)
                unique.append(c)

        return unique

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
            self.current_volume = 0.0

        candidates = self._get_fallback_candidates(device_index)
        last_err = None
        started = False

        for dev_idx, ch, sr in candidates:
            try:
                self.stream = sd.RawInputStream(
                    samplerate=sr,
                    channels=ch,
                    dtype="int16",
                    device=dev_idx,
                    callback=self._callback,
                    blocksize=2048
                )
                self.stream.start()
                self.sample_rate = sr
                self.active_channels = ch
                self.active_device_index = dev_idx
                self.is_recording = True
                started = True
                break
            except Exception as e:
                last_err = e
                if self.stream:
                    try:
                        self.stream.close()
                    except Exception:
                        pass
                    self.stream = None

        if not started:
            self.is_recording = False
            raise RuntimeError(f"Unable to access any microphone: {last_err}")

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
        
        # Downmix if stereo
        if getattr(self, "active_channels", 2) == 2:
            mono_samples = []
            for i in range(0, len(shorts) - 1, 2):
                mono_samples.append((shorts[i] + shorts[i+1]) // 2)
            if not mono_samples:
                mono_samples = list(shorts)
        else:
            mono_samples = list(shorts)

        peak_orig = max(abs(s) for s in mono_samples) if mono_samples else 0
        
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
