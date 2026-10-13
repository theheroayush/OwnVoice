# Project: OwnVoice Brand, UI/UX & Architectural Issue Resolution

## Architecture
OwnVoice is a cross-platform voice productivity ecosystem comprising:
1. **Web Platform (`ownvoice-web-landing-page`)**: Next.js 14 App Router, TailwindCSS, TypeScript, Prisma ORM, Cashfree & Stripe checkout flows, and secure HMAC-SHA256 binary delivery.
2. **Desktop Hub (`core/`, `ui/`)**: Python CustomTkinter desktop application with local and cloud transcription engines, Win32 active window context detection, productivity metrics logging, and global hotkey injection.
3. **Android App (`android/`)**: Native Kotlin / Jetpack Compose IME keyboard featuring Air-Typing, voice dictation, obsidian Linear-grade theming, and local credential security.
4. **Cross-Device Bridge (`core/bridge_server.py`, `BridgeClient.kt`)**: Local network bridge supporting UDP broadcast discovery (port 8766), TCP sweep (port 8765), live Air-Typing keystroke streaming (`/inject`), and Meeting Mode document syncing (`/meeting/save`).

```
+--------------------------------------------------------------------+
|                         OwnVoice Ecosystem                         |
+--------------------------------------------------------------------+
       |                                              |
       v                                              v
[Web Landing Page]                         [Android IME Keyboard]
- Linear-grade Windows design tokens       - Obsidian theme (#0B0F17)
- Cashfree & Stripe checkouts              - Calibrated touch target geometry
- Secure token download stream             - Air-Typing & Voice controls
       |                                              |
       |                                              | UDP 8766 / TCP 8765
       +--------------------+                         v
                            |               [Desktop Bridge Server]
                            v                         |
                     [Desktop Hub]                    v
               - Low-latency dictation <--------------+
               - Real history & notes
               - Win32 Context tone detector
```

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| F1 | Web Directory Consolidation | Make `ownvoice-web-landing-page` authoritative, update root `run_web.bat` and `package.json` | M1 | Survey / R1 |
| F2 | Honest Windows Marketing UI | Replace macOS traffic lights and `⌘ Space` in `HeroVisual.tsx` and `WorkspaceShowcase.tsx` with Windows controls (`—`, `▢`, `✕`) and `Ctrl+Space` / `F8` tokens | M1 | Survey / R1 |
| F3 | 404 & Phantom Link Removal | Remove phantom macOS button in `/download/lookup`; fix footer links to `/docs/changelog`, `/docs/getting-started`, `/docs/privacy`, `/contact` | M1 | Survey / R1 |
| F4 | Secure Checkout & Token Delivery | Ensure Cashfree & Stripe checkout flows and HMAC-SHA256 download token generator cleanly serve Windows and Android binaries | M1 | Survey / R1 |
| F5 | Gemini Live Latency Elimination | Eliminate 2.0s penalty in `core/gemini_live_client.py`: update target model from invalid `gemini-3.5-transcribe-live` to supported `gemini-2.0-flash` with instant fail-fast on handshake drop | M2 | Survey / R2 |
| F6 | Honest Productivity Analytics | Remove fake stats (`1284 words, 42 dictations`) in `config.py`; bind `ProductivityView` directly to genuine data from `history.json` and `notes.db` with clean zero-metric empty states; fix `NoteStore.list_notes` API call | M2 | Survey / R2 |
| F7 | Process Context Telemetry | Persist Win32 active process name (`code.exe`, `slack.exe`, etc.) and window title from `ContextDetector` into `history.json`; connect `StyleView` application selectors to configuration | M2 | Survey / R2 |
| F8 | Touch Target Geometry Calibration | Fix touch slop, remove thumb-switching multi-touch cancellation bug in `GboardKey` (`KeyboardView.kt`), calibrate bounding boxes and long-press timeout to prevent adjacent key bleeding | M3 | Survey / R3 |
| F9 | Obsidian Color Standard | Purge legacy Android Studio purple/pink tokens (`Purple80`, `Pink80`, etc.) in `Color.kt` and bind `Theme.kt` directly to `DesignTokens.kt` (`#0B0F17` Obsidian, `#2563EB` Cobalt Blue) | M3 | Survey / R3 |
| F10 | Cleanup Stubs & Fallback Keys | Delete unused `DataRepository.kt` stub; remove hardcoded `DEFAULT_API_KEY` and mock MacBook history entries in `SecureConfig.kt` | M3 | Survey / R3 |
| F11 | Keyboard Toolbar De-clutter | Demote Wi-Fi Trackpad from primary toolbar `LazyRow` into `GboardToolsDrawerView` to elevate Voice Typing and Air-Typing | M3 | Survey / R3 |
| F12 | Cross-Device Bridge Discovery | Verify UDP broadcast discovery (port 8766) and TCP subnet sweep (port 8765) in `BridgeClient.kt` and `BridgeServer.py` | M4 | Survey / R4 |
| F13 | Air-Typing Keystroke Streaming | Verify live streaming of spoken words and keystrokes from phone to PC cursor via `/inject` | M4 | Survey / R4 |
| F14 | Meeting Mode Sync & Alignment | Verify markdown meeting notes and action items save cleanly to PC `Documents/OwnVoice/Meetings` via `/meeting/save` | M4 | Survey / R4 |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Web Platform Consolidation & Honest Linear-Grade UI | F1, F2, F3, F4 (`ownvoice-web-landing-page`, root `run_web.bat`, `package.json`, `HeroVisual.tsx`, `WorkspaceShowcase.tsx`, `lookup/page.tsx`, `MarketingFooter.tsx`, `route.ts`) | None | IN_PROGRESS |
| M2 | Desktop Dictation Latency & Analytics Hardening | F5, F6, F7 (`core/gemini_live_client.py`, `config.py`, `ui/views/productivity_view.py`, `ui/views/style_view.py`, `app.py`, `ui/views/history_view.py`, `tests/`) | None | IN_PROGRESS |
| M3 | Android IME Keyboard Touch Alignment & Visual Consistency | F8, F9, F10, F11 (`android/.../KeyboardView.kt`, `Color.kt`, `Theme.kt`, `DesignTokens.kt`, `DataRepository.kt`, `SecureConfig.kt`, `GboardToolsDrawerView.kt`) | None | IN_PROGRESS |
| M4 | Cross-Device Ecosystem Hardening & Verification | F12, F13, F14 (`android/.../BridgeClient.kt`, `core/bridge_server.py`, `tests/test_bridge_discovery.py`, cross-device verification) | M2, M3 | PLANNED |

