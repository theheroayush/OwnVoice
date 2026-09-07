package com.example.ownvoice.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ownvoice.OwnVoiceApplication
import com.example.ownvoice.overlay.FloatingBubbleService
import com.example.ownvoice.overlay.OverlayAccessibilityService
import com.example.ownvoice.theme.DesignTokens

@Composable
fun SnippetsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OwnVoiceApplication

    var snippets by remember { mutableStateOf(app.secureConfig.getSnippets()) }
    var floatingBubbleEnabled by remember { mutableStateOf(app.secureConfig.isFloatingBubbleEnabled) }
    var hapticFeedback by remember { mutableStateOf(app.secureConfig.isHapticFeedbackEnabled) }
    var soundEffects by remember { mutableStateOf(app.secureConfig.isSoundEffectsEnabled) }

    var showAddDialog by remember { mutableStateOf(false) }
    var newTrigger by remember { mutableStateOf("") }
    var newExpansion by remember { mutableStateOf("") }

    var editingKey by remember { mutableStateOf<String?>(null) }
    var editTrigger by remember { mutableStateOf("") }
    var editExpansion by remember { mutableStateOf("") }

    var showRestrictedDialog by remember { mutableStateOf(false) }

    val isA11yRunning = OverlayAccessibilityService.isRunning()

    Scaffold(
        containerColor = DesignTokens.Colors.BackgroundDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = DesignTokens.Colors.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Spoken Snippets & Behavior",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextPrimary
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Snippets Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VOICE SHORTCUTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.Colors.TextSubtle,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "+ New Snippet",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DesignTokens.Colors.PrimaryLightBlue,
                        modifier = Modifier.clickable {
                            showAddDialog = true
                        }
                    )
                }
            }

            // Snippet items
            if (snippets.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                            Text("No snippets yet. Tap '+ New Snippet' to add one.", color = DesignTokens.Colors.TextSubtle, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(snippets.entries.toList()) { (trigger, expansion) ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "\"$trigger\"",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.PrimaryLightBlue
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = expansion,
                                    fontSize = 12.sp,
                                    color = DesignTokens.Colors.TextPrimary,
                                    maxLines = 2
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        editingKey = trigger
                                        editTrigger = trigger
                                        editExpansion = expansion
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = DesignTokens.Colors.TextMuted, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        val updated = snippets.toMutableMap()
                                        updated.remove(trigger)
                                        snippets = updated
                                        app.secureConfig.saveSnippets(updated)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = DesignTokens.Colors.StatusRecording, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Behavior & Overlay Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "FLOATING BUBBLE & ACCESSIBILITY",
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
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Floating Bubble Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Floating Voice Bubble",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Draws an overlay button on top of any app for 1-tap dictation",
                                    fontSize = 12.sp,
                                    color = DesignTokens.Colors.TextMuted
                                )
                            }
                            Switch(
                                checked = floatingBubbleEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (Settings.canDrawOverlays(context)) {
                                            floatingBubbleEnabled = true
                                            app.secureConfig.isFloatingBubbleEnabled = true
                                            val serviceIntent = Intent(context, FloatingBubbleService::class.java).apply {
                                                action = FloatingBubbleService.ACTION_START_BUBBLE
                                            }
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                context.startForegroundService(serviceIntent)
                                            } else {
                                                context.startService(serviceIntent)
                                            }
                                        } else {
                                            showRestrictedDialog = true
                                        }
                                    } else {
                                        floatingBubbleEnabled = false
                                        app.secureConfig.isFloatingBubbleEnabled = false
                                        val serviceIntent = Intent(context, FloatingBubbleService::class.java).apply {
                                            action = FloatingBubbleService.ACTION_STOP_BUBBLE
                                        }
                                        context.startService(serviceIntent)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DesignTokens.Colors.PrimaryBlue
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = DesignTokens.Colors.BorderSubtle,
                            thickness = 0.5.dp
                        )

                        // Accessibility status
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Accessibility Auto-Typing",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = if (isA11yRunning) "Active • Types text automatically into focused fields" else "Disabled • Tap to enable in Android settings",
                                    fontSize = 12.sp,
                                    color = if (isA11yRunning) DesignTokens.Colors.StatusReady else DesignTokens.Colors.TextMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Configure",
                                tint = DesignTokens.Colors.TextMuted
                            )
                        }
                    }
                }
            }

            // Feedback Toggles Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "FEEDBACK & SOUNDS",
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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Haptic Vibration Feedback",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Vibrates briefly on mic start and completion",
                                    fontSize = 12.sp,
                                    color = DesignTokens.Colors.TextMuted
                                )
                            }
                            Switch(
                                checked = hapticFeedback,
                                onCheckedChange = {
                                    hapticFeedback = it
                                    app.secureConfig.isHapticFeedbackEnabled = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DesignTokens.Colors.PrimaryBlue
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = DesignTokens.Colors.BorderSubtle,
                            thickness = 0.5.dp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sound Effects",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Plays gentle chimes on recording start and stop",
                                    fontSize = 12.sp,
                                    color = DesignTokens.Colors.TextMuted
                                )
                            }
                            Switch(
                                checked = soundEffects,
                                onCheckedChange = {
                                    soundEffects = it
                                    app.secureConfig.isSoundEffectsEnabled = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DesignTokens.Colors.PrimaryBlue
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Add Snippet Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Voice Snippet", color = DesignTokens.Colors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTrigger,
                        onValueChange = { newTrigger = it },
                        label = { Text("Spoken Trigger (e.g. 'my email')") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DesignTokens.Colors.PrimaryBlue,
                            unfocusedBorderColor = DesignTokens.Colors.BorderSubtle,
                            focusedTextColor = DesignTokens.Colors.TextPrimary,
                            unfocusedTextColor = DesignTokens.Colors.TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newExpansion,
                        onValueChange = { newExpansion = it },
                        label = { Text("Expanded Text (e.g. 'ayush@example.com')") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DesignTokens.Colors.PrimaryBlue,
                            unfocusedBorderColor = DesignTokens.Colors.BorderSubtle,
                            focusedTextColor = DesignTokens.Colors.TextPrimary,
                            unfocusedTextColor = DesignTokens.Colors.TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTrigger.isNotBlank() && newExpansion.isNotBlank()) {
                            val updated = snippets.toMutableMap()
                            updated[newTrigger.trim().lowercase()] = newExpansion.trim()
                            snippets = updated
                            app.secureConfig.saveSnippets(updated)
                            newTrigger = ""
                            newExpansion = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.PrimaryBlue)
                ) {
                    Text("Save Snippet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = DesignTokens.Colors.TextMuted)
                }
            },
            containerColor = DesignTokens.Colors.CardSurface
        )
    }

    // Edit Snippet Dialog
    if (editingKey != null) {
        AlertDialog(
            onDismissRequest = { editingKey = null },
            title = { Text("Edit Voice Snippet", color = DesignTokens.Colors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editTrigger,
                        onValueChange = { editTrigger = it },
                        label = { Text("Spoken Trigger") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DesignTokens.Colors.PrimaryBlue,
                            unfocusedBorderColor = DesignTokens.Colors.BorderSubtle,
                            focusedTextColor = DesignTokens.Colors.TextPrimary,
                            unfocusedTextColor = DesignTokens.Colors.TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editExpansion,
                        onValueChange = { editExpansion = it },
                        label = { Text("Expanded Text") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DesignTokens.Colors.PrimaryBlue,
                            unfocusedBorderColor = DesignTokens.Colors.BorderSubtle,
                            focusedTextColor = DesignTokens.Colors.TextPrimary,
                            unfocusedTextColor = DesignTokens.Colors.TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTrigger.isNotBlank() && editExpansion.isNotBlank()) {
                            val updated = snippets.toMutableMap()
                            editingKey?.let { updated.remove(it) }
                            updated[editTrigger.trim().lowercase()] = editExpansion.trim()
                            snippets = updated
                            app.secureConfig.saveSnippets(updated)
                            editingKey = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.PrimaryBlue)
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingKey = null }) {
                    Text("Cancel", color = DesignTokens.Colors.TextMuted)
                }
            },
            containerColor = DesignTokens.Colors.CardSurface
        )
    }

    // Restricted Settings Dialog
    if (showRestrictedDialog) {
        AlertDialog(
            onDismissRequest = { showRestrictedDialog = false },
            title = { Text("Unlock Android Permission", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "Android restricts 'Display over other apps' until enabled in App Info:\n\n" +
                               "1. Tap 'Open App Info' below.\n" +
                               "2. Tap the 3 dots (⋮) in the top-right corner.\n" +
                               "3. Tap 'Allow restricted settings'.\n" +
                               "4. Return and toggle Floating Bubble ON.",
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestrictedDialog = false
                        val appInfoIntent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        context.startActivity(appInfoIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.PrimaryBlue)
                ) {
                    Text("Open App Info")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestrictedDialog = false
                        val overlayIntent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        context.startActivity(overlayIntent)
                    }
                ) {
                    Text("Direct Overlay Screen", color = DesignTokens.Colors.TextMuted)
                }
            },
            containerColor = DesignTokens.Colors.CardSurface
        )
    }
}
