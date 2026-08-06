package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipFile

class ArchiveOperationCoordinator(
    context: Context,
    private val navigator: FileManagerNavigator,
    private val namingPolicy: FileTransferNamingPolicy,
    private val listArchiveDirectory: suspend (archiveFile: File, innerPath: String) -> List<FileManagerEntry>
) {
    private val appContext = context.applicationContext

    suspend fun copyArchiveEntryToSafDirectory(
        source: FileEntrySource.ArchiveEntry,
        targetDirectory: DocumentFile,
        conflictPolicy: TransferConflictPolicy = TransferConflictPolicy.Fail
    ) {
        if (!targetDirectory.canWrite()) throw IOException(string(R.string.local_file_manager_batch_copy_no_permission))
        if (source.isDirectory) {
            val targetName = resolveSafTransferName(targetDirectory, source.leafName(), conflictPolicy)
            val target = targetDirectory.createDirectory(targetName)
                ?: throw IOException(string(R.string.local_file_manager_batch_copy_failed))
            copyArchiveDirectoryChildrenToSaf(source.archiveFile, source.innerPath, target)
            return
        }
        val targetName = resolveSafTransferName(targetDirectory, source.leafName(), conflictPolicy)
        val target = targetDirectory.createFile(FileMimeTypePolicy.mimeTypeForName(targetName), targetName)
            ?: throw IOException(string(R.string.local_file_manager_batch_copy_failed))
        appContext.contentResolver.openOutputStream(target.uri, "w")?.use { output ->
            copyArchiveFileEntryToOutput(source.archiveFile, source.innerPath, output)
        } ?: throw IOException(string(R.string.local_file_manager_batch_copy_failed))
    }

    suspend fun copyArchiveEntryToLocalDirectory(
        source: FileEntrySource.ArchiveEntry,
        targetDirectory: File,
        conflictPolicy: TransferConflictPolicy = TransferConflictPolicy.Fail
    ) {
        if (!targetDirectory.exists() || !targetDirectory.isDirectory || !targetDirectory.canWrite()) {
            throw IOException(string(R.string.local_file_manager_batch_copy_no_permission))
        }
        val target = resolveLocalTransferTarget(targetDirectory, source.leafName(), conflictPolicy)
        if (source.isDirectory) {
            if (!target.mkdirs()) throw IOException(string(R.string.local_file_manager_batch_copy_failed))
            copyArchiveDirectoryChildrenToLocal(source.archiveFile, source.innerPath, target)
            return
        }
        target.outputStream().use { output ->
            copyArchiveFileEntryToOutput(source.archiveFile, source.innerPath, output)
        }
    }

    suspend fun cacheArchiveEntryForContentAccess(source: FileEntrySource.ArchiveEntry, displayName: String): File {
        if (source.isDirectory) throw IOException(string(R.string.local_file_manager_file_not_readable))
        val cacheDirectory = File(appContext.cacheDir, ArchiveContentCacheDirectory).also { it.mkdirs() }
        cleanupOldContentCacheFiles(cacheDirectory)
        val safeName = displayName.ifBlank { source.leafName() }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "archive-entry" }
        val cachedFile = File(cacheDirectory, "${System.currentTimeMillis()}-$safeName")
        cachedFile.outputStream().use { output ->
            copyArchiveFileEntryToOutput(source.archiveFile, source.innerPath, output)
        }
        return cachedFile
    }

    suspend fun copyArchiveFileEntryToOutput(archiveFile: File, innerPath: String, output: OutputStream) {
        currentCoroutineContext().ensureActive()
        val normalizedInnerPath = navigator.normalizeArchiveInnerPath(innerPath)
        ZipFile(archiveFile).use { zipFile ->
            val entry = zipFile.getEntry(normalizedInnerPath)
                ?: throw IOException(string(R.string.local_file_manager_batch_copy_missing))
            if (entry.isDirectory) throw IOException(string(R.string.local_file_manager_batch_copy_no_permission))
            zipFile.getInputStream(entry).use { input -> input.copyToCancellable(output) }
        }
    }

    private suspend fun copyArchiveDirectoryChildrenToSaf(archiveFile: File, innerPath: String, targetDirectory: DocumentFile) {
        listArchiveDirectory(archiveFile, innerPath).forEach { child ->
            val childSource = child.source as? FileEntrySource.ArchiveEntry ?: return@forEach
            copyArchiveEntryToSafDirectory(childSource, targetDirectory)
        }
    }

    private suspend fun copyArchiveDirectoryChildrenToLocal(archiveFile: File, innerPath: String, targetDirectory: File) {
        listArchiveDirectory(archiveFile, innerPath).forEach { child ->
            val childSource = child.source as? FileEntrySource.ArchiveEntry ?: return@forEach
            copyArchiveEntryToLocalDirectory(childSource, targetDirectory)
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

    private fun cleanupOldContentCacheFiles(cacheDirectory: File) {
        cacheDirectory.listFiles().orEmpty().forEach { cached ->
            if (System.currentTimeMillis() - cached.lastModified() > ArchiveContentCacheMaxAgeMillis) {
                cached.delete()
            }
        }
    }

    private fun FileEntrySource.ArchiveEntry.leafName(): String {
        return innerPath.substringAfterLast('/').ifBlank { string(R.string.local_file_preview_unknown_name) }
    }

    private fun string(resId: Int): String = appContext.getString(resId)

    private companion object {
        const val ArchiveContentCacheDirectory = "archive-content-cache"
        const val ArchiveContentCacheMaxAgeMillis = 5 * 60 * 1000L
    }
}
