package com.example.ownvoice.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ownvoice.OwnVoiceApplication
import com.example.ownvoice.theme.DesignTokens

@Composable
fun BehaviorSettingsScreen(
    onBack: () -> Unit,
    onNavigateToCustomCommands: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as OwnVoiceApplication

    var autoCorrection by remember { mutableStateOf(app.secureConfig.isSelfCorrectionEnabled) }
    var contextAwareness by remember { mutableStateOf(app.secureConfig.isAutoContextToneEnabled) }
    var spokenSelfCorrection by remember { mutableStateOf(app.secureConfig.isSelfCorrectionEnabled) }

    var audioFeedback by remember { mutableStateOf(app.secureConfig.isSoundEffectsEnabled) }
    var hapticFeedback by remember { mutableStateOf(app.secureConfig.isHapticFeedbackEnabled) }

    var silenceDuration by remember { mutableFloatStateOf(app.secureConfig.silenceDetectionSeconds) }
    var continuousListening by remember { mutableStateOf(app.secureConfig.isContinuousListening) }
    var useVoiceCommands by remember { mutableStateOf(app.secureConfig.isVoiceCommandsEnabled) }

    var showSilenceDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DesignTokens.Colors.BackgroundDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onBack)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DesignTokens.ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Settings",
                        color = DesignTokens.ElectricBlue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "OwnVoice",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.Colors.TextPrimary
                    )
                    Text(
                        text = "Voice Behavior",
                        fontSize = 10.sp,
                        color = DesignTokens.Colors.TextSubtle
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Behavior Settings",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Fine-tune how OwnVoice listens, auto-corrects, and detects speech pauses.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = DesignTokens.Colors.TextMuted
                )
            }

            // Group 1: TRANSCRIPTION & CORRECTION
            item {
                Text(
                    text = "TRANSCRIPTION & CORRECTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextSubtle,
                    letterSpacing = 0.8.sp
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Auto Correction
                        SwitchRow(
                            title = "Auto Correction",
                            subtitle = "Automatically fix grammar, punctuation and minor slip-ups.",
                            icon = Icons.Default.AutoAwesome,
                            iconBg = Color(0xFF0F766E).copy(alpha = 0.5f),
                            iconTint = Color(0xFF2DD4BF),
                            checked = autoCorrection,
                            onCheckedChange = {
                                autoCorrection = it
                                app.secureConfig.isSelfCorrectionEnabled = it
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DesignTokens.Colors.BorderSubtle, thickness = 0.5.dp)

                        // Context Awareness
                        SwitchRow(
                            title = "Context Awareness",
                            subtitle = "Understands app context (Slack vs Docs vs Email) to select appropriate formatting.",
                            icon = Icons.Default.Description,
                            iconBg = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                            iconTint = DesignTokens.ElectricBlue,
                            checked = contextAwareness,
                            onCheckedChange = {
                                contextAwareness = it
                                app.secureConfig.isAutoContextToneEnabled = it
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DesignTokens.Colors.BorderSubtle, thickness = 0.5.dp)

                        // Spoken Self-Correction
                        SwitchRow(
                            title = "Spoken Self-Correction",
                            subtitle = "Say \"actually\" or \"I mean\" mid-sentence to output only the final thought.",
                            icon = Icons.Default.GraphicEq,
                            iconBg = Color(0xFF581C87).copy(alpha = 0.5f),
                            iconTint = DesignTokens.PurpleAccent,
                            checked = spokenSelfCorrection,
                            onCheckedChange = {
                                spokenSelfCorrection = it
                                app.secureConfig.isSelfCorrectionEnabled = it
                            }
                        )
                    }
                }
            }

            // Group 2: AUDIO & HAPTIC FEEDBACK
            item {
                Text(
                    text = "FEEDBACK & HAPTICS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextSubtle,
                    letterSpacing = 0.8.sp
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Audio Feedback
                        SwitchRow(
                            title = "Audio Feedback",
                            subtitle = "Play subtle chime when dictation starts and stops.",
                            icon = Icons.Default.VolumeUp,
                            iconBg = Color(0xFFC2410C).copy(alpha = 0.5f),
                            iconTint = Color(0xFFFB923C),
                            checked = audioFeedback,
                            onCheckedChange = {
                                audioFeedback = it
                                app.secureConfig.isSoundEffectsEnabled = it
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DesignTokens.Colors.BorderSubtle, thickness = 0.5.dp)

                        // Haptic Feedback
                        SwitchRow(
                            title = "Haptic Ticks",
                            subtitle = "Provide tactile vibration feedback on button presses and recording state.",
                            icon = Icons.Default.Vibration,
                            iconBg = Color(0xFFBE185D).copy(alpha = 0.5f),
                            iconTint = Color(0xFFF472B6),
                            checked = hapticFeedback,
                            onCheckedChange = {
                                hapticFeedback = it
                                app.secureConfig.isHapticFeedbackEnabled = it
                            }
                        )
                    }
                }
            }

            // Group 3: LISTENING BEHAVIOR & VAD
            item {
                Text(
                    text = "LISTENING & PAUSE DETECTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextSubtle,
                    letterSpacing = 0.8.sp
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Silence Detection
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSilenceDialog = true }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF6B21A8).copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Silence",
                                    tint = DesignTokens.PurpleAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Silence Auto-Stop (VAD)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Stops listening automatically after pause",
                                    fontSize = 11.sp,
                                    color = DesignTokens.Colors.TextMuted
                                )
                            }
                            Text(
                                text = "${silenceDuration}s",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DesignTokens.ElectricBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Edit",
                                tint = DesignTokens.Colors.TextSubtle,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DesignTokens.Colors.BorderSubtle, thickness = 0.5.dp)

                        // Continuous Listening
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    continuousListening = !continuousListening
                                    app.secureConfig.isContinuousListening = continuousListening
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0D9488).copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Continuous",
                                    tint = Color(0xFF2DD4BF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Continuous Dictation",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Allows prolonged uninterrupted speaking",
                                    fontSize = 11.sp,
                                    color = DesignTokens.Colors.TextMuted
                                )
                            }
                            Text(
                                text = if (continuousListening) "ON" else "OFF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (continuousListening) DesignTokens.Colors.StatusReady else DesignTokens.Colors.TextMuted
                            )
                        }
                    }
                }
            }

            // Group 4: VOICE COMMANDS & CUSTOM SHORTCUTS
            item {
                Text(
                    text = "VOICE COMMANDS & SHORTCUTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextSubtle,
                    letterSpacing = 0.8.sp
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Use Voice Commands
                        SwitchRow(
                            title = "Punctuation Voice Commands",
                            subtitle = "Converts spoken \"new line\", \"period\", \"comma\", \"question mark\" to symbols.",
                            icon = Icons.Default.Tag,
                            iconBg = Color(0xFFB45309).copy(alpha = 0.5f),
                            iconTint = DesignTokens.AmberAccent,
                            checked = useVoiceCommands,
                            onCheckedChange = {
                                useVoiceCommands = it
                                app.secureConfig.isVoiceCommandsEnabled = it
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DesignTokens.Colors.BorderSubtle, thickness = 0.5.dp)

                        // Custom Commands / Snippets
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToCustomCommands() }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E293B))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = "Shortcuts",
                                    tint = DesignTokens.Colors.IconSquircleSnippets,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Spoken Snippets & Macros",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Manage triggers like \"my email\" or \"calendar link\"",
                                    fontSize = 11.sp,
                                    color = DesignTokens.Colors.TextMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Navigate",
                                tint = DesignTokens.Colors.TextSubtle,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showSilenceDialog) {
        val durations = listOf(1.0f, 1.5f, 2.0f, 2.5f, 3.0f)
        AlertDialog(
            onDismissRequest = { showSilenceDialog = false },
            title = { Text("Silence Auto-Stop Duration", color = DesignTokens.Colors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("How long to wait after you stop speaking before auto-stopping:", color = DesignTokens.Colors.TextMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    durations.forEach { d ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    silenceDuration = d
                                    app.secureConfig.silenceDetectionSeconds = d
                                    showSilenceDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = silenceDuration == d,
                                onClick = {
                                    silenceDuration = d
                                    app.secureConfig.silenceDetectionSeconds = d
                                    showSilenceDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = DesignTokens.Colors.PrimaryBlue)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("$d seconds ${if (d == 1.5f) "(Recommended)" else ""}", color = DesignTokens.Colors.TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSilenceDialog = false }) {
                    Text("Cancel", color = DesignTokens.Colors.TextMuted)
                }
            },
            containerColor = DesignTokens.Colors.CardSurface
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DesignTokens.Colors.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = DesignTokens.Colors.TextMuted,
                lineHeight = 15.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DesignTokens.Colors.PrimaryBlue,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}
