package com.example.ownvoice.ime.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ownvoice.ime.model.ActionKeyType
import com.example.ownvoice.ime.model.ClipboardItem
import com.example.ownvoice.ime.model.KeyboardLayoutMode
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.sin

enum class KeyboardState {
    IDLE,
    RECORDING,
    PROCESSING,
    RESULT,
    TOOLS,
    ERROR,
    EMOJI,
    CLIPBOARD,
    WAND,
    TRACKPAD
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
    clipboardItems: List<ClipboardItem> = emptyList(),
    layoutMode: KeyboardLayoutMode = KeyboardLayoutMode.QWERTY,
    actionKeyType: ActionKeyType = ActionKeyType.ENTER,
    isBridgeActive: Boolean = false,
    isAutoVadActive: Boolean = false,
    desktopBridgeIp: String = "",
    pcClipboardText: String? = null,
    bridgeClient: com.example.ownvoice.network.BridgeClient? = null,
    onPastePcClipboard: (String) -> Unit = {},
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
    onTypeChar: (String) -> Unit = {},
    onSpacebarGlide: (Int) -> Unit = {},
    onBackspaceScrub: (Int) -> Unit = {},
    onBackspaceScrubCommit: (Int) -> Unit = {},
    onWandTransform: (String) -> Unit = {},
    onTogglePinClip: (String) -> Unit = {},
    onDeleteClip: (String) -> Unit = {},
    onSendClipToPc: (String) -> Unit = {}
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

