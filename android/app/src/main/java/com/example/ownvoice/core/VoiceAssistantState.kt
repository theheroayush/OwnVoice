package com.example.ownvoice.core

sealed interface VoiceAssistantState {
    object Idle : VoiceAssistantState
    data class Recording(val durationMs: Long = 0L) : VoiceAssistantState
    object Processing : VoiceAssistantState
    data class Success(
        val text: String,
        val latencyMs: Long = 0L,
        val target: String = "PC"
    ) : VoiceAssistantState
    data class Error(val message: String) : VoiceAssistantState
}
