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
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ownvoice.core.VoiceAssistantViewModel
import com.example.ownvoice.theme.DesignTokens
import com.example.ownvoice.theme.OwnVoiceTheme
import com.example.ownvoice.ui.HomeScreen
import com.example.ownvoice.ui.OnboardingScreen
import com.example.ownvoice.ui.SettingsScreen
import com.example.ownvoice.ui.components.BottomNavBar
import com.example.ownvoice.ui.components.LiveTranscriptionDrawer
import com.example.ownvoice.ui.navigation.ScreenDestination
import com.example.ownvoice.ui.screens.*

class MainActivity : ComponentActivity() {

    private val voiceViewModel: VoiceAssistantViewModel by viewModels()

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
                val voiceState by voiceViewModel.state.collectAsState()
                val amplitude by voiceViewModel.amplitudeFlow.collectAsState()
                val isDrawerVisible by voiceViewModel.isDrawerVisible.collectAsState()
                val feedbackMessage by voiceViewModel.feedbackMessage.collectAsState()
                val lastTranscription by voiceViewModel.lastTranscription.collectAsState()

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
                        is ScreenDestination.SettingsHub,
                        is ScreenDestination.MeetingMode -> {
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

                Scaffold(
                    containerColor = DesignTokens.Colors.BackgroundDark,
                    bottomBar = {
                        if (currentDestination !is ScreenDestination.Onboarding) {
                            BottomNavBar(
                                currentDestination = currentDestination,
                                onNavigate = { currentDestination = it },
                                voiceState = voiceState,
                                amplitude = amplitude,
                                showCenterMic = true,
                                onCenterMicClick = { voiceViewModel.toggleRecording() }
                            )
                        }
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
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
                                    viewModel = voiceViewModel,
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
                            is ScreenDestination.MeetingMode -> {
                                MeetingModeScreen(
                                    onBack = { currentDestination = ScreenDestination.Home }
                                )
                            }
                        }

                        // In-Situ Dynamic Live Voice Assistant Drawer
                        LiveTranscriptionDrawer(
                            visible = isDrawerVisible,
                            state = voiceState,
                            amplitude = amplitude,
                            feedbackMessage = feedbackMessage,
                            lastTranscription = lastTranscription,
                            onDismiss = { voiceViewModel.dismissDrawer() },
                            onCopy = { voiceViewModel.copyToClipboard(it) },
                            onSendToPc = { voiceViewModel.sendToPcManual(it) },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
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
