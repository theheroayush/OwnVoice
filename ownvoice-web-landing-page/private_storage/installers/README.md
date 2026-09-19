# Private Installer Storage Vault

Files placed in this directory are NEVER served publicly by Next.js static routing.
They can ONLY be accessed via authenticated, 24-hour cryptographically signed HMAC tokens through `/api/download/secure`.

Supported platforms:
- Windows: OwnVoice-v2.0.0-Windows.zip / OwnVoice-Setup.exe
- Android: OwnVoice.apk
- Mac: OwnVoice-v2.0.0.dmg / OwnVoice-Mac.zip
