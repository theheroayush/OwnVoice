package com.example.ownvoice.ui

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.ownvoice.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ownvoice.OwnVoiceApplication
import com.example.ownvoice.network.ApiKeyValidator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OwnVoiceApplication
    val scope = rememberCoroutineScope()

    var inputKey by remember { mutableStateOf(app.secureConfig.apiKey) }
    var isValidating by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(app.secureConfig.apiKey.isNotBlank()) }

    val darkBg = Color(0xFF121214)
    val cardBg = Color(0xFF1C1C22)
    val accentBlue = Color(0xFF0A84FF)
    val accentGreen = Color(0xFF34C759)
    val textMuted = Color(0xFF8E8E93)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo & Title
        Image(
            painter = painterResource(id = R.drawable.ic_app_icon),
            contentDescription = "OwnVoice Logo",
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(18.dp))
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "OwnVoice",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Zero-Cost, High-Speed AI Voice Typing",
            fontSize = 14.sp,
            color = textMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Step 1: Explain Free Key
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Key",
                        tint = accentBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "1. Get 100% Free API Key",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Google AI Studio gives every user 1,500 free dictations every day with zero credit card required.",
                    fontSize = 13.sp,
                    color = textMuted,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://aistudio.google.com/app/apikey")
                        ).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(browserIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Google AI Studio", fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Step 2: Paste & Validate
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "2. Paste & Save Key",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    placeholder = { Text("Paste AI Studio Key (AIzaSy...)", color = textMuted, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentBlue,
                        unfocusedBorderColor = Color(0xFF33333E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Auto paste button
                    OutlinedButton(
                        onClick = {
                            val clipMgr = context.getSystemService(ClipboardManager::class.java)
                            if (clipMgr != null && clipMgr.hasPrimaryClip() &&
                                clipMgr.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true
                            ) {
                                val item = clipMgr.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                if (item.isNotBlank()) {
                                    inputKey = item.trim()
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Paste Clipboard", color = Color.White, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Validate & Save Button
                    Button(
                        onClick = {
                            if (inputKey.isBlank()) return@Button
                            isValidating = true
                            validationMessage = ""
                            scope.launch {
                                val (ok, msg) = ApiKeyValidator.validateKey(inputKey)
                                isValidating = false
                                if (ok) {
                                    app.secureConfig.apiKey = inputKey
                                    isSuccess = true
                                    validationMessage = "Connected successfully!"
                                } else {
                                    isSuccess = false
                                    validationMessage = msg
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSuccess) accentGreen else accentBlue),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isValidating && inputKey.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isValidating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else if (isSuccess) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verified", fontSize = 12.sp)
                        } else {
                            Text("Test & Save", fontSize = 12.sp)
                        }
                    }
                }

                if (validationMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = validationMessage,
                        color = if (isSuccess) accentGreen else Color(0xFFFF453A),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Continue Button
        Button(
            onClick = onSetupComplete,
            enabled = isSuccess || app.secureConfig.apiKey.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Continue to Settings & Activation", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
