package com.Sunset.REN.GitHub.data.filemanager.provider

import com.Sunset.REN.GitHub.data.filemanager.LocalFileAccessProvider
import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileListResult
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileOperationResult
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileReadResult
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileStatResult
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileSystemCapabilities
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileSystemProvider
import com.Sunset.REN.GitHub.domain.filemanager.path.FileManagerPath
import com.Sunset.REN.GitHub.domain.filemanager.path.FileSystemProviderId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalFileSystemProvider(
    private val localFileAccessProvider: LocalFileAccessProvider
) : FileSystemProvider {
    override val id: FileSystemProviderId = FileSystemProviderId.Local
    override val displayName: String = "本地文件"
    override val capabilities: FileSystemCapabilities = FileSystemCapabilities.Local

    override suspend fun list(path: FileManagerPath): FileListResult = withContext(Dispatchers.IO) {
        val local = path as? FileManagerPath.Local ?: return@withContext FileListResult.Failed("不是本地路径")
        localFileAccessProvider.listDirectory(File(local.absolutePath), verifyFileType = false).fold(
            onSuccess = { FileListResult.Success(it) },
            onFailure = { FileListResult.Failed(it.message ?: "读取目录失败", it) }
        )
    }

    override suspend fun stat(path: FileManagerPath): FileStatResult = withContext(Dispatchers.IO) {
        val local = path as? FileManagerPath.Local ?: return@withContext FileStatResult.Failed("不是本地路径")
        runCatching { localFileAccessProvider.buildEntry(File(local.absolutePath), verifyFileType = false) }.fold(
            onSuccess = { FileStatResult.Success(it) },
            onFailure = { FileStatResult.Failed(it.message ?: "读取属性失败", it) }
        )
    }

    override suspend fun read(path: FileManagerPath): FileReadResult = withContext(Dispatchers.IO) {
        val local = path as? FileManagerPath.Local ?: return@withContext FileReadResult.Failed("不是本地路径")
        runCatching { File(local.absolutePath).readBytes() }.fold(
            onSuccess = { FileReadResult.Success(it) },
            onFailure = { FileReadResult.Failed(it.message ?: "读取文件失败", it) }
        )
    }

    override suspend fun write(path: FileManagerPath, content: ByteArray): FileOperationResult = withContext(Dispatchers.IO) {
        val local = path as? FileManagerPath.Local ?: return@withContext FileOperationResult.Failed("不是本地路径")
        runCatching { File(local.absolutePath).writeBytes(content) }.fold(
            onSuccess = { FileOperationResult.Success },
            onFailure = { FileOperationResult.Failed(it.message ?: "写入文件失败", it) }
        )
    }

    override suspend fun createDirectory(path: FileManagerPath): FileOperationResult = withContext(Dispatchers.IO) {
        val local = path as? FileManagerPath.Local ?: return@withContext FileOperationResult.Failed("不是本地路径")
        val directory = File(local.absolutePath)
        if (directory.mkdirs() || directory.isDirectory) FileOperationResult.Success else FileOperationResult.Failed("创建目录失败")
    }

    override suspend fun rename(path: FileManagerPath, newName: String): FileOperationResult = withContext(Dispatchers.IO) {
        val local = path as? FileManagerPath.Local ?: return@withContext FileOperationResult.Failed("不是本地路径")
        val source = File(local.absolutePath)
        val target = File(source.parentFile, newName)
        if (source.renameTo(target)) FileOperationResult.Success else FileOperationResult.Failed("重命名失败")
    }

    override suspend fun delete(path: FileManagerPath): FileOperationResult = withContext(Dispatchers.IO) {
        val local = path as? FileManagerPath.Local ?: return@withContext FileOperationResult.Failed("不是本地路径")
        val file = File(local.absolutePath)
        if (file.deleteRecursively()) FileOperationResult.Success else FileOperationResult.Failed("删除失败")
    }
}
