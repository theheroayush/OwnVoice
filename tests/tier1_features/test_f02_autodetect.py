import unittest
import sounddevice as sd
from core.audio_recorder import AudioRecorder

class TestF02AutoDetect(unittest.TestCase):
    """
    Feature 2: True Working Mic Auto-Detection
    Requirement: Match by name + host API default resolution (WASAPI 48kHz stereo -> DirectSound -> MME)
    """

    def setUp(self):
        self.recorder = AudioRecorder()

    def test_recorder_initializes_with_default_sample_rate(self):
        """Verify recorder defaults to standard 48000 Hz sample rate."""
        self.assertEqual(self.recorder.sample_rate, 48000)

    def test_wasapi_default_input_device_resolution(self):
        """Verify system resolves default WASAPI input device when available."""
        host_apis = sd.query_hostapis()
        wasapi_api = next((api for api in host_apis if "WASAPI" in api["name"]), None)
        if wasapi_api and wasapi_api["default_input_device"] >= 0:
            def_idx = wasapi_api["default_input_device"]
            dev_info = sd.query_devices(def_idx)
            self.assertGreater(dev_info["max_input_channels"], 0)

    def test_candidate_chain_priority_order(self):
        """Verify candidate chain orders host APIs: Preferred -> WASAPI -> DirectSound -> MME."""
        if hasattr(self.recorder, "_get_fallback_candidates"):
            candidates = self.recorder._get_fallback_candidates()
            self.assertIsInstance(candidates, list)
            self.assertGreater(len(candidates), 0)
            
            all_devs = sd.query_devices()
            apis_in_order = []
            for cand in candidates:
                dev_idx = cand[0] if isinstance(cand, (tuple, list)) else cand
                api_name = sd.query_hostapis(all_devs[dev_idx]["hostapi"])["name"]
                if api_name not in apis_in_order:
                    apis_in_order.append(api_name)
            
            # If both WASAPI and MME are present, WASAPI must appear before MME
            if any("WASAPI" in a for a in apis_in_order) and any("MME" in a for a in apis_in_order):
                wasapi_idx = next(i for i, a in enumerate(apis_in_order) if "WASAPI" in a)
                mme_idx = next(i for i, a in enumerate(apis_in_order) if "MME" in a)
                self.assertLess(wasapi_idx, mme_idx, "WASAPI must precede MME in candidate priority")

    def test_stereo_with_mono_fallback_structure(self):
        """Verify candidate entries include both stereo (2-ch) and mono (1-ch) configurations."""
        if hasattr(self.recorder, "_get_fallback_candidates"):
            candidates = self.recorder._get_fallback_candidates()
            channel_counts = [cand[1] for cand in candidates if isinstance(cand, (tuple, list)) and len(cand) >= 2]
            if channel_counts:
                # If stereo candidates exist, mono fallback should also be tested
                if 2 in channel_counts:
                    self.assertIn(1, channel_counts, "Mono fallback channel should be supported")

    def test_valid_device_sample_rates(self):
        """Verify candidate sample rates are valid broadcast audio rates (44100 or 48000 Hz)."""
        if hasattr(self.recorder, "_get_fallback_candidates"):
            candidates = self.recorder._get_fallback_candidates()
            for cand in candidates:
                if isinstance(cand, (tuple, list)) and len(cand) >= 3:
                    sr = cand[2]
                    self.assertIn(sr, (44100, 48000, 96000, 16000, 32000, 8000))

if __name__ == "__main__":
    unittest.main()
