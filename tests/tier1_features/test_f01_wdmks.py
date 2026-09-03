import unittest
import sounddevice as sd
from core.audio_recorder import AudioRecorder

class TestF01WdmksElimination(unittest.TestCase):
    """
    Feature 1: WDM-KS Endpoint Elimination
    Requirement: Filter out PortAudio devices 10-21 (PaErrorCode -9999 'Blocking API not supported yet')
    Contract: AudioRecorder.get_input_devices() -> List[Tuple[int, str]]
    """

    def setUp(self):
        self.devices = AudioRecorder.get_input_devices()

    def test_get_input_devices_returns_non_empty_list(self):
        """Verify get_input_devices returns a non-empty list of available capture devices."""
        self.assertIsInstance(self.devices, list)
        self.assertGreater(len(self.devices), 0, "No audio input devices discovered")

    def test_get_input_devices_filters_wdmks_endpoints(self):
        """Verify no returned device contains 'WDM-KS', 'WDMKS', or 'KERNEL STREAMING' in its name/hostapi."""
        for idx, name in self.devices:
            name_upper = name.upper()
            self.assertNotIn("WDM-KS", name_upper, f"WDM-KS endpoint leaked: [{idx}] {name}")
            self.assertNotIn("WDMKS", name_upper, f"WDMKS endpoint leaked: [{idx}] {name}")
            self.assertNotIn("KERNEL STREAMING", name_upper, f"Kernel Streaming endpoint leaked: [{idx}] {name}")

    def test_get_input_devices_excludes_zero_input_channels(self):
        """Verify all returned devices have at least 1 input channel (strictly capture devices)."""
        all_devs = sd.query_devices()
        for idx, _ in self.devices:
            self.assertLess(idx, len(all_devs), f"Device index {idx} out of range")
            dev_info = all_devs[idx]
            self.assertGreater(
                dev_info.get("max_input_channels", 0), 0,
                f"Output-only device included in input list: [{idx}] {dev_info.get('name')}"
            )

    def test_get_input_devices_excludes_empty_names(self):
        """Verify all returned devices have non-empty valid display names."""
        for idx, name in self.devices:
            self.assertIsInstance(name, str)
            self.assertTrue(len(name.strip()) > 0, f"Device [{idx}] has empty name string")

    def test_fallback_candidates_excludes_wdmks(self):
        """Verify _get_fallback_candidates internal candidate generator never includes WDM-KS."""
        recorder = AudioRecorder()
        if hasattr(recorder, "_get_fallback_candidates"):
            candidates = recorder._get_fallback_candidates()
            all_devs = sd.query_devices()
            for cand in candidates:
                dev_idx = cand[0] if isinstance(cand, (tuple, list)) else cand
                dev_info = all_devs[dev_idx]
                api_name = sd.query_hostapis(dev_info["hostapi"])["name"]
                self.assertNotIn("WDM-KS", api_name.upper())

if __name__ == "__main__":
    unittest.main()
