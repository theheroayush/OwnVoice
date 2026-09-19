package com.example.ownvoice.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.ownvoice.MainActivity
import com.example.ownvoice.OwnVoiceApplication
import com.example.ownvoice.R
import com.example.ownvoice.audio.AudioRecordStreamer
import com.example.ownvoice.network.GeminiRestClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Android Foreground Service for autonomous, long-form meeting recording.
 * Holds PARTIAL_WAKE_LOCK to prevent CPU sleep, slices audio into rolling chunks,
 * transcribes with Gemini, and incrementally synthesizes Key Decisions and Action Items.
 */
class MeetingRecordingService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START_MEETING"
        const val ACTION_STOP = "ACTION_STOP_MEETING"
        private const val NOTIFICATION_ID = 3001
        private const val CHANNEL_ID = "meeting_recording_channel"

        private val _isRecording = MutableStateFlow(false)
        val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

        private val _durationSeconds = MutableStateFlow(0L)
        val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()

        private val _liveTranscript = MutableStateFlow("")
        val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

        private val _decisions = MutableStateFlow<List<String>>(emptyList())
        val decisions: StateFlow<List<String>> = _decisions.asStateFlow()

        private val _actionItems = MutableStateFlow<List<String>>(emptyList())
        val actionItems: StateFlow<List<String>> = _actionItems.asStateFlow()

        private val _executiveSummary = MutableStateFlow("")
        val executiveSummary: StateFlow<String> = _executiveSummary.asStateFlow()

        private val _isProcessingChunk = MutableStateFlow(false)
        val isProcessingChunk: StateFlow<Boolean> = _isProcessingChunk.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, MeetingRecordingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MeetingRecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun clearSession() {
            _liveTranscript.value = ""
            _decisions.value = emptyList()
            _actionItems.value = emptyList()
            _executiveSummary.value = ""
            _durationSeconds.value = 0L
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var audioRecorder: AudioRecordStreamer
    private lateinit var geminiClient: GeminiRestClient

    private var timerJob: Job? = null
    private var recordingLoopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as OwnVoiceApplication
        audioRecorder = AudioRecordStreamer(16000)
        geminiClient = GeminiRestClient(app.secureConfig)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OwnVoice:MeetingRecordingWakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMeetingSession()
            ACTION_STOP -> stopMeetingSession()
        }
        return START_NOT_STICKY
    }

    private fun startMeetingSession() {
        if (_isRecording.value) return

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Meeting recording active..."))

        wakeLock?.acquire(4 * 60 * 60 * 1000L) // up to 4 hours meeting lock
        _isRecording.value = true

        startTimer()
        startRollingRecordingLoop()
    }

    private fun stopMeetingSession() {
        _isRecording.value = false
        timerJob?.cancel()
        recordingLoopJob?.cancel()

        try {
            audioRecorder.stopRecording()
        } catch (_: Exception) {}

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (_isRecording.value) {
                delay(1000)
                _durationSeconds.value += 1
            }
        }
    }

    private fun startRollingRecordingLoop() {
        recordingLoopJob?.cancel()
        recordingLoopJob = serviceScope.launch {
            while (_isRecording.value) {
                try {
                    audioRecorder.startRecording()
                    // Record in 35-second rolling chunks
                    delay(35000)

                    if (!_isRecording.value) break

                    val wavBytes = withContext(Dispatchers.IO) {
                        audioRecorder.stopRecording()
                    }

                    if (wavBytes.isNotEmpty() && wavBytes.size > 2000) {
                        _isProcessingChunk.value = true
                        processAudioChunk(wavBytes)
                        _isProcessingChunk.value = false
                    }
                } catch (e: Exception) {
                    _isProcessingChunk.value = false
                    delay(1000)
                }
            }
        }
    }

    private suspend fun processAudioChunk(wavBytes: ByteArray) {
        try {
            val (chunkText, _) = geminiClient.transcribeAudio(wavBytes, mode = "verbatim")
            val cleanChunk = chunkText.trim()
            if (cleanChunk.isNotBlank()) {
                val updatedTranscript = if (_liveTranscript.value.isBlank()) cleanChunk else "${_liveTranscript.value} $cleanChunk"
                _liveTranscript.value = updatedTranscript

                // Incrementally synthesize Decisions, Action Items, and Executive Summary
                synthesizeMeetingIntelligence(updatedTranscript)
            }
        } catch (e: Exception) {
            // Keep recording even if single chunk processing experiences a momentary network timeout
        }
    }

    private suspend fun synthesizeMeetingIntelligence(transcript: String) {
        if (transcript.length < 50) return

        val instruction = """
            You are an expert executive meeting secretary.
            Analyze the following transcript from an ongoing meeting:
            ===
            $transcript
            ===
            Synthesize key takeaways into raw JSON with this exact format:
            {
              "summary": "2-3 sentence executive synopsis of topics covered.",
              "decisions": ["Key decision 1", "Key decision 2"],
              "action_items": ["Action item 1 (Assigned person if known)", "Action item 2"]
            }
            Output ONLY valid raw JSON with zero markdown code fences or commentary.
        """.trimIndent()

        try {
            val (jsonResponse, _) = geminiClient.reshapeText(transcript, instruction)
            val cleanJson = jsonResponse.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = JSONObject(cleanJson)

            val sum = parsed.optString("summary", "")
            if (sum.isNotBlank()) {
                _executiveSummary.value = sum
            }

            val decArr = parsed.optJSONArray("decisions")
            if (decArr != null) {
                val list = mutableListOf<String>()
                for (i in 0 until decArr.length()) {
                    list.add(decArr.getString(i))
                }
                _decisions.value = list
            }

            val actArr = parsed.optJSONArray("action_items")
            if (actArr != null) {
                val list = mutableListOf<String>()
                for (i in 0 until actArr.length()) {
                    list.add(actArr.getString(i))
                }
                _actionItems.value = list
            }
        } catch (_: Exception) {
            // synthesis fallback
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Meeting Recording Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running long-form meeting transcription and executive intelligence"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OwnVoice Meeting Mode")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopMeetingSession()
        super.onDestroy()
    }
}
