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

    @Test
    fun testGetPromptWithSnippetsAndPasteCommand() {
        val prompt = TonePromptManager.getPrompt(
            mode = "smart_flow",
            snippets = mapOf("my email" to "ayush@example.com", "meeting link" to "https://cal.com/ayush"),
            enableSelfCorrection = true
        )
        assert(prompt.contains("CLIPBOARD PASTE COMMAND"))
        assert(prompt.contains("[COMMAND:PASTE]"))
        assert(prompt.contains("VOICE SNIPPETS & CONTEXTUAL SUBSTITUTIONS"))
        assert(prompt.contains("my email"))
        assert(prompt.contains("ayush@example.com"))
        assert(prompt.contains("meeting link"))
        assert(prompt.contains("https://cal.com/ayush"))
    }

    @Test
    fun testContextualCarrierPhraseLogic() {
        val snippets = mapOf(
            "my email" to "ayush@example.com",
            "meeting link" to "https://cal.com/ayush"
        )
        fun expandWithCarrier(input: String): String {
            var text = input
            val sorted = snippets.entries.sortedByDescending { it.key.length }
            for ((trigger, expansion) in sorted) {
                val clean = trigger.trim()
                val core = clean.replaceFirst(Regex("^(?i)(?:my|the)\\s+"), "").trim()
                val triggerRegexPart = if (core.isNotEmpty() && !core.equals(clean, ignoreCase = true)) {
                    "(?:" + java.util.regex.Pattern.quote(clean) + "|" + java.util.regex.Pattern.quote(core) + ")"
                } else {
                    java.util.regex.Pattern.quote(clean)
                }
                val carrierPattern = "(?i)\\b(?:please\\s+)?(?:put|insert|give|share|send|type|write|add|paste|here is|here's)\\s+(?:my|the)?\\s*" +
                    triggerRegexPart + "(?:\\s+(?:here|please|now|link))?\\b"
                text = text.replace(Regex(carrierPattern), expansion)
            }
            return text
        }

        assertEquals("ayush@example.com", expandWithCarrier("put my email here"))
        assertEquals("ayush@example.com", expandWithCarrier("put my email"))
        assertEquals("ayush@example.com", expandWithCarrier("share the email here"))
        assertEquals("https://cal.com/ayush", expandWithCarrier("give the meeting link"))
        assertEquals("https://cal.com/ayush", expandWithCarrier("please send the meeting link here"))
    }

    @Test
    fun testBridgeQrUriParsing() {
        val uriStr = "ownvoice://pair?ip=192.168.1.15&port=8765&name=Ayush-Laptop&pin=849201&token=7f8b9a1c4d2e"
        val parsed = java.net.URI(uriStr)
        assertEquals("ownvoice", parsed.scheme)
        assertEquals("pair", parsed.host)
        val queryPairs = parsed.query.split("&").associate {
            val parts = it.split("=")
            parts[0] to parts.getOrElse(1) { "" }
        }
        assertEquals("192.168.1.15", queryPairs["ip"])
        assertEquals("8765", queryPairs["port"])
        assertEquals("Ayush-Laptop", queryPairs["name"])
        assertEquals("849201", queryPairs["pin"])
        assertEquals("7f8b9a1c4d2e", queryPairs["token"])
    }

    @Test
    fun testBridgeDiscoveryPacketParsing() {
        val jsonStr = """
            {
                "service": "ownvoice-bridge",
                "version": "2.5.0",
                "device_name": "Ayush-PC",
                "ip": "192.168.1.50",
                "port": 8765,
                "pin": "123456",
                "token": "abcdef123456"
            }
        """.trimIndent()
        val json = org.json.JSONObject(jsonStr)
        assertEquals("ownvoice-bridge", json.getString("service"))
        assertEquals("Ayush-PC", json.getString("device_name"))
        assertEquals("192.168.1.50", json.getString("ip"))
        assertEquals(8765, json.getInt("port"))
        assertEquals("123456", json.getString("pin"))
        assertEquals("abcdef123456", json.getString("token"))
    }
}
