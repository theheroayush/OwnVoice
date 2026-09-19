package com.example.ownvoice.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ownvoice.ime.model.EmojiCategory
import com.example.ownvoice.ime.model.EmojiData

@Composable
fun EmojiPickerView(
    keyBg: Color,
    accentKeyBg: Color,
    textWhite: Color,
    onEmojiSelected: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onSwitchToQwerty: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(EmojiCategory.SMILEYS) }
    val currentEmojis = EmojiData.categories[selectedCategory] ?: emptyList()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Category Selector Tab Rail
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(EmojiCategory.values()) { category ->
                val isSelected = category == selectedCategory
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) accentKeyBg else Color.Transparent,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { selectedCategory = category }
                ) {
                    Text(
                        text = "${category.icon} ${category.label}",
                        color = if (isSelected) Color.White else Color(0xFFBAC7DE),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Emoji Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(currentEmojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 22.sp)
                }
            }
        }

        // Bottom Controls: ABC switch, Spacebar, Backspace
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentKeyBg,
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .clickable { onSwitchToQwerty() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = "ABC", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = keyBg,
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .clickable { onEmojiSelected(" ") }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = "Space", color = Color(0xFF8A99B5), fontSize = 12.sp)
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentKeyBg,
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
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
    }
}
