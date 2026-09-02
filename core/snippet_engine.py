import re
from typing import Dict

class SnippetEngine:
    """
    High-speed voice snippet expansion engine.
    Expands spoken triggers into multi-line snippets, URLs, or templates.
    """
    def __init__(self, config_manager):
        self.config = config_manager

    def get_snippets(self) -> Dict[str, str]:
        return self.config.get("snippets", {
            "my email": "ayush@example.com",
            "meeting link": "https://cal.com/ayush",
            "sign off": "Best regards,\nAyush Upadhyay"
        })

    def save_snippets(self, snippets: Dict[str, str]):
        self.config.set("snippets", snippets, save=True)

    def expand(self, text: str) -> str:
        snippets = self.get_snippets()
        if not snippets or not text:
            return text

        # Sort by trigger length descending to match longer phrases first
        for trigger, expansion in sorted(snippets.items(), key=lambda x: len(x[0]), reverse=True):
            clean_trig = trigger.strip()
            if not clean_trig:
                continue
            # Whole phrase match with word boundaries
            pattern = re.compile(rf"\b{re.escape(clean_trig)}\b", re.IGNORECASE)
            text = pattern.sub(expansion, text)

        return text
