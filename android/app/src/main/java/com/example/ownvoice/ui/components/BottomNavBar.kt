package com.example.ownvoice.ui.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    showCenterMic: Boolean = true,
    onCenterMicClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeTab = if (currentDestination is ScreenDestination.SettingsHub ||
        currentDestination is ScreenDestination.ConnectedDevices ||
        currentDestination is ScreenDestination.VoiceSettings ||
        currentDestination is ScreenDestination.BehaviorSettings ||
        currentDestination is ScreenDestination.RecentTranscriptions) {
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
        showCenterMic = showCenterMic,
        onCenterMicClick = onCenterMicClick ?: { onNavigate(ScreenDestination.Home) },
        modifier = modifier
    )
}

@Composable
fun BottomNavBar(
    activeTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    showCenterMic: Boolean = true,
    onCenterMicClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DesignTokens.ObsidianBg)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
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

        // Center Floating Glowing Hero Mic Orb
        if (showCenterMic) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                DesignTokens.ElectricBlue.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    DesignTokens.PrimaryBlue,
                                    Color(0xFF1D4ED8)
                                )
                            )
                        )
                        .border(2.dp, DesignTokens.ElectricBlue.copy(alpha = 0.7f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onCenterMicClick?.invoke() }
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Speak",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
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
