package com.example.ownvoice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.ownvoice.theme.OwnVoiceTheme
import com.example.ownvoice.ui.OnboardingScreen
import com.example.ownvoice.ui.SettingsScreen

sealed class Screen {
    data object Onboarding : Screen()
    data object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions evaluated
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestRequiredPermissions()

        val app = application as OwnVoiceApplication

        setContent {
            OwnVoiceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121214)
                ) {
                    var currentScreen by remember {
                        mutableStateOf<Screen>(
                            if (app.secureConfig.apiKey.isBlank()) Screen.Onboarding else Screen.Settings
                        )
                    }

                    when (currentScreen) {
                        is Screen.Onboarding -> {
                            OnboardingScreen(
                                onSetupComplete = { currentScreen = Screen.Settings }
                            )
                        }
                        is Screen.Settings -> {
                            SettingsScreen(
                                onNavigateToOnboarding = { currentScreen = Screen.Onboarding }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
