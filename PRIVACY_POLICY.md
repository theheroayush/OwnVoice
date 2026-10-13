# Privacy Policy for OwnVoice

**Effective Date:** October 14, 2026  
**Last Updated:** October 14, 2026  
**Website:** [https://ownvoice.in](https://ownvoice.in)  
**Repository:** [https://github.com/theheroayush/OwnVoice](https://github.com/theheroayush/OwnVoice)  
**Developer Contact:** `support@ownvoice.in`

---

## 1. Introduction & Core Privacy Philosophy

OwnVoice ("we", "our", or "us") provides a cross-platform voice productivity ecosystem comprising the **OwnVoice Android IME Keyboard & Voice Companion**, the **OwnVoice Desktop Hub**, and the **OwnVoice Web Platform**.

Our core operating principle is simple: **Your voice, keystrokes, and thoughts belong solely to you.**  
OwnVoice is engineered from the ground up as a privacy-first, zero-retention utility. We do not sell your data, we do not monetize your personal information, and we do not bundle third-party behavioral advertising or tracking SDKs.

---

## 2. Information We Handle and How It Is Used

### A. Audio and Voice Recordings
* **Purpose**: Used exclusively to transcribe spoken words into text and format them according to your selected tone or context (e.g., Email, Code, Concise, Notes).
* **Processing**: Voice audio is captured in high-fidelity (48kHz) and processed ephemerally in volatile memory (RAM). Audio is streamed over encrypted TLS 1.3 connections directly to the AI transcription service (e.g., Google Gemini, OpenAI Whisper, Groq, or Anthropic).
* **Retention**: Raw voice recordings are never stored on our servers. Once transcription is complete, the in-memory audio buffers are immediately purged.

### B. Transcribed Notes and Dictation History
* **Local Storage**: Your transcribed notes, meeting minutes, and recent snippets are stored strictly **locally on your device** (in private application storage or SQLite database).
* **Control**: You have full control over your history. You can view, search, export to Markdown, or permanently delete individual items or the entire database at any time in Settings.

### C. Bring Your Own Key (BYOK) & Credentials
* **Security**: If you supply an API key (e.g., Google Gemini, Groq, OpenAI), it is stored securely on your device using hardware-backed **Android Keystore** and `EncryptedSharedPreferences`.
* **Zero Plaintext Transmission**: Your keys are used solely for direct communication with the AI provider's official API endpoints. They are never transmitted to any third-party intermediary or logged.

---

## 3. Android Permissions & Prominent Disclosures

To deliver seamless voice typing, floating dictation, and laptop synchronization, OwnVoice requests the following permissions on Android. Each permission is strictly scoped to its stated functionality:

| Permission | Purpose & Scope |
| :--- | :--- |
| `RECORD_AUDIO` | Required to capture your spoken voice for real-time transcription and Meeting Mode. Never activates without your explicit touch action. |
| `BIND_ACCESSIBILITY_SERVICE` | **Prominent Disclosure**: The `OverlayAccessibilityService` is used solely to detect active, editable text input fields and inject transcribed text hands-free when you tap the Floating Bubble widget. It **does NOT** read passwords, log keystrokes, collect credit card details, capture screen contents, or transmit personal data. |
| `SYSTEM_ALERT_WINDOW` | Enables the optional floating microphone capsule overlay so you can trigger voice dictation over any third-party app without switching keyboards. |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MICROPHONE` | Required by Android OS to show an active, dismissible notification status bar icon while recording meetings or active voice sessions. |
| `ACCESS_NETWORK_STATE` & `ACCESS_WIFI_STATE` | Used solely for local Wi-Fi peer discovery (UDP port 8766 / TCP port 8765) to connect your phone to your laptop for Air-Typing. |
| `CHANGE_WIFI_MULTICAST_STATE` | Used on local Wi-Fi networks to broadcast and receive zero-configuration desktop pairing signals. |
| `VIBRATE` | Provides subtle haptic feedback on key presses and recording initiation. |
| `POST_NOTIFICATIONS` | Displays the status of ongoing meeting recordings or active desktop link connections. |

---

## 4. Cross-Device Air-Typing & Local Network Communication

OwnVoice features **Air-Typing**, enabling your phone to act as a wireless studio microphone and remote keyboard for your computer:
* **Private Subnet Only**: Communication between your phone and laptop occurs over your local Wi-Fi network (LAN) using a secure 6-digit PIN and rotating authentication token.
* **No Cloud Snooping**: Keystrokes, mouse trackpad coordinates, and meeting notes transferred between your devices never route through public relay servers when connected on the same Wi-Fi network.
* **Optional Zero-Knowledge Cloud Relay**: If you choose to connect over different networks, communication is end-to-end tokenized and ephemeral.

---

## 5. Third-Party Service Providers

OwnVoice interfaces with the following AI APIs based on the provider you configure:
* **Google Gemini API**: Subject to Google's Cloud API Terms & Data Privacy.
* **OpenAI API / Groq / Anthropic**: Subject to their respective commercial API privacy terms (commercial enterprise API policies generally state that API data is not used to train models).

OwnVoice does not integrate advertising SDKs, data brokers, or third-party behavioral analytics tools (such as Facebook SDK, AppsFlyer, or Google AdMob).

---

## 6. Data Retention, Export & Deletion Rights

You have complete sovereignty over your data:
1. **Immediate Deletion**: In the Android app, navigate to **Settings → Privacy & History → Clear Data** to permanently purge all local logs, notes, and cached tokens.
2. **Key Revocation**: You can remove or replace your API keys at any moment with a single tap.
3. **App Uninstall**: Uninstalling the OwnVoice application automatically deletes all sandboxed databases and preferences from your device.

---

## 7. Children's Privacy (COPPA Compliance)

OwnVoice is a productivity and utility tool intended for general audiences and knowledge workers. We do not knowingly collect or solicit personal data from children under the age of 13. If you believe a child has provided us with personal information, please contact us immediately for prompt deletion.

---

## 8. International Data Privacy Compliance (GDPR & CCPA)

* **European Economic Area (GDPR)**: Processing of audio and text is grounded on your explicit consent (when activating the microphone). Because data is stored locally on your device and ephemeral in transit, you retain rights to access, portability, and erasure.
* **California Residents (CCPA / CPRA)**: We do not sell or share personal information with third parties for monetary or commercial consideration.

---

## 9. Changes to This Privacy Policy

We may update this Privacy Policy periodically to reflect new features or regulatory requirements. Any updates will be published to our repository at [https://github.com/theheroayush/OwnVoice/blob/main/PRIVACY_POLICY.md](https://github.com/theheroayush/OwnVoice/blob/main/PRIVACY_POLICY.md) and on our website at [https://ownvoice.in/privacy](https://ownvoice.in/privacy).

---

## 10. Contact Us

If you have questions, feedback, or inquiries regarding this Privacy Policy or your data, please contact:

* **Email:** `support@ownvoice.in`
* **Website:** [https://ownvoice.in](https://ownvoice.in)
* **GitHub Issues:** [https://github.com/theheroayush/OwnVoice/issues](https://github.com/theheroayush/OwnVoice/issues)
