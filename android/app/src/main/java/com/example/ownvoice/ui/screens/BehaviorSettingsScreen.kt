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
                        text = "Your voice, everywhere",
                        fontSize = 10.sp,
                        color = DesignTokens.Colors.TextSubtle
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DesignTokens.Colors.BackgroundDark)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        app.secureConfig.isSelfCorrectionEnabled = autoCorrection
                        app.secureConfig.isAutoContextToneEnabled = contextAwareness
                        app.secureConfig.isSoundEffectsEnabled = audioFeedback
                        app.secureConfig.isHapticFeedbackEnabled = hapticFeedback
                        app.secureConfig.silenceDetectionSeconds = silenceDuration
                        app.secureConfig.isContinuousListening = continuousListening
                        app.secureConfig.isVoiceCommandsEnabled = useVoiceCommands
                        Toast.makeText(context, "Behavior settings saved", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.PrimaryBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Save Changes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = DesignTokens.Colors.TextSubtle,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "These settings will apply immediately.",
                        fontSize = 11.sp,
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
                    text = "Fine-tune how OwnVoice listens, understands and responds to your voice.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = DesignTokens.Colors.TextMuted
                )
            }

            // Group 1: TRANSCRIPTION
            item {
                Text(
                    text = "TRANSCRIPTION",
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
                            subtitle = "Automatically fix grammar, punctuation and minor mistakes.",
                            icon = Icons.Default.AutoAwesome,
                            iconBg = Color(0xFF0F766E).copy(alpha = 0.5f),
                            iconTint = Color(0xFF2DD4BF),
                            checked = autoCorrection,
                            onCheckedChange = { autoCorrection = it }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DesignTokens.Colors.BorderSubtle, thickness = 0.5.dp)

                        // Context Awareness
                        SwitchRow(
                            title = "Context Awareness",
                            subtitle = "Understands context, intent and previous conversation to give better results.",
                            icon = Icons.Default.Description,
                            iconBg = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                            iconTint = DesignTokens.ElectricBlue,
                            checked = contextAwareness,
                            onCheckedChange = { contextAwareness = it }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DesignTokens.Colors.BorderSubtle, thickness = 0.5.dp)

                        // Spoken Self-Correction
                        SwitchRow(
                            title = "Spoken Self-Correction",
                            subtitle = "Let you naturally say \"actually\" or \"correction\" to modify the last part.",
                            icon = Icons.Default.GraphicEq,
                            iconBg = Color(0xFF581C87).copy(alpha = 0.5f),
                            iconTint = DesignTokens.PurpleAccent,
                            checked = spokenSelfCorrection,
                            onCheckedChange = { spokenSelfCorrection = it }
                        )
                    }
                }
            }

            // Group 2: AUDIO FEEDBACK
            item {
                Text(
                    text = "AUDIO FEEDBACK",
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
                            subtitle = "Play subtle sounds for start, stop and completion.",
                            icon = Icons.Default.VolumeUp,
                            iconBg = Color(0xFFC2410C).copy(alpha = 0.5f),
                            iconTint = Color(0xFFFB923C),
                            checked = audioFeedback,
                            onCheckedChange = { audioFeedback = it }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DesignTokens.Colors.BorderSubtle, thickness = 0.5.dp)

                        // Haptic Feedback
                        SwitchRow(
                            title = "Haptic Feedback",
                            subtitle = "Vibrate on key actions for a more tactile experience.",
                            icon = Icons.Default.Vibration,
                            iconBg = Color(0xFFBE185D).copy(alpha = 0.5f),
                            iconTint = Color(0xFFF472B6),
                            checked = hapticFeedback,
                            onCheckedChange = { hapticFeedback = it }
                        )
                    }
                }
            }

            // Group 3: LISTENING BEHAVIOR
            item {
                Text(
                    text = "LISTENING BEHAVIOR",
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
                                    text = "Silence Detection",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Automatically stop listening after a pause.",
                                    fontSize = 11.sp,
                                    color = DesignTokens.Colors.TextMuted
                                )
                            }
                            Text(
                                text = "${silenceDuration} seconds",
                                fontSize = 12.sp,
                                color = DesignTokens.Colors.TextMuted
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Navigate",
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
                                .clickable { continuousListening = !continuousListening }
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
                                    text = "Continuous Listening",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Keep listening for longer conversations.",
                                    fontSize = 11.sp,
                                    color = DesignTokens.Colors.TextMuted
                                )
                            }
                            Text(
                                text = if (continuousListening) "On" else "Off",
                                fontSize = 12.sp,
                                color = if (continuousListening) DesignTokens.ElectricBlue else DesignTokens.Colors.TextMuted
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Navigate",
                                tint = DesignTokens.Colors.TextSubtle,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Group 4: SMART FEATURES
            item {
                Text(
                    text = "SMART FEATURES",
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
                            title = "Use Voice Commands",
                            subtitle = "Control OwnVoice with voice commands (e.g. \"new line\", \"comma\", \"open\").",
                            icon = Icons.Default.Tag,
                            iconBg = Color(0xFFB45309).copy(alpha = 0.5f),
                            iconTint = DesignTokens.AmberAccent,
                            checked = useVoiceCommands,
                            onCheckedChange = { useVoiceCommands = it }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DesignTokens.Colors.BorderSubtle, thickness = 0.5.dp)

                        // Custom Commands
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
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "Commands",
                                    tint = DesignTokens.Colors.TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Custom Commands",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Create your own voice shortcuts.",
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
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showSilenceDialog) {
        val durations = listOf(1.0f, 1.5f, 2.0f, 2.5f, 3.0f)
        AlertDialog(
            onDismissRequest = { showSilenceDialog = false },
            title = { Text("Silence Detection Duration", color = DesignTokens.Colors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    durations.forEach { d ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    silenceDuration = d
                                    showSilenceDialog = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = silenceDuration == d,
                                onClick = {
                                    silenceDuration = d
                                    showSilenceDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = DesignTokens.Colors.PrimaryBlue)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("$d seconds", color = DesignTokens.Colors.TextPrimary, fontSize = 14.sp)
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
