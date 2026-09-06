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

        // 2. Contextual carrier phrase matching (e.g. 'put my email here' -> email, 'give the meeting link' -> link)
        val sortedSnippets = snippets.entries.sortedByDescending { it.key.length }
        for ((trigger, expansion) in sortedSnippets) {
            val cleanTrigger = trigger.trim()
            if (cleanTrigger.isEmpty()) continue

            val coreTrigger = cleanTrigger.replaceFirst(Regex("^(?i)(?:my|the)\\s+"), "").trim()
            val triggerRegexPart = if (coreTrigger.isNotEmpty() && !coreTrigger.equals(cleanTrigger, ignoreCase = true)) {
                "(?:" + Pattern.quote(cleanTrigger) + "|" + Pattern.quote(coreTrigger) + ")"
            } else {
                Pattern.quote(cleanTrigger)
            }

            val carrierPattern = "(?i)\\b(?:please\\s+)?(?:put|insert|give|share|send|type|write|add|paste|here is|here's)\\s+(?:my|the)?\\s*" + 
                triggerRegexPart + "(?:\\s+(?:here|please|now|link))?\\b"
            workingText = workingText.replace(Regex(carrierPattern), expansion)
        }

        // 3. Substring matching ordered by trigger length descending
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
