package com.example.ownvoice.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = DesignTokens.PrimaryBlue,
    onPrimary = DesignTokens.TextPrimary,
    primaryContainer = DesignTokens.KeycapPressed,
    onPrimaryContainer = DesignTokens.TextPrimary,
    secondary = DesignTokens.ElectricBlue,
    onSecondary = DesignTokens.TextPrimary,
    secondaryContainer = DesignTokens.KeycapBg,
    onSecondaryContainer = DesignTokens.TextPrimary,
    tertiary = DesignTokens.AccentSky,
    onTertiary = DesignTokens.ObsidianBg,
    tertiaryContainer = DesignTokens.AccentSky.copy(alpha = 0.2f),
    onTertiaryContainer = DesignTokens.TextPrimary,
    background = DesignTokens.ObsidianBg,
    onBackground = DesignTokens.TextPrimary,
    surface = DesignTokens.CardBg,
    onSurface = DesignTokens.TextPrimary,
    surfaceVariant = DesignTokens.KeycapBg,
    onSurfaceVariant = DesignTokens.TextSecondary,
    outline = DesignTokens.CardBorder,
    outlineVariant = DesignTokens.CardBorderSubtle,
    error = DesignTokens.RecordingPulse,
    onError = Color.White,
    errorContainer = DesignTokens.RecordingPulse.copy(alpha = 0.2f),
    onErrorContainer = DesignTokens.RecordingPulse,
    surfaceTint = DesignTokens.PrimaryBlue,
    inverseSurface = DesignTokens.TextPrimary,
    inverseOnSurface = DesignTokens.ObsidianBg,
  )

private val LightColorScheme = DarkColorScheme


@Composable
fun OwnVoiceTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // For OwnVoice, dynamicColor defaults to false to maintain the Obsidian brand aesthetic
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> DarkColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