## Interface Contracts
### Web ↔ Binaries
- Route `/api/download/secure?token=<HMAC>&os=<windows|android>` resolves release packages from `releases/OwnVoice-v2.0.0-Windows.zip` and `releases/OwnVoice-v3.0.0.apk`.
- If `os=mac`, the API cleanly responds with 404 or unsupported status without advertising download buttons in the UI.

### Desktop Context Detector ↔ History
- History entries in `history.json` record:
  ```json
  {
    "timestamp": "YYYY-MM-DD HH:MM:SS",
    "text": "transcribed string",
    "mode": "casual | professional | concise | bullet",
    "app": "code.exe",
    "window_title": "PROJECT.md - ownvoice",
    "latency": 0.42
  }
  ```

### Desktop Style Config ↔ UI
- `config.json` stores `app_styles`:
  ```json
  {
    "app_styles": {
      "code.exe": "concise",
      "slack.exe": "casual",
      "outlook.exe": "professional"
    }
  }
  ```

### Android IME ↔ Desktop Bridge Server
- Discovery: UDP broadcast `DISCOVER_OWNVOICE_PC` on port 8766 responds with server metadata (`status`, `ip`, `port`, `pin`, `token`).
- Keystroke injection: `POST http://<pc_ip>:8765/inject` with `{ "text": "<char>", "action": "type", "token": "<token>" }`.
- Meeting Note Sync: `POST http://<pc_ip>:8765/meeting/save` with `{ "title": "<title>", "content": "<markdown>", "token": "<token>" }` writes to `C:\Users\<user>\Documents\OwnVoice\Meetings\<title>_<timestamp>.md`.

## Code Layout
- Web Platform: `ownvoice-web-landing-page/`
- Desktop Hub: `core/`, `ui/`, `config.py`, `app.py`, `tests/`
- Android App: `android/app/src/main/java/com/example/ownvoice/`
- Releases: `releases/`
