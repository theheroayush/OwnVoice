import asyncio
import base64
import json
import logging
import threading
import time
from typing import Optional, Tuple

import websockets

logger = logging.getLogger("ownvoice.gemini_live")

DEFAULT_LIVE_MODEL = "models/gemini-2.0-flash"
LIVE_WS_BASE = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"


class GeminiLiveClient:
    """
    High-performance real-time WebSocket client for Google Gemini Live API.
    Enables low-latency streaming speech-to-text dictation by streaming audio
    chunks continuously while the user speaks.
    """

    def __init__(self, api_key: str, model: str = DEFAULT_LIVE_MODEL):
        self.api_key = api_key.strip()
        self.model = model
        self._loop: Optional[asyncio.AbstractEventLoop] = None
        self._thread: Optional[threading.Thread] = None
        self._ws = None
        self._is_active = False
        self._is_ready = threading.Event()
        self._transcript_done = threading.Event()
        self._interim_text = ""
        self._final_text = ""
        self._start_time = 0.0
        self._error: Optional[str] = None
        self._lock = threading.Lock()

        # Dedicated background async event loop thread
        self._init_loop_thread()

    def _init_loop_thread(self):
        def run_loop():
            self._loop = asyncio.new_event_loop()
            asyncio.set_event_loop(self._loop)
            self._loop.run_forever()

        self._thread = threading.Thread(target=run_loop, name="GeminiLiveClientLoop", daemon=True)
        self._thread.start()
        # Wait briefly for loop to initialize
        while self._loop is None:
            time.sleep(0.005)

    def is_connected(self) -> bool:
        if not self._is_active or self._ws is None:
            return False
        if getattr(self._ws, "closed", False):
            return False
        return True

    def start_session(self, system_instruction: str, timeout: float = 3.0) -> bool:
        """
        Initiates the WebSocket connection and setup handshake.
        Runs asynchronously without blocking the UI mainloop.
        """
        if not self.api_key:
            self._is_active = False
            self._transcript_done.set()
            return False

        with self._lock:
            self._is_ready.clear()
            self._transcript_done.clear()
            self._interim_text = ""
            self._final_text = ""
            self._error = None
            self._start_time = time.time()

        future = asyncio.run_coroutine_threadsafe(
            self._async_connect_and_setup(system_instruction),
            self._loop
        )

        try:
            ok = future.result(timeout=timeout)
            if not ok:
                self._is_active = False
                self._transcript_done.set()
                self.cancel()
            return bool(ok)
        except Exception as e:
            logger.warning(f"Gemini Live session start failed: {e}")
            self._error = str(e)
            self._is_active = False
            self._transcript_done.set()
            self.cancel()
            return False

    async def _async_connect_and_setup(self, system_instruction: str) -> bool:
        uri = f"{LIVE_WS_BASE}?key={self.api_key}"
        try:
            self._ws = await websockets.connect(
                uri,
                ping_interval=20,
                ping_timeout=10,
                max_size=10 * 1024 * 1024
            )

            setup_msg = {
                "setup": {
                    "model": self.model,
                    "generationConfig": {
                        "responseModalities": ["TEXT"]
                    },
                    "systemInstruction": {
                        "parts": [{"text": system_instruction}]
                    }
                }
            }
            await self._ws.send(json.dumps(setup_msg))

            # Await setupComplete confirmation
            resp_raw = await asyncio.wait_for(self._ws.recv(), timeout=3.0)
            data = json.loads(resp_raw)
            if "setupComplete" in data:
                self._is_active = True
                self._is_ready.set()
                # Launch background message listener
                self._loop.create_task(self._async_listen_loop())
                return True
            else:
                self._error = f"Unexpected setup response: {resp_raw[:150]}"
                self._is_active = False
                self._transcript_done.set()
                return False
        except Exception as e:
            self._error = str(e)
            self._is_active = False
            self._transcript_done.set()
            return False

    async def _async_listen_loop(self):
        """Continuously reads incoming server frames for transcripts."""
        try:
            while self._is_active and self._ws is not None:
                msg_raw = await self._ws.recv()
                data = json.loads(msg_raw)
                sc = data.get("serverContent", {})

                # 1. Interim real-time transcription
                if "interimInputTranscription" in sc:
                    t = sc["interimInputTranscription"].get("text", "")
                    if t:
                        self._interim_text = t

                # 2. Final input transcription
                if "inputTranscription" in sc:
                    t = sc["inputTranscription"].get("text", "")
                    if t:
                        self._final_text = t

                # 3. Model turn generation parts (if model output text is used)
                model_turn = sc.get("modelTurn", {})
                parts = model_turn.get("parts", [])
                for p in parts:
                    if "text" in p and p["text"]:
                        # Append or set model text
                        if not self._final_text:
                            self._final_text = p["text"]
                        else:
                            self._final_text += p["text"]

                # 4. Turn completion / generation complete
                if sc.get("turnComplete") or sc.get("generationComplete"):
                    self._transcript_done.set()

        except websockets.exceptions.ConnectionClosed as e:
            logger.debug(f"Gemini Live connection closed: {e}")
            self._is_active = False
            self._error = f"Connection closed: {e}"
            self._transcript_done.set()
        except Exception as e:
            logger.debug(f"Gemini Live receiver exception: {e}")
            self._is_active = False
            self._error = str(e)
            self._transcript_done.set()
        finally:
            self._is_active = False
            self._transcript_done.set()

    def send_audio_chunk(self, pcm_16k_mono_bytes: bytes) -> bool:
        """
        Sends a single PCM 16kHz mono audio chunk to the live stream.
        Thread-safe and non-blocking.
        """
        if not self._is_active or self._ws is None:
            return False

        b64_audio = base64.b64encode(pcm_16k_mono_bytes).decode("utf-8")
        msg = {
            "realtimeInput": {
                "mediaChunks": [
                    {
                        "mimeType": "audio/pcm;rate=16000",
                        "data": b64_audio
                    }
                ]
            }
        }

        asyncio.run_coroutine_threadsafe(
            self._async_send(json.dumps(msg)),
            self._loop
        )
        return True

    async def _async_send(self, payload: str):
        if self._ws and self._is_active:
            try:
                await self._ws.send(payload)
            except Exception as e:
                logger.debug(f"Gemini Live send exception: {e}")
                self._is_active = False
                self._error = str(e)
                self._transcript_done.set()

    def finish_and_get_transcript(self, timeout: float = 2.0) -> Tuple[str, float]:
        """
        Flushes end of user turn, waits for final transcription, and returns (text, latency).
        """
        if not self._is_active or self._ws is None or getattr(self._ws, "closed", False) or self._error:
            self._transcript_done.set()
            result_text = self._final_text.strip() or self._interim_text.strip()
            self.cancel()
            return result_text, (time.time() - self._start_time if self._start_time > 0 else 0.0)

        # Send turnComplete signal
        end_msg = {"clientContent": {"turns": [], "turnComplete": True}}
        asyncio.run_coroutine_threadsafe(
            self._async_send(json.dumps(end_msg)),
            self._loop
        )

        # Wait for server to signal turnComplete or emit final transcription
        self._transcript_done.wait(timeout=timeout)
        latency = time.time() - self._start_time if self._start_time > 0 else 0.0

        result_text = self._final_text.strip() or self._interim_text.strip()

        # Cleanly terminate session
        self.cancel()
        return result_text, latency

    def cancel(self):
        """Immediately closes the WebSocket session and resets state."""
        self._is_active = False
        self._transcript_done.set()
        if self._ws and self._loop:
            asyncio.run_coroutine_threadsafe(self._async_close(), self._loop)

    async def _async_close(self):
        if self._ws:
            try:
                await self._ws.close()
            except Exception:
                pass
            self._ws = None

    def get_last_error(self) -> Optional[str]:
        return self._error
