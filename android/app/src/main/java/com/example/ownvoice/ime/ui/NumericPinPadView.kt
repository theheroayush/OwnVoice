package com.example.ownvoice.ime.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NumericPinPadView(
    keyBg: Color,
    keyBorder: Color,
    accentKeyBg: Color,
    actionEnterBg: Color,
    textWhite: Color,
    actionKeyLabel: String,
    onTypeChar: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onEnterClick: () -> Unit,
    onSwitchToQwerty: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowKeyHeight = 52.dp
    val spacing = 6.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // Row 1: 1, 2, 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            listOf("1", "2", "3").forEach { digit ->
                PinKey(
                    text = digit,
                    keyBg = keyBg,
                    textColor = textWhite,
                    keyBorder = keyBorder,
                    height = rowKeyHeight,
                    modifier = Modifier.weight(1f),
                    onClick = { onTypeChar(digit) }
                )
            }
        }

        // Row 2: 4, 5, 6
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            listOf("4", "5", "6").forEach { digit ->
                PinKey(
                    text = digit,
                    keyBg = keyBg,
                    textColor = textWhite,
                    keyBorder = keyBorder,
                    height = rowKeyHeight,
                    modifier = Modifier.weight(1f),
                    onClick = { onTypeChar(digit) }
                )
            }
        }

        // Row 3: 7, 8, 9
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            listOf("7", "8", "9").forEach { digit ->
                PinKey(
                    text = digit,
                    keyBg = keyBg,
                    textColor = textWhite,
                    keyBorder = keyBorder,
                    height = rowKeyHeight,
                    modifier = Modifier.weight(1f),
                    onClick = { onTypeChar(digit) }
                )
            }
        }

        // Row 4: ABC, 0, Backspace, Action/Enter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ABC switch key
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentKeyBg,
                modifier = Modifier
                    .weight(1f)
                    .height(rowKeyHeight)
                    .clickable { onSwitchToQwerty() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "ABC",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 0 Key
            PinKey(
                text = "0",
                keyBg = keyBg,
                textColor = textWhite,
                keyBorder = keyBorder,
                height = rowKeyHeight,
                modifier = Modifier.weight(1f),
                onClick = { onTypeChar("0") }
            )

            // Backspace Key
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentKeyBg,
                modifier = Modifier
                    .weight(1f)
                    .height(rowKeyHeight)
                    .clickable { onBackspaceClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Enter / Action Key
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = actionEnterBg,
                modifier = Modifier
                    .weight(1f)
                    .height(rowKeyHeight)
                    .clickable { onEnterClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = actionKeyLabel,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PinKey(
    text: String,
    keyBg: Color,
    textColor: Color,
    keyBorder: Color,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = keyBg,
        border = BorderStroke(1.dp, keyBorder),
        modifier = modifier
            .height(height)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = text,
                color = textColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
