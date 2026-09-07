package com.example.ownvoice.ui.screens

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
        PillOption("fast", "Fast", "Low latency"),
        PillOption("balanced", "Balanced", "Best for most users"),
        PillOption("high_quality", "High Quality", "Maximum accuracy")
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
                        text = "Your voice, everywhere",
                        fontSize = 10.sp,
                        color = DesignTokens.Colors.TextSubtle
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DesignTokens.Colors.BackgroundDark)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        app.secureConfig.language = language
                        app.secureConfig.voiceModelTier = voiceModelTier
                        app.secureConfig.speakingStyle = speakingStyle
                        app.secureConfig.inputSensitivity = inputSensitivity
                        // Map speakingStyle to dictationMode
                        app.secureConfig.dictationMode = when (speakingStyle) {
                            "professional" -> "professional"
                            "casual" -> "casual"
                            "concise" -> "smart_flow"
                            else -> "smart_flow"
                        }
                        Toast.makeText(context, "Voice settings saved", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.PrimaryBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Save Changes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = DesignTokens.Colors.TextSubtle,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Changes will apply immediately.",
                        fontSize = 11.sp,
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    text = "Customize how OwnVoice understands and converts your voice into text.",
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
                                    text = "Language",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Choose the language you primarily speak.",
                                    fontSize = 11.sp,
                                    color = DesignTokens.Colors.TextMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Navigate",
                                tint = DesignTokens.Colors.TextSubtle,
                                modifier = Modifier.size(16.dp)
                            )
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
                                            languageExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card 2: Voice Model
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
                                    text = "Voice Model",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Select the AI model that best suits your needs.",
                                    fontSize = 11.sp,
                                    color = DesignTokens.Colors.TextMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Navigate",
                                tint = DesignTokens.Colors.TextSubtle,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        SegmentedPillSelector(
                            options = modelOptions,
                            selectedId = voiceModelTier,
                            onOptionSelected = { voiceModelTier = it }
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
                                    text = "Choose how OwnVoice should interpret your voice.",
                                    fontSize = 11.sp,
                                    color = DesignTokens.Colors.TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        SegmentedPillSelector(
                            options = styleOptions,
                            selectedId = speakingStyle,
                            onOptionSelected = { speakingStyle = it }
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
                                    text = "Voice Input Sensitivity",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Adjust how sensitive the microphone is to your voice.",
                                    fontSize = 11.sp,
                                    color = DesignTokens.Colors.TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Slider(
                            value = inputSensitivity,
                            onValueChange = { inputSensitivity = it },
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
                            Text("Less Sensitive", fontSize = 11.sp, color = DesignTokens.Colors.TextSubtle)
                            Text("Balanced", fontSize = 11.sp, color = DesignTokens.Colors.TextSubtle)
                            Text("More Sensitive", fontSize = 11.sp, color = DesignTokens.Colors.TextSubtle)
                        }
                    }
                }
            }

            // Card 5: Advanced Options
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAdvanced() }
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
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Advanced",
                                tint = DesignTokens.Colors.TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Advanced Options",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DesignTokens.Colors.TextPrimary
                            )
                            Text(
                                text = "Custom vocabulary, noise filtering and more.",
                                fontSize = 11.sp,
                                color = DesignTokens.Colors.TextMuted
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Navigate",
                            tint = DesignTokens.Colors.TextSubtle,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Card 6: Preview
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
                                    text = "Preview",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Test your settings with a sample phrase.",
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
                                        Toast.makeText(context, "Playing audio preview...", Toast.LENGTH_SHORT).show()
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
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
