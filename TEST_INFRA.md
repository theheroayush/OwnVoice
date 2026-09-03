# TEST_INFRA.md — OwnVoice E2E Test Infrastructure & Methodology

## 1. Executive Summary & Test Philosophy

The OwnVoice End-to-End (E2E) Test Suite provides rigorous, opaque-box, requirement-driven automated verification across all 20 features defined in `PROJECT.md` for Qualcomm Snapdragon ARM64 and x64 Windows systems.

### Core Testing Principles
1. **Opaque-Box Requirement Verification**: Tests validate behavior against public interface contracts and observable system states (audio buffers, Win32 input events, clipboard states, window hierarchy, process memory, CPU utilization).
2. **Zero Facade & Zero Mocking of Internal Logic**: Tests execute actual production application code (`core/`, `ui/`, `app.py`, `config.py`). Internal application logic is never mocked or bypassed. Real Win32 API calls and genuine audio processing pipelines are exercised.
3. **Independent & Deterministic**: Each test creates and tears down its own isolated state, config sandbox, and test fixtures without cross-test state leakage.
4. **4-Tier Stratified Architecture**:
   - **Tier 1: Feature Coverage**: Baseline happy paths and functional contracts for every feature in isolation (>=5 tests per feature, >=100 tests).
   - **Tier 2: Boundary & Corner Cases**: Stress conditions, malformed inputs, boundary coordinates, audio clipping, clipboard contention, and hardware failures (>=5 tests per feature, >=100 tests).
   - **Tier 3: Cross-Feature Combinations**: Pairwise and multi-module interaction tests (VU monitor + recording, hotkey toggle + ✕ cancel, symbol snippet expansion + caret injection).
   - **Tier 4: Real-World Application Scenarios**: Multi-step end-to-end user workflows, hardware failure recovery benchmarks, and system resource threshold validation.

---

## 2. 20-Feature Coverage Matrix

| Feature # | Feature Name | Target Module | Interface Contract | Tier 1 Tests | Tier 2 Tests | Tier 3 Interaction | Tier 4 Workflow |
|---|---|---|---|:---:|:---:|:---:|:---:|
| **F01** | WDM-KS Endpoint Elimination | `core.audio_recorder` | `AudioRecorder.get_input_devices()` | >=5 | >=5 | Yes | Yes |
| **F02** | True Working Mic Auto-Detection | `core.audio_recorder` | `AudioRecorder.start_recording()` | >=5 | >=5 | Yes | Yes |
| **F03** | Sub-50ms Self-Healing Audio Fallback | `core.audio_recorder` | `AudioRecorder._get_fallback_candidates()` | >=5 | >=5 | Yes | Yes |
| **F04** | Unified Audio & Settings VU Stream | `core.audio_recorder`, `ui.settings_window` | `start_monitoring()`, `stop_monitoring()` | >=5 | >=5 | Yes | Yes |
| **F05** | Calibrated Logarithmic dBFS VU Meter | `core.audio_recorder` | `AudioRecorder.get_current_volume()` | >=5 | >=5 | Yes | Yes |
| **F06** | Percentile 50x Dynamic AGC & Energy Downmix | `core.audio_recorder` | `AudioRecorder._apply_resilient_agc()` | >=5 | >=5 | Yes | Yes |
| **F07** | Single Atomic Text Injection | `core.injector` | `CursorInjector.inject_text()` | >=5 | >=5 | Yes | Yes |
| **F08** | Clipboard Preservation & Unicode Fallback | `core.injector` | `get_clipboard_text()`, `set_clipboard_text()` | >=5 | >=5 | Yes | Yes |
| **F09** | Caret & Search Box Focus Restoration | `core.injector` | `CursorInjector.refocus_target()` | >=5 | >=5 | Yes | Yes |
| **F10** | PID-Aware Focus Tracking Filter | `core.injector`, `ui.floating_widget` | `CursorInjector.update_target_hwnd()` | >=5 | >=5 | Yes | Yes |
| **F11** | F8 Key-Repeat & Alert Ding Suppression | `core.hotkey_manager` | `HotkeyManager._on_press()` | >=5 | >=5 | Yes | Yes |
| **F12** | Synchronized Hotkey & Mouse State Machine | `core.hotkey_manager`, `app` | `HotkeyManager.sync_state()` | >=5 | >=5 | Yes | Yes |
| **F13** | Symbol-Safe Snippet Engine | `core.snippet_engine` | `SnippetEngine.expand()` | >=5 | >=5 | Yes | Yes |
| **F14** | Low-Latency Context Tone Detection | `core.context_detector` | `ContextDetector.detect_tone()` | >=5 | >=5 | Yes | Yes |
| **F15** | Snapdragon ARM64 Idle Memory < 40MB | `app`, `config`, `core.ai_engine` | Idle Process RSS Footprint | >=5 | >=5 | Yes | Yes |
| **F16** | Zero Idle CPU Footprint (0.0% - 0.1%) | `ui.floating_widget`, `app` | Idle Process CPU Consumption | >=5 | >=5 | Yes | Yes |
| **F17** | 100% Silent Operation | `config`, `core.sound_effects` | Audio Playback & Windows Beep Hooks | >=5 | >=5 | Yes | Yes |
| **F18** | Draggable Capsule Accidental Trigger Guard | `ui.floating_widget` | `_on_mouse_down()`, `_on_mouse_drag()` | >=5 | >=5 | Yes | Yes |
| **F19** | Dedicated ✕ Button & Tray Synchronization | `ui.floating_widget`, `app` | `FloatingWidget.hide()`, `_on_mouse_up()` | >=5 | >=5 | Yes | Yes |
| **F20** | Multi-Monitor Bounds Clamping & DPI Awareness | `ui.floating_widget` | Virtual Desktop Bounds & DPI Set | >=5 | >=5 | Yes | Yes |
| **TOTAL** | **20 Features** | **Full Application** | **Comprehensive Architecture** | **>=100** | **>=100** | **Pairwise** | **E2E Scenarios** |

