package com.example.ownvoice.ime.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class KeyboardState {
    IDLE,
    RECORDING,
    PROCESSING,
    ERROR
}

private fun formatToneLabel(tone: String): String {
    return when (tone.lowercase()) {
        "smart_flow" -> "Smart Flow"
        "search" -> "Search Query"
        "translate_hindi" -> "Hindi Translation"
        "translate_english" -> "English Translation"
        "chat" -> "Chat / Messaging"
        "formal_email" -> "Formal Email"
        "formal_document" -> "Document"
        "code" -> "Code & Terminal"
        "bullet_notes" -> "Bullet Notes"
        "verbatim" -> "Verbatim"
        else -> tone.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}

@Composable
fun KeyboardView(
    state: KeyboardState,
    amplitude: Float,
    statusMessage: String,
    activeTone: String,
    snippets: Map<String, String>,
    onToggleRecording: () -> Unit,
    onToneCycle: () -> Unit = {},
    onToneSelect: (String) -> Unit = {},
    onClearClick: () -> Unit = {},
    onNewLineClick: () -> Unit = {},
    onSnippetClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onEnterClick: () -> Unit,
    onSpaceClick: () -> Unit,
    onSwitchKeyboardClick: () -> Unit
) {
    val darkBackground = Color(0xFF131316)
    val cardBackground = Color(0xFF1E1E24)
    val chipBorder = Color(0xFF2C2C36)
    val accentBlue = Color(0xFF0A84FF)
    val accentRed = Color(0xFFFF3B30)
    val textPrimary = Color(0xFFF2F2F7)
    val textSecondary = Color(0xFF8E8E93)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 255.dp, max = 285.dp)
            .background(darkBackground)
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Section: Pre-Filled Action Chips & Tone Selector (Horizontal Scroll Row)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tone Mode Pill (Tap to cycle tones)
            item {
                Surface(
                    color = if (activeTone != "smart_flow") accentBlue.copy(alpha = 0.2f) else cardBackground,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (activeTone != "smart_flow") accentBlue else chipBorder),
                    modifier = Modifier.clickable { onToneCycle() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⚡ ${formatToneLabel(activeTone)}",
                            color = if (activeTone != "smart_flow") accentBlue else textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Quick Action: 🔍 Search Query Mode
            item {
                val isSearch = activeTone == "search"
                Surface(
                    color = if (isSearch) accentBlue.copy(alpha = 0.25f) else cardBackground,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isSearch) accentBlue else chipBorder),
                    modifier = Modifier.clickable {
                        onToneSelect(if (isSearch) "smart_flow" else "search")
                    }
                ) {
                    Text(
                        text = "🔍 Search",
                        color = if (isSearch) accentBlue else textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Quick Action: 🌐 Hindi Translation Mode
            item {
                val isHindi = activeTone == "translate_hindi"
                Surface(
                    color = if (isHindi) accentBlue.copy(alpha = 0.25f) else cardBackground,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isHindi) accentBlue else chipBorder),
                    modifier = Modifier.clickable {
                        onToneSelect(if (isHindi) "smart_flow" else "translate_hindi")
                    }
                ) {
                    Text(
                        text = "🌐 Hindi",
                        color = if (isHindi) accentBlue else textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Quick Action: ↵ New Line
            item {
                Surface(
                    color = cardBackground,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, chipBorder),
                    modifier = Modifier.clickable { onNewLineClick() }
                ) {
                    Text(
                        text = "↵ New Line",
                        color = textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Quick Action: ✕ Clear
            item {
                Surface(
                    color = cardBackground,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, chipBorder),
                    modifier = Modifier.clickable { onClearClick() }
                ) {
                    Text(
                        text = "✕ Clear",
                        color = textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // User-configured snippets (guaranteed single line, never wrapping vertically)
            items(snippets.keys.toList()) { trigger ->
                Surface(
                    color = cardBackground,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, chipBorder),
                    modifier = Modifier.clickable { onSnippetClick(trigger) }
                ) {
                    Text(
                        text = trigger,
                        color = textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 2. Center Voice Engine Hub (Waveform / Status + Huge Centered Circular Mic Button)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Status & Dynamic Audio Waveform
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    KeyboardState.RECORDING -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            for (i in 0 until 9) {
                                val factor = when (i) {
                                    0, 8 -> 0.35f
                                    1, 7 -> 0.55f
                                    2, 6 -> 0.75f
                                    3, 5 -> 0.90f
                                    else -> 1.0f
                                }
                                val barHeight by animateDpAsState(
                                    targetValue = (5f + 19f * amplitude * factor).coerceIn(4f, 24f).dp,
                                    animationSpec = tween(durationMillis = 60),
                                    label = "wave_$i"
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .width(3.5.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(accentRed)
                                )
                            }
                        }
                    }
                    KeyboardState.PROCESSING -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = accentBlue,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Transcribing with Gemini…",
                                color = textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    KeyboardState.ERROR -> {
                        Text(
                            text = statusMessage.ifBlank { "Microphone Error" },
                            color = accentRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    KeyboardState.IDLE -> {
                        Text(
                            text = if (statusMessage.isNotBlank()) statusMessage else "Tap microphone to dictate",
                            color = textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Prominent 76dp Centered Circular Mic Button
            val infiniteTransition = rememberInfiniteTransition(label = "recordingPulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseAnim"
            )

            val currentScale = if (state == KeyboardState.RECORDING) {
                pulseScale + (amplitude * 0.2f).coerceAtMost(0.2f)
            } else {
                1.0f
            }

            Box(
                modifier = Modifier.size(84.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing glowing ring when actively recording
                if (state == KeyboardState.RECORDING) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(currentScale)
                            .clip(CircleShape)
                            .background(accentRed.copy(alpha = 0.25f))
                    )
                }

                // Main Circular Button
                Surface(
                    shape = CircleShape,
                    color = when (state) {
                        KeyboardState.RECORDING -> accentRed
                        KeyboardState.PROCESSING -> accentBlue.copy(alpha = 0.85f)
                        else -> accentBlue
                    },
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(72.dp)
                        .clickable { onToggleRecording() }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (state) {
                            KeyboardState.RECORDING -> {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Dictation",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            KeyboardState.PROCESSING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(30.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Start Dictation",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle state hint
            Text(
                text = when (state) {
                    KeyboardState.RECORDING -> "Tap to send"
                    KeyboardState.PROCESSING -> "Processing..."
                    else -> "Tap to speak"
                },
                color = if (state == KeyboardState.RECORDING) accentRed else textSecondary,
                fontSize = 11.sp,
                fontWeight = if (state == KeyboardState.RECORDING) FontWeight.Medium else FontWeight.Normal
            )
        }

        // 3. Bottom Ergonomic Utility Bar (Switch Keyboard, Space Bar, Backspace, Enter)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Switch Keyboard (Globe Icon)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cardBackground,
                modifier = Modifier
                    .size(46.dp)
                    .clickable { onSwitchKeyboardClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Switch Keyboard",
                        tint = textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Wide Ergonomic Space Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cardBackground,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clickable { onSpaceClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Space",
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Backspace Button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cardBackground,
                modifier = Modifier
                    .size(46.dp)
                    .clickable { onBackspaceClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Enter / Action Button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentBlue,
                modifier = Modifier
                    .size(46.dp)
                    .clickable { onEnterClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "↵",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
