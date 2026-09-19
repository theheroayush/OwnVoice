package com.example.ownvoice.ime.model

enum class KeyboardLayoutMode {
    QWERTY,
    SYMBOLS,
    NUMERIC_PAD,
    EMAIL,
    EMOJI,
    CLIPBOARD
}

enum class ActionKeyType(val defaultLabel: String) {
    ENTER("↵"),
    SEARCH("🔍"),
    SEND("✈"),
    GO("➔"),
    NEXT("⇥"),
    DONE("✓")
}
