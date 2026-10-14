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


## 2026-10-13T15:36:45Z

Complete brand, UI/UX, and architectural issue resolution across the OwnVoice ecosystem (Landing Page, Web App, Desktop Hub, Android App). Eliminate all broken links, fake marketing elements, touch-alignment bugs in the Android IME keyboard, desktop latency penalties, and code duplication while hardening core voice productivity features.

Working directory: c:\Users\ayush\OneDrive\Documents\ownvoice
Integrity mode: development

## Requirements

### R1. Web Platform Consolidation & Honest Linear-Grade UI
- Consolidate the web application so that 'ownvoice-web-landing-page' is the authoritative production codebase; update root 'run_web.bat' accordingly.
- Replace misleading platform marketing in 'HeroVisual.tsx' and 'WorkspaceShowcase.tsx': remove macOS '⌘ Space' widget and macOS window control dots, replacing them with authentic Windows 'Ctrl+Space' / 'F8' design tokens and obsidian aesthetic.
- Remove phantom 404 links: remove the macOS download button in '/download/lookup' until a macOS binary is built; fix footer documentation links ('/docs/changelog', '/docs/privacy').
- Ensure Stripe and Cashfree checkout flows and secure download token generators cleanly serve Windows and Android packages.

### R2. Desktop Dictation Latency & Analytics Hardening
- Resolve the Gemini Live WebSocket model issue in 'core/gemini_live_client.py' where targeting a non-existent model ('models/gemini-3.5-transcribe-live') causes a 2.0s timeout before falling back to REST.
- Remove fabricated baseline statistics ('1284 words, 42 dictations') in 'config.py'; bind 'ProductivityView' directly to genuine user metrics from 'history.json' and 'notes.db' with clean empty states for fresh installs.
- Persist the actual active process detected by 'ContextDetector' into history entries, and connect 'StyleView' application selectors to configuration.

### R3. Android IME Keyboard Touch Alignment & Visual Consistency
- Fix the character touch target geometry in 'KeyboardView.kt': calibrate key bounding boxes, touch slop, and padding so rapid typing does not trigger erroneous adjacent characters.
- Eliminate legacy template colors ('Purple80', 'Pink80') in 'Color.kt' and standardize theme components onto 'DesignTokens.kt' ('#0B0F17' Obsidian, '#2563EB' Cobalt Blue).
- Clean up placeholder stubs ('DataRepository.kt') and hardcoded fallback keys in 'SecureConfig.kt'.
- De-clutter the keyboard interface by demoting the low-utility Wi-Fi Trackpad into an optional tools menu, keeping the primary focus on Voice Typing and Air-Typing.

### R4. Cross-Device Ecosystem Hardening & Verification
- Verify UDP broadcast discovery (port 8766) and TCP subnet sweep in 'BridgeClient.kt' and 'BridgeServer.py' for zero-friction pairing.
- Verify end-to-end Air-Typing: spoken words and keystrokes on the phone stream reliably into the PC cursor.
- Verify Meeting Mode: minutes and action items captured on mobile save cleanly to PC 'Documents/OwnVoice/Meetings'.

## Acceptance Criteria

### Web & Brand
- [ ] Root 'run_web.bat' starts the full Next.js landing page at 'ownvoice-web-landing-page'.
- [ ] Zero 404 links on the landing page, navigation bars, and documentation footer.
- [ ] Hero visual reflects authentic Windows hotkeys ('Ctrl+Space' / 'F8') without conflicting Mac symbology.

### Desktop Performance
- [ ] No 2-second timeout penalty on dictation; Gemini requests stream without WebSocket handshake failure.
- [ ] 'ProductivityView' displays real data from 'history.json' with zero fabricated baselines.
- [ ] History records accurately log the foreground application.

### Mobile Usability
- [ ] Android IME keyboard registers accurate keystrokes during rapid typing test without adjacent key bleeding.
- [ ] Zero Android Studio default purple theme colors in active Compose themes.
- [ ] Phone-to-PC Air-Typing and Meeting Mode file transfer pass end-to-end verification.


## 2026-10-14T07:58:27Z

Redesign the OwnVoice Android Mobile Application (IME voice keyboard, floating bubble overlay, companion screens, typography, and theme system) to match top-tier consumer standards (inspired by Wispr Flow, Superwhisper, and Linear) with modern font styling, cohesive Obsidian dark theming, fluid spring animations, tactile micro-interactions, and zero functionality degradation.

Working directory: c:\Users\ayush\OneDrive\Documents\ownvoice
Integrity mode: development

## Requirements

