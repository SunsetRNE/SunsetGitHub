package com.Sunset.REN.GitHub.ui.filemanager

import com.Sunset.REN.GitHub.domain.filemanager.ArchiveFormat
import com.Sunset.REN.GitHub.domain.filemanager.ArchiveFormatResolver

object ArchiveExtractionPolicy {
    private val archiveSuffixes = listOf(
        ".tar.gz", ".tar.bz2", ".tar.xz", ".tgz", ".tbz2", ".tbz", ".txz",
        ".zip", ".jar", ".aar", ".war", ".ear", ".7z", ".cpio", ".deb", ".ar",
        ".gzip", ".gz", ".bzip2", ".bz2", ".xz"
    )

    fun isExtractionSupported(name: String): Boolean {
        return ArchiveFormatResolver.resolve(name)?.supportsExtraction == true
    }

    fun resolvedExtractionFormat(name: String): ArchiveFormat? {
        return ArchiveFormatResolver.resolve(name)
    }

    fun archiveBaseName(archiveName: String, fallbackName: String): String {
        return archiveName.substringAfterLast('/').let { name ->
            archiveSuffixes.firstOrNull { suffix -> name.endsWith(suffix, ignoreCase = true) }
                ?.let { suffix -> name.dropLast(suffix.length) }
                ?: name.substringBeforeLast('.', missingDelimiterValue = name)
        }.ifBlank { fallbackName }
    }

    fun singleCompressedEntryName(displayName: String, fallbackName: String): String {
        val normalized = displayName.substringAfterLast('/').ifBlank { fallbackName }
        return when {
            normalized.endsWith(".tar.gz", ignoreCase = true) -> normalized.dropLast(3)
            normalized.endsWith(".tgz", ignoreCase = true) -> normalized.dropLast(4) + ".tar"
            normalized.endsWith(".tar.bz2", ignoreCase = true) -> normalized.dropLast(4)
            normalized.endsWith(".tbz2", ignoreCase = true) -> normalized.dropLast(5) + ".tar"
            normalized.endsWith(".tbz", ignoreCase = true) -> normalized.dropLast(4) + ".tar"
            normalized.endsWith(".tar.xz", ignoreCase = true) -> normalized.dropLast(3)
            normalized.endsWith(".txz", ignoreCase = true) -> normalized.dropLast(4) + ".tar"
            normalized.endsWith(".gzip", ignoreCase = true) -> normalized.dropLast(5)
            normalized.endsWith(".gz", ignoreCase = true) -> normalized.dropLast(3)
            normalized.endsWith(".bzip2", ignoreCase = true) -> normalized.dropLast(6)
            normalized.endsWith(".bz2", ignoreCase = true) -> normalized.dropLast(4)
            normalized.endsWith(".xz", ignoreCase = true) -> normalized.dropLast(3)
            else -> normalized
        }.ifBlank { fallbackName }
    }

    fun isTarLikeName(displayName: String): Boolean {
        val normalized = displayName.lowercase()
        return normalized.endsWith(".tar") ||
            normalized.endsWith(".tar.gz") ||
            normalized.endsWith(".tgz") ||
            normalized.endsWith(".tar.bz2") ||
            normalized.endsWith(".tbz") ||
            normalized.endsWith(".tbz2") ||
            normalized.endsWith(".tar.xz") ||
            normalized.endsWith(".txz")
    }

    fun normalizeArchiveEntryPath(path: String, fallbackName: String): String {
        return path.replace('\\', '/').trim('/').ifBlank { fallbackName }
    }

    fun safeArchivePathParts(path: String): List<String> {
        val parts = path.replace('\\', '/').split('/').filter { it.isNotBlank() }
        if (parts.any { it == "." || it == ".." }) {
            throw UnsafeArchiveEntryException(path)
        }
        return parts
    }
}

class UnsafeArchiveEntryException(path: String) : IllegalArgumentException("Unsafe archive entry path: $path")
