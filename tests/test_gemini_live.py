import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import time
import wave
import numpy as np
from config import config_manager
from core.gemini_live_client import GeminiLiveClient

def main():
    api_key = config_manager.get('google_api_key', '')
    client = GeminiLiveClient(api_key=api_key)

    print("Connecting to Gemini Live WebSocket...")
    ok = client.start_session("You are an expert real-time voice dictation assistant. Transcribe cleanly with no filler words.")
    print("Session start result:", ok)
    if not ok:
        print("Error:", client.get_last_error())
        return

    with wave.open("test_loopback_recording.wav", "rb") as wf:
        sr = wf.getframerate()
        nch = wf.getnchannels()
        raw = wf.readframes(wf.getnframes())

    samples = np.frombuffer(raw, dtype=np.int16)
    if nch == 2:
        samples = samples[::2]
    new_len = int(len(samples) * 16000 / sr)
    samples_16k = np.interp(np.linspace(0, len(samples)-1, new_len), np.arange(len(samples)), samples).astype(np.int16)
    pcm = samples_16k.tobytes()

    chunk_size = 3200 # 100ms chunks
    t_start = time.time()
    for i in range(0, len(pcm), chunk_size):
        client.send_audio_chunk(pcm[i:i+chunk_size])
        time.sleep(0.005)

    print(f"Sent {len(pcm)} bytes in {time.time()-t_start:.3f}s. Fetching final transcript...")
    t_fetch = time.time()
    text, lat = client.finish_and_get_transcript(timeout=2.5)
    fetch_time = time.time() - t_fetch
    print(f"Result: '{text}'")
    print(f"Wait after finish: {fetch_time:.3f}s, Total Session Latency: {lat:.3f}s")

if __name__ == "__main__":
    main()
