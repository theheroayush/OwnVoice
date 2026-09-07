package com.example.ownvoice.ui.navigation

sealed class ScreenDestination {
    data class Onboarding(val step: Int = 1) : ScreenDestination()
    data object Home : ScreenDestination()
    data object SettingsHub : ScreenDestination()
    data object VoiceSettings : ScreenDestination()
    data object BehaviorSettings : ScreenDestination()
    data object RecentTranscriptions : ScreenDestination()
    data object ConnectedDevices : ScreenDestination()
    data object ConnectNewDevice : ScreenDestination()
    data object ComputerConnection : ScreenDestination()
    data object SnippetsManager : ScreenDestination()
    data object HistoryPrivacy : ScreenDestination()
    data object AdvancedSettings : ScreenDestination()
}
