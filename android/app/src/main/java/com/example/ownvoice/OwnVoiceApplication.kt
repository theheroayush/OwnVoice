package com.example.ownvoice

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.ownvoice.core.SecureConfig

class OwnVoiceApplication : Application() {

    companion object {
        const val CHANNEL_ID_OVERLAY = "ownvoice_overlay_channel"
        const val CHANNEL_ID_RECORDING = "ownvoice_recording_channel"
        lateinit var instance: OwnVoiceApplication
            private set
    }

    lateinit var secureConfig: SecureConfig
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        secureConfig = SecureConfig(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val overlayChannel = NotificationChannel(
                CHANNEL_ID_OVERLAY,
                "OwnVoice Floating Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the OwnVoice floating bubble ready on your screen"
                setShowBadge(false)
            }

            val recordingChannel = NotificationChannel(
                CHANNEL_ID_RECORDING,
                "OwnVoice Active Recording",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active voice dictation session"
            }

            notificationManager.createNotificationChannel(overlayChannel)
            notificationManager.createNotificationChannel(recordingChannel)
        }
    }
}
