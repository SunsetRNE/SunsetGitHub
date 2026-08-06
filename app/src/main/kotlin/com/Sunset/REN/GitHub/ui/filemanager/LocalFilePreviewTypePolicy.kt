package com.Sunset.REN.GitHub.ui.filemanager

import com.Sunset.REN.GitHub.domain.filemanager.ArchiveFormatResolver
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType

object LocalFilePreviewTypePolicy {
    fun shouldShowMarkdownPreviewByDefault(openMode: String, entryType: FileEntryType, displayName: String, displayPath: String): Boolean {
        return openMode != MODE_EDIT && canPreviewAsMarkdown(entryType, displayName, displayPath)
    }

    const val MODE_EDIT = "edit"

    fun canPreviewAsMarkdown(entryType: FileEntryType, displayName: String, displayPath: String): Boolean {
        return entryType == FileEntryType.Markdown || isMarkdownFileName(displayName) || isMarkdownFileName(displayPath)
    }

    fun isSpecializedPreview(entryType: FileEntryType, displayName: String, displayPath: String): Boolean {
        return canPreviewAsImage(entryType, displayName, displayPath) ||
            canPreviewAsApk(entryType, displayName, displayPath) ||
            canPreviewAsZipArchive(entryType, displayName, displayPath)
    }

    fun canExtractDocxText(displayName: String, displayPath: String): Boolean {
        return isDocxFileName(displayName) || isDocxFileName(displayPath)
    }

    fun canExtractPdfText(displayName: String, displayPath: String): Boolean {
        return isPdfFileName(displayName) || isPdfFileName(displayPath)
    }

    fun canPreviewAsImage(entryType: FileEntryType, displayName: String, displayPath: String): Boolean {
        return entryType == FileEntryType.Image || isImageFileName(displayName) || isImageFileName(displayPath)
    }

    fun canPreviewAsZipArchive(entryType: FileEntryType, displayName: String, displayPath: String): Boolean {
        if (canExtractDocxText(displayName, displayPath)) return false
        return entryType == FileEntryType.Archive && (isArchiveFileName(displayName) || isArchiveFileName(displayPath))
    }

    fun canPreviewAsApk(entryType: FileEntryType, displayName: String, displayPath: String): Boolean {
        return entryType == FileEntryType.Apk || isApkFileName(displayName) || isApkFileName(displayPath)
    }

    private fun isMarkdownFileName(path: String): Boolean {
        val name = path.substringAfterLast('/').lowercase()
        return name == "readme" ||
            name.startsWith("readme.") ||
            name.endsWith(".md") ||
            name.endsWith(".markdown") ||
            name.endsWith(".mdown") ||
            name.endsWith(".mkdn")
    }

    private fun isImageFileName(path: String): Boolean {
        return when (path.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "heic", "heif", "svg" -> true
            else -> false
        }
    }

    private fun isArchiveFileName(path: String): Boolean {
        return ArchiveFormatResolver.isPreviewSupported(path)
    }

    private fun isDocxFileName(path: String): Boolean {
        return path.substringAfterLast('.', missingDelimiterValue = "").equals("docx", ignoreCase = true)
    }

    private fun isPdfFileName(path: String): Boolean {
        return path.substringAfterLast('.', missingDelimiterValue = "").equals("pdf", ignoreCase = true)
    }

    private fun isApkFileName(path: String): Boolean {
        return path.substringAfterLast('.', missingDelimiterValue = "").equals("apk", ignoreCase = true)
    }
}
