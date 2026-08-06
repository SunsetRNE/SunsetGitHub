package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.filemanager.FileContentAccessRepository
import com.Sunset.REN.GitHub.data.filemanager.SafFileAccessProvider
import com.Sunset.REN.GitHub.domain.filemanager.FileContentReadResult
import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.FileSizeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class TextExportCoordinator(
    context: Context,
    private val contentAccessRepository: FileContentAccessRepository,
    private val safProvider: SafFileAccessProvider,
    private val navigator: FileManagerNavigator,
    private val cacheArchiveEntryForContentAccess: suspend (FileEntrySource.ArchiveEntry, String) -> File
) {
    private val appContext = context.applicationContext

    suspend fun exportEntriesAsTextInDirectoryPath(
        entries: List<FileManagerEntry>,
        targetDirectoryPath: String,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchTextExportResult = withContext(Dispatchers.IO) {
        val failures = mutableListOf<BatchTextExportFailure>()
        var completedCount = 0
        entries.forEach { entry ->
            currentCoroutineContext().ensureActive()
            runCatching { exportEntryAsTextInDirectoryPathInternal(entry, targetDirectoryPath) }
                .onFailure { error ->
                    failures += BatchTextExportFailure(
                        entry = entry,
                        message = error.message ?: string(R.string.local_file_manager_batch_text_export_failed)
                    )
                }
            completedCount++
            onProgress(completedCount, entries.size)
        }
        BatchTextExportResult(
            requestedCount = entries.size,
            successCount = entries.size - failures.size,
            failures = failures
        )
    }

    private suspend fun exportEntryAsTextInDirectoryPathInternal(entry: FileManagerEntry, targetDirectoryPath: String) {
        if (entry.type == FileEntryType.Directory) {
            throw IOException(string(R.string.local_file_manager_batch_text_export_directory_unsupported))
        }
        val sourceUri = sourceUriFor(entry)
        val readResult = when {
            isDocxEntry(entry) -> contentAccessRepository.readDocxText(sourceUri, entry.name, entry.sizeBytes)
            isPdfEntry(entry) -> contentAccessRepository.readPdfText(sourceUri, entry.name, entry.sizeBytes)
            else -> contentAccessRepository.readText(
                sourceUri = sourceUri,
                displayName = entry.name,
                entryType = entry.type,
                declaredSizeBytes = entry.sizeBytes
            )
        }
        val text = when (readResult) {
            is FileContentReadResult.Text -> readResult.content
            is FileContentReadResult.BinaryBlocked -> throw IOException(readResult.reason)
            is FileContentReadResult.TooLarge -> throw IOException(
                string(R.string.local_file_preview_too_large, FileSizeFormatter.format(readResult.limitBytes))
            )
            is FileContentReadResult.Failed -> throw IOException(readResult.message)
        }
        writeExportedText(targetDirectoryPath, defaultTextExportName(entry.name), text)
    }

    private suspend fun sourceUriFor(entry: FileManagerEntry): Uri {
        return when (val source = entry.source) {
            is FileEntrySource.ParentDirectory -> throw IOException(string(R.string.local_file_manager_file_not_readable))
            is FileEntrySource.LocalFile -> Uri.fromFile(source.file)
            is FileEntrySource.DocumentUri -> source.uri
            is FileEntrySource.ContentUri -> source.uri
            is FileEntrySource.ArchiveEntry -> Uri.fromFile(cacheArchiveEntryForContentAccess(source, entry.name))
            is FileEntrySource.RootPath -> throw IOException(string(R.string.local_file_manager_file_not_readable))
        }
    }

    private fun writeExportedText(targetDirectoryPath: String, desiredName: String, content: String) {
        val targetUri = navigator.toContentUriOrNull(targetDirectoryPath)
        if (targetUri != null) {
            val directory = documentForPathUri(targetUri)
                ?: throw IOException(string(R.string.local_file_manager_batch_text_export_target_unavailable))
            if (!directory.canWrite()) throw IOException(string(R.string.local_file_manager_batch_text_export_no_permission))
            val targetName = nextAvailableSafExportName(directory, desiredName)
            val target = directory.createFile("text/plain", targetName)
                ?: throw IOException(string(R.string.local_file_manager_batch_text_export_failed))
            appContext.contentResolver.openOutputStream(target.uri, "w")?.use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
            } ?: throw IOException(string(R.string.local_file_manager_batch_text_export_failed))
            return
        }

        val directory = File(targetDirectoryPath)
        if (!directory.exists() || !directory.isDirectory) {
            throw IOException(string(R.string.local_file_manager_batch_text_export_target_unavailable))
        }
        if (!directory.canWrite()) throw IOException(string(R.string.local_file_manager_batch_text_export_no_permission))
        val target = nextAvailableLocalExportTarget(directory, desiredName)
        target.writeText(content, Charsets.UTF_8)
    }

    private fun defaultTextExportName(originalName: String): String {
        if (originalName.endsWith(".txt", ignoreCase = true)) return originalName
        val dotIndex = originalName.lastIndexOf('.').takeIf { it > 0 }
        val baseName = dotIndex?.let { originalName.substring(0, it) } ?: originalName
        return "${baseName.ifBlank { string(R.string.local_file_preview_unknown_name) }}.txt"
    }

    private fun nextAvailableLocalExportTarget(directory: File, desiredName: String): File {
        var index = 0
        while (true) {
            val candidate = File(directory, exportNameFor(desiredName, index))
            if (!candidate.exists()) return candidate
            index++
        }
    }

    private fun nextAvailableSafExportName(directory: DocumentFile, desiredName: String): String {
        var index = 0
        while (true) {
            val candidate = exportNameFor(desiredName, index)
            if (directory.findFile(candidate) == null) return candidate
            index++
        }
    }

    private fun exportNameFor(desiredName: String, index: Int): String {
        if (index == 0) return desiredName
        val dotIndex = desiredName.lastIndexOf('.').takeIf { it > 0 && it < desiredName.lastIndex }
        return if (dotIndex == null) {
            "$desiredName-$index"
        } else {
            desiredName.substring(0, dotIndex) + "-$index" + desiredName.substring(dotIndex)
        }
    }

    private fun isDocxEntry(entry: FileManagerEntry): Boolean {
        return entry.name.endsWith(".docx", ignoreCase = true) || entry.displayPath.endsWith(".docx", ignoreCase = true)
    }

    private fun isPdfEntry(entry: FileManagerEntry): Boolean {
        return entry.name.endsWith(".pdf", ignoreCase = true) || entry.displayPath.endsWith(".pdf", ignoreCase = true)
    }

    private fun documentForPathUri(uri: Uri): DocumentFile? {
        return safProvider.documentFromUri(uri) ?: safProvider.treeFromUri(uri)
    }

    private fun string(resId: Int): String = appContext.getString(resId)

    private fun string(resId: Int, vararg args: Any): String = appContext.getString(resId, *args)
}
