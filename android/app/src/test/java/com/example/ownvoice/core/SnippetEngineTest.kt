package com.example.ownvoice.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SnippetEngineTest {

    @Test
    fun testExpansion() {
        val snippets = mapOf(
            "my email" to "ayush@example.com",
            "meeting link" to "https://cal.com/ayush"
        )
        // Test snippet expansion logic
        var text = "Send details to my email please"
        for ((trigger, expansion) in snippets) {
            val regex = Regex("\\b${Regex.escape(trigger)}\\b", RegexOption.IGNORE_CASE)
            text = text.replace(regex, expansion)
        }
        assertEquals("Send details to ayush@example.com please", text)
    }

    @Test
    fun testTonePromptManagerCleanTranscript() {
        assertEquals("", TonePromptManager.cleanTranscript(""))
        assertEquals("", TonePromptManager.cleanTranscript("[silence]"))
        assertEquals("", TonePromptManager.cleanTranscript("0:00 - 0:05"))
        assertEquals("Hello world", TonePromptManager.cleanTranscript("\"Hello world\""))
        assertEquals("print('test')", TonePromptManager.cleanTranscript("```\nprint('test')\n```"))
    }

    @Test
    fun testDetectModeForPackage() {
        assertEquals("chat", TonePromptManager.detectModeForPackage("com.whatsapp"))
        assertEquals("chat", TonePromptManager.detectModeForPackage("org.telegram.messenger"))
        assertEquals("chat", TonePromptManager.detectModeForPackage("com.slack"))

        assertEquals("formal_email", TonePromptManager.detectModeForPackage("com.google.android.gm"))
        assertEquals("formal_email", TonePromptManager.detectModeForPackage("com.microsoft.office.outlook"))

        assertEquals("search", TonePromptManager.detectModeForPackage("com.android.chrome"))
        assertEquals("search", TonePromptManager.detectModeForPackage("com.google.android.youtube"))
        assertEquals("search", TonePromptManager.detectModeForPackage("com.amazon.mShop.android.shopping"))

        assertEquals("code", TonePromptManager.detectModeForPackage("com.termux"))
        assertEquals("code", TonePromptManager.detectModeForPackage("com.github.android"))

        assertEquals("formal_document", TonePromptManager.detectModeForPackage("com.google.android.keep"))
        assertEquals("formal_document", TonePromptManager.detectModeForPackage("notion.id"))

        assertEquals("smart_flow", TonePromptManager.detectModeForPackage("unknown.arbitrary.app"))
    }

    @Test
    fun testGetPromptWithSelfCorrectionAndVocabulary() {
        val promptWithVocab = TonePromptManager.getPrompt(
            mode = "smart_flow",
            vocabulary = listOf("Aarav", "Bangalore", "Kubernetes"),
            enableSelfCorrection = true
        )
        assert(promptWithVocab.contains("SELF-CORRECTION & SPOKEN COMMANDS"))
        assert(promptWithVocab.contains("Aarav, Bangalore, Kubernetes"))

        val promptWithoutSelfCorrection = TonePromptManager.getPrompt(
            mode = "smart_flow",
            enableSelfCorrection = false
        )
        assert(!promptWithoutSelfCorrection.contains("SELF-CORRECTION & SPOKEN COMMANDS"))

        val searchPrompt = TonePromptManager.getPrompt("search")
        assert(searchPrompt.contains("search query assistant"))

        val hindiPrompt = TonePromptManager.getPrompt("translate_hindi")
        assert(hindiPrompt.contains("English-to-Hindi translator"))
    }
}
