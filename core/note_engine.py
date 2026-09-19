import json
import base64
import time
import re
from typing import Dict, Any, Optional, Tuple
import requests
from requests.adapters import HTTPAdapter
from core.offline_engine import OfflineWhisperEngine

NOTE_SYSTEM_PROMPT = """You are an elite executive Chief of Staff and AI Note Architect.
Your task is to take raw, rambling spoken thoughts (or meeting transcripts) and compile them into clean, highly structured, production-grade notes.

You must output a single valid JSON object with the following schema:
{
  "title": "Short, punchy title (3-6 words)",
  "category": "brain_dump" | "meeting" | "task_list" | "standup" | "memo" | "general",
  "tags": ["tag1", "tag2", "tag3"],
  "summary": "1-2 sentence executive summary",
  "structured_content": "Full markdown note with clean headers, bullet points, and '- [ ]' action items",
  "raw_transcript": "The cleaned verbatim transcript of what was spoken"
}

ARCHITECTURAL RULES FOR 'structured_content':
1. BRAIN DUMP:
   ### 💡 Executive Summary
   ### 📌 Key Pillars & Insights
   ### ⚡ Action Items & Next Steps (- [ ] ...)

2. MEETING:
   ### 🎯 Objective & Context
   ### 🗣️ Discussion Points
   ### 🤝 Decisions Made
   ### 📋 Action Items & Owners (- [ ] ...)
   ### ❓ Open Questions

3. TASK LIST:
   ### 📋 Priority Checklist
   - [ ] [High] ...
   - [ ] [Medium] ...
   - [ ] [Low] ...

4. STANDUP:
   ### ✅ Completed (Yesterday)
   ### 🚀 In Progress (Today)
   ### 🚧 Blockers & Risks

5. CONTEXTUAL TRANSFORM (when reference text is provided):
   Apply the user's spoken instruction strictly to the provided reference text. Output the transformed result directly.

Output ONLY valid JSON. Zero markdown fences (` ```json `), zero preamble, zero conversational commentary.
"""

class NoteEngine:
    """
    Transforms raw speech or transcripts into cleanly structured notes.
    """

    def __init__(self, config_manager):
        self.config = config_manager
        self.session = requests.Session()
        adapter = HTTPAdapter(pool_connections=5, pool_maxsize=10)
        self.session.mount("https://", adapter)
        self.session.mount("http://", adapter)
        self.offline_engine = OfflineWhisperEngine()

    def structure_note(
        self,
        audio_wav_bytes: Optional[bytes] = None,
        raw_text: Optional[str] = None,
        category: str = "auto",
        selection_context: Optional[str] = None,
        source: str = "desktop"
    ) -> Dict[str, Any]:
        """
        Takes spoken audio WAV or raw transcript text and compiles it into a structured note.
        """
        api_key = self.config.get("google_api_key", "").strip()
        model = self.config.get("model", "gemini-3.6-flash")

        # Fallback offline if no API key
        if not api_key:
            return self._offline_structure(audio_wav_bytes, raw_text, category, selection_context, source)

        prompt = NOTE_SYSTEM_PROMPT
        if category and category != "auto":
            prompt += f"\n\nFORCED CATEGORY: Structure specifically as '{category}'."

        if selection_context:
            prompt += f"\n\nCURRENT ON-SCREEN SELECTION CONTEXT:\n\"\"\"\n{selection_context}\n\"\"\"\nThe user's spoken words are an instruction to transform, summarize, or act upon this selected text."

        user_content_parts = []
        if audio_wav_bytes and len(audio_wav_bytes) >= 1000:
            audio_b64 = base64.b64encode(audio_wav_bytes).decode("utf-8")
            user_content_parts.append({
                "inlineData": {
                    "mimeType": "audio/wav",
                    "data": audio_b64
                }
            })
            user_content_parts.append({
                "text": f"{prompt}\n\nPlease listen to this audio and compile the structured note JSON."
            })
        elif raw_text:
            user_content_parts.append({
                "text": f"{prompt}\n\nRAW USER INPUT TO STRUCTURE:\n\"\"\"\n{raw_text}\n\"\"\"\n\nCompile into the structured note JSON."
            })
        else:
            return {
                "title": "Empty Note",
                "category": "general",
                "tags": ["empty"],
                "summary": "No audio or text provided.",
                "structured_content": "",
                "raw_transcript": "",
                "source": source
            }

        candidate_models = [model] + [m for m in ["gemini-3.6-flash", "gemini-3.5-flash-lite", "gemini-3.5-flash"] if m != model]
        for m in candidate_models:
            url = f"https://generativelanguage.googleapis.com/v1beta/models/{m}:generateContent?key={api_key}"
            payload = {
                "contents": [{"parts": user_content_parts}],
                "generationConfig": {
                    "temperature": 0.2,
                    "maxOutputTokens": 4096,
                    "responseMimeType": "application/json"
                }
            }

            try:
                resp = self.session.post(url, json=payload, timeout=15)
                if resp.status_code == 200:
                    data = resp.json()
                    candidates = data.get("candidates", [])
                    if candidates:
                        parts = candidates[0].get("content", {}).get("parts", [])
                        if parts:
                            text_resp = parts[0].get("text", "").strip()
                            parsed = self._extract_json(text_resp)
                            if parsed:
                                parsed["source"] = source
                                return parsed
            except Exception:
                continue

        # If online structuring fails, fallback gracefully
        return self._offline_structure(audio_wav_bytes, raw_text, category, selection_context, source)

    def _extract_json(self, text: str) -> Optional[Dict[str, Any]]:
        """Safely parses JSON even if wrapped in markdown code blocks."""
        cleaned = text.strip()
        if cleaned.startswith("```"):
            cleaned = re.sub(r"^```[a-zA-Z]*\n", "", cleaned)
            cleaned = re.sub(r"\n```$", "", cleaned)
            cleaned = cleaned.strip()

        try:
            data = json.loads(cleaned)
            if isinstance(data, dict):
                # Ensure all required keys exist
                data.setdefault("title", "Voice Note")
                data.setdefault("category", "general")
                data.setdefault("tags", [])
                data.setdefault("summary", "")
                data.setdefault("structured_content", "")
                data.setdefault("raw_transcript", "")
                return data
        except Exception:
            pass
        return None

    def _offline_structure(
        self,
        audio_wav_bytes: Optional[bytes],
        raw_text: Optional[str],
        category: str,
        selection_context: Optional[str],
        source: str
    ) -> Dict[str, Any]:
        """Deterministic offline fallback if cloud API is unavailable."""
        transcript = raw_text or ""
        if not transcript and audio_wav_bytes:
            try:
                transcript, _ = self.offline_engine.transcribe(audio_wav_bytes)
            except Exception:
                transcript = ""

        transcript = transcript.strip()
        title = "Spoken Note"
        if transcript:
            first_sentence = re.split(r"[.!?\n]", transcript)[0]
            words = first_sentence.split()[:6]
            title = " ".join(words).capitalize() if words else "Spoken Note"

        structured = f"### 📝 Notes\n- {transcript}\n\n### ⚡ Action Items\n- [ ] Review note contents"
        if selection_context:
            structured = f"### 📌 Context\n> {selection_context[:200]}...\n\n" + structured

        return {
            "title": title,
            "category": category if category != "auto" else "general",
            "tags": ["voice-note", "offline"],
            "summary": transcript[:140] + ("..." if len(transcript) > 140 else ""),
            "structured_content": structured,
            "raw_transcript": transcript,
            "source": source
        }
