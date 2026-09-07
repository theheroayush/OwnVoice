package com.example.ownvoice.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ownvoice.OwnVoiceApplication
import com.example.ownvoice.network.BridgeClient
import com.example.ownvoice.network.DiscoveredDesktop
import com.example.ownvoice.theme.DesignTokens
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

@Composable
fun ComputerConnectionScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OwnVoiceApplication
    val scope = rememberCoroutineScope()
    val bridgeClient = remember { BridgeClient(app.secureConfig, context) }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Wi-Fi, 1 = QR Code
    var bridgeIp by remember { mutableStateOf(app.secureConfig.desktopBridgeIp) }
    var bridgeName by remember { mutableStateOf(app.secureConfig.desktopBridgeName) }
    var isUseForPc by remember { mutableStateOf(app.secureConfig.isUseForPcEnabled) }

    var isSearching by remember { mutableStateOf(false) }
    var discoveredPCs by remember { mutableStateOf(listOf<DiscoveredDesktop>()) }
    var statusMessage by remember { mutableStateOf("") }

    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var showManualIp by remember { mutableStateOf(false) }
    var manualIpInput by remember { mutableStateOf(bridgeIp) }

    val qrScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val uri = result.contents.trim()
            if (uri.startsWith("ownvoice://pair")) {
                try {
                    val parsed = android.net.Uri.parse(uri)
                    val ip = parsed.getQueryParameter("ip") ?: ""
                    val name = parsed.getQueryParameter("name") ?: "Windows PC"
                    val token = parsed.getQueryParameter("token") ?: ""
                    val pin = parsed.getQueryParameter("pin") ?: ""
                    val qrApiKey = parsed.getQueryParameter("api_key") ?: ""

                    if (ip.isNotBlank()) {
                        bridgeIp = ip
                        bridgeName = name
                        app.secureConfig.desktopBridgeIp = ip
                        app.secureConfig.desktopBridgeName = name
                        if (token.isNotBlank()) app.secureConfig.desktopBridgeToken = token
                        if (pin.isNotBlank()) app.secureConfig.desktopBridgePin = pin
                        if (qrApiKey.isNotBlank() && app.secureConfig.isDefaultOrBlankApiKey) {
                            app.secureConfig.apiKey = qrApiKey
                        }
                        app.secureConfig.isUseForPcEnabled = true
                        isUseForPc = true
                        statusMessage = "Paired with $name ($ip)"
                    }
                } catch (e: Exception) {
                    statusMessage = "QR scan error: ${e.message}"
                }
            } else {
                statusMessage = "Unrecognized QR format"
            }
        }
    }

    Scaffold(
        containerColor = DesignTokens.Colors.BackgroundDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = DesignTokens.Colors.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Connect Your Computer",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextPrimary
                )
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
            // Top Mode Tabs (Wi-Fi vs QR Code)
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        // Same Wi-Fi Tab
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 0) DesignTokens.Colors.PrimaryBlue else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = 0 }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Same Wi-Fi",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedTab == 0) Color.White else DesignTokens.Colors.TextMuted
                                )
                            }
                        }

                        // Scan QR Tab
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 1) DesignTokens.Colors.PrimaryBlue else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = 1 }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Scan QR Code",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedTab == 1) Color.White else DesignTokens.Colors.TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Live Paired PC Status Card (If Connected)
            if (bridgeIp.isNotBlank()) {
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(DesignTokens.Colors.StatusReady, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = bridgeName.ifBlank { "Windows PC" },
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DesignTokens.Colors.TextPrimary
                                        )
                                        Text(
                                            text = "$bridgeIp:8765",
                                            fontSize = 12.sp,
                                            color = DesignTokens.Colors.TextMuted
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                statusMessage = "Pinging $bridgeIp:8765..."
                                                val ok = bridgeClient.checkStatus(bridgeIp)
                                                statusMessage = if (ok) "PC is Connected & Ready!" else "PC unreachable at $bridgeIp:8765 (${bridgeClient.lastError})"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.PrimaryBlue.copy(alpha = 0.2f)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Test", fontSize = 11.sp, color = DesignTokens.Colors.PrimaryLightBlue)
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
                                            statusMessage = "Unpaired from PC"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B1D22)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Unpair", fontSize = 11.sp, color = DesignTokens.Colors.StatusRecording)
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = DesignTokens.Colors.BorderSubtle,
                                thickness = 0.5.dp
                            )

                            // Use for PC Keystroke Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Stream Voice to PC Cursor",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DesignTokens.Colors.TextPrimary
                                    )
                                    Text(
                                        text = "Types into whichever text box your PC mouse clicked",
                                        fontSize = 11.sp,
                                        color = DesignTokens.Colors.TextMuted
                                    )
                                }
                                Switch(
                                    checked = isUseForPc,
                                    onCheckedChange = {
                                        isUseForPc = it
                                        app.secureConfig.isUseForPcEnabled = it
                                        statusMessage = if (it) "PC Streaming Active" else "PC Streaming Paused"
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = DesignTokens.Colors.StatusReady
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (statusMessage.isNotBlank()) {
                item {
                    Text(
                        text = statusMessage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (statusMessage.startsWith("Paired") || statusMessage.startsWith("PC is")) DesignTokens.Colors.StatusReady
                               else if (statusMessage.startsWith("PC unreachable") || statusMessage.startsWith("QR")) DesignTokens.Colors.StatusRecording
                               else DesignTokens.Colors.PrimaryLightBlue
                    )
                }
            }

            // Content for Tab 0: Same Wi-Fi Network
            if (selectedTab == 0) {
                // 3-Step Guide Card
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "HOW TO CONNECT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DesignTokens.Colors.PrimaryLightBlue,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Step 1
                            Row(verticalAlignment = Alignment.Top) {
                                Surface(
                                    shape = CircleShape,
                                    color = DesignTokens.Colors.PrimaryBlue.copy(alpha = 0.2f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DesignTokens.Colors.PrimaryLightBlue)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Open OwnVoice on your Computer", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DesignTokens.Colors.TextPrimary)
                                    Text("Double-click 'run_phone_link.bat' or run 'python app.py --link'.", fontSize = 11.sp, color = DesignTokens.Colors.TextMuted)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Step 2
                            Row(verticalAlignment = Alignment.Top) {
                                Surface(
                                    shape = CircleShape,
                                    color = DesignTokens.Colors.PrimaryBlue.copy(alpha = 0.2f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("2", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DesignTokens.Colors.PrimaryLightBlue)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Verify Same Wi-Fi Network", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DesignTokens.Colors.TextPrimary)
                                    Text("Make sure phone and computer are connected to the same router.", fontSize = 11.sp, color = DesignTokens.Colors.TextMuted)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Step 3
                            Row(verticalAlignment = Alignment.Top) {
                                Surface(
                                    shape = CircleShape,
                                    color = DesignTokens.Colors.PrimaryBlue.copy(alpha = 0.2f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("3", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DesignTokens.Colors.PrimaryLightBlue)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Tap Auto-Discover or Enter PIN", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DesignTokens.Colors.TextPrimary)
                                    Text("Use the buttons below to pair in 1 second.", fontSize = 11.sp, color = DesignTokens.Colors.TextMuted)
                                }
                            }
                        }
                    }
                }

                // 1-Tap Auto-Discover Button
                item {
                    Button(
                        onClick = {
                            isSearching = true
                            statusMessage = "Sweeping Wi-Fi subnet for OwnVoice PC..."
                            discoveredPCs = emptyList()
                            scope.launch {
                                val found = bridgeClient.discoverLocalDesktops(timeoutMs = 1800)
                                discoveredPCs = found
                                isSearching = false
                                if (found.isEmpty()) {
                                    statusMessage = "No PC found. Ensure OwnVoice is running on your PC."
                                } else if (found.size == 1) {
                                    val pc = found.first()
                                    bridgeIp = pc.ip
                                    bridgeName = pc.name
                                    app.secureConfig.desktopBridgeIp = pc.ip
                                    app.secureConfig.desktopBridgeName = pc.name
                                    if (pc.token.isNotBlank()) app.secureConfig.desktopBridgeToken = pc.token
                                    if (pc.pin.isNotBlank()) app.secureConfig.desktopBridgePin = pc.pin
                                    if (pc.apiKey.isNotBlank() && app.secureConfig.isDefaultOrBlankApiKey) {
                                        app.secureConfig.apiKey = pc.apiKey
                                    }
                                    app.secureConfig.isUseForPcEnabled = true
                                    isUseForPc = true
                                    statusMessage = "Auto-Paired with ${pc.name} (${pc.ip})!"
                                } else {
                                    statusMessage = "Found ${found.size} PCs on Wi-Fi. Tap your PC below to pair."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isSearching
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Searching Local Wi-Fi...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = "Auto-Discover", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("1-Tap Auto-Discover PC", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Discovered PCs list
                if (discoveredPCs.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DesignTokens.Colors.CardSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Select Your Computer:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DesignTokens.Colors.TextPrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                discoveredPCs.forEach { pc ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                bridgeIp = pc.ip
                                                bridgeName = pc.name
                                                app.secureConfig.desktopBridgeIp = pc.ip
                                                app.secureConfig.desktopBridgeName = pc.name
                                                if (pc.token.isNotBlank()) app.secureConfig.desktopBridgeToken = pc.token
                                                if (pc.pin.isNotBlank()) app.secureConfig.desktopBridgePin = pc.pin
                                                if (pc.apiKey.isNotBlank() && app.secureConfig.isDefaultOrBlankApiKey) {
                                                    app.secureConfig.apiKey = pc.apiKey
                                                }
                                                app.secureConfig.isUseForPcEnabled = true
                                                isUseForPc = true
                                                statusMessage = "Paired with ${pc.name}!"
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(pc.name, fontWeight = FontWeight.Bold, color = DesignTokens.Colors.TextPrimary, fontSize = 13.sp)
                                            Text("${pc.ip}:8765", color = DesignTokens.Colors.TextMuted, fontSize = 11.sp)
                                        }
                                        Text("Pair", color = DesignTokens.Colors.PrimaryLightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Secondary Pair Option: 6-Digit PIN
                item {
                    OutlinedButton(
                        onClick = { showPinDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DesignTokens.Colors.TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Pin, contentDescription = "PIN", tint = DesignTokens.Colors.PrimaryLightBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enter 6-Digit PIN from PC Screen", fontSize = 13.sp)
                    }
                }

                // Manual IP Option Toggle
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showManualIp = !showManualIp }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showManualIp) "Hide manual IP entry" else "Or enter IP address manually",
                            fontSize = 12.sp,
                            color = DesignTokens.Colors.TextMuted
                        )
                        Icon(
                            imageVector = if (showManualIp) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle",
                            tint = DesignTokens.Colors.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (showManualIp) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DesignTokens.Colors.CardSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                OutlinedTextField(
                                    value = manualIpInput,
                                    onValueChange = { manualIpInput = it },
                                    label = { Text("PC IP Address (e.g. 192.168.1.50)") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DesignTokens.Colors.PrimaryBlue,
                                        unfocusedBorderColor = DesignTokens.Colors.BorderSubtle,
                                        focusedTextColor = DesignTokens.Colors.TextPrimary,
                                        unfocusedTextColor = DesignTokens.Colors.TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        val cleanIp = manualIpInput.trim()
                                        if (cleanIp.isNotBlank()) {
                                            bridgeIp = cleanIp
                                            bridgeName = "Windows PC"
                                            app.secureConfig.desktopBridgeIp = cleanIp
                                            app.secureConfig.desktopBridgeName = "Windows PC"
                                            app.secureConfig.isUseForPcEnabled = true
                                            isUseForPc = true
                                            statusMessage = "Connected to $cleanIp:8765"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.PrimaryBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save & Connect")
                                }
                            }
                        }
                    }
                }
            }

            // Content for Tab 1: Scan QR Code
            if (selectedTab == 1) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DesignTokens.Colors.PrimaryBlue.copy(alpha = 0.15f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "QR Code",
                                        tint = DesignTokens.Colors.PrimaryLightBlue,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Instant QR Code Pairing",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DesignTokens.Colors.TextPrimary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Point your camera at the QR code displayed in the OwnVoice PC terminal or desktop window.",
                                fontSize = 12.sp,
                                color = DesignTokens.Colors.TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    val options = ScanOptions().apply {
                                        setPrompt("Scan OwnVoice PC QR Code")
                                        setBeepEnabled(true)
                                        setOrientationLocked(false)
                                    }
                                    qrScanLauncher.launch(options)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.PrimaryBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open QR Scanner", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Bottom Security & Encryption Badge
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = DesignTokens.Colors.StatusReady,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Encrypted over Local Network • Zero Cloud Relay",
                        fontSize = 11.sp,
                        color = DesignTokens.Colors.TextSubtle
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 6-Digit PIN Pairing Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = {
                Text("Enter 6-Digit PIN", color = DesignTokens.Colors.TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Enter the PIN displayed in your OwnVoice PC window:",
                        fontSize = 12.sp,
                        color = DesignTokens.Colors.TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            if (it.length <= 6) enteredPin = it
                        },
                        placeholder = { Text("e.g. 849201") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DesignTokens.Colors.PrimaryBlue,
                            unfocusedBorderColor = DesignTokens.Colors.BorderSubtle,
                            focusedTextColor = DesignTokens.Colors.TextPrimary,
                            unfocusedTextColor = DesignTokens.Colors.TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pin = enteredPin.trim()
                        if (pin.length == 6) {
                            showPinDialog = false
                            isSearching = true
                            statusMessage = "Searching for PC with PIN $pin..."
                            scope.launch {
                                val found = bridgeClient.discoverLocalDesktops(timeoutMs = 1500)
                                isSearching = false
                                val matched = found.firstOrNull { it.pin == pin } ?: found.firstOrNull()
                                if (matched != null) {
                                    bridgeIp = matched.ip
                                    bridgeName = matched.name
                                    app.secureConfig.desktopBridgeIp = matched.ip
                                    app.secureConfig.desktopBridgeName = matched.name
                                    app.secureConfig.desktopBridgePin = pin
                                    if (matched.token.isNotBlank()) app.secureConfig.desktopBridgeToken = matched.token
                                    if (matched.apiKey.isNotBlank() && app.secureConfig.isDefaultOrBlankApiKey) {
                                        app.secureConfig.apiKey = matched.apiKey
                                    }
                                    app.secureConfig.isUseForPcEnabled = true
                                    isUseForPc = true
                                    statusMessage = "Paired via PIN with ${matched.name} (${matched.ip})!"
                                } else {
                                    statusMessage = "No PC with PIN $pin found on Wi-Fi."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.PrimaryBlue)
                ) {
                    Text("Pair with PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel", color = DesignTokens.Colors.TextMuted)
                }
            },
            containerColor = DesignTokens.Colors.CardSurface
        )
    }
}
