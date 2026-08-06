package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.filemanager.SafFileAccessProvider
import com.Sunset.REN.GitHub.domain.filemanager.ArchiveFormat
import com.Sunset.REN.GitHub.domain.filemanager.ArchiveFormatResolver
import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

class ArchiveExtractionExecutor(
    context: Context,
    private val safProvider: SafFileAccessProvider,
    private val namingPolicy: ArchiveExtractionNamingPolicy
) {
    private val appContext = context.applicationContext

    suspend fun unzipSourceFileToLocalDirectory(directory: File, sourceFile: File, archiveName: String): String {
        if (!directory.exists() || !directory.isDirectory || !directory.canWrite()) throw IOException(string(R.string.local_file_manager_unzip_no_permission))
        if (!sourceFile.exists() || !sourceFile.isFile) throw IOException(string(R.string.local_file_manager_unzip_missing))
        val targetDirectory = namingPolicy.nextAvailableLocalUnzipDirectory(directory, archiveName)
        if (!targetDirectory.mkdirs()) throw IOException(string(R.string.local_file_manager_unzip_failed))
        val canonicalRoot = targetDirectory.canonicalFile
        if (ArchiveFormatResolver.resolve(archiveName) == ArchiveFormat.SevenZip) {
            extractSevenZipToLocalDirectory(sourceFile, targetDirectory, canonicalRoot)
            return targetDirectory.name
        }
        openArchiveInputStream(sourceFile.inputStream().buffered(), archiveName).use { archive ->
            if (archive == null) {
                val target = File(targetDirectory, singleCompressedEntryName(archiveName)).canonicalFile
                writeLocalArchiveEntry(target, canonicalRoot, openDecompressedInputStream(sourceFile.inputStream().buffered(), archiveName))
            } else {
                extractArchiveStreamToLocalDirectory(archive, targetDirectory, canonicalRoot)
            }
        }
        return targetDirectory.name
    }

    suspend fun unzipLocalEntryToLocalDirectory(directory: File, entry: FileManagerEntry): String {
        if (!directory.canWrite()) throw IOException(string(R.string.local_file_manager_unzip_no_permission))
        val sourceFile = (entry.source as? FileEntrySource.LocalFile)?.file
            ?: throw IOException(string(R.string.local_file_manager_unzip_no_permission))
        return unzipSourceFileToLocalDirectory(directory, sourceFile, entry.name)
    }

    suspend fun unzipSourceFileToSafDirectory(parent: DocumentFile?, sourceFile: File, archiveName: String): String {
        val directory = parent ?: throw IOException(string(R.string.local_file_manager_authorized_directory_unavailable))
        if (!directory.canWrite()) throw IOException(string(R.string.local_file_manager_unzip_no_permission))
        if (!sourceFile.exists() || !sourceFile.isFile) throw IOException(string(R.string.local_file_manager_unzip_missing))
        val targetDirectoryName = namingPolicy.nextAvailableSafUnzipDirectoryName(directory, archiveName)
        val targetDirectory = directory.createDirectory(targetDirectoryName)
            ?: throw IOException(string(R.string.local_file_manager_unzip_failed))
        openArchiveInputStream(sourceFile.inputStream().buffered(), archiveName).use { archive ->
            if (archive == null) {
                val fileName = singleCompressedEntryName(archiveName)
                val targetFile = targetDirectory.createFile(FileMimeTypePolicy.mimeTypeForName(fileName), fileName)
                    ?: throw IOException(string(R.string.local_file_manager_unzip_failed))
                appContext.contentResolver.openOutputStream(targetFile.uri, "w")?.use { output ->
                    openDecompressedInputStream(sourceFile.inputStream().buffered(), archiveName).use { decompressed -> decompressed.copyToCancellable(output) }
                } ?: throw IOException(string(R.string.local_file_manager_unzip_failed))
            } else {
                extractArchiveStreamToSafDirectory(archive, targetDirectory)
            }
        }
        return targetDirectoryName
    }

    suspend fun unzipDocumentEntryToSafDirectory(parent: DocumentFile?, entry: FileManagerEntry): String {
        val directory = parent ?: throw IOException(string(R.string.local_file_manager_authorized_directory_unavailable))
        if (!directory.canWrite()) throw IOException(string(R.string.local_file_manager_unzip_no_permission))
        val source = when (val source = entry.source) {
            is FileEntrySource.DocumentUri -> safProvider.documentFromUri(source.uri)
            else -> null
        } ?: throw IOException(string(R.string.local_file_manager_unzip_no_permission))
        if (!source.exists() || !source.isFile) throw IOException(string(R.string.local_file_manager_unzip_missing))
        val targetDirectoryName = namingPolicy.nextAvailableSafUnzipDirectoryName(directory, entry.name)
        val targetDirectory = directory.createDirectory(targetDirectoryName)
            ?: throw IOException(string(R.string.local_file_manager_unzip_failed))
        appContext.contentResolver.openInputStream(source.uri)?.use { input ->
            openArchiveInputStream(input.buffered(), entry.name).use { archive ->
                if (archive == null) {
                    val fileName = singleCompressedEntryName(entry.name)
                    val targetFile = targetDirectory.createFile(FileMimeTypePolicy.mimeTypeForName(fileName), fileName)
                        ?: throw IOException(string(R.string.local_file_manager_unzip_failed))
                    appContext.contentResolver.openOutputStream(targetFile.uri, "w")?.use { output ->
                        openDecompressedInputStream(
                            appContext.contentResolver.openInputStream(source.uri)?.buffered()
                                ?: throw IOException(string(R.string.local_file_manager_unzip_missing)),
                            entry.name
                        ).use { decompressed -> decompressed.copyToCancellable(output) }
                    } ?: throw IOException(string(R.string.local_file_manager_unzip_failed))
                } else {
                    extractArchiveStreamToSafDirectory(archive, targetDirectory)
                }
            }
        } ?: throw IOException(string(R.string.local_file_manager_unzip_missing))
        return targetDirectoryName
    }

    private suspend fun extractArchiveStreamToLocalDirectory(
        archive: ArchiveInputStream<out ArchiveEntry>,
        targetDirectory: File,
        canonicalRoot: File
    ) {
        while (true) {
            currentCoroutineContext().ensureActive()
            val archiveEntry = archive.nextEntry ?: break
            if (!archive.canReadEntryData(archiveEntry)) continue
            val target = File(targetDirectory, normalizeArchiveEntryPath(archiveEntry.name)).canonicalFile
            if (!target.path.startsWith(canonicalRoot.path + File.separator) && target != canonicalRoot) {
                throw IOException(string(R.string.local_file_manager_unzip_unsafe_entry))
            }
            if (archiveEntry.isDirectory) {
                if (!target.exists() && !target.mkdirs()) throw IOException(string(R.string.local_file_manager_unzip_failed))
            } else {
                target.parentFile?.let { parent ->
                    if (!parent.exists() && !parent.mkdirs()) throw IOException(string(R.string.local_file_manager_unzip_failed))
                }
                target.outputStream().buffered().use { output -> archive.copyToCancellable(output) }
            }
        }
    }

    private suspend fun extractArchiveStreamToSafDirectory(
        archive: ArchiveInputStream<out ArchiveEntry>,
        targetDirectory: DocumentFile
    ) {
        while (true) {
            currentCoroutineContext().ensureActive()
            val archiveEntry = archive.nextEntry ?: break
            if (!archive.canReadEntryData(archiveEntry)) continue
            val pathParts = safeArchivePathParts(archiveEntry.name)
            if (pathParts.isEmpty()) continue
            if (archiveEntry.isDirectory) {
                ensureSafDirectoryPath(targetDirectory, pathParts)
            } else {
                val fileName = pathParts.last()
                val fileParent = ensureSafDirectoryPath(targetDirectory, pathParts.dropLast(1))
                val targetFile = fileParent.createFile(FileMimeTypePolicy.mimeTypeForName(fileName), fileName)
                    ?: throw IOException(string(R.string.local_file_manager_unzip_failed))
                appContext.contentResolver.openOutputStream(targetFile.uri, "w")?.use { output ->
                    archive.copyToCancellable(output)
                } ?: throw IOException(string(R.string.local_file_manager_unzip_failed))
            }
        }
    }

    private suspend fun ensureSafDirectoryPath(root: DocumentFile, parts: List<String>): DocumentFile {
        var current = root
        parts.forEach { part ->
            currentCoroutineContext().ensureActive()
            val existing = current.findFile(part)
            current = when {
                existing != null && existing.isDirectory -> existing
                existing != null -> throw IOException(string(R.string.local_file_manager_unzip_failed))
                else -> current.createDirectory(part)
                    ?: throw IOException(string(R.string.local_file_manager_unzip_failed))
            }
        }
        return current
    }

    private suspend fun extractSevenZipToLocalDirectory(sourceFile: File, targetDirectory: File, canonicalRoot: File) {
        SevenZFile.builder().setFile(sourceFile).get().use { sevenZ ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val entry = sevenZ.nextEntry ?: break
                val target = File(targetDirectory, normalizeArchiveEntryPath(entry.name)).canonicalFile
                if (!target.path.startsWith(canonicalRoot.path + File.separator) && target != canonicalRoot) {
                    throw IOException(string(R.string.local_file_manager_unzip_unsafe_entry))
                }
                if (entry.isDirectory) {
                    if (!target.exists() && !target.mkdirs()) throw IOException(string(R.string.local_file_manager_unzip_failed))
                } else {
                    target.parentFile?.let { parent ->
                        if (!parent.exists() && !parent.mkdirs()) throw IOException(string(R.string.local_file_manager_unzip_failed))
                    }
                    target.outputStream().buffered().use { output ->
                        var remaining = entry.size
                        while (remaining > 0L) {
                            currentCoroutineContext().ensureActive()
                            val read = sevenZ.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            remaining -= read.toLong()
                        }
                    }
                }
            }
        }
    }

    private suspend fun writeLocalArchiveEntry(target: File, canonicalRoot: File, input: InputStream) {
        input.use { source ->
            if (!target.path.startsWith(canonicalRoot.path + File.separator) && target != canonicalRoot) {
                throw IOException(string(R.string.local_file_manager_unzip_unsafe_entry))
            }
            target.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) throw IOException(string(R.string.local_file_manager_unzip_failed))
            }
            target.outputStream().buffered().use { output -> source.copyToCancellable(output) }
        }
    }

    private fun openArchiveInputStream(input: BufferedInputStream, displayName: String): ArchiveInputStream<out ArchiveEntry>? {
        val format = ArchiveFormatResolver.resolve(displayName)
            ?: throw IOException(string(R.string.local_file_manager_unzip_not_zip))
        if (!format.supportsExtraction) {
            throw IOException(string(R.string.local_file_manager_archive_format_unsupported, format.displayName))
        }
        val decompressed = openDecompressedInputStream(input, displayName)
        return when {
            ArchiveExtractionPolicy.isTarLikeName(displayName) -> ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.TAR, decompressed)
            format == ArchiveFormat.Gzip || format == ArchiveFormat.Bzip2 || format == ArchiveFormat.Xz -> null
            format == ArchiveFormat.SevenZip -> throw IOException(string(R.string.local_file_manager_archive_streaming_unsupported, format.displayName))
            else -> ArchiveStreamFactory().createArchiveInputStream(decompressed)
        }
    }

    private fun openDecompressedInputStream(input: BufferedInputStream, displayName: String): BufferedInputStream {
        val format = ArchiveFormatResolver.resolve(displayName) ?: return input
        val stream = when (format) {
            ArchiveFormat.Gzip -> CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.GZIP, input)
            ArchiveFormat.Bzip2 -> CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.BZIP2, input)
            ArchiveFormat.Xz -> CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.XZ, input)
            else -> input
        }
        return stream.buffered()
    }

    private fun normalizeArchiveEntryPath(path: String): String {
        return ArchiveExtractionPolicy.normalizeArchiveEntryPath(path, string(R.string.local_file_preview_unknown_name))
    }

    private fun safeArchivePathParts(path: String): List<String> {
        return try {
            ArchiveExtractionPolicy.safeArchivePathParts(path)
        } catch (_: UnsafeArchiveEntryException) {
            throw IOException(string(R.string.local_file_manager_unzip_unsafe_entry))
        }
    }

    private fun singleCompressedEntryName(displayName: String): String {
        return ArchiveExtractionPolicy.singleCompressedEntryName(displayName, string(R.string.local_file_preview_unknown_name))
    }

    private fun string(resId: Int): String = appContext.getString(resId)
    private fun string(resId: Int, vararg args: Any): String = appContext.getString(resId, *args)
}
