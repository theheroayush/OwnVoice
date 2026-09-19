package com.example.ownvoice.ui.screens

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ownvoice.OwnVoiceApplication
import com.example.ownvoice.theme.DesignTokens
import com.example.ownvoice.ui.components.PillOption
import com.example.ownvoice.ui.components.SegmentedPillSelector
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    onBack: () -> Unit,
    onNavigateToAdvanced: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as OwnVoiceApplication

    var language by remember { mutableStateOf(app.secureConfig.language) }
    var languageExpanded by remember { mutableStateOf(false) }

    var voiceModelTier by remember { mutableStateOf(app.secureConfig.voiceModelTier) }
    var speakingStyle by remember { mutableStateOf(app.secureConfig.speakingStyle) }
    var inputSensitivity by remember { mutableFloatStateOf(app.secureConfig.inputSensitivity) }

    var showAdvancedDialog by remember { mutableStateOf(false) }
    var customInstructions by remember { mutableStateOf(app.secureConfig.customInstructions) }

    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var speech: TextToSpeech? = null
        speech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                speech?.language = Locale.US
                isTtsReady = true
            }
        }
        tts = speech
        onDispose {
            speech?.stop()
            speech?.shutdown()
        }
    }

    val languageOptions = listOf(
        "English (US)",
        "English (India)",
        "English (UK)",
        "Hindi",
        "Spanish",
        "French",
        "German"
    )

    val modelOptions = listOf(
        PillOption("fast", "Fast", "Ultra low latency"),
        PillOption("balanced", "Balanced", "Best accuracy & speed"),
        PillOption("high_quality", "High Quality", "Deep reasoning")
    )

    val styleOptions = listOf(
        PillOption("natural", "Natural"),
        PillOption("professional", "Professional"),
        PillOption("casual", "Casual"),
        PillOption("concise", "Concise")
    )

    Scaffold(
        containerColor = DesignTokens.Colors.BackgroundDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onBack)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DesignTokens.ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Settings",
                        color = DesignTokens.ElectricBlue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "OwnVoice",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.Colors.TextPrimary
                    )
                    Text(
                        text = "Voice Intelligence",
                        fontSize = 10.sp,
                        color = DesignTokens.Colors.TextSubtle
                    )
                }
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
            // Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Voice Settings",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Customize language, AI model, style, and microphone sensitivity.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = DesignTokens.Colors.TextMuted
                )
            }

            // Card 1: Language
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E3A8A).copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Language",
                                    tint = DesignTokens.ElectricBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Target Language",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Transcription prompt adapts automatically.",
                                    fontSize = 11.sp,
                                    color = DesignTokens.Colors.TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dropdown Selector
                        Box {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0F1420),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { languageExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = language,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = DesignTokens.Colors.TextPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Dropdown",
                                        tint = DesignTokens.Colors.TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = languageExpanded,
                                onDismissRequest = { languageExpanded = false },
                                modifier = Modifier.background(DesignTokens.Colors.CardSurface)
                            ) {
                                languageOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt, color = DesignTokens.Colors.TextPrimary, fontSize = 13.sp) },
                                        onClick = {
                                            language = opt
                                            app.secureConfig.language = opt
                                            languageExpanded = false
                                            Toast.makeText(context, "Language set to $opt", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card 2: Voice Model Tier
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF581C87).copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Voice Model",
                                    tint = DesignTokens.PurpleAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Voice Model Tier",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Gemini 3.6 Flash reasoning depth",
                                    fontSize = 11.sp,
                                    color = DesignTokens.Colors.TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        SegmentedPillSelector(
                            options = modelOptions,
                            selectedId = voiceModelTier,
                            onOptionSelected = {
                                voiceModelTier = it
                                app.secureConfig.voiceModelTier = it
                            }
                        )
                    }
                }
            }

            // Card 3: Speaking Style
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF065F46).copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Speaking Style",
                                    tint = DesignTokens.StatusGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Speaking Style",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Adapts tone and vocabulary to your workflow",
                                    fontSize = 11.sp,
                                    color = DesignTokens.Colors.TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        SegmentedPillSelector(
                            options = styleOptions,
                            selectedId = speakingStyle,
                            onOptionSelected = {
                                speakingStyle = it
                                app.secureConfig.speakingStyle = it
                                app.secureConfig.dictationMode = when (it) {
                                    "professional" -> "formal_document"
                                    "casual" -> "chat"
                                    "concise" -> "bullet_notes"
                                    else -> "smart_flow"
                                }
                            }
                        )
                    }
                }
            }

            // Card 4: Voice Input Sensitivity
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF9A3412).copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Sensitivity",
                                    tint = DesignTokens.AmberAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Microphone Gain & Sensitivity",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Adjusts dynamic Auto-Gain Control (AGC) scaling",
                                    fontSize = 11.sp,
                                    color = DesignTokens.Colors.TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Slider(
                            value = inputSensitivity,
                            onValueChange = {
                                inputSensitivity = it
                                app.secureConfig.inputSensitivity = it
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = DesignTokens.Colors.PrimaryBlue,
                                inactiveTrackColor = Color(0xFF1E293B)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("1.0x (Quiet)", fontSize = 11.sp, color = DesignTokens.Colors.TextSubtle)
                            Text("2.5x (Balanced)", fontSize = 11.sp, color = DesignTokens.Colors.TextSubtle)
                            Text("4.0x (Sensitive)", fontSize = 11.sp, color = DesignTokens.Colors.TextSubtle)
                        }
                    }
                }
            }

            // Card 5: Custom Prompt & System Instructions
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvancedDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Custom Prompt",
                                tint = DesignTokens.Colors.TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Custom Prompt Instructions",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DesignTokens.Colors.TextPrimary
                            )
                            Text(
                                text = if (customInstructions.isNotBlank()) "Custom prompt active" else "Add persona rules or domain terminology",
                                fontSize = 11.sp,
                                color = if (customInstructions.isNotBlank()) DesignTokens.Colors.StatusReady else DesignTokens.Colors.TextMuted
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Edit",
                            tint = DesignTokens.Colors.TextSubtle,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Card 6: Preview Sample
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E3A8A).copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Preview",
                                    tint = DesignTokens.ElectricBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Audio Preview",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Test TTS speech output preview",
                                    fontSize = 11.sp,
                                    color = DesignTokens.Colors.TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0F1420),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "This is how your voice will look in text.",
                                    fontSize = 12.sp,
                                    color = DesignTokens.Colors.TextMuted,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    if (isTtsReady && tts != null) {
                                        tts?.speak("This is how your voice will look in text.", TextToSpeech.QUEUE_FLUSH, null, "sample_tts")
                                    } else {
                                        Toast.makeText(context, "TTS engine initializing...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.PrimaryBlue),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Try Sample", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showAdvancedDialog) {
        AlertDialog(
            onDismissRequest = { showAdvancedDialog = false },
            title = { Text("Custom Prompt Instructions", color = DesignTokens.Colors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Add custom instructions injected into the Gemini 3.6 Flash transcription prompt (e.g. formatting preferences, technical terms):",
                        color = DesignTokens.Colors.TextMuted,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customInstructions,
                        onValueChange = { customInstructions = it },
                        placeholder = { Text("e.g. Always format medical terms in Latin...", color = DesignTokens.Colors.TextSubtle) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DesignTokens.Colors.PrimaryBlue,
                            unfocusedBorderColor = DesignTokens.Colors.BorderSubtle,
                            focusedTextColor = DesignTokens.Colors.TextPrimary,
                            unfocusedTextColor = DesignTokens.Colors.TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        app.secureConfig.customInstructions = customInstructions
                        showAdvancedDialog = false
                        Toast.makeText(context, "Custom instructions saved", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.PrimaryBlue)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdvancedDialog = false }) {
                    Text("Cancel", color = DesignTokens.Colors.TextMuted)
                }
            },
            containerColor = DesignTokens.Colors.CardSurface
        )
    }
}
