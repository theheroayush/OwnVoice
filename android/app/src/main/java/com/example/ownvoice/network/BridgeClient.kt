package com.example.ownvoice.network

import android.content.Context
import android.net.wifi.WifiManager
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
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

data class DiscoveredDesktop(
    val name: String,
    val ip: String,
    val port: Int,
    val pin: String,
    val token: String,
    val apiKey: String = ""
)

data class PairResult(
    val success: Boolean,
    val deviceName: String = "",
    val token: String = "",
    val apiKey: String = "",
    val error: String = ""
)

class BridgeClient(
    private val config: SecureConfig? = null,
    private val context: Context? = null
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun getBroadcastAddresses(): List<InetAddress> {
        val broadcastList = LinkedHashSet<InetAddress>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces != null && interfaces.hasMoreElements()) {
                val ni = interfaces.nextElement()
                if (ni.isLoopback || !ni.isUp) continue
                for (ia in ni.interfaceAddresses) {
                    ia.broadcast?.let { broadcastList.add(it) }
                }
            }
        } catch (e: Exception) {
            // ignore network interface scan exceptions
        }
        try {
            broadcastList.add(InetAddress.getByName("255.255.255.255"))
        } catch (e: Exception) {
            // ignore
        }
        return broadcastList.toList()
    }

    /**
     * Broadcasts a UDP probe on local Wi-Fi port 8766 to discover active OwnVoice desktop instances.
     * Broadcasts to both subnet-directed broadcast addresses and 255.255.255.255.
     * Acquires WifiManager.MulticastLock when context is available.
     */
    suspend fun discoverLocalDesktops(timeoutMs: Int = 1800, discoveryPort: Int = 8766): List<DiscoveredDesktop> = withContext(Dispatchers.IO) {
        val found = mutableListOf<DiscoveredDesktop>()
        var socket: DatagramSocket? = null
        var multicastLock: WifiManager.MulticastLock? = null

        try {
            context?.let { ctx ->
                try {
                    val wifiManager = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    multicastLock = wifiManager?.createMulticastLock("OwnVoiceBridgeDiscovery")
                    multicastLock?.setReferenceCounted(true)
                    multicastLock?.acquire()
                } catch (e: Exception) {
                    android.util.Log.w("BridgeClient", "Could not acquire MulticastLock: ${e.message}")
                }
            }

            socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = 300

            val probeData = "DISCOVER_OWNVOICE_PC".toByteArray(Charsets.UTF_8)
            val targets = getBroadcastAddresses()

            fun sendProbes() {
                for (addr in targets) {
                    try {
                        val packet = DatagramPacket(probeData, probeData.size, addr, discoveryPort)
                        socket.send(packet)
                    } catch (e: Exception) {
                        // ignore individual interface error
                    }
                }
            }

            sendProbes()

            val startTime = System.currentTimeMillis()
            var lastProbeTime = startTime
            val buf = ByteArray(2048)

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                val now = System.currentTimeMillis()
                if (found.isEmpty() && now - lastProbeTime > 400) {
                    sendProbes()
                    lastProbeTime = now
                }

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
                            token = json.optString("token", ""),
                            apiKey = json.optString("api_key", "")
                        )
                        if (desktop.apiKey.isNotBlank() && (config?.isDefaultOrBlankApiKey == true)) {
                            config?.apiKey = desktop.apiKey
                        }
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
            try {
                if (multicastLock?.isHeld == true) {
                    multicastLock?.release()
                }
            } catch (e: Exception) {}
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
                val apiKey = respJson.optString("api_key", "")
                if (apiKey.isNotBlank() && (config?.isDefaultOrBlankApiKey == true)) {
                    config?.apiKey = apiKey
                }
                PairResult(true, deviceName = name, token = token, apiKey = apiKey)
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
            if (resp.isSuccessful) {
                val respStr = resp.body?.string() ?: "{}"
                val json = JSONObject(respStr)
                val returnedKey = json.optString("api_key", "")
                if (returnedKey.isNotBlank() && (config?.isDefaultOrBlankApiKey == true)) {
                    config?.apiKey = returnedKey
                }
                val returnedToken = json.optString("token", "")
                if (returnedToken.isNotBlank() && config?.desktopBridgeToken.isNullOrBlank()) {
                    config?.desktopBridgeToken = returnedToken
                }
                true
            } else {
                false
            }
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
        if (text.isBlank()) return@withContext false

        var effectiveIp = desktopIp.ifBlank { config?.desktopBridgeIp ?: "" }
        var effectiveToken = token.ifBlank { config?.desktopBridgeToken ?: "" }

        // Attempt 1: Direct injection if IP is known
        if (effectiveIp.isNotBlank()) {
            val success = doInject(effectiveIp, text, port, effectiveToken)
            if (success) return@withContext true
        }

        // Attempt 2: Dynamic IP Self-Healing & Instant Auto-Discovery
        val discovered = discoverLocalDesktops(timeoutMs = 1500)
        val matched = discovered.find { 
            (config?.desktopBridgeName?.isNotBlank() == true && it.name == config.desktopBridgeName) ||
            (effectiveToken.isNotBlank() && it.token == effectiveToken) ||
            discovered.size == 1
        } ?: discovered.firstOrNull()

        if (matched != null) {
            effectiveIp = matched.ip
            config?.desktopBridgeIp = matched.ip
            config?.desktopBridgeName = matched.name
            if (matched.token.isNotBlank()) {
                effectiveToken = matched.token
                config?.desktopBridgeToken = matched.token
            }
            if (matched.pin.isNotBlank()) {
                config?.desktopBridgePin = matched.pin
            }
            if (matched.apiKey.isNotBlank() && config?.isDefaultOrBlankApiKey == true) {
                config?.apiKey = matched.apiKey
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
            val pin = config?.desktopBridgePin ?: ""
            if (pin.isNotBlank()) put("pin", pin)
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
