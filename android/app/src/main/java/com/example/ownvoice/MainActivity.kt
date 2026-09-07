package com.example.ownvoice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ownvoice.theme.DesignTokens
import com.example.ownvoice.theme.OwnVoiceTheme
import com.example.ownvoice.ui.HomeScreen
import com.example.ownvoice.ui.OnboardingScreen
import com.example.ownvoice.ui.SettingsScreen
import com.example.ownvoice.ui.navigation.ScreenDestination
import com.example.ownvoice.ui.screens.*

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
                    color = DesignTokens.Colors.BackgroundDark
                ) {
                    var currentDestination by remember {
                        mutableStateOf<ScreenDestination>(
                            if (!app.secureConfig.hasCompletedOnboarding) {
                                ScreenDestination.Onboarding(0)
                            } else {
                                ScreenDestination.Home
                            }
                        )
                    }

                    // System Back Button Handling
                    BackHandler(enabled = currentDestination != ScreenDestination.Home) {
                        when (currentDestination) {
                            is ScreenDestination.Onboarding -> {
                                finish()
                            }
                            is ScreenDestination.Home -> {
                                finish()
                            }
                            is ScreenDestination.SettingsHub -> {
                                currentDestination = ScreenDestination.Home
                            }
                            is ScreenDestination.VoiceSettings,
                            is ScreenDestination.BehaviorSettings,
                            is ScreenDestination.RecentTranscriptions,
                            is ScreenDestination.ConnectedDevices,
                            is ScreenDestination.HistoryPrivacy,
                            is ScreenDestination.AdvancedSettings -> {
                                currentDestination = ScreenDestination.SettingsHub
                            }
                            is ScreenDestination.ConnectNewDevice,
                            is ScreenDestination.ComputerConnection -> {
                                currentDestination = ScreenDestination.ConnectedDevices
                            }
                            is ScreenDestination.SnippetsManager -> {
                                currentDestination = ScreenDestination.BehaviorSettings
                            }
                        }
                    }

                    when (currentDestination) {
                        is ScreenDestination.Onboarding -> {
                            OnboardingScreen(
                                onSetupComplete = {
                                    app.secureConfig.hasCompletedOnboarding = true
                                    currentDestination = ScreenDestination.Home
                                }
                            )
                        }
                        is ScreenDestination.Home -> {
                            HomeScreen(
                                onNavigate = { destination ->
                                    currentDestination = destination
                                }
                            )
                        }
                        is ScreenDestination.SettingsHub -> {
                            SettingsScreen(
                                onNavigate = { destination ->
                                    currentDestination = destination
                                }
                            )
                        }
                        is ScreenDestination.VoiceSettings -> {
                            VoiceSettingsScreen(
                                onBack = { currentDestination = ScreenDestination.SettingsHub },
                                onNavigateToAdvanced = { currentDestination = ScreenDestination.AdvancedSettings }
                            )
                        }
                        is ScreenDestination.BehaviorSettings -> {
                            BehaviorSettingsScreen(
                                onBack = { currentDestination = ScreenDestination.SettingsHub },
                                onNavigateToCustomCommands = { currentDestination = ScreenDestination.SnippetsManager }
                            )
                        }
                        is ScreenDestination.RecentTranscriptions -> {
                            RecentTranscriptionsScreen(
                                onBack = { currentDestination = ScreenDestination.SettingsHub }
                            )
                        }
                        is ScreenDestination.ConnectedDevices -> {
                            ConnectedDevicesScreen(
                                onBack = { currentDestination = ScreenDestination.SettingsHub },
                                onNavigate = { destination -> currentDestination = destination },
                                onConnectNewDevice = { currentDestination = ScreenDestination.ConnectNewDevice }
                            )
                        }
                        is ScreenDestination.ConnectNewDevice,
                        is ScreenDestination.ComputerConnection -> {
                            ComputerConnectionScreen(
                                onBack = { currentDestination = ScreenDestination.ConnectedDevices }
                            )
                        }
                        is ScreenDestination.SnippetsManager -> {
                            SnippetsScreen(
                                onBack = { currentDestination = ScreenDestination.BehaviorSettings }
                            )
                        }
                        is ScreenDestination.HistoryPrivacy -> {
                            HistoryPrivacyScreen(
                                onBack = { currentDestination = ScreenDestination.SettingsHub }
                            )
                        }
                        is ScreenDestination.AdvancedSettings -> {
                            HistoryPrivacyScreen(
                                onBack = { currentDestination = ScreenDestination.SettingsHub }
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
