package com.example.ownvoice.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ownvoice.OwnVoiceApplication
import com.example.ownvoice.theme.DesignTokens
import com.example.ownvoice.ui.components.BottomNavBar
import com.example.ownvoice.ui.components.SettingsListItem
import com.example.ownvoice.ui.navigation.ScreenDestination

@Composable
fun SettingsScreen(
    onNavigate: (ScreenDestination) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OwnVoiceApplication

    var searchQuery by remember { mutableStateOf("") }
    val defaultIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
    val isOwnVoiceActiveIme = defaultIme.contains("ownvoice", ignoreCase = true)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Settings",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextPrimary
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search settings...",
                            fontSize = 13.sp,
                            color = DesignTokens.Colors.TextSubtle
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = DesignTokens.Colors.TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = DesignTokens.Colors.TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DesignTokens.Colors.PrimaryBlue,
                        unfocusedBorderColor = DesignTokens.Colors.BorderSubtle,
                        focusedContainerColor = DesignTokens.Colors.CardSurface,
                        unfocusedContainerColor = DesignTokens.Colors.CardSurface,
                        focusedTextColor = DesignTokens.Colors.TextPrimary,
                        unfocusedTextColor = DesignTokens.Colors.TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val q = searchQuery.trim().lowercase()

            // Group 1: VOICE & BEHAVIOR
            val showVoice = q.isEmpty() || "voice intelligence language model style sensitivity".contains(q)
            val showBehavior = q.isEmpty() || "behavior correction audio haptic silence commands".contains(q)
            val showSnippets = q.isEmpty() || "snippets expansion shortcuts macros triggers".contains(q)

            if (showVoice || showBehavior || showSnippets) {
                item {
                    Text(
                        text = "VOICE & BEHAVIOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.Colors.TextSubtle,
                        letterSpacing = 0.8.sp
                    )
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            if (showVoice) {
                                SettingsListItem(
                                    title = "Voice Settings",
                                    subtitle = "Language, AI model, style & mic sensitivity",
                                    icon = Icons.Default.GraphicEq,
                                    iconTint = DesignTokens.Colors.IconSquircleVoice,
                                    onClick = { onNavigate(ScreenDestination.VoiceSettings) }
                                )
                            }
                            if (showVoice && showBehavior) {
                                HorizontalDivider(color = DesignTokens.Colors.BorderSubtle, thickness = 0.5.dp)
                            }
                            if (showBehavior) {
                                SettingsListItem(
                                    title = "Behavior Settings",
                                    subtitle = "Auto-correction, audio feedback & silence detection",
                                    icon = Icons.Default.Tune,
                                    iconTint = DesignTokens.AmberAccent,
                                    onClick = { onNavigate(ScreenDestination.BehaviorSettings) }
                                )
                            }
                            if ((showVoice || showBehavior) && showSnippets) {
                                HorizontalDivider(color = DesignTokens.Colors.BorderSubtle, thickness = 0.5.dp)
                            }
                            if (showSnippets) {
                                SettingsListItem(
                                    title = "Spoken Snippets",
                                    subtitle = "Text expansion macros & triggers",
                                    icon = Icons.Default.FlashOn,
                                    iconTint = DesignTokens.Colors.IconSquircleSnippets,
                                    onClick = { onNavigate(ScreenDestination.SnippetsManager) }
                                )
                            }
                        }
                    }
                }
            }

            // Group 2: CONNECTED DEVICES & INPUT
            val showDevices = q.isEmpty() || "devices connected computer link pc macbook iphone ipad laptop wifi qr".contains(q)
            val showKeyboard = q.isEmpty() || "keyboard ime input method bubble floating accessibility".contains(q)

            if (showDevices || showKeyboard) {
                item {
                    Text(
                        text = "CONNECTED DEVICES & INPUT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.Colors.TextSubtle,
                        letterSpacing = 0.8.sp
                    )
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            if (showDevices) {
                                SettingsListItem(
                                    title = "PC connection",
                                    subtitle = "Set up or manage the OwnVoice desktop connection",
                                    icon = Icons.Default.Laptop,
                                    iconTint = DesignTokens.Colors.IconSquircleComputer,
                                    onClick = { onNavigate(ScreenDestination.ConnectedDevices) }
                                )
                            }
                            if (showDevices && showKeyboard) {
                                HorizontalDivider(color = DesignTokens.Colors.BorderSubtle, thickness = 0.5.dp)
                            }
                            if (showKeyboard) {
                                SettingsListItem(
                                    title = "Keyboard & Floating Bubble",
                                    subtitle = if (isOwnVoiceActiveIme) "Active Keyboard • Overlay active" else "Tap to configure input methods",
                                    icon = Icons.Default.Keyboard,
                                    iconTint = DesignTokens.Colors.IconSquircleKeyboard,
                                    onClick = {
                                        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Group 3: HISTORY & BACKUP
            val showHistory = q.isEmpty() || "history transcriptions logs data export clear".contains(q)
            val showCloud = q.isEmpty() || "cloud sync gist backup restore github".contains(q)

            if (showHistory || showCloud) {
                item {
                    Text(
                        text = "HISTORY & BACKUP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.Colors.TextSubtle,
                        letterSpacing = 0.8.sp
                    )
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            if (showHistory) {
                                SettingsListItem(
                                    title = "Recent Transcriptions",
                                    subtitle = "Searchable history, audio playback & tags",
                                    icon = Icons.Default.History,
                                    iconTint = DesignTokens.Colors.IconSquircleHistory,
                                    onClick = { onNavigate(ScreenDestination.RecentTranscriptions) }
                                )
                            }
                            if (showHistory && showCloud) {
                                HorizontalDivider(color = DesignTokens.Colors.BorderSubtle, thickness = 0.5.dp)
                            }
                            if (showCloud) {
                                SettingsListItem(
                                    title = "GitHub Gist Cloud Sync",
                                    subtitle = "Sync vocabulary & snippets across devices",
                                    icon = Icons.Default.CloudSync,
                                    iconTint = DesignTokens.Colors.IconSquircleSync,
                                    onClick = { onNavigate(ScreenDestination.HistoryPrivacy) }
                                )
                            }
                        }
                    }
                }
            }

            // Group 4: SYSTEM & ENGINE
            val showApi = q.isEmpty() || "api key gemini flash token model".contains(q)
            val showAbout = q.isEmpty() || "about version info updates license".contains(q)

            if (showApi || showAbout) {
                item {
                    Text(
                        text = "SYSTEM & ENGINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.Colors.TextSubtle,
                        letterSpacing = 0.8.sp
                    )
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            if (showApi) {
                                SettingsListItem(
                                    title = "API & Model Configuration",
                                    subtitle = "Gemini 3.6 Flash • Custom Google API Key",
                                    icon = Icons.Default.Key,
                                    iconTint = DesignTokens.Colors.IconSquircleKey,
                                    onClick = { onNavigate(ScreenDestination.AdvancedSettings) }
                                )
                            }
                            if (showApi && showAbout) {
                                HorizontalDivider(color = DesignTokens.Colors.BorderSubtle, thickness = 0.5.dp)
                            }
                            if (showAbout) {
                                SettingsListItem(
                                    title = "App Version",
                                    subtitle = "OwnVoice v2.6.0 (Build 42) • Production",
                                    icon = Icons.Default.Info,
                                    iconTint = DesignTokens.Colors.TextMuted,
                                    trailing = {}
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
}
