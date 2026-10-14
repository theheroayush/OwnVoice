package com.example.ownvoice.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
    PROCESSING,
    SUCCESS
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
        isSuccess = state == MicOrbState.SUCCESS,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun HeroMicOrb(
    isRecording: Boolean,
    isProcessing: Boolean,
    isSuccess: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MicPulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 1.25f else 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 750 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isRecording) 0.5f else 0.2f,
        targetValue = if (isRecording) 0.9f else 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 750 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(175.dp)
    ) {
        // 1. Outermost ambient glow ring
        Box(
            modifier = Modifier
                .size(170.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    when {
                        isSuccess -> DesignTokens.StatusGreen.copy(alpha = 0.35f)
                        isRecording -> DesignTokens.RedAccent.copy(alpha = pulseAlpha * 0.45f)
                        else -> DesignTokens.ElectricBlue.copy(alpha = pulseAlpha * 0.30f)
                    }
                )
        )

        // 2. Secondary concentric acoustic ripple ring
        Box(
            modifier = Modifier
                .size(142.dp)
                .scale(if (isRecording) 1.0f + ((pulseScale - 1.0f) * 0.6f) else 1.0f)
                .clip(CircleShape)
                .background(
                    when {
                        isSuccess -> DesignTokens.StatusGreen.copy(alpha = 0.25f)
                        isRecording -> DesignTokens.RedAccent.copy(alpha = 0.28f)
                        else -> DesignTokens.PrimaryBlue.copy(alpha = 0.22f)
                    }
                )
        )

        // 3. Inner halo ring
        Box(
            modifier = Modifier
                .size(118.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSuccess -> DesignTokens.StatusGreen.copy(alpha = 0.20f)
                        isRecording -> DesignTokens.AccentSky.copy(alpha = 0.25f)
                        else -> DesignTokens.AccentSky.copy(alpha = 0.15f)
                    }
                )
        )

        // 4. Center Hero Orb
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSuccess -> DesignTokens.StatusGreen
                        isRecording -> DesignTokens.RedAccent
                        else -> DesignTokens.PrimaryBlue
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        ) {
            when {
                isProcessing -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(38.dp),
                        strokeWidth = 3.dp
                    )
                }
                isSuccess -> {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
                isRecording -> {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop Recording",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Start Recording",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }
    }
}
