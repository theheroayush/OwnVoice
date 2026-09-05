package com.example.ownvoice.ime.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class KeyboardState {
    IDLE,
    RECORDING,
    PROCESSING,
    ERROR
}

@Composable
fun KeyboardView(
    state: KeyboardState,
    amplitude: Float,
    statusMessage: String,
    activeTone: String,
    snippets: Map<String, String>,
    onToggleRecording: () -> Unit,
    onSnippetClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onEnterClick: () -> Unit,
    onSpaceClick: () -> Unit,
    onSwitchKeyboardClick: () -> Unit
) {
    val darkBackground = Color(0xFF121214)
    val cardBackground = Color(0xFF1E1E24)
    val accentRed = Color(0xFFFF453A)
    val accentBlue = Color(0xFF0A84FF)
    val textPrimary = Color(0xFFF2F2F7)
    val textSecondary = Color(0xFF8E8E93)

    val micScale by animateFloatAsState(
        targetValue = if (state == KeyboardState.RECORDING) 1.0f + (amplitude * 0.4f) else 1.0f,
        animationSpec = tween(durationMillis = 80),
        label = "micScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(darkBackground)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Bar: Snippets & Tone Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Tone indicator pill
            Surface(
                color = cardBackground,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = "Tone: ${activeTone.replace('_', ' ').replaceFirstChar { it.uppercase() }}",
                    color = accentBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Quick Snippets Chips
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                snippets.keys.take(3).forEach { trigger ->
                    Surface(
                        color = cardBackground,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable { onSnippetClick(trigger) }
                    ) {
                        Text(
                            text = trigger,
                            color = textSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Switch keyboard button
            IconButton(
                onClick = onSwitchKeyboardClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Switch Keyboard",
                    tint = textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 2. Central Waveform & Status Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cardBackground)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                KeyboardState.IDLE -> {
                    Text(
                        text = "Tap microphone to dictate",
                        color = textSecondary,
                        fontSize = 13.sp
                    )
                }
                KeyboardState.RECORDING -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Live Audio Amplitude Bars
                        for (i in 1..7) {
                            val barHeight = (12.dp + (amplitude * (20 + (i % 3) * 8)).dp).coerceIn(8.dp, 40.dp)
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .width(4.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(accentRed)
                            )
                        }
                    }
                }
                KeyboardState.PROCESSING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = accentBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini thinking…",
                            color = textPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
                KeyboardState.ERROR -> {
                    Text(
                        text = statusMessage.ifBlank { "Microphone Error" },
                        color = accentRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Main Microphone & Typing Utility Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Space button
            Button(
                onClick = onSpaceClick,
                colors = ButtonDefaults.buttonColors(containerColor = cardBackground),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Space", color = textPrimary, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Large Center Mic Button
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .scale(micScale)
                    .clip(CircleShape)
                    .background(if (state == KeyboardState.RECORDING) accentRed else accentBlue)
                    .clickable { onToggleRecording() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state == KeyboardState.RECORDING) Icons.Default.Done else Icons.Default.Mic,
                    contentDescription = "Dictate",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Backspace button
            IconButton(
                onClick = onBackspaceClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBackground)
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Backspace",
                    tint = textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Enter / Done button
            Button(
                onClick = onEnterClick,
                colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("↵", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
