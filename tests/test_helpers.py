import os
import sys
import math
import struct
import tempfile
import numpy as np

class InMemoryConfigManager:
    """Isolated, in-memory config manager that does not alter production config.json."""
    def __init__(self, initial_config=None):
        self.config = {
            "google_api_key": "test_api_key_12345",
            "model": "gemini-3.5-transcribe",
            "hotkey": "f8",
            "hotkey_mode": "toggle",
            "dictation_mode": "smart_flow",
            "custom_instructions": "",
            "sound_effects": False,
            "input_device_index": None,
            "overlay_position": "bottom_center",
            "auto_paste": True,
            "save_history": False,
            "history_limit": 100,
            "theme": "dark",
            "snippets": {
                "my email": "ayush@example.com",
                "meeting link": "https://cal.com/ayush",
                "sign off": "Best regards,\nAyush Upadhyay",
                "c++": "std::vector<int>",
                "node.js": "const express = require('express');",
                "c#": "Console.WriteLine();"
            }
        }
        if initial_config:
            self.config.update(initial_config)

    def get(self, key, default=None):
        return self.config.get(key, default)

    def set(self, key, value, save=True):
        self.config[key] = value

    def load_history(self):
        return []

    def add_history_entry(self, entry):
        pass

    def clear_history(self):
        pass


def generate_pcm_audio(duration_sec=1.0, sample_rate=48000, channels=1, amplitude=5000, freq=440.0):
    """Generates 16-bit signed PCM byte stream for testing."""
    total_samples = int(duration_sec * sample_rate)
    samples = []
    for i in range(total_samples):
        val = int(amplitude * math.sin(2 * math.pi * freq * i / sample_rate))
        val = max(-32768, min(32767, val))
        for _ in range(channels):
            samples.append(val)
    return struct.pack(f"<{len(samples)}h", *samples)


def generate_pcm_silence(duration_sec=1.0, sample_rate=48000, channels=1):
    """Generates 16-bit silence PCM byte stream."""
    total_samples = int(duration_sec * sample_rate) * channels
    return b"\x00" * (total_samples * 2)


def generate_pcm_spike(duration_sec=1.0, sample_rate=48000, speech_amplitude=200, spike_amplitude=16000, channels=1):
    """Generates quiet speech PCM buffer with a single impulsive transient spike."""
    total_samples = int(duration_sec * sample_rate)
    np.random.seed(42)
    signal = (np.random.randn(total_samples) * speech_amplitude).astype(np.int16)
    # Insert transient spike at sample 500
    spike_idx = min(500, total_samples - 5)
    signal[spike_idx:spike_idx + 4] = spike_amplitude
    return signal.tobytes()


def generate_stereo_dc_offset(duration_sec=0.5, sample_rate=48000, dc_offset=5000):
    """Generates stereo PCM with strong DC bias."""
    total_samples = int(duration_sec * sample_rate)
    samples = []
    for _ in range(total_samples):
        # Ch0 and Ch1 both have DC offset
        samples.append(dc_offset)
        samples.append(dc_offset)
    return struct.pack(f"<{len(samples)}h", *samples)


def generate_out_of_phase_stereo(duration_sec=0.5, sample_rate=48000, amplitude=5000, freq=440.0):
    """Generates stereo audio where Ch0 and Ch1 are 180 degrees out of phase."""
    total_samples = int(duration_sec * sample_rate)
    samples = []
    for i in range(total_samples):
        s0 = int(amplitude * math.sin(2 * math.pi * freq * i / sample_rate))
        s1 = -s0  # 180 degrees inverted
        samples.append(s0)
        samples.append(s1)
    return struct.pack(f"<{len(samples)}h", *samples)
