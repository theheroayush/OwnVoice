"""Sandbox-only fake `sounddevice` module (Windows-like device topology).

Used to execute the OwnVoice test suite in a headless Linux container.
It is NOT part of the repository; it is injected via PYTHONPATH.
"""
import math
import struct
import threading
import time

__version__ = "0.5.1"
PortAudioError = type("PortAudioError", (Exception,), {})


class _Device(dict):
    """Mimics sounddevice's DeviceList entries (dict subclass)."""


_HOSTAPIS = [
    {"name": "MME", "devices": [0, 1, 2], "default_input_device": 0, "default_output_device": 1},
    {"name": "Windows DirectSound", "devices": [3, 4, 5], "default_input_device": 3, "default_output_device": 4},
    {"name": "Windows WASAPI", "devices": [6, 7, 8], "default_input_device": 6, "default_output_device": 7},
    {"name": "Windows WDM-KS", "devices": [9, 10, 11], "default_input_device": -1, "default_output_device": -1},
]

_DEVICES = [
    # MME
    {"name": "Microphone (Realtek(R) Audio)", "hostapi": 0, "max_input_channels": 2, "max_output_channels": 0, "default_samplerate": 44100.0},
    {"name": "Speakers (Realtek(R) Audio)", "hostapi": 0, "max_input_channels": 0, "max_output_channels": 2, "default_samplerate": 44100.0},
    {"name": "Microphone (Qualcomm Aqstic)", "hostapi": 0, "max_input_channels": 2, "max_output_channels": 0, "default_samplerate": 44100.0},
    # DirectSound
    {"name": "Primary Sound Capture Driver", "hostapi": 1, "max_input_channels": 2, "max_output_channels": 0, "default_samplerate": 44100.0},
    {"name": "Primary Sound Driver", "hostapi": 1, "max_input_channels": 0, "max_output_channels": 2, "default_samplerate": 44100.0},
    {"name": "Microphone (USB Audio)", "hostapi": 1, "max_input_channels": 1, "max_output_channels": 0, "default_samplerate": 44100.0},
    # WASAPI
    {"name": "Microphone Array (Qualcomm Aqstic)", "hostapi": 2, "max_input_channels": 2, "max_output_channels": 0, "default_samplerate": 48000.0},
    {"name": "Speakers (Qualcomm Aqstic)", "hostapi": 2, "max_input_channels": 0, "max_output_channels": 2, "default_samplerate": 48000.0},
    {"name": "Headset Microphone (Bluetooth)", "hostapi": 2, "max_input_channels": 1, "max_output_channels": 0, "default_samplerate": 48000.0},
    # WDM-KS (broken kernel streaming endpoints that must be filtered out)
    {"name": "Microphone (WDM-KS)", "hostapi": 3, "max_input_channels": 2, "max_output_channels": 0, "default_samplerate": 48000.0},
    {"name": "Speakers (WDM-KS)", "hostapi": 3, "max_input_channels": 0, "max_output_channels": 2, "default_samplerate": 48000.0},
    {"name": "", "hostapi": 3, "max_input_channels": 2, "max_output_channels": 0, "default_samplerate": 48000.0},
]

# Failure injection knobs used by the tests / harness
FAIL_DEVICES = set()
OPEN_CALLS = []


def query_devices(device=None, kind=None):
    if device is None:
        return [_Device(d) for d in _DEVICES]
    if isinstance(device, int):
        if device < 0 or device >= len(_DEVICES):
            raise PortAudioError("Error querying device -1")
        return _Device(_DEVICES[device])
    for d in _DEVICES:
        if d["name"] == device:
            return _Device(d)
    raise PortAudioError("No device found matching %r" % (device,))


def query_hostapis(index=None):
    if index is None:
        return [dict(h) for h in _HOSTAPIS]
    return dict(_HOSTAPIS[index])


def _default_device(kind):
    apis = query_hostapis()
    preferred = {"input": 2, "output": 2}
    api = apis[preferred[kind]]
    idx = api["default_%s_device" % kind]
    if idx >= 0:
        return idx
    for i, d in enumerate(_DEVICES):
        if d["max_%s_channels" % kind] > 0:
            return i
    return -1


def default():
    return _Device({
        "name": "default",
        "hostapi": -1,
        "max_input_channels": 2,
        "max_output_channels": 2,
        "default_samplerate": 48000.0,
    })


def _terminate():
    return None


def _initialize():
    return None


class RawInputStream:
    """Stand-in stream. Feeds a 440 Hz sine wave into the callback."""

    def __init__(self, samplerate=None, channels=1, dtype="int16", device=None, callback=None,
                 blocksize=0, **kwargs):
        if device is None:
            device = _default_device("input")
        if isinstance(device, int) and (device < 0 or device >= len(_DEVICES)) and device != -1:
            raise PortAudioError("Error opening RawInputStream: Invalid device")
        if isinstance(device, str) and not any(d["name"] == device for d in _DEVICES):
            raise PortAudioError("Error opening RawInputStream: Invalid device")
        if device in FAIL_DEVICES:
            raise PortAudioError("Error opening RawInputStream: Device unavailable")
        OPEN_CALLS.append({"device": device, "samplerate": samplerate, "channels": channels,
                           "blocksize": blocksize})

        self.device = device
        self.samplerate = float(samplerate or 48000)
        self.channels = int(channels)
        self.blocksize = int(blocksize or 1024)
        self.dtype = dtype
        self.callback = callback
        self._active = False
        self._thread = None
        self._stop_evt = threading.Event()
        self._counter = 0

    @property
    def active(self):
        return self._active

    def _run(self):
        while not self._stop_evt.is_set():
            n = self.blocksize
            buf = bytearray()
            for i in range(n):
                val = int(12000 * math.sin(2 * math.pi * 440.0 * (self._counter + i) / self.samplerate))
                for _ in range(self.channels):
                    buf += struct.pack("<h", val)
            self._counter += n
            if self.callback:
                try:
                    import numpy as np
                    arr = np.frombuffer(bytes(buf), dtype=np.int16).reshape(-1, self.channels)
                    self.callback(arr, n, None, None)
                except Exception:
                    pass
            time.sleep(n / self.samplerate)

    def start(self):
        if not self._active:
            self._stop_evt.clear()
            self._active = True
            self._thread = threading.Thread(target=self._run, daemon=True)
            self._thread.start()

    def stop(self):
        self._active = False
        self._stop_evt.set()

    def close(self):
        self.stop()

    def __enter__(self):
        self.start()
        return self

    def __exit__(self, *exc):
        self.close()
