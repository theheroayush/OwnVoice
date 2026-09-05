package com.example.ownvoice.network

import android.util.Base64
import com.example.ownvoice.core.SecureConfig
import com.example.ownvoice.core.TonePromptManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GeminiLiveWebSocketClient(
    private val config: SecureConfig,
    private val onTokenReceived: (String) -> Unit,
    private val onTranscriptionComplete: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep-alive WebSocket
        .build()

    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)
    private val accumulatedText = StringBuilder()

    fun connect(mode: String = config.dictationMode) {
        val apiKey = config.apiKey.trim()
        if (apiKey.isBlank()) {
            onError("API key is missing")
            return
        }

        accumulatedText.clear()
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected.set(true)
                sendInitialSetup(webSocket, mode)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseServerMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected.set(false)
                onError(t.localizedMessage ?: "WebSocket connection failure")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected.set(false)
                val fullCleaned = TonePromptManager.cleanTranscript(accumulatedText.toString())
                onTranscriptionComplete(fullCleaned)
            }
        })
    }

    private fun sendInitialSetup(ws: WebSocket, mode: String) {
        val prompt = TonePromptManager.getPrompt(mode, config.customInstructions)
        val setupPayload = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", "models/gemini-2.0-flash-exp")
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply { put("TEXT") })
                    put("temperature", 0.1)
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
        }
        ws.send(setupPayload.toString())
    }

    fun sendAudioChunk(pcmChunk: ByteArray) {
        if (!isConnected.get() || webSocket == null) return

        val b64 = Base64.encodeToString(pcmChunk, Base64.NO_WRAP)
        val realtimeInput = JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("mediaChunks", JSONArray().apply {
                    put(JSONObject().apply {
                        put("mimeType", "audio/pcm;rate=16000")
                        put("data", b64)
                    })
                })
            })
        }
        webSocket?.send(realtimeInput.toString())
    }

    private fun parseServerMessage(rawJson: String) {
        try {
            val root = JSONObject(rawJson)
            val serverContent = root.optJSONObject("serverContent") ?: return
            val modelTurn = serverContent.optJSONObject("modelTurn")
            val parts = modelTurn?.optJSONArray("parts")
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    val text = part.optString("text", "")
                    if (text.isNotEmpty()) {
                        accumulatedText.append(text)
                        onTokenReceived(text)
                    }
                }
            }

            if (serverContent.optBoolean("turnComplete", false)) {
                val full = TonePromptManager.cleanTranscript(accumulatedText.toString())
                onTranscriptionComplete(full)
            }
        } catch (e: Exception) {
            // Ignore parse errors on telemetry frames
        }
    }

    fun close() {
        isConnected.set(false)
        webSocket?.close(1000, "Client closed")
        webSocket = null
    }
}
