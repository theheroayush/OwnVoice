package com.example.ownvoice.ime.model

enum class KeystrokeType {
    CHAR,
    BACKSPACE,
    ENTER,
    SPACE,
    CLEAR
}

data class KeystrokeEvent(
    val type: KeystrokeType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
