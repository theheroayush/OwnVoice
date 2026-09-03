# 🎙️ OwnVoice

> **OwnVoice** is an ultra-fast, private, zero-latency desktop voice-to-text dictation and auto-typing engine designed specifically for modern Windows and Qualcomm Snapdragon ARM64 devices.

<p align="center">
  <img src="assets/screenshots/architecture_diagram.png" width="850" alt="OwnVoice Architecture Pipeline" />
</p>

---

## 📸 Interface Previews

| Idle / Ready State (`F8`) | Active Dictation (App-Aware) | Multimodal Processing |
| :---: | :---: | :---: |
| <img src="assets/screenshots/floating_pill_idle.png" width="270" alt="OwnVoice Idle Capsule" /> | <img src="assets/screenshots/floating_pill_recording.png" width="290" alt="OwnVoice Recording Capsule" /> | <img src="assets/screenshots/floating_pill_processing.png" width="270" alt="OwnVoice Processing Capsule" /> |

<p align="center">
  <img src="assets/screenshots/settings_window.png" width="750" alt="OwnVoice Settings Window" />
</p>

---

## ✨ Features

- **⚡ Zero-Lag Dictation**: Press `F8` anywhere in Windows, speak naturally, and text is instantly typed at your active cursor.
- **🎯 App-Aware Tone Detection**: 100% native Win32 context awareness automatically adjusts output formatting based on your active window:
  - **VS Code / Terminal / Cursor** $\rightarrow$ Code casing (`camelCase`, `snake_case`), backticks, shell flags.
  - **MS Word / Docs** $\rightarrow$ Structured executive paragraphs and bullet points.
  - **Outlook / Gmail** $\rightarrow$ Formal business email formatting.
  - **Slack / WhatsApp / Discord** $\rightarrow$ Friendly, conversational messaging tone.
- **✂️ Voice Snippets & Text Expander**: Say spoken triggers (e.g. *"my email"*, *"meeting link"*, *"my portfolio"*) to instantly expand into complex links, emails, or templates.
- **🎯 Caret & Search Box Focus Memory**: Automatically remembers and re-focuses your active search box or document before typing without triggering menu bar ribbons.
- **🔇 100% Silent Operation**: Pure silence on quiet rooms (no `00:00` hallucinations), no Windows dings, and no audio interruptions.
- **🖱️ Draggable Capsule with Pointer Capture**: Minimalist obsidian floating capsule with live soundwave visualization, freely draggable across screens with Win32 `SetCapture` support.
- **✕ Dedicated Controls**: Cancel recording or network processing with one click via the `✕` button.
- **🧠 Google Gemini 3.5 Flash-Lite Multimodal Engine**: Cleans filler words (*um, uh, like, you know*), formats punctuation, and supports persistent `requests.Session` connection pooling for sub-200ms round trips.
- **🎙️ Snapdragon Hardware Matching**: Native 48,000 Hz stereo WASAPI audio capture with 50x dynamic Auto-Gain Control (AGC), eliminating all unstable WDM-KS endpoints.
- **🛡️ Direct Keystroke Fallback**: If Windows clipboard access is locked by external utilities, the injector automatically falls back to direct Unicode keystroke generation (`SendInput(KEYEVENTF_UNICODE)`).

---

## 🚀 Quick Start

### 1. Installation
```bash
git clone https://github.com/theheroayush/OwnVoice.git
cd OwnVoice
pip install -r requirements.txt
```

### 2. Configuration
Copy `config.example.json` to `config.json` and insert your Google AI Studio API key:
```json
{
  "google_api_key": "YOUR_GEMINI_API_KEY",
  "model": "gemini-3.5-flash-lite",
  "hotkey": "f8",
  "sound_effects": false
}
```

### 3. Run
```bash
python app.py
```

Or launch the compiled standalone executable from:
```
C:\Users\ayush\AppData\Local\Programs\OwnVoice\OwnVoice.exe
```

---

## 🎮 How to Dictate

1. Click into **any text box, search bar, or MS Word**.
2. Press **`F8`** once *(or click the floating capsule)* $\rightarrow$ capsule glows: `Listening...`.
3. Speak your sentence or query.
4. Press **`F8`** again $\rightarrow$ your cleaned, punctuated sentence is typed directly at your cursor!

---

## 🧪 Comprehensive Automated Test Suite

OwnVoice includes 252 automated unit and integration tests across 4 quality tiers:

```bash
python -m unittest discover -s tests
```
```
Ran 252 tests in 22.236s
OK (100% passing)
```

---

## 🔒 Privacy & Security

- Audio snippets are streamed directly via Google AI Studio's multimodal endpoint with zero permanent cloud audio retention.
- All configuration, snippet templates, and history logs remain 100% local on your device.
- Direct memory-isolated Win32 ctypes function prototypes prevent cross-library namespace collision.
