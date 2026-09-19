package com.example.ownvoice.ime.model

data class ClipboardItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isSensitive: Boolean = false
)
