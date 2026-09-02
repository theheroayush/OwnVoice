import base64
import time
import requests
from typing import Tuple

DICTATION_PROMPTS = {
    "smart_flow": (
        "You are an expert real-time voice dictation assistant (like Wispr Flow). "
        "Transcribe everything said in this audio recording into clean, natural written text. "
        "Remove filler words (um, uh, like, you know, ah, so yeah) and hesitation sounds. "
        "Apply proper punctuation, capitalization, and formatting. "
        "Output ONLY the transcribed words with zero preamble, no quotes, and no markdown fences."
    ),
    "verbatim": (
        "Transcribe the audio word-for-word exactly as spoken. "
        "Output ONLY the verbatim transcription."
    ),
    "code": (
        "Transcribe the spoken audio into code, terminal commands, or variable names. "
        "Format identifiers in appropriate casing (camelCase, snake_case), wrap code in backticks. "
        "Output ONLY the code."
    ),
    "bullet_notes": (
        "Transcribe the spoken audio into concise markdown bullet points (- Item). "
        "Output ONLY the bulleted list."
    ),
    "formal_email": (
        "Transcribe the spoken audio into a professional, well-formatted email message. "
        "Output ONLY the email text."
    )
}

FALLBACK_MODELS = [
    "gemini-3.5-flash-lite",
    "gemini-3-flash-preview",
    "gemini-3.5-flash"
]

class AIEngine:
    def __init__(self, config_manager):
        self.config = config_manager
        self.working_model = self.config.get("model", "gemini-3.5-flash-lite")

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
                response = requests.post(url, json=payload, timeout=12)
                latency = time.time() - start_time

                if response.status_code == 200:
                    data = response.json()
                    candidates = data.get("candidates", [])
                    if candidates:
                        parts = candidates[0].get("content", {}).get("parts", [])
                        if parts:
                            text = parts[0].get("text", "").strip()
                            for artifact in ["[silence]", "[no speech]", "[music]", "[tones]", "<noise>", "[audio detected]", "[]"]:
                                if text.lower() == artifact:
                                    text = ""
                            self.working_model = model
                            self.config.set("model", model, save=False)
                            return text, latency
                    return "", latency
                elif response.status_code == 404:
                    continue
                elif response.status_code in (401, 403):
                    raise ValueError("Invalid Google AI Studio API Key. Please verify your key.")
                else:
                    last_error = f"API Error ({response.status_code}): {response.text[:150]}"
            except requests.exceptions.Timeout:
                continue
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
                resp = requests.post(url, json=payload, timeout=6)
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
