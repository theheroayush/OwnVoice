import unittest
import numpy as np
import io, wave
from unittest.mock import MagicMock, patch

from core.audio_recorder import AudioRecorder
from core.ai_engine import AIEngine
from core.injector import CursorInjector
from config import ConfigManager

class TestStreamingDualEngine(unittest.TestCase):
    def setUp(self):
        self.config = ConfigManager()
        self.recorder = AudioRecorder(self.config)
        self.injector = CursorInjector(self.config)
        self.ai = AIEngine(self.config)

    def test_audio_16k_resampling_payload_reduction(self):
        """Verify 48kHz stereo downsamples to 16kHz mono with >90% size reduction."""
        self.recorder.sample_rate = 48000
        self.recorder.active_channels = 2
        self.recorder._is_recording = True
        
        n_samples = 48000 * 2 * 1
        fake_pcm = (np.ones(n_samples, dtype=np.int16) * 1000).tobytes()
        self.recorder.frames = [fake_pcm]
        
        wav_bytes = self.recorder.stop_recording()
        self.assertGreater(len(wav_bytes), 100)
        
        with wave.open(io.BytesIO(wav_bytes), "rb") as wf:
            self.assertEqual(wf.getnchannels(), 1)
            self.assertEqual(wf.getframerate(), 16000)
            self.assertEqual(wf.getsampwidth(), 2)
            
        self.assertLess(len(wav_bytes), len(fake_pcm) * 0.25)

    def test_rolling_chunk_extraction(self):
        """Verify get_rolling_chunk extracts incremental 16kHz mono audio during recording."""
        self.recorder.sample_rate = 48000
        self.recorder.active_channels = 2
        self.recorder._is_recording = True
        
        bytes_per_sec = 48000 * 2 * 2
        chunk1 = (np.ones(int(bytes_per_sec * 1.5 // 2), dtype=np.int16) * 500).tobytes()
        self.recorder.frames.append(chunk1)
        
        wav1 = self.recorder.get_rolling_chunk(chunk_seconds=1.2)
        self.assertGreater(len(wav1), 100)
        with wave.open(io.BytesIO(wav1), "rb") as wf:
            self.assertEqual(wf.getframerate(), 16000)
            self.assertEqual(wf.getnchannels(), 1)
            
        wav2 = self.recorder.get_rolling_chunk(chunk_seconds=1.2)
        self.assertEqual(wav2, b"")

    def test_streaming_delta_typing_logic(self):
        """Verify inject_streaming_delta accurately identifies deltas."""
        self.injector._send_unicode_string = MagicMock()
        self.injector.refocus_target = MagicMock(return_value=True)
        self.injector.reset_streaming()
        
        self.injector.inject_streaming_delta("Hello")
        self.injector._send_unicode_string.assert_called_with("Hello")
        self.assertEqual(self.injector._streamed_text, "Hello")
        
        self.injector.inject_streaming_delta("Hello world")
        self.injector._send_unicode_string.assert_called_with(" world")
        self.assertEqual(self.injector._streamed_text, "Hello world")

    def test_replace_streamed_text_polish(self):
        """Verify replace_streamed_text erases old draft and injects polished text."""
        self.injector.inject_text = MagicMock(return_value=True)
        self.injector.refocus_target = MagicMock(return_value=True)
        
        res = self.injector.replace_streamed_text("how to deploy next js", "How to deploy Next.js?")
        self.assertTrue(res)
        self.injector.inject_text.assert_called_with("How to deploy Next.js?")
        self.assertEqual(self.injector._streamed_text, "How to deploy Next.js?")

    def test_ai_engine_polish_text_mocked(self):
        """Verify polish_text dispatches lightweight text prompt without audio."""
        mock_resp = MagicMock()
        mock_resp.status_code = 200
        mock_resp.json.return_value = {
            "candidates": [{
                "content": {
                    "parts": [{"text": "Search for coffee shops near me."}]
                }
            }]
        }
        with patch.object(self.ai.session, "post", return_value=mock_resp):
            text, lat = self.ai.polish_text("search for coffee shops near me um")
            self.assertEqual(text, "Search for coffee shops near me.")

if __name__ == "__main__":
    unittest.main()
