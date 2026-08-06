package com.Sunset.REN.GitHub.data.filemanager

import android.content.Context
import android.os.Environment
import com.Sunset.REN.GitHub.domain.filemanager.FileCompileCapabilityResolver
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryCapabilityResolver
import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryTypeResolver
import com.Sunset.REN.GitHub.domain.filemanager.FileLocation
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntrySorter
import java.io.File
import java.io.IOException

class LocalFileAccessProvider(
    private val context: Context
) {
    fun resolveLocation(location: FileLocation): File {
        return when (location) {
            FileLocation.AppFiles -> context.filesDir
            FileLocation.AppCache -> context.cacheDir
            FileLocation.Downloads -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            is FileLocation.LocalPath -> location.file
            is FileLocation.SafTree -> throw IllegalArgumentException("SAF tree is not a local file location")
        }
    }

    fun list(location: FileLocation): Result<List<FileManagerEntry>> {
        return listDirectory(resolveLocation(location))
    }

    fun listDirectory(directory: File, verifyFileType: Boolean = true): Result<List<FileManagerEntry>> {
        return runCatching {
            if (!directory.exists()) throw IOException("Directory does not exist: ${directory.absolutePath}")
            if (!directory.isDirectory) throw IOException("Path is not a directory: ${directory.absolutePath}")
            val children = directory.listFiles() ?: throw IOException("Cannot read directory: ${directory.absolutePath}")
            FileManagerEntrySorter.sort(
                children
                    .filter { it.exists() }
                    .map { buildEntry(it, verifyFileType) }
            )
        }
    }

    fun buildEntry(file: File, verifyFileType: Boolean = true): FileManagerEntry {
        val isDirectory = file.isDirectory
        val isFile = file.isFile
        val canRead = runCatching { file.canRead() }.getOrDefault(false)
        val canWrite = runCatching { file.canWrite() }.getOrDefault(false)
        val type = if (verifyFileType) {
            FileEntryTypeResolver.resolveVerified(
                name = file.name,
                isDirectory = isDirectory,
                sampleBytes = sampleFileHeader(file)
            )
        } else {
            FileEntryTypeResolver.resolve(
                name = file.name,
                isDirectory = isDirectory
            )
        }
        return FileManagerEntry(
            id = file.absolutePath,
            name = file.name.ifBlank { file.absolutePath },
            displayPath = file.absolutePath,
            type = type,
            source = FileEntrySource.LocalFile(file),
            sizeBytes = if (isFile) file.length() else null,
            modifiedAtMillis = file.lastModified().takeIf { it > 0L },
            capabilities = FileEntryCapabilityResolver.resolve(
                type = type,
                isFile = isFile,
                isDirectory = isDirectory,
                canRead = canRead,
                canWrite = canWrite
            ),
            compileCapability = FileCompileCapabilityResolver.resolve(file.name, type, file.isDirectory)
        )
    }

    fun parentOf(directory: File): File? {
        return directory.parentFile?.takeIf { it.exists() && it.isDirectory }
    }

    private fun sampleFileHeader(file: File): ByteArray? {
        if (!file.isFile) return null
        if (!runCatching { file.canRead() }.getOrDefault(false)) return null
        if (file.length() <= 0L) return null
        return runCatching {
            file.inputStream().use { input ->
                val buffer = ByteArray(FileTypeSampleBytes)
                val read = input.read(buffer)
                if (read > 0) buffer.copyOf(read) else null
            }
        }.getOrNull()
    }

    private companion object {
        const val FileTypeSampleBytes = 4096
    }
}