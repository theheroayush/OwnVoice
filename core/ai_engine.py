import base64
import re
import time
from typing import Tuple

import requests
from requests.adapters import HTTPAdapter

DICTATION_PROMPTS = {
    "smart_flow": (
        "You are an expert real-time voice dictation assistant (like Wispr Flow). "
        "Transcribe everything said in this audio recording into clean, natural written text. "
        "Remove filler words (um, uh, like, you know, ah, so yeah) and hesitation sounds. "
        "Apply proper punctuation, capitalization, and formatting. "
        "Output ONLY the transcribed words with zero preamble, no quotes, and no markdown fences. "
        "If there is no speech, silence, or only background noise, output ABSOLUTELY NOTHING."
    ),
    "code": (
        "You are a coding voice assistant. Transcribe the spoken audio into precise code, "
        "terminal commands, or variable names. Format identifiers in appropriate casing "
        "(camelCase, snake_case, PascalCase, kebab-case), wrap inline code in backticks. "
        "Output ONLY the code or commands with zero explanations. "
        "If there is no speech, silence, or only background noise, output ABSOLUTELY NOTHING."
    ),
    "formal_email": (
        "You are an executive email assistant. Transcribe the spoken audio into a professional, "
        "well-structured email message with appropriate greetings, body paragraphs, and sign-offs. "
        "Output ONLY the clean email text. "
        "If there is no speech, silence, or only background noise, output ABSOLUTELY NOTHING."
    ),
    "formal_document": (
        "You are an executive documentation assistant. Transcribe the spoken audio into structured, "
        "formal prose suitable for MS Word or Google Docs. Format bullet points (- Item) and "
        "paragraphs cleanly. Output ONLY the document text. "
        "If there is no speech, silence, or only background noise, output ABSOLUTELY NOTHING."
    ),
    "chat": (
        "You are a real-time messaging voice assistant for Slack, WhatsApp, and Discord. "
        "Transcribe spoken audio into friendly, natural chat messages. Preserve emojis if mentioned, "
        "keep casual phrasing and clean conversational punctuation. Output ONLY the chat message. "
        "If there is no speech, silence, or only background noise, output ABSOLUTELY NOTHING."
    ),
    "verbatim": (
        "Transcribe the audio word-for-word exactly as spoken. Output ONLY the verbatim transcription. "
        "If there is no speech, silence, or only background noise, output ABSOLUTELY NOTHING."
    ),
    "bullet_notes": (
        "Transcribe the spoken audio into concise markdown bullet points (- Item). "
        "Output ONLY the bulleted list. "
        "If there is no speech, silence, or only background noise, output ABSOLUTELY NOTHING."
    )
}

FALLBACK_MODELS = [
    "gemini-3.5-flash-lite",
    "gemini-3.5-flash",
    "gemini-3-flash-preview",
    "gemini-2.5-flash"
]

SILENCE_ARTIFACTS = {
    "[silence]", "[no speech]", "[music]", "[tones]", "<noise>",
    "[audio detected]", "[]", "(silence)", "[blank_audio]",
    "(blank audio)", "...", ".", "none", "[silence.]"
}

