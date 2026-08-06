package com.Sunset.REN.GitHub.data.filemanager.provider

import com.Sunset.REN.GitHub.domain.filemanager.FileEntryCapabilities
import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryTypeResolver
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntrySorter
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
import java.util.zip.ZipFile

class ArchiveFileSystemProvider : FileSystemProvider {
    override val id: FileSystemProviderId = FileSystemProviderId.Archive
    override val displayName: String = "压缩包"
    override val capabilities: FileSystemCapabilities = FileSystemCapabilities.Archive

    override suspend fun list(path: FileManagerPath): FileListResult = withContext(Dispatchers.IO) {
        val archive = path as? FileManagerPath.Archive ?: return@withContext FileListResult.Failed("不是压缩包路径")
        runCatching { listZip(File(archive.archivePath), archive.innerPath) }.fold(
            onSuccess = { FileListResult.Success(it) },
            onFailure = { FileListResult.Failed(it.message ?: "读取压缩包失败", it) }
        )
    }

    override suspend fun stat(path: FileManagerPath): FileStatResult = FileStatResult.Failed("压缩包 stat 暂未实现")
    override suspend fun read(path: FileManagerPath): FileReadResult = withContext(Dispatchers.IO) {
        val archive = path as? FileManagerPath.Archive ?: return@withContext FileReadResult.Failed("不是压缩包路径")
        runCatching {
            ZipFile(File(archive.archivePath)).use { zip ->
                val entry = zip.getEntry(archive.innerPath.trim('/')) ?: error("压缩包条目不存在")
                zip.getInputStream(entry).use { it.readBytes() }
            }
        }.fold(
            onSuccess = { FileReadResult.Success(it) },
            onFailure = { FileReadResult.Failed(it.message ?: "读取压缩包条目失败", it) }
        )
    }
    override suspend fun write(path: FileManagerPath, content: ByteArray): FileOperationResult = FileOperationResult.Failed("压缩包浏览模式不可写入")
    override suspend fun createDirectory(path: FileManagerPath): FileOperationResult = FileOperationResult.Failed("压缩包浏览模式不可新建")
    override suspend fun rename(path: FileManagerPath, newName: String): FileOperationResult = FileOperationResult.Failed("压缩包浏览模式不可重命名")
    override suspend fun delete(path: FileManagerPath): FileOperationResult = FileOperationResult.Failed("压缩包浏览模式不可删除")

    private fun listZip(archiveFile: File, innerPath: String): List<FileManagerEntry> {
        val normalizedInnerPath = innerPath.trim('/')
        val children = linkedMapOf<String, ArchiveChild>()
        ZipFile(archiveFile).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name.trim('/')
                if (name.isBlank()) continue
                val relative = if (normalizedInnerPath.isBlank()) name else name.removePrefix("$normalizedInnerPath/").takeIf { it != name } ?: continue
                val first = relative.substringBefore('/')
                val childInner = if (normalizedInnerPath.isBlank()) first else "$normalizedInnerPath/$first"
                val isDir = relative.contains('/') || entry.isDirectory
                children[first] = ArchiveChild(first, childInner, isDir, if (isDir) null else entry.size.takeIf { it >= 0 }, entry.time.takeIf { it > 0 })
            }
        }
        return FileManagerEntrySorter.sort(children.values.map { child ->
            val type = FileEntryTypeResolver.resolve(name = child.name, isDirectory = child.isDirectory)
            FileManagerEntry(
                id = "${archiveFile.absolutePath}!/${child.innerPath}",
                name = child.name,
                displayPath = "${archiveFile.absolutePath}!/${child.innerPath}",
                type = type,
                source = FileEntrySource.ArchiveEntry(archiveFile, child.innerPath, child.isDirectory),
                sizeBytes = child.sizeBytes,
                modifiedAtMillis = child.modifiedAtMillis,
                capabilities = FileEntryCapabilities(true, false, false, false, false, false, !child.isDirectory, false)
            )
        })
    }

    private data class ArchiveChild(val name: String, val innerPath: String, val isDirectory: Boolean, val sizeBytes: Long?, val modifiedAtMillis: Long?)
}
