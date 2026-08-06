package com.Sunset.REN.GitHub.ui.filemanager

import android.net.Uri
import android.os.Environment
import com.Sunset.REN.GitHub.domain.filemanager.path.FileManagerPath
import java.io.File

/**
 * Centralizes display-path parsing for the local file manager while the screen is
 * being migrated from string routing to FileManagerPath/provider routing.
 */
class FileManagerNavigator {
    fun parseDirectoryPath(path: String): DirectoryPathTarget {
        parseRootDisplayPath(path)?.let { return DirectoryPathTarget.Root(it) }
        parseArchiveDisplayPath(path)?.let { return DirectoryPathTarget.Archive(it.archiveFile, it.innerPath) }
        toContentUriOrNull(path)?.let { return DirectoryPathTarget.Saf(it) }
        return DirectoryPathTarget.Local(File(path))
    }

    fun parseFileManagerPath(path: String): FileManagerPath {
        return when (val target = parseDirectoryPath(path)) {
            is DirectoryPathTarget.Root -> FileManagerPath.Root(target.path)
            is DirectoryPathTarget.Archive -> FileManagerPath.Archive(
                archivePath = target.archiveFile.absolutePath,
                innerPath = target.innerPath
            )
            is DirectoryPathTarget.Saf -> FileManagerPath.Saf(target.uri)
            is DirectoryPathTarget.Local -> FileManagerPath.Local(target.directory.absolutePath)
        }
    }

    fun parentPathForDirectoryPath(path: String, safParentUriFor: (Uri) -> Uri?): String? {
        return when (val target = parseDirectoryPath(path)) {
            is DirectoryPathTarget.Root -> File(target.path).parentFile?.absolutePath?.let { "$RootPrefix$it" }
            is DirectoryPathTarget.Archive -> {
                val parentInnerPath = archiveParentInnerPath(target.innerPath)
                if (parentInnerPath != null) {
                    "${target.archiveFile.absolutePath}$ArchivePathMarker$parentInnerPath"
                } else {
                    target.archiveFile.parentFile?.absolutePath
                }
            }
            is DirectoryPathTarget.Saf -> safParentUriFor(target.uri)?.toString()
            is DirectoryPathTarget.Local -> target.directory.parentFile?.takeIf { it.isDirectory }?.absolutePath
        }
    }

    fun parseRootDisplayPath(path: String): String? {
        return path.removePrefix(RootPrefix)
            .takeIf { it != path && it.startsWith("/") }
            ?.let(::normalizeRootPath)
    }

    fun normalizeRootPath(path: String): String {
        val normalized = path.trim()
            .removePrefix(RootPrefix)
            .replace('\\', '/')
            .trimEnd('/')
            .ifBlank { "/" }
        return if (normalized.startsWith("/")) normalized else "/$normalized"
    }

    fun parseArchiveDisplayPath(path: String): ArchiveDisplayPath? {
        val marker = path.indexOf(ArchivePathMarker)
        if (marker <= 0) return null
        return ArchiveDisplayPath(
            archiveFile = File(path.substring(0, marker)),
            innerPath = normalizeArchiveInnerPath(path.substring(marker + ArchivePathMarker.length))
        )
    }

    fun normalizeArchiveInnerPath(path: String): String {
        return path.replace('\\', '/').trim('/').split('/').filter { it.isNotBlank() }.joinToString("/")
    }

    fun normalizeArchiveEntryName(name: String): String = normalizeArchiveInnerPath(name)

    fun archiveParentInnerPath(innerPath: String): String? {
        val normalized = normalizeArchiveInnerPath(innerPath)
        if (normalized.isBlank()) return null
        return normalized.substringBeforeLast('/', missingDelimiterValue = "")
    }

    fun toContentUriOrNull(path: String): Uri? {
        val uri = runCatching { Uri.parse(path) }.getOrNull()
        return uri?.takeIf { it.scheme.equals("content", ignoreCase = true) }
    }
    fun shouldShowParentDirectoryEntry(path: String, isRootGranted: Boolean): Boolean {
        parseRootDisplayPath(path)?.let { rootPath ->
            return isRootGranted && rootPath != "/"
        }
        return parseArchiveDisplayPath(path) != null ||
            isStorageRootDescendant(path) ||
            (isRootGranted && isStorageRootPath(path))
    }

    fun parentEntryTargetPath(
        path: String,
        isRootGranted: Boolean,
        parentPathForDirectoryPath: (String) -> String?
    ): String? {
        if (isRootGranted && isStorageRootPath(path)) {
            return rootParentPathForStorageRoot(path, true)
        }
        return parentPathForDirectoryPath(path)
    }

    fun rightPaneLookupPath(path: String, isRootGranted: Boolean): String {
        parseRootDisplayPath(path)?.let { rootPath -> return "$RootPrefix$rootPath" }
        return if (isRootGranted && path.startsWith("/") && !isStorageRootPath(path) && !isStorageRootDescendant(path)) {
            "$RootPrefix$path"
        } else {
            path
        }
    }

    fun rootParentPathForStorageRoot(path: String, isRootGranted: Boolean): String? {
        if (!isRootGranted || !isStorageRootPath(path)) return null
        val parent = File(normalizeFileManagerPath(path)).parentFile?.absolutePath ?: "/"
        return "$RootPrefix$parent"
    }


    fun isStorageRootPath(path: String): Boolean {
        return normalizeFileManagerPath(path) == normalizedStorageRootPath()
    }

    fun isStorageRootDescendant(path: String): Boolean {
        return normalizeFileManagerPath(path).startsWith("${normalizedStorageRootPath()}/")
    }

    fun normalizeFileManagerPath(path: String): String {
        return path.trim()
            .removePrefix(RootPrefix)
            .removeSuffix("/")
            .replace('\\', '/')
            .trimEnd('/')
            .ifBlank { "/" }
    }

    private fun normalizedStorageRootPath(): String {
        return Environment.getExternalStorageDirectory().absolutePath
            .replace('\\', '/')
            .trimEnd('/')
    }

    data class ArchiveDisplayPath(
        val archiveFile: File,
        val innerPath: String
    )

    sealed interface DirectoryPathTarget {
        data class Root(val path: String) : DirectoryPathTarget
        data class Archive(val archiveFile: File, val innerPath: String) : DirectoryPathTarget
        data class Saf(val uri: Uri) : DirectoryPathTarget
        data class Local(val directory: File) : DirectoryPathTarget
    }

    private companion object {
        const val RootPrefix = "root://"
        const val ArchivePathMarker = "!/"
    }
}
