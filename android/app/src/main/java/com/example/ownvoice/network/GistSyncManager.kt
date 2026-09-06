package com.example.ownvoice.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GistSyncManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val GIST_FILENAME = "ownvoice_sync.json"
    }

    data class SyncResult(
        val success: Boolean,
        val message: String,
        val gistId: String? = null,
        val payload: JSONObject? = null
    )

    suspend fun syncToGist(
        token: String,
        payload: JSONObject,
        existingGistId: String? = null
    ): SyncResult = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext SyncResult(false, "GitHub Token is required")

        val filesObj = JSONObject().apply {
            put(GIST_FILENAME, JSONObject().apply {
                put("content", payload.toString(2))
            })
        }
        val requestBodyJson = JSONObject().apply {
            put("description", "OwnVoice Cloud Sync (Vocabulary & Snippets)")
            put("public", false)
            put("files", filesObj)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestBodyJson.toString().toRequestBody(mediaType)

        val url = if (!existingGistId.isNullOrBlank()) {
            "https://api.github.com/gists/$existingGistId"
        } else {
            "https://api.github.com/gists"
        }

        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("Authorization", "token ${token.trim()}")
            .addHeader("Accept", "application/vnd.github.v3+json")
            .addHeader("User-Agent", "OwnVoice-Android")

        val request = if (!existingGistId.isNullOrBlank()) {
            reqBuilder.patch(body).build()
        } else {
            reqBuilder.post(body).build()
        }

        try {
            val resp = client.newCall(request).execute()
            val respStr = resp.body?.string() ?: ""
            if (resp.isSuccessful) {
                val json = JSONObject(respStr)
                val id = json.optString("id")
                SyncResult(true, "Synced to GitHub Gist!", gistId = id)
            } else {
                SyncResult(false, "GitHub error (${resp.code})")
            }
        } catch (e: Exception) {
            SyncResult(false, "Sync error: ${e.localizedMessage}")
        }
    }

    suspend fun syncFromGist(token: String, gistId: String): SyncResult = withContext(Dispatchers.IO) {
        if (gistId.isBlank()) return@withContext SyncResult(false, "Gist ID is required")

        val reqBuilder = Request.Builder()
            .url("https://api.github.com/gists/${gistId.trim()}")
            .addHeader("Accept", "application/vnd.github.v3+json")
            .addHeader("User-Agent", "OwnVoice-Android")

        if (token.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "token ${token.trim()}")
        }

        try {
            val resp = client.newCall(reqBuilder.build()).execute()
            val respStr = resp.body?.string() ?: ""
            if (resp.isSuccessful) {
                val json = JSONObject(respStr)
                val files = json.optJSONObject("files")
                val syncFile = files?.optJSONObject(GIST_FILENAME)
                val content = syncFile?.optString("content")
                if (!content.isNullOrBlank()) {
                    val parsed = JSONObject(content)
                    SyncResult(true, "Pulled data from GitHub Gist!", gistId = gistId, payload = parsed)
                } else {
                    SyncResult(false, "File '$GIST_FILENAME' not found in Gist")
                }
            } else {
                SyncResult(false, "GitHub error (${resp.code})")
            }
        } catch (e: Exception) {
            SyncResult(false, "Network error: ${e.localizedMessage}")
        }
    }
}