---

## 3. Tier Architecture & Test Scopes

### Tier 1: Feature Coverage (>=5 per feature, 100+ tests)
Verifies the nominal functional contracts of each feature in isolation under valid inputs:
- **F01**: Validates WDM-KS endpoints are eliminated from `get_input_devices()`, zero-channel endpoints are pruned, valid tuples `(idx, name)` returned.
- **F02**: Validates WASAPI default mic prioritized, host API candidate list built in order (WASAPI -> DirectSound -> MME), 48kHz stereo prioritized.
- **F03**: Validates graceful fallback on out-of-bounds indices, output-only devices, PortAudio re-init pathway.
- **F04**: Validates `start_monitoring()` sets `is_monitoring=True`, frames are not accumulated in monitoring mode, transitions to recording seamlessly.
- **F05**: Validates dBFS formula maps silence to 0.0, max amplitude to 1.0, ambient noise (RMS 400) to ~0.25-0.35, speech to ~0.7-0.85.
- **F06**: Validates 99.2th percentile AGC ignores single-sample impulsive transient spikes, gain capped at 50x, adaptive stereo downmix preserves single-channel audio.
- **F07**: Validates single atomic injection via Win32 `SendInput` without secondary pynput duplication, 40-byte union struct size on 64-bit Windows.
- **F08**: Validates clipboard preservation and restoration, direct Unicode typing fallback on persistent clipboard lock.
- **F09**: Validates caret focus preservation (omits `SetFocus` on parent HWND), `AttachThreadInput` and `SetForegroundWindow` sequence, Alt key unlock.
- **F10**: Validates OwnVoice PID (`os.getpid()`) filtering, canvas HWND rejection, external process target retention.
- **F11**: Validates OS repeat `WM_KEYDOWN` suppression, 350ms debounce interval, F8 hook suppression filter.
- **F12**: Validates bidirectional state synchronization between `HotkeyManager` and `AudioRecorder`.
- **F13**: Validates symbol snippet expansion (`c++`, `node.js`, `c#`), exact phrase matching with trailing punctuation stripping.
- **F14**: Validates tone detection latency (<0.2ms, target <0.01ms), category mapping for Coding, Email, Document, Chat.
- **F15**: Validates lazy-loading of CustomTkinter and requests, idle memory footprint budget (<40MB RSS).
- **F16**: Validates elimination of 25Hz and 10Hz infinite polling threads, event-driven animation, idle CPU <= 0.1%.
- **F17**: Validates `sound_effects: False` by default, silent state transitions, zero audible console dings.
- **F18**: Validates Euclidean drag threshold (6px), 400ms time window, canvas grab set/release, SetWindowPos movement.
- **F19**: Validates dedicated ✕ button behavior in RECORDING, PROCESSING, and DOCKED states, tray visibility synchronization.
- **F20**: Validates multi-monitor coordinate clamping against virtual screen metrics, Per-Monitor V2 DPI awareness initialization.

