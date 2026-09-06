package com.example.ownvoice.network

import com.example.ownvoice.core.SecureConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.TimeUnit

data class DiscoveredDesktop(
    val name: String,
    val ip: String,
    val port: Int,
    val pin: String,
    val token: String
)

data class PairResult(
    val success: Boolean,
    val deviceName: String = "",
    val token: String = "",
    val error: String = ""
)

class BridgeClient(private val config: SecureConfig? = null) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .writeTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    /**
     * Broadcasts a UDP probe on local Wi-Fi port 8766 to discover active OwnVoice desktop instances.
     * Returns a list of discovered desktops within the timeout window.
     */
    suspend fun discoverLocalDesktops(timeoutMs: Int = 1500, discoveryPort: Int = 8766): List<DiscoveredDesktop> = withContext(Dispatchers.IO) {
        val found = mutableListOf<DiscoveredDesktop>()
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = 400

            val probeData = "DISCOVER_OWNVOICE_PC".toByteArray(Charsets.UTF_8)
            val broadcastAddr = InetAddress.getByName("255.255.255.255")
            val packet = DatagramPacket(probeData, probeData.size, broadcastAddr, discoveryPort)
            socket.send(packet)

            val startTime = System.currentTimeMillis()
            val buf = ByteArray(2048)

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    val recvPacket = DatagramPacket(buf, buf.size)
                    socket.receive(recvPacket)
                    val jsonStr = String(recvPacket.data, 0, recvPacket.length, Charsets.UTF_8).trim()
                    val json = JSONObject(jsonStr)
                    if (json.optString("service") == "ownvoice-bridge") {
                        val senderIp = recvPacket.address.hostAddress ?: json.optString("ip")
                        val desktop = DiscoveredDesktop(
                            name = json.optString("device_name", "Windows PC"),
                            ip = if (json.optString("ip").isNotBlank()) json.optString("ip") else senderIp,
                            port = json.optInt("port", 8765),
                            pin = json.optString("pin", ""),
                            token = json.optString("token", "")
                        )
                        if (found.none { it.ip == desktop.ip }) {
                            found.add(desktop)
                        }
                    }
                } catch (e: Exception) {
                    // Socket timeout or parse error, continue until loop timeout
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("BridgeClient", "discoverLocalDesktops error", e)
        } finally {
            socket?.close()
        }
        found
    }

    /**
     * Attempts to pair with a desktop instance using a 6-digit PIN.
     */
    suspend fun pairWithPin(desktopIp: String, pin: String, port: Int = 8765): PairResult = withContext(Dispatchers.IO) {
        if (desktopIp.isBlank() || pin.isBlank()) {
            return@withContext PairResult(false, error = "IP and PIN cannot be empty")
        }
        val host = if (!desktopIp.contains(":")) "$desktopIp:$port" else desktopIp
        val url = "http://$host/pair"

        val json = JSONObject().apply {
            put("pin", pin.trim())
            put("device", "Android Phone")
        }
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        try {
            val resp = client.newCall(request).execute()
            val respStr = resp.body?.string() ?: "{}"
            val respJson = JSONObject(respStr)
            if (resp.isSuccessful && respJson.optBoolean("success")) {
                val token = respJson.optString("token", "")
                val name = respJson.optString("device_name", "Windows PC")
                PairResult(true, deviceName = name, token = token)
            } else {
                PairResult(false, error = respJson.optString("error", "Pairing failed (HTTP ${resp.code})"))
            }
        } catch (e: Exception) {
            PairResult(false, error = e.localizedMessage ?: "Connection error")
        }
    }

    /**
     * Checks if the desktop bridge is alive and reachable.
     */
    suspend fun checkStatus(desktopIp: String, port: Int = 8765): Boolean = withContext(Dispatchers.IO) {
        if (desktopIp.isBlank()) return@withContext false
        val host = if (!desktopIp.contains(":")) "$desktopIp:$port" else desktopIp
        val url = "http://$host/status"
        val request = Request.Builder().url(url).get().build()
        try {
            val resp = client.newCall(request).execute()
            resp.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Injects text directly into the PC cursor.
     * Includes dynamic IP self-healing: if the configured IP fails, it attempts a quick
     * UDP auto-discovery probe to re-locate the PC and retries automatically.
     */
    suspend fun sendToDesktop(desktopIp: String, text: String, port: Int = 8765, token: String = ""): Boolean = withContext(Dispatchers.IO) {
        if (desktopIp.isBlank() || text.isBlank()) return@withContext false

        var effectiveIp = desktopIp
        var effectiveToken = token.ifBlank { config?.desktopBridgeToken ?: "" }

        // Attempt 1: Direct injection
        val success = doInject(effectiveIp, text, port, effectiveToken)
        if (success) return@withContext true

        // Attempt 2: Dynamic IP Self-Healing
        val discovered = discoverLocalDesktops(timeoutMs = 800)
        val matched = discovered.find { 
            (config?.desktopBridgeName?.isNotBlank() == true && it.name == config.desktopBridgeName) ||
            (effectiveToken.isNotBlank() && it.token == effectiveToken) ||
            discovered.size == 1
        }

        if (matched != null && matched.ip != effectiveIp) {
            effectiveIp = matched.ip
            config?.desktopBridgeIp = matched.ip
            if (matched.token.isNotBlank()) {
                effectiveToken = matched.token
                config?.desktopBridgeToken = matched.token
            }
            return@withContext doInject(effectiveIp, text, port, effectiveToken)
        }

        false
    }

    private fun doInject(desktopIp: String, text: String, port: Int, token: String): Boolean {
        val host = if (!desktopIp.contains(":")) "$desktopIp:$port" else desktopIp
        val url = "http://$host/inject"

        val json = JSONObject().apply {
            put("text", text)
            put("client", "android")
            if (token.isNotBlank()) put("token", token)
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            val resp = client.newCall(request).execute()
            resp.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
