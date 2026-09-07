package com.example.ownvoice.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ownvoice.OwnVoiceApplication
import com.example.ownvoice.audio.AudioRecordStreamer
import com.example.ownvoice.core.HistoryItem
import com.example.ownvoice.network.BridgeClient
import com.example.ownvoice.network.GeminiRestClient
import com.example.ownvoice.theme.DesignTokens
import com.example.ownvoice.ui.components.BottomNavBar
import com.example.ownvoice.ui.components.ComputerStatusCard
import com.example.ownvoice.ui.components.HeroMicOrb
import com.example.ownvoice.ui.components.MicOrbState
import com.example.ownvoice.ui.navigation.ScreenDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    onNavigate: (ScreenDestination) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OwnVoiceApplication
    val scope = rememberCoroutineScope()

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

    val audioRecorder = remember { AudioRecordStreamer(16000) }
    val geminiClient = remember { GeminiRestClient(app.secureConfig) }
    val bridgeClient = remember { BridgeClient(app.secureConfig, context) }

    var orbState by remember { mutableStateOf(MicOrbState.IDLE) }
    var currentTranscription by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf("") }
    var recentHistory by remember { mutableStateOf(app.secureConfig.getHistory()) }

    var isPcConnected by remember { mutableStateOf(app.secureConfig.desktopBridgeIp.isNotBlank()) }
    var isUseForPc by remember { mutableStateOf(app.secureConfig.isUseForPcEnabled) }
    var selectedTone by remember { mutableStateOf(app.secureConfig.dictationMode) }

    DisposableEffect(Unit) {
        onDispose {
            if (orbState == MicOrbState.LISTENING) {
                audioRecorder.cancelRecording()
            }
        }
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OwnVoice Transcription", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun formatRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${days}d ago"
        }
    }

    Scaffold(
        containerColor = DesignTokens.Colors.BackgroundDark,
        bottomBar = {
            BottomNavBar(
                currentDestination = ScreenDestination.Home,
                onNavigate = onNavigate,
                showCenterMic = false
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: App branding & Ready status
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DesignTokens.Colors.PrimaryBlue.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "OwnVoice",
                                    tint = DesignTokens.Colors.PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "OwnVoice",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DesignTokens.Colors.TextPrimary
                        )
                    }

                    // Status chip
                    Surface(
                        shape = CircleShape,
                        color = DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (orbState == MicOrbState.LISTENING) DesignTokens.Colors.StatusRecording
                                        else DesignTokens.Colors.StatusReady,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (orbState) {
                                    MicOrbState.LISTENING -> "Recording"
                                    MicOrbState.PROCESSING -> "Thinking"
                                    MicOrbState.IDLE -> "Ready"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = DesignTokens.Colors.TextPrimary
                            )
                        }
                    }
                }
            }

            // Hero Mic Orb
            item {
                Spacer(modifier = Modifier.height(10.dp))
                HeroMicOrb(
                    state = orbState,
                    onClick = {
                        if (!hasMicPermission) {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            return@HeroMicOrb
                        }

                        if (orbState == MicOrbState.IDLE) {
                            val started = audioRecorder.startRecording()
                            if (started) {
                                orbState = MicOrbState.LISTENING
                                feedbackMessage = "Listening... Speak naturally"
                            } else {
                                feedbackMessage = "Microphone is busy. Please try again."
                            }
                        } else if (orbState == MicOrbState.LISTENING) {
                            orbState = MicOrbState.PROCESSING
                            feedbackMessage = "Transcribing with Gemini 3.6 Flash..."

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
                                            currentTranscription = expanded

                                            val target = if (app.secureConfig.isUseForPcEnabled && app.secureConfig.desktopBridgeIp.isNotBlank()) "PC" else "Phone"
                                            val newItem = app.secureConfig.addHistoryItem(expanded, target = target)
                                            recentHistory = app.secureConfig.getHistory()

                                            // If Use for PC is enabled, inject keystrokes directly into PC cursor
                                            if (app.secureConfig.isUseForPcEnabled && app.secureConfig.desktopBridgeIp.isNotBlank()) {
                                                val ok = bridgeClient.sendToDesktop(
                                                    desktopIp = app.secureConfig.desktopBridgeIp,
                                                    text = expanded,
                                                    token = app.secureConfig.desktopBridgeToken
                                                )
                                                feedbackMessage = if (ok) {
                                                    "Typed to PC cursor: \"${expanded.take(30)}...\""
                                                } else {
                                                    "Transcribed! (PC link error: ${bridgeClient.lastError})"
                                                }
                                            } else {
                                                feedbackMessage = "Transcribed successfully!"
                                            }
                                        } else {
                                            feedbackMessage = "No speech detected. Please try again."
                                        }
                                    } catch (e: Exception) {
                                        feedbackMessage = "Error: ${e.message ?: "Transcription failed"}"
                                    }
                                } else {
                                    feedbackMessage = "Audio too short. Please speak longer."
                                }
                                orbState = MicOrbState.IDLE
                            }
                        }
                    }
                )
            }

            // Status feedback message under Hero Mic
            if (feedbackMessage.isNotBlank()) {
                item {
                    Text(
                        text = feedbackMessage,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (feedbackMessage.startsWith("Error")) DesignTokens.Colors.StatusRecording
                               else if (feedbackMessage.startsWith("Typed")) DesignTokens.Colors.StatusReady
                               else DesignTokens.Colors.PrimaryLightBlue
                    )
                }
            }

            // Live / Current Transcription Result Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Transcription Output",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DesignTokens.Colors.TextMuted
                            )
                            if (currentTranscription.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Clear",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = DesignTokens.Colors.TextMuted,
                                        modifier = Modifier.clickable {
                                            currentTranscription = ""
                                            feedbackMessage = ""
                                        }
                                    )
                                    Text(
                                        text = "Copy",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DesignTokens.Colors.PrimaryLightBlue,
                                        modifier = Modifier.clickable {
                                            copyToClipboard(currentTranscription)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (currentTranscription.isNotBlank()) {
                            Text(
                                text = currentTranscription,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = DesignTokens.Colors.TextPrimary,
                                fontWeight = FontWeight.Normal
                            )
                        } else {
                            Text(
                                text = "Your voice transcription will appear here in real time. Tap the mic above to speak.",
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = DesignTokens.Colors.TextSubtle
                            )
                        }
                    }
                }
            }

            // Computer Connection Card
            item {
                ComputerStatusCard(
                    isConnected = isPcConnected,
                    computerName = app.secureConfig.desktopBridgeName.ifBlank { "Windows PC" },
                    ipAddress = app.secureConfig.desktopBridgeIp,
                    onClick = {
                        onNavigate(ScreenDestination.ConnectedDevices)
                    }
                )
            }

            // Mode & Feature Quick Pills
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tone pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onNavigate(ScreenDestination.VoiceSettings)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Tone",
                                tint = DesignTokens.Colors.PrimaryLightBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (selectedTone) {
                                    "raw" -> "Raw"
                                    "professional" -> "Executive"
                                    "casual" -> "Casual"
                                    "code" -> "Code"
                                    else -> "Smart Flow"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = DesignTokens.Colors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Use for PC toggle pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isUseForPc && isPcConnected) DesignTokens.Colors.PrimaryBlue.copy(alpha = 0.2f) else DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isUseForPc && isPcConnected) DesignTokens.Colors.PrimaryBlue else DesignTokens.Colors.BorderSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (isPcConnected) {
                                    isUseForPc = !isUseForPc
                                    app.secureConfig.isUseForPcEnabled = isUseForPc
                                    feedbackMessage = if (isUseForPc) "PC Keystrokes Active" else "PC Keystrokes Paused"
                                } else {
                                    onNavigate(ScreenDestination.ConnectedDevices)
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Laptop,
                                contentDescription = "PC",
                                tint = if (isUseForPc && isPcConnected) DesignTokens.Colors.PrimaryLightBlue else DesignTokens.Colors.TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isUseForPc && isPcConnected) "PC Typing: ON" else "PC Typing: OFF",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isUseForPc && isPcConnected) DesignTokens.Colors.PrimaryLightBlue else DesignTokens.Colors.TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Recent Transcriptions Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transcriptions",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.Colors.TextPrimary
                    )
                    Text(
                        text = "See all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DesignTokens.Colors.PrimaryLightBlue,
                        modifier = Modifier.clickable {
                            onNavigate(ScreenDestination.RecentTranscriptions)
                        }
                    )
                }
            }

            // Show up to 3 recent items
            val displayItems = recentHistory.take(3)
            if (displayItems.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transcriptions yet. Tap the mic to record!",
                                fontSize = 13.sp,
                                color = DesignTokens.Colors.TextSubtle
                            )
                        }
                    }
                }
            } else {
                items(displayItems) { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                copyToClipboard(item.text)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.text,
                                    fontSize = 13.sp,
                                    color = DesignTokens.Colors.TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = formatRelativeTime(item.timestamp),
                                        fontSize = 11.sp,
                                        color = DesignTokens.Colors.TextSubtle
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "• ${item.target}",
                                        fontSize = 11.sp,
                                        color = DesignTokens.Colors.PrimaryLightBlue
                                    )
                                }
                            }
                            IconButton(
                                onClick = { copyToClipboard(item.text) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = DesignTokens.Colors.TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Pro-Tip Banner
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Pro-Tip",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pro-Tip",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DesignTokens.Colors.TextPrimary
                            )
                            Text(
                                text = "Turn on 'PC Typing' to speak directly into your desktop cursor anywhere on your screen.",
                                fontSize = 11.sp,
                                color = DesignTokens.Colors.TextMuted,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
