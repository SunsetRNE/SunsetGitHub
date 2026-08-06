package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.domain.repo.RepositoryFilePreview

object RepositoryFilePreviewTextFormatter {

    fun format(preview: RepositoryFilePreview, maxLines: Int): RepositoryFilePreviewText {
        val isMarkdown = preview.name.isMarkdownFileName() || preview.path.isMarkdownFileName()
        val isReadme = preview.name.isReadmeFileName() || preview.path.isReadmeFileName()
        val formattedText = if (isMarkdown || isReadme) {
            preview.text.toPlainPreviewText(maxLines)
        } else {
            preview.text.toNumberedPreviewText(maxLines)
        }
        return RepositoryFilePreviewText(
            text = formattedText,
            isMarkdown = isMarkdown || isReadme,
            isDocument = isMarkdown || isReadme
        )
    }

    private fun String.isReadmeFileName(): Boolean {
        val lowerName = substringAfterLast('/').lowercase()
        return lowerName == "readme" || lowerName.startsWith("readme.")
    }

    private fun String.isMarkdownFileName(): Boolean {
        val lowerName = lowercase()
        return lowerName == "readme" ||
            lowerName.endsWith(".md") ||
            lowerName.endsWith(".markdown") ||
            lowerName.endsWith(".mdown") ||
            lowerName.endsWith(".mkdn")
    }

    private fun String.toPlainPreviewText(maxLines: Int): String {
        val lines = lineSequence().toList()
        val visibleText = lines.take(maxLines).joinToString(separator = "\n")
        if (lines.size <= maxLines) return visibleText
        return buildString {
            append(visibleText)
            append("\n\n已省略 ")
            append(lines.size - maxLines)
            append(" 行，仅展示前 ")
            append(maxLines)
            append(" 行。")
        }
    }

    private fun String.toNumberedPreviewText(maxLines: Int): String {
        val lines = lineSequence().toList()
        val visibleLines = lines.take(maxLines)
        val lineNumberWidth = visibleLines.size.coerceAtLeast(1).toString().length
        val numberedText = visibleLines.mapIndexed { index, line ->
            "${(index + 1).toString().padStart(lineNumberWidth)} | $line"
        }.joinToString(separator = "\n")
        if (lines.size <= maxLines) return numberedText
        return buildString {
            append(numberedText)
            append("\n… | 已省略 ")
            append(lines.size - maxLines)
            append(" 行，仅展示前 ")
            append(maxLines)
            append(" 行。")
        }
    }
}

data class RepositoryFilePreviewText(
    val text: String,
    val isMarkdown: Boolean,
    val isDocument: Boolean
)