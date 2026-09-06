package com.example.ownvoice.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ownvoice.OwnVoiceApplication
import com.example.ownvoice.R
import com.example.ownvoice.audio.AudioRecordStreamer
import com.example.ownvoice.network.GeminiRestClient
import com.example.ownvoice.overlay.FloatingBubbleService
import com.example.ownvoice.overlay.OverlayAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateToOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OwnVoiceApplication

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    var floatingBubbleEnabled by remember { mutableStateOf(app.secureConfig.isFloatingBubbleEnabled) }
    var autoContextToneEnabled by remember { mutableStateOf(app.secureConfig.isAutoContextToneEnabled) }
    var selfCorrectionEnabled by remember { mutableStateOf(app.secureConfig.isSelfCorrectionEnabled) }
    var soundEffectsEnabled by remember { mutableStateOf(app.secureConfig.isSoundEffectsEnabled) }
    var hapticFeedbackEnabled by remember { mutableStateOf(app.secureConfig.isHapticFeedbackEnabled) }
    var selectedTone by remember { mutableStateOf(app.secureConfig.dictationMode) }
    var snippets by remember { mutableStateOf(app.secureConfig.getSnippets()) }
    var vocabulary by remember { mutableStateOf(app.secureConfig.getVocabulary()) }
    var bridgeIp by remember { mutableStateOf(app.secureConfig.desktopBridgeIp) }
    var bridgeName by remember { mutableStateOf(app.secureConfig.desktopBridgeName) }
    var isUseForPcEnabled by remember { mutableStateOf(app.secureConfig.isUseForPcEnabled) }
    var isSearchingPC by remember { mutableStateOf(false) }
    var discoveredPCs by remember { mutableStateOf(listOf<com.example.ownvoice.network.DiscoveredDesktop>()) }
    var bridgeStatusMessage by remember { mutableStateOf("") }
    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var targetPinIp by remember { mutableStateOf("") }
    var showManualIp by remember { mutableStateOf(false) }
    val bridgeClient = remember { com.example.ownvoice.network.BridgeClient(app.secureConfig, context) }

    val audioRecorder = remember { AudioRecordStreamer(16000) }
    val geminiClient = remember { GeminiRestClient(app.secureConfig) }
    var isPcDictating by remember { mutableStateOf(false) }
    var isPcProcessing by remember { mutableStateOf(false) }
    var pcDictateFeedback by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            if (isPcDictating) {
                audioRecorder.cancelRecording()
            }
        }
    }

    val qrScanLauncher = rememberLauncherForActivityResult(com.journeyapps.barcodescanner.ScanContract()) { result ->
        if (result.contents != null) {
            val uri = result.contents.trim()
            if (uri.startsWith("ownvoice://pair")) {
                try {
                    val parsed = android.net.Uri.parse(uri)
                    val ip = parsed.getQueryParameter("ip") ?: ""
                    val name = parsed.getQueryParameter("name") ?: "Windows PC"
                    val token = parsed.getQueryParameter("token") ?: ""
                    val pin = parsed.getQueryParameter("pin") ?: ""
                    val qrApiKey = parsed.getQueryParameter("api_key") ?: ""
                    if (ip.isNotBlank()) {
                        bridgeIp = ip
                        bridgeName = name
                        app.secureConfig.desktopBridgeIp = ip
                        app.secureConfig.desktopBridgeName = name
                        if (token.isNotBlank()) app.secureConfig.desktopBridgeToken = token
                        if (pin.isNotBlank()) app.secureConfig.desktopBridgePin = pin
                        if (qrApiKey.isNotBlank() && app.secureConfig.isDefaultOrBlankApiKey) {
                            app.secureConfig.apiKey = qrApiKey
                        }
                        app.secureConfig.isUseForPcEnabled = true
                        isUseForPcEnabled = true
                        bridgeStatusMessage = "✅ Paired with $name ($ip)"
                    }
                } catch (e: Exception) {
                    bridgeStatusMessage = "Error reading QR: ${e.message}"
                }
            } else {
                bridgeStatusMessage = "Unrecognized QR code"
            }
        }
    }

    var gistToken by remember { mutableStateOf(app.secureConfig.gistToken) }
    var gistId by remember { mutableStateOf(app.secureConfig.gistId) }
    var syncStatusMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val gistManager = remember { com.example.ownvoice.network.GistSyncManager() }

    var showRestrictedSettingsDialog by remember { mutableStateOf(false) }
    var showAddSnippetDialog by remember { mutableStateOf(false) }
    var showEditSnippetDialog by remember { mutableStateOf(false) }
    var editingSnippetOriginalKey by remember { mutableStateOf("") }
    var editSnippetTrigger by remember { mutableStateOf("") }
    var editSnippetExpansion by remember { mutableStateOf("") }
    var showAddVocabDialog by remember { mutableStateOf(false) }
    var newTrigger by remember { mutableStateOf("") }
    var newExpansion by remember { mutableStateOf("") }
    var newVocabTerm by remember { mutableStateOf("") }

    val defaultIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
    val isOwnVoiceActiveIme = defaultIme.contains("ownvoice", ignoreCase = true)

    val darkBg = Color(0xFF121214)
    val cardBg = Color(0xFF1C1C22)
    val accentBlue = Color(0xFF0A84FF)
    val accentGreen = Color(0xFF34C759)
    val accentRed = Color(0xFFFF453A)
    val textMuted = Color(0xFF8E8E93)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_icon),
                            contentDescription = "OwnVoice Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("OwnVoice Settings", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBg),
                actions = {
                    IconButton(onClick = onNavigateToOnboarding) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = "API Key", tint = accentBlue)
                    }
                }
            )
        },
        containerColor = darkBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section 0: Microphone Permission Banner (if not granted)
            if (!hasMicPermission) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF3A1A1A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("⚠️ Microphone Permission Needed", fontWeight = FontWeight.Bold, color = accentRed)
                            Text("The keyboard and floating bubble cannot record your voice without microphone access.", fontSize = 12.sp, color = Color.White, modifier = Modifier.padding(vertical = 4.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                colors = ButtonDefaults.buttonColors(containerColor = accentRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Grant Microphone Permission", color = Color.White)
                            }
                        }
                    }
                }
            }

            // Section 1: Input Modalities
            item {
                Text("INPUT MODALITIES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMuted)
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Mode 1: Native Keyboard
                        Text("Mode 1: Native Voice Keyboard (IME)", fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(
                            text = if (isOwnVoiceActiveIme) "✅ OwnVoice is currently your ACTIVE keyboard" else "⚠️ Gboard is currently active. Follow Step 1 & 2 below:",
                            fontSize = 12.sp,
                            color = if (isOwnVoiceActiveIme) accentGreen else accentRed,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C35)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("1. Enable in Settings", fontSize = 11.sp, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    val imm = context.getSystemService(InputMethodManager::class.java)
                                    imm?.showInputMethodPicker()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("2. Switch Keyboard", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFF2C2C35))

                        // Mode 2: Floating Bubble Overlay
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Mode 2: Floating Bubble Overlay", fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("Draggable capsule to dictate over any app", fontSize = 12.sp, color = textMuted)
                            }
                            Switch(
                                checked = floatingBubbleEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                            // On Android 13+, sideloaded apps require restricted settings unlock first!
                                            showRestrictedSettingsDialog = true
                                            return@Switch
                                        }

                                        if (!OverlayAccessibilityService.isRunning()) {
                                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                        }

                                        floatingBubbleEnabled = true
                                        app.secureConfig.isFloatingBubbleEnabled = true
                                        val serviceIntent = Intent(context, FloatingBubbleService::class.java)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            context.startForegroundService(serviceIntent)
                                        } else {
                                            context.startService(serviceIntent)
                                        }
                                    } else {
                                        floatingBubbleEnabled = false
                                        app.secureConfig.isFloatingBubbleEnabled = false
                                        val serviceIntent = Intent(context, FloatingBubbleService::class.java).apply {
                                            action = FloatingBubbleService.ACTION_STOP_BUBBLE
                                        }
                                        context.startService(serviceIntent)
                                    }
                                }
                            )
                        }

                        val isA11yRunning = OverlayAccessibilityService.isRunning()
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isA11yRunning) "✅ Auto-Typing Active (Accessibility ON)" else "⚠️ Auto-Typing requires Accessibility (Tap to Enable)",
                                fontSize = 11.sp,
                                color = if (isA11yRunning) accentGreen else Color(0xFFFF9500),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Section 2: Universal PC Link - FRONT AND CENTER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "UNIVERSAL PC LINK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentBlue,
                        maxLines = 1,
                        softWrap = false
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (bridgeIp.isNotBlank()) accentGreen.copy(alpha = 0.2f) else Color(0xFF33333F)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = if (bridgeIp.isNotBlank()) accentGreen else Color(0xFF8E8E93),
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (bridgeIp.isNotBlank()) "CONNECTED" else "READY TO PAIR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bridgeIp.isNotBlank()) accentGreen else textMuted,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💻 Voice Type Directly into Your Computer", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Text(
                            "Speak into your phone and watch words type directly into your PC/Mac/Linux cursor in real time over local Wi-Fi.",
                            fontSize = 12.sp,
                            color = textMuted,
                            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                        )

                        // Paired Device Status Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF25252E),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (bridgeIp.isNotBlank()) accentGreen else Color(0xFFFF9500),
                                            modifier = Modifier.size(8.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (bridgeIp.isNotBlank()) "Paired: ${bridgeName.ifBlank { "Desktop PC" }}" else "No Computer Paired Yet",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                    if (bridgeIp.isNotBlank()) {
                                        Text("Host: $bridgeIp:8765", fontSize = 11.sp, color = textMuted)
                                    } else {
                                        Text("No PC linked. Tap Auto-Discover or enter PIN below", fontSize = 11.sp, color = Color(0xFFFFCC00))
                                    }
                                }

                                if (bridgeIp.isNotBlank()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    bridgeStatusMessage = "Testing connection to $bridgeIp:8765..."
                                                    val alive = bridgeClient.checkStatus(bridgeIp)
                                                    bridgeStatusMessage = if (alive) {
                                                        "✅ PC Connected & Ready ($bridgeIp:8765)!"
                                                    } else {
                                                        "❌ Unreachable at $bridgeIp:8765. Check app is running on PC & both are on same Wi-Fi."
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF33333F)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Test", fontSize = 11.sp, color = Color.White)
                                        }

                                        Button(
                                            onClick = {
                                                bridgeIp = ""
                                                bridgeName = ""
                                                app.secureConfig.desktopBridgeIp = ""
                                                app.secureConfig.desktopBridgeName = ""
                                                app.secureConfig.desktopBridgeToken = ""
                                                app.secureConfig.desktopBridgePin = ""
                                                app.secureConfig.isUseForPcEnabled = false
                                                isUseForPcEnabled = false
                                                bridgeStatusMessage = "Unpaired"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF442222)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Unpair", fontSize = 11.sp, color = accentRed)
                                        }
                                    }
                                }
                            }
                        }

                        // Feature: "Use for PC" Direct Streaming & Dictation Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isUseForPcEnabled && bridgeIp.isNotBlank()) Color(0xFF132838) else Color(0xFF22222B),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isUseForPcEnabled && bridgeIp.isNotBlank()) accentGreen.copy(alpha = 0.6f) else Color(0xFF2E2E3A)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Use for PC",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 15.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            if (isUseForPcEnabled && bridgeIp.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = accentGreen.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "STREAMING ACTIVE",
                                                        color = accentGreen,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = if (bridgeIp.isNotBlank()) {
                                                "When enabled, speaking types directly into your PC's active cursor."
                                            } else {
                                                "Connect your computer below to activate live PC cursor typing."
                                            },
                                            fontSize = 11.sp,
                                            color = textMuted,
                                            modifier = Modifier.padding(top = 3.dp)
                                        )
                                    }

                                    Switch(
                                        checked = isUseForPcEnabled && bridgeIp.isNotBlank(),
                                        enabled = bridgeIp.isNotBlank(),
                                        onCheckedChange = { checked ->
                                            isUseForPcEnabled = checked
                                            app.secureConfig.isUseForPcEnabled = checked
                                            bridgeStatusMessage = if (checked) {
                                                "💻 Use for PC enabled! Speak below or use your keyboard."
                                            } else {
                                                "PC streaming paused."
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = accentGreen,
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = Color(0xFF33333F)
                                        )
                                    )
                                }

                                // Interactive In-App Mic when "Use for PC" is enabled
                                if (isUseForPcEnabled && bridgeIp.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = Color(0xFF28384A))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "PIN CURSOR ON PC & SPEAK INTO PHONE:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentBlue,
                                            letterSpacing = 0.5.sp
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = {
                                                if (!hasMicPermission) {
                                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                    return@Button
                                                }
                                                if (app.secureConfig.apiKey.isBlank()) {
                                                    bridgeStatusMessage = "⚠️ Please enter your Gemini API Key in AI Settings below."
                                                    return@Button
                                                }

                                                if (!isPcDictating) {
                                                    val started = audioRecorder.startRecording()
                                                    if (started) {
                                                        isPcDictating = true
                                                        isPcProcessing = false
                                                        pcDictateFeedback = "🎙️ Listening... Speak naturally, tap again when done"
                                                    } else {
                                                        pcDictateFeedback = "❌ Microphone is busy. Try again."
                                                    }
                                                } else {
                                                    isPcDictating = false
                                                    isPcProcessing = true
                                                    pcDictateFeedback = "⏳ Transcribing & typing into PC cursor..."

                                                    scope.launch {
                                                        val wavBytes = withContext(Dispatchers.IO) {
                                                            audioRecorder.stopRecording()
                                                        }
                                                        if (wavBytes.isNotEmpty() && wavBytes.size >= 400) {
                                                            try {
                                                                val (text, _) = geminiClient.transcribeAudio(
                                                                    wavBytes,
                                                                    mode = app.secureConfig.dictationMode
                                                                )
                                                                if (text.isNotBlank()) {
                                                                    val expanded = app.secureConfig.getSnippets().entries.fold(text) { acc, (k, v) ->
                                                                        acc.replace(k, v)
                                                                    }

                                                                    var targetIp = bridgeIp.ifBlank { app.secureConfig.desktopBridgeIp }
                                                                    if (targetIp.isBlank()) {
                                                                        pcDictateFeedback = "🔍 Auto-discovering PC on local Wi-Fi..."
                                                                        val found = bridgeClient.discoverLocalDesktops(timeoutMs = 1500)
                                                                        if (found.isNotEmpty()) {
                                                                            val pc = found.first()
                                                                            targetIp = pc.ip
                                                                            bridgeIp = pc.ip
                                                                            bridgeName = pc.name
                                                                            app.secureConfig.desktopBridgeIp = pc.ip
                                                                            app.secureConfig.desktopBridgeName = pc.name
                                                                            if (pc.token.isNotBlank()) app.secureConfig.desktopBridgeToken = pc.token
                                                                            if (pc.pin.isNotBlank()) app.secureConfig.desktopBridgePin = pc.pin
                                                                            if (pc.apiKey.isNotBlank() && app.secureConfig.isDefaultOrBlankApiKey) {
                                                                                app.secureConfig.apiKey = pc.apiKey
                                                                            }
                                                                            app.secureConfig.isUseForPcEnabled = true
                                                                            isUseForPcEnabled = true
                                                                        }
                                                                    }

                                                                    if (targetIp.isBlank()) {
                                                                        pcDictateFeedback = "⚠️ No PC found on Wi-Fi. Tap '⚡ Auto-Discover' or '🔢 PIN' below to link your PC."
                                                                    } else {
                                                                        val ok = bridgeClient.sendToDesktop(
                                                                            desktopIp = targetIp,
                                                                            text = expanded,
                                                                            token = app.secureConfig.desktopBridgeToken
                                                                        )
                                                                        if (ok) {
                                                                            pcDictateFeedback = "✅ Typed to PC: \"$expanded\""
                                                                        } else {
                                                                            pcDictateFeedback = "❌ PC didn't respond at $targetIp:8765. Verify OwnVoice is running on PC."
                                                                        }
                                                                    }
                                                                } else {
                                                                    pcDictateFeedback = "⚠️ No speech detected (silence)"
                                                                }
                                                            } catch (e: Exception) {
                                                                pcDictateFeedback = "❌ Error: ${e.message ?: "Transcription failed"}"
                                                            }
                                                        } else {
                                                            pcDictateFeedback = "⚠️ Audio too short. Speak and tap to send."
                                                        }
                                                        isPcProcessing = false
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isPcDictating) accentRed else if (isPcProcessing) Color(0xFF244CA8) else accentGreen
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            enabled = !isPcProcessing
                                        ) {
                                            if (isPcProcessing) {
                                                CircularProgressIndicator(
                                                    color = Color.White,
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Transcribing to PC…", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            } else if (isPcDictating) {
                                                Text("⏹️ Tap to Stop & Type to PC", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            } else {
                                                Text("🎙️ Tap to Speak to PC Cursor", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }

                                        if (pcDictateFeedback.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = pcDictateFeedback,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (pcDictateFeedback.startsWith("✅")) accentGreen else if (pcDictateFeedback.startsWith("❌") || pcDictateFeedback.startsWith("⚠️")) Color(0xFFFF9500) else accentBlue
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Instructions banner
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E2433),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("HOW TO CONNECT IN 2 SECONDS:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentBlue)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("1. On your PC, open OwnVoice (double-click 'run_phone_link.bat' or run 'python app.py --link').", fontSize = 11.sp, color = Color.White)
                                Text("2. Look at your PC screen ➔ You will see a QR Code and 6-Digit PIN.", fontSize = 11.sp, color = Color.White)
                                Text("3. Tap '⚡ Auto-Discover' or '📷 Scan QR' below.", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        if (bridgeStatusMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(bridgeStatusMessage, fontSize = 12.sp, color = accentBlue, fontWeight = FontWeight.Medium)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3 Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1-Tap Auto-Discover
                            Button(
                                onClick = {
                                    isSearchingPC = true
                                    bridgeStatusMessage = "Searching local Wi-Fi for OwnVoice PC..."
                                    discoveredPCs = emptyList()
                                    scope.launch {
                                        val found = bridgeClient.discoverLocalDesktops(timeoutMs = 1800)
                                        discoveredPCs = found
                                        isSearchingPC = false
                                        if (found.isEmpty()) {
                                            bridgeStatusMessage = "No PC found. Ensure OwnVoice is running on PC on the same Wi-Fi."
                                        } else if (found.size == 1) {
                                            val pc = found.first()
                                            bridgeIp = pc.ip
                                            bridgeName = pc.name
                                            app.secureConfig.desktopBridgeIp = pc.ip
                                            app.secureConfig.desktopBridgeName = pc.name
                                            if (pc.token.isNotBlank()) app.secureConfig.desktopBridgeToken = pc.token
                                            if (pc.pin.isNotBlank()) app.secureConfig.desktopBridgePin = pc.pin
                                            if (pc.apiKey.isNotBlank() && app.secureConfig.isDefaultOrBlankApiKey) {
                                                app.secureConfig.apiKey = pc.apiKey
                                            }
                                            app.secureConfig.isUseForPcEnabled = true
                                            isUseForPcEnabled = true
                                            bridgeStatusMessage = "⚡ Auto-Paired with ${pc.name} (${pc.ip})!"
                                        } else {
                                            bridgeStatusMessage = "Found ${found.size} PCs on Wi-Fi. Tap below to pair."
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Text(
                                    if (isSearchingPC) "⏳ Searching…" else "⚡ Auto-Discover",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Scan QR Code
                            Button(
                                onClick = {
                                    try {
                                        qrScanLauncher.launch(
                                            com.journeyapps.barcodescanner.ScanOptions()
                                                .setPrompt("Point camera at OwnVoice Desktop QR Code")
                                                .setBeepEnabled(true)
                                                .setOrientationLocked(false)
                                        )
                                    } catch (e: Exception) {
                                        bridgeStatusMessage = "Camera scanner error: ${e.message}"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C35)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("📷 Scan QR", fontSize = 11.sp, color = Color.White)
                            }

                            // 6-Digit PIN
                            Button(
                                onClick = {
                                    enteredPin = ""
                                    targetPinIp = bridgeIp
                                    showPinDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C35)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Text("🔢 PIN", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        // Discovered PCs list
                        if (discoveredPCs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                discoveredPCs.forEach { pc ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E293B),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("💻 ${pc.name}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                Text("IP: ${pc.ip}:${pc.port}", color = textMuted, fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    bridgeIp = pc.ip
                                                    bridgeName = pc.name
                                                    app.secureConfig.desktopBridgeIp = pc.ip
                                                    app.secureConfig.desktopBridgeName = pc.name
                                                    if (pc.token.isNotBlank()) app.secureConfig.desktopBridgeToken = pc.token
                                                    if (pc.pin.isNotBlank()) app.secureConfig.desktopBridgePin = pc.pin
                                                    if (pc.apiKey.isNotBlank() && app.secureConfig.isDefaultOrBlankApiKey) {
                                                        app.secureConfig.apiKey = pc.apiKey
                                                    }
                                                    app.secureConfig.isUseForPcEnabled = true
                                                    isUseForPcEnabled = true
                                                    bridgeStatusMessage = "⚡ Paired with ${pc.name}!"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = accentGreen),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("Pair", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Collapsible Advanced Manual IP
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showManualIp = !showManualIp }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (showManualIp) "▲ Hide Manual IP Override" else "▼ Advanced: Manual IP Setup",
                                fontSize = 11.sp,
                                color = textMuted
                            )
                        }

                        if (showManualIp) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = bridgeIp,
                                onValueChange = {
                                    bridgeIp = it
                                    app.secureConfig.desktopBridgeIp = it
                                },
                                label = { Text("PC Wi-Fi IP (e.g. 192.168.1.15)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Section 3: AI Intelligence & Auto-Tone (Version 2.0)
            item {
                Text("AI INTELLIGENCE (v2.0)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMuted)
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Feature 1: Auto Context-Aware Tone
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto Context-Aware Tone", fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("Adapts tone to active app (WhatsApp: Chat, Gmail: Email, Chrome: Search)", fontSize = 12.sp, color = textMuted)
                            }
                            Switch(
                                checked = autoContextToneEnabled,
                                onCheckedChange = { isChecked ->
                                    autoContextToneEnabled = isChecked
                                    app.secureConfig.isAutoContextToneEnabled = isChecked
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF2C2C35))

                        // Feature 2: Spoken Self-Correction
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Spoken Self-Correction", fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("Fixes mid-sentence slips ('meet at 4, actually 5 PM' ➔ 'meet at 5 PM')", fontSize = 12.sp, color = textMuted)
                            }
                            Switch(
                                checked = selfCorrectionEnabled,
                                onCheckedChange = { isChecked ->
                                    selfCorrectionEnabled = isChecked
                                    app.secureConfig.isSelfCorrectionEnabled = isChecked
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF2C2C35))

                        // Feature 3: Acoustic Audio Cues
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Acoustic Audio Cues", fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("Subtle sound effects on start and stop dictation", fontSize = 12.sp, color = textMuted)
                            }
                            Switch(
                                checked = soundEffectsEnabled,
                                onCheckedChange = { isChecked ->
                                    soundEffectsEnabled = isChecked
                                    app.secureConfig.isSoundEffectsEnabled = isChecked
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF2C2C35))

                        // Feature 4: Tactile Haptic Feedback
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Subtle Haptic Feedback", fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("Crisp, gentle keypress ticks and hold-backspace pulses", fontSize = 12.sp, color = textMuted)
                            }
                            Switch(
                                checked = hapticFeedbackEnabled,
                                onCheckedChange = { isChecked ->
                                    hapticFeedbackEnabled = isChecked
                                    app.secureConfig.isHapticFeedbackEnabled = isChecked
                                }
                            )
                        }
                    }
                }
            }

            // Section 3: Dictation Tone (Fallback & Keyboard Override)
            item {
                Text("DEFAULT DICTATION TONE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMuted)
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val tones = listOf(
                            "smart_flow" to "Smart Flow (Universal)",
                            "search" to "Search Query (Chrome / YouTube / Spotify)",
                            "chat" to "Casual Chat (WhatsApp / Telegram / Slack)",
                            "formal_email" to "Executive Email (Gmail / Outlook)",
                            "formal_document" to "Documentation Prose (Notion / Keep)",
                            "code" to "Coding / Shell (Termux / GitHub / VS Code)",
                            "translate_hindi" to "Translate English ➔ Hindi",
                            "translate_english" to "Translate Hindi ➔ English",
                            "bullet_notes" to "Bullet Points (- List)",
                            "verbatim" to "Verbatim (Word-for-word)"
                        )

                        tones.forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedTone = key
                                        app.secureConfig.dictationMode = key
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (selectedTone == key),
                                    onClick = {
                                        selectedTone = key
                                        app.secureConfig.dictationMode = key
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = accentBlue)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Section 4: Personal Vocabulary Bank (Version 2.0)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("PERSONAL VOCABULARY BANK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMuted)
                    IconButton(onClick = { showAddVocabDialog = true }, modifier = Modifier.size(28.dp)) {
                        Text("+", color = accentBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Custom names, slang, and technical jargon taught to Gemini:", fontSize = 12.sp, color = textMuted)
                        Spacer(modifier = Modifier.height(10.dp))
                        if (vocabulary.isEmpty()) {
                            Text("No custom words added yet. Tap '+' above to add your own names, Indian terminology, or company acronyms.", color = textMuted, fontSize = 12.sp)
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                vocabulary.forEach { term ->
                                    Surface(
                                        color = Color(0xFF2C2C35),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(term, color = Color.White, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete",
                                                tint = textMuted,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable {
                                                        app.secureConfig.removeVocabularyTerm(term)
                                                        vocabulary = app.secureConfig.getVocabulary()
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Voice Snippets
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("VOICE SNIPPETS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMuted)
                    IconButton(onClick = { showAddSnippetDialog = true }, modifier = Modifier.size(28.dp)) {
                        Text("+", color = accentBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Tap any snippet to edit trigger or expansion, or tap '+' above to add new:",
                            fontSize = 12.sp,
                            color = textMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (snippets.isEmpty()) {
                            Text("No snippets configured. Tap '+' to add.", color = textMuted, fontSize = 13.sp)
                        } else {
                            snippets.forEach { (trigger, expansion) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            editingSnippetOriginalKey = trigger
                                            editSnippetTrigger = trigger
                                            editSnippetExpansion = expansion
                                            showEditSnippetDialog = true
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(trigger, fontWeight = FontWeight.Medium, color = Color.White, fontSize = 14.sp)
                                        Text(expansion, color = textMuted, fontSize = 12.sp, maxLines = 1)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                editingSnippetOriginalKey = trigger
                                                editSnippetTrigger = trigger
                                                editSnippetExpansion = expansion
                                                showEditSnippetDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Snippet",
                                                tint = accentBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val updated = snippets.toMutableMap()
                                                updated.remove(trigger)
                                                snippets = updated
                                                app.secureConfig.saveSnippets(updated)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete Snippet",
                                                tint = accentRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 6: GitHub Gist Cloud Sync
            item {
                Text("GITHUB GIST CLOUD SYNC", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMuted)
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("☁️ Sync Vocabulary & Snippets", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "Keep your custom vocabulary, names, and snippets synchronized between your Windows PC and your phone.",
                            fontSize = 12.sp,
                            color = textMuted,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = gistToken,
                            onValueChange = {
                                gistToken = it
                                app.secureConfig.gistToken = it
                            },
                            label = { Text("GitHub Personal Access Token (PAT)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = gistId,
                            onValueChange = {
                                gistId = it
                                app.secureConfig.gistId = it
                            },
                            label = { Text("Gist ID (leave blank to create new)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (syncStatusMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(syncStatusMessage, fontSize = 12.sp, color = accentBlue)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        syncStatusMessage = "Syncing to GitHub Gist..."
                                        val payload = org.json.JSONObject().apply {
                                            put("vocabulary", org.json.JSONArray(app.secureConfig.getVocabulary()))
                                            val snipJson = org.json.JSONObject()
                                            app.secureConfig.getSnippets().forEach { (k, v) -> snipJson.put(k, v) }
                                            put("snippets", snipJson)
                                            put("dictation_mode", app.secureConfig.dictationMode)
                                        }
                                        val res = gistManager.syncToGist(gistToken, payload, gistId.ifBlank { null })
                                        syncStatusMessage = res.message
                                        if (res.success && res.gistId != null) {
                                            gistId = res.gistId
                                            app.secureConfig.gistId = res.gistId
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("⬆️ Push")
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        syncStatusMessage = "Pulling from GitHub Gist..."
                                        val res = gistManager.syncFromGist(gistToken, gistId)
                                        syncStatusMessage = res.message
                                        if (res.success && res.payload != null) {
                                            val p = res.payload
                                            val vocabArr = p.optJSONArray("vocabulary")
                                            if (vocabArr != null) {
                                                for (i in 0 until vocabArr.length()) {
                                                    val term = vocabArr.getString(i)
                                                    app.secureConfig.addVocabularyTerm(term)
                                                }
                                                vocabulary = app.secureConfig.getVocabulary()
                                            }
                                            val snipObj = p.optJSONObject("snippets")
                                            if (snipObj != null) {
                                                val existing = app.secureConfig.getSnippets().toMutableMap()
                                                val keys = snipObj.keys()
                                                while (keys.hasNext()) {
                                                    val k = keys.next()
                                                    existing[k] = snipObj.getString(k)
                                                }
                                                app.secureConfig.saveSnippets(existing)
                                                snippets = existing
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("⬇️ Pull", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for Android 13+ Restricted Settings Unlock
    if (showRestrictedSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showRestrictedSettingsDialog = false },
            title = { Text("Unlock Android Permission First", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "Android 13/14+ restricts 'Display over other apps' for sideloaded apps until you allow it:\n\n" +
                               "1. Tap 'Open App Info' below.\n" +
                               "2. Tap the 3 dots (⋮) in the top-right corner.\n" +
                               "3. Tap 'Allow restricted settings' and enter your PIN.\n" +
                               "4. Then tap 'Display over other apps' to turn it ON.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestrictedSettingsDialog = false
                        // Open App Info where the 3 dots exist!
                        val appInfoIntent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        context.startActivity(appInfoIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
                ) {
                    Text("Open App Info (3 Dots)")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestrictedSettingsDialog = false
                        // Direct overlay settings fallback
                        val overlayIntent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        context.startActivity(overlayIntent)
                    }
                ) {
                    Text("Direct Overlay Screen")
                }
            },
            containerColor = cardBg
        )
    }

    if (showAddSnippetDialog) {
        AlertDialog(
            onDismissRequest = { showAddSnippetDialog = false },
            title = { Text("Add Voice Snippet", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTrigger,
                        onValueChange = { newTrigger = it },
                        label = { Text("Spoken Trigger (e.g. 'my email')") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newExpansion,
                        onValueChange = { newExpansion = it },
                        label = { Text("Expanded Text (e.g. 'me@email.com')") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTrigger.isNotBlank() && newExpansion.isNotBlank()) {
                            val updated = snippets.toMutableMap()
                            updated[newTrigger.trim().lowercase()] = newExpansion.trim()
                            snippets = updated
                            app.secureConfig.saveSnippets(updated)
                            newTrigger = ""
                            newExpansion = ""
                            showAddSnippetDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSnippetDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = cardBg
        )
    }

    if (showEditSnippetDialog) {
        AlertDialog(
            onDismissRequest = {
                showEditSnippetDialog = false
                editingSnippetOriginalKey = ""
                editSnippetTrigger = ""
                editSnippetExpansion = ""
            },
            title = { Text("Edit Voice Snippet", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Update the trigger phrase and text expansion for this snippet.",
                        color = textMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editSnippetTrigger,
                        onValueChange = { editSnippetTrigger = it },
                        label = { Text("Spoken Trigger (e.g. 'my email')") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editSnippetExpansion,
                        onValueChange = { editSnippetExpansion = it },
                        label = { Text("Expanded Text (e.g. 'me@email.com')") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedTrigger = editSnippetTrigger.trim().lowercase()
                        val trimmedExpansion = editSnippetExpansion.trim()
                        if (trimmedTrigger.isNotBlank() && trimmedExpansion.isNotBlank()) {
                            val updated = snippets.toMutableMap()
                            if (editingSnippetOriginalKey.isNotBlank() && editingSnippetOriginalKey != trimmedTrigger) {
                                updated.remove(editingSnippetOriginalKey)
                            }
                            updated[trimmedTrigger] = trimmedExpansion
                            snippets = updated
                            app.secureConfig.saveSnippets(updated)
                            showEditSnippetDialog = false
                            editingSnippetOriginalKey = ""
                            editSnippetTrigger = ""
                            editSnippetExpansion = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditSnippetDialog = false
                        editingSnippetOriginalKey = ""
                        editSnippetTrigger = ""
                        editSnippetExpansion = ""
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = cardBg
        )
    }

    if (showAddVocabDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddVocabDialog = false
                newVocabTerm = ""
            },
            title = { Text("Add Personal Vocabulary", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Teach Gemini specific names, acronyms, or technical jargon (e.g. 'Aarav', 'Bengaluru', 'Kubernetes', 'B2B SaaS'). Gemini will prioritize this exact spelling.",
                        color = textMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newVocabTerm,
                        onValueChange = { newVocabTerm = it },
                        label = { Text("Word / Name / Jargon") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newVocabTerm.trim()
                        if (trimmed.isNotBlank()) {
                            app.secureConfig.addVocabularyTerm(trimmed)
                            vocabulary = app.secureConfig.getVocabulary()
                            newVocabTerm = ""
                            showAddVocabDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
                ) {
                    Text("Add Term")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddVocabDialog = false
                        newVocabTerm = ""
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = cardBg
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Enter 6-Digit PIN", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Enter the 6-digit PIN displayed on your PC in the 'Phone Link' tab:",
                        color = textMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { if (it.length <= 6) enteredPin = it.filter { char -> char.isDigit() } },
                        label = { Text("6-Digit PIN (e.g. 849201)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (bridgeIp.isBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = targetPinIp,
                            onValueChange = { targetPinIp = it.trim() },
                            label = { Text("PC IP Address (optional if auto-detected)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredPin.length == 6) {
                            scope.launch {
                                bridgeStatusMessage = "Verifying PIN with PC..."
                                var targetIp = targetPinIp.ifBlank { bridgeIp }
                                if (targetIp.isBlank()) {
                                    val found = bridgeClient.discoverLocalDesktops(timeoutMs = 1200)
                                    if (found.isNotEmpty()) {
                                        targetIp = found.first().ip
                                    }
                                }
                                if (targetIp.isNotBlank()) {
                                    val res = bridgeClient.pairWithPin(targetIp, enteredPin)
                                    if (res.success) {
                                        bridgeIp = targetIp
                                        bridgeName = res.deviceName
                                        app.secureConfig.desktopBridgeIp = targetIp
                                        app.secureConfig.desktopBridgeName = res.deviceName
                                        app.secureConfig.desktopBridgeToken = res.token
                                        app.secureConfig.desktopBridgePin = enteredPin
                                        if (res.apiKey.isNotBlank() && app.secureConfig.isDefaultOrBlankApiKey) {
                                            app.secureConfig.apiKey = res.apiKey
                                        }
                                        app.secureConfig.isUseForPcEnabled = true
                                        isUseForPcEnabled = true
                                        bridgeStatusMessage = "✅ Paired with ${res.deviceName}!"
                                        showPinDialog = false
                                    } else {
                                        bridgeStatusMessage = "❌ PIN verification failed: ${res.error}"
                                    }
                                } else {
                                    bridgeStatusMessage = "❌ PC not found. Enter IP above or ensure PC is on."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
                ) {
                    Text("Pair")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = cardBg
        )
    }
}
