import os
import sys
import json
from pathlib import Path
from dotenv import load_dotenv

if getattr(sys, "frozen", False):
    BASE_DIR = Path(sys.executable).resolve().parent
else:
    BASE_DIR = Path(__file__).resolve().parent

CONFIG_FILE = BASE_DIR / "config.json"
HISTORY_FILE = BASE_DIR / "history.json"
ENV_FILE = BASE_DIR / ".env"

if ENV_FILE.exists():
    load_dotenv(ENV_FILE)

DEFAULT_CONFIG = {
    "google_api_key": os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY") or "",
    "model": "gemini-3.6-flash",
    "hotkey": "f8",
    "hotkey_mode": "toggle",
    "dictation_mode": "smart_flow",
    "auto_context": True,
    "self_correction": True,
    "vocabulary": [
        "Aarav",
        "Bengaluru",
        "Kubernetes",
        "B2B SaaS"
    ],
    "custom_instructions": "",
    "sound_effects": True,
    "input_device_index": None,
    "sample_rate": 48000,
    "overlay_position": "bottom_center",
    "auto_paste": True,
    "save_history": True,
    "history_limit": 100,
    "theme": "dark",
    "snippets": {
        "my portfolio": "https://ayushupadhyay.com",
        "meeting link": "https://cal.com/ayush",
        "my email": "ayush@example.com"
    }
}

class ConfigManager:
    def __init__(self):
        self.config = self.load_config()

    def load_config(self) -> dict:
        config = DEFAULT_CONFIG.copy()
        if CONFIG_FILE.exists():
            try:
                with open(CONFIG_FILE, "r", encoding="utf-8") as f:
                    saved = json.load(f)
                    config.update(saved)
            except Exception as e:
                print(f"Error reading config.json: {e}")
        if not config.get("google_api_key"):
            env_key = os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY")
            if env_key:
                config["google_api_key"] = env_key
        return config

    def save_config(self) -> bool:
        try:
            with open(CONFIG_FILE, "w", encoding="utf-8") as f:
                json.dump(self.config, f, indent=4)
            if self.config.get("google_api_key"):
                with open(ENV_FILE, "w", encoding="utf-8") as f:
                    f.write(f'GEMINI_API_KEY={self.config["google_api_key"]}\n')
            return True
        except Exception as e:
            print(f"Error saving config.json: {e}")
            return False

    def get(self, key, default=None):
        return self.config.get(key, default)

    def set(self, key, value, save=True):
        self.config[key] = value
        if save:
            self.save_config()

    def get_vocabulary(self) -> list:
        return self.config.get("vocabulary", ["Aarav", "Bengaluru", "Kubernetes", "B2B SaaS"])

    def add_vocabulary_term(self, term: str) -> bool:
        term = term.strip()
        if not term:
            return False
        vocab = list(self.get_vocabulary())
        if term not in vocab:
            vocab.append(term)
            self.set("vocabulary", vocab, save=True)
            return True
        return False

    def remove_vocabulary_term(self, term: str) -> bool:
        vocab = list(self.get_vocabulary())
        if term in vocab:
            vocab.remove(term)
            self.set("vocabulary", vocab, save=True)
            return True
        return False

    def load_history(self) -> list:
        if not HISTORY_FILE.exists():
            return []
        try:
            with open(HISTORY_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"Error reading history: {e}")
            return []

    def add_history_entry(self, entry: dict):
        if not self.config.get("save_history", True):
            return
        history = self.load_history()
        history.insert(0, entry)
        limit = self.config.get("history_limit", 100)
        history = history[:limit]
        try:
            with open(HISTORY_FILE, "w", encoding="utf-8") as f:
                json.dump(history, f, indent=2, ensure_ascii=False)
        except Exception as e:
            print(f"Error writing history: {e}")

    def clear_history(self):
        try:
            with open(HISTORY_FILE, "w", encoding="utf-8") as f:
                json.dump([], f)
        except Exception as e:
            print(f"Error clearing history: {e}")

config_manager = ConfigManager()
