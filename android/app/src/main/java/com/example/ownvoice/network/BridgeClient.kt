package com.example.ownvoice.network

import android.content.Context
import android.net.wifi.WifiManager
import com.example.ownvoice.core.SecureConfig
import com.example.ownvoice.ime.model.KeystrokeEvent
import com.example.ownvoice.ime.model.KeystrokeType
import com.example.ownvoice.ime.model.TrackpadAction
import com.example.ownvoice.ime.model.TrackpadEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.CopyOnWriteArrayList
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

    @Volatile
    var lastError: String = ""

    private val bridgeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val keystrokeChannel = Channel<KeystrokeEvent>(capacity = 128, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val mouseChannel = Channel<TrackpadEvent>(capacity = 128, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val _latestPcClipboardText = MutableStateFlow<String?>(null)
    val latestPcClipboardText: StateFlow<String?> = _latestPcClipboardText.asStateFlow()

    private val _isCloudRelayConnected = MutableStateFlow(false)
    val isCloudRelayConnected: StateFlow<Boolean> = _isCloudRelayConnected.asStateFlow()

    private var relayWebSocket: WebSocket? = null

    init {
        startKeystrokeStreamWorker()
        startTrackpadStreamWorker()
        startCloudRelayConnection()
        startPeriodicClipboardSync()
    }

    fun enqueueKeystroke(event: KeystrokeEvent) {
        keystrokeChannel.trySend(event)
    }

    fun enqueueChar(char: String) {
        keystrokeChannel.trySend(KeystrokeEvent(KeystrokeType.CHAR, char))
    }

    fun enqueueBackspace() {
        keystrokeChannel.trySend(KeystrokeEvent(KeystrokeType.BACKSPACE, "\b"))
    }

    fun enqueueEnter() {
        keystrokeChannel.trySend(KeystrokeEvent(KeystrokeType.ENTER, "\n"))
    }

    fun enqueueSpace() {
        keystrokeChannel.trySend(KeystrokeEvent(KeystrokeType.SPACE, " "))
    }

    fun enqueueTrackpadEvent(event: TrackpadEvent) {
        mouseChannel.trySend(event)
    }

    fun sendMouseMove(dx: Float, dy: Float) {
        val sensitivity = config?.trackpadSensitivity ?: 1.2f
        enqueueTrackpadEvent(TrackpadEvent(TrackpadAction.MOVE, dx = dx * sensitivity, dy = dy * sensitivity))
    }

    fun sendMouseClick(button: String = "left", double: Boolean = false) {
        enqueueTrackpadEvent(TrackpadEvent(TrackpadAction.CLICK, button = button, isDouble = double))
    }

    fun sendMouseDown(button: String = "left") {
        enqueueTrackpadEvent(TrackpadEvent(TrackpadAction.DOWN, button = button))
    }

    fun sendMouseUp(button: String = "left") {
        enqueueTrackpadEvent(TrackpadEvent(TrackpadAction.UP, button = button))
    }

    fun sendMouseScroll(delta: Float) {
        enqueueTrackpadEvent(TrackpadEvent(TrackpadAction.SCROLL, dy = delta))
    }

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

    private fun getLocalIps(): List<String> {
        val list = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces != null && interfaces.hasMoreElements()) {
                val ni = interfaces.nextElement()
                if (ni.isLoopback || !ni.isUp) continue
                for (ia in ni.interfaceAddresses) {
                    val addr = ia.address
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: ""
                        if (host.isNotBlank()) list.add(host)
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }

    /**
     * High-speed concurrent TCP subnet sweep.
     * Essential for hotel Wi-Fi, university networks, and corporate access points that
     * block UDP broadcast and multicast packets between clients.
     */
    suspend fun scanSubnetForPCs(port: Int = 8765): List<DiscoveredDesktop> = withContext(Dispatchers.IO) {
        val localIps = getLocalIps()
        val foundList = CopyOnWriteArrayList<DiscoveredDesktop>()
        val fastClient = OkHttpClient.Builder()
            .connectTimeout(350, TimeUnit.MILLISECONDS)
            .readTimeout(600, TimeUnit.MILLISECONDS)
            .build()

        coroutineScope {
            for (localIp in localIps) {
                val prefix = localIp.substringBeforeLast(".") + "."
                (1..254).map { i ->
                    async {
                        val targetIp = "$prefix$i"
                        try {
                            val req = Request.Builder().url("http://$targetIp:$port/status").get().build()
                            fastClient.newCall(req).execute().use { resp ->
                                if (resp.isSuccessful) {
                                    val body = resp.body?.string() ?: ""
                                    val json = JSONObject(body)
                                    if (json.optString("service") == "ownvoice-bridge" || json.optString("status") == "ready") {
                                        val desktop = DiscoveredDesktop(
                                            name = json.optString("device_name", "Windows PC"),
                                            ip = targetIp,
                                            port = json.optInt("port", port),
                                            pin = json.optString("pin", ""),
                                            token = json.optString("token", ""),
                                            apiKey = json.optString("api_key", "")
                                        )
                                        if (desktop.apiKey.isNotBlank() && (config?.isDefaultOrBlankApiKey == true)) {
                                            config?.apiKey = desktop.apiKey
                                        }
                                        if (foundList.none { it.ip == desktop.ip }) {
                                            foundList.add(desktop)
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            // Non-responsive host on subnet
                        }
                    }
                }
            }
        }
        foundList.toList()
    }

    /**
     * Dual-Engine Discovery:
     * 1. Broadcasts a UDP probe on local Wi-Fi port 8766.
     * 2. If UDP broadcast fails or is blocked by router AP isolation (common in hotels),
     *    automatically executes an ultra-fast concurrent TCP subnet sweep.
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
            socket.soTimeout = 250

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

            // Listen for UDP responses up to min(1000ms, timeoutMs)
            val udpWaitMs = minOf(timeoutMs, 1000)
            while (System.currentTimeMillis() - startTime < udpWaitMs) {
                val now = System.currentTimeMillis()
                if (found.isEmpty() && now - lastProbeTime > 300) {
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
                    // Socket timeout or parse error
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("BridgeClient", "discoverLocalDesktops UDP error", e)
        } finally {
            socket?.close()
            try {
                if (multicastLock?.isHeld == true) {
                    multicastLock?.release()
                }
            } catch (e: Exception) {}
        }

        // Secondary fallback: High-speed TCP Subnet Sweep if UDP broadcast was blocked
        if (found.isEmpty()) {
            val subnetPCs = scanSubnetForPCs(discoveryPort - 1)
            for (pc in subnetPCs) {
                if (found.none { it.ip == pc.ip }) {
                    found.add(pc)
                }
            }
        }

        found
    }

    /**
     * Attempts to pair with a desktop instance using a 6-digit PIN.
     */
    suspend fun pairWithPin(desktopIp: String, pin: String, port: Int = 8765): PairResult = withContext(Dispatchers.IO) {
        if (desktopIp.isBlank() || pin.isBlank()) {
            lastError = "IP and PIN cannot be empty"
            return@withContext PairResult(false, error = lastError)
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
                lastError = ""
                PairResult(true, deviceName = name, token = token, apiKey = apiKey)
            } else {
                lastError = respJson.optString("error", "Pairing failed (HTTP ${resp.code})")
                PairResult(false, error = lastError)
            }
        } catch (e: Exception) {
            lastError = e.localizedMessage ?: "Connection error"
            PairResult(false, error = lastError)
        }
    }

    /**
     * Checks if the desktop bridge is alive and reachable.
     */
    suspend fun checkStatus(desktopIp: String, port: Int = 8765): Boolean = withContext(Dispatchers.IO) {
        if (desktopIp.isBlank()) {
            lastError = "Desktop IP is blank"
            return@withContext false
        }
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
                val clip = json.optString("latest_clipboard", "")
                if (clip.isNotBlank() && clip != _latestPcClipboardText.value) {
                    _latestPcClipboardText.value = clip
                }
                lastError = ""
                true
            } else {
                lastError = "Server responded with HTTP ${resp.code}"
                false
            }
        } catch (e: Exception) {
            lastError = e.localizedMessage ?: "Connection error"
            false
        }
    }

    /**
     * Injects text directly into the PC cursor.
     * Includes dynamic IP self-healing: if the configured IP fails, it attempts a quick
     * UDP auto-discovery probe and TCP subnet sweep to re-locate the PC and retries automatically.
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

        // Attempt 2: Dynamic IP Self-Healing & Instant Dual-Engine Discovery
        val discovered = discoverLocalDesktops(timeoutMs = 1200)
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

    private fun startKeystrokeStreamWorker() {
        bridgeScope.launch {
            while (true) {
                try {
                    val first = keystrokeChannel.receive()
                    val targetIp = config?.desktopBridgeIp ?: ""
                    if (targetIp.isBlank()) continue

                    val port = 8765
                    val token = config?.desktopBridgeToken ?: ""

                    when (first.type) {
                        KeystrokeType.BACKSPACE -> {
                            doInjectPayload(targetIp, text = "\b", action = "backspace", key = "backspace", port = port, token = token)
                        }
                        KeystrokeType.ENTER -> {
                            doInjectPayload(targetIp, text = "\n", action = "enter", key = "enter", port = port, token = token)
                        }
                        KeystrokeType.CLEAR -> {
                            doInjectPayload(targetIp, text = "", action = "clear", key = "clear", port = port, token = token)
                        }
                        KeystrokeType.CHAR, KeystrokeType.SPACE -> {
                            // Coalesce rapid consecutive characters within 16ms window
                            val sb = StringBuilder(first.text)
                            val deadline = System.currentTimeMillis() + 16L
                            while (System.currentTimeMillis() < deadline) {
                                val next = keystrokeChannel.tryReceive().getOrNull() ?: break
                                if (next.type == KeystrokeType.CHAR || next.type == KeystrokeType.SPACE) {
                                    sb.append(next.text)
                                } else {
                                    doInjectPayload(targetIp, text = sb.toString(), action = "type", key = "", port = port, token = token)
                                    sb.setLength(0)
                                    when (next.type) {
                                        KeystrokeType.BACKSPACE -> doInjectPayload(targetIp, text = "\b", action = "backspace", key = "backspace", port = port, token = token)
                                        KeystrokeType.ENTER -> doInjectPayload(targetIp, text = "\n", action = "enter", key = "enter", port = port, token = token)
                                        KeystrokeType.CLEAR -> doInjectPayload(targetIp, text = "", action = "clear", key = "clear", port = port, token = token)
                                        KeystrokeType.CHAR, KeystrokeType.SPACE -> {}
                                    }
                                    break
                                }
                            }
                            if (sb.isNotEmpty()) {
                                doInjectPayload(targetIp, text = sb.toString(), action = "type", key = "", port = port, token = token)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) break
                    delay(40)
                }
            }
        }
    }

    private fun doInject(desktopIp: String, text: String, port: Int, token: String): Boolean {
        return doInjectPayload(desktopIp, text, action = "inject", port = port, token = token)
    }

    private fun doInjectPayload(desktopIp: String, text: String, action: String = "", key: String = "", port: Int = 8765, token: String = ""): Boolean {
        val json = JSONObject().apply {
            put("text", text)
            put("client", "android")
            if (action.isNotBlank()) put("action", action)
            if (key.isNotBlank()) put("key", key)
            if (token.isNotBlank()) put("token", token)
            val pin = config?.desktopBridgePin ?: ""
            if (pin.isNotBlank()) put("pin", pin)
        }
        return sendDualPathPayload(desktopIp, json, port)
    }

    private fun startTrackpadStreamWorker() {
        bridgeScope.launch {
            while (true) {
                try {
                    val first = mouseChannel.receive()
                    val targetIp = config?.desktopBridgeIp ?: ""
                    val token = config?.desktopBridgeToken ?: ""

                    when (first.action) {
                        TrackpadAction.MOVE -> {
                            var totalDx = first.dx
                            var totalDy = first.dy
                            val deadline = System.currentTimeMillis() + 8L
                            while (System.currentTimeMillis() < deadline) {
                                val next = mouseChannel.tryReceive().getOrNull() ?: break
                                if (next.action == TrackpadAction.MOVE) {
                                    totalDx += next.dx
                                    totalDy += next.dy
                                } else {
                                    dispatchMousePayload(targetIp, token, "mouse_move", dx = totalDx.toInt(), dy = totalDy.toInt())
                                    when (next.action) {
                                        TrackpadAction.CLICK -> dispatchMousePayload(targetIp, token, "mouse_click", button = next.button, double = next.isDouble)
                                        TrackpadAction.DOWN -> dispatchMousePayload(targetIp, token, "mouse_down", button = next.button)
                                        TrackpadAction.UP -> dispatchMousePayload(targetIp, token, "mouse_up", button = next.button)
                                        TrackpadAction.SCROLL -> dispatchMousePayload(targetIp, token, "mouse_scroll", delta = next.dy.toInt())
                                        TrackpadAction.MOVE -> {}
                                    }
                                    totalDx = 0f
                                    totalDy = 0f
                                    break
                                }
                            }
                            if (totalDx != 0f || totalDy != 0f) {
                                dispatchMousePayload(targetIp, token, "mouse_move", dx = totalDx.toInt(), dy = totalDy.toInt())
                            }
                        }
                        TrackpadAction.CLICK -> {
                            dispatchMousePayload(targetIp, token, "mouse_click", button = first.button, double = first.isDouble)
                        }
                        TrackpadAction.DOWN -> {
                            dispatchMousePayload(targetIp, token, "mouse_down", button = first.button)
                        }
                        TrackpadAction.UP -> {
                            dispatchMousePayload(targetIp, token, "mouse_up", button = first.button)
                        }
                        TrackpadAction.SCROLL -> {
                            dispatchMousePayload(targetIp, token, "mouse_scroll", delta = first.dy.toInt())
                        }
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) break
                    delay(15)
                }
            }
        }
    }

    private fun dispatchMousePayload(
        desktopIp: String,
        token: String,
        action: String,
        dx: Int = 0,
        dy: Int = 0,
        delta: Int = 0,
        button: String = "left",
        double: Boolean = false
    ): Boolean {
        val json = JSONObject().apply {
            put("action", action)
            put("client", "android_trackpad")
            if (token.isNotBlank()) put("token", token)
            val pin = config?.desktopBridgePin ?: ""
            if (pin.isNotBlank()) put("pin", pin)
            if (action == "mouse_move") {
                put("dx", dx)
                put("dy", dy)
            } else if (action == "mouse_click") {
                put("button", button)
                put("double", double)
            } else if (action == "mouse_down" || action == "mouse_up") {
                put("button", button)
            } else if (action == "mouse_scroll") {
                put("dy", delta)
            }
        }
        return sendDualPathPayload(desktopIp, json)
    }

    private fun sendDualPathPayload(desktopIp: String, json: JSONObject, port: Int = 8765): Boolean {
        // Path 1: Direct LAN socket
        if (desktopIp.isNotBlank()) {
            val host = if (!desktopIp.contains(":")) "$desktopIp:$port" else desktopIp
            val url = "http://$host/inject"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).post(body).build()
            try {
                val resp = client.newCall(request).execute()
                if (resp.isSuccessful) {
                    lastError = ""
                    return true
                }
            } catch (_: Exception) {
                // Fallback to Cloud Relay
            }
        }

        // Path 2: Universal Zero-Config Cloud Relay WebSocket
        val ws = relayWebSocket
        if (ws != null && _isCloudRelayConnected.value) {
            val sent = ws.send(json.toString())
            if (sent) {
                lastError = ""
                return true
            }
        }

        lastError = "Connection unavailable (LAN and Relay unreachable)"
        return false
    }

    suspend fun sendClipboardToPc(text: String): Boolean = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext false
        val token = config?.desktopBridgeToken ?: ""
        val json = JSONObject().apply {
            put("action", "clipboard_set")
            put("text", text)
            if (token.isNotBlank()) put("token", token)
        }
        val targetIp = config?.desktopBridgeIp ?: ""
        if (targetIp.isNotBlank()) {
            val host = if (!targetIp.contains(":")) "$targetIp:8765" else targetIp
            val url = "http://$host/clipboard"
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            try {
                val resp = client.newCall(request).execute()
                if (resp.isSuccessful) return@withContext true
            } catch (_: Exception) {}
        }
        sendDualPathPayload(targetIp, json)
    }

    suspend fun sendMeetingNotesToPc(title: String, markdownContent: String): Boolean = withContext(Dispatchers.IO) {
        if (markdownContent.isBlank()) return@withContext false
        val token = config?.desktopBridgeToken ?: ""
        val json = JSONObject().apply {
            put("action", "meeting_save")
            put("title", title)
            put("content", markdownContent)
            if (token.isNotBlank()) put("token", token)
        }
        val targetIp = config?.desktopBridgeIp ?: ""
        if (targetIp.isNotBlank()) {
            val host = if (!targetIp.contains(":")) "$targetIp:8765" else targetIp
            val url = "http://$host/meeting/save"
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            try {
                val resp = client.newCall(request).execute()
                if (resp.isSuccessful) return@withContext true
            } catch (_: Exception) {}
        }
        sendDualPathPayload(targetIp, json)
    }

    fun startCloudRelayConnection() {
        if (config?.isCloudRelayEnabled != true) return
        val roomToken = config.desktopBridgeToken.ifBlank { config.relayRoomToken }
        if (roomToken.isBlank()) return

        bridgeScope.launch {
            val rawUrl = config.cloudRelayUrl
            val wsUrl = rawUrl.replace("http://", "ws://").replace("https://", "wss://")
            val cleanUrl = if (wsUrl.endsWith("/ws")) wsUrl else wsUrl.trimEnd('/') + "/ws"
            val fullUrl = "$cleanUrl?room=$roomToken&role=phone"

            val wsRequest = Request.Builder().url(fullUrl).build()
            val wsListener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    relayWebSocket = webSocket
                    _isCloudRelayConnected.value = true
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val json = JSONObject(text)
                        val clip = json.optString("latest_clipboard", json.optString("text", ""))
                        if (clip.isNotBlank() && json.optString("action") == "CLIPBOARD_PUSH") {
                            _latestPcClipboardText.value = clip
                        }
                    } catch (_: Exception) {}
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    _isCloudRelayConnected.value = false
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    _isCloudRelayConnected.value = false
                    relayWebSocket = null
                }
            }

            try {
                client.newWebSocket(wsRequest, wsListener)
            } catch (_: Exception) {
                _isCloudRelayConnected.value = false
            }
        }
    }

    private fun startPeriodicClipboardSync() {
        bridgeScope.launch {
            while (true) {
                try {
                    delay(2500)
                    if (config?.isTwoWayClipboardEnabled == true) {
                        val targetIp = config.desktopBridgeIp
                        if (targetIp.isNotBlank()) {
                            val host = if (!targetIp.contains(":")) "$targetIp:8765" else targetIp
                            val url = "http://$host/clipboard"
                            val request = Request.Builder().url(url).get().build()
                            val resp = client.newCall(request).execute()
                            if (resp.isSuccessful) {
                                val json = JSONObject(resp.body?.string() ?: "{}")
                                val text = json.optString("text", "")
                                if (text.isNotBlank() && text != _latestPcClipboardText.value) {
                                    _latestPcClipboardText.value = text
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    delay(5000)
                }
            }
        }
    }
}
