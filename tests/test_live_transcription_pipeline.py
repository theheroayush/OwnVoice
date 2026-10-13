import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import time
import wave
import numpy as np
from config import config_manager
from core.ai_engine import AIEngine
from core.injector import CursorInjector

def test_live_pipeline():
    print("=== Testing Gemini Live Streaming Pipeline ===")
    ai = AIEngine(config_manager)
    injector = CursorInjector(config_manager)

    # 1. Test live session start
    print("[1] Starting live session...")
    ok = ai.start_live_session(mode="smart_flow")
    print(f"Live session start status: {ok}")
    assert ok, "Live session should connect successfully"

    # 2. Read test 44.1k/48k WAV
    with wave.open("test_loopback_recording.wav", "rb") as wf:
        sr = wf.getframerate()
        nch = wf.getnchannels()
        raw = wf.readframes(wf.getnframes())

    samples = np.frombuffer(raw, dtype=np.int16)
    if nch == 2 and len(samples) >= 2:
        mono = ((samples[0::2].astype(np.float32) + samples[1::2].astype(np.float32)) * 0.5)
    else:
        mono = samples.astype(np.float32)

    # 3. Simulate streaming in ~40ms slices (similar to sounddevice callback)
    print(f"[2] Streaming audio frames ({len(mono)} samples at {sr}Hz)...")
    slice_size = 2048
    t_stream_start = time.time()
    for i in range(0, len(mono), slice_size):
        chunk = mono[i:i+slice_size]
        target_len = int(len(chunk) * 16000 / sr)
        if target_len > 0:
            resampled = np.interp(
                np.linspace(0, len(chunk)-1, target_len),
                np.arange(len(chunk)),
                chunk
            ).astype(np.int16)
            ai.stream_live_chunk(resampled.tobytes())
        time.sleep(0.005)

    print(f"Finished streaming chunks in {time.time()-t_stream_start:.3f}s")

    # 4. Transcribe audio
    print("[3] Finalizing and transcribing audio...")
    t0 = time.time()
    text, latency = ai.transcribe_audio(raw, mode="smart_flow")
    total_elapsed = time.time() - t0
    print(f"Transcription result: '{text}'")
    print(f"Reported latency: {latency:.3f}s | Real wait on transcribe: {total_elapsed:.3f}s")

    assert text and len(text) > 0, "Expected non-empty transcript"
    print("[PASS] Live transcription passed successfully!")

    # 5. Test Fallback (when live session is not started)
    print("\n[4] Testing HTTP Fallback...")
    ai.cancel_live_session() # ensure live session is inactive
    with open("test_loopback_recording.wav", "rb") as f:
        full_wav_bytes = f.read()
    t_fb = time.time()
    fb_text, fb_lat = ai.transcribe_audio(full_wav_bytes, mode="smart_flow")
    print(f"HTTP Fallback result: '{fb_text}' (took {time.time()-t_fb:.3f}s)")
    assert fb_text and len(fb_text) > 0, "Expected non-empty fallback transcript"
    print("[PASS] HTTP fallback passed successfully!")

    # 6. Test Injector async clipboard restore
    print("\n[5] Testing Injector non-blocking clipboard restore...")
    t_inj = time.time()
    # Test setting clipboard text
    ok_set = injector.set_clipboard_text("OwnVoice Test")
    assert ok_set, "Clipboard set failed"
    got = injector.get_clipboard_text()
    assert "OwnVoice Test" in got, f"Expected 'OwnVoice Test', got '{got}'"
    print("[PASS] Injector passed successfully!")

if __name__ == "__main__":
    test_live_pipeline()
