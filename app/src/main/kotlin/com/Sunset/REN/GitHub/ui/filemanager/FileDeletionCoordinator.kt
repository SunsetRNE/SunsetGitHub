package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.filemanager.RecycleBinRecord
import com.Sunset.REN.GitHub.data.filemanager.RecycleBinRecordStore
import com.Sunset.REN.GitHub.data.filemanager.SafFileAccessProvider
import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.RecycleBinSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.io.FileAlreadyExistsException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class FileDeletionCoordinator(
    private val context: Context,
    private val safProvider: SafFileAccessProvider,
    private val recycleBinRecordStore: RecycleBinRecordStore,
    private val recycleBinSettingsProvider: () -> RecycleBinSettings,
    private val transferNamingPolicy: FileTransferNamingPolicy = FileTransferNamingPolicy(context)
) {
    private val appContext = context.applicationContext

    fun deleteEntryBlocking(entry: FileManagerEntry, moveToRecycleBin: Boolean): Result<Unit> {
        return runCatching {
            if (shouldMoveToRecycleBin(moveToRecycleBin)) {
                moveEntryToRecycleBinBlocking(entry)
            } else {
                deleteEntryInternalBlocking(entry)
            }
        }.map { Unit }
    }

    suspend fun deleteEntries(
        entries: List<FileManagerEntry>,
        moveToRecycleBin: Boolean,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchDeleteResult = withContext(Dispatchers.IO) {
        val failures = mutableListOf<BatchDeleteFailure>()
        var completedCount = 0
        val shouldMoveToRecycleBin = shouldMoveToRecycleBin(moveToRecycleBin)
        entries.forEach { entry ->
            currentCoroutineContext().ensureActive()
            runCatching {
                if (shouldMoveToRecycleBin) moveEntryToRecycleBin(entry) else deleteEntryInternal(entry)
            }.onFailure { error ->
                failures += BatchDeleteFailure(
                    entry = entry,
                    message = error.message ?: string(R.string.local_file_manager_delete_failed)
                )
            }
            completedCount++
            onProgress(completedCount, entries.size)
        }
        BatchDeleteResult(
            requestedCount = entries.size,
            successCount = entries.size - failures.size,
            failures = failures
        )
    }

    suspend fun deleteEntryPermanently(entry: FileManagerEntry) {
        deleteEntryInternal(entry)
    }

    fun recycleBinDirectory(): File {
        return File(appContext.filesDir, RecycleBinDirectoryName).also { it.mkdirs() }
    }

    fun cleanRecycleBinIfNeeded(): Int {
        val days = recycleBinSettingsProvider().autoCleanDays
        if (days <= 0) return 0
        val thresholdMillis = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
        var cleanedCount = 0
        recycleBinRecordStore.getRecords()
            .filter { it.deletedAtMillis < thresholdMillis }
            .forEach { record ->
                File(record.recyclePath).deleteRecursively()
                recycleBinRecordStore.removeForRecyclePath(record.recyclePath)
                cleanedCount++
            }
        return cleanedCount
    }

    fun clearRecycleBin(): Result<Unit> {
        return runCatching {
            recycleBinDirectory().listFiles().orEmpty().forEach { it.deleteRecursively() }
            recycleBinRecordStore.clear()
        }.map { Unit }
    }

    fun isRecycleBinPath(path: String): Boolean {
        val recycleRoot = recycleBinDirectory().absoluteFile
        val file = runCatching { File(path).absoluteFile }.getOrNull() ?: return false
        return file == recycleRoot || file.path.startsWith(recycleRoot.path + File.separator)
    }

    fun restoreRecycleBinEntries(entries: List<FileManagerEntry>): BatchRestoreResult {
        val failures = mutableListOf<BatchRestoreFailure>()
        var restoredCount = 0
        entries.forEach { entry ->
            val result = runCatching {
                val source = entry.source as? FileEntrySource.LocalFile
                    ?: throw IOException(string(R.string.local_file_manager_restore_recycle_bin_failed))
                restoreRecycleBinFile(source.file)
            }
            result.fold(
                onSuccess = { restoredCount++ },
                onFailure = { error ->
                    failures += BatchRestoreFailure(
                        entry = entry,
                        message = error.message ?: string(R.string.local_file_manager_restore_recycle_bin_failed)
                    )
                }
            )
        }
        return BatchRestoreResult(
            requestedCount = entries.size,
            successCount = restoredCount,
            failures = failures
        )
    }

    private fun shouldMoveToRecycleBin(moveToRecycleBin: Boolean): Boolean {
        return recycleBinSettingsProvider().enabled && moveToRecycleBin
    }

    private fun moveEntryToRecycleBinBlocking(entry: FileManagerEntry) {
        ensureCanDelete(entry)
        val targetDirectory = recycleBinDirectory()
        when (val source = entry.source) {
            is FileEntrySource.LocalFile -> {
                val original = source.file
                if (!original.exists()) throw IOException(string(R.string.local_file_manager_delete_missing))
                val target = nextAvailableLocalRecycleTarget(targetDirectory, original.name)
                if (!original.renameTo(target)) {
                    if (original.isDirectory) {
                        original.copyRecursively(target, overwrite = false)
                        if (!original.deleteRecursively()) throw IOException(string(R.string.local_file_manager_delete_failed))
                    } else {
                        original.copyTo(target, overwrite = false)
                        if (!original.delete()) throw IOException(string(R.string.local_file_manager_delete_failed))
                    }
                }
                recordRecycleBinEntry(target, original.absolutePath, original.name)
            }
            is FileEntrySource.DocumentUri -> {
                val document = safProvider.documentFromUri(source.uri)
                    ?: throw IOException(string(R.string.local_file_manager_delete_missing))
                val targetName = document.name.orEmpty().ifBlank { entry.name.ifBlank { string(R.string.local_file_preview_unknown_name) } }
                val target = copySafDocumentToRecycleBinBlocking(document, targetDirectory, targetName)
                if (!document.delete()) throw IOException(string(R.string.local_file_manager_delete_failed))
                recordRecycleBinEntry(target, source.uri.toString(), targetName)
            }
            is FileEntrySource.ParentDirectory,
            is FileEntrySource.ArchiveEntry,
            is FileEntrySource.RootPath,
            is FileEntrySource.ContentUri -> throw IOException(string(R.string.local_file_manager_delete_no_permission))
        }
    }

    private fun copySafDocumentToRecycleBinBlocking(source: DocumentFile, targetDirectory: File, targetName: String): File {
        if (!source.exists()) throw IOException(string(R.string.local_file_manager_delete_missing))
        val target = nextAvailableLocalRecycleTarget(targetDirectory, targetName)
        if (source.isDirectory) {
            if (!target.mkdirs()) throw IOException(string(R.string.local_file_manager_delete_failed))
            source.listFiles().forEach { child ->
                val childName = child.name.orEmpty().ifBlank { string(R.string.local_file_preview_unknown_name) }
                copySafDocumentToRecycleBinBlocking(child, target, childName)
            }
            return target
        }
        appContext.contentResolver.openInputStream(source.uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException(string(R.string.local_file_manager_delete_missing))
        return target
    }

    private suspend fun moveEntryToRecycleBin(entry: FileManagerEntry) {
        ensureCanDelete(entry)
        val targetDirectory = recycleBinDirectory()
        when (val source = entry.source) {
            is FileEntrySource.LocalFile -> {
                if (!source.file.exists()) throw IOException(string(R.string.local_file_manager_delete_missing))
                val target = nextAvailableLocalRecycleTarget(targetDirectory, source.file.name)
                if (!source.file.renameTo(target)) {
                    if (source.file.isDirectory) {
                        source.file.copyRecursivelyCancellable(target)
                        source.file.deleteRecursivelyCancellable()
                    } else {
                        source.file.copyToCancellable(target, overwrite = false)
                        currentCoroutineContext().ensureActive()
                        if (!source.file.delete()) throw IOException(string(R.string.local_file_manager_delete_failed))
                    }
                }
                recordRecycleBinEntry(target, source.file.absolutePath, source.file.name)
            }
            is FileEntrySource.DocumentUri -> {
                val document = safProvider.documentFromUri(source.uri)
                    ?: throw IOException(string(R.string.local_file_manager_delete_missing))
                val targetName = document.name.orEmpty().ifBlank { entry.name.ifBlank { string(R.string.local_file_preview_unknown_name) } }
                val target = nextAvailableLocalRecycleTarget(targetDirectory, targetName)
                copySafDocumentToLocalDirectory(document, targetDirectory, target.name)
                if (!document.delete()) throw IOException(string(R.string.local_file_manager_delete_failed))
                recordRecycleBinEntry(target, source.uri.toString(), targetName)
            }
            is FileEntrySource.ParentDirectory,
            is FileEntrySource.ArchiveEntry,
            is FileEntrySource.RootPath,
            is FileEntrySource.ContentUri -> throw IOException(string(R.string.local_file_manager_delete_no_permission))
        }
    }

    private fun nextAvailableLocalRecycleTarget(parent: File, originalName: String): File {
        val safeName = originalName.ifBlank { string(R.string.local_file_preview_unknown_name) }
        val timestampedName = "${System.currentTimeMillis()}-$safeName"
        var candidate = File(parent, timestampedName)
        var index = 2
        while (candidate.exists()) {
            candidate = File(parent, "${System.currentTimeMillis()}-$index-$safeName")
            index++
        }
        return candidate
    }

    private fun recordRecycleBinEntry(recycleFile: File, originalPath: String, originalName: String) {
        recycleBinRecordStore.record(
            RecycleBinRecord(
                recyclePath = recycleFile.absolutePath,
                originalPath = originalPath,
                originalName = originalName,
                deletedAtMillis = System.currentTimeMillis()
            )
        )
    }

    private fun restoreRecycleBinFile(recycleFile: File) {
        if (!recycleFile.exists()) throw IOException(string(R.string.local_file_manager_restore_recycle_bin_missing))
        val record = recycleBinRecordStore.recordForRecyclePath(recycleFile.absolutePath)
            ?: throw IOException(string(R.string.local_file_manager_restore_recycle_bin_missing_record))
        if (record.originalPath.startsWith("content://", ignoreCase = true)) {
            restoreRecycleBinFileToSaf(recycleFile, record)
            recycleBinRecordStore.removeForRecyclePath(recycleFile.absolutePath)
            return
        }
        val originalTarget = File(record.originalPath)
        val parent = originalTarget.parentFile
            ?: throw IOException(string(R.string.local_file_manager_restore_recycle_bin_failed))
        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException(string(R.string.local_file_manager_restore_recycle_bin_failed))
        }
        val target = if (originalTarget.exists()) transferNamingPolicy.nextAvailableLocalCopyTarget(parent, record.originalName) else originalTarget
        if (!recycleFile.renameTo(target)) {
            if (recycleFile.isDirectory) {
                recycleFile.copyRecursively(target, overwrite = false)
                if (!recycleFile.deleteRecursively()) throw IOException(string(R.string.local_file_manager_restore_recycle_bin_failed))
            } else {
                recycleFile.copyTo(target, overwrite = false)
                if (!recycleFile.delete()) throw IOException(string(R.string.local_file_manager_restore_recycle_bin_failed))
            }
        }
        recycleBinRecordStore.removeForRecyclePath(recycleFile.absolutePath)
    }

    private fun restoreRecycleBinFileToSaf(recycleFile: File, record: RecycleBinRecord) {
        val originalUri = Uri.parse(record.originalPath)
        val parentUri = safProvider.parentUriFor(originalUri)
            ?: throw IOException(string(R.string.local_file_manager_restore_recycle_bin_saf_unsupported))
        val parent = safProvider.documentFromUri(parentUri) ?: safProvider.treeFromUri(parentUri)
            ?: throw IOException(string(R.string.local_file_manager_restore_recycle_bin_saf_unsupported))
        if (!parent.exists() || !parent.isDirectory || !parent.canWrite()) {
            throw IOException(string(R.string.local_file_manager_restore_recycle_bin_saf_unsupported))
        }
        copyLocalRecycleFileToSaf(recycleFile, parent, record.originalName.ifBlank { recycleFile.name })
        if (recycleFile.isDirectory) {
            if (!recycleFile.deleteRecursively()) throw IOException(string(R.string.local_file_manager_restore_recycle_bin_failed))
        } else if (!recycleFile.delete()) {
            throw IOException(string(R.string.local_file_manager_restore_recycle_bin_failed))
        }
    }

    private fun copyLocalRecycleFileToSaf(source: File, targetParent: DocumentFile, desiredName: String) {
        if (!source.exists()) throw IOException(string(R.string.local_file_manager_restore_recycle_bin_missing))
        val targetName = if (targetParent.findFile(desiredName) == null) desiredName else transferNamingPolicy.nextAvailableSafCopyName(targetParent, desiredName)
        if (source.isDirectory) {
            val targetDirectory = targetParent.createDirectory(targetName)
                ?: throw IOException(string(R.string.local_file_manager_restore_recycle_bin_failed))
            source.listFiles().orEmpty().forEach { child ->
                copyLocalRecycleFileToSaf(child, targetDirectory, child.name)
            }
            return
        }
        val targetFile = targetParent.createFile(FileMimeTypePolicy.mimeTypeForName(targetName), targetName)
            ?: throw IOException(string(R.string.local_file_manager_restore_recycle_bin_failed))
        source.inputStream().use { input ->
            appContext.contentResolver.openOutputStream(targetFile.uri, "w")?.use { output ->
                input.copyTo(output)
            } ?: throw IOException(string(R.string.local_file_manager_restore_recycle_bin_failed))
        }
    }

    private fun deleteEntryInternalBlocking(entry: FileManagerEntry) {
        ensureCanDelete(entry)
        when (val source = entry.source) {
            is FileEntrySource.LocalFile -> {
                if (!source.file.exists()) throw IOException(string(R.string.local_file_manager_delete_missing))
                val deleted = if (source.file.isDirectory) source.file.deleteRecursively() else source.file.delete()
                if (!deleted) throw IOException(string(R.string.local_file_manager_delete_failed))
            }
            is FileEntrySource.DocumentUri -> {
                val document = safProvider.documentFromUri(source.uri)
                    ?: throw IOException(string(R.string.local_file_manager_delete_missing))
                if (!document.delete()) throw IOException(string(R.string.local_file_manager_delete_failed))
            }
            is FileEntrySource.ParentDirectory,
            is FileEntrySource.ArchiveEntry,
            is FileEntrySource.RootPath,
            is FileEntrySource.ContentUri -> throw IOException(string(R.string.local_file_manager_delete_no_permission))
        }
    }

    private suspend fun deleteEntryInternal(entry: FileManagerEntry) {
        ensureCanDelete(entry)
        when (val source = entry.source) {
            is FileEntrySource.LocalFile -> {
                if (!source.file.exists()) throw IOException(string(R.string.local_file_manager_delete_missing))
                if (source.file.isDirectory) {
                    source.file.deleteRecursivelyCancellable()
                } else {
                    currentCoroutineContext().ensureActive()
                    if (!source.file.delete()) throw IOException(string(R.string.local_file_manager_delete_failed))
                }
            }
            is FileEntrySource.DocumentUri -> {
                val document = safProvider.documentFromUri(source.uri)
                    ?: throw IOException(string(R.string.local_file_manager_delete_missing))
                if (!document.delete()) throw IOException(string(R.string.local_file_manager_delete_failed))
            }
            is FileEntrySource.ParentDirectory,
            is FileEntrySource.ArchiveEntry,
            is FileEntrySource.RootPath,
            is FileEntrySource.ContentUri -> throw IOException(string(R.string.local_file_manager_delete_no_permission))
        }
    }

    private fun ensureCanDelete(entry: FileManagerEntry) {
        if (!entry.capabilities.canDelete) {
            throw IOException(string(R.string.local_file_manager_delete_no_permission))
        }
    }

    private suspend fun copySafDocumentToLocalDirectory(source: DocumentFile, targetDirectory: File, targetName: String) {
        if (!source.exists()) throw IOException(string(R.string.local_file_manager_batch_copy_missing))
        if (!targetDirectory.exists() || !targetDirectory.isDirectory || !targetDirectory.canWrite()) {
            throw IOException(string(R.string.local_file_manager_batch_copy_no_permission))
        }
        val target = resolveLocalTransferTarget(targetDirectory, targetName)
        if (source.isDirectory) {
            if (!target.mkdirs()) throw IOException(string(R.string.local_file_manager_batch_copy_failed))
            source.listFiles().forEach { child ->
                currentCoroutineContext().ensureActive()
                val childName = child.name.orEmpty().ifBlank { string(R.string.local_file_preview_unknown_name) }
                copySafDocumentToLocalDirectory(child, target, childName)
            }
            return
        }
        appContext.contentResolver.openInputStream(source.uri)?.use { input ->
            target.outputStream().use { output -> input.copyToCancellable(output) }
        } ?: throw IOException(string(R.string.local_file_manager_batch_copy_missing))
    }

    private suspend fun resolveLocalTransferTarget(directory: File, desiredName: String): File {
        return transferNamingPolicy.resolveLocalTransferTarget(
            directory = directory,
            desiredName = desiredName,
            policy = TransferConflictPolicy.KeepBoth,
            deleteExisting = { it.deleteRecursivelyCancellable() }
        )
    }

    private fun string(resId: Int): String = appContext.getString(resId)

    private fun string(resId: Int, vararg args: Any): String = appContext.getString(resId, *args)

    private companion object {
        const val RecycleBinDirectoryName = "file-manager-recycle-bin"
    }
}
