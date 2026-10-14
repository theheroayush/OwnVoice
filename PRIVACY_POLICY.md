# OwnVoice Privacy Policy

Effective date: 14 October 2026
Last updated: 14 October 2026

OwnVoice provides an Android voice keyboard, optional floating dictation and meeting notes, and optional connections to a user's computer. This policy describes the Android app. For questions, contact support@ownvoice.in.

## Information processed by OwnVoice

### Voice audio and text

When you start dictation, meeting transcription or another voice feature, OwnVoice records audio through the microphone and sends the audio to the Google Gemini API over HTTPS using the Google API key configured in the app. When you ask the app to reshape text, the selected text and your instruction are sent to that API. Google processes those requests under the terms and privacy practices that apply to the API key you use. Google may retain or otherwise process requests under those terms; OwnVoice cannot control or promise Google's retention practices.

The app may keep transcripts, notes, snippets, vocabulary and preferences in app-private storage on your device. You can clear local history and settings in the app. Android backup is disabled for the app.

### Accessibility auto-typing

Accessibility auto-typing is optional. Before the app opens Android Accessibility settings, it displays a separate disclosure and asks you to agree. If enabled, the service observes the active app and focused editable field so it can insert a transcript after you tap the floating bubble. It may inspect the focused field's existing text to preserve that field's contents when inserting text. It excludes password fields. These accessibility observations are processed on the device and are not sent to OwnVoice. The transcript itself may be sent to Google Gemini when you use a voice feature. You can decline the disclosure or disable the service in Android settings.

### Optional GitHub Gist sync

If you choose to use cloud sync, OwnVoice sends the vocabulary, snippets and settings selected for sync to GitHub's Gist API using a GitHub token that you provide. Newly created Gists are private. An existing Gist keeps its existing visibility. GitHub handles the data under its own terms. Remove synced content from GitHub if you want it deleted there; clearing app data does not delete a Gist.

### Optional phone-to-PC connection

If you pair the app with the OwnVoice desktop companion, the app can send dictated text, keyboard input, clipboard text and trackpad commands to the paired PC or a relay endpoint you configure. These features are user-initiated. Local companion and custom relay connections may use cleartext HTTP or WebSocket protocols; they are not universally encrypted by OwnVoice. Only pair with a device and network you trust.

### Credentials and local settings

The Google API key and optional GitHub, pairing and relay credentials are encrypted with Android Keystore where supported and stored in app-private preferences. Other settings and local history are stored in app-private storage. Credentials are sent to the corresponding provider or endpoint when you use the associated feature.

## Permissions

- Microphone: capture audio when you start a voice or meeting feature.
- Accessibility Service: identify the active app and focused non-password field for optional transcript insertion, only after in-app disclosure and consent.
- Display over other apps: show the optional floating dictation bubble.
- Camera: scan a pairing QR code when you choose that flow; image data is used locally for pairing.
- Notifications and foreground service: show the system notification while microphone recording is active.
- Network and Wi-Fi: communicate with Google, GitHub when sync is used, and your paired PC or configured relay.

## Data retention and deletion

OwnVoice has no user account or OwnVoice-operated transcription server. Local history and settings remain on the device until cleared in the app or the app is uninstalled. Deleting local data does not remove information already sent to Google, GitHub or a configured relay. Their retention and deletion are governed by their respective terms and tools. Revoke API tokens with the provider if needed.

## Advertising and sale

The Android app does not contain advertising. OwnVoice does not sell personal information. The app sends information to third-party providers only when you use features that require those providers, as described above.

## Children

The app is intended for users aged 18 and over and is not designed for children.

## Changes

Material policy changes will be posted at this URL. For privacy questions, contact support@ownvoice.in.
