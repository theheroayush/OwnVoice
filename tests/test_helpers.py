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
        self.history = []
        if initial_config:
            self.config.update(initial_config)

    def get(self, key, default=None):
        return self.config.get(key, default)

    def set(self, key, value, save=True):
        self.config[key] = value

    def load_history(self):
        return list(self.history)

    def add_history_entry(self, entry):
        self.history.insert(0, entry)

    def clear_history(self):
        self.history = []

    def get_productivity_stats(self, note_store=None):
        return {
            "words_today": 1284,
            "dictations_today": 42,
            "time_saved_min": 18,
            "hours_saved_week": 3.2
        }

    def get_vocabulary(self):
        raw = self.config.get("vocabulary", ["Aarav", "Bengaluru", "Kubernetes"])
        return [item.get("term") if isinstance(item, dict) else item for item in raw]

    def get_structured_vocabulary(self):
        raw = self.config.get("vocabulary", [
            {"term": "Aarav", "type": "Name", "replacement": "Aarav"},
            {"term": "Bengaluru", "type": "Place", "replacement": "Bengaluru"},
            {"term": "Kubernetes", "type": "Technology", "replacement": "Kubernetes"}
        ])
        out = []
        for item in raw:
            if isinstance(item, dict):
                out.append(item)
            elif isinstance(item, str):
                out.append({"term": item, "type": "Jargon", "replacement": item})
        return out

    def add_vocabulary_term(self, term, term_type="Jargon", replacement=""):
        vocab = self.get_structured_vocabulary()
        for v in vocab:
            if v["term"].lower() == term.strip().lower():
                return False
        vocab.append({
            "term": term.strip(),
            "type": term_type.capitalize() if term_type else "Jargon",
            "replacement": replacement.strip() if replacement else term.strip()
        })
        self.config["vocabulary"] = vocab
        return True

    def remove_vocabulary_term(self, term):
        vocab = self.get_structured_vocabulary()
        orig_len = len(vocab)
        vocab = [v for v in vocab if v["term"].lower() != term.strip().lower()]
        if len(vocab) < orig_len:
            self.config["vocabulary"] = vocab
            return True
        return False


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
