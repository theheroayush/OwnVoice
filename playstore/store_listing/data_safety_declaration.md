# Play Console Data Safety preparation — OwnVoice Android

Prepared from the current Android source. Confirm the answers in Play Console against the exact release bundle and current provider terms before submission.

## Collection and sharing

- Data leaves the device when the user invokes speech transcription, meeting transcription, or text reshaping. Google Gemini receives the audio or text and the user's Google API key over HTTPS. Google processes the request under the terms for that API key and may retain request data according to those terms. Do not declare this ephemeral unless Google's applicable terms and implementation establish ephemeral processing.
- Optional GitHub Gist sync sends the user's custom vocabulary, snippets and selected settings to GitHub using a GitHub token supplied by the user. New Gists are created as private. If an existing Gist is selected, its visibility is controlled by that Gist's settings.
- Optional phone-to-PC features send user-selected dictated text, keystrokes, clipboard text and trackpad commands to the paired PC or relay endpoint. Local companion traffic can use cleartext HTTP/WebSocket; the app must not be described as universally encrypted in transit.
- OwnVoice does not operate an account or transcription server for these flows. Accessibility observations remain on-device; the Accessibility Service is gated behind in-app disclosure and affirmative consent.

## Likely data types to declare

- Audio files: voice or sound recordings — collected/shared with Google for app functionality when a user starts a voice feature.
- Other user-generated content: transcripts, selected text, snippets and vocabulary — collected/shared with Google when the user invokes transcription or text shaping; shared with GitHub when optional Gist sync is used; may be sent to the user's PC/relay when those features are used.
- Authentication information: Google API key and optional GitHub/relay credentials — stored on device, and transmitted to the matching provider/endpoint for authentication.

No advertising ID or ad-network data is used in the Android source. No ads are included.

## Security and deletion

- Google/GitHub API credentials are encrypted with Android Keystore where supported. App history and other preferences remain in app-private storage. Android backup is disabled.
- All Gemini and GitHub API requests use HTTPS. Because optional PC/relay connections can use cleartext protocols, answer “No” if Play Console asks whether all user data in transit is encrypted.
- Users can clear local history/settings in the app. Gist content must be deleted from GitHub; provider-side request retention follows the provider's terms. Do not claim that uninstalling deletes remote provider data.
- No OwnVoice account is created in the app. The app does not provide a remote account/data deletion request endpoint.
