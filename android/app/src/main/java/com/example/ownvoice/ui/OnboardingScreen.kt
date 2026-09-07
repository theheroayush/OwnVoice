package com.example.ownvoice.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ownvoice.OwnVoiceApplication
import com.example.ownvoice.network.BridgeClient
import com.example.ownvoice.theme.DesignTokens
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OwnVoiceApplication
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 4

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted && currentStep == 2) {
            currentStep = 3
        }
    }

    val bridgeClient = remember { BridgeClient(app.secureConfig, context) }
    var connectionMethod by remember { mutableIntStateOf(1) } // 1: Wi-Fi, 2: QR
    var isConnecting by remember { mutableStateOf(false) }
    var connectStatusMessage by remember { mutableStateOf("") }

    val qrScanLauncher = rememberLauncherForActivityResult(
        com.journeyapps.barcodescanner.ScanContract()
    ) { result ->
        if (result.contents != null) {
            val uri = result.contents.trim()
            if (uri.startsWith("ownvoice://pair")) {
                try {
                    val parsed = android.net.Uri.parse(uri)
                    val ip = parsed.getQueryParameter("ip") ?: ""
                    val name = parsed.getQueryParameter("name") ?: "Windows PC"
                    val token = parsed.getQueryParameter("token") ?: ""
                    val pin = parsed.getQueryParameter("pin") ?: ""
                    val apiKey = parsed.getQueryParameter("api_key") ?: ""
                    if (ip.isNotBlank()) {
                        app.secureConfig.desktopBridgeIp = ip
                        app.secureConfig.desktopBridgeName = name
                        if (token.isNotBlank()) app.secureConfig.desktopBridgeToken = token
                        if (pin.isNotBlank()) app.secureConfig.desktopBridgePin = pin
                        if (apiKey.isNotBlank() && app.secureConfig.isDefaultOrBlankApiKey) {
                            app.secureConfig.apiKey = apiKey
                        }
                        app.secureConfig.isUseForPcEnabled = true
                        onSetupComplete()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Main Canvas Container
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignTokens.ObsidianBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Navigation Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (currentStep > 1) {
                IconButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }

            // Step Indicator Pills
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 1..totalSteps) {
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(if (i == currentStep) 24.dp else 12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (i == currentStep) DesignTokens.PrimaryBlue else DesignTokens.CardBorderSubtle
                            )
                    )
                }
            }

            Text(
                text = "Skip",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DesignTokens.TextMuted,
                modifier = Modifier
                    .clickable { onSetupComplete() }
                    .padding(8.dp)
            )
        }

        // Branding Sub-header
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Own",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Voice",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.CyanAccent
                )
            }
            Text(
                text = "Your voice, everywhere",
                fontSize = 11.sp,
                color = DesignTokens.TextMuted
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Step Content Area (Scrollable)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentStep) {
                1 -> Step1Welcome(
                    onGetStarted = { currentStep = 2 },
                    onSignIn = { onSetupComplete() }
                )
                2 -> Step2MicPermission(
                    hasPermission = hasMicPermission,
                    onRequestPermission = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onMaybeLater = { currentStep = 3 }
                )
                3 -> Step3KeyboardSetup(
                    onContinue = { currentStep = 4 }
                )
                4 -> Step4ConnectComputer(
                    connectionMethod = connectionMethod,
                    onSelectMethod = { connectionMethod = it },
                    isConnecting = isConnecting,
                    statusMessage = connectStatusMessage,
                    onConnect = {
                        if (connectionMethod == 2) {
                            try {
                                qrScanLauncher.launch(
                                    com.journeyapps.barcodescanner.ScanOptions()
                                        .setPrompt("Point camera at OwnVoice Desktop QR Code")
                                        .setBeepEnabled(true)
                                        .setOrientationLocked(false)
                                )
                            } catch (e: Exception) {
                                connectStatusMessage = "Camera scanner error: ${e.message}"
                            }
                        } else {
                            isConnecting = true
                            connectStatusMessage = "Scanning Wi-Fi for your PC..."
                            scope.launch {
                                val pcs = bridgeClient.discoverLocalDesktops(timeoutMs = 1600)
                                isConnecting = false
                                if (pcs.isNotEmpty()) {
                                    val pc = pcs.first()
                                    app.secureConfig.desktopBridgeIp = pc.ip
                                    app.secureConfig.desktopBridgeName = pc.name
                                    if (pc.token.isNotBlank()) app.secureConfig.desktopBridgeToken = pc.token
                                    if (pc.pin.isNotBlank()) app.secureConfig.desktopBridgePin = pc.pin
                                    if (pc.apiKey.isNotBlank() && app.secureConfig.isDefaultOrBlankApiKey) {
                                        app.secureConfig.apiKey = pc.apiKey
                                    }
                                    app.secureConfig.isUseForPcEnabled = true
                                    onSetupComplete()
                                } else {
                                    connectStatusMessage = "No PC detected on Wi-Fi. Check app is open on PC or tap Continue."
                                    onSetupComplete()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 1: WELCOME & VALUE PROPOSITION (Image 1)
// -------------------------------------------------------------
@Composable
private fun Step1Welcome(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Turn your voice\ninto action.",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                lineHeight = 38.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Speak naturally. OwnVoice turns your voice into accurate text and sends it to your devices in real time.",
                fontSize = 13.sp,
                color = DesignTokens.TextMuted,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Visual Card: Phone & Computer Bridge Illustration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(DesignTokens.RadiusCard))
                    .background(DesignTokens.CardBg)
                    .border(1.dp, DesignTokens.CardBorder, RoundedCornerShape(DesignTokens.RadiusCard))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Mobile badge
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DesignTokens.PrimaryBlue.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Mic",
                                tint = DesignTokens.ElectricBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Just speak...", fontSize = 11.sp, color = DesignTokens.TextMuted)
                    }

                    // Sound wave bridge
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val heights = listOf(14, 24, 34, 44, 30, 20, 36, 16)
                        heights.forEach { h ->
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(h.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(DesignTokens.CyanAccent)
                            )
                        }
                    }

                    // Desktop badge
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DesignTokens.CardBgElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Laptop,
                                contentDescription = "Laptop",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Your words, anywhere.|", fontSize = 11.sp, color = DesignTokens.TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2x2 Feature Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeaturePill(
                    icon = Icons.Filled.Bolt,
                    iconTint = DesignTokens.ElectricBlue,
                    iconBg = DesignTokens.ElectricBlue.copy(alpha = 0.15f),
                    title = "Fast & Accurate",
                    desc = "Real-time transcription with high accuracy.",
                    modifier = Modifier.weight(1f)
                )
                FeaturePill(
                    icon = Icons.Filled.Security,
                    iconTint = DesignTokens.PurpleAccent,
                    iconBg = DesignTokens.PurpleAccent.copy(alpha = 0.15f),
                    title = "Private & Secure",
                    desc = "Your data stays yours.",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeaturePill(
                    icon = Icons.Filled.Devices,
                    iconTint = DesignTokens.CyanAccent,
                    iconBg = DesignTokens.CyanAccent.copy(alpha = 0.15f),
                    title = "Works Across Devices",
                    desc = "Type directly on your computer and other apps.",
                    modifier = Modifier.weight(1f)
                )
                FeaturePill(
                    icon = Icons.Filled.AutoAwesome,
                    iconTint = DesignTokens.AmberAccent,
                    iconBg = DesignTokens.AmberAccent.copy(alpha = 0.15f),
                    title = "Built for Productivity",
                    desc = "Write, search, message and more with your voice.",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            Button(
                onClick = onGetStarted,
                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.PrimaryBlue),
                shape = RoundedCornerShape(DesignTokens.RadiusPill),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Get Started", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account? ", fontSize = 12.sp, color = DesignTokens.TextMuted)
                Text(
                    text = "Sign In",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.ElectricBlue,
                    modifier = Modifier.clickable { onSignIn() }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 2: MICROPHONE ACCESS (Image 3)
// -------------------------------------------------------------
@Composable
private fun Step2MicPermission(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onMaybeLater: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(10.dp))

            // Waveform Hero Visual
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val barsLeft = listOf(12, 22, 38, 52, 30, 20)
                    barsLeft.forEach { h ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .width(3.dp)
                                .height(h.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(DesignTokens.ElectricBlue.copy(alpha = 0.5f))
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(DesignTokens.MicButtonBrush)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Microphone",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    val barsRight = listOf(20, 30, 52, 38, 22, 12)
                    barsRight.forEach { h ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .width(3.dp)
                                .height(h.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(DesignTokens.ElectricBlue.copy(alpha = 0.5f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Allow Microphone Access",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "OwnVoice needs access to your microphone to listen to your voice and turn it into text.",
                fontSize = 13.sp,
                color = DesignTokens.TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            PermissionCard(
                icon = Icons.Filled.GraphicEq,
                iconTint = DesignTokens.ElectricBlue,
                iconBg = DesignTokens.ElectricBlue.copy(alpha = 0.15f),
                title = "Real-time transcription",
                desc = "Speak naturally and see your words instantly converted to text."
            )
            Spacer(modifier = Modifier.height(10.dp))
            PermissionCard(
                icon = Icons.Filled.CheckCircle,
                iconTint = DesignTokens.StatusGreen,
                iconBg = DesignTokens.StatusGreen.copy(alpha = 0.15f),
                title = "Completely private",
                desc = "Your audio is processed securely. We don't store your voice without your consent."
            )
            Spacer(modifier = Modifier.height(10.dp))
            PermissionCard(
                icon = Icons.Filled.Bolt,
                iconTint = DesignTokens.PurpleAccent,
                iconBg = DesignTokens.PurpleAccent.copy(alpha = 0.15f),
                title = "Better experience",
                desc = "Enables hands-free typing, commands, and smarter features."
            )
        }

        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.PrimaryBlue),
                shape = RoundedCornerShape(DesignTokens.RadiusPill),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(imageVector = Icons.Filled.Mic, contentDescription = "Mic", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasPermission) "Microphone Allowed ✓" else "Allow Microphone Access",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = Color.White)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Maybe later",
                fontSize = 13.sp,
                color = DesignTokens.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMaybeLater() }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Lock",
                    tint = DesignTokens.TextSubtle,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "We respect your privacy. You can change this anytime in Settings.",
                    fontSize = 10.sp,
                    color = DesignTokens.TextSubtle
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 3: KEYBOARD & IME SETUP
// -------------------------------------------------------------
@Composable
private fun Step3KeyboardSetup(
    onContinue: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(DesignTokens.ElectricBlue.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Keyboard,
                    contentDescription = "Keyboard",
                    tint = DesignTokens.ElectricBlue,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Enable Voice Keyboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Type with your voice anywhere on your phone — in WhatsApp, Gmail, Slack, and your browser.",
                fontSize = 13.sp,
                color = DesignTokens.TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            Surface(
                shape = RoundedCornerShape(DesignTokens.RadiusCard),
                color = DesignTokens.CardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Step 1: Turn on OwnVoice in System Settings", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                        colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.CardBgElevated),
                        shape = RoundedCornerShape(DesignTokens.RadiusButton),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("1. Open Keyboard Settings", color = Color.White, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Step 2: Switch to OwnVoice Keyboard", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val imm = context.getSystemService(InputMethodManager::class.java)
                            imm?.showInputMethodPicker()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.PrimaryBlue),
                        shape = RoundedCornerShape(DesignTokens.RadiusButton),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("2. Choose OwnVoice Keyboard", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.PrimaryBlue),
                shape = RoundedCornerShape(DesignTokens.RadiusPill),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Continue", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = Color.White)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 4: CONNECT COMPUTER (Image 4)
// -------------------------------------------------------------
@Composable
private fun Step4ConnectComputer(
    connectionMethod: Int,
    onSelectMethod: (Int) -> Unit,
    isConnecting: Boolean,
    statusMessage: String,
    onConnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Connect Your Computer",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Link your computer to start typing with your voice directly into any app.",
                fontSize = 13.sp,
                color = DesignTokens.TextMuted,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Method 1: Same Wi-Fi Network
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DesignTokens.RadiusCard))
                    .background(DesignTokens.CardBg)
                    .border(
                        width = if (connectionMethod == 1) 1.5.dp else 1.dp,
                        color = if (connectionMethod == 1) DesignTokens.ElectricBlue else DesignTokens.CardBorder,
                        shape = RoundedCornerShape(DesignTokens.RadiusCard)
                    )
                    .clickable { onSelectMethod(1) }
                    .padding(14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DesignTokens.ElectricBlue.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Wifi,
                        contentDescription = "Wi-Fi",
                        tint = DesignTokens.ElectricBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Same Wi-Fi Network", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
                    Text("Make sure your phone and computer are on the same Wi-Fi.", color = DesignTokens.TextMuted, fontSize = 11.sp)
                }

                RadioButton(
                    selected = connectionMethod == 1,
                    onClick = { onSelectMethod(1) },
                    colors = RadioButtonDefaults.colors(selectedColor = DesignTokens.ElectricBlue)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Method 2: Scan QR Code
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DesignTokens.RadiusCard))
                    .background(DesignTokens.CardBg)
                    .border(
                        width = if (connectionMethod == 2) 1.5.dp else 1.dp,
                        color = if (connectionMethod == 2) DesignTokens.ElectricBlue else DesignTokens.CardBorder,
                        shape = RoundedCornerShape(DesignTokens.RadiusCard)
                    )
                    .clickable { onSelectMethod(2) }
                    .padding(14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DesignTokens.CardBgElevated)
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = "QR",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Scan QR Code", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
                    Text("Scan the code shown on your computer screen.", color = DesignTokens.TextMuted, fontSize = 11.sp)
                }

                RadioButton(
                    selected = connectionMethod == 2,
                    onClick = { onSelectMethod(2) },
                    colors = RadioButtonDefaults.colors(selectedColor = DesignTokens.ElectricBlue)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3-Step Timeline Guide
            Surface(
                shape = RoundedCornerShape(DesignTokens.RadiusCard),
                color = DesignTokens.CardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Steps", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    TimelineStep(step = "1", title = "Open OwnVoice on your computer", desc = "Ensure OwnVoice desktop is running.")
                    Spacer(modifier = Modifier.height(10.dp))
                    TimelineStep(step = "2", title = "Make sure both devices are on same Wi-Fi", desc = "Your phone and computer must share the same network.")
                    Spacer(modifier = Modifier.height(10.dp))
                    TimelineStep(step = "3", title = "Click 'Connect' below", desc = "We'll find your computer automatically.")
                }
            }

            if (statusMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = statusMessage,
                    fontSize = 12.sp,
                    color = DesignTokens.AmberAccent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            Button(
                onClick = onConnect,
                enabled = !isConnecting,
                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.PrimaryBlue),
                shape = RoundedCornerShape(DesignTokens.RadiusPill),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(imageVector = Icons.Filled.Laptop, contentDescription = "Laptop", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnecting) "Connecting..." else "Connect to Computer",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = Color.White)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = "Shield",
                    tint = DesignTokens.TextSubtle,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Your connection is end-to-end encrypted.",
                    fontSize = 10.sp,
                    color = DesignTokens.TextSubtle
                )
            }
        }
    }
}

@Composable
private fun FeaturePill(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DesignTokens.CardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.CardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg)
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, color = DesignTokens.TextMuted, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    desc: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DesignTokens.CardBg)
            .border(1.dp, DesignTokens.CardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg)
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, color = DesignTokens.TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun TimelineStep(step: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(DesignTokens.CardBgElevated)
        ) {
            Text(step, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(desc, fontSize = 10.sp, color = DesignTokens.TextMuted)
        }
    }
}
