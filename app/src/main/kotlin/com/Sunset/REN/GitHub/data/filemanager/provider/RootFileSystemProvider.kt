package com.Sunset.REN.GitHub.data.filemanager.provider

import com.Sunset.REN.GitHub.domain.filemanager.provider.FileListResult
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileOperationResult
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileReadResult
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileStatResult
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileSystemCapabilities
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileSystemProvider
import com.Sunset.REN.GitHub.domain.filemanager.path.FileManagerPath
import com.Sunset.REN.GitHub.domain.filemanager.path.FileSystemProviderId
import com.Sunset.REN.GitHub.domain.filemanager.root.RootAccessManager
import com.Sunset.REN.GitHub.domain.filemanager.root.RootAccessState
import com.Sunset.REN.GitHub.domain.filemanager.root.RootCommandRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

class RootFileSystemProvider(
    private val rootAccessManager: RootAccessManager,
    private val runner: RootCommandRunner = RootCommandRunner()
) : FileSystemProvider {
    override val id: FileSystemProviderId = FileSystemProviderId.Root
    override val displayName: String = "Root 文件系统"
    override val capabilities: FileSystemCapabilities
        get() = if (rootAccessManager.currentState() is RootAccessState.Granted) FileSystemCapabilities.RootGranted else FileSystemCapabilities.RootUnavailable

    override suspend fun list(path: FileManagerPath): FileListResult = withContext(Dispatchers.IO) {
        val root = path as? FileManagerPath.Root ?: return@withContext FileListResult.Failed("不是 Root 路径")
        if (rootAccessManager.currentState() !is RootAccessState.Granted) return@withContext FileListResult.Failed("Root 未授权")
        val result = runner.run(runner.buildCommand("ls", "-laL", "--", root.absolutePath), timeoutMillis = 8000L)
            .recoverCatching {
                runner.run(runner.buildCommand("ls", "-la", "--", root.absolutePath), timeoutMillis = 8000L).getOrThrow()
            }
        result.fold(
            onSuccess = { output -> FileListResult.Success(RootDirectoryListingParser.parseDirectory(root.absolutePath, output)) },
            onFailure = { FileListResult.Failed(it.message ?: "Root 读取目录失败", it) }
        )
    }

    fun listBlocking(path: FileManagerPath.Root): FileListResult {
        if (rootAccessManager.currentState() !is RootAccessState.Granted) return FileListResult.Failed("Root 未授权")
        return runner.runBlocking(runner.buildCommand("ls", "-la", "--", path.absolutePath), timeoutMillis = 8000L).fold(
            onSuccess = { output -> FileListResult.Success(RootDirectoryListingParser.parseDirectory(path.absolutePath, output)) },
            onFailure = { FileListResult.Failed(it.message ?: "Root 读取目录失败", it) }
        )
    }

    override suspend fun stat(path: FileManagerPath): FileStatResult = withContext(Dispatchers.IO) {
        val root = path as? FileManagerPath.Root ?: return@withContext FileStatResult.Failed("不是 Root 路径")
        if (rootAccessManager.currentState() !is RootAccessState.Granted) return@withContext FileStatResult.Failed("Root 未授权")
        runner.run(runner.buildCommand("ls", "-ld", "--", root.absolutePath), timeoutMillis = 8000L).fold(
            onSuccess = { output ->
                RootDirectoryListingParser.parseStat(root.absolutePath, output)
                    ?.let(FileStatResult::Success)
                    ?: FileStatResult.Failed("Root stat 解析失败")
            },
            onFailure = { FileStatResult.Failed(it.message ?: "Root stat 失败", it) }
        )
    }
    override suspend fun read(path: FileManagerPath): FileReadResult = withContext(Dispatchers.IO) {
        val root = path as? FileManagerPath.Root ?: return@withContext FileReadResult.Failed("不是 Root 路径")
        if (rootAccessManager.currentState() !is RootAccessState.Granted) return@withContext FileReadResult.Failed("Root 未授权")
        val stat = stat(root)
        val entry = (stat as? FileStatResult.Success)?.entry ?: return@withContext FileReadResult.Failed((stat as? FileStatResult.Failed)?.message ?: "Root stat 失败")
        if ((entry.source as? com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource.RootPath)?.isDirectory == true) return@withContext FileReadResult.Failed("Root read 不支持目录")
        val size = entry.sizeBytes ?: return@withContext FileReadResult.Failed("Root read 需要文件大小信息")
        if (size > MaxReadBytes) return@withContext FileReadResult.Failed("Root read 仅开放 ${MaxReadBytes / 1024}KB 以内的小文件")
        runner.run(runner.buildCommand("dd", "if=${root.absolutePath}", "bs=1", "count=${min(size, MaxReadBytes).toInt()}"), timeoutMillis = 8000L).fold(
            onSuccess = { output -> FileReadResult.Success(output.toByteArray()) },
            onFailure = { FileReadResult.Failed(it.message ?: "Root read 失败", it) }
        )
    }

    override suspend fun write(path: FileManagerPath, content: ByteArray): FileOperationResult = FileOperationResult.Failed("Root 写入暂未开放")
    override suspend fun createDirectory(path: FileManagerPath): FileOperationResult = FileOperationResult.Failed("Root 新建暂未开放")
    override suspend fun rename(path: FileManagerPath, newName: String): FileOperationResult = FileOperationResult.Failed("Root 重命名暂未开放")
    override suspend fun delete(path: FileManagerPath): FileOperationResult = FileOperationResult.Failed("Root 删除暂未开放")

    private companion object {
        const val MaxReadBytes: Long = 64 * 1024
    }
}