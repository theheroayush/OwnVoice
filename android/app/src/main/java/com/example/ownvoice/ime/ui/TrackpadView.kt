package com.example.ownvoice.ime.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ownvoice.network.BridgeClient

@Composable
fun TrackpadView(
    bridgeClient: BridgeClient?,
    isPcConnected: Boolean = true,
    onSwitchToKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var isDragLocked by remember { mutableStateOf(false) }
    var sensitivity by remember { mutableStateOf(1.2f) }

    val darkBg = Color(0xFF0D1117)
    val padBg = Color(0xFF161D2B)
    val padBorder = Color(0xFF30363D)
    val buttonBg = Color(0xFF1E293B)
    val activeBlue = Color(0xFF2563EB)
    val textWhite = Color(0xFFF1F5F9)
    val textMuted = Color(0xFF94A3B8)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .background(darkBg)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (isPcConnected) Color(0xFF34C759) else Color(0xFFFF9500),
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPcConnected) "PC Connected" else "Pairing Needed",
                    color = textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Sensitivity Picker
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(0.8f to "0.8x", 1.2f to "1.2x", 1.8f to "1.8x").forEach { (speed, label) ->
                    val isSelected = (sensitivity == speed)
                    Surface(
                        color = if (isSelected) activeBlue else buttonBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable {
                            sensitivity = speed
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) textWhite else textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Return to Keyboard Button
            Surface(
                color = buttonBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, padBorder),
                modifier = Modifier.clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onSwitchToKeyboard()
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Keyboard",
                        tint = textWhite,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Keys", color = textWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Center Surface: Main Trackpad + Dedicated Vertical Scroll Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Main Touchpad Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(padBg)
                    .pointerInput(sensitivity) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            bridgeClient?.sendMouseMove(dragAmount.x * sensitivity, dragAmount.y * sensitivity)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                bridgeClient?.sendMouseClick("left")
                            },
                            onDoubleTap = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                bridgeClient?.sendMouseClick("left", double = true)
                            },
                            onLongPress = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                bridgeClient?.sendMouseClick("right")
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Glide to move cursor\nTap to click • Long-press to right-click",
                    color = textMuted.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Dedicated High-Precision Vertical Scroll Strip
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(buttonBg)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Positive dy dragged downwards scrolls down (-1)
                            val delta = if (dragAmount.y < 0) 1 else -1
                            bridgeClient?.sendMouseScroll(delta.toFloat())
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▲\n\nS\nC\nR\nO\nL\nL\n\n▼",
                    color = textMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom Hardware Buttons Row (Left Click, Drag Lock, Right Click)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Left Click Bar
            Surface(
                color = buttonBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, padBorder),
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        bridgeClient?.sendMouseClick("left")
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "Left Click", color = textWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Drag Lock Button
            Surface(
                color = if (isDragLocked) Color(0xFFEF4444) else buttonBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isDragLocked) Color(0xFFF87171) else padBorder),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable {
                        isDragLocked = !isDragLocked
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        if (isDragLocked) {
                            bridgeClient?.sendMouseDown("left")
                        } else {
                            bridgeClient?.sendMouseUp("left")
                        }
                    }
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isDragLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Drag Lock",
                        tint = textWhite,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isDragLocked) "Locked" else "Drag",
                        color = textWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Right Click Bar
            Surface(
                color = buttonBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, padBorder),
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        bridgeClient?.sendMouseClick("right")
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "Right Click", color = textWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
