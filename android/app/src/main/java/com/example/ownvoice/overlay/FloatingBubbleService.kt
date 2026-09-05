package com.example.ownvoice.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.*
import android.Manifest
import android.content.pm.PackageManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.ownvoice.MainActivity
import com.example.ownvoice.OwnVoiceApplication
import com.example.ownvoice.R
import com.example.ownvoice.audio.AudioRecordStreamer
import com.example.ownvoice.core.SnippetEngine
import com.example.ownvoice.core.TonePromptManager
import com.example.ownvoice.ime.OwnVoiceInputMethodService
import com.example.ownvoice.network.GeminiRestClient
import kotlinx.coroutines.*
import kotlin.math.abs

class FloatingBubbleService : Service() {

    companion object {
        const val ACTION_START_BUBBLE = "ACTION_START_BUBBLE"
        const val ACTION_STOP_BUBBLE = "ACTION_STOP_BUBBLE"
        private const val NOTIFICATION_ID = 2001
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private lateinit var audioRecorder: AudioRecordStreamer
    private lateinit var geminiClient: GeminiRestClient
    private lateinit var snippetEngine: SnippetEngine
    private var vibrator: Vibrator? = null
    private var toneGenerator: android.media.ToneGenerator? = null

    private var isRecording = false
    private var isProcessing = false
    private var activeSessionToneOverride: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as OwnVoiceApplication
        audioRecorder = AudioRecordStreamer(16000)
        geminiClient = GeminiRestClient(app.secureConfig)
        snippetEngine = SnippetEngine(app.secureConfig)
        vibrator = getSystemService(Vibrator::class.java)
        windowManager = getSystemService(WindowManager::class.java)

        try {
            toneGenerator = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 60)
        } catch (ignored: Exception) {}

        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        createBubbleView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_BUBBLE -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private fun buildForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, OwnVoiceApplication.CHANNEL_ID_OVERLAY)
            .setContentTitle(getString(R.string.bubble_notification_title))
            .setContentText(getString(R.string.bubble_notification_desc))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createBubbleView() {
        if (bubbleView != null) return

        val app = application as OwnVoiceApplication
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = app.secureConfig.bubbleX
            y = app.secureConfig.bubbleY
        }

        // Inflate custom programmatic layout
        val rootLayout = FrameLayout(this).apply {
            setPadding(12, 12, 12, 12)
        }

        // Capsule background
        val capsule = FrameLayout(this).apply {
            val bg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0xEE1A1A1E.toInt())
                setStroke(3, 0xFF33333E.toInt())
            }
            background = bg
            layoutParams = FrameLayout.LayoutParams(130, 130)
        }

        // Inner App Icon Logo
        val micIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_bubble_icon)
            val lp = FrameLayout.LayoutParams(92, 92).apply {
                gravity = Gravity.CENTER
            }
            layoutParams = lp
        }

        // Loading spinner
        val spinner = ProgressBar(this).apply {
            visibility = View.GONE
            val lp = FrameLayout.LayoutParams(70, 70).apply {
                gravity = Gravity.CENTER
            }
            layoutParams = lp
        }

        capsule.addView(micIcon)
        capsule.addView(spinner)
        rootLayout.addView(capsule)

        // Touch & Drag Handling with Snapping and Long-Press Quick Tone
        rootLayout.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false
            private var downTimestamp = 0L

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                val params = layoutParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        downTimestamp = System.currentTimeMillis()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (abs(dx) > 10 || abs(dy) > 10) {
                            isDragging = true
                        }
                        if (isDragging) {
                            params.x = (initialX + dx).toInt()
                            params.y = (initialY + dy).toInt()
                            windowManager?.updateViewLayout(rootLayout, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val pressDuration = System.currentTimeMillis() - downTimestamp
                        if (!isDragging) {
                            if (pressDuration >= 450) {
                                // Long press -> Cycle quick tone
                                cycleQuickTone()
                            } else {
                                // Single tap -> Toggle recording
                                handleBubbleClick(capsule, micIcon, spinner)
                            }
                        } else {
                            // Snap to edge
                            snapToEdge(params, rootLayout)
                            app.secureConfig.bubbleX = params.x
                            app.secureConfig.bubbleY = params.y
                        }
                        return true
                    }
                }
                return false
            }
        })

        bubbleView = rootLayout
        try {
            windowManager?.addView(bubbleView, layoutParams)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun cycleQuickTone() {
        val tones = listOf(
            null to "Auto-Context",
            "bullet_notes" to "Bullet Notes",
            "translate_hindi" to "English ➔ Hindi",
            "translate_english" to "Hindi ➔ English",
            "code" to "Coding / Shell",
            "formal_email" to "Executive Email"
        )
        val currentIndex = tones.indexOfFirst { it.first == activeSessionToneOverride }
        val nextIndex = (currentIndex + 1) % tones.size
        val (nextTone, label) = tones[nextIndex]
        activeSessionToneOverride = nextTone
        vibrate(60)
        Toast.makeText(this, "Quick Mode: $label", Toast.LENGTH_SHORT).show()
    }

    private fun snapToEdge(params: WindowManager.LayoutParams, view: View) {
        val display = windowManager?.defaultDisplay ?: return
        val size = Point()
        display.getSize(size)
        val screenWidth = size.x
        params.x = if (params.x < screenWidth / 2) 20 else screenWidth - 150
        windowManager?.updateViewLayout(view, params)
    }

    private fun handleBubbleClick(capsule: FrameLayout, micIcon: ImageView, spinner: ProgressBar) {
        if (isProcessing) return

        if (!isRecording) {
            val app = application as OwnVoiceApplication

            // 1. Check microphone permission
            val hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (!hasMic) {
                vibrate(120)
                Toast.makeText(this, "Microphone permission required! Open OwnVoice to allow.", Toast.LENGTH_LONG).show()
                return
            }

            // 2. Check Gemini API key
            if (app.secureConfig.apiKey.isBlank()) {
                vibrate(120)
                Toast.makeText(this, "API Key missing! Open OwnVoice app to enter key.", Toast.LENGTH_LONG).show()
                return
            }

            val started = audioRecorder.startRecording()
            if (started) {
                isRecording = true
                vibrate(40)
                if (app.secureConfig.isSoundEffectsEnabled) {
                    try { toneGenerator?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 35) } catch (ignored: Exception) {}
                }
                (capsule.background as? android.graphics.drawable.GradientDrawable)?.apply {
                    setColor(0xEEFF453A.toInt())
                    setStroke(4, 0xFFFF453A.toInt())
                }
            } else {
                vibrate(120)
                Toast.makeText(this, "Microphone is busy or locked by another app", Toast.LENGTH_SHORT).show()
            }
        } else {
            val app = application as OwnVoiceApplication
            // Stop and transcribe
            isRecording = false
            isProcessing = true
            vibrate(40)
            if (app.secureConfig.isSoundEffectsEnabled) {
                try { toneGenerator?.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 40) } catch (ignored: Exception) {}
            }

            micIcon.visibility = View.GONE
            spinner.visibility = View.VISIBLE
            (capsule.background as? android.graphics.drawable.GradientDrawable)?.apply {
                setColor(0xEE1E1E24.toInt())
                setStroke(3, 0xFF0A84FF.toInt())
            }

            val activePkg = OverlayAccessibilityService.getActiveAppPackage()
            val effectiveTone = if (activeSessionToneOverride != null) {
                activeSessionToneOverride!!
            } else if (app.secureConfig.isAutoContextToneEnabled && activePkg.isNotBlank()) {
                TonePromptManager.detectModeForPackage(activePkg)
            } else {
                app.secureConfig.dictationMode
            }

            serviceScope.launch {
                val wavBytes = withContext(Dispatchers.IO) {
                    audioRecorder.stopRecording()
                }

                if (wavBytes.isEmpty() || wavBytes.size < 400) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FloatingBubbleService, "No speech detected (empty audio)", Toast.LENGTH_SHORT).show()
                        vibrate(80)
                    }
                } else {
                    try {
                        val (text, _) = geminiClient.transcribeAudio(wavBytes, mode = effectiveTone)
                        if (text.isNotBlank()) {
                            val expanded = snippetEngine.expand(text)
                            withContext(Dispatchers.Main) {
                                // Tier 1: Direct native InputConnection injection if OwnVoice keyboard is active
                                var typed = false
                                if (OwnVoiceInputMethodService.isKeyboardActive()) {
                                    typed = OwnVoiceInputMethodService.injectText(expanded)
                                }

                                // Tier 2: Multi-window accessibility injection (Paste / Set Text)
                                if (!typed) {
                                    val outcome = OverlayAccessibilityService.injectText(expanded)
                                    when (outcome) {
                                        OverlayAccessibilityService.InjectionOutcome.PASTED,
                                        OverlayAccessibilityService.InjectionOutcome.TEXT_SET -> {
                                            typed = true
                                            Toast.makeText(this@FloatingBubbleService, "Typed: \"$expanded\"", Toast.LENGTH_SHORT).show()
                                            vibrate(30)
                                        }
                                        OverlayAccessibilityService.InjectionOutcome.CLIPBOARD_ONLY -> {
                                            Toast.makeText(this@FloatingBubbleService, "Copied to clipboard: \"$expanded\" (Tap input box & paste)", Toast.LENGTH_LONG).show()
                                            vibrate(40)
                                        }
                                        OverlayAccessibilityService.InjectionOutcome.FAILED -> {
                                            Toast.makeText(this@FloatingBubbleService, "Copied: \"$expanded\"\nEnable Accessibility in Settings to auto-type!", Toast.LENGTH_LONG).show()
                                            vibrate(60)
                                        }
                                    }
                                } else {
                                    Toast.makeText(this@FloatingBubbleService, "Typed: \"$expanded\"", Toast.LENGTH_SHORT).show()
                                    vibrate(30)
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@FloatingBubbleService, "No words recognized (silence)", Toast.LENGTH_SHORT).show()
                                vibrate(80)
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            val errMsg = e.localizedMessage ?: "Transcription failed"
                            Toast.makeText(this@FloatingBubbleService, errMsg, Toast.LENGTH_LONG).show()
                            vibrate(120)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    isProcessing = false
                    micIcon.visibility = View.VISIBLE
                    spinner.visibility = View.GONE
                    (capsule.background as? android.graphics.drawable.GradientDrawable)?.apply {
                        setColor(0xEE1A1A1E.toInt())
                        setStroke(3, 0xFF33333E.toInt())
                    }
                }
            }
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (ignored: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        audioRecorder.cancelRecording()
        try {
            toneGenerator?.release()
        } catch (ignored: Exception) {}
        toneGenerator = null
        if (bubbleView != null) {
            windowManager?.removeView(bubbleView)
            bubbleView = null
        }
    }
}
