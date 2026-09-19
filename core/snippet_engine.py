import re
from typing import Dict

class SnippetEngine:
    """
    High-speed voice snippet expansion engine.
    Expands spoken triggers into multi-line snippets, URLs, or templates.
    """
    def __init__(self, config_manager=None):
        self.config = config_manager

    def get_snippets(self) -> Dict[str, str]:
        default_snippets = {
            "my email": "ayush@example.com",
            "meeting link": "https://cal.com/ayush",
            "sign off": "Best regards,\nAyush Upadhyay"
        }
        raw = self.config.get("snippets", default_snippets) if self.config else default_snippets
        out = {}
        for trig, val in raw.items():
            if isinstance(val, dict):
                out[trig] = val.get("expansion", "")
            else:
                out[trig] = str(val)
        return out

    def get_structured_snippets(self) -> Dict[str, dict]:
        raw = self.config.get("snippets", {}) if self.config else {}
        if not raw:
            raw = {
                "my email": {"expansion": "ayush@example.com", "category": "Email", "auto_format": True, "title": "My Email"},
                "meeting link": {"expansion": "https://cal.com/ayush", "category": "Meeting", "auto_format": True, "title": "Meeting Link"},
                "sign off": {"expansion": "Best regards,\nAyush Upadhyay", "category": "Text", "auto_format": True, "title": "Sign Off"}
            }
        out = {}
        for trig, val in raw.items():
            if isinstance(val, dict):
                out[trig] = {
                    "title": val.get("title", trig.title()),
                    "expansion": val.get("expansion", ""),
                    "category": val.get("category", "Text").capitalize(),
                    "auto_format": val.get("auto_format", True)
                }
            else:
                s_val = str(val)
                cat = "Link" if s_val.startswith("http") else ("Email" if "@" in s_val else "Text")
                out[trig] = {
                    "title": trig.title(),
                    "expansion": s_val,
                    "category": cat,
                    "auto_format": True
                }
        return out

    def save_snippets(self, snippets: Dict[str, str]):
        if self.config:
            self.config.set("snippets", snippets, save=True)

    def save_structured_snippet(self, trigger: str, expansion: str, category: str = "Text", auto_format: bool = True, title: str = ""):
        if not trigger or not expansion:
            return
        clean_trig = trigger.strip().lower()
        struct = self.get_structured_snippets()
        struct[clean_trig] = {
            "title": title.strip() or trigger.strip().title(),
            "expansion": expansion.strip(),
            "category": category.capitalize() if category else "Text",
            "auto_format": bool(auto_format)
        }
        if self.config:
            self.config.set("snippets", struct, save=True)

    def delete_snippet(self, trigger: str):
        clean_trig = trigger.strip().lower()
        struct = self.get_structured_snippets()
        keys_to_del = [k for k in struct if k.lower() == clean_trig]
        for k in keys_to_del:
            del struct[k]
        if self.config:
            self.config.set("snippets", struct, save=True)

    def expand(self, text: str) -> str:
        snippets = self.get_snippets()
        if not snippets or not text:
            return text

        # Exact phrase match with terminal punctuation stripping
        # e.g., 'My email.' or 'meeting link?' expands directly to snippet
        stripped = text.strip()
        terminal_stripped = stripped.rstrip(".!?,;:")
        for trigger, expansion in snippets.items():
            if terminal_stripped.lower() == trigger.strip().lower():
                return expansion

        # Sort by trigger length descending to match longer phrases first
        for trigger, expansion in sorted(snippets.items(), key=lambda x: len(x[0]), reverse=True):
            clean_trig = trigger.strip()
            if not clean_trig or clean_trig.lower() not in text.lower():
                continue
            # Symbol-safe lookaround assertion: prevents matching inside words, but supports c++, node.js, etc.
            pattern = re.compile(rf"(?<!\w){re.escape(clean_trig)}(?!\w)", re.IGNORECASE)
            text = pattern.sub(expansion, text)

        return text
