package com.Sunset.REN.GitHub.domain.filemanager

import java.nio.charset.Charset

sealed interface FileContentReadResult {
    data class Text(
        val content: String,
        val charset: Charset,
        val truncated: Boolean = false,
        val sizeBytes: Long,
        val hadBom: Boolean = false,
        val lineEnding: FileTextEncodingPolicy.LineEnding = FileTextEncodingPolicy.LineEnding.Lf
    ) : FileContentReadResult

    data class BinaryBlocked(
        val reason: String
    ) : FileContentReadResult

    data class TooLarge(
        val sizeBytes: Long,
        val limitBytes: Long
    ) : FileContentReadResult

    data class Failed(
        val message: String
    ) : FileContentReadResult
}
data class FileContentWriteResult(
    val sizeBytes: Long,
    val lastModifiedMillis: Long? = null
)

data class ArchiveFileEntryPreview(
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long?,
    val type: FileEntryType = if (isDirectory) FileEntryType.Directory else FileEntryType.Unknown,
    val canPreviewText: Boolean = false,
    val compileCapability: FileCompileCapability? = null
)

data class FileCompileCapability(
    val language: String,
    val mode: FileCompileMode,
    val toolHint: String
)

enum class FileCompileMode {
    Compile,
    Interpret,
    Package,
    BuildScript,
    Markup
}

data class ArchiveEntryTextPreview(
    val entryName: String,
    val content: String,
    val charset: Charset,
    val sizeBytes: Long,
    val truncated: Boolean = false
)

data class ArchivePreview(
    val displayName: String,
    val sizeBytes: Long?,
    val entryCount: Int,
    val directoryCount: Int,
    val fileCount: Int,
    val entries: List<ArchiveFileEntryPreview>,
    val truncated: Boolean
)

data class ArchiveExtractSummary(
    val targetName: String,
    val directoryCount: Int,
    val fileCount: Int,
    val skippedEntryCount: Int = 0
)

data class ApkPreview(
    val displayName: String,
    val sizeBytes: Long?,
    val entryCount: Int,
    val hasManifest: Boolean,
    val hasClassesDex: Boolean,
    val dexCount: Int,
    val hasResourcesArsc: Boolean,
    val nativeArchitectures: List<String>,
    val certificateEntries: List<String>,
    val dexFiles: List<DexFilePreview> = emptyList(),
    val dexParseFailures: List<String> = emptyList(),
    val entries: List<ArchiveFileEntryPreview>,
    val truncated: Boolean
)

data class DexFilePreview(
    val name: String,
    val version: String,
    val fileSizeBytes: Long,
    val headerSizeBytes: Long,
    val endianTag: String,
    val stringCount: Int,
    val typeCount: Int,
    val protoCount: Int,
    val fieldCount: Int,
    val methodCount: Int,
    val classCount: Int
)


