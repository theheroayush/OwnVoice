package com.example.ownvoice.ime.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class WandOption(
    val id: String,
    val label: String,
    val icon: String,
    val description: String
)

val defaultWandOptions = listOf(
    WandOption("fix", "Polish & Fix", "✨", "Fix grammar, typos & punctuation"),
    WandOption("executive", "Executive", "💼", "Professional workplace communication"),
    WandOption("shorter", "Make Shorter", "⚡", "Condense into punchy sentences"),
    WandOption("translate_hindi", "Hindi ↔ EN", "🌐", "Translate between Hindi and English"),
    WandOption("bullets", "Bullet Points", "📝", "Convert text into clean bullet points"),
    WandOption("casual", "Casual", "💬", "Friendly, conversational tone")
)

@Composable
fun MagicWandPalette(
    onSelectOption: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillBg = Color(0xFF1E283D)
    val pillBorder = Color(0xFF3B82F6)
    val textWhite = Color(0xFFF0F4F8)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Wand Icon & Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 6.dp)
        ) {
            Text(text = "🪄", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Magic Wand",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Horizontal Options
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(defaultWandOptions) { option ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = pillBg,
                    border = BorderStroke(1.dp, pillBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.clickable { onSelectOption(option.id) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = option.icon, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = option.label,
                            color = textWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Close Button
        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color(0xFFBAC7DE),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
