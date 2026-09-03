# Original User Request

## 2026-09-03T05:21:33Z

OwnVoice - Full Architecture Diagnostic, Resilient Audio Engine, and Zero-Error Bug Fix for Qualcomm Snapdragon ARM64 Windows.

Working directory: C:\Users\ayush\Documents\OwnVoice
Integrity mode: development

## Requirements

### R1. Resilient Zero-Crash Microphone Capture Engine
The audio capture system must never show Mic Error or crash under any circumstances.
- Filter out all invalid Windows WDM-KS kernel streaming endpoints.
- Auto-detect the true working microphone (WASAPI 48kHz stereo, falling back to DirectSound/MME dynamically).
- If the configured device index becomes unavailable or is locked by another process, automatically self-heal and fall back to the primary working capture device in < 50ms without raising errors.
- Ensure the live VU monitor in Settings never locks or contends with the recording stream for exclusive device access.

### R2. End-to-End Dictation & Injection Reliability
- Ensure global F8 shortcut and mouse clicks trigger recording cleanly.
- Keep audio downmixed and boosted with 50x dynamic AGC to prevent No speech heard.
- Seamless focus restoration and text typing into active search boxes, MS Word, and coding tools with clipboard lock recovery.
- App-aware context tone detection (Coding, Email, Docs, Chat) operating with zero latency (<0.2ms) via native Win32 APIs.

### R3. Ultra-Lightweight Snapdragon ARM64 Native Execution
- Maintain zero bloated dependencies and zero heavy frameworks.
- Keep memory usage < 40MB and CPU usage near 0.1% when idle.
- 100% silent operation (zero unwanted chimes or beeps).

## Acceptance Criteria

### Audio Resilience
- [x] Attempting to record when a device index shifts or is busy automatically recovers to a working device without displaying Mic Error.
- [x] All WDM-KS endpoints are removed from the device selector.
- [x] Concurrent Settings VU meter and dictation streams do not collide or lock the hardware.

### Full Pipeline Verification
- [x] Speech recording -> Gemini 3.5 Flash-Lite transcription -> Voice snippet expansion -> Cursor injection verified end-to-end.
- [x] Draggable capsule widget moves smoothly across screens without triggering accidental dictation.
- [x] Dedicated ✕ button cancels active recording or hides to system tray.
