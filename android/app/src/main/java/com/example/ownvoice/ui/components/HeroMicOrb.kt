package com.example.ownvoice.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ownvoice.theme.DesignTokens

enum class MicOrbState {
    IDLE,
    LISTENING,
    PROCESSING
}

@Composable
fun HeroMicOrb(
    state: MicOrbState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HeroMicOrb(
        isRecording = state == MicOrbState.LISTENING,
        isProcessing = state == MicOrbState.PROCESSING,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun HeroMicOrb(
    isRecording: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MicPulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 1.22f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 800 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isRecording) 0.5f else 0.2f,
        targetValue = if (isRecording) 0.85f else 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 800 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(170.dp)
    ) {
        // Outer ambient glow ring
        Box(
            modifier = Modifier
                .size(165.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    if (isRecording) {
                        DesignTokens.RedAccent.copy(alpha = pulseAlpha * 0.4f)
                    } else {
                        DesignTokens.ElectricBlue.copy(alpha = pulseAlpha * 0.3f)
                    }
                )
        )

        // Middle concentric ripple ring
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(
                    if (isRecording) {
                        DesignTokens.RedAccent.copy(alpha = 0.2f)
                    } else {
                        DesignTokens.PrimaryBlue.copy(alpha = 0.25f)
                    }
                )
        )

        // Center Hero Orb
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    if (isRecording) {
                        DesignTokens.RedAccent
                    } else {
                        DesignTokens.PrimaryBlue
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(38.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
    }
}
