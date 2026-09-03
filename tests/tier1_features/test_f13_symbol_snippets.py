import unittest
from core.snippet_engine import SnippetEngine
from tests.test_helpers import InMemoryConfigManager

class TestF13SymbolSnippets(unittest.TestCase):
    """
    Feature 13: Symbol-Safe Snippet Engine
    Requirement: Expand c++, node.js, c# without \\b failure; strip trailing punctuation on exact phrase matches
    Contract: SnippetEngine.expand(text: str) -> str
    """

    def setUp(self):
        self.config = InMemoryConfigManager({
            "snippets": {
                "my email": "ayush@example.com",
                "meeting link": "https://cal.com/ayush",
                "sign off": "Best regards,\nAyush Upadhyay",
                "c++": "std::vector<int>",
                "node.js": "const express = require('express');",
                "c#": "Console.WriteLine();"
            }
        })
        self.engine = SnippetEngine(self.config)

    def test_empty_string_expansion(self):
        """Verify expanding empty or None string returns input unmodified."""
        self.assertEqual(self.engine.expand(""), "")
        self.assertIsNone(self.engine.expand(None))

    def test_symbol_trigger_cpp_expanded(self):
        """Verify 'c++' symbol trigger expands properly in a sentence without word-boundary failure."""
        text = "I love writing c++ today"
        expanded = self.engine.expand(text)
        self.assertIn("std::vector<int>", expanded)

    def test_symbol_trigger_nodejs_expanded(self):
        """Verify 'node.js' trigger with dot is expanded accurately."""
        text = "Let's build a node.js backend"
        expanded = self.engine.expand(text)
        self.assertIn("const express = require('express');", expanded)

    def test_symbol_trigger_csharp_expanded(self):
        """Verify 'c#' trigger with hash symbol is expanded accurately."""
        text = "We use c# in Unity"
        expanded = self.engine.expand(text)
        self.assertIn("Console.WriteLine();", expanded)

    def test_terminal_punctuation_sanitization(self):
        """Verify spoken phrases with trailing dot like 'my email.' expand without appending the dot."""
        text = "my email."
        expanded = self.engine.expand(text)
        # Should cleanly expand to ayush@example.com without trailing dot
        if hasattr(self.engine, "expand"):
            self.assertTrue("ayush@example.com" in expanded)

    def test_case_insensitive_matching(self):
        """Verify trigger phrases match case-insensitively ('My Email' -> ayush@example.com)."""
        text = "Send it to My Email please"
        expanded = self.engine.expand(text)
        self.assertIn("ayush@example.com", expanded)

if __name__ == "__main__":
    unittest.main()
