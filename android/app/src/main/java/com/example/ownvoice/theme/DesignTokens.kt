package com.example.ownvoice.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object DesignTokens {
    // Primary Surfaces
    val ObsidianBg = Color(0xFF0B0F17)
    val CardBg = Color(0xFF131926)
    val CardBgElevated = Color(0xFF1E293B)
    val CardBorder = Color(0xFF1E293B)
    val CardBorderSubtle = Color(0xFF26334D)

    // Brand Accents
    val PrimaryBlue = Color(0xFF2563EB)
    val ElectricBlue = Color(0xFF3B82F6)
    val GlowBlue = Color(0xFF007AFF)
    val CyanAccent = Color(0xFF06B6D4)
    val StatusGreen = Color(0xFF10B981)
    val StatusGreenGlow = Color(0xFF059669)
    val PurpleAccent = Color(0xFF8B5CF6)
    val AmberAccent = Color(0xFFF59E0B)
    val RedAccent = Color(0xFFEF4444)

    // Text Tokens
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFE2E8F0)
    val TextMuted = Color(0xFF94A3B8)
    val TextSubtle = Color(0xFF64748B)

    // Colors Sub-Object for unified consumption
    object Colors {
        val BackgroundDark = ObsidianBg
        val CardSurface = CardBg
        val CardSurfaceElevated = CardBgElevated
        val BorderSubtle = CardBorderSubtle
        val PrimaryBlue = DesignTokens.PrimaryBlue
        val PrimaryLightBlue = ElectricBlue
        val StatusReady = StatusGreen
        val StatusRecording = RedAccent
        val TextPrimary = DesignTokens.TextPrimary
        val TextSecondary = DesignTokens.TextSecondary
        val TextMuted = DesignTokens.TextMuted
        val TextSubtle = DesignTokens.TextSubtle
        val IconSquircleVoice = PrimaryBlue
        val IconSquircleSnippets = PurpleAccent
        val IconSquircleComputer = CyanAccent
        val IconSquircleKeyboard = AmberAccent
        val IconSquircleSync = StatusGreen
        val IconSquircleHistory = PurpleAccent
        val IconSquircleKey = AmberAccent
    }

    // Gradients
    val MicGlowBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFF3B82F6).copy(alpha = 0.55f),
            Color(0xFF1D4ED8).copy(alpha = 0.25f),
            Color.Transparent
        )
    )

    val MicButtonBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF3B82F6),
            Color(0xFF1D4ED8),
            Color(0xFF1E40AF)
        )
    )

    val TitleGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFF93C5FD),
            Color(0xFF818CF8)
        )
    )

    val ActionTextGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF818CF8),
            Color(0xFF38BDF8)
        )
    )

    val CardMeshBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF161F2E),
            Color(0xFF101724)
        )
    )

    // Sizing & Spacing
    val RadiusCard = 16.dp
    val RadiusPill = 28.dp
    val RadiusButton = 12.dp
    val RadiusIconSquircle = 10.dp
}
