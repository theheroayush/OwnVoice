package com.example.ownvoice.ui.screens

import android.os.Build
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ConnectedDevicesScreen(
    onBack: () -> Unit,
    onNavigate: (com.example.ownvoice.ui.navigation.ScreenDestination) -> Unit,
    onConnectNewDevice: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OwnVoiceApplication
    val scope = rememberCoroutineScope()
    val bridgeClient = remember { BridgeClient(app.secureConfig, context) }

    var bridgeIp by remember { mutableStateOf(app.secureConfig.desktopBridgeIp) }
    var bridgeName by remember { mutableStateOf(app.secureConfig.desktopBridgeName) }
    var isUseForPc by remember { mutableStateOf(app.secureConfig.isUseForPcEnabled) }
    var isPinging by remember { mutableStateOf(false) }
    var pingStatus by remember { mutableStateOf("") }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val hasPairedPc = bridgeIp.isNotBlank()

    // Real Android hardware info
    val phoneManufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    val phoneModel = Build.MODEL
    val androidVersion = Build.VERSION.RELEASE

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
                        text = "Device Ecosystem",
                        fontSize = 10.sp,
                        color = DesignTokens.Colors.TextSubtle
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    text = "Manage computers and local bridges paired with your voice assistant.",
                    fontSize = 13.sp,
                    color = DesignTokens.Colors.TextMuted
                )
            }

            // Summary Card
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
                                imageVector = Icons.Default.Devices,
                                contentDescription = "Devices",
                                tint = DesignTokens.ElectricBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = if (hasPairedPc) "2 Devices in Mesh" else "1 Device Active",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DesignTokens.Colors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (hasPairedPc) "Phone microphone linked to PC cursor" else "Phone voice dictation ready",
                                fontSize = 12.sp,
                                color = DesignTokens.Colors.TextMuted
                            )
                        }
                    }
                }
            }

            // Device 1: This Android Device (Real hardware detection)
            item {
                Text(
                    text = "THIS DEVICE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextSubtle,
                    letterSpacing = 0.8.sp
                )
            }

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
                                .background(Color(0xFF0F766E).copy(alpha = 0.3f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = "Android Phone",
                                tint = Color(0xFF2DD4BF),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$phoneManufacturer $phoneModel",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DesignTokens.Colors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Android $androidVersion • OwnVoice v2.6.0",
                                fontSize = 11.sp,
                                color = DesignTokens.Colors.TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DesignTokens.Colors.PrimaryBlue.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Current Device",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DesignTokens.ElectricBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Device 2: Paired PC Bridge Target
            item {
                Text(
                    text = "PAIRED DESKTOP TARGET",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextSubtle,
                    letterSpacing = 0.8.sp
                )
            }

            if (hasPairedPc) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.StatusReady.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF1E293B))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Laptop,
                                            contentDescription = "PC",
                                            tint = DesignTokens.ElectricBlue,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = bridgeName.ifBlank { "Windows PC" },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DesignTokens.Colors.TextPrimary
                                        )
                                        Text(
                                            text = "$bridgeIp:8765",
                                            fontSize = 12.sp,
                                            color = DesignTokens.Colors.StatusReady
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DesignTokens.Colors.StatusReady.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "PAIRED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DesignTokens.Colors.StatusReady,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (pingStatus.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = pingStatus,
                                    fontSize = 12.sp,
                                    color = if (pingStatus.startsWith("Connected")) DesignTokens.Colors.StatusReady else DesignTokens.Colors.StatusRecording
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isPinging = true
                                        pingStatus = "Testing bridge link at $bridgeIp:8765..."
                                        scope.launch {
                                            val ok = withContext(Dispatchers.IO) { bridgeClient.checkStatus(bridgeIp) }
                                            isPinging = false
                                            pingStatus = if (ok) "Connected & Responsive! (HTTP 200 OK)" else "Offline or Unreachable (${bridgeClient.lastError})"
                                        }
                                    },
                                    enabled = !isPinging,
                                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.PrimaryBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isPinging) "Testing..." else "Test Ping")
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val ok = withContext(Dispatchers.IO) {
                                                bridgeClient.sendToDesktop(
                                                    desktopIp = bridgeIp,
                                                    text = "Hello from OwnVoice Android! 🎉",
                                                    token = app.secureConfig.desktopBridgeToken
                                                )
                                            }
                                            Toast.makeText(context, if (ok) "Typed test string into PC!" else "Failed to send keystrokes", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Test Type", color = DesignTokens.Colors.TextPrimary)
                                }

                                Button(
                                    onClick = {
                                        bridgeIp = ""
                                        bridgeName = ""
                                        app.secureConfig.desktopBridgeIp = ""
                                        app.secureConfig.desktopBridgeName = ""
                                        app.secureConfig.desktopBridgeToken = ""
                                        app.secureConfig.desktopBridgePin = ""
                                        app.secureConfig.isUseForPcEnabled = false
                                        isUseForPc = false
                                        Toast.makeText(context, "Unpaired from PC", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B1D22))
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Unpair", tint = DesignTokens.Colors.StatusRecording, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No computer linked yet. Tap below to connect via Wi-Fi sweep or QR Code.",
                                fontSize = 13.sp,
                                color = DesignTokens.Colors.TextSubtle
                            )
                        }
                    }
                }
            }

            // Action: Connect / Reconnect New Computer
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
                                .background(DesignTokens.Colors.PrimaryBlue.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddLink,
                                contentDescription = "Pair",
                                tint = DesignTokens.ElectricBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasPairedPc) "Pair Another Computer" else "Connect Your Computer",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DesignTokens.Colors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Link Windows PC or Mac via Wi-Fi or QR Code",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPrivacyDialog = true }
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
                                text = "Local Bridge Encryption",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DesignTokens.Colors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Audio and keystrokes travel over your private local network. Tap for details.",
                                fontSize = 11.sp,
                                color = DesignTokens.Colors.TextMuted,
                                lineHeight = 15.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Details",
                            tint = DesignTokens.Colors.TextSubtle,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Local Bridge Security", color = DesignTokens.Colors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "OwnVoice connects your Android phone directly to your computer using your local Wi-Fi router (Port 8765 TCP & 8766 UDP).\n\n" +
                               "• Zero Cloud Relays: Keystrokes never touch third-party servers.\n" +
                               "• SHA-256 Token Auth: All desktop keystroke injections require session token authentication.\n" +
                               "• Full Local Control: You can pause or unpair at any time.",
                        color = DesignTokens.Colors.TextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.PrimaryBlue)
                ) {
                    Text("Understood")
                }
            },
            containerColor = DesignTokens.Colors.CardSurface
        )
    }
}
