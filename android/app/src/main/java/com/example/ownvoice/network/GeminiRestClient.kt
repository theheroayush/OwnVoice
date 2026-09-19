package com.example.ownvoice.network

import android.util.Base64
import com.example.ownvoice.core.SecureConfig
import com.example.ownvoice.core.TonePromptManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiRestClient(private val config: SecureConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Verified production models in Google AI Studio
    private val candidateModels = listOf(
        "gemini-3.6-flash",
        "gemini-3.5-flash-lite",
        "gemini-3.5-flash",
        "gemini-flash-lite-latest",
        "gemini-flash-latest"
    )

    private var activeWorkingModel: String = "gemini-3.6-flash"

    suspend fun transcribeAudio(
        wavBytes: ByteArray,
        mode: String = config.dictationMode,
        personalLexicon: List<String> = emptyList()
    ): Pair<String, Long> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val apiKey = config.apiKey.trim()
        if (apiKey.isBlank()) {
            throw IllegalStateException("API Key is missing. Please enter your key in OwnVoice settings.")
        }

        if (wavBytes.isEmpty() || wavBytes.size < 400) {
            return@withContext Pair("", 0L)
        }

        val base64Audio = Base64.encodeToString(wavBytes, Base64.NO_WRAP)
        val prompt = TonePromptManager.getPrompt(
            mode = mode,
            language = config.language,
            customInstructions = config.customInstructions,
            vocabulary = config.getVocabulary(),
            snippets = config.getSnippets(),
            enableSelfCorrection = config.isSelfCorrectionEnabled,
            enableVoiceCommands = config.isVoiceCommandsEnabled,
            personalLexicon = personalLexicon
        )

        val jsonPayload = JSONObject().apply {
            val partsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "audio/wav")
                        put("data", base64Audio)
                    })
                })
                put(JSONObject().apply {
                    put("text", prompt)
                })
            }

            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", partsArray)
                })
            })

            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("maxOutputTokens", 2048)
            })
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        // Try cached working model first, then fallback to candidate list
        val modelsToTry = listOf(activeWorkingModel) + candidateModels.filter { it != activeWorkingModel }
        var lastError: String? = null

        for (model in modelsToTry) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    val respBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val rootJson = JSONObject(respBody)
                        val candidates = rootJson.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text", "")
                                val cleaned = TonePromptManager.cleanTranscript(text)
                                activeWorkingModel = model
                                return@withContext Pair(cleaned, latency)
                            }
                        }
                        return@withContext Pair("", latency)
                    } else if (response.code == 404) {
                        // Model not available, try next
                        continue
                    } else if (response.code in listOf(401, 403)) {
                        throw IllegalArgumentException("Invalid API Key (HTTP ${response.code}). Check your key in Settings.")
                    } else if (response.code == 429) {
                        lastError = "Gemini Quota Exceeded (HTTP 429). Please wait a moment."
                        continue
                    } else {
                        lastError = "API Error (HTTP ${response.code}): ${respBody.take(120)}"
                    }
                }
            } catch (e: IllegalArgumentException) {
                throw e
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "Connection failure"
            }
        }

        throw RuntimeException(lastError ?: "Failed to transcribe audio. Check network connection.")
    }

    suspend fun reshapeText(inputText: String, instruction: String): Pair<String, Long> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val apiKey = config.apiKey.trim()
        if (apiKey.isBlank()) {
            throw IllegalStateException("API Key is missing. Please enter your key in OwnVoice settings.")
        }
        if (inputText.isBlank()) {
            return@withContext Pair("", 0L)
        }

        val systemPrompt = "You are an expert text editor. Apply this instruction directly to the user's text:\n" +
            "Instruction: $instruction\n" +
            "Rules:\n" +
            "- Output strictly ONLY the reshaped text.\n" +
            "- Do not add explanations, quotes, conversational preambles, or markdown commentary.\n" +
            "- Preserve names, numbers, dates, and core meaning unless asked to summarize."

        val jsonPayload = JSONObject().apply {
            val partsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("text", "$systemPrompt\n\nText to reshape:\n$inputText")
                })
            }

            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", partsArray)
                })
            })

            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("maxOutputTokens", 2048)
            })
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val modelsToTry = listOf(activeWorkingModel) + candidateModels.filter { it != activeWorkingModel }
        var lastError: String? = null

        for (model in modelsToTry) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    val respBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val rootJson = JSONObject(respBody)
                        val candidates = rootJson.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text", "")
                                val cleaned = TonePromptManager.cleanTranscript(text)
                                activeWorkingModel = model
                                return@withContext Pair(cleaned, latency)
                            }
                        }
                        return@withContext Pair("", latency)
                    } else if (response.code == 404) {
                        continue
                    } else if (response.code in listOf(401, 403)) {
                        throw IllegalArgumentException("Invalid API Key (HTTP ${response.code}). Check your key in Settings.")
                    } else if (response.code == 429) {
                        lastError = "Gemini Quota Exceeded (HTTP 429). Please wait a moment."
                        continue
                    } else {
                        lastError = "API Error (HTTP ${response.code}): ${respBody.take(120)}"
                    }
                }
            } catch (e: IllegalArgumentException) {
                throw e
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "Connection failure"
            }
        }

        throw RuntimeException(lastError ?: "Failed to reshape text. Check network connection.")
    }
}
