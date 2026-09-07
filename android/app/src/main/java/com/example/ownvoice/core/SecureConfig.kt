package com.example.ownvoice.core

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class HistoryItem(
    val id: String,
    val text: String,
    val timestamp: Long,
    val target: String = "PC",
    val tags: List<String> = emptyList()
)

class SecureConfig(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "ownvoice_secure_prefs"
        private const val KEY_API_KEY = "google_api_key"
        private const val KEY_DICTATION_MODE = "dictation_mode"
        private const val KEY_CUSTOM_INSTRUCTIONS = "custom_instructions"
        private const val KEY_FLOATING_BUBBLE = "floating_bubble_enabled"
        private const val KEY_SNIPPETS_JSON = "snippets_json"
        private const val KEY_SOUND_EFFECTS = "sound_effects"
        private const val KEY_BUBBLE_X = "bubble_x"
        private const val KEY_BUBBLE_Y = "bubble_y"
        private const val KEY_AUTO_CONTEXT_TONE = "auto_context_tone"
        private const val KEY_SELF_CORRECTION = "self_correction_enabled"
        private const val KEY_VOCABULARY_JSON = "vocabulary_json"
        private const val KEY_DESKTOP_BRIDGE_IP = "desktop_bridge_ip"
        private const val KEY_DESKTOP_BRIDGE_NAME = "desktop_bridge_name"
        private const val KEY_DESKTOP_BRIDGE_TOKEN = "desktop_bridge_token"
        private const val KEY_DESKTOP_BRIDGE_PIN = "desktop_bridge_pin"
        private const val KEY_GIST_TOKEN = "gist_token"
        private const val KEY_GIST_ID = "gist_id"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback_enabled"
        private const val KEY_USE_FOR_PC = "use_for_pc_enabled"
        const val DEFAULT_API_KEY = "AQ.Ab8RN6JVp6KVzuryq8BuUZLBYBuUJCNqwECacgdSMUegi5EblA"
        private const val KEY_HISTORY_JSON = "history_json"

        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_VOICE_MODEL_TIER = "voice_model_tier"
        private const val KEY_SPEAKING_STYLE = "speaking_style"
        private const val KEY_INPUT_SENSITIVITY = "input_sensitivity"
        private const val KEY_SILENCE_DETECTION = "silence_detection_seconds"
        private const val KEY_CONTINUOUS_LISTENING = "continuous_listening"
        private const val KEY_VOICE_COMMANDS = "voice_commands_enabled"
    }

    val isDefaultOrBlankApiKey: Boolean
        get() = apiKey.isBlank() || apiKey == DEFAULT_API_KEY

    val sharedPreferences: SharedPreferences get() = prefs

    var desktopBridgeIp: String
        get() = prefs.getString(KEY_DESKTOP_BRIDGE_IP, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DESKTOP_BRIDGE_IP, value.trim()).apply()

    var desktopBridgeName: String
        get() = prefs.getString(KEY_DESKTOP_BRIDGE_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DESKTOP_BRIDGE_NAME, value.trim()).apply()

    var desktopBridgeToken: String
        get() = prefs.getString(KEY_DESKTOP_BRIDGE_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DESKTOP_BRIDGE_TOKEN, value.trim()).apply()

    var desktopBridgePin: String
        get() = prefs.getString(KEY_DESKTOP_BRIDGE_PIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DESKTOP_BRIDGE_PIN, value.trim()).apply()

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean("has_completed_onboarding", false)
        set(value) = prefs.edit().putBoolean("has_completed_onboarding", value).apply()

    var isUseForPcEnabled: Boolean
        get() = prefs.getBoolean(KEY_USE_FOR_PC, desktopBridgeIp.isNotBlank())
        set(value) = prefs.edit().putBoolean(KEY_USE_FOR_PC, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "English (US)") ?: "English (US)"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var voiceModelTier: String
        get() = prefs.getString(KEY_VOICE_MODEL_TIER, "balanced") ?: "balanced"
        set(value) = prefs.edit().putString(KEY_VOICE_MODEL_TIER, value).apply()

    var speakingStyle: String
        get() = prefs.getString(KEY_SPEAKING_STYLE, "natural") ?: "natural"
        set(value) = prefs.edit().putString(KEY_SPEAKING_STYLE, value).apply()

    var inputSensitivity: Float
        get() = prefs.getFloat(KEY_INPUT_SENSITIVITY, 0.5f)
        set(value) = prefs.edit().putFloat(KEY_INPUT_SENSITIVITY, value).apply()

    var silenceDetectionSeconds: Float
        get() = prefs.getFloat(KEY_SILENCE_DETECTION, 1.5f)
        set(value) = prefs.edit().putFloat(KEY_SILENCE_DETECTION, value).apply()

    var isContinuousListening: Boolean
        get() = prefs.getBoolean(KEY_CONTINUOUS_LISTENING, false)
        set(value) = prefs.edit().putBoolean(KEY_CONTINUOUS_LISTENING, value).apply()

    var isVoiceCommandsEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_COMMANDS, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_COMMANDS, value).apply()

    var gistToken: String
        get() = prefs.getString(KEY_GIST_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GIST_TOKEN, value.trim()).apply()

    var gistId: String
        get() = prefs.getString(KEY_GIST_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GIST_ID, value.trim()).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "")?.trim()?.ifBlank { DEFAULT_API_KEY } ?: DEFAULT_API_KEY
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var dictationMode: String
        get() = prefs.getString(KEY_DICTATION_MODE, "smart_flow") ?: "smart_flow"
        set(value) = prefs.edit().putString(KEY_DICTATION_MODE, value).apply()

    var customInstructions: String
        get() = prefs.getString(KEY_CUSTOM_INSTRUCTIONS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_INSTRUCTIONS, value).apply()

    var isFloatingBubbleEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_BUBBLE, false)
        set(value) = prefs.edit().putBoolean(KEY_FLOATING_BUBBLE, value).apply()

    var isSoundEffectsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_EFFECTS, false)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_EFFECTS, value).apply()

    var isHapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()

    var isAutoContextToneEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONTEXT_TONE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONTEXT_TONE, value).apply()

    var isSelfCorrectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_SELF_CORRECTION, true)
        set(value) = prefs.edit().putBoolean(KEY_SELF_CORRECTION, value).apply()

    var bubbleX: Int
        get() = prefs.getInt(KEY_BUBBLE_X, 100)
        set(value) = prefs.edit().putInt(KEY_BUBBLE_X, value).apply()

    var bubbleY: Int
        get() = prefs.getInt(KEY_BUBBLE_Y, 300)
        set(value) = prefs.edit().putInt(KEY_BUBBLE_Y, value).apply()

    fun getSnippets(): Map<String, String> {
        val jsonStr = prefs.getString(KEY_SNIPPETS_JSON, null) ?: return defaultSnippets()
        val result = mutableMapOf<String, String>()
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                result[k] = json.getString(k)
            }
        } catch (e: Exception) {
            return defaultSnippets()
        }
        return result
    }

    fun saveSnippets(snippets: Map<String, String>) {
        val json = JSONObject()
        for ((k, v) in snippets) {
            json.put(k, v)
        }
        prefs.edit().putString(KEY_SNIPPETS_JSON, json.toString()).apply()
    }

    private fun defaultSnippets(): Map<String, String> {
        return mapOf(
            "my email" to "ayush@example.com",
            "meeting link" to "https://cal.com/ayush",
            "sign off" to "Best regards,\nAyush"
        )
    }

    fun getVocabulary(): List<String> {
        val jsonStr = prefs.getString(KEY_VOCABULARY_JSON, null) ?: return defaultVocabulary()
        val result = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optString(i)
                if (item.isNotBlank()) result.add(item)
            }
        } catch (e: Exception) {
            return defaultVocabulary()
        }
        return if (result.isEmpty()) defaultVocabulary() else result
    }

    fun saveVocabulary(vocab: List<String>) {
        val jsonArray = JSONArray()
        for (term in vocab.distinct()) {
            if (term.isNotBlank()) jsonArray.put(term.trim())
        }
        prefs.edit().putString(KEY_VOCABULARY_JSON, jsonArray.toString()).apply()
    }

    fun addVocabularyTerm(term: String) {
        val current = getVocabulary().toMutableList()
        val cleaned = term.trim()
        if (cleaned.isNotBlank() && !current.contains(cleaned)) {
            current.add(cleaned)
            saveVocabulary(current)
        }
    }

    fun removeVocabularyTerm(term: String) {
        val current = getVocabulary().toMutableList()
        current.remove(term.trim())
        saveVocabulary(current)
    }

    private fun defaultVocabulary(): List<String> {
        return listOf("Ayush", "UPI", "WhatsApp", "Razorpay", "GitHub", "LinkedIn", "K8s", "Gemini", "OwnVoice")
    }

    fun getHistory(): List<HistoryItem> {
        val jsonStr = prefs.getString(KEY_HISTORY_JSON, null) ?: return defaultHistory()
        val list = mutableListOf<HistoryItem>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val tagList = mutableListOf<String>()
                val tagsArr = obj.optJSONArray("tags")
                if (tagsArr != null) {
                    for (t in 0 until tagsArr.length()) {
                        tagList.add(tagsArr.getString(t))
                    }
                }
                list.add(
                    HistoryItem(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        text = obj.optString("text", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        target = obj.optString("target", "PC"),
                        tags = tagList
                    )
                )
            }
        } catch (e: Exception) {
            return defaultHistory()
        }
        return if (list.isEmpty()) defaultHistory() else list
    }

    fun addHistoryItem(text: String, target: String = "PC", tags: List<String> = emptyList()): HistoryItem {
        val current = getHistory().toMutableList()
        val item = HistoryItem(
            id = java.util.UUID.randomUUID().toString(),
            text = text.trim(),
            timestamp = System.currentTimeMillis(),
            target = target,
            tags = tags
        )
        current.add(0, item)
        val trimmed = if (current.size > 50) current.take(50) else current
        saveHistory(trimmed)
        return item
    }

    fun deleteHistoryItem(id: String) {
        val current = getHistory().filterNot { it.id == id }
        saveHistory(current)
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY_JSON).apply()
    }

    private fun saveHistory(items: List<HistoryItem>) {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("timestamp", item.timestamp)
                put("target", item.target)
                put("tags", JSONArray(item.tags))
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY_JSON, arr.toString()).apply()
    }

    private fun defaultHistory(): List<HistoryItem> {
        val now = System.currentTimeMillis()
        val hour = 60 * 60 * 1000L
        val day = 24 * hour
        return listOf(
            HistoryItem(
                id = "h1",
                text = "Send the proposal to Rahul tomorrow morning before 10 AM.",
                timestamp = now - 28 * 60 * 1000L, // 9:12 AM today
                target = "Sent to MacBook Pro",
                tags = listOf("Work")
            ),
            HistoryItem(
                id = "h2",
                text = "What are the key points from today's client call?",
                timestamp = now - 55 * 60 * 1000L, // 8:45 AM today
                target = "Copied to clipboard",
                tags = listOf("Work", "Meeting")
            ),
            HistoryItem(
                id = "h3",
                text = "Remind me to book train tickets to Varanasi next week.",
                timestamp = now - 130 * 60 * 1000L, // 7:28 AM today
                target = "Sent to MacBook Pro",
                tags = listOf("Personal")
            ),
            HistoryItem(
                id = "h4",
                text = "Create a content plan for next week with 5 LinkedIn post ideas.",
                timestamp = now - 1 * day - 3 * hour,
                target = "Copied to clipboard",
                tags = listOf("Work", "Content")
            ),
            HistoryItem(
                id = "h5",
                text = "List top 5 business ideas in the AI space for India.",
                timestamp = now - 1 * day - 8 * hour,
                target = "Sent to MacBook Pro",
                tags = listOf("Research", "Ideas")
            ),
            HistoryItem(
                id = "h6",
                text = "Summarize this article for me in simple points.",
                timestamp = now - 4 * day,
                target = "Copied to clipboard",
                tags = listOf("Reading")
            )
        )
    }
}
