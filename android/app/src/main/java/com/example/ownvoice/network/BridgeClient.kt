package com.example.ownvoice.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BridgeClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    suspend fun sendToDesktop(desktopIp: String, text: String, port: Int = 8765): Boolean = withContext(Dispatchers.IO) {
        if (desktopIp.isBlank() || text.isBlank()) return@withContext false

        val host = if (!desktopIp.contains(":")) "$desktopIp:$port" else desktopIp
        val url = "http://$host/inject"

        val json = JSONObject().apply {
            put("text", text)
            put("client", "android")
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            val resp = client.newCall(request).execute()
            resp.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
