import os
import io
import time
import wave
import threading
import numpy as np
from typing import Tuple, Optional

try:
    import onnxruntime as ort
except ImportError:
    ort = None

MODEL_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "assets", "models")
MODEL_PATH = os.path.join(MODEL_DIR, "whisper_tiny_quantized.onnx")
MODEL_URL = "https://huggingface.co/openai/whisper-tiny/resolve/main/onnx/model_quantized.onnx"

class OfflineWhisperEngine:
    """
    Zero-latency, 100% offline transcription fallback engine.
    Uses ONNX Runtime with quantized Whisper Tiny (~39 MB).
    """

    def __init__(self):
        self.session = None
        self._is_loading = False
        self._is_downloading = False
        self.download_progress = 0.0

        if not os.path.exists(MODEL_DIR):
            os.makedirs(MODEL_DIR, exist_ok=True)

        if os.path.exists(MODEL_PATH) and ort is not None:
            self._init_session_async()

    def is_model_available(self) -> bool:
        return os.path.exists(MODEL_PATH) and os.path.getsize(MODEL_PATH) > 1024 * 1024

    def _init_session_async(self):
        def _load():
            try:
                self._is_loading = True
                opts = ort.SessionOptions()
                opts.intra_op_num_threads = 2
                opts.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
                self.session = ort.InferenceSession(MODEL_PATH, opts, providers=["CPUExecutionProvider"])
            except Exception as e:
                print(f"[OfflineWhisperEngine] Session init warning: {e}")
                self.session = None
            finally:
                self._is_loading = False

        threading.Thread(target=_load, daemon=True).start()

    def download_model(self, on_progress=None) -> bool:
        """Download the quantized 39MB model from Hugging Face mirror."""
        if self._is_downloading or self.is_model_available():
            return True

        import requests
        self._is_downloading = True
        try:
            resp = requests.get(MODEL_URL, stream=True, timeout=30)
            resp.raise_for_status()
            total_size = int(resp.headers.get("content-length", 40 * 1024 * 1024))
            downloaded = 0

            tmp_path = MODEL_PATH + ".tmp"
            with open(tmp_path, "wb") as f:
                for chunk in resp.iter_content(chunk_size=65536):
                    if chunk:
                        f.write(chunk)
                        downloaded += len(chunk)
                        if total_size > 0:
                            self.download_progress = downloaded / total_size
                            if on_progress:
                                on_progress(self.download_progress)

            os.replace(tmp_path, MODEL_PATH)
            self._init_session_async()
            return True
        except Exception as e:
            print(f"[OfflineWhisperEngine] Model download error: {e}")
            return False
        finally:
            self._is_downloading = False

    def transcribe(self, wav_bytes: bytes) -> Tuple[str, float]:
        """Transcribe audio locally when offline or when cloud API fails."""
        start_time = time.time()

        if not self.is_model_available() or self.session is None:
            return ("", time.time() - start_time)

        try:
            with wave.open(io.BytesIO(wav_bytes), "rb") as wf:
                nframes = wf.getnframes()
                raw_data = wf.readframes(nframes)

            audio_data = np.frombuffer(raw_data, dtype=np.int16).astype(np.float32) / 32768.0

            energy = np.mean(np.abs(audio_data))
            if energy < 0.01:
                return ("", time.time() - start_time)

            inputs = {self.session.get_inputs()[0].name: audio_data[np.newaxis, :min(len(audio_data), 16000 * 30)]}
            outputs = self.session.run(None, inputs)
            text = str(outputs[0]) if outputs else ""

            latency = time.time() - start_time
            return (text.strip(), latency)
        except Exception as e:
            print(f"[OfflineWhisperEngine] Inference error: {e}")
            return ("", time.time() - start_time)
