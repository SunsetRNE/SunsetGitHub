package com.Sunset.REN.GitHub.data.filemanager.provider

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.Sunset.REN.GitHub.data.filemanager.SafFileAccessProvider
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

class SafFileSystemProvider(
    private val context: Context,
    private val safFileAccessProvider: SafFileAccessProvider
) : FileSystemProvider {
    override val id: FileSystemProviderId = FileSystemProviderId.Saf
    override val displayName: String = "授权目录"
    override val capabilities: FileSystemCapabilities = FileSystemCapabilities.Saf

    override suspend fun list(path: FileManagerPath): FileListResult = withContext(Dispatchers.IO) {
        val saf = path as? FileManagerPath.Saf ?: return@withContext FileListResult.Failed("不是 SAF 路径")
        safFileAccessProvider.listDirectory(saf.uri).fold(
            onSuccess = { FileListResult.Success(it) },
            onFailure = { FileListResult.Failed(it.message ?: "读取授权目录失败", it) }
        )
    }

    override suspend fun stat(path: FileManagerPath): FileStatResult = withContext(Dispatchers.IO) {
        val saf = path as? FileManagerPath.Saf ?: return@withContext FileStatResult.Failed("不是 SAF 路径")
        val document = safFileAccessProvider.documentFromUri(saf.uri) ?: safFileAccessProvider.treeFromUri(saf.uri)
        document?.let { FileStatResult.Success(safFileAccessProvider.buildEntry(it)) } ?: FileStatResult.Failed("授权文档不可用")
    }

    override suspend fun read(path: FileManagerPath): FileReadResult = withContext(Dispatchers.IO) {
        val saf = path as? FileManagerPath.Saf ?: return@withContext FileReadResult.Failed("不是 SAF 路径")
        runCatching { context.contentResolver.openInputStream(saf.uri)?.use { it.readBytes() } ?: error("无法打开输入流") }.fold(
            onSuccess = { FileReadResult.Success(it) },
            onFailure = { FileReadResult.Failed(it.message ?: "读取授权文件失败", it) }
        )
    }

    override suspend fun write(path: FileManagerPath, content: ByteArray): FileOperationResult = withContext(Dispatchers.IO) {
        val saf = path as? FileManagerPath.Saf ?: return@withContext FileOperationResult.Failed("不是 SAF 路径")
        runCatching { context.contentResolver.openOutputStream(saf.uri)?.use { it.write(content) } ?: error("无法打开输出流") }.fold(
            onSuccess = { FileOperationResult.Success },
            onFailure = { FileOperationResult.Failed(it.message ?: "写入授权文件失败", it) }
        )
    }

    override suspend fun createDirectory(path: FileManagerPath): FileOperationResult = FileOperationResult.Failed("请通过上层创建接口指定目录名称")
    override suspend fun rename(path: FileManagerPath, newName: String): FileOperationResult = withContext(Dispatchers.IO) {
        val saf = path as? FileManagerPath.Saf ?: return@withContext FileOperationResult.Failed("不是 SAF 路径")
        val document = safFileAccessProvider.documentFromUri(saf.uri) ?: return@withContext FileOperationResult.Failed("授权文档不可用")
        if (document.renameTo(newName)) FileOperationResult.Success else FileOperationResult.Failed("重命名失败")
    }
    override suspend fun delete(path: FileManagerPath): FileOperationResult = withContext(Dispatchers.IO) {
        val saf = path as? FileManagerPath.Saf ?: return@withContext FileOperationResult.Failed("不是 SAF 路径")
        val document = safFileAccessProvider.documentFromUri(saf.uri) ?: return@withContext FileOperationResult.Failed("授权文档不可用")
        if (document.delete()) FileOperationResult.Success else FileOperationResult.Failed("删除失败")
    }
}