### Tier 2: Boundary & Corner Cases (>=5 per feature, 100+ tests)
Verifies resilience against extreme, malformed, hostile, and boundary inputs:
- Device index bounds: -1, -999, 2^31-1, non-existent endpoints.
- Audio signals: pure silence (RMS 0), maximum DC offset, full square wave clipping ([-32768, 32767]), inverted 180-degree stereo phase cancellation, tiny buffers (<10 samples).
- Text & Clipboard: 100KB+ mega-strings, control characters (`\x00` through `\x1f`), Unicode emoji sequences, persistent clipboard lock simulation.
- Window coordinates: negative multi-monitor offsets (monitors placed to left/above primary), zero-width bounds, high-DPI scaling factors (125%, 150%, 200%).
- Concurrency & Flapping: Rapid 50-toggle cycles within 1 second, multithreaded concurrent recording attempts, aborted streams during hardware initialization.

### Tier 3: Cross-Feature Combinations (Pairwise Coverage)
Verifies multi-subsystem interaction contracts:
- **C01: Concurrent Settings VU Monitoring + Active Dictation**: `start_monitoring()` runs continuously while `start_recording()` and `stop_recording()` execute without stream contention, PortAudio deadlock, or VU thread termination.
- **C02: F8 Hotkey Trigger + ✕ Cancel in PROCESSING**: User initiates dictation via F8, stops dictation, and clicks ✕ during transcription; verifies in-flight token invalidation, cancellation of paste, and return to DOCKED.
- **C03: Rapid Alternating Input Methods (Click -> F8 -> Click -> ✕)**: Mixed input source toggling verifies state consistency across `HotkeyManager`, `FloatingWidget`, and `AudioRecorder`.
- **C04: Audio Capture + Symbol Snippet Expansion + Atomic Caret Injection**: Full pipeline test processing simulated spoken symbol phrase ("c++ vector"), expanding via `SnippetEngine`, restoring caret, and verifying single atomic paste with original clipboard restored.
- **C05: Multi-Monitor Drag + Hide to Tray + Restore & Hotkey**: Capsule moved to secondary monitor coordinates, hidden via ✕ button, restored via tray menu with `WS_EX_NOACTIVATE` preserved, followed by hotkey dictation.

### Tier 4: Real-World Application Scenarios
Simulates realistic end-to-end user workflows and hardware failure events:
- **S01: End-to-End Dictation with Preserved User Clipboard**: User copies sensitive text ("Original Clipboard Secret"), activates dictation in a simulated editor, speaks snippet trigger, receives atomic injection of expanded snippet, verifies single paste, and confirms original clipboard is restored intact.
- **S02: Dynamic Audio Hardware Self-Healing**: Hardware device index shifts to an invalid device mid-session; `AudioRecorder` detects failure and self-heals to primary working capture endpoint in < 50ms without raising "Mic Error".
- **S03: Idle Resource Compliance Verification**: Launches application in a clean subprocess, verifies idle memory RSS < 40MB, verifies idle CPU <= 0.1% over a 3-second window, and confirms 0 polling threads burn background cycles.
- **S04: Mid-Speech Dictation Cancellation**: User initiates dictation, speaks, and presses ✕ cancel button before finishing; verifies audio buffer is discarded, zero text is injected, clipboard remains unmodified, and capsule returns to DOCKED.
- **S05: Processing State In-Flight Cancellation**: Simulates active background AI transcription, user clicks ✕ cancel; verifies cancellation token aborts injection, zero text is typed into the foreground app, and focus is preserved.
- **S06: Multi-Application Context Tone Switching**: Fast context switching between Coding (VS Code), Email (Outlook), Document (Winword), and Chat (Slack); verifies `ContextDetector` identifies tone in < 0.2ms and formats appropriately.

---

## 4. Test Infrastructure Architecture

### Standalone Test Runner (`tests/run_tests.py`)
- Standard library Python (`unittest` + custom test runner engine).
- Zero mandatory external test runner dependencies (works out-of-the-box on clean Windows ARM64/x64).
- Command-line arguments:
  ```powershell
  python tests/run_tests.py [--tier 1,2,3,4,all] [--verbose] [--feature F01]
  ```
- Exit codes:
  - `0`: All executed tests passed successfully.
  - `1`: One or more tests failed or encountered errors.
  - `2`: Invalid command-line arguments or test discovery failure.

### Output Formatting & Reporting
The runner outputs formatted test progress with timestamps, per-test status (`PASS`, `FAIL`, `ERROR`, `SKIP`), per-tier summary metrics, latency benchmarks, and a final feature verification report.
