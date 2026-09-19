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
import com.example.ownvoice.core.HistoryItem
import com.example.ownvoice.core.VoiceAssistantState
import com.example.ownvoice.core.VoiceAssistantViewModel
import com.example.ownvoice.service.MeetingRecordingService
import com.example.ownvoice.theme.DesignTokens
import com.example.ownvoice.ui.components.ComputerStatusCard
import com.example.ownvoice.ui.components.HeroMicOrb
import com.example.ownvoice.ui.components.MicOrbState
import com.example.ownvoice.ui.navigation.ScreenDestination

@Composable
fun HomeScreen(
    viewModel: VoiceAssistantViewModel,
    onNavigate: (ScreenDestination) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OwnVoiceApplication

    val voiceState by viewModel.state.collectAsState()
    val lastTranscription by viewModel.lastTranscription.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.startRecording()
        }
    }

    var isPcConnected by remember { mutableStateOf(app.secureConfig.desktopBridgeIp.isNotBlank()) }
    var isUseForPc by remember { mutableStateOf(app.secureConfig.isUseForPcEnabled) }
    var selectedTone by remember { mutableStateOf(app.secureConfig.dictationMode) }
    var recentHistory by remember { mutableStateOf(app.secureConfig.getHistory()) }

    // Refresh history whenever voiceState transitions to Success
    LaunchedEffect(voiceState) {
        if (voiceState is VoiceAssistantState.Success) {
            recentHistory = app.secureConfig.getHistory()
        }
    }

    // Map VoiceAssistantState to HeroMicOrb State
    val heroOrbState = when (voiceState) {
        is VoiceAssistantState.Recording -> MicOrbState.LISTENING
        is VoiceAssistantState.Processing -> MicOrbState.PROCESSING
        else -> MicOrbState.IDLE
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: App branding & Ready status
        item {
            Spacer(modifier = Modifier.height(10.dp))
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
                                    color = if (voiceState is VoiceAssistantState.Recording) DesignTokens.Colors.StatusRecording
                                    else if (voiceState is VoiceAssistantState.Processing) DesignTokens.PurpleAccent
                                    else DesignTokens.Colors.StatusReady,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (voiceState) {
                                is VoiceAssistantState.Recording -> "Recording"
                                is VoiceAssistantState.Processing -> "Thinking"
                                else -> "Ready"
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
            Spacer(modifier = Modifier.height(8.dp))
            HeroMicOrb(
                state = heroOrbState,
                onClick = {
                    if (!hasMicPermission) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.toggleRecording()
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
                        if (lastTranscription.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Send to PC",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.ElectricBlue,
                                    modifier = Modifier.clickable {
                                        viewModel.sendToPcManual(lastTranscription)
                                    }
                                )
                                Text(
                                    text = "Copy",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.PrimaryLightBlue,
                                    modifier = Modifier.clickable {
                                        copyToClipboard(lastTranscription)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (lastTranscription.isNotBlank()) {
                        Text(
                            text = lastTranscription,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = DesignTokens.Colors.TextPrimary,
                            fontWeight = FontWeight.Normal
                        )
                    } else {
                        Text(
                            text = "Your voice transcription will appear here in real time. Tap the mic above or the floating voice assistant below to speak.",
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

        // Meeting Studio Launcher Card
        item {
            val isMeetingRecording by MeetingRecordingService.isRecording.collectAsState()
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DesignTokens.Colors.CardSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isMeetingRecording) DesignTokens.Colors.StatusRecording.copy(alpha = 0.6f) else DesignTokens.Colors.BorderSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onNavigate(ScreenDestination.MeetingMode)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isMeetingRecording) DesignTokens.Colors.StatusRecording.copy(alpha = 0.2f) else DesignTokens.PurpleAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isMeetingRecording) Icons.Default.GraphicEq else Icons.Default.Groups,
                                    contentDescription = "Meeting Studio",
                                    tint = if (isMeetingRecording) DesignTokens.Colors.StatusRecording else DesignTokens.PurpleAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Meeting Studio",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                if (isMeetingRecording) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(DesignTokens.Colors.StatusRecording, CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isMeetingRecording) "Recording live minutes & AI actions..." else "Autonomous minutes, decisions & PC sync",
                                fontSize = 12.sp,
                                color = if (isMeetingRecording) DesignTokens.Colors.StatusRecording else DesignTokens.Colors.TextSubtle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Meeting Studio",
                        tint = DesignTokens.Colors.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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
                                "raw", "verbatim" -> "Verbatim"
                                "formal_document" -> "Executive"
                                "chat" -> "Casual"
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
                                Toast.makeText(context, if (isUseForPc) "PC Typing Enabled" else "PC Typing Paused", Toast.LENGTH_SHORT).show()
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
                            text = "No transcriptions yet. Tap the mic above or in the bottom bar to speak!",
                            fontSize = 13.sp,
                            color = DesignTokens.Colors.TextSubtle
                        )
                    }
                }
            }
        } else {
            items(displayItems, key = { it.id }) { item ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { copyToClipboard(item.text) }
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
                            text = "Universal Voice Assistant",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DesignTokens.Colors.TextPrimary
                        )
                        Text(
                            text = "Tap the center mic in the bottom bar from any screen to dictate anytime!",
                            fontSize = 11.sp,
                            color = DesignTokens.Colors.TextMuted,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
