package com.Sunset.REN.GitHub.domain.filemanager

import org.junit.Assert.assertEquals
import org.junit.Test

class TextCursorPositionPolicyTest {
    @Test
    fun calculateReturnsFirstLineAndColumnForStartOfFile() {
        val position = TextCursorPositionPolicy.calculate("hello", selectionStart = 0, selectionEnd = 0)

        assertEquals(1, position.line)
        assertEquals(1, position.column)
        assertEquals(0, position.selectionLength)
    }

    @Test
    fun calculateTracksLfLineAndColumn() {
        val position = TextCursorPositionPolicy.calculate("one\ntwo\nthree", selectionStart = 8, selectionEnd = 8)

        assertEquals(3, position.line)
        assertEquals(1, position.column)
        assertEquals(0, position.selectionLength)
    }

    @Test
    fun calculateTreatsCrLfAsSingleLineBreak() {
        val position = TextCursorPositionPolicy.calculate("one\r\ntwo", selectionStart = 6, selectionEnd = 6)

        assertEquals(2, position.line)
        assertEquals(2, position.column)
        assertEquals(0, position.selectionLength)
    }

    @Test
    fun calculateReportsSelectionLength() {
        val position = TextCursorPositionPolicy.calculate("abcdef", selectionStart = 1, selectionEnd = 4)

        assertEquals(1, position.line)
        assertEquals(5, position.column)
        assertEquals(3, position.selectionLength)
    }

    @Test
    fun calculateCoercesSelectionIntoContentBounds() {
        val position = TextCursorPositionPolicy.calculate("a\nb", selectionStart = -10, selectionEnd = 50)

        assertEquals(2, position.line)
        assertEquals(2, position.column)
        assertEquals(3, position.selectionLength)
    }
}