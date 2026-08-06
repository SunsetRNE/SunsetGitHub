package com.Sunset.REN.GitHub.domain.filemanager

import android.net.Uri
import java.io.File

sealed interface FileLocation {
    data object AppFiles : FileLocation
    data object AppCache : FileLocation
    data object Downloads : FileLocation
    data class LocalPath(val file: File) : FileLocation
    data class SafTree(val uri: Uri, val label: String) : FileLocation
}

data class FileManagerEntry(
    val id: String,
    val name: String,
    val displayPath: String,
    val type: FileEntryType,
    val source: FileEntrySource,
    val sizeBytes: Long?,
    val modifiedAtMillis: Long?,
    val capabilities: FileEntryCapabilities,
    val compileCapability: FileCompileCapability? = null
)

sealed interface FileEntrySource {
    data class ParentDirectory(val targetPath: String) : FileEntrySource
    data class LocalFile(val file: File) : FileEntrySource
    data class DocumentUri(val uri: Uri) : FileEntrySource
    data class ContentUri(val uri: Uri) : FileEntrySource
    data class ArchiveEntry(val archiveFile: File, val innerPath: String, val isDirectory: Boolean) : FileEntrySource
    data class RootPath(val absolutePath: String, val isDirectory: Boolean) : FileEntrySource
}

enum class FileEntryType {
    Parent,
    Directory,
    Text,
    Markdown,
    Code,
    Image,
    Archive,
    Apk,
    Binary,
    Unknown
}

data class FileEntryCapabilities(
    val canRead: Boolean,
    val canWrite: Boolean,
    val canRename: Boolean,
    val canDelete: Boolean,
    val canCreateChild: Boolean,
    val canUpload: Boolean,
    val canAccessContent: Boolean,
    val canEditAsText: Boolean
)
