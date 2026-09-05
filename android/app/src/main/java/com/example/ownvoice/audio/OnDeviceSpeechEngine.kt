package com.example.ownvoice.audio

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class OnDeviceSpeechEngine(private val context: Context) {

    companion object {
        fun isNetworkAvailable(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        fun isSpeechRecognitionAvailable(context: Context): Boolean {
            return SpeechRecognizer.isRecognitionAvailable(context)
        }
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onPartialResult: ((String) -> Unit)? = null
    ) {
        if (isListening) return

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NETWORK -> "Network offline"
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission missing"
                            else -> "Recognition error ($error)"
                        }
                        onError(msg)
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        onResult(text.trim())
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            onPartialResult?.invoke(text.trim())
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                // Prefer offline recognition if available on device
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            isListening = true
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            onError(e.localizedMessage ?: "Failed to start speech recognizer")
        }
    }

    fun stopListening() {
        if (isListening) {
            isListening = false
            try {
                speechRecognizer?.stopListening()
            } catch (ignored: Exception) {}
        }
    }

    fun cancel() {
        isListening = false
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (ignored: Exception) {}
        speechRecognizer = null
    }

    fun isCurrentlyListening(): Boolean = isListening
}
