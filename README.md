# 🎙️ OwnVoice

> **OwnVoice** is an ultra-fast, private, zero-latency desktop voice-to-text dictation and auto-typing engine designed specifically for modern Windows and Qualcomm Snapdragon ARM64 devices.

---

## ✨ Features

- **⚡ Zero-Lag Wispr Flow Experience**: Press `F8` anywhere in Windows, speak naturally, and text is instantly typed at your active cursor.
- **🎯 App-Aware Tone Detection**: 100% native Win32 context awareness automatically adjusts output formatting based on your active window:
  - **VS Code / Terminal / Cursor** $\rightarrow$ Code casing (`camelCase`, `snake_case`), backticks, shell flags.
  - **MS Word / Docs** $\rightarrow$ Structured executive paragraphs and bullet points.
  - **Outlook / Gmail** $\rightarrow$ Formal business email formatting.
  - **Slack / WhatsApp / Discord** $\rightarrow$ Friendly, conversational messaging tone.
- **✂️ Voice Snippets & Text Expander**: Say spoken triggers (e.g. *"my email"*, *"meeting link"*, *"sign off"*) to instantly expand into complex links, emails, or templates.
- **🎯 Caret & Search Box Focus Memory**: Automatically remembers and re-focuses your active search box or document before typing.
- **🔇 100% Silent Operation**: No beeps, chimes, or audio interruptions.
- **🖱️ Draggable Capsule**: Minimalist obsidian floating capsule with live cyan soundwave visualization, freely draggable anywhere across your screens.
- **✕ Dedicated Controls**: Cancel recording or hide to tray with one click.
- **🧠 Google Gemini 2.0 / 3.5 Flash Multimodal Engine**: Cleans filler words (*um, uh, like, you know*), formats punctuation, and supports custom vocabulary/jargon.
- **🎙️ Snapdragon Hardware Matching**: Native 48,000 Hz stereo WASAPI audio capture with 50x dynamic Auto-Gain Control (AGC).

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

---

## 🎮 How to Dictate

1. Click into **any text box, search bar, or MS Word**.
2. Press **`F8`** once *(or click the floating capsule)* $\rightarrow$ capsule glows red: `🔴 ılıl Listening...`.
3. Speak your sentence or query.
4. Press **`F8`** again $\rightarrow$ your cleaned, punctuated sentence is typed directly at your cursor!

---

## 🔒 Privacy & Architecture

- Audio is processed directly via Google AI Studio's multimodal endpoint with zero permanent audio retention.
- All configuration and history logs remain 100% local on your PC.
