package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import com.Sunset.REN.GitHub.data.filemanager.LocalFileManagerSettingsStore
import com.Sunset.REN.GitHub.data.filemanager.RecycleBinRecordStore
import com.Sunset.REN.GitHub.data.filemanager.SafFileAccessProvider
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.RecycleBinSettings
import java.io.File

/**
 * Coordinates recycle-bin settings and file deletion/restore operations for the
 * local file manager screen.
 *
 * FileDeletionCoordinator still owns the low-level filesystem work. This class
 * keeps LocalFileManagerViewModel from wiring recycle-bin storage, settings and
 * deletion behavior directly.
 */
class RecycleBinCoordinator(
    context: Context,
    safProvider: SafFileAccessProvider,
    private val settingsStore: LocalFileManagerSettingsStore,
    transferNamingPolicy: FileTransferNamingPolicy = FileTransferNamingPolicy(context)
) {
    private val recycleBinRecordStore = RecycleBinRecordStore(context)
    private val deletionCoordinator = FileDeletionCoordinator(
        context = context,
        safProvider = safProvider,
        recycleBinRecordStore = recycleBinRecordStore,
        recycleBinSettingsProvider = settingsStore::recycleBinSettings,
        transferNamingPolicy = transferNamingPolicy
    )

    fun settings(): RecycleBinSettings = settingsStore.recycleBinSettings()

    fun setSettings(settings: RecycleBinSettings) {
        settingsStore.setRecycleBinSettings(settings)
    }

    fun defaultMoveToRecycleBin(): Boolean = settings().defaultMoveToRecycleBin

    fun isEnabled(): Boolean = settingsStore.isRecycleBinEnabled()

    fun toggleEnabled(): Boolean = settingsStore.toggleRecycleBinEnabled()

    fun deleteEntryBlocking(entry: FileManagerEntry, moveToRecycleBin: Boolean): Result<Unit> {
        return deletionCoordinator.deleteEntryBlocking(entry, moveToRecycleBin)
    }

    suspend fun deleteEntries(
        entries: List<FileManagerEntry>,
        moveToRecycleBin: Boolean,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchDeleteResult {
        return deletionCoordinator.deleteEntries(entries, moveToRecycleBin, onProgress)
    }

    suspend fun deleteEntryPermanently(entry: FileManagerEntry) {
        deletionCoordinator.deleteEntryPermanently(entry)
    }

    fun directory(): File = deletionCoordinator.recycleBinDirectory()

    fun cleanIfNeeded(): Int = deletionCoordinator.cleanRecycleBinIfNeeded()

    fun clear(): Result<Unit> = deletionCoordinator.clearRecycleBin()

    fun isRecycleBinPath(path: String): Boolean = deletionCoordinator.isRecycleBinPath(path)

    fun restoreEntries(entries: List<FileManagerEntry>): BatchRestoreResult {
        return deletionCoordinator.restoreRecycleBinEntries(entries)
    }
}