### R1. Typography & Theme System Overhaul
- Establish a modern, high-grade typographical system in 'Type.kt' and 'DesignTokens.kt' with clean font weights, tracking/letter-spacing, and clear visual hierarchy (Display, Title, Body, Keycap Glyphs, Chip Labels, Subscript Numbers).
- Upgrade the Obsidian theme palette ('#0B0F17' base, '#131926' elevated card surfaces, '#1E293B' border outlines, '#2563EB' / '#38BDF8' accent highlights, '#F8FAFC' primary high-legibility text, '#94A3B8' secondary text) across 'Color.kt', 'DesignTokens.kt', and 'Theme.kt'.
- Ensure strict contrast ratios meeting WCAG AA standards for both dark and ambient lighting conditions.

### R2. Android IME Keyboard UI/UX Overhaul
- Redesign the primary keyboard layout and top toolbar in 'KeyboardView.kt' with the updated typography, elevated keycap depth, subtle tactile pressed states, and sleek Material You / Wispr-style aesthetic.
- Replace static microphone iconography with an interactive acoustic pulse ring and amplitude-reactive visualizer directly on the voice input pill.
- Upgrade the quick tone selector (Smart Flow, Email, Chat, Code, Hindi/English) with an intuitive, scrollable pill bar, clean font hierarchy, and instant haptic feedback.
- Preserve all existing touch-calibration geometry (calibrated 'touchSlopPx', 'releaseMarginPx', pointer ID isolation) so rapid two-thumb typing remains 100% accurate.
- Maintain existing glide typing, backspace scrub gesture, clipboard history drawer, and emoji picker workflows.

### R3. Ambient Floating Bubble HUD (Wispr Flow Style)
- Redesign 'FloatingBubbleService.kt' to transform the floating bubble from a basic static shape into an ambient, modern capsule HUD.
- Implement smooth edge-snapping spring physics and dragging affordances.
- Add an animated acoustic waveform / pulsating halo during voice recording that reacts to audio state and amplitude instead of a static red fill.
- Add a smooth quick-mode indicator chip (Auto-Context, Meeting, Notes, Translate) accessible via quick gesture.
- Guarantee that background text injection via 'OverlayAccessibilityService.kt' and 'FloatingBubbleService.kt' remains completely unbroken.

### R4. Modern Companion App Screens & Navigation
- Modernize 'HomeScreen.kt', 'SettingsScreen.kt', 'HistoryPrivacyScreen.kt', and 'MeetingModeScreen.kt' with clean glassmorphic card containers, consistent typography, and refined status pills.
- Enhance 'HeroMicOrb.kt' with layered concentric ambient ripples, dynamic spring easing, and distinct visual states for Idle, Listening, Processing, and Success.
- Ensure the Bottom Navigation bar ('BottomNavBar.kt') and status cards reflect the polished theme and typography.

### R5. Zero Regression & Functional Verification
- Maintain complete backwards compatibility for all core functionality: 48kHz audio streaming, Gemini Live WebSocket and REST fallbacks, UDP broadcast discovery (port 8766), TCP bridge commands to PC, and SQLite history persistence.
- Execute automated Gradle unit tests and assemble an installable debug APK verifying zero compilation or runtime regressions.

## Verification Resources
- Existing Unit Test: 'android/app/src/test/java/com/example/ownvoice/ime/KeyboardLayoutSpecTest.kt'
- Existing Test Suite: 'tests/test_live_bridge_and_phone_link.py'
- Android Manifest & Permissions: 'android/app/src/main/AndroidManifest.xml'
- Gradle Toolchain: Configured with JDK 17 at 'C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot'

## Acceptance Criteria

### Typography & Theme
- [ ] 'Type.kt' and 'DesignTokens.kt' provide a unified typographical scale with modern tracking and font styling.
- [ ] Theme palette is strictly unified on dark obsidian tokens with high-contrast text and zero default template colors.

### Keyboard UI & Voice Bar
- [ ] Keyboard surface displays modern obsidian palette with elevated keycaps and crisp typography.
- [ ] Microphone trigger features dynamic amplitude-reactive pulse animations during voice dictation.
- [ ] Tone selector chips toggle cleanly with visible active highlights and zero layout shifts.
- [ ] Rapid typing tests and character key layout tests pass without adjacent key bleed or dropped strokes.

### Floating Bubble HUD
- [ ] Floating bubble renders with modern capsule styling, refined borders, and smooth edge snapping.
- [ ] Active recording state displays an acoustic waveform/pulsing halo with instant state feedback for processing and completion.
- [ ] Direct text injection via accessibility service into third-party target apps functions cleanly.

### Companion Screens & Navigation
- [ ] Home, Settings, History, and Meeting Mode screens adhere to unified typography and design tokens.
- [ ] Hero Mic Orb displays smooth spring-animated ripples across Idle, Listening, and Processing states.

### Build & Integrity Verification
- [ ] 'gradlew.bat testDebugUnitTest' passes 100% of unit tests.
- [ ] 'gradlew.bat assembleDebug' succeeds, producing a valid APK package in 'android/app/build/outputs/apk/debug/'.