class AIEngine:
    def __init__(self, config_manager):
        self.config = config_manager
        self.working_model = self.config.get("model", "gemini-3.5-flash-lite")
        self.session = requests.Session()
        adapter = HTTPAdapter(pool_connections=5, pool_maxsize=10)
        self.session.mount("https://", adapter)
        self.session.mount("http://", adapter)

    @staticmethod
    def _clean_transcript(raw_text: str) -> str:
        """Strips markdown fences, wrapping quotes, timestamps, and acoustic silence artifacts."""
        if not raw_text:
            return ""
        text = raw_text.strip()
        # Filter markdown code fences if wrapped
        if text.startswith("```") and text.endswith("```"):
            lines = text.splitlines()
            if len(lines) >= 2:
                text = "\n".join(lines[1:-1]).strip()
        # Filter wrapping quotes
        if (text.startswith('"') and text.endswith('"')) or (text.startswith("'") and text.endswith("'")):
            text = text[1:-1].strip()
        # Filter timestamp hallucinations e.g. "00:00", "0:00", "00:00 - 00:01"
        if re.match(r"^(\d{1,2}:\d{2})(\s*-\s*\d{1,2}:\d{2})?\.?$", text):
            return ""
        # Filter silence/noise marker strings
        if text.lower() in SILENCE_ARTIFACTS:
            return ""
        return text

    def transcribe_audio(self, audio_wav_bytes: bytes, mode: str = None) -> Tuple[str, float]:
        start_time = time.time()

        if not audio_wav_bytes or len(audio_wav_bytes) < 1000:
            return "", 0.0

        api_key = self.config.get("google_api_key", "").strip()
        if not api_key:
            raise ValueError("Google AI Studio API Key is missing! Please configure it in Settings.")

        if not mode:
            mode = self.config.get("dictation_mode", "smart_flow")

        system_instruction = DICTATION_PROMPTS.get(mode, DICTATION_PROMPTS["smart_flow"])
        custom_instructions = self.config.get("custom_instructions", "").strip()
        if custom_instructions:
            system_instruction += f"\n\nUser Vocabulary / Jargon:\n{custom_instructions}"

        audio_b64 = base64.b64encode(audio_wav_bytes).decode("utf-8")
        models_to_try = [self.working_model] + [m for m in FALLBACK_MODELS if m != self.working_model]

        last_error = None
        for model in models_to_try:
            url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
            payload = {
                "contents": [
                    {
                        "parts": [
                            {
                                "inlineData": {
                                    "mimeType": "audio/wav",
                                    "data": audio_b64
                                }
                            },
                            {
                                "text": system_instruction
                            }
                        ]
                    }
                ],
                "generationConfig": {
                    "temperature": 0.1,
                    "maxOutputTokens": 2048
                }
            }

            try:
                response = self.session.post(url, json=payload, timeout=12)
                latency = time.time() - start_time

                if response.status_code == 200:
                    data = response.json()
                    candidates = data.get("candidates", [])
                    if candidates:
                        parts = candidates[0].get("content", {}).get("parts", [])
                        if parts:
                            text = parts[0].get("text", "")
                            cleaned = self._clean_transcript(text)
                            self.working_model = model
                            self.config.set("model", model, save=False)
                            return cleaned, latency
                    return "", latency
                elif response.status_code == 404:
                    continue
                elif response.status_code in (401, 403):
                    raise ValueError("Invalid Google AI Studio API Key. Please verify your key.")
                elif response.status_code == 429:
                    last_error = f"Gemini API Quota Exceeded (HTTP 429): {response.text[:150]}"
                    continue
                else:
                    last_error = f"API Error ({response.status_code}): {response.text[:150]}"
            except requests.exceptions.Timeout:
                continue
            except ValueError:
                raise
            except Exception as e:
                last_error = str(e)

        raise RuntimeError(last_error or "Unable to transcribe audio with Gemini models.")

    def test_connection(self, api_key: str = None) -> Tuple[bool, str, float]:
        if not api_key:
            api_key = self.config.get("google_api_key", "").strip()
        if not api_key:
            return False, "API Key is empty", 0.0

        start_time = time.time()
        models_to_try = [self.working_model] + [m for m in FALLBACK_MODELS if m != self.working_model]

        for model in models_to_try:
            url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
            payload = {
                "contents": [{"parts": [{"text": "OK"}]}],
                "generationConfig": {"maxOutputTokens": 5}
            }

            try:
                resp = self.session.post(url, json=payload, timeout=6)
                latency = time.time() - start_time
                if resp.status_code == 200:
                    self.working_model = model
                    self.config.set("model", model, save=True)
                    return True, f"Connected to {model}! ({int(latency * 1000)}ms)", latency
                elif resp.status_code in (401, 403):
                    return False, "Invalid API Key. Please check your key.", latency
            except Exception:
                continue

        return False, "Connection test failed across Gemini models.", 0.0

    def test_key(self, api_key: str = None) -> Tuple[bool, str]:
        """Fast lightweight ping to Google AI Studio to validate an API key."""
        if not api_key:
            api_key = self.config.get("google_api_key", "").strip()
        if not api_key:
            return False, "Invalid Key"

        try:
            url = f"https://generativelanguage.googleapis.com/v1beta/models?key={api_key}"
            resp = self.session.get(url, timeout=5)
            if resp.status_code == 200:
                return True, "Valid Key"
            elif resp.status_code in (400, 401, 403):
                return False, "Invalid Key"
            else:
                ok, _, _ = self.test_connection(api_key)
                return (True, "Valid Key") if ok else (False, "Invalid Key")
        except Exception:
            ok, _, _ = self.test_connection(api_key)
            return (True, "Valid Key") if ok else (False, "Invalid Key")

