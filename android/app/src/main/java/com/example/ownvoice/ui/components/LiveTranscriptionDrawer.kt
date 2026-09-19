package com.example.ownvoice.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ownvoice.core.VoiceAssistantState
import com.example.ownvoice.theme.DesignTokens

@Composable
fun LiveTranscriptionDrawer(
    visible: Boolean,
    state: VoiceAssistantState,
    amplitude: Float,
    feedbackMessage: String,
    lastTranscription: String,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
    onSendToPc: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color(0xFF111827),
            border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header: Drag handle & close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusDotColor = when (state) {
                            is VoiceAssistantState.Recording -> DesignTokens.Colors.StatusRecording
                            is VoiceAssistantState.Processing -> DesignTokens.PurpleAccent
                            is VoiceAssistantState.Success -> DesignTokens.Colors.StatusReady
                            is VoiceAssistantState.Error -> DesignTokens.AmberAccent
                            is VoiceAssistantState.Idle -> DesignTokens.Colors.TextMuted
                        }

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(statusDotColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (state) {
                                is VoiceAssistantState.Recording -> "Listening..."
                                is VoiceAssistantState.Processing -> "Transcribing with Gemini 3.6 Flash..."
                                is VoiceAssistantState.Success -> "Transcription Complete"
                                is VoiceAssistantState.Error -> "Dictation Notice"
                                is VoiceAssistantState.Idle -> "Voice Assistant Ready"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DesignTokens.Colors.TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = DesignTokens.Colors.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Waveform animation when recording
                if (state is VoiceAssistantState.Recording) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val heights = listOf(
                            0.3f, 0.6f, 0.9f, 1.0f, 0.8f, 0.5f, 0.4f
                        )
                        heights.forEachIndexed { index, scale ->
                            val barHeight = (8.dp + (24.dp * (amplitude * scale))).coerceIn(6.dp, 32.dp)
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .width(4.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(DesignTokens.Colors.StatusRecording)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Transcription or Status Output
                val displayText = when {
                    lastTranscription.isNotBlank() && (state is VoiceAssistantState.Success || state is VoiceAssistantState.Idle) -> lastTranscription
                    feedbackMessage.isNotBlank() -> feedbackMessage
                    else -> "Speak into your microphone. Your transcribed text will appear here."
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F1420),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = displayText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = if (displayText.startsWith("Error")) DesignTokens.Colors.StatusRecording
                               else if (displayText.startsWith("Typed")) DesignTokens.Colors.StatusReady
                               else DesignTokens.Colors.TextPrimary,
                        modifier = Modifier.padding(14.dp),
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action chips
                if (lastTranscription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Copy chip
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DesignTokens.Colors.CardSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onCopy(lastTranscription) }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = DesignTokens.Colors.PrimaryLightBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Copy Text",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.PrimaryLightBlue
                                )
                            }
                        }

                        // Send to PC chip
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DesignTokens.Colors.CardSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSendToPc(lastTranscription) }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Laptop,
                                    contentDescription = "PC",
                                    tint = DesignTokens.ElectricBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Send to PC",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.ElectricBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
