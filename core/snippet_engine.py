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
        if self.config:
            return self.config.get("snippets", default_snippets)
        return default_snippets

    def save_snippets(self, snippets: Dict[str, str]):
        if self.config:
            self.config.set("snippets", snippets, save=True)

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
