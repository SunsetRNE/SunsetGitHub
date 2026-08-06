package com.Sunset.REN.GitHub.data.filemanager.provider

import com.Sunset.REN.GitHub.domain.filemanager.FileEntryCapabilities
import com.Sunset.REN.GitHub.domain.filemanager.FileContentAccessPolicy
import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryTypeResolver
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntrySorter

/** Parses Root `ls -la` / `ls -ld` output into read-only Root file-manager entries. */
object RootDirectoryListingParser {
    fun parseDirectory(parentPath: String, output: String): List<FileManagerEntry> {
        val entries = output.lineSequence()
            .filterNot { line -> line.trim().startsWith("total ") }
            .mapNotNull { line -> parseLine(parentPath, line) }
            .filterNot { entry -> entry.name == "." || entry.name == ".." }
            .toList()
        return FileManagerEntrySorter.sort(entries)
    }

    fun parseStat(path: String, output: String): FileManagerEntry? {
        val parentPath = path.substringBeforeLast('/', missingDelimiterValue = "/").ifBlank { "/" }
        return output.lineSequence().mapNotNull { line -> parseLine(parentPath, line) }.firstOrNull()
    }

    fun parseLine(parentPath: String, line: String): FileManagerEntry? {
        val parts = line.trim().split(Regex("\\s+"), limit = 9)
        val nameIndex = if (parts.getOrNull(5)?.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) == true) 7 else 8
        if (parts.size <= nameIndex) return null
        val rawName = parts.drop(nameIndex).joinToString(" ")
        val name = rawName.substringBefore(" -> ").ifBlank { return null }
        val isDirectory = parts[0].startsWith('d')
        val isRegularFile = parts[0].startsWith('-')
        val absolute = if (name.startsWith('/')) name else if (parentPath == "/") "/$name" else "${parentPath.trimEnd('/')}/$name"
        val type = FileEntryTypeResolver.resolve(name = name.substringAfterLast('/'), isDirectory = isDirectory)
        val canAccessContent = FileContentAccessPolicy.canAccessContent(type = type, isFile = isRegularFile, canRead = true)
        return FileManagerEntry(
            id = "root:$absolute",
            name = name.substringAfterLast('/').ifBlank { absolute },
            displayPath = absolute,
            type = type,
            source = FileEntrySource.RootPath(absolute, isDirectory),
            sizeBytes = parts.getOrNull(4)?.toLongOrNull()?.takeIf { !isDirectory },
            modifiedAtMillis = null,
            capabilities = FileEntryCapabilities(
                canRead = isDirectory || isRegularFile,
                canWrite = false,
                canRename = false,
                canDelete = false,
                canCreateChild = false,
                canUpload = false,
                canAccessContent = canAccessContent,
                canEditAsText = false
            )
        )
    }
}
