package com.example.ownvoice.core

object TonePromptManager {

    private val PROMPTS = mapOf(
        "smart_flow" to (
            "You are an expert real-time voice dictation assistant (like Wispr Flow). " +
            "Transcribe everything said in this audio recording into clean, natural written text. " +
            "Remove filler words (um, uh, like, you know, ah, so yeah) and hesitation sounds. " +
            "Apply proper punctuation, capitalization, and formatting. " +
            "Output ONLY the transcribed words with zero preamble, no quotes, and no markdown fences. " +
            "If there is no speech, silence, or only background noise, output ABSOLUTELY NOTHING."
        ),
        "code" to (
            "You are a coding voice assistant. Transcribe the spoken audio into precise code, " +
            "terminal commands, or variable names. Format identifiers in appropriate casing " +
            "(camelCase, snake_case, PascalCase, kebab-case), wrap inline code in backticks. " +
            "Output ONLY the code or commands with zero explanations. " +
            "If there is no speech, silence, or only background noise, output ABSOLUTELY NOTHING."
        ),
        "formal_email" to (
            "You are an executive email assistant. Transcribe the spoken audio into a professional, " +
            "well-structured email message with appropriate greetings, body paragraphs, and sign-offs. " +
            "Output ONLY the clean email text. " +
            "If there is no speech, silence, or only background noise, output ABSOLUTELY NOTHING."
        ),
        "formal_document" to (
            "You are an executive documentation assistant. Transcribe the spoken audio into structured, " +
            "formal prose suitable for documents. Format bullet points (- Item) and paragraphs cleanly. " +
            "Output ONLY the document text. " +
            "If there is no speech, silence, or only background noise, output ABSOLUTELY NOTHING."
        ),
        "chat" to (
            "You are a real-time messaging voice assistant for Slack, WhatsApp, and Discord. " +
            "Transcribe spoken audio into friendly, natural chat messages. Preserve emojis if mentioned, " +
            "keep casual phrasing and clean conversational punctuation. Output ONLY the chat message. " +
            "If there is no speech, silence, or only background noise, output ABSOLUTELY NOTHING."
        ),
        "verbatim" to (
            "Transcribe the audio word-for-word exactly as spoken. Output ONLY the verbatim transcription. " +
            "If there is no speech, silence, or only background noise, output ABSOLUTELY NOTHING."
        ),
        "bullet_notes" to (
            "Transcribe the spoken audio into concise markdown bullet points (- Item). " +
            "Output ONLY the bulleted list. " +
            "If there is no speech, silence, or only background noise, output ABSOLUTELY NOTHING."
        ),
        "search" to (
            "You are a search query assistant. Transcribe the spoken audio into a crisp, concise search query. " +
            "Strip conversational fluff (like 'Can you search for', 'Please find me', 'I want to see'). " +
            "Output ONLY the essential search keywords with zero punctuation or preamble. " +
            "If there is no speech, silence, or only background noise, output ABSOLUTELY NOTHING."
        ),
        "translate_hindi" to (
            "You are an expert real-time English-to-Hindi translator. Translate whatever is spoken in English " +
            "into natural, fluent Hindi (written in Devanagari script). " +
            "Output ONLY the translated Hindi text with zero explanations or preamble. " +
            "If there is no speech, silence, or only background noise, output ABSOLUTELY NOTHING."
        ),
        "translate_english" to (
            "You are an expert real-time Hindi-to-English translator. Translate whatever is spoken in Hindi or Hinglish " +
            "into natural, fluent, professional English. " +
            "Output ONLY the translated English text with zero explanations or preamble. " +
            "If there is no speech, silence, or only background noise, output ABSOLUTELY NOTHING."
        )
    )

    fun detectModeForPackage(packageName: String): String {
        val pkg = packageName.lowercase()
        return when {
            pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("discord") ||
            pkg.contains("signal") || pkg.contains("messaging") || pkg.contains("orca") ||
            pkg.contains("messenger") || pkg.contains("slack") -> "chat"

            pkg.contains("gmail") || pkg.contains("outlook") || pkg.contains("mail") ||
            pkg.contains("email") || pkg.contains(".gm") -> "formal_email"

            pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("search") ||
            pkg.contains("youtube") || pkg.contains("spotify") || pkg.contains("amazon") ||
            pkg.contains("flipkart") -> "search"

            pkg.contains("termux") || pkg.contains("github") || pkg.contains("code") ||
            pkg.contains("terminal") || pkg.contains("ide") -> "code"

            pkg.contains("keep") || pkg.contains("notes") || pkg.contains("notion") ||
            pkg.contains("word") || pkg.contains("docs") -> "formal_document"

            else -> "smart_flow"
        }
    }

