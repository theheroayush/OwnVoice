package com.example.ownvoice.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ownvoice.core.VoiceAssistantState
import com.example.ownvoice.theme.DesignTokens
import com.example.ownvoice.ui.navigation.ScreenDestination

enum class BottomTab {
    HOME,
    SETTINGS
}

@Composable
fun BottomNavBar(
    currentDestination: ScreenDestination,
    onNavigate: (ScreenDestination) -> Unit,
    voiceState: VoiceAssistantState = VoiceAssistantState.Idle,
    amplitude: Float = 0f,
    onCenterMicClick: () -> Unit = {},
    showCenterMic: Boolean = true,
    modifier: Modifier = Modifier
) {
    val activeTab = if (currentDestination is ScreenDestination.SettingsHub ||
        currentDestination is ScreenDestination.ConnectedDevices ||
        currentDestination is ScreenDestination.VoiceSettings ||
        currentDestination is ScreenDestination.BehaviorSettings ||
        currentDestination is ScreenDestination.RecentTranscriptions ||
        currentDestination is ScreenDestination.SnippetsManager ||
        currentDestination is ScreenDestination.HistoryPrivacy ||
        currentDestination is ScreenDestination.AdvancedSettings) {
        BottomTab.SETTINGS
    } else {
        BottomTab.HOME
    }

    BottomNavBar(
        activeTab = activeTab,
        onTabSelected = { tab ->
            when (tab) {
                BottomTab.HOME -> onNavigate(ScreenDestination.Home)
                BottomTab.SETTINGS -> onNavigate(ScreenDestination.SettingsHub)
            }
        },
        voiceState = voiceState,
        amplitude = amplitude,
        showCenterMic = showCenterMic,
        onCenterMicClick = onCenterMicClick,
        modifier = modifier
    )
}

@Composable
fun BottomNavBar(
    activeTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    voiceState: VoiceAssistantState = VoiceAssistantState.Idle,
    amplitude: Float = 0f,
    showCenterMic: Boolean = true,
    onCenterMicClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isRecording = voiceState is VoiceAssistantState.Recording
    val isProcessing = voiceState is VoiceAssistantState.Processing

    // Infinite transition for gentle idle/recording breathing
    val infiniteTransition = rememberInfiniteTransition(label = "micOrbGlow")
    val idleGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleGlowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DesignTokens.ObsidianBg)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating pill container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(DesignTokens.CardBg)
                .border(1.dp, DesignTokens.CardBorderSubtle, RoundedCornerShape(22.dp))
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NavItem(
                icon = Icons.Filled.Home,
                label = "Home",
                isActive = activeTab == BottomTab.HOME,
                onClick = { onTabSelected(BottomTab.HOME) }
            )

            if (showCenterMic) {
                Spacer(modifier = Modifier.width(60.dp))
            }

            NavItem(
                icon = Icons.Filled.Settings,
                label = "Settings",
                isActive = activeTab == BottomTab.SETTINGS,
                onClick = { onTabSelected(BottomTab.SETTINGS) }
            )
        }

        // Center Floating Glowing Hero Voice Assistant Mic Orb
        if (showCenterMic) {
            val outerGlowSize = when {
                isRecording -> (56.dp + (30.dp * amplitude).coerceIn(0.dp, 30.dp))
                isProcessing -> 60.dp
                else -> 54.dp
            }

            val glowColors = when {
                isRecording -> listOf(
                    DesignTokens.Colors.StatusRecording.copy(alpha = 0.6f + amplitude * 0.4f),
                    Color.Transparent
                )
                isProcessing -> listOf(
                    DesignTokens.PurpleAccent.copy(alpha = 0.5f),
                    Color.Transparent
                )
                else -> listOf(
                    DesignTokens.ElectricBlue.copy(alpha = idleGlowAlpha),
                    Color.Transparent
                )
            }

            val buttonGradient = when {
                isRecording -> listOf(
                    DesignTokens.Colors.StatusRecording,
                    Color(0xFFB91C1C)
                )
                isProcessing -> listOf(
                    Color(0xFF7C3AED),
                    Color(0xFF4C1D95)
                )
                else -> listOf(
                    DesignTokens.PrimaryBlue,
                    Color(0xFF1D4ED8)
                )
            }

            val borderColor = when {
                isRecording -> Color(0xFFFCA5A5)
                isProcessing -> Color(0xFFC4B5FD)
                else -> DesignTokens.ElectricBlue.copy(alpha = 0.8f)
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(y = (-10).dp)
                    .size(outerGlowSize)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(colors = glowColors))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(colors = buttonGradient))
                        .border(2.dp, borderColor, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCenterMicClick
                        )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = if (isRecording) "Stop Listening" else "Speak with Voice Assistant",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(DesignTokens.RadiusPill))
            .background(if (isActive) DesignTokens.PrimaryBlue.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) DesignTokens.ElectricBlue else DesignTokens.TextSubtle,
            modifier = Modifier.size(20.dp)
        )
        if (isActive) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DesignTokens.ElectricBlue
            )
        }
    }
}
