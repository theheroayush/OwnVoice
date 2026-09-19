package com.example.ownvoice.ime.model

enum class TrackpadAction {
    MOVE,
    CLICK,
    DOWN,
    UP,
    SCROLL
}

data class TrackpadEvent(
    val action: TrackpadAction,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val button: String = "left",
    val isDouble: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
