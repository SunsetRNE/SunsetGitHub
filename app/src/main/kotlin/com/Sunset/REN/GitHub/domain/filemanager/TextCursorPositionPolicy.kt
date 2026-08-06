package com.Sunset.REN.GitHub.domain.filemanager

object TextCursorPositionPolicy {
    data class CursorPosition(
        val line: Int,
        val column: Int,
        val selectionLength: Int
    )

    fun calculate(content: String, selectionStart: Int, selectionEnd: Int): CursorPosition {
        val start = selectionStart.coerceIn(0, content.length)
        val end = selectionEnd.coerceIn(0, content.length)
        val caret = end
        var line = 1
        var column = 1
        var index = 0
        while (index < caret) {
            when (content[index]) {
                '\r' -> {
                    line += 1
                    column = 1
                    if (index + 1 < caret && content[index + 1] == '\n') {
                        index += 2
                    } else {
                        index += 1
                    }
                }
                '\n' -> {
                    line += 1
                    column = 1
                    index += 1
                }
                else -> {
                    column += 1
                    index += 1
                }
            }
        }
        return CursorPosition(
            line = line,
            column = column,
            selectionLength = kotlin.math.abs(end - start)
        )
    }
}