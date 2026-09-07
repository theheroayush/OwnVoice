package com.example.ownvoice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ownvoice.theme.DesignTokens

data class PillOption(
    val id: String,
    val title: String,
    val subtitle: String? = null
)

@Composable
fun SegmentedPillSelector(
    options: List<PillOption>,
    selectedId: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F1420))
            .border(1.dp, DesignTokens.CardBorderSubtle, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option.id == selectedId
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) DesignTokens.PrimaryBlue.copy(alpha = 0.25f)
                        else Color.Transparent
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) DesignTokens.PrimaryBlue else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onOptionSelected(option.id) }
                    .padding(vertical = if (option.subtitle != null) 8.dp else 10.dp, horizontal = 4.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = option.title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else DesignTokens.TextMuted
                    )
                    if (option.subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = option.subtitle,
                            fontSize = 9.sp,
                            color = if (isSelected) DesignTokens.ElectricBlue else DesignTokens.TextSubtle
                        )
                    }
                }
            }
        }
    }
}
