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
    secondary = DesignTokens.ElectricBlue,
    tertiary = DesignTokens.CyanAccent,
    background = DesignTokens.ObsidianBg,
    surface = DesignTokens.CardBg,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = DesignTokens.TextPrimary,
    onSurface = DesignTokens.TextPrimary,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = DesignTokens.PrimaryBlue,
    secondary = DesignTokens.ElectricBlue,
    tertiary = DesignTokens.CyanAccent,
    background = DesignTokens.ObsidianBg,
    surface = DesignTokens.CardBg,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = DesignTokens.TextPrimary,
    onSurface = DesignTokens.TextPrimary,
  )

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
