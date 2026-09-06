package com.example.ownvoice.ime.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

enum class KeyboardState {
    IDLE,
    RECORDING,
    PROCESSING,
    RESULT,
    TOOLS,
    TYPE,
    ERROR
}

private fun formatToneLabel(tone: String): String {
    return when (tone.lowercase()) {
        "smart_flow" -> "Smart Flow"
        "search" -> "Search"
        "translate_hindi" -> "Hindi"
        "translate_english" -> "English"
        "chat" -> "Chat"
        "formal_email" -> "Formal Email"
        "formal_document" -> "Document"
        "code" -> "Code"
        "bullet_notes" -> "Bullets"
        "verbatim" -> "Verbatim"
        else -> tone.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}

@Composable
fun KeyboardView(
    state: KeyboardState,
    amplitude: Float,
    statusMessage: String,
    activeTone: String,
    snippets: Map<String, String>,
    lastInjectedText: String = "",
    isBridgeActive: Boolean = false,
    isAutoVadActive: Boolean = false,
    desktopBridgeIp: String = "",
    onToggleRecording: () -> Unit,
    onToneCycle: () -> Unit = {},
    onToneSelect: (String) -> Unit = {},
    onClearClick: () -> Unit = {},
    onNewLineClick: () -> Unit = {},
    onToggleBridge: () -> Unit = {},
    onToggleAutoVad: () -> Unit = {},
    onSnippetClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onEnterClick: () -> Unit,
    onSpaceClick: () -> Unit,
    onSwitchKeyboardClick: () -> Unit,
    onStateChange: (KeyboardState) -> Unit = {},
    onShapeText: (String) -> Unit = {},
    onRetryClick: () -> Unit = {},
    onSendClick: () -> Unit = {},
    onTypeChar: (String) -> Unit = {}
) {
    // Horizon Design System Tokens (Obsidian & Warm Gold Titanium)
    val obsidianDark = Color(0xFF121316)
    val cardDark = Color(0xFF1B1C22)
    val cardSurface = Color(0xFF242630)
    val hairlineBorder = Color(0xFF2E313D)
    val goldAccent = Color(0xFFE5C07B)
    val goldAccentGlow = Color(0xFFD4AF37)
    val accentRed = Color(0xFFFF453A)
    val accentBlue = Color(0xFF0A84FF)
    val accentGreen = Color(0xFF34C759)
    val textPrimary = Color(0xFFF5F5F7)
    val textSecondary = Color(0xFF9898A0)
    val textMuted = Color(0xFF636366)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp, max = 290.dp)
            .background(obsidianDark)
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state) {
            KeyboardState.RECORDING -> {
                // SCREEN 2: LISTENING (Non-blocking fluid acoustic waveform)
                ListeningStateView(
                    amplitude = amplitude,
                    goldAccent = goldAccent,
                    accentRed = accentRed,
                    cardDark = cardDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onStopClick = onToggleRecording
                )
            }
            KeyboardState.PROCESSING -> {
                // Processing with Gemini Indicator
                ProcessingStateView(
                    statusMessage = statusMessage,
                    goldAccent = goldAccent,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }
            KeyboardState.RESULT -> {
                // SCREEN 3: RESULT & IN-PLACE AI SHAPING
                ResultShapingStateView(
                    lastInjectedText = lastInjectedText,
                    goldAccent = goldAccent,
                    cardDark = cardDark,
                    cardSurface = cardSurface,
                    hairlineBorder = hairlineBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accentBlue = accentBlue,
                    onShapeText = onShapeText,
                    onRetryClick = onRetryClick,
                    onAddMoreClick = onToggleRecording,
                    onSendClick = onSendClick,
                    onDismiss = { onStateChange(KeyboardState.IDLE) }
                )
            }
            KeyboardState.TOOLS -> {
                // SCREEN 4: TOOLS DRAWER
                ToolsDrawerView(
                    isBridgeActive = isBridgeActive,
                    isAutoVadActive = isAutoVadActive,
                    desktopBridgeIp = desktopBridgeIp,
                    snippets = snippets,
                    cardDark = cardDark,
                    cardSurface = cardSurface,
                    hairlineBorder = hairlineBorder,
                    goldAccent = goldAccent,
                    accentBlue = accentBlue,
                    accentGreen = accentGreen,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onToggleBridge = onToggleBridge,
                    onToggleAutoVad = onToggleAutoVad,
                    onSnippetClick = onSnippetClick,
                    onClose = { onStateChange(KeyboardState.IDLE) }
                )
            }
            KeyboardState.TYPE -> {
                // SCREEN 5: MINIMALIST QWERTY FALLBACK
                MinimalQwertyView(
                    cardDark = cardDark,
                    cardSurface = cardSurface,
                    hairlineBorder = hairlineBorder,
                    goldAccent = goldAccent,
                    accentBlue = accentBlue,
                    textPrimary = textPrimary,
                    onTypeChar = onTypeChar,
                    onBackspaceClick = onBackspaceClick,
                    onEnterClick = onEnterClick,
                    onSpaceClick = onSpaceClick,
                    onVoiceSwitchClick = { onStateChange(KeyboardState.IDLE) }
                )
            }
            KeyboardState.ERROR -> {
                // Error State View
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = accentRed,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage.ifBlank { "An unexpected error occurred." },
                        color = textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onStateChange(KeyboardState.IDLE) },
                        colors = ButtonDefaults.buttonColors(containerColor = cardSurface)
                    ) {
                        Text(text = "Dismiss", color = textPrimary)
                    }
                }
            }
            KeyboardState.IDLE -> {
                // SCREEN 1: IDLE (Clean, Focused, Low-Profile)
                IdleStateView(
                    activeTone = activeTone,
                    statusMessage = statusMessage,
                    snippets = snippets,
                    cardDark = cardDark,
                    cardSurface = cardSurface,
                    hairlineBorder = hairlineBorder,
                    goldAccent = goldAccent,
                    goldAccentGlow = goldAccentGlow,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accentBlue = accentBlue,
                    onToneCycle = onToneCycle,
                    onToneSelect = onToneSelect,
                    onClearClick = onClearClick,
                    onNewLineClick = onNewLineClick,
                    onSnippetClick = onSnippetClick,
                    onToggleRecording = onToggleRecording,
                    onOpenTools = { onStateChange(KeyboardState.TOOLS) },
                    onOpenType = { onStateChange(KeyboardState.TYPE) },
                    onSwitchKeyboardClick = onSwitchKeyboardClick,
                    onSpaceClick = onSpaceClick,
                    onBackspaceClick = onBackspaceClick,
                    onEnterClick = onEnterClick
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SCREEN 1: IDLE VIEW (Low-profile dock with centered circular mic and hairline gold ring)
// -------------------------------------------------------------------------------------------------
@Composable
private fun IdleStateView(
    activeTone: String,
    statusMessage: String,
    snippets: Map<String, String>,
    cardDark: Color,
    cardSurface: Color,
    hairlineBorder: Color,
    goldAccent: Color,
    goldAccentGlow: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentBlue: Color,
    onToneCycle: () -> Unit,
    onToneSelect: (String) -> Unit,
    onClearClick: () -> Unit,
    onNewLineClick: () -> Unit,
    onSnippetClick: (String) -> Unit,
    onToggleRecording: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenType: () -> Unit,
    onSwitchKeyboardClick: () -> Unit,
    onSpaceClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onEnterClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Action Pills Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tone Mode Pill
            item {
                Surface(
                    color = if (activeTone != "smart_flow") goldAccent.copy(alpha = 0.18f) else cardDark,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (activeTone != "smart_flow") goldAccent else hairlineBorder),
                    modifier = Modifier.clickable { onToneCycle() }
                ) {
                    Text(
                        text = "⚡ ${formatToneLabel(activeTone)}",
                        color = if (activeTone != "smart_flow") goldAccent else textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Quick Search Pill
            item {
                val isSearch = activeTone == "search"
                Surface(
                    color = if (isSearch) goldAccent.copy(alpha = 0.22f) else cardDark,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isSearch) goldAccent else hairlineBorder),
                    modifier = Modifier.clickable { onToneSelect(if (isSearch) "smart_flow" else "search") }
                ) {
                    Text(
                        text = "🔍 Search",
                        color = if (isSearch) goldAccent else textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Quick Hindi Translate Pill
            item {
                val isHindi = activeTone == "translate_hindi"
                Surface(
                    color = if (isHindi) goldAccent.copy(alpha = 0.22f) else cardDark,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isHindi) goldAccent else hairlineBorder),
                    modifier = Modifier.clickable { onToneSelect(if (isHindi) "smart_flow" else "translate_hindi") }
                ) {
                    Text(
                        text = "🌐 Hindi",
                        color = if (isHindi) goldAccent else textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Tools Drawer Trigger Pill
            item {
                Surface(
                    color = cardDark,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, hairlineBorder),
                    modifier = Modifier.clickable { onOpenTools() }
                ) {
                    Text(
                        text = "🛠️ Tools",
                        color = textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Clear Field Pill
            item {
                Surface(
                    color = cardDark,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, hairlineBorder),
                    modifier = Modifier.clickable { onClearClick() }
                ) {
                    Text(
                        text = "✕ Clear",
                        color = textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Snippets
            items(snippets.keys.toList()) { trigger ->
                Surface(
                    color = cardDark,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, hairlineBorder),
                    modifier = Modifier.clickable { onSnippetClick(trigger) }
                ) {
                    Text(
                        text = trigger,
                        color = textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Center Voice Hub (Centered Mic Button with hairline gold ring & idle pulse)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "idleBreath")
            val breathScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.035f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "breathAnim"
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(breathScale),
                contentAlignment = Alignment.Center
            ) {
                // Outer subtle hairline halo
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(goldAccent.copy(alpha = 0.08f))
                )

                // Main Circular Button
                Surface(
                    shape = CircleShape,
                    color = cardDark,
                    border = BorderStroke(1.5.dp, goldAccentGlow),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(68.dp)
                        .clickable { onToggleRecording() }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Tap to speak",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (statusMessage.isNotBlank()) statusMessage else "Tap to speak",
                color = textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // Bottom Ergonomic Dock (Keyboard Toggle, Space Bar, Backspace, Enter)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Type (Keyboard) Switch Button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cardDark,
                border = BorderStroke(1.dp, hairlineBorder),
                modifier = Modifier
                    .size(44.dp)
                    .clickable { onOpenType() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Switch to QWERTY typing",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Globe (System Switch)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cardDark,
                border = BorderStroke(1.dp, hairlineBorder),
                modifier = Modifier
                    .size(44.dp)
                    .clickable { onSwitchKeyboardClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Switch system keyboard",
                        tint = textSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Wide Space Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cardDark,
                border = BorderStroke(1.dp, hairlineBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable { onSpaceClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Space",
                        color = textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Backspace
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cardDark,
                border = BorderStroke(1.dp, hairlineBorder),
                modifier = Modifier
                    .size(44.dp)
                    .clickable { onBackspaceClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = textPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Enter / Send
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentBlue,
                modifier = Modifier
                    .size(44.dp)
                    .clickable { onEnterClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "↵",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SCREEN 2: LISTENING VIEW (Non-blocking fluid acoustic waveform)
// -------------------------------------------------------------------------------------------------
@Composable
private fun ListeningStateView(
    amplitude: Float,
    goldAccent: Color,
    accentRed: Color,
    cardDark: Color,
    textPrimary: Color,
    textSecondary: Color,
    onStopClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveFlow")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseAnim"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(goldAccent)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Listening…",
                color = textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Fluid Multi-Harmonic Soundwave Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val midY = height / 2f
                val activeAmp = (amplitude * 1.4f).coerceIn(0.1f, 1.0f)

                // Primary Golden Wave
                val path1 = Path()
                path1.moveTo(0f, midY)
                val points = 80
                for (i in 0..points) {
                    val x = (i.toFloat() / points) * width
                    val angle = (i.toFloat() / points) * 3f * Math.PI.toFloat() + phase
                    val envelope = sin((i.toFloat() / points) * Math.PI.toFloat())
                    val y = midY + sin(angle) * (height * 0.42f * activeAmp * envelope)
                    path1.lineTo(x, y)
                }
                drawPath(
                    path = path1,
                    brush = Brush.horizontalGradient(
                        listOf(goldAccent.copy(alpha = 0.4f), goldAccent, Color(0xFFFFB347))
                    ),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Secondary Harmonic Wave
                val path2 = Path()
                path2.moveTo(0f, midY)
                for (i in 0..points) {
                    val x = (i.toFloat() / points) * width
                    val angle = (i.toFloat() / points) * 4.5f * Math.PI.toFloat() - (phase * 1.2f)
                    val envelope = sin((i.toFloat() / points) * Math.PI.toFloat())
                    val y = midY + sin(angle) * (height * 0.28f * activeAmp * envelope)
                    path2.lineTo(x, y)
                }
                drawPath(
                    path = path2,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFFF7E5F).copy(alpha = 0.3f), Color(0xFFFF7E5F), goldAccent.copy(alpha = 0.5f))
                    ),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Concentric Pulsing Stop Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "stopPulse"
            )

            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                // Expanding Ripple
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(accentRed.copy(alpha = 0.20f))
                )

                // Stop Circle
                Surface(
                    shape = CircleShape,
                    color = accentRed,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(64.dp)
                        .clickable { onStopClick() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop dictation",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Speak naturally • Tap to stop",
                color = goldAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// PROCESSING STATE VIEW
// -------------------------------------------------------------------------------------------------
@Composable
private fun ProcessingStateView(
    statusMessage: String,
    goldAccent: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(42.dp),
            color = goldAccent,
            strokeWidth = 3.5.dp
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = statusMessage.ifBlank { "Refining with Gemini…" },
            color = textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Formatting, punctuating & eliminating stumbles",
            color = textSecondary,
            fontSize = 11.sp
        )
    }
}

// -------------------------------------------------------------------------------------------------
// SCREEN 3: RESULT & IN-PLACE AI SHAPING VIEW
// -------------------------------------------------------------------------------------------------
@Composable
private fun ResultShapingStateView(
    lastInjectedText: String,
    goldAccent: Color,
    cardDark: Color,
    cardSurface: Color,
    hairlineBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentBlue: Color,
    onShapeText: (String) -> Unit,
    onRetryClick: () -> Unit,
    onAddMoreClick: () -> Unit,
    onSendClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top "Shape with AI" Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "✨ Shape with AI",
                    color = goldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Surface(
                color = cardDark,
                shape = CircleShape,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onDismiss() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Horizontal Transform Pills
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                ShapeChip(
                    label = "⚡ Shorter",
                    cardColor = cardDark,
                    borderColor = hairlineBorder,
                    textColor = textPrimary,
                    onClick = { onShapeText("shorter") }
                )
            }
            item {
                ShapeChip(
                    label = "💼 Executive",
                    cardColor = cardDark,
                    borderColor = hairlineBorder,
                    textColor = textPrimary,
                    onClick = { onShapeText("executive") }
                )
            }
            item {
                ShapeChip(
                    label = "💬 Casual",
                    cardColor = cardDark,
                    borderColor = hairlineBorder,
                    textColor = textPrimary,
                    onClick = { onShapeText("casual") }
                )
            }
            item {
                ShapeChip(
                    label = "🌐 Translate",
                    cardColor = cardDark,
                    borderColor = hairlineBorder,
                    textColor = textPrimary,
                    onClick = { onShapeText("translate") }
                )
            }
            item {
                ShapeChip(
                    label = "✨ Fix",
                    cardColor = cardDark,
                    borderColor = hairlineBorder,
                    textColor = textPrimary,
                    onClick = { onShapeText("fix") }
                )
            }
        }

        // Injected Text Preview Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = cardDark,
            border = BorderStroke(1.dp, hairlineBorder),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (lastInjectedText.isNotBlank()) "\"${lastInjectedText.take(120)}${if (lastInjectedText.length > 120) "…" else ""}\"" else "Text typed at cursor",
                    color = textPrimary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Action Dock (Retry, Add more, Send)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Retry (Erasure & Re-record)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cardDark,
                border = BorderStroke(1.dp, hairlineBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable { onRetryClick() }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Retry", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Add More (Continue Speaking)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cardDark,
                border = BorderStroke(1.dp, goldAccent),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable { onAddMoreClick() }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Add more",
                        tint = goldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Add more", color = goldAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Send / Commit
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentBlue,
                modifier = Modifier
                    .weight(1.2f)
                    .height(44.dp)
                    .clickable { onSendClick() }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "Send ➔", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ShapeChip(
    label: String,
    cardColor: Color,
    borderColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = cardColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

// -------------------------------------------------------------------------------------------------
// SCREEN 4: TOOLS DRAWER VIEW
// -------------------------------------------------------------------------------------------------
@Composable
private fun ToolsDrawerView(
    isBridgeActive: Boolean,
    isAutoVadActive: Boolean,
    desktopBridgeIp: String,
    snippets: Map<String, String>,
    cardDark: Color,
    cardSurface: Color,
    hairlineBorder: Color,
    goldAccent: Color,
    accentBlue: Color,
    accentGreen: Color,
    textPrimary: Color,
    textSecondary: Color,
    onToggleBridge: () -> Unit,
    onToggleAutoVad: () -> Unit,
    onSnippetClick: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🛠️ OwnVoice Tools & Bridge",
                color = textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                color = cardDark,
                shape = CircleShape,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onClose() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = textSecondary, modifier = Modifier.size(14.dp))
                }
            }
        }

        // Tools Row 1: PC Bridge & Auto VAD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // PC Bridge Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isBridgeActive) Color(0xFF00C7BE).copy(alpha = 0.2f) else cardDark,
                border = BorderStroke(1.dp, if (isBridgeActive) Color(0xFF00C7BE) else hairlineBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .clickable { onToggleBridge() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isBridgeActive) "💻 PC Bridge: ON" else "💻 PC Bridge",
                        color = if (isBridgeActive) Color(0xFF00C7BE) else textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (desktopBridgeIp.isNotBlank()) desktopBridgeIp else "Tap to connect",
                        color = textSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            // Auto VAD Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isAutoVadActive) accentGreen.copy(alpha = 0.2f) else cardDark,
                border = BorderStroke(1.dp, if (isAutoVadActive) accentGreen else hairlineBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .clickable { onToggleAutoVad() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isAutoVadActive) "🎙️ Auto VAD: ON" else "🎙️ Auto VAD",
                        color = if (isAutoVadActive) accentGreen else textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "1.2s silence commit",
                        color = textSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Snippets shelf
        Text(text = "Personal Voice Snippets", color = textSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(snippets.keys.toList()) { trigger ->
                Surface(
                    color = cardDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, hairlineBorder),
                    modifier = Modifier.clickable {
                        onSnippetClick(trigger)
                        onClose()
                    }
                ) {
                    Text(
                        text = trigger,
                        color = textPrimary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Done button
        Button(
            onClick = { onClose() },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = cardSurface)
        ) {
            Text(text = "Back to Voice Keyboard", color = textPrimary, fontSize = 12.sp)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SCREEN 5: MINIMALIST QWERTY FALLBACK
// -------------------------------------------------------------------------------------------------
@Composable
private fun MinimalQwertyView(
    cardDark: Color,
    cardSurface: Color,
    hairlineBorder: Color,
    goldAccent: Color,
    accentBlue: Color,
    textPrimary: Color,
    onTypeChar: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onEnterClick: () -> Unit,
    onSpaceClick: () -> Unit,
    onVoiceSwitchClick: () -> Unit
) {
    var isShifted by remember { mutableStateOf(false) }
    var isSymbolsMode by remember { mutableStateOf(false) }

    val row1 = if (isSymbolsMode) listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
               else listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")

    val row2 = if (isSymbolsMode) listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
               else listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")

    val row3 = if (isSymbolsMode) listOf("=", "*", "\"", "'", ":", ";", "!", "?")
               else listOf("z", "x", "c", "v", "b", "n", "m")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            row1.forEach { char ->
                val displayChar = if (isShifted && !isSymbolsMode) char.uppercase() else char
                KeyButton(char = displayChar, modifier = Modifier.weight(1f)) {
                    onTypeChar(displayChar)
                    if (isShifted) isShifted = false
                }
            }
        }

        // Row 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            row2.forEach { char ->
                val displayChar = if (isShifted && !isSymbolsMode) char.uppercase() else char
                KeyButton(char = displayChar, modifier = Modifier.weight(1f)) {
                    onTypeChar(displayChar)
                    if (isShifted) isShifted = false
                }
            }
        }

        // Row 3 (Shift, letters, Backspace)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shift
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isShifted) goldAccent.copy(alpha = 0.3f) else cardSurface,
                border = BorderStroke(1.dp, if (isShifted) goldAccent else hairlineBorder),
                modifier = Modifier
                    .weight(1.3f)
                    .height(38.dp)
                    .clickable { isShifted = !isShifted }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = "⇧", color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            row3.forEach { char ->
                val displayChar = if (isShifted && !isSymbolsMode) char.uppercase() else char
                KeyButton(char = displayChar, modifier = Modifier.weight(1f)) {
                    onTypeChar(displayChar)
                    if (isShifted) isShifted = false
                }
            }

            // Backspace
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = cardSurface,
                border = BorderStroke(1.dp, hairlineBorder),
                modifier = Modifier
                    .weight(1.3f)
                    .height(38.dp)
                    .clickable { onBackspaceClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Row 4 (Symbols toggle, Voice Switch, Space, Enter)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ?123 toggle
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = cardSurface,
                border = BorderStroke(1.dp, hairlineBorder),
                modifier = Modifier
                    .width(44.dp)
                    .height(40.dp)
                    .clickable { isSymbolsMode = !isSymbolsMode }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = if (isSymbolsMode) "ABC" else "?123", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Return to Voice Mode Button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = goldAccent.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, goldAccent),
                modifier = Modifier
                    .width(44.dp)
                    .height(40.dp)
                    .clickable { onVoiceSwitchClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Return to Voice", tint = goldAccent, modifier = Modifier.size(20.dp))
                }
            }

            // Space bar
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = cardDark,
                border = BorderStroke(1.dp, hairlineBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable { onSpaceClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = "Space", color = textPrimary, fontSize = 13.sp)
                }
            }

            // Period
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = cardSurface,
                border = BorderStroke(1.dp, hairlineBorder),
                modifier = Modifier
                    .width(36.dp)
                    .height(40.dp)
                    .clickable { onTypeChar(".") }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = ".", color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Enter
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentBlue,
                modifier = Modifier
                    .width(48.dp)
                    .height(40.dp)
                    .clickable { onEnterClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = "↵", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    char: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF23252E),
        modifier = modifier
            .height(38.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = char,
                color = Color(0xFFF5F5F7),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
