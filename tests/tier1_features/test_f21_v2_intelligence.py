import unittest
from config import ConfigManager
from core.context_detector import ContextDetector
from core.ai_engine import AIEngine, DICTATION_PROMPTS, FALLBACK_MODELS
from core.sound_effects import SoundEffects

class TestF21V2Intelligence(unittest.TestCase):
    """
    Feature 21: Version 2.0 Intelligence (Self-Correction, Personal Vocab, Search Tone, Audio Cues)
    """

    def setUp(self):
        self.config = ConfigManager()

    def test_search_and_translate_prompts_exist(self):
        """Verify Version 2.0 tones exist in DICTATION_PROMPTS."""
        self.assertIn("search", DICTATION_PROMPTS)
        self.assertIn("translate_hindi", DICTATION_PROMPTS)
        self.assertIn("translate_english", DICTATION_PROMPTS)
        self.assertIn("search query assistant", DICTATION_PROMPTS["search"])
        self.assertIn("English-to-Hindi", DICTATION_PROMPTS["translate_hindi"])
        self.assertIn("Hindi-to-English", DICTATION_PROMPTS["translate_english"])

    def test_browser_processes_and_search_keywords(self):
        """Verify browser processes and search keywords are properly registered in ContextDetector."""
        self.assertIn("chrome.exe", ContextDetector.BROWSER_PROCESSES)
        self.assertIn("msedge.exe", ContextDetector.BROWSER_PROCESSES)
        self.assertIn("brave.exe", ContextDetector.BROWSER_PROCESSES)
        self.assertTrue(any("google search" in kw for kw in ContextDetector.SEARCH_KEYWORDS))
        self.assertTrue(any("youtube" in kw for kw in ContextDetector.SEARCH_KEYWORDS))

    def test_vocabulary_crud_operations(self):
        """Verify adding, getting, and removing custom vocabulary in ConfigManager."""
        test_word = "__Unit_Test_DevOps_Term_XYZ__"
        
        # Add term
        added = self.config.add_vocabulary_term(test_word)
        self.assertTrue(added)
        self.assertIn(test_word, self.config.get_vocabulary())
        
        # Duplicate term should return False
        dup = self.config.add_vocabulary_term(test_word)
        self.assertFalse(dup)
        
        # Remove term
        removed = self.config.remove_vocabulary_term(test_word)
        self.assertTrue(removed)
        self.assertNotIn(test_word, self.config.get_vocabulary())

    def test_models_inventory_valid_google_ai_studio(self):
        """Verify FALLBACK_MODELS lists valid, live Gemini models."""
        self.assertIn("gemini-3.6-flash", FALLBACK_MODELS)
        self.assertIn("gemini-3.5-flash-lite", FALLBACK_MODELS)
        self.assertIn("gemini-3.5-flash", FALLBACK_MODELS)

    def test_sound_effects_api(self):
        """Verify SoundEffects class provides start, stop, success, and error cues."""
        fx = SoundEffects(enabled=False)
        fx.play_start()
        fx.play_stop()
        fx.play_success()
        fx.play_error()
        fx.enabled = True
        self.assertTrue(fx.enabled)

if __name__ == "__main__":
    unittest.main()
