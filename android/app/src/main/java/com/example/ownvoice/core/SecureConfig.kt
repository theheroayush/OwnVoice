package com.example.ownvoice.core

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

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
    }

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

    var gistToken: String
        get() = prefs.getString(KEY_GIST_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GIST_TOKEN, value.trim()).apply()

    var gistId: String
        get() = prefs.getString(KEY_GIST_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GIST_ID, value.trim()).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "")?.trim() ?: ""
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
            val jsonArray = org.json.JSONArray(jsonStr)
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
        val jsonArray = org.json.JSONArray()
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
}
