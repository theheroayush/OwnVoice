package com.example.ownvoice.core

import java.util.regex.Pattern

class SnippetEngine(private val config: SecureConfig) {

    fun expand(text: String): String {
        if (text.isBlank()) return text
        val snippets = config.getSnippets()
        if (snippets.isEmpty()) return text

        var workingText = text

        // 1. Exact phrase match with terminal punctuation stripped
        val stripped = workingText.trim()
        val terminalStripped = stripped.trimEnd('.', '!', '?', ';', ':')
        for ((trigger, expansion) in snippets) {
            if (terminalStripped.equals(trigger.trim(), ignoreCase = true)) {
                return expansion
            }
        }

        // 2. Substring matching ordered by trigger length descending
        val sortedSnippets = snippets.entries.sortedByDescending { it.key.length }
        for ((trigger, expansion) in sortedSnippets) {
            val cleanTrigger = trigger.trim()
            if (cleanTrigger.isEmpty()) continue

            // Symbol-safe boundary: (?<!\w)trigger(?!\w)
            val regexPattern = "(?i)(?<!\\w)" + Pattern.quote(cleanTrigger) + "(?!\\w)"
            workingText = workingText.replace(Regex(regexPattern), expansion)
        }

        return workingText
    }
}
