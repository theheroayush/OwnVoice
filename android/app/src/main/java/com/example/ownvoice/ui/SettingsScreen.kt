package com.example.ownvoice.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ownvoice.OwnVoiceApplication
import com.example.ownvoice.R
import com.example.ownvoice.overlay.FloatingBubbleService
import com.example.ownvoice.overlay.OverlayAccessibilityService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateToOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OwnVoiceApplication

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    var floatingBubbleEnabled by remember { mutableStateOf(app.secureConfig.isFloatingBubbleEnabled) }
    var autoContextToneEnabled by remember { mutableStateOf(app.secureConfig.isAutoContextToneEnabled) }
    var selfCorrectionEnabled by remember { mutableStateOf(app.secureConfig.isSelfCorrectionEnabled) }
    var soundEffectsEnabled by remember { mutableStateOf(app.secureConfig.isSoundEffectsEnabled) }
    var selectedTone by remember { mutableStateOf(app.secureConfig.dictationMode) }
    var snippets by remember { mutableStateOf(app.secureConfig.getSnippets()) }
    var vocabulary by remember { mutableStateOf(app.secureConfig.getVocabulary()) }
    var bridgeIp by remember { mutableStateOf(app.secureConfig.desktopBridgeIp) }
    var gistToken by remember { mutableStateOf(app.secureConfig.gistToken) }
    var gistId by remember { mutableStateOf(app.secureConfig.gistId) }
    var syncStatusMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val gistManager = remember { com.example.ownvoice.network.GistSyncManager() }

    var showRestrictedSettingsDialog by remember { mutableStateOf(false) }
    var showAddSnippetDialog by remember { mutableStateOf(false) }
    var showAddVocabDialog by remember { mutableStateOf(false) }
    var newTrigger by remember { mutableStateOf("") }
    var newExpansion by remember { mutableStateOf("") }
    var newVocabTerm by remember { mutableStateOf("") }

    val defaultIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
    val isOwnVoiceActiveIme = defaultIme.contains("ownvoice", ignoreCase = true)

    val darkBg = Color(0xFF121214)
    val cardBg = Color(0xFF1C1C22)
    val accentBlue = Color(0xFF0A84FF)
    val accentGreen = Color(0xFF34C759)
    val accentRed = Color(0xFFFF453A)
    val textMuted = Color(0xFF8E8E93)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_icon),
                            contentDescription = "OwnVoice Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("OwnVoice Settings", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBg),
                actions = {
                    IconButton(onClick = onNavigateToOnboarding) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = "API Key", tint = accentBlue)
                    }
                }
            )
        },
        containerColor = darkBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section 0: Microphone Permission Banner (if not granted)
            if (!hasMicPermission) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF3A1A1A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("⚠️ Microphone Permission Needed", fontWeight = FontWeight.Bold, color = accentRed)
                            Text("The keyboard and floating bubble cannot record your voice without microphone access.", fontSize = 12.sp, color = Color.White, modifier = Modifier.padding(vertical = 4.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                colors = ButtonDefaults.buttonColors(containerColor = accentRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Grant Microphone Permission", color = Color.White)
                            }
                        }
                    }
                }
            }

            // Section 1: Input Modalities
            item {
                Text("INPUT MODALITIES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMuted)
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Mode 1: Native Keyboard
                        Text("Mode 1: Native Voice Keyboard (IME)", fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(
                            text = if (isOwnVoiceActiveIme) "✅ OwnVoice is currently your ACTIVE keyboard" else "⚠️ Gboard is currently active. Follow Step 1 & 2 below:",
                            fontSize = 12.sp,
                            color = if (isOwnVoiceActiveIme) accentGreen else accentRed,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C35)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("1. Enable in Settings", fontSize = 11.sp, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    val imm = context.getSystemService(InputMethodManager::class.java)
                                    imm?.showInputMethodPicker()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("2. Switch Keyboard", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFF2C2C35))

                        // Mode 2: Floating Bubble Overlay
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Mode 2: Floating Bubble Overlay", fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("Draggable capsule to dictate over any app", fontSize = 12.sp, color = textMuted)
                            }
                            Switch(
                                checked = floatingBubbleEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                            // On Android 13+, sideloaded apps require restricted settings unlock first!
                                            showRestrictedSettingsDialog = true
                                            return@Switch
                                        }

                                        if (!OverlayAccessibilityService.isRunning()) {
                                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                        }

                                        floatingBubbleEnabled = true
                                        app.secureConfig.isFloatingBubbleEnabled = true
                                        val serviceIntent = Intent(context, FloatingBubbleService::class.java)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            context.startForegroundService(serviceIntent)
                                        } else {
                                            context.startService(serviceIntent)
                                        }
                                    } else {
                                        floatingBubbleEnabled = false
                                        app.secureConfig.isFloatingBubbleEnabled = false
                                        val serviceIntent = Intent(context, FloatingBubbleService::class.java).apply {
                                            action = FloatingBubbleService.ACTION_STOP_BUBBLE
                                        }
                                        context.startService(serviceIntent)
                                    }
                                }
                            )
                        }

                        val isA11yRunning = OverlayAccessibilityService.isRunning()
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isA11yRunning) "✅ Auto-Typing Active (Accessibility ON)" else "⚠️ Auto-Typing requires Accessibility (Tap to Enable)",
                                fontSize = 11.sp,
                                color = if (isA11yRunning) accentGreen else Color(0xFFFF9500),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Section 2: AI Intelligence & Auto-Tone (Version 2.0)
            item {
                Text("AI INTELLIGENCE (v2.0)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMuted)
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Feature 1: Auto Context-Aware Tone
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto Context-Aware Tone", fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("Adapts tone to active app (WhatsApp: Chat, Gmail: Email, Chrome: Search)", fontSize = 12.sp, color = textMuted)
                            }
                            Switch(
                                checked = autoContextToneEnabled,
                                onCheckedChange = { isChecked ->
                                    autoContextToneEnabled = isChecked
                                    app.secureConfig.isAutoContextToneEnabled = isChecked
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF2C2C35))

                        // Feature 2: Spoken Self-Correction
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Spoken Self-Correction", fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("Fixes mid-sentence slips ('meet at 4, actually 5 PM' ➔ 'meet at 5 PM')", fontSize = 12.sp, color = textMuted)
                            }
                            Switch(
                                checked = selfCorrectionEnabled,
                                onCheckedChange = { isChecked ->
                                    selfCorrectionEnabled = isChecked
                                    app.secureConfig.isSelfCorrectionEnabled = isChecked
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF2C2C35))

                        // Feature 3: Acoustic Audio Cues
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Acoustic Audio Cues", fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("Subtle sound effects on start and stop dictation", fontSize = 12.sp, color = textMuted)
                            }
                            Switch(
                                checked = soundEffectsEnabled,
                                onCheckedChange = { isChecked ->
                                    soundEffectsEnabled = isChecked
                                    app.secureConfig.isSoundEffectsEnabled = isChecked
                                }
                            )
                        }
                    }
                }
            }

            // Section 3: Dictation Tone (Fallback & Keyboard Override)
            item {
                Text("DEFAULT DICTATION TONE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMuted)
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val tones = listOf(
                            "smart_flow" to "Smart Flow (Universal)",
                            "search" to "Search Query (Chrome / YouTube / Spotify)",
                            "chat" to "Casual Chat (WhatsApp / Telegram / Slack)",
                            "formal_email" to "Executive Email (Gmail / Outlook)",
                            "formal_document" to "Documentation Prose (Notion / Keep)",
                            "code" to "Coding / Shell (Termux / GitHub / VS Code)",
                            "translate_hindi" to "Translate English ➔ Hindi",
                            "translate_english" to "Translate Hindi ➔ English",
                            "bullet_notes" to "Bullet Points (- List)",
                            "verbatim" to "Verbatim (Word-for-word)"
                        )

                        tones.forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedTone = key
                                        app.secureConfig.dictationMode = key
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (selectedTone == key),
                                    onClick = {
                                        selectedTone = key
                                        app.secureConfig.dictationMode = key
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = accentBlue)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Section 4: Personal Vocabulary Bank (Version 2.0)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("PERSONAL VOCABULARY BANK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMuted)
                    IconButton(onClick = { showAddVocabDialog = true }, modifier = Modifier.size(28.dp)) {
                        Text("+", color = accentBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Custom names, slang, and technical jargon taught to Gemini:", fontSize = 12.sp, color = textMuted)
                        Spacer(modifier = Modifier.height(10.dp))
                        if (vocabulary.isEmpty()) {
                            Text("No custom words added yet. Tap '+' above to add your own names, Indian terminology, or company acronyms.", color = textMuted, fontSize = 12.sp)
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                vocabulary.forEach { term ->
                                    Surface(
                                        color = Color(0xFF2C2C35),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(term, color = Color.White, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete",
                                                tint = textMuted,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable {
                                                        app.secureConfig.removeVocabularyTerm(term)
                                                        vocabulary = app.secureConfig.getVocabulary()
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Voice Snippets
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("VOICE SNIPPETS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMuted)
                    IconButton(onClick = { showAddSnippetDialog = true }, modifier = Modifier.size(28.dp)) {
                        Text("+", color = accentBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (snippets.isEmpty()) {
                            Text("No snippets configured. Tap '+' to add.", color = textMuted, fontSize = 13.sp)
                        } else {
                            snippets.forEach { (trigger, expansion) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(trigger, fontWeight = FontWeight.Medium, color = Color.White, fontSize = 14.sp)
                                        Text(expansion, color = textMuted, fontSize = 12.sp, maxLines = 1)
                                    }
                                    IconButton(
                                        onClick = {
                                            val updated = snippets.toMutableMap()
                                            updated.remove(trigger)
                                            snippets = updated
                                            app.secureConfig.saveSnippets(updated)
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Delete", tint = accentRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 5: Universal PC Dictation Bridge
            item {
                Text("UNIVERSAL PC DICTATION BRIDGE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMuted)
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💻 Stream Speech to Windows PC", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "Dictate using your phone and have text automatically appear at your cursor on your PC over local Wi-Fi.",
                            fontSize = 12.sp,
                            color = textMuted,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bridgeIp,
                            onValueChange = {
                                bridgeIp = it
                                app.secureConfig.desktopBridgeIp = it
                            },
                            label = { Text("PC Wi-Fi IP (e.g. 192.168.1.15)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Section 6: GitHub Gist Cloud Sync
            item {
                Text("GITHUB GIST CLOUD SYNC", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMuted)
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("☁️ Sync Vocabulary & Snippets", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "Keep your custom vocabulary, names, and snippets synchronized between your Windows PC and your phone.",
                            fontSize = 12.sp,
                            color = textMuted,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = gistToken,
                            onValueChange = {
                                gistToken = it
                                app.secureConfig.gistToken = it
                            },
                            label = { Text("GitHub Personal Access Token (PAT)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = gistId,
                            onValueChange = {
                                gistId = it
                                app.secureConfig.gistId = it
                            },
                            label = { Text("Gist ID (leave blank to create new)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (syncStatusMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(syncStatusMessage, fontSize = 12.sp, color = accentBlue)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        syncStatusMessage = "Syncing to GitHub Gist..."
                                        val payload = org.json.JSONObject().apply {
                                            put("vocabulary", org.json.JSONArray(app.secureConfig.getVocabulary()))
                                            val snipJson = org.json.JSONObject()
                                            app.secureConfig.getSnippets().forEach { (k, v) -> snipJson.put(k, v) }
                                            put("snippets", snipJson)
                                            put("dictation_mode", app.secureConfig.dictationMode)
                                        }
                                        val res = gistManager.syncToGist(gistToken, payload, gistId.ifBlank { null })
                                        syncStatusMessage = res.message
                                        if (res.success && res.gistId != null) {
                                            gistId = res.gistId
                                            app.secureConfig.gistId = res.gistId
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("⬆️ Push")
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        syncStatusMessage = "Pulling from GitHub Gist..."
                                        val res = gistManager.syncFromGist(gistToken, gistId)
                                        syncStatusMessage = res.message
                                        if (res.success && res.payload != null) {
                                            val p = res.payload
                                            val vocabArr = p.optJSONArray("vocabulary")
                                            if (vocabArr != null) {
                                                for (i in 0 until vocabArr.length()) {
                                                    val term = vocabArr.getString(i)
                                                    app.secureConfig.addVocabularyTerm(term)
                                                }
                                                vocabulary = app.secureConfig.getVocabulary()
                                            }
                                            val snipObj = p.optJSONObject("snippets")
                                            if (snipObj != null) {
                                                val existing = app.secureConfig.getSnippets().toMutableMap()
                                                val keys = snipObj.keys()
                                                while (keys.hasNext()) {
                                                    val k = keys.next()
                                                    existing[k] = snipObj.getString(k)
                                                }
                                                app.secureConfig.saveSnippets(existing)
                                                snippets = existing
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("⬇️ Pull", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for Android 13+ Restricted Settings Unlock
    if (showRestrictedSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showRestrictedSettingsDialog = false },
            title = { Text("Unlock Android Permission First", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "Android 13/14+ restricts 'Display over other apps' for sideloaded apps until you allow it:\n\n" +
                               "1. Tap 'Open App Info' below.\n" +
                               "2. Tap the 3 dots (⋮) in the top-right corner.\n" +
                               "3. Tap 'Allow restricted settings' and enter your PIN.\n" +
                               "4. Then tap 'Display over other apps' to turn it ON.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestrictedSettingsDialog = false
                        // Open App Info where the 3 dots exist!
                        val appInfoIntent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        context.startActivity(appInfoIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
                ) {
                    Text("Open App Info (3 Dots)")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestrictedSettingsDialog = false
                        // Direct overlay settings fallback
                        val overlayIntent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        context.startActivity(overlayIntent)
                    }
                ) {
                    Text("Direct Overlay Screen")
                }
            },
            containerColor = cardBg
        )
    }

    if (showAddSnippetDialog) {
        AlertDialog(
            onDismissRequest = { showAddSnippetDialog = false },
            title = { Text("Add Voice Snippet", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTrigger,
                        onValueChange = { newTrigger = it },
                        label = { Text("Spoken Trigger (e.g. 'my email')") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newExpansion,
                        onValueChange = { newExpansion = it },
                        label = { Text("Expanded Text (e.g. 'me@email.com')") }
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
                            showAddSnippetDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSnippetDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = cardBg
        )
    }

    if (showAddVocabDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddVocabDialog = false
                newVocabTerm = ""
            },
            title = { Text("Add Personal Vocabulary", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Teach Gemini specific names, acronyms, or technical jargon (e.g. 'Aarav', 'Bengaluru', 'Kubernetes', 'B2B SaaS'). Gemini will prioritize this exact spelling.",
                        color = textMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newVocabTerm,
                        onValueChange = { newVocabTerm = it },
                        label = { Text("Word / Name / Jargon") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newVocabTerm.trim()
                        if (trimmed.isNotBlank()) {
                            app.secureConfig.addVocabularyTerm(trimmed)
                            vocabulary = app.secureConfig.getVocabulary()
                            newVocabTerm = ""
                            showAddVocabDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
                ) {
                    Text("Add Term")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddVocabDialog = false
                        newVocabTerm = ""
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = cardBg
        )
    }
}
