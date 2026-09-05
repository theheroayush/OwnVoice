package com.example.ownvoice.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ApiKeyValidator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun validateKey(apiKey: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Pair(false, "API key cannot be empty")
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$cleanKey"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Pair(true, "API Key is valid and connected!")
                } else if (response.code in listOf(400, 401, 403)) {
                    Pair(false, "Invalid API key (HTTP ${response.code}). Please check your key.")
                } else if (response.code == 429) {
                    Pair(false, "Quota exceeded (HTTP 429). Please wait a moment.")
                } else {
                    Pair(false, "Validation returned HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Network error: ${e.localizedMessage ?: "Unable to reach Google servers"}")
        }
    }
}
