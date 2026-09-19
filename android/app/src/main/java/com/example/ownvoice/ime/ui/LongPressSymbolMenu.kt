package com.example.ownvoice.ime.ui

object LongPressSymbolMenu {
    private val symbolMap = mapOf(
        // Row 1: Numbers
        "q" to "1", "Q" to "1",
        "w" to "2", "W" to "2",
        "e" to "3", "E" to "3",
        "r" to "4", "R" to "4",
        "t" to "5", "T" to "5",
        "y" to "6", "Y" to "6",
        "u" to "7", "U" to "7",
        "i" to "8", "I" to "8",
        "o" to "9", "O" to "9",
        "p" to "0", "P" to "0",

        // Row 2: Common Symbols
        "a" to "@", "A" to "@",
        "s" to "#", "S" to "#",
        "d" to "$", "D" to "$",
        "f" to "_", "F" to "_",
        "g" to "&", "G" to "&",
        "h" to "-", "H" to "-",
        "j" to "+", "J" to "+",
        "k" to "*", "K" to "*",
        "l" to "/", "L" to "/",

        // Row 3: Punctuation & Quotes
        "z" to "=", "Z" to "=",
        "x" to "\"", "X" to "\"",
        "c" to "'", "C" to "'",
        "v" to ":", "V" to ":",
        "b" to ";", "B" to ";",
        "n" to "!", "N" to "!",
        "m" to "?", "M" to "?"
    )

    fun getSecondarySymbol(char: String): String? {
        return symbolMap[char]
    }
}