    var activeMagnifierChar by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 275.dp, max = 310.dp)
            .background(gboardBg)
            .navigationBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // -----------------------------------------------------------------------------------------
            // 1. TOP TOOLBAR: Apps Grid, Magic Wand, Clipboard, Tone/Bridge Pills, Mic
            // -----------------------------------------------------------------------------------------
            if (state == KeyboardState.WAND) {
                MagicWandPalette(
                    onSelectOption = { optionId ->
                        onWandTransform(optionId)
                        onStateChange(KeyboardState.IDLE)
                    },
                    onClose = { onStateChange(KeyboardState.IDLE) }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: 4-Square Grid Apps Icon
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                if (state == KeyboardState.TOOLS) onStateChange(KeyboardState.IDLE)
                                else onStateChange(KeyboardState.TOOLS)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            AppsGridIcon(tint = toolbarIconTint)
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Center: Scrollable Pills (Shape with AI, Magic Wand, PC Air-Typing, Clips)
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
                                        Text(text = "⚡ Shorter", color = textWhite, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                                item {
                                    Surface(
                                        color = accentKeyBg.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, accentKeyBg),
                                        modifier = Modifier.clickable { onShapeText("executive") }
                                    ) {
                                        Text(text = "💼 Executive", color = textWhite, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                                item {
                                    Surface(
                                        color = accentKeyBg.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, accentKeyBg),
                                        modifier = Modifier.clickable { onShapeText("fix") }
                                    ) {
                                        Text(text = "✨ Fix", color = textWhite, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                                item {
                                    Surface(
                                        color = keyBg,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, keyBorder),
                                        modifier = Modifier.clickable { onRetryClick() }
                                    ) {
                                        Text(text = "↺ Retry", color = subscriptGray, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // 1. Prominent Magic Wand Pill (In-situ AI)
                                item {
                                    Surface(
                                        color = Color(0xFF7C3AED).copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, Color(0xFFA78BFA)),
                                        modifier = Modifier.clickable {
                                            onStateChange(if (state == KeyboardState.WAND) KeyboardState.IDLE else KeyboardState.WAND)
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                        ) {
                                            Text(text = "🪄", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "Wand", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // 2. Prominent PC Bridge / Air-Typing Pill
                                item {
                                    val isPaired = desktopBridgeIp.isNotBlank()
                                    val pcColor = if (isBridgeActive) Color(0xFF00C7BE) else keyBorder
                                    val dotColor = if (isBridgeActive) Color(0xFF34C759) else if (isPaired) Color(0xFF8E8E93) else Color(0xFFFF9500)
                                    val pcLabel = when {
                                        isBridgeActive -> "💻 Air-Type: ON"
                                        isPaired -> "💻 PC: OFF"
                                        else -> "💻 PC Link"
                                    }
                                    Surface(
                                        color = if (isBridgeActive) Color(0xFF00C7BE).copy(alpha = 0.22f) else keyBg,
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, pcColor),
                                        modifier = Modifier.clickable { onToggleBridge() }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = dotColor,
                                                modifier = Modifier.size(6.dp)
                                            ) {}
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = pcLabel,
                                                color = if (isBridgeActive) Color.White else toolbarIconTint,
                                                fontSize = 11.sp,
                                                fontWeight = if (isBridgeActive) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                // 2b. Remote Trackpad & Air Mouse Pill
                                item {
                                    val isTrackpad = state == KeyboardState.TRACKPAD
                                    Surface(
                                        color = if (isTrackpad) Color(0xFF2563EB) else keyBg,
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, if (isTrackpad) Color(0xFF60A5FA) else keyBorder),
                                        modifier = Modifier.clickable {
                                            onStateChange(if (isTrackpad) KeyboardState.IDLE else KeyboardState.TRACKPAD)
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                        ) {
                                            Text(text = "🖱️", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Trackpad",
                                                color = textWhite,
                                                fontSize = 11.sp,
                                                fontWeight = if (isTrackpad) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                // 2c. Real-Time "From PC" Clipboard Chip
                                if (!pcClipboardText.isNullOrBlank()) {
                                    item {
                                        val preview = if (pcClipboardText.length > 14) pcClipboardText.take(14) + "…" else pcClipboardText
                                        Surface(
                                            color = Color(0xFF00C7BE).copy(alpha = 0.28f),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, Color(0xFF00C7BE)),
                                            modifier = Modifier.clickable { onPastePcClipboard(pcClipboardText) }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                            ) {
                                                Text(text = "💻", fontSize = 11.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "PC: \"$preview\"",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                // 3. Clipboard Drawer Pill
                                item {
                                    val hasClips = clipboardItems.isNotEmpty() || clipboardText.isNotBlank()
                                    val isClipOpen = state == KeyboardState.CLIPBOARD
                                    Surface(
                                        color = if (isClipOpen) accentKeyBg else keyBg,
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, if (isClipOpen) micButtonGlow else keyBorder),
                                        modifier = Modifier.clickable {
                                            onStateChange(if (isClipOpen) KeyboardState.IDLE else KeyboardState.CLIPBOARD)
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = if (clipboardText.isNotBlank()) "📋 Paste: ${clipboardText.take(10)}…" else "📋 Clips",
                                                color = textWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                // 4. Tone Pill
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

                                // 5. Search Pill
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

                                // 6. Hindi Pill
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
            }

            Spacer(modifier = Modifier.height(3.dp))

            // -----------------------------------------------------------------------------------------
            // 2. MAIN KEYBOARD BODY: QWERTY, PIN PAD, EMOJI, CLIPBOARD, LISTENING, OR TOOLS
            // -----------------------------------------------------------------------------------------
            when {
                state == KeyboardState.RECORDING -> {
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
                state == KeyboardState.PROCESSING -> {
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
                state == KeyboardState.TRACKPAD -> {
                    TrackpadView(
                        bridgeClient = bridgeClient,
                        isPcConnected = isBridgeActive,
                        onSwitchToKeyboard = { onStateChange(KeyboardState.IDLE) }
                    )
                }
                state == KeyboardState.TOOLS -> {
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
                state == KeyboardState.EMOJI -> {
                    EmojiPickerView(
                        keyBg = keyBg,
                        accentKeyBg = accentKeyBg,
                        textWhite = textWhite,
                        onEmojiSelected = { onTypeChar(it) },
                        onBackspaceClick = onBackspaceClick,
                        onSwitchToQwerty = { onStateChange(KeyboardState.IDLE) }
                    )
                }
                state == KeyboardState.CLIPBOARD -> {
                    ClipboardHistoryDrawer(
                        items = clipboardItems,
                        isBridgeActive = isBridgeActive,
                        onPasteItem = { clipText ->
                            onPasteClick()
                            onStateChange(KeyboardState.IDLE)
                        },
                        onSendToPc = onSendClipToPc,
                        onTogglePin = onTogglePinClip,
                        onDeleteItem = onDeleteClip,
                        onClose = { onStateChange(KeyboardState.IDLE) }
                    )
                }
                layoutMode == KeyboardLayoutMode.NUMERIC_PAD -> {
                    NumericPinPadView(
                        keyBg = keyBg,
                        keyBorder = keyBorder,
                        accentKeyBg = accentKeyBg,
                        actionEnterBg = actionEnterBg,
                        textWhite = textWhite,
                        actionKeyLabel = actionKeyType.defaultLabel,
                        onTypeChar = onTypeChar,
                        onBackspaceClick = onBackspaceClick,
                        onEnterClick = onEnterClick,
                        onSwitchToQwerty = onSwitchKeyboardClick
                    )
                }
                else -> {
                    // FULL-HEIGHT QWERTY WITH GLIDE, SWIPE-DELETE, AND ADAPTIVE EMAIL KEYS
                    GboardQwertyView(
                        keyBg = keyBg,
                        keyBorder = keyBorder,
                        accentKeyBg = accentKeyBg,
                        actionEnterBg = actionEnterBg,
                        textWhite = textWhite,
                        subscriptGray = subscriptGray,
                        layoutMode = layoutMode,
                        actionKeyType = actionKeyType,
                        onTypeChar = onTypeChar,
                        onBackspaceClick = onBackspaceClick,
                        onEnterClick = onEnterClick,
                        onSpaceClick = onSpaceClick,
                        onSwitchKeyboardClick = onSwitchKeyboardClick,
                        onOpenEmoji = { onStateChange(KeyboardState.EMOJI) },
                        onSpacebarGlide = onSpacebarGlide,
                        onBackspaceScrub = onBackspaceScrub,
                        onBackspaceScrubCommit = onBackspaceScrubCommit,
                        onKeyPressed = { char, isDown ->
                            activeMagnifierChar = if (isDown) char else ""
                        }
                    )
                }
            }
        }

        // Key Magnifier Bubble Overlay (Top-Center of Keyboard when a key is pressed)
        if (activeMagnifierChar.isNotBlank() && state == KeyboardState.IDLE) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-4).dp)
            ) {
                KeyMagnifierBubble(char = activeMagnifierChar)
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// GOOGLE GBOARD QWERTY VIEW WITH GLIDE, SWIPE-DELETE, AND LONG-PRESS SYMBOLS
// -------------------------------------------------------------------------------------------------
@Composable
private fun GboardQwertyView(
    keyBg: Color,
    keyBorder: Color,
    accentKeyBg: Color,
    actionEnterBg: Color,
    textWhite: Color,
    subscriptGray: Color,
    layoutMode: KeyboardLayoutMode,
    actionKeyType: ActionKeyType,
    onTypeChar: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onEnterClick: () -> Unit,
    onSpaceClick: () -> Unit,
    onSwitchKeyboardClick: () -> Unit,
    onOpenEmoji: () -> Unit,
    onSpacebarGlide: (Int) -> Unit,
    onBackspaceScrub: (Int) -> Unit,
    onBackspaceScrubCommit: (Int) -> Unit,
    onKeyPressed: (String, Boolean) -> Unit
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
    val symbolsRow3 = listOf("=", "*", "\u0022", "'", ":", ";", "!", "?")

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
                        modifier = Modifier.weight(1f),
                        onPressed = { onKeyPressed(sym, it) }
                    ) { onTypeChar(sym) }
                }
            } else {
                lettersRow1.forEachIndexed { index, letter ->
                    val displayChar = if (isShifted) letter.uppercase() else letter
                    val secondary = LongPressSymbolMenu.getSecondarySymbol(letter)
                    GboardKey(
                        char = displayChar,
                        subscript = numbersRow1[index],
                        keyBg = keyBg,
                        textColor = textWhite,
                        subscriptColor = subscriptGray,
                        height = rowKeyHeight,
                        modifier = Modifier.weight(1f),
                        onLongPress = {
                            if (secondary != null) {
                                onTypeChar(secondary)
                            } else {
                                onTypeChar(displayChar)
                            }
                        },
                        onPressed = { onKeyPressed(displayChar, it) }
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
                .padding(horizontal = 14.dp),
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
                        modifier = Modifier.weight(1f),
                        onPressed = { onKeyPressed(sym, it) }
                    ) { onTypeChar(sym) }
                }
            } else {
                lettersRow2.forEach { letter ->
                    val displayChar = if (isShifted) letter.uppercase() else letter
                    val secondary = LongPressSymbolMenu.getSecondarySymbol(letter)
                    GboardKey(
                        char = displayChar,
                        subscript = secondary,
                        keyBg = keyBg,
                        textColor = textWhite,
                        subscriptColor = subscriptGray,
                        height = rowKeyHeight,
                        modifier = Modifier.weight(1f),
                        onLongPress = {
                            if (secondary != null) {
                                onTypeChar(secondary)
                            } else {
                                onTypeChar(displayChar)
                            }
                        },
                        onPressed = { onKeyPressed(displayChar, it) }
                    ) {
                        onTypeChar(displayChar)
                        if (isShifted) isShifted = false
                    }
                }
            }
        }

        // ROW 3: Shift, Z - M, Backspace (with Swipe-to-Delete)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(keyHorizontalGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shift Key
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isShifted) actionEnterBg else accentKeyBg,
                modifier = Modifier
                    .weight(1.4f)
                    .height(rowKeyHeight)
                    .pointerInput(isShifted) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            isShifted = !isShifted
                            waitForUpOrCancellation()
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = "⇧", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Letters or Symbols
            if (isSymbolsMode) {
                symbolsRow3.forEach { sym ->
                    GboardKey(
                        char = sym,
                        subscript = null,
                        keyBg = keyBg,
                        textColor = textWhite,
                        subscriptColor = subscriptGray,
                        height = rowKeyHeight,
                        modifier = Modifier.weight(1f),
                        onPressed = { onKeyPressed(sym, it) }
                    ) { onTypeChar(sym) }
                }
            } else {
                lettersRow3.forEach { letter ->
                    val displayChar = if (isShifted) letter.uppercase() else letter
                    val secondary = LongPressSymbolMenu.getSecondarySymbol(letter)
                    GboardKey(
                        char = displayChar,
                        subscript = secondary,
                        keyBg = keyBg,
                        textColor = textWhite,
                        subscriptColor = subscriptGray,
                        height = rowKeyHeight,
                        modifier = Modifier.weight(1f),
                        onLongPress = {
                            if (secondary != null) {
                                onTypeChar(secondary)
                            } else {
                                onTypeChar(displayChar)
                            }
                        },
                        onPressed = { onKeyPressed(displayChar, it) }
                    ) {
                        onTypeChar(displayChar)
                        if (isShifted) isShifted = false
                    }
                }
            }

            // Backspace Key with Swipe-to-Delete Scrubbing
            GboardBackspaceKey(
                accentKeyBg = accentKeyBg,
                height = rowKeyHeight,
                modifier = Modifier.weight(1.4f),
                onBackspaceClick = onBackspaceClick,
                onBackspaceScrub = onBackspaceScrub,
                onBackspaceScrubCommit = onBackspaceScrubCommit
            )
        }

        // ROW 4: ?123, Comma, Emoji, Space Bar (with Glide), Period (or @/.com), Action/Enter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(keyHorizontalGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ?123 / ABC Toggle Key
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentKeyBg,
                modifier = Modifier
                    .weight(1.3f)
                    .height(rowKeyHeight)
                    .pointerInput(isSymbolsMode) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            isSymbolsMode = !isSymbolsMode
                            waitForUpOrCancellation()
                        }
                    }
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

            // Comma Key
            var isCommaPressed by remember { mutableStateOf(false) }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isCommaPressed) accentKeyBg.copy(alpha = 0.72f) else accentKeyBg,
                modifier = Modifier
                    .weight(0.9f)
                    .height(rowKeyHeight)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            isCommaPressed = true
                            onTypeChar(",")
                            waitForUpOrCancellation()
                            isCommaPressed = false
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = ",", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Emoji Picker Switcher
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = keyBg,
                modifier = Modifier
                    .weight(0.9f)
                    .height(rowKeyHeight)
                    .clickable { onOpenEmoji() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = "☺", color = textWhite, fontSize = 16.sp)
                }
            }

            // Space Bar with Trackpad Cursor Glide
            GboardSpacebarKey(
                keyBg = keyBg,
                height = rowKeyHeight,
                modifier = Modifier.weight(if (layoutMode == KeyboardLayoutMode.EMAIL) 3.2f else 4.2f),
                onSpaceClick = onSpaceClick,
                onSpacebarGlide = onSpacebarGlide
            )

            // Email Layout Extra Key: "@" or ".com"
            if (layoutMode == KeyboardLayoutMode.EMAIL) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentKeyBg,
                    modifier = Modifier
                        .weight(1.0f)
                        .height(rowKeyHeight)
                        .clickable { onTypeChar("@") }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(text = "@", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Period Key
            var isPeriodPressed by remember { mutableStateOf(false) }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isPeriodPressed) accentKeyBg.copy(alpha = 0.72f) else accentKeyBg,
                modifier = Modifier
                    .weight(0.9f)
                    .height(rowKeyHeight)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            isPeriodPressed = true
                            onTypeChar(".")
                            waitForUpOrCancellation()
                            isPeriodPressed = false
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = ".", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Action / Enter Key with Adaptive Icon
            var isEnterPressed by remember { mutableStateOf(false) }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isEnterPressed) actionEnterBg.copy(alpha = 0.75f) else actionEnterBg,
                modifier = Modifier
                    .weight(1.35f)
                    .height(rowKeyHeight)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            isEnterPressed = true
                            onEnterClick()
                            waitForUpOrCancellation()
                            isEnterPressed = false
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    when (actionKeyType) {
                        ActionKeyType.SEARCH -> Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(20.dp))
                        ActionKeyType.DONE -> Icon(imageVector = Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(20.dp))
                        ActionKeyType.SEND -> Text(text = "✈", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        ActionKeyType.NEXT, ActionKeyType.GO -> Text(text = "➔", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        else -> Text(text = "↵", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// GBOARD SPACEBAR KEY COMPOSABLE WITH TRACKPAD CURSOR GLIDE & PRECAUTION
// -------------------------------------------------------------------------------------------------
@Composable
private fun GboardSpacebarKey(
    keyBg: Color,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onSpaceClick: () -> Unit,
    onSpacebarGlide: (Int) -> Unit
) {
    var isGliding by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val deadzonePx = with(density) { 12.dp.toPx() }
    val stepPx = with(density) { 10.dp.toPx() }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isGliding) Color(0xFF2563EB).copy(alpha = 0.25f) else if (isPressed) keyBg.copy(alpha = 0.72f) else keyBg,
        border = if (isGliding) BorderStroke(1.5.dp, Color(0xFF3B82F6)) else null,
        modifier = modifier
            .height(height)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    var lastStepX = startX
                    var currentX = startX
                    isPressed = true
                    isGliding = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            // Finger lifted!
                            if (!isGliding) {
                                onSpaceClick()
                            }
                            break
                        }

                        currentX = change.position.x
                        val totalDeltaX = currentX - startX

                        // Precaution threshold: Require 12dp horizontal drag before locking into glide
                        if (!isGliding && abs(totalDeltaX) > deadzonePx) {
                            isGliding = true
                            lastStepX = currentX
                        }

                        if (isGliding) {
                            val stepDeltaX = currentX - lastStepX
                            if (abs(stepDeltaX) >= stepPx) {
                                val steps = (stepDeltaX / stepPx).toInt()
                                if (steps != 0) {
                                    onSpacebarGlide(steps)
                                    lastStepX = currentX
                                }
                            }
                        }
                    }
                    isPressed = false
                    isGliding = false
                }
            }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (isGliding) {
                Text(
                    text = "◄   Slide to move cursor   ►",
                    color = Color(0xFF60A5FA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = "OwnVoice",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// GBOARD BACKSPACE KEY WITH SWIPE-TO-DELETE (WORD SCRUBBING)
// -------------------------------------------------------------------------------------------------
@Composable
private fun GboardBackspaceKey(
    accentKeyBg: Color,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onBackspaceClick: () -> Unit,
    onBackspaceScrub: (Int) -> Unit,
    onBackspaceScrubCommit: (Int) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    var scrubWordCount by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    val scrubThresholdPx = with(density) { 15.dp.toPx() }
    val wordStepPx = with(density) { 26.dp.toPx() }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (scrubWordCount > 0) Color(0xFFDC2626) else if (isPressed) accentKeyBg.copy(alpha = 0.72f) else accentKeyBg,
        modifier = modifier
            .height(height)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    isPressed = true
                    scrubWordCount = 0

                    // Initial single backspace on tap
                    onBackspaceClick()

                    var isHoldRepeating = false
                    val holdJob = withTimeoutOrNull(350L) {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break

                            val currentX = change.position.x
                            val dragLeft = startX - currentX

                            // Swiping left to scrub words
                            if (dragLeft > scrubThresholdPx) {
                                val words = ((dragLeft - scrubThresholdPx) / wordStepPx).toInt() + 1
                                scrubWordCount = words.coerceIn(1, 15)
                                onBackspaceScrub(scrubWordCount)
                            } else if (scrubWordCount > 0 && dragLeft <= scrubThresholdPx) {
                                // Slide back to cancel
                                scrubWordCount = 0
                                onBackspaceScrub(0)
                            }
                        }
                    }

                    // If user was just holding without swiping, repeat backspace
                    if (scrubWordCount == 0 && holdJob == null) {
                        val holdStart = System.currentTimeMillis()
                        while (true) {
                            val elapsed = System.currentTimeMillis() - holdStart
                            val interval = if (elapsed > 1200L) 25L else 45L
                            val released = withTimeoutOrNull(interval) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull()
                                if (change != null && !change.pressed) change else null
                            }
                            if (released != null) break
                            onBackspaceClick()
                        }
                    }

                    // On finger lift: commit word scrub if active
                    if (scrubWordCount > 0) {
                        onBackspaceScrubCommit(scrubWordCount)
                    }

                    isPressed = false
                    scrubWordCount = 0
                }
            }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (scrubWordCount > 0) {
                Text(
                    text = "-$scrubWordCount w",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// GBOARD KEY COMPOSABLE (With Long-Press Symbols and Magnifier Hooks)
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
    onLongPress: (() -> Unit)? = null,
    onPressed: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val effectiveBg = if (isPressed) keyBg.copy(alpha = 0.72f) else keyBg

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = effectiveBg,
        modifier = modifier
            .height(height)
            .pointerInput(char, onClick) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    onPressed?.invoke(true)

                    // Long press timeout: 260ms
                    val upOrCancel = withTimeoutOrNull(260L) {
                        waitForUpOrCancellation()
                    }

                    if (upOrCancel == null) {
                        // Long press fired!
                        onLongPress?.invoke() ?: onClick()
                        waitForUpOrCancellation()
                    } else {
                        // Regular tap
                        onClick()
                    }
                    isPressed = false
                    onPressed?.invoke(false)
                }
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = char,
                color = textColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal
            )

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
// 4-SQUARE APPS GRID ICON
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

        drawRoundRect(tint, Offset(0f, 0f), Size(squareSize, squareSize), CornerRadius(r, r))
        drawRoundRect(tint, Offset(squareSize + gap, 0f), Size(squareSize, squareSize), CornerRadius(r, r))
        drawRoundRect(tint, Offset(0f, squareSize + gap), Size(squareSize, squareSize), CornerRadius(r, r))
        drawRoundRect(tint, Offset(squareSize + gap, squareSize + gap), Size(squareSize, squareSize), CornerRadius(r, r))
    }
}

// -------------------------------------------------------------------------------------------------
// LISTENING VIEW
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
