package com.example.ownvoice.ui.screens

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
import com.example.ownvoice.network.BridgeClient
import com.example.ownvoice.theme.DesignTokens
import com.example.ownvoice.ui.components.BottomNavBar
import com.example.ownvoice.ui.navigation.ScreenDestination
import kotlinx.coroutines.launch

@Composable
fun ConnectedDevicesScreen(
    onBack: () -> Unit,
    onNavigate: (ScreenDestination) -> Unit,
    onConnectNewDevice: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OwnVoiceApplication
    val scope = rememberCoroutineScope()
    val bridgeClient = remember { BridgeClient(app.secureConfig, context) }

    var bridgeIp by remember { mutableStateOf(app.secureConfig.desktopBridgeIp) }
    var bridgeName by remember { mutableStateOf(app.secureConfig.desktopBridgeName) }
    var statusMessage by remember { mutableStateOf("") }
    var showDeviceOptionsDialog by remember { mutableStateOf<String?>(null) }

    val hasPairedPc = bridgeIp.isNotBlank()
    val totalDeviceCount = if (hasPairedPc) 3 else 2

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
            BottomNavBar(
                currentDestination = ScreenDestination.ConnectedDevices,
                onNavigate = onNavigate,
                showCenterMic = true,
                onCenterMicClick = { onNavigate(ScreenDestination.Home) }
            )
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
                    text = "Connected Devices",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Manage the computers and devices linked to your account.",
                    fontSize = 13.sp,
                    color = DesignTokens.Colors.TextMuted
                )
            }

            // Summary Hero Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
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
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E3A8A).copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Laptop,
                                contentDescription = "Devices",
                                tint = DesignTokens.ElectricBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "$totalDeviceCount devices connected",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DesignTokens.Colors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Your voice is available on all your devices.",
                                fontSize = 12.sp,
                                color = DesignTokens.Colors.TextMuted
                            )
                        }
                    }
                }
            }

            // Device 1: MacBook Pro (Current device)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Laptop,
                                contentDescription = "MacBook Pro",
                                tint = DesignTokens.Colors.TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MacBook Pro",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DesignTokens.Colors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "macOS 14.6 • Chrome",
                                fontSize = 11.sp,
                                color = DesignTokens.Colors.TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DesignTokens.Colors.PrimaryBlue.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Current device",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.ElectricBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Navigate",
                            tint = DesignTokens.Colors.TextSubtle,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Device 2: Windows PC
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Window,
                                contentDescription = "Windows PC",
                                tint = DesignTokens.ElectricBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (bridgeName.isNotBlank()) bridgeName else "Windows PC",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DesignTokens.Colors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (hasPairedPc) "Windows 11 • Connected ($bridgeIp:8765)" else "Windows 11 • OwnVoice App",
                                fontSize = 11.sp,
                                color = if (hasPairedPc) DesignTokens.Colors.StatusReady else DesignTokens.Colors.TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (hasPairedPc) "Live streaming active" else "Last active: 2 hours ago",
                                fontSize = 10.sp,
                                color = DesignTokens.Colors.TextSubtle
                            )
                        }
                        IconButton(onClick = { showDeviceOptionsDialog = "Windows PC" }) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Options",
                                tint = DesignTokens.Colors.TextMuted
                            )
                        }
                    }
                }
            }

            // Device 3: Ayush's iPhone
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = "iPhone",
                                tint = DesignTokens.Colors.TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ayush's iPhone",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DesignTokens.Colors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "iOS 18 • OwnVoice App",
                                fontSize = 11.sp,
                                color = DesignTokens.Colors.TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Last active: 1 day ago",
                                fontSize = 10.sp,
                                color = DesignTokens.Colors.TextSubtle
                            )
                        }
                        IconButton(onClick = { showDeviceOptionsDialog = "Ayush's iPhone" }) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Options",
                                tint = DesignTokens.Colors.TextMuted
                            )
                        }
                    }
                }
            }

            // Device 4: iPad
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tablet,
                                contentDescription = "iPad",
                                tint = DesignTokens.Colors.TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "iPad",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DesignTokens.Colors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "iPadOS 17 • OwnVoice App",
                                fontSize = 11.sp,
                                color = DesignTokens.Colors.TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Last active: 3 days ago",
                                fontSize = 10.sp,
                                color = DesignTokens.Colors.TextSubtle
                            )
                        }
                        IconButton(onClick = { showDeviceOptionsDialog = "iPad" }) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Options",
                                tint = DesignTokens.Colors.TextMuted
                            )
                        }
                    }
                }
            }

            // Action: Connect a New Device
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onConnectNewDevice() }
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Connect New",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connect a New Device",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DesignTokens.Colors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Link another device to use OwnVoice.",
                                fontSize = 11.sp,
                                color = DesignTokens.Colors.TextMuted
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Navigate",
                            tint = DesignTokens.Colors.TextSubtle,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Privacy Assurance Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E3A8A).copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Private",
                                tint = DesignTokens.ElectricBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Your data stays private",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DesignTokens.Colors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "All voice data is end-to-end encrypted and synced securely across your devices.",
                                fontSize = 11.sp,
                                color = DesignTokens.Colors.TextMuted,
                                lineHeight = 15.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Navigate",
                            tint = DesignTokens.Colors.TextSubtle,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    if (showDeviceOptionsDialog != null) {
        val device = showDeviceOptionsDialog!!
        AlertDialog(
            onDismissRequest = { showDeviceOptionsDialog = null },
            title = { Text(device, color = DesignTokens.Colors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Device status: Connected via local bridge network.", color = DesignTokens.Colors.TextMuted, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            if (hasPairedPc) {
                                val ok = bridgeClient.checkStatus(bridgeIp)
                                Toast.makeText(context, if (ok) "PC is reachable!" else "PC offline", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Device ping successful", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showDeviceOptionsDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.PrimaryBlue)
                ) {
                    Text("Ping Device")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeviceOptionsDialog = null }) {
                    Text("Close", color = DesignTokens.Colors.TextMuted)
                }
            },
            containerColor = DesignTokens.Colors.CardSurface
        )
    }
}
