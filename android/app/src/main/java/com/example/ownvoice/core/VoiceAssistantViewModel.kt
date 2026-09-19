package com.example.ownvoice.core

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ownvoice.OwnVoiceApplication
import com.example.ownvoice.audio.AudioRecordStreamer
import com.example.ownvoice.network.BridgeClient
import com.example.ownvoice.network.GeminiRestClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as OwnVoiceApplication
    val config: SecureConfig get() = app.secureConfig

    private val audioRecorder = AudioRecordStreamer(16000)
    private val geminiClient = GeminiRestClient(app.secureConfig)
    private val bridgeClient = BridgeClient(app.secureConfig, application)

    private val _state = MutableStateFlow<VoiceAssistantState>(VoiceAssistantState.Idle)
    val state: StateFlow<VoiceAssistantState> = _state.asStateFlow()

    val amplitudeFlow: StateFlow<Float> = audioRecorder.amplitudeFlow

    private val _lastTranscription = MutableStateFlow("")
    val lastTranscription: StateFlow<String> = _lastTranscription.asStateFlow()

    private val _feedbackMessage = MutableStateFlow("")
    val feedbackMessage: StateFlow<String> = _feedbackMessage.asStateFlow()

    private val _isDrawerVisible = MutableStateFlow(false)
    val isDrawerVisible: StateFlow<Boolean> = _isDrawerVisible.asStateFlow()

    private var recordingStartTime = 0L

    fun toggleRecording() {
        when (_state.value) {
            is VoiceAssistantState.Idle, is VoiceAssistantState.Success, is VoiceAssistantState.Error -> {
                startRecording()
            }
            is VoiceAssistantState.Recording -> {
                stopAndTranscribe()
            }
            is VoiceAssistantState.Processing -> {
                // Ignore while in processing
            }
        }
    }

    fun startRecording() {
        if (_state.value is VoiceAssistantState.Recording) return

        val sensitivity = config.inputSensitivity
        val silenceSeconds = config.silenceDetectionSeconds

        val started = audioRecorder.startRecording(
            sensitivity = sensitivity,
            silenceDurationSeconds = silenceSeconds,
            onSilenceDetected = {
                viewModelScope.launch {
                    stopAndTranscribe()
                }
            }
        )

        if (started) {
            recordingStartTime = System.currentTimeMillis()
            _state.value = VoiceAssistantState.Recording(0L)
            _isDrawerVisible.value = true
            _feedbackMessage.value = "Listening... Speak naturally"
        } else {
            _state.value = VoiceAssistantState.Error("Microphone unavailable")
            _feedbackMessage.value = "Microphone unavailable. Please grant mic permission."
            _isDrawerVisible.value = true
        }
    }

    fun stopAndTranscribe() {
        if (_state.value !is VoiceAssistantState.Recording) return

        _state.value = VoiceAssistantState.Processing
        _feedbackMessage.value = "Transcribing with Gemini 3.6 Flash..."

        viewModelScope.launch {
            val wavBytes = withContext(Dispatchers.IO) {
                audioRecorder.stopRecording()
            }

            if (wavBytes.isEmpty() || wavBytes.size < 400) {
                _state.value = VoiceAssistantState.Error("Audio too short")
                _feedbackMessage.value = "Audio too short. Please speak longer."
                return@launch
            }

            try {
                val (rawText, latency) = geminiClient.transcribeAudio(
                    wavBytes = wavBytes,
                    mode = config.dictationMode
                )

                if (rawText.isBlank()) {
                    _state.value = VoiceAssistantState.Error("No speech detected")
                    _feedbackMessage.value = "No speech detected. Please try again."
                    return@launch
                }

                // Apply spoken snippets substitutions
                val expandedText = config.getSnippets().entries.fold(rawText) { acc, (k, v) ->
                    acc.replace(k, v)
                }

                _lastTranscription.value = expandedText

                // Determine target and save to persistent history
                val isPcActive = config.isUseForPcEnabled && config.desktopBridgeIp.isNotBlank()
                val targetName = if (isPcActive) "PC" else "Phone"
                config.addHistoryItem(expandedText, target = targetName)

                // Inject keystrokes to PC cursor if enabled
                if (isPcActive) {
                    val typed = withContext(Dispatchers.IO) {
                        bridgeClient.sendToDesktop(
                            desktopIp = config.desktopBridgeIp,
                            text = expandedText,
                            token = config.desktopBridgeToken
                        )
                    }
                    if (typed) {
                        _feedbackMessage.value = "Typed to PC cursor!"
                    } else {
                        _feedbackMessage.value = "Transcribed! (PC error: ${bridgeClient.lastError})"
                    }
                } else {
                    _feedbackMessage.value = "Transcribed successfully!"
                }

                _state.value = VoiceAssistantState.Success(
                    text = expandedText,
                    latencyMs = latency,
                    target = targetName
                )
            } catch (e: Exception) {
                val errMsg = e.message ?: "Transcription failed"
                _state.value = VoiceAssistantState.Error(errMsg)
                _feedbackMessage.value = "Error: $errMsg"
            }
        }
    }

    fun cancelRecording() {
        audioRecorder.cancelRecording()
        _state.value = VoiceAssistantState.Idle
        _feedbackMessage.value = ""
    }

    fun dismissDrawer() {
        if (_state.value is VoiceAssistantState.Recording) {
            audioRecorder.cancelRecording()
        }
        _isDrawerVisible.value = false
        _state.value = VoiceAssistantState.Idle
    }

    fun copyToClipboard(text: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OwnVoice Transcription", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(getApplication(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun sendToPcManual(text: String) {
        if (config.desktopBridgeIp.isBlank()) {
            Toast.makeText(getApplication(), "No PC connected. Pair in Connected Devices.", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                bridgeClient.sendToDesktop(
                    desktopIp = config.desktopBridgeIp,
                    text = text,
                    token = config.desktopBridgeToken
                )
            }
            if (ok) {
                Toast.makeText(getApplication(), "Sent to PC cursor!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(getApplication(), "PC error: ${bridgeClient.lastError}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.cancelRecording()
    }
}
