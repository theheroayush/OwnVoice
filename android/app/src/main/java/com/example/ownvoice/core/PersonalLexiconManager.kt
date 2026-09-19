package com.example.ownvoice.core

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * OwnVoice Compounding Intelligence Engine.
 * Automatically tracks user corrections, specialized terminology, technical acronyms,
 * and frequent personal vocabulary, synthesizing them dynamically into Gemini prompts.
 */
class PersonalLexiconManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val wordFrequencyMap = ConcurrentHashMap<String, Int>()

    companion object {
        private const val PREFS_NAME = "ownvoice_personal_lexicon"
        private const val KEY_LEXICON_JSON = "lexicon_json"

        private val STOP_WORDS = setOf(
            "the", "and", "that", "have", "for", "not", "with", "you", "this", "but", "his", "from",
            "they", "say", "her", "she", "will", "one", "all", "would", "there", "their", "what",
            "out", "about", "who", "get", "which", "when", "make", "can", "like", "time", "just",
            "him", "know", "take", "people", "into", "year", "your", "good", "some", "could", "them",
            "see", "other", "than", "then", "now", "look", "only", "come", "its", "over", "think",
            "also", "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
            "even", "new", "want", "because", "any", "these", "give", "day", "most", "us"
        )
    }

    init {
        loadLexicon()
    }

    @Synchronized
    private fun loadLexicon() {
        wordFrequencyMap.clear()
        val jsonStr = prefs.getString(KEY_LEXICON_JSON, null) ?: return
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val count = json.optInt(key, 1)
                wordFrequencyMap[key] = count
            }
        } catch (e: Exception) {
            // ignore malformed stored json
        }
    }

    @Synchronized
    private fun saveLexicon() {
        val json = JSONObject()
        for ((word, count) in wordFrequencyMap) {
            json.put(word, count)
        }
        prefs.edit().putString(KEY_LEXICON_JSON, json.toString()).apply()
    }

    /**
     * Records an explicit correction when a user replaces an AI mistake.
     * Corrections are weighted heavily (+3).
     */
    fun recordCorrection(oldWord: String, newWord: String) {
        val cleanNew = cleanWord(newWord)
        if (cleanNew.isNotBlank()) {
            wordFrequencyMap[cleanNew] = (wordFrequencyMap[cleanNew] ?: 0) + 3
            saveLexicon()
        }
    }

    /**
     * Records a single word usage with optional count weight.
     */
    fun recordWordUsage(word: String, countDelta: Int = 1) {
        val cleaned = cleanWord(word)
        if (isEligibleWord(cleaned)) {
            wordFrequencyMap[cleaned] = (wordFrequencyMap[cleaned] ?: 0) + countDelta
            saveLexicon()
        }
    }

    /**
     * Scans a full text passage and extracts technical terms, acronyms, and proper nouns.
     */
    fun recordTextUsage(text: String) {
        val tokens = text.split(Regex("[\\s,;:.!?\"'()\\[\\]{}]+"))
        var modified = false
        for (token in tokens) {
            val cleaned = cleanWord(token)
            if (isEligibleWord(cleaned)) {
                wordFrequencyMap[cleaned] = (wordFrequencyMap[cleaned] ?: 0) + 1
                modified = true
            }
        }
        if (modified) {
            saveLexicon()
        }
    }

    fun addCustomWord(word: String) {
        val cleaned = cleanWord(word)
        if (cleaned.isNotBlank()) {
            wordFrequencyMap[cleaned] = (wordFrequencyMap[cleaned] ?: 0) + 5
            saveLexicon()
        }
    }

    fun removeWord(word: String) {
        val cleaned = cleanWord(word)
        if (wordFrequencyMap.remove(cleaned) != null) {
            saveLexicon()
        }
    }

    fun clearLexicon() {
        wordFrequencyMap.clear()
        prefs.edit().remove(KEY_LEXICON_JSON).apply()
    }

    fun getAllWords(): Map<String, Int> = wordFrequencyMap.toMap()

    fun getTopVocabulary(limit: Int = 30): List<String> {
        return wordFrequencyMap.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }

    /**
     * Synthesizes learned custom vocabulary for direct injection into Gemini's system instructions.
     */
    fun getCustomLexiconPrompt(): String {
        val topWords = getTopVocabulary(25)
        if (topWords.isEmpty()) return ""
        return "Specialized personal vocabulary and preferred proper nouns: [${topWords.joinToString(", ")}]. Ensure exact spelling when these terms appear."
    }

    private fun cleanWord(raw: String): String {
        return raw.trim().trim('.', ',', '!', '?', ':', ';', '"', '\'', '(', ')', '[', ']')
    }

    private fun isEligibleWord(word: String): Boolean {
        if (word.length < 3 || word.length > 35) return false
        val lower = word.lowercase()
        if (STOP_WORDS.contains(lower)) return false
        // Eligible if contains uppercase letters (acronym/proper noun) or numbers or specialized symbols
        return word.any { it.isUpperCase() } || word.any { it.isDigit() } || word.length >= 6
    }
}
