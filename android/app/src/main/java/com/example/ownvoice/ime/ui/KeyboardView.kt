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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    clipboardText: String = "",
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
    onPasteClick: () -> Unit = {},
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
    // Google Gboard Material You Dark Theme Color Tokens
    val gboardBg = Color(0xFF0E1320)           // Deep Midnight Navy Background
    val keyBg = Color(0xFF1F273B)              // Translucent Slate Navy Letter Keys
    val keyBorder = Color(0xFF26324D)          // Subtle Key Outline
    val accentKeyBg = Color(0xFF244CA8)        // Rich Cobalt Blue Accent Keys (Shift, Backspace, ?123, Period)
    val actionEnterBg = Color(0xFF1D54D8)      // Vibrant Royal Blue Action / Search / Enter Key
    val micButtonGlow = Color(0xFF2563EB)      // Circular Mic Highlight
    val textWhite = Color(0xFFF0F4F8)          // Crisp Primary White Glyph
    val subscriptGray = Color(0xFF8A99B5)      // Top-Right Number Subscript
    val toolbarIconTint = Color(0xFFBAC7DE)    // Top Toolbar Glyphs
    val accentRed = Color(0xFFFF453A)          // Recording indicator

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 275.dp, max = 300.dp)
            .background(gboardBg)
            .navigationBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // -----------------------------------------------------------------------------------------
        // 1. TOP TOOLBAR: 4-Square Apps Grid, Tone & Shape Pills, Circular Mic Button
        // -----------------------------------------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: 4-Square Grid Apps Icon (Expands Tools)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .size(38.dp)
                    .clickable {
                        if (state == KeyboardState.TOOLS) onStateChange(KeyboardState.IDLE)
                        else onStateChange(KeyboardState.TOOLS)
                    }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    AppsGridIcon(tint = toolbarIconTint)
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Center: Scrollable Pills (Shape with AI when in RESULT, Tone / Chips otherwise)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (state == KeyboardState.RESULT) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            Surface(
                                color = accentKeyBg.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, accentKeyBg),
                                modifier = Modifier.clickable { onShapeText("shorter") }
                            ) {
                                Text(
                                    text = "⚡ Shorter",
                                    color = textWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        item {
                            Surface(
                                color = accentKeyBg.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, accentKeyBg),
                                modifier = Modifier.clickable { onShapeText("executive") }
                            ) {
                                Text(
                                    text = "💼 Executive",
                                    color = textWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        item {
                            Surface(
                                color = accentKeyBg.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, accentKeyBg),
                                modifier = Modifier.clickable { onShapeText("casual") }
                            ) {
                                Text(
                                    text = "💬 Casual",
                                    color = textWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        item {
                            Surface(
                                color = accentKeyBg.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, accentKeyBg),
                                modifier = Modifier.clickable { onShapeText("translate") }
                            ) {
                                Text(
                                    text = "🌐 Translate",
                                    color = textWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        item {
                            Surface(
                                color = accentKeyBg.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, accentKeyBg),
                                modifier = Modifier.clickable { onShapeText("fix") }
                            ) {
                                Text(
                                    text = "✨ Fix",
                                    color = textWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        item {
                            Surface(
                                color = keyBg,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, keyBorder),
                                modifier = Modifier.clickable { onRetryClick() }
                            ) {
                                Text(
                                    text = "↺ Retry",
                                    color = subscriptGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Quick 1-Tap Paste Pill
                        if (clipboardText.isNotBlank()) {
                            item {
                                Surface(
                                    color = accentKeyBg.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, micButtonGlow),
                                    modifier = Modifier.clickable { onPasteClick() }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "📋 Paste: ${clipboardText.take(14)}${if (clipboardText.length > 14) "…" else ""}",
                                            color = textWhite,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        // Tone Pill
                        item {
                            Surface(
                                color = if (activeTone != "smart_flow") accentKeyBg else keyBg,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, if (activeTone != "smart_flow") micButtonGlow else keyBorder),
                                modifier = Modifier.clickable { onToneCycle() }
                            ) {
                                Text(
                                    text = "⚡ ${formatToneLabel(activeTone)}",
                                    color = textWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Search Pill
                        item {
                            val isSearch = activeTone == "search"
                            Surface(
                                color = if (isSearch) accentKeyBg else keyBg,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, if (isSearch) micButtonGlow else keyBorder),
                                modifier = Modifier.clickable { onToneSelect(if (isSearch) "smart_flow" else "search") }
                            ) {
                                Text(
                                    text = "🔍 Search",
                                    color = if (isSearch) Color.White else toolbarIconTint,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Hindi Pill
                        item {
                            val isHindi = activeTone == "translate_hindi"
                            Surface(
                                color = if (isHindi) accentKeyBg else keyBg,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, if (isHindi) micButtonGlow else keyBorder),
                                modifier = Modifier.clickable { onToneSelect(if (isHindi) "smart_flow" else "translate_hindi") }
                            ) {
                                Text(
                                    text = "🌐 Hindi",
                                    color = if (isHindi) Color.White else toolbarIconTint,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Clear Pill
                        item {
                            Surface(
                                color = keyBg,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, keyBorder),
                                modifier = Modifier.clickable { onClearClick() }
                            ) {
                                Text(
                                    text = "✕ Clear",
                                    color = subscriptGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Snippets
                        items(snippets.keys.toList()) { trigger ->
                            Surface(
                                color = keyBg,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, keyBorder),
                                modifier = Modifier.clickable { onSnippetClick(trigger) }
                            ) {
                                Text(
                                    text = trigger,
                                    color = toolbarIconTint,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Right: Circular Glowing Blue Microphone Button
            val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "micPulseAnim"
            )

            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state == KeyboardState.RECORDING) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(accentRed.copy(alpha = 0.35f))
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (state == KeyboardState.RECORDING) accentRed else actionEnterBg,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { onToggleRecording() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (state == KeyboardState.RECORDING) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (state == KeyboardState.PROCESSING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Dictation",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // -----------------------------------------------------------------------------------------
        // 2. MAIN KEYBOARD BODY: GBOARD QWERTY or ACOUSTIC LISTENING WAVE or TOOLS DRAWER
        // -----------------------------------------------------------------------------------------
        when (state) {
            KeyboardState.RECORDING -> {
                // Listening Waveform within Keyboard Bounds
                GboardListeningView(
                    amplitude = amplitude,
                    accentKeyBg = accentKeyBg,
                    micButtonGlow = micButtonGlow,
                    accentRed = accentRed,
                    textWhite = textWhite,
                    subscriptGray = subscriptGray,
                    onStopClick = onToggleRecording
                )
            }
            KeyboardState.PROCESSING -> {
                // Processing Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = micButtonGlow, strokeWidth = 3.dp, modifier = Modifier.size(38.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = statusMessage.ifBlank { "Refining with Gemini…" }, color = textWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            KeyboardState.TOOLS -> {
                // Tools Drawer
                GboardToolsDrawerView(
                    isBridgeActive = isBridgeActive,
                    isAutoVadActive = isAutoVadActive,
                    desktopBridgeIp = desktopBridgeIp,
                    snippets = snippets,
                    keyBg = keyBg,
                    keyBorder = keyBorder,
                    accentKeyBg = accentKeyBg,
                    textWhite = textWhite,
                    subscriptGray = subscriptGray,
                    onToggleBridge = onToggleBridge,
                    onToggleAutoVad = onToggleAutoVad,
                    onSnippetClick = onSnippetClick,
                    onClose = { onStateChange(KeyboardState.IDLE) }
                )
            }
            else -> {
                // GOOGLE GBOARD FULL-HEIGHT QWERTY KEYBOARD LAYOUT
                GboardQwertyView(
                    keyBg = keyBg,
                    keyBorder = keyBorder,
                    accentKeyBg = accentKeyBg,
                    actionEnterBg = actionEnterBg,
                    textWhite = textWhite,
                    subscriptGray = subscriptGray,
                    onTypeChar = onTypeChar,
                    onBackspaceClick = onBackspaceClick,
                    onEnterClick = onEnterClick,
                    onSpaceClick = onSpaceClick,
                    onSwitchKeyboardClick = onSwitchKeyboardClick
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// GOOGLE GBOARD FULL-HEIGHT QWERTY VIEW (Pixel-perfect key heights & tight 5dp vertical spacing)
// -------------------------------------------------------------------------------------------------
@Composable
private fun GboardQwertyView(
    keyBg: Color,
    keyBorder: Color,
    accentKeyBg: Color,
    actionEnterBg: Color,
    textWhite: Color,
    subscriptGray: Color,
    onTypeChar: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onEnterClick: () -> Unit,
    onSpaceClick: () -> Unit,
    onSwitchKeyboardClick: () -> Unit
) {
    var isShifted by remember { mutableStateOf(false) }
    var isSymbolsMode by remember { mutableStateOf(false) }

    // Row 1 Letters & Top-Right Superscript Numbers (1 to 0)
    val lettersRow1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val numbersRow1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

    val lettersRow2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val lettersRow3 = listOf("z", "x", "c", "v", "b", "n", "m")

    // Symbols Layout
    val symbolsRow1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val symbolsRow2 = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
    val symbolsRow3 = listOf("=", "*", "\"", "'", ":", ";", "!", "?")

    val rowKeyHeight = 46.dp
    val rowVerticalGap = 5.dp
    val keyHorizontalGap = 4.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(rowVerticalGap)
    ) {
        // ROW 1: Q - P with Superscript Numbers 1 - 0
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(keyHorizontalGap)
        ) {
            if (isSymbolsMode) {
                symbolsRow1.forEach { sym ->
                    GboardKey(
                        char = sym,
                        subscript = null,
                        keyBg = keyBg,
                        textColor = textWhite,
                        subscriptColor = subscriptGray,
                        height = rowKeyHeight,
                        modifier = Modifier.weight(1f)
                    ) { onTypeChar(sym) }
                }
            } else {
                lettersRow1.forEachIndexed { index, letter ->
                    val displayChar = if (isShifted) letter.uppercase() else letter
                    GboardKey(
                        char = displayChar,
                        subscript = numbersRow1[index],
                        keyBg = keyBg,
                        textColor = textWhite,
                        subscriptColor = subscriptGray,
                        height = rowKeyHeight,
                        modifier = Modifier.weight(1f)
                    ) {
                        onTypeChar(displayChar)
                        if (isShifted) isShifted = false
                    }
                }
            }
        }

        // ROW 2: A - L (Centered with side spacing matching Gboard)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(keyHorizontalGap)
        ) {
            if (isSymbolsMode) {
                symbolsRow2.forEach { sym ->
                    GboardKey(
                        char = sym,
                        subscript = null,
                        keyBg = keyBg,
                        textColor = textWhite,
                        subscriptColor = subscriptGray,
                        height = rowKeyHeight,
                        modifier = Modifier.weight(1f)
                    ) { onTypeChar(sym) }
                }
            } else {
                lettersRow2.forEach { letter ->
                    val displayChar = if (isShifted) letter.uppercase() else letter
                    GboardKey(
                        char = displayChar,
                        subscript = null,
                        keyBg = keyBg,
                        textColor = textWhite,
                        subscriptColor = subscriptGray,
                        height = rowKeyHeight,
                        modifier = Modifier.weight(1f)
                    ) {
                        onTypeChar(displayChar)
                        if (isShifted) isShifted = false
                    }
                }
            }
        }

        // ROW 3: Shift (Cobalt Blue), Z - M, Backspace (Cobalt Blue)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(keyHorizontalGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shift Key (Cobalt Blue)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isShifted) actionEnterBg else accentKeyBg,
                modifier = Modifier
                    .weight(1.4f)
                    .height(rowKeyHeight)
                    .clickable { isShifted = !isShifted }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "⇧",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Middle Letters or Symbols
            if (isSymbolsMode) {
                symbolsRow3.forEach { sym ->
                    GboardKey(
                        char = sym,
                        subscript = null,
                        keyBg = keyBg,
                        textColor = textWhite,
                        subscriptColor = subscriptGray,
                        height = rowKeyHeight,
                        modifier = Modifier.weight(1f)
                    ) { onTypeChar(sym) }
                }
            } else {
                lettersRow3.forEach { letter ->
                    val displayChar = if (isShifted) letter.uppercase() else letter
                    GboardKey(
                        char = displayChar,
                        subscript = null,
                        keyBg = keyBg,
                        textColor = textWhite,
                        subscriptColor = subscriptGray,
                        height = rowKeyHeight,
                        modifier = Modifier.weight(1f)
                    ) {
                        onTypeChar(displayChar)
                        if (isShifted) isShifted = false
                    }
                }
            }

            // Backspace Key (Cobalt Blue)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentKeyBg,
                modifier = Modifier
                    .weight(1.4f)
                    .height(rowKeyHeight)
                    .clickable { onBackspaceClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ROW 4: ?123, Comma, Emoji/Globe, Wide Space Bar, Period, Enter/Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(keyHorizontalGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ?123 / ABC Toggle Key (Cobalt Blue)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentKeyBg,
                modifier = Modifier
                    .weight(1.35f)
                    .height(rowKeyHeight)
                    .clickable { isSymbolsMode = !isSymbolsMode }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = if (isSymbolsMode) "ABC" else "?123",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Comma Key (Cobalt Blue)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentKeyBg,
                modifier = Modifier
                    .weight(0.9f)
                    .height(rowKeyHeight)
                    .clickable { onTypeChar(",") }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = ",", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Emoji / System Keyboard Switcher
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = keyBg,
                modifier = Modifier
                    .weight(0.9f)
                    .height(rowKeyHeight)
                    .clickable { onSwitchKeyboardClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = "☺", color = textWhite, fontSize = 16.sp)
                }
            }

            // Wide Gboard Space Bar with "OwnVoice" Watermark
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = keyBg,
                modifier = Modifier
                    .weight(4.2f)
                    .height(rowKeyHeight)
                    .clickable { onSpaceClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "OwnVoice",
                        color = textWhite.copy(alpha = 0.45f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // Period Key (Cobalt Blue)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentKeyBg,
                modifier = Modifier
                    .weight(0.9f)
                    .height(rowKeyHeight)
                    .clickable { onTypeChar(".") }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = ".", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Action / Enter / Search Key (Vibrant Cobalt Blue)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = actionEnterBg,
                modifier = Modifier
                    .weight(1.35f)
                    .height(rowKeyHeight)
                    .clickable { onEnterClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search or Enter",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// GBOARD KEY COMPOSABLE (With Top-Right Number Subscript)
// -------------------------------------------------------------------------------------------------
@Composable
private fun GboardKey(
    char: String,
    subscript: String?,
    keyBg: Color,
    textColor: Color,
    subscriptColor: Color,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = keyBg,
        modifier = modifier
            .height(height)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Main Character Glyph
            Text(
                text = char,
                color = textColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal
            )

            // Top-Right Superscript Number
            if (subscript != null) {
                Text(
                    text = subscript,
                    color = subscriptColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 4.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 4-SQUARE APPS GRID ICON (Vector Canvas)
// -------------------------------------------------------------------------------------------------
@Composable
private fun AppsGridIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier.size(17.dp)) {
        val s = size.width
        val squareSize = s * 0.40f
        val gap = s * 0.20f
        val r = 2.dp.toPx()

        // Top-Left
        drawRoundRect(tint, Offset(0f, 0f), Size(squareSize, squareSize), CornerRadius(r, r))
        // Top-Right
        drawRoundRect(tint, Offset(squareSize + gap, 0f), Size(squareSize, squareSize), CornerRadius(r, r))
        // Bottom-Left
        drawRoundRect(tint, Offset(0f, squareSize + gap), Size(squareSize, squareSize), CornerRadius(r, r))
        // Bottom-Right
        drawRoundRect(tint, Offset(squareSize + gap, squareSize + gap), Size(squareSize, squareSize), CornerRadius(r, r))
    }
}

// -------------------------------------------------------------------------------------------------
// LISTENING VIEW (Non-blocking fluid acoustic waveform)
// -------------------------------------------------------------------------------------------------
@Composable
private fun GboardListeningView(
    amplitude: Float,
    accentKeyBg: Color,
    micButtonGlow: Color,
    accentRed: Color,
    textWhite: Color,
    subscriptGray: Color,
    onStopClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "listeningWave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseAnim"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Listening… Speak naturally",
            color = textWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Fluid Waveform Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val midY = height / 2f
                val activeAmp = (amplitude * 1.5f).coerceIn(0.12f, 1.0f)

                val path = Path()
                path.moveTo(0f, midY)
                val points = 70
                for (i in 0..points) {
                    val x = (i.toFloat() / points) * width
                    val angle = (i.toFloat() / points) * 3.5f * Math.PI.toFloat() + phase
                    val envelope = sin((i.toFloat() / points) * Math.PI.toFloat())
                    val y = midY + sin(angle) * (height * 0.40f * activeAmp * envelope)
                    path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        listOf(micButtonGlow.copy(alpha = 0.3f), micButtonGlow, Color(0xFF60A5FA))
                    ),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Center Stop Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accentRed,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(56.dp)
                    .clickable { onStopClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop recording",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Tap to finish", color = subscriptGray, fontSize = 11.sp)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TOOLS DRAWER VIEW
// -------------------------------------------------------------------------------------------------
@Composable
private fun GboardToolsDrawerView(
    isBridgeActive: Boolean,
    isAutoVadActive: Boolean,
    desktopBridgeIp: String,
    snippets: Map<String, String>,
    keyBg: Color,
    keyBorder: Color,
    accentKeyBg: Color,
    textWhite: Color,
    subscriptGray: Color,
    onToggleBridge: () -> Unit,
    onToggleAutoVad: () -> Unit,
    onSnippetClick: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(215.dp)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🛠️ OwnVoice Tools", color = textWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Surface(
                color = keyBg,
                shape = CircleShape,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onClose() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = textWhite, modifier = Modifier.size(14.dp))
                }
            }
        }

        // Feature cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // PC Bridge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isBridgeActive) Color(0xFF00C7BE).copy(alpha = 0.25f) else keyBg,
                border = BorderStroke(1.dp, if (isBridgeActive) Color(0xFF00C7BE) else keyBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(55.dp)
                    .clickable { onToggleBridge() }
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.Center) {
                    Text(text = if (isBridgeActive) "💻 PC: ON" else "💻 PC Bridge", color = textWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = if (desktopBridgeIp.isNotBlank()) desktopBridgeIp else "Tap to link", color = subscriptGray, fontSize = 10.sp)
                }
            }

            // Auto VAD
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isAutoVadActive) Color(0xFF34C759).copy(alpha = 0.25f) else keyBg,
                border = BorderStroke(1.dp, if (isAutoVadActive) Color(0xFF34C759) else keyBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(55.dp)
                    .clickable { onToggleAutoVad() }
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.Center) {
                    Text(text = if (isAutoVadActive) "🎙️ Auto: ON" else "🎙️ Auto VAD", color = textWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = "1.2s silence commit", color = subscriptGray, fontSize = 10.sp)
                }
            }
        }

        // Snippets List
        Text(text = "Personal Snippets", color = subscriptGray, fontSize = 11.sp)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
        ) {
            items(snippets.keys.toList()) { trigger ->
                Surface(
                    color = keyBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, keyBorder),
                    modifier = Modifier.clickable {
                        onSnippetClick(trigger)
                        onClose()
                    }
                ) {
                    Text(text = trigger, color = textWhite, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }

        Button(
            onClick = { onClose() },
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentKeyBg)
        ) {
            Text(text = "Back to Keyboard", color = Color.White, fontSize = 12.sp)
        }
    }
}
