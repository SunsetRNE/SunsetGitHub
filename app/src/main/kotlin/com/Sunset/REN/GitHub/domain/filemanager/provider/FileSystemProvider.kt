package com.Sunset.REN.GitHub.domain.filemanager.provider

import com.Sunset.REN.GitHub.domain.filemanager.FileEntryCapabilities
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.path.FileManagerPath
import com.Sunset.REN.GitHub.domain.filemanager.path.FileSystemProviderId

/** Capability description for a concrete file source. */
data class FileSystemCapabilities(
    val canList: Boolean,
    val canStat: Boolean,
    val canRead: Boolean,
    val canWrite: Boolean,
    val canCreateDirectory: Boolean,
    val canRename: Boolean,
    val canDelete: Boolean,
    val canArchive: Boolean,
    val canExtract: Boolean,
    val canEditPermission: Boolean,
    val canEditOwner: Boolean
) {
    companion object {
        val Local = FileSystemCapabilities(true, true, true, true, true, true, true, true, true, false, false)
        val Saf = FileSystemCapabilities(true, true, true, true, true, true, true, false, true, false, false)
        val Archive = FileSystemCapabilities(true, true, true, false, false, false, false, false, true, false, false)
        val RootBrowsingOnly = FileSystemCapabilities(true, true, true, false, false, false, false, false, false, false, false)
        val RootGranted = RootBrowsingOnly
        val RootUnavailable = FileSystemCapabilities(false, false, false, false, false, false, false, false, false, false, false)
    }
}

sealed interface FileOperationResult {
    data object Success : FileOperationResult
    data class Failed(val message: String, val throwable: Throwable? = null) : FileOperationResult
}

sealed interface FileListResult {
    data class Success(val entries: List<FileManagerEntry>) : FileListResult
    data class Failed(val message: String, val throwable: Throwable? = null) : FileListResult
}

sealed interface FileStatResult {
    data class Success(val entry: FileManagerEntry) : FileStatResult
    data class Failed(val message: String, val throwable: Throwable? = null) : FileStatResult
}

sealed interface FileReadResult {
    data class Success(val bytes: ByteArray) : FileReadResult
    data class Failed(val message: String, val throwable: Throwable? = null) : FileReadResult
}

interface FileSystemProvider {
    val id: FileSystemProviderId
    val displayName: String
    val capabilities: FileSystemCapabilities

    suspend fun list(path: FileManagerPath): FileListResult
    suspend fun stat(path: FileManagerPath): FileStatResult
    suspend fun read(path: FileManagerPath): FileReadResult
    suspend fun write(path: FileManagerPath, content: ByteArray): FileOperationResult
    suspend fun createDirectory(path: FileManagerPath): FileOperationResult
    suspend fun rename(path: FileManagerPath, newName: String): FileOperationResult
    suspend fun delete(path: FileManagerPath): FileOperationResult
}

fun FileEntryCapabilities.toProviderLikeCapabilities(): FileSystemCapabilities = FileSystemCapabilities(
    canList = canRead,
    canStat = true,
    canRead = canRead || canAccessContent,
    canWrite = canWrite,
    canCreateDirectory = canCreateChild,
    canRename = canRename,
    canDelete = canDelete,
    canArchive = canRead,
    canExtract = canRead,
    canEditPermission = false,
    canEditOwner = false
)
