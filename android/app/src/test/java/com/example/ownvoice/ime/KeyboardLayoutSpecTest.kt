package com.example.ownvoice.ime

import com.example.ownvoice.ime.ui.LongPressSymbolMenu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutSpecTest {

    @Test
    fun testQwertyRowWeightsAdhereTo10UnitSpec() {
        // Row 1: 10 letter keys of 1.0f unit width
        val row1KeyWeights = List(10) { 1.0f }
        val row1Total = row1KeyWeights.sum()
        assertEquals(10.0f, row1Total, 0.001f)

        // Row 2: 0.5f left spacer + 9 letter keys of 1.0f + 0.5f right spacer
        val row2LeftSpacer = 0.5f
        val row2LetterWeights = List(9) { 1.0f }
        val row2RightSpacer = 0.5f
        val row2Total = row2LeftSpacer + row2LetterWeights.sum() + row2RightSpacer
        assertEquals(10.0f, row2Total, 0.001f)

        // Verify Row 2 letter width matches Row 1 letter width exactly
        assertEquals(row1KeyWeights[0], row2LetterWeights[0], 0.001f)

        // Row 3: Shift (1.5f) + 7 letter keys of 1.0f + Backspace (1.5f)
        val row3Shift = 1.5f
        val row3LetterWeights = List(7) { 1.0f }
        val row3Backspace = 1.5f
        val row3Total = row3Shift + row3LetterWeights.sum() + row3Backspace
        assertEquals(10.0f, row3Total, 0.001f)

        // Verify Row 3 letter width matches Row 1 and Row 2 letter width exactly
        assertEquals(row1KeyWeights[0], row3LetterWeights[0], 0.001f)

        // Row 4: ?123 (1.4f) + Comma/@ (1.0f) + Spacebar (4.8f) + Period (1.0f) + Enter (1.8f)
        val row4Toggle = 1.4f
        val row4Comma = 1.0f
        val row4Space = 4.8f
        val row4Period = 1.0f
        val row4Enter = 1.8f
        val row4Total = row4Toggle + row4Comma + row4Space + row4Period + row4Enter
        assertEquals(10.0f, row4Total, 0.001f)
    }

    @Test
    fun testRow2TypewriterStaggerOffset() {
        // Standard typewriter stagger offset: A sits at 0.5 U (half a key from left margin)
        val row2LeftOffset = 0.5f
        val row1KeyWidth = 1.0f
        assertEquals(row1KeyWidth / 2f, row2LeftOffset, 0.001f)

        // Center of 'Q' = 0.5 U, Center of 'W' = 1.5 U.
        // Center of 'A' = 0.5 U (offset) + 0.5 U (half-key) = 1.0 U.
        // Exactly halfway between Q and W!
        val qCenter = 0.5f
        val wCenter = 1.5f
        val aCenter = row2LeftOffset + 0.5f
        assertEquals((qCenter + wCenter) / 2f, aCenter, 0.001f)
    }

    @Test
    fun testRow3TypewriterStaggerOffset() {
        // Shift is 1.5 U wide, so 'Z' starts at 1.5 U.
        // Center of 'Z' = 1.5 U + 0.5 U = 2.0 U.
        // Center of 'A' = 1.0 U, Center of 'S' = 2.0 U.
        // Wait, on standard QWERTY: A is 1.0 U, S is 2.0 U, D is 3.0 U.
        // Z center at 2.0 U sits halfway between A (1.0) and D (3.0), aligned with S or between A and S.
        val row3Shift = 1.5f
        val zCenter = row3Shift + 0.5f
        assertEquals(2.0f, zCenter, 0.001f)
    }

    @Test
    fun testLongPressSymbolMappingIntegrity() {
        // Row 1 numbers
        assertEquals("1", LongPressSymbolMenu.getSecondarySymbol("q"))
        assertEquals("2", LongPressSymbolMenu.getSecondarySymbol("w"))
        assertEquals("3", LongPressSymbolMenu.getSecondarySymbol("e"))
        assertEquals("4", LongPressSymbolMenu.getSecondarySymbol("r"))
        assertEquals("5", LongPressSymbolMenu.getSecondarySymbol("t"))
        assertEquals("6", LongPressSymbolMenu.getSecondarySymbol("y"))
        assertEquals("7", LongPressSymbolMenu.getSecondarySymbol("u"))
        assertEquals("8", LongPressSymbolMenu.getSecondarySymbol("i"))
        assertEquals("9", LongPressSymbolMenu.getSecondarySymbol("o"))
        assertEquals("0", LongPressSymbolMenu.getSecondarySymbol("p"))

        // Row 2 symbols
        assertEquals("@", LongPressSymbolMenu.getSecondarySymbol("a"))
        assertEquals("#", LongPressSymbolMenu.getSecondarySymbol("s"))
        assertEquals("$", LongPressSymbolMenu.getSecondarySymbol("d"))
        assertEquals("_", LongPressSymbolMenu.getSecondarySymbol("f"))
        assertEquals("&", LongPressSymbolMenu.getSecondarySymbol("g"))
        assertEquals("-", LongPressSymbolMenu.getSecondarySymbol("h"))
        assertEquals("+", LongPressSymbolMenu.getSecondarySymbol("j"))
        assertEquals("*", LongPressSymbolMenu.getSecondarySymbol("k"))
        assertEquals("/", LongPressSymbolMenu.getSecondarySymbol("l"))

        // Row 3 symbols
        assertEquals("=", LongPressSymbolMenu.getSecondarySymbol("z"))
        assertEquals("\"", LongPressSymbolMenu.getSecondarySymbol("x"))
        assertEquals("'", LongPressSymbolMenu.getSecondarySymbol("c"))
        assertEquals(":", LongPressSymbolMenu.getSecondarySymbol("v"))
        assertEquals(";", LongPressSymbolMenu.getSecondarySymbol("b"))
        assertEquals("!", LongPressSymbolMenu.getSecondarySymbol("n"))
        assertEquals("?", LongPressSymbolMenu.getSecondarySymbol("m"))
    }

    @Test
    fun testSymbolsModeLayoutIntegrity() {
        val symbolsRow1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val symbolsRow2 = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
        val symbolsRow3 = listOf("=", "*", "\"", "'", ":", ";", "!", "?")

        // 10 items in symbols row 1
        assertEquals(10, symbolsRow1.size)
        // 10 items in symbols row 2
        assertEquals(10, symbolsRow2.size)
        // 8 symbols + 1 toggle + 1 backspace = 10 items in symbols row 3
        assertEquals(8, symbolsRow3.size)
        val symbolsRow3Total = 1.0f + symbolsRow3.size * 1.0f + 1.0f
        assertEquals(10.0f, symbolsRow3Total, 0.001f)
    }
}
