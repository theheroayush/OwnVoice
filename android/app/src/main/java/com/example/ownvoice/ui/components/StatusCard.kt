package com.example.ownvoice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material3.Icon
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

@Composable
fun ComputerStatusCard(
    isConnected: Boolean,
    deviceName: String = "Windows PC",
    deviceIp: String = "",
    computerName: String = deviceName,
    ipAddress: String = deviceIp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = if (computerName.isNotBlank()) computerName else deviceName
    val displayIp = if (ipAddress.isNotBlank()) ipAddress else deviceIp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignTokens.RadiusCard))
            .background(DesignTokens.CardBg)
            .border(1.dp, DesignTokens.CardBorder, RoundedCornerShape(DesignTokens.RadiusCard))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        // Laptop icon in dark container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B))
        ) {
            Icon(
                imageVector = Icons.Filled.Laptop,
                contentDescription = "Computer",
                tint = if (isConnected) DesignTokens.ElectricBlue else DesignTokens.TextSubtle,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Live status dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) DesignTokens.StatusGreen else DesignTokens.TextSubtle)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isConnected) "$displayName • Connected" else "No Computer Connected",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DesignTokens.TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (isConnected && displayIp.isNotBlank()) "Local Wi-Fi • $displayIp:8765" else "Tap to pair via Wi-Fi or QR Code",
                fontSize = 11.sp,
                color = DesignTokens.TextMuted
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open Link Hub",
            tint = DesignTokens.TextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}