    fun getPrompt(
        mode: String,
        customInstructions: String = "",
        vocabulary: List<String> = emptyList(),
        snippets: Map<String, String> = emptyMap(),
        enableSelfCorrection: Boolean = true
    ): String {
        val basePrompt = PROMPTS[mode] ?: PROMPTS["smart_flow"]!!
        val builder = StringBuilder(basePrompt)

        if (enableSelfCorrection) {
            builder.append("\n\nSELF-CORRECTION & SPOKEN COMMANDS:")
            builder.append("\n- ELIMINATE MISTAKES: If the speaker stumbles, corrects numbers, dates, or names, or changes their mind mid-sentence (e.g. 'call at two, no wait, three PM', 'twenty, sorry thirty-five', 'send to Bob, I mean Alice'), output ONLY the final corrected thought. Never include the mistake or false start.")
            builder.append("\n- ERASURE COMMANDS: If the speaker says ONLY 'scratch that', 'delete that', 'undo that', 'clear that', or 'erase that', output exactly '[COMMAND:DELETE_LAST]'.")
            builder.append("\n- CLEAR ALL COMMANDS: If the speaker says ONLY 'clear all', 'delete line', or 'clear text', output exactly '[COMMAND:CLEAR_ALL]'.")
            builder.append("\n- CLIPBOARD PASTE COMMAND: If the speaker says ONLY 'paste', 'paste it', 'paste that', or 'paste it here', output exactly '[COMMAND:PASTE]'.")
            builder.append("\n- FORMATTING COMMANDS: If the speaker says 'new line' or 'next line', insert a newline. If they say 'new paragraph', insert two newlines.")
        }

        if (snippets.isNotEmpty()) {
            builder.append("\n\nVOICE SNIPPETS & CONTEXTUAL SUBSTITUTIONS:")
            builder.append("\nThe user has predefined personal snippets. Understand conversational intent when they refer to them:")
            snippets.forEach { (trigger, expansion) ->
                builder.append("\n- When the speaker mentions '$trigger' or asks to insert/give/put/share it (e.g. 'put my email here', 'send my email', 'give the meeting link', 'here is my meeting link'), substitute: $expansion")
            }
            builder.append("\nEnsure the substitution flows naturally in the sentence without leaving awkward carrier words like 'put ... here'.")
        }

        if (vocabulary.isNotEmpty()) {
            builder.append("\n\nPRIORITIZED VOCABULARY & NAMES (use exact spelling):")
            builder.append("\n${vocabulary.joinToString(", ")}")
        }

        if (customInstructions.isNotBlank()) {
            builder.append("\n\nUSER CUSTOM INSTRUCTIONS:\n${customInstructions.trim()}")
        }

        return builder.toString()
    }

    fun cleanTranscript(rawText: String): String {
        if (rawText.isBlank()) return ""
        var text = rawText.trim()

        // Strip markdown code fences if wrapped
        if (text.startsWith("```") && text.endsWith("```")) {
            val lines = text.lines()
            if (lines.size >= 2) {
                text = lines.subList(1, lines.size - 1).joinToString("\n").trim()
            }
        }

        // Strip wrapping quotes
        if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'"))) {
            text = text.substring(1, text.length - 1).trim()
        }

        // Filter silence/timestamp hallucinations
        val silenceTokens = setOf(
            "[silence]", "[no speech]", "[music]", "[tones]", "<noise>",
            "[audio detected]", "[]", "(silence)", "[blank_audio]",
            "(blank audio)", "...", ".", "none", "[silence.]"
        )
        if (text.lowercase() in silenceTokens) {
            return ""
        }

        if (text.matches(Regex("^(\\d{1,2}:\\d{2})(\\s*-\\s*\\d{1,2}:\\d{2})?\\.?$"))) {
            return ""
        }

        return text
    }
}
