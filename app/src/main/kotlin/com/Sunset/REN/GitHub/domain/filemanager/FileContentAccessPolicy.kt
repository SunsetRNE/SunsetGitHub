package com.Sunset.REN.GitHub.domain.filemanager

/**
 * Central rules for deciding whether the in-app file manager may open a file's
 * content inside the app.
 *
 * This policy is intentionally conservative: directories and known binary
 * containers are never opened as text; unknown files may be opened read-only and
 * are validated again by the preview screen using byte sniffing before display.
 */
object FileContentAccessPolicy {
    val inlineTextTypes = setOf(
        FileEntryType.Text,
        FileEntryType.Markdown,
        FileEntryType.Code
    )

    val sniffableTypes = inlineTextTypes + FileEntryType.Unknown

    val specializedPreviewTypes = setOf(
        FileEntryType.Image,
        FileEntryType.Archive,
        FileEntryType.Apk
    )

    val blockedInlineTypes = setOf(
        FileEntryType.Directory,
        FileEntryType.Image,
        FileEntryType.Archive,
        FileEntryType.Apk,
        FileEntryType.Binary
    )

    fun canAccessContent(
        type: FileEntryType,
        isFile: Boolean,
        canRead: Boolean
    ): Boolean {
        if (!isFile || !canRead) return false
        return type in sniffableTypes || type in specializedPreviewTypes
    }

    fun canEditAsText(
        type: FileEntryType,
        isFile: Boolean,
        canRead: Boolean
    ): Boolean {
        return isFile && canRead && type in inlineTextTypes
    }

    fun verifiedContentType(
        declaredType: FileEntryType,
        displayName: String,
        sampleBytes: ByteArray
    ): FileEntryType {
        if (sampleBytes.isEmpty()) return declaredType
        val verifiedType = FileEntryTypeResolver.resolveVerified(
            name = displayName,
            sampleBytes = sampleBytes
        )
        return when {
            declaredType == FileEntryType.Unknown -> verifiedType
            declaredType in inlineTextTypes && verifiedType !in inlineTextTypes -> verifiedType
            else -> declaredType
        }
    }

    fun canTreatSampleAsText(
        declaredType: FileEntryType,
        displayName: String,
        sampleBytes: ByteArray
    ): Boolean {
        if (declaredType in blockedInlineTypes) return false
        if (sampleBytes.isEmpty()) return declaredType == FileEntryType.Unknown || declaredType in inlineTextTypes
        val verifiedType = verifiedContentType(
            declaredType = declaredType,
            displayName = displayName,
            sampleBytes = sampleBytes
        )
        return verifiedType in inlineTextTypes ||
            (verifiedType == FileEntryType.Unknown && FileSignatureSniffer.isLikelyText(sampleBytes))
    }
}
