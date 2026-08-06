package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.filemanager.SafFileAccessProvider
import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class FileTransferCoordinator(
    context: Context,
    private val safProvider: SafFileAccessProvider,
    private val navigator: FileManagerNavigator,
    private val namingPolicy: FileTransferNamingPolicy,
    private val copyArchiveEntryToLocalDirectory: suspend (FileEntrySource.ArchiveEntry, File, TransferConflictPolicy) -> Unit,
    private val copyArchiveEntryToSafDirectory: suspend (FileEntrySource.ArchiveEntry, DocumentFile, TransferConflictPolicy) -> Unit,
    private val deleteEntryPermanently: suspend (FileManagerEntry) -> Unit
) {
    private val appContext = context.applicationContext

    suspend fun copyEntriesInCurrentDirectory(
        entries: List<FileManagerEntry>,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchCopyResult = withContext(Dispatchers.IO) {
        copyEntriesInternal(entries, onProgress) { entry -> copyEntryInCurrentDirectory(entry) }
    }

    suspend fun copyEntriesToDirectoryPath(
        entries: List<FileManagerEntry>,
        targetDirectoryPath: String,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> },
        conflictPolicy: TransferConflictPolicy = TransferConflictPolicy.Fail
    ): BatchCopyResult = withContext(Dispatchers.IO) {
        copyEntriesInternal(entries, onProgress) { entry ->
            copyEntryToDirectoryPath(entry, targetDirectoryPath, conflictPolicy)
        }
    }

    suspend fun moveEntriesToParentDirectory(
        entries: List<FileManagerEntry>,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchMoveResult = withContext(Dispatchers.IO) {
        moveEntriesInternal(entries, onProgress) { entry -> moveEntryToParentDirectory(entry) }
    }

    suspend fun moveEntriesToDirectoryPath(
        entries: List<FileManagerEntry>,
        targetDirectoryPath: String,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> },
        conflictPolicy: TransferConflictPolicy = TransferConflictPolicy.Fail
    ): BatchMoveResult = withContext(Dispatchers.IO) {
        moveEntriesInternal(entries, onProgress) { entry ->
            moveEntryToDirectoryPath(entry, targetDirectoryPath, conflictPolicy)
        }
    }

    private suspend fun copyEntriesInternal(
        entries: List<FileManagerEntry>,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit,
        copyEntry: suspend (FileManagerEntry) -> Unit
    ): BatchCopyResult {
        val failures = mutableListOf<BatchCopyFailure>()
        var completedCount = 0
        entries.forEach { entry ->
            currentCoroutineContext().ensureActive()
            runCatching { copyEntry(entry) }
                .onFailure { error ->
                    failures += BatchCopyFailure(
                        entry = entry,
                        message = error.message ?: string(R.string.local_file_manager_batch_copy_failed)
                    )
                }
            completedCount++
            onProgress(completedCount, entries.size)
        }
        return BatchCopyResult(entries.size, entries.size - failures.size, failures)
    }

    private suspend fun moveEntriesInternal(
        entries: List<FileManagerEntry>,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit,
        moveEntry: suspend (FileManagerEntry) -> Unit
    ): BatchMoveResult {
        val failures = mutableListOf<BatchMoveFailure>()
        var completedCount = 0
        entries.forEach { entry ->
            currentCoroutineContext().ensureActive()
            runCatching { moveEntry(entry) }
                .onFailure { error ->
                    failures += BatchMoveFailure(
                        entry = entry,
                        message = error.message ?: string(R.string.local_file_manager_batch_move_failed)
                    )
                }
            completedCount++
            onProgress(completedCount, entries.size)
        }
        return BatchMoveResult(entries.size, entries.size - failures.size, failures)
    }

    private suspend fun copyEntryInCurrentDirectory(entry: FileManagerEntry) {
        when (val source = entry.source) {
            is FileEntrySource.LocalFile -> copyLocalEntryInCurrentDirectory(source.file)
            is FileEntrySource.DocumentUri -> copySafEntryInCurrentDirectory(source.uri)
            is FileEntrySource.ParentDirectory,
            is FileEntrySource.ArchiveEntry,
            is FileEntrySource.RootPath,
            is FileEntrySource.ContentUri -> throw IOException(string(R.string.local_file_manager_batch_copy_no_permission))
        }
    }

    private suspend fun copyEntryToDirectoryPath(
        entry: FileManagerEntry,
        targetDirectoryPath: String,
        conflictPolicy: TransferConflictPolicy
    ) {
        val targetUri = navigator.toContentUriOrNull(targetDirectoryPath)
        if (targetUri != null) {
            copyEntryToSafDirectory(entry, targetUri, conflictPolicy)
        } else {
            copyEntryToLocalDirectory(entry, File(targetDirectoryPath), conflictPolicy)
        }
    }

    private suspend fun copyEntryToSafDirectory(
        entry: FileManagerEntry,
        targetDirectoryUri: Uri,
        conflictPolicy: TransferConflictPolicy
    ) {
        val targetDirectory = safProvider.documentFromUri(targetDirectoryUri) ?: safProvider.treeFromUri(targetDirectoryUri)
            ?: throw IOException(string(R.string.local_file_manager_batch_copy_target_unavailable))
        if (!targetDirectory.exists() || !targetDirectory.isDirectory) {
            throw IOException(string(R.string.local_file_manager_batch_copy_target_unavailable))
        }
        if (!targetDirectory.canWrite()) {
            throw IOException(string(R.string.local_file_manager_batch_copy_no_permission))
        }
        when (val source = entry.source) {
            is FileEntrySource.LocalFile -> copyLocalEntryToSafDirectory(source.file, targetDirectory, conflictPolicy)
            is FileEntrySource.ArchiveEntry -> copyArchiveEntryToSafDirectory(source, targetDirectory, conflictPolicy)
            is FileEntrySource.DocumentUri -> {
                val document = safProvider.documentFromUri(source.uri)
                    ?: throw IOException(string(R.string.local_file_manager_batch_copy_missing))
                val desiredName = document.name.orEmpty().ifBlank { entry.name }
                val targetName = resolveSafTransferName(targetDirectory, desiredName, conflictPolicy)
                copySafDocument(document, targetDirectory, targetName)
            }
            is FileEntrySource.ParentDirectory,
            is FileEntrySource.RootPath,
            is FileEntrySource.ContentUri -> throw IOException(string(R.string.local_file_manager_batch_copy_no_permission))
        }
    }

    private suspend fun copyEntryToLocalDirectory(
        entry: FileManagerEntry,
        targetDirectory: File,
        conflictPolicy: TransferConflictPolicy
    ) {
        if (!targetDirectory.exists() || !targetDirectory.isDirectory) {
            throw IOException(string(R.string.local_file_manager_batch_copy_target_unavailable))
        }
        if (!targetDirectory.canWrite()) {
            throw IOException(string(R.string.local_file_manager_batch_copy_no_permission))
        }
        when (val source = entry.source) {
            is FileEntrySource.LocalFile -> copyLocalEntryToDirectory(source.file, targetDirectory, conflictPolicy)
            is FileEntrySource.DocumentUri -> copySafEntryToLocalDirectory(source.uri, targetDirectory, conflictPolicy)
            is FileEntrySource.ArchiveEntry -> copyArchiveEntryToLocalDirectory(source, targetDirectory, conflictPolicy)
            is FileEntrySource.ParentDirectory,
            is FileEntrySource.RootPath,
            is FileEntrySource.ContentUri -> throw IOException(string(R.string.local_file_manager_batch_copy_no_permission))
        }
    }

    private suspend fun copyLocalEntryInCurrentDirectory(source: File) {
        if (!source.exists()) throw IOException(string(R.string.local_file_manager_batch_copy_missing))
        val parent = source.parentFile ?: throw IOException(string(R.string.local_file_manager_batch_copy_failed))
        if (!parent.canWrite()) throw IOException(string(R.string.local_file_manager_batch_copy_no_permission))
        copyLocalEntryToDirectory(source, parent, TransferConflictPolicy.KeepBoth)
    }

    private suspend fun copyLocalEntryToDirectory(
        source: File,
        targetDirectory: File,
        conflictPolicy: TransferConflictPolicy
    ) {
        currentCoroutineContext().ensureActive()
        if (!source.exists()) throw IOException(string(R.string.local_file_manager_batch_copy_missing))
        val sourceCanonical = source.canonicalFile
        val targetCanonical = targetDirectory.canonicalFile
        if (source.isDirectory && targetCanonical.isSameOrDescendantOf(sourceCanonical)) {
            throw IOException(string(R.string.local_file_manager_transfer_target_inside_source))
        }
        if (sourceCanonical.parentFile == targetCanonical) {
            val target = namingPolicy.nextAvailableLocalCopyTarget(targetDirectory, source.name)
            if (source.isDirectory) source.copyRecursivelyCancellable(target) else source.copyToCancellable(target, overwrite = false)
            return
        }
        if (!targetDirectory.canWrite()) throw IOException(string(R.string.local_file_manager_batch_copy_no_permission))
        val directTarget = File(targetDirectory, source.name)
        if (conflictPolicy == TransferConflictPolicy.Replace && directTarget.exists() && source.canonicalFile == directTarget.canonicalFile) {
            throw IOException(string(R.string.local_file_manager_batch_copy_exists))
        }
        val target = resolveLocalTransferTarget(targetDirectory, source.name, conflictPolicy)
        if (source.isDirectory) source.copyRecursivelyCancellable(target) else source.copyToCancellable(target, overwrite = false)
    }

    private suspend fun copyLocalEntryToSafDirectory(
        source: File,
        targetDirectory: DocumentFile,
        conflictPolicy: TransferConflictPolicy
    ) {
        if (!source.exists()) throw IOException(string(R.string.local_file_manager_batch_copy_missing))
        if (source.isDirectory && targetDirectory.isLocalDocumentInside(source)) {
            throw IOException(string(R.string.local_file_manager_transfer_target_inside_source))
        }
        if (!targetDirectory.canWrite()) throw IOException(string(R.string.local_file_manager_batch_copy_no_permission))
        val targetName = resolveSafTransferName(targetDirectory, source.name, conflictPolicy)
        if (source.isDirectory) {
            val target = targetDirectory.createDirectory(targetName)
                ?: throw IOException(string(R.string.local_file_manager_batch_copy_failed))
            source.listFiles().orEmpty().forEach { child ->
                currentCoroutineContext().ensureActive()
                copyLocalEntryToSafDirectory(child, target, TransferConflictPolicy.Fail)
            }
            return
        }
        val targetFile = targetDirectory.createFile(FileMimeTypePolicy.mimeTypeForName(targetName), targetName)
            ?: throw IOException(string(R.string.local_file_manager_batch_copy_failed))
        source.inputStream().use { input ->
            appContext.contentResolver.openOutputStream(targetFile.uri, "w")?.use { output ->
                input.copyToCancellable(output)
            } ?: throw IOException(string(R.string.local_file_manager_batch_copy_failed))
        }
    }

    private suspend fun copySafEntryToLocalDirectory(
        sourceUri: Uri,
        targetDirectory: File,
        conflictPolicy: TransferConflictPolicy
    ) {
        val source = safProvider.documentFromUri(sourceUri)
            ?: throw IOException(string(R.string.local_file_manager_batch_copy_missing))
        val targetName = source.name.orEmpty().ifBlank { string(R.string.local_file_preview_unknown_name) }
        copySafDocumentToLocalDirectory(source, targetDirectory, targetName, conflictPolicy)
    }

    private suspend fun copySafDocumentToLocalDirectory(
        source: DocumentFile,
        targetDirectory: File,
        targetName: String,
        conflictPolicy: TransferConflictPolicy
    ) {
        if (!source.exists()) throw IOException(string(R.string.local_file_manager_batch_copy_missing))
        if (source.isDirectory && source.isLocalDocumentAncestorOf(targetDirectory)) {
            throw IOException(string(R.string.local_file_manager_transfer_target_inside_source))
        }
        if (!targetDirectory.exists() || !targetDirectory.isDirectory || !targetDirectory.canWrite()) {
            throw IOException(string(R.string.local_file_manager_batch_copy_no_permission))
        }
        val target = resolveLocalTransferTarget(targetDirectory, targetName, conflictPolicy)
        if (source.isDirectory) {
            if (!target.mkdirs()) throw IOException(string(R.string.local_file_manager_batch_copy_failed))
            source.listFiles().forEach { child ->
                currentCoroutineContext().ensureActive()
                val childName = child.name.orEmpty().ifBlank { string(R.string.local_file_preview_unknown_name) }
                copySafDocumentToLocalDirectory(child, target, childName, TransferConflictPolicy.Fail)
            }
            return
        }
        appContext.contentResolver.openInputStream(source.uri)?.use { input ->
            target.outputStream().use { output -> input.copyToCancellable(output) }
        } ?: throw IOException(string(R.string.local_file_manager_batch_copy_missing))
    }

    private suspend fun copySafEntryInCurrentDirectory(sourceUri: Uri) {
        val source = safProvider.documentFromUri(sourceUri)
            ?: throw IOException(string(R.string.local_file_manager_batch_copy_missing))
        val parent = source.parentFile ?: throw IOException(string(R.string.local_file_manager_batch_copy_failed))
        if (!parent.canWrite()) throw IOException(string(R.string.local_file_manager_batch_copy_no_permission))
        val targetName = namingPolicy.nextAvailableSafCopyName(parent, source.name.orEmpty().ifBlank { string(R.string.local_file_preview_unknown_name) })
        copySafDocument(source, parent, targetName)
    }

    private suspend fun copySafDocument(source: DocumentFile, targetParent: DocumentFile, targetName: String) {
        if (source.isDirectory) {
            if (targetParent.uri.isSameOrDescendantDocumentOf(source.uri)) {
                throw IOException(string(R.string.local_file_manager_transfer_target_inside_source))
            }
            val targetDirectory = targetParent.createDirectory(targetName)
                ?: throw IOException(string(R.string.local_file_manager_batch_copy_failed))
            source.listFiles().forEach { child ->
                currentCoroutineContext().ensureActive()
                val childName = child.name.orEmpty().ifBlank { string(R.string.local_file_preview_unknown_name) }
                copySafDocument(child, targetDirectory, childName)
            }
            return
        }
        val targetFile = targetParent.createFile(source.type ?: FileMimeTypePolicy.mimeTypeForName(targetName), targetName)
            ?: throw IOException(string(R.string.local_file_manager_batch_copy_failed))
        appContext.contentResolver.openInputStream(source.uri)?.use { input ->
            appContext.contentResolver.openOutputStream(targetFile.uri, "w")?.use { output ->
                input.copyToCancellable(output)
            } ?: throw IOException(string(R.string.local_file_manager_batch_copy_failed))
        } ?: throw IOException(string(R.string.local_file_manager_batch_copy_missing))
    }

    private suspend fun moveEntryToDirectoryPath(
        entry: FileManagerEntry,
        targetDirectoryPath: String,
        conflictPolicy: TransferConflictPolicy
    ) {
        if (conflictPolicy == TransferConflictPolicy.KeepBoth || conflictPolicy == TransferConflictPolicy.Replace) {
            copyEntryToDirectoryPath(entry, targetDirectoryPath, conflictPolicy)
            deleteMovedSourceAfterCopy(entry)
            return
        }
        val targetUri = navigator.toContentUriOrNull(targetDirectoryPath)
        if (targetUri != null) {
            moveEntryToSafDirectory(entry, targetUri)
        } else {
            moveEntryToLocalDirectory(entry, File(targetDirectoryPath))
        }
    }

    private suspend fun moveEntryToSafDirectory(entry: FileManagerEntry, targetDirectoryUri: Uri) {
        copyEntryToSafDirectory(entry, targetDirectoryUri, TransferConflictPolicy.Fail)
        deleteMovedSourceAfterCopy(entry)
    }

    private suspend fun deleteMovedSourceAfterCopy(entry: FileManagerEntry) {
        if (entry.source is FileEntrySource.ArchiveEntry) return
        try {
            deleteEntryPermanently(entry)
        } catch (error: Throwable) {
            throw IOException(string(R.string.local_file_manager_batch_move_copied_but_delete_failed), error)
        }
    }

    private suspend fun moveEntryToParentDirectory(entry: FileManagerEntry) {
        when (val source = entry.source) {
            is FileEntrySource.LocalFile -> moveLocalEntryToParentDirectory(source.file)
            is FileEntrySource.DocumentUri -> moveSafEntryToParentDirectory(source.uri)
            is FileEntrySource.ParentDirectory,
            is FileEntrySource.ArchiveEntry,
            is FileEntrySource.RootPath,
            is FileEntrySource.ContentUri -> throw IOException(string(R.string.local_file_manager_batch_move_no_permission))
        }
    }

    private suspend fun moveEntryToLocalDirectory(entry: FileManagerEntry, targetDirectory: File) {
        if (!targetDirectory.exists() || !targetDirectory.isDirectory) {
            throw IOException(string(R.string.local_file_manager_batch_move_target_unavailable))
        }
        if (!targetDirectory.canWrite()) {
            throw IOException(string(R.string.local_file_manager_batch_move_no_permission))
        }
        when (val source = entry.source) {
            is FileEntrySource.LocalFile -> moveLocalEntryToDirectory(source.file, targetDirectory)
            is FileEntrySource.DocumentUri -> moveSafEntryToLocalDirectory(source.uri, targetDirectory)
            is FileEntrySource.ArchiveEntry -> copyArchiveEntryToLocalDirectory(source, targetDirectory, TransferConflictPolicy.Fail)
            is FileEntrySource.ParentDirectory,
            is FileEntrySource.RootPath,
            is FileEntrySource.ContentUri -> throw IOException(string(R.string.local_file_manager_batch_move_no_permission))
        }
    }

    private suspend fun moveLocalEntryToParentDirectory(source: File) {
        if (!source.exists()) throw IOException(string(R.string.local_file_manager_batch_move_missing))
        val currentParent = source.parentFile ?: throw IOException(string(R.string.local_file_manager_batch_move_no_parent))
        val targetParent = currentParent.parentFile ?: throw IOException(string(R.string.local_file_manager_batch_move_no_parent))
        moveLocalEntryToDirectory(source, targetParent)
    }

    private suspend fun moveLocalEntryToDirectory(source: File, targetDirectory: File) {
        currentCoroutineContext().ensureActive()
        if (!source.exists()) throw IOException(string(R.string.local_file_manager_batch_move_missing))
        val sourceCanonical = source.canonicalFile
        val targetDirectoryCanonical = targetDirectory.canonicalFile
        if (source.isDirectory && targetDirectoryCanonical.isSameOrDescendantOf(sourceCanonical)) {
            throw IOException(string(R.string.local_file_manager_transfer_target_inside_source))
        }
        if (sourceCanonical.parentFile == targetDirectoryCanonical) {
            throw IOException(string(R.string.local_file_manager_batch_move_exists))
        }
        if (source.parentFile?.canWrite() != true || !targetDirectory.canWrite()) {
            throw IOException(string(R.string.local_file_manager_batch_move_no_permission))
        }
        val target = File(targetDirectory, source.name)
        if (target.exists()) throw IOException(string(R.string.local_file_manager_batch_move_exists))
        if (!source.renameTo(target)) {
            if (source.isDirectory) {
                source.copyRecursivelyCancellable(target)
                source.deleteRecursivelyCancellable()
            } else {
                source.copyToCancellable(target, overwrite = false)
                currentCoroutineContext().ensureActive()
                if (!source.delete()) throw IOException(string(R.string.local_file_manager_batch_move_failed))
            }
        }
    }

    private suspend fun moveSafEntryToParentDirectory(sourceUri: Uri) {
        val source = safProvider.documentFromUri(sourceUri)
            ?: throw IOException(string(R.string.local_file_manager_batch_move_missing))
        val currentParent = source.parentFile ?: throw IOException(string(R.string.local_file_manager_batch_move_no_parent))
        val targetParent = currentParent.parentFile ?: throw IOException(string(R.string.local_file_manager_batch_move_no_parent))
        if (!currentParent.canWrite() || !targetParent.canWrite()) throw IOException(string(R.string.local_file_manager_batch_move_no_permission))
        val targetName = source.name.orEmpty().ifBlank { string(R.string.local_file_preview_unknown_name) }
        if (targetParent.findFile(targetName) != null) throw IOException(string(R.string.local_file_manager_batch_move_exists))
        copySafDocument(source, targetParent, targetName)
        if (!source.delete()) throw IOException(string(R.string.local_file_manager_batch_move_failed))
    }

    private suspend fun moveSafEntryToLocalDirectory(sourceUri: Uri, targetDirectory: File) {
        val source = safProvider.documentFromUri(sourceUri)
            ?: throw IOException(string(R.string.local_file_manager_batch_move_missing))
        val targetName = source.name.orEmpty().ifBlank { string(R.string.local_file_preview_unknown_name) }
        copySafDocumentToLocalDirectory(source, targetDirectory, targetName, TransferConflictPolicy.Fail)
        try {
            if (!source.delete()) throw IOException(string(R.string.local_file_manager_batch_move_failed))
        } catch (error: Throwable) {
            throw IOException(string(R.string.local_file_manager_batch_move_copied_but_delete_failed), error)
        }
    }

    private suspend fun resolveLocalTransferTarget(directory: File, desiredName: String, policy: TransferConflictPolicy): File {
        return namingPolicy.resolveLocalTransferTarget(directory, desiredName, policy, ::deleteExistingLocalTransferTarget)
    }

    private suspend fun resolveSafTransferName(directory: DocumentFile, desiredName: String, policy: TransferConflictPolicy): String {
        return namingPolicy.resolveSafTransferName(directory, desiredName, policy, ::deleteExistingSafTransferTarget)
    }

    private suspend fun deleteExistingLocalTransferTarget(target: File) {
        currentCoroutineContext().ensureActive()
        if (!target.exists()) return
        if (target.isDirectory) {
            target.deleteRecursivelyCancellable()
        } else if (!target.delete()) {
            throw IOException(string(R.string.local_file_manager_delete_failed))
        }
    }

    private suspend fun deleteExistingSafTransferTarget(target: DocumentFile) {
        currentCoroutineContext().ensureActive()
        if (target.exists() && !target.delete()) {
            throw IOException(string(R.string.local_file_manager_delete_failed))
        }
    }

    private fun string(resId: Int): String = appContext.getString(resId)
}
