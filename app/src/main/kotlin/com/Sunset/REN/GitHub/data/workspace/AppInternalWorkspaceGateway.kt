package com.Sunset.REN.GitHub.data.workspace

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.Sunset.REN.GitHub.domain.workspace.SensitiveWorkspaceFile
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceFile
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceFileStatus
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceGateway
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceImportRequest
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceImportResult
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceImportSource
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceImportedFile
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceProject
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceRemoteBinding
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceScanOptions
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceScanResult
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceSkippedFile
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceSnapshot
import com.Sunset.REN.GitHub.domain.workspace.toNormalizedRepositoryPath
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * 最小 App 内部工作区实现。
 *
 * 元数据暂存 SharedPreferences + JSON，文件始终落在 App 私有 files/workspaces/repositories/{workspaceId}。
 * 该实现优先服务批量同步 MVP，后续可平滑迁移到 Room 或更完整的工作区数据库。
 */
class AppInternalWorkspaceGateway(
    context: Context,
    private val fileScanner: AppInternalWorkspaceFileScanner = AppInternalWorkspaceFileScanner(),
    private val ignoreMatcher: WorkspaceIgnoreMatcher = WorkspaceIgnoreMatcher.default(),
    private val sensitiveFileDetector: SensitiveFileDetector = SensitiveFileDetector.default()
) : WorkspaceGateway {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    private val workspacesRoot = File(appContext.filesDir, WorkspacesRootDirectory)
    private val repositoriesRoot = File(workspacesRoot, RepositoriesDirectory)

    override suspend fun listWorkspaces(): List<WorkspaceProject> {
        ensureRootDirectories()
        return readWorkspaceIndex().sortedByDescending { it.updatedAtMillis }
    }

    override suspend fun getWorkspace(workspaceId: String): WorkspaceProject? {
        ensureRootDirectories()
        return readWorkspaceIndex().firstOrNull { it.id == workspaceId }
    }

    override suspend fun createWorkspace(name: String): WorkspaceProject {
        ensureRootDirectories()
        val workspaceId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val root = workspaceRoot(workspaceId).also { it.mkdirs() }
        val workspace = WorkspaceProject(
            id = workspaceId,
            name = name.trim().takeIf { it.isNotBlank() } ?: "Workspace",
            rootPath = root.absolutePath,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        writeWorkspaceIndex(readWorkspaceIndex() + workspace)
        return workspace
    }

    override suspend fun renameWorkspace(workspaceId: String, name: String): WorkspaceProject {
        val updatedName = name.trim().takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("工作区名称不能为空。")
        return updateWorkspace(workspaceId) { workspace ->
            workspace.copy(name = updatedName, updatedAtMillis = System.currentTimeMillis())
        }
    }

    override suspend fun deleteWorkspace(workspaceId: String) {
        ensureRootDirectories()
        val remaining = readWorkspaceIndex().filterNot { it.id == workspaceId }
        writeWorkspaceIndex(remaining)
        preferences.edit { remove(snapshotKey(workspaceId)) }
        workspaceRoot(workspaceId).deleteRecursively()
    }

    override suspend fun bindRemote(workspaceId: String, binding: WorkspaceRemoteBinding): WorkspaceProject {
        return updateWorkspace(workspaceId) { workspace ->
            workspace.copy(remoteBinding = binding, updatedAtMillis = System.currentTimeMillis())
        }
    }

    override suspend fun clearRemoteBinding(workspaceId: String): WorkspaceProject {
        return updateWorkspace(workspaceId) { workspace ->
            workspace.copy(remoteBinding = null, updatedAtMillis = System.currentTimeMillis())
        }
    }

    override suspend fun importIntoWorkspace(request: WorkspaceImportRequest): WorkspaceImportResult {
        val workspace = getWorkspace(request.workspaceId)
            ?: throw IllegalArgumentException("工作区不存在：${request.workspaceId}")
        val root = File(workspace.rootPath).canonicalFile
        require(root.isDirectory) { "工作区目录不存在：${workspace.rootPath}" }

        val targetDirectory = request.targetDirectory.toNormalizedRepositoryPath()
        val importedFiles = mutableListOf<WorkspaceImportedFile>()
        val skippedFiles = mutableListOf<WorkspaceSkippedFile>()
        val sensitiveFiles = mutableListOf<SensitiveWorkspaceFile>()

        request.sources.forEach { source ->
            when (source) {
                is WorkspaceImportSource.AppInternalPath -> importInternalPath(
                    source = source,
                    root = root,
                    targetDirectory = targetDirectory,
                    request = request,
                    importedFiles = importedFiles,
                    skippedFiles = skippedFiles,
                    sensitiveFiles = sensitiveFiles
                )

                is WorkspaceImportSource.ContentUri -> importContentUri(
                    source = source,
                    root = root,
                    targetDirectory = targetDirectory,
                    request = request,
                    importedFiles = importedFiles,
                    skippedFiles = skippedFiles,
                    sensitiveFiles = sensitiveFiles
                )
            }
        }

        if (importedFiles.isNotEmpty()) {
            touchWorkspace(request.workspaceId)
        }

        return WorkspaceImportResult(
            workspaceId = request.workspaceId,
            importedFiles = importedFiles.sortedBy { it.relativePath },
            skippedFiles = skippedFiles,
            sensitiveFiles = sensitiveFiles.distinctBy { it.relativePath }.sortedBy { it.relativePath }
        )
    }

    override suspend fun scanWorkspace(
        workspaceId: String,
        options: WorkspaceScanOptions
    ): WorkspaceScanResult {
        val workspace = getWorkspace(workspaceId) ?: throw IllegalArgumentException("工作区不存在：$workspaceId")
        return fileScanner.scan(
            workspaceId = workspaceId,
            rootDirectory = File(workspace.rootPath),
            options = options
        )
    }

    override suspend fun getLatestSnapshot(workspaceId: String): WorkspaceSnapshot? {
        val raw = preferences.getString(snapshotKey(workspaceId), null).orEmpty()
        if (raw.isBlank()) return null
        return runCatching { JSONObject(raw).toWorkspaceSnapshot() }.getOrNull()
    }

    override suspend fun saveSnapshot(snapshot: WorkspaceSnapshot) {
        preferences.edit {
            putString(snapshotKey(snapshot.workspaceId), snapshot.toJson().toString())
        }
    }

    fun resolveWorkspaceRoot(workspaceId: String): File {
        return workspaceRoot(workspaceId).canonicalFile
    }

    private fun importInternalPath(
        source: WorkspaceImportSource.AppInternalPath,
        root: File,
        targetDirectory: String,
        request: WorkspaceImportRequest,
        importedFiles: MutableList<WorkspaceImportedFile>,
        skippedFiles: MutableList<WorkspaceSkippedFile>,
        sensitiveFiles: MutableList<SensitiveWorkspaceFile>
    ) {
        val sourceFile = File(source.absolutePath)
        if (!sourceFile.exists()) {
            skippedFiles += WorkspaceSkippedFile(source.absolutePath, "来源路径不存在。")
            return
        }
        if (sourceFile.isDirectory) {
            sourceFile.walkTopDown()
                .onEnter { directory ->
                    if (directory == sourceFile) return@onEnter true
                    val relative = sourceFile.toPath().relativize(directory.toPath()).toString().toNormalizedRepositoryPath()
                    shouldImportPath(relative, isDirectory = true, request = request, skippedFiles = skippedFiles)
                }
                .filter { it.isFile }
                .forEach { file ->
                    val relative = sourceFile.toPath().relativize(file.toPath()).toString().toNormalizedRepositoryPath()
                    importFileBytes(
                        sourceDescription = file.absolutePath,
                        sourceName = relative,
                        inputBytes = { file.readBytes() },
                        root = root,
                        targetDirectory = targetDirectory,
                        request = request,
                        importedFiles = importedFiles,
                        skippedFiles = skippedFiles,
                        sensitiveFiles = sensitiveFiles
                    )
                }
        } else {
            importFileBytes(
                sourceDescription = sourceFile.absolutePath,
                sourceName = sourceFile.name,
                inputBytes = { sourceFile.readBytes() },
                root = root,
                targetDirectory = targetDirectory,
                request = request,
                importedFiles = importedFiles,
                skippedFiles = skippedFiles,
                sensitiveFiles = sensitiveFiles
            )
        }
    }

    private fun importContentUri(
        source: WorkspaceImportSource.ContentUri,
        root: File,
        targetDirectory: String,
        request: WorkspaceImportRequest,
        importedFiles: MutableList<WorkspaceImportedFile>,
        skippedFiles: MutableList<WorkspaceSkippedFile>,
        sensitiveFiles: MutableList<SensitiveWorkspaceFile>
    ) {
        val displayName = source.displayName?.takeIf { it.isNotBlank() }
            ?: Uri.parse(source.uri).lastPathSegment?.substringAfterLast('/')
            ?: "imported-file"
        importFileBytes(
            sourceDescription = source.uri,
            sourceName = displayName,
            inputBytes = {
                appContext.contentResolver.openInputStream(Uri.parse(source.uri))?.use { input ->
                    input.readBytes()
                } ?: throw IllegalArgumentException("无法打开导入来源：${source.uri}")
            },
            root = root,
            targetDirectory = targetDirectory,
            request = request,
            importedFiles = importedFiles,
            skippedFiles = skippedFiles,
            sensitiveFiles = sensitiveFiles
        )
    }

    private fun importFileBytes(
        sourceDescription: String,
        sourceName: String,
        inputBytes: () -> ByteArray,
        root: File,
        targetDirectory: String,
        request: WorkspaceImportRequest,
        importedFiles: MutableList<WorkspaceImportedFile>,
        skippedFiles: MutableList<WorkspaceSkippedFile>,
        sensitiveFiles: MutableList<SensitiveWorkspaceFile>
    ) {
        val relativeName = if (request.options.preserveDirectoryStructure) {
            sourceName.toNormalizedRepositoryPath()
        } else {
            sourceName.substringAfterLast('/').toNormalizedRepositoryPath()
        }
        if (!shouldImportPath(relativeName, isDirectory = false, request = request, skippedFiles = skippedFiles)) return

        val relativePath = listOf(targetDirectory, relativeName)
            .filter { it.isNotBlank() }
            .joinToString("/")
            .toNormalizedRepositoryPath()
        if (request.options.detectSensitiveFiles) {
            sensitiveFileDetector.detect(relativePath)?.let { sensitiveFiles += it }
        }

        val targetFile = root.resolveInsideWorkspace(relativePath)
        if (targetFile.exists() && !request.options.overwriteExisting) {
            skippedFiles += WorkspaceSkippedFile(sourceDescription, "目标文件已存在：$relativePath")
            return
        }
        val bytes = inputBytes()
        targetFile.parentFile?.mkdirs()
        targetFile.writeBytes(bytes)
        importedFiles += WorkspaceImportedFile(
            sourceDescription = sourceDescription,
            relativePath = relativePath,
            sizeBytes = bytes.size.toLong()
        )
    }

    private fun shouldImportPath(
        relativePath: String,
        isDirectory: Boolean,
        request: WorkspaceImportRequest,
        skippedFiles: MutableList<WorkspaceSkippedFile>
    ): Boolean {
        val normalized = relativePath.toNormalizedRepositoryPath()
        if (request.options.skipHiddenFiles && normalized.split('/').any { it.startsWith('.') }) {
            skippedFiles += WorkspaceSkippedFile(normalized, "已跳过隐藏文件或隐藏目录。")
            return false
        }
        if (request.options.applyIgnoreRules && ignoreMatcher.shouldIgnore(normalized, isDirectory = isDirectory)) {
            skippedFiles += WorkspaceSkippedFile(normalized, "已按默认忽略规则跳过。")
            return false
        }
        return true
    }

    private fun updateWorkspace(workspaceId: String, transform: (WorkspaceProject) -> WorkspaceProject): WorkspaceProject {
        ensureRootDirectories()
        val workspaces = readWorkspaceIndex()
        var updated: WorkspaceProject? = null
        val next = workspaces.map { workspace ->
            if (workspace.id == workspaceId) {
                transform(workspace).also { updated = it }
            } else {
                workspace
            }
        }
        val result = updated ?: throw IllegalArgumentException("工作区不存在：$workspaceId")
        writeWorkspaceIndex(next)
        return result
    }

    private fun touchWorkspace(workspaceId: String) {
        updateWorkspace(workspaceId) { workspace ->
            workspace.copy(updatedAtMillis = System.currentTimeMillis())
        }
    }

    private fun readWorkspaceIndex(): List<WorkspaceProject> {
        val raw = preferences.getString(KeyWorkspaces, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.toWorkspaceProject()?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeWorkspaceIndex(workspaces: List<WorkspaceProject>) {
        preferences.edit {
            putString(KeyWorkspaces, workspaces.toJsonArray().toString())
        }
    }

    private fun ensureRootDirectories() {
        repositoriesRoot.mkdirs()
    }

    private fun workspaceRoot(workspaceId: String): File {
        return File(repositoriesRoot, workspaceId)
    }

    private fun snapshotKey(workspaceId: String): String = "$KeySnapshotPrefix$workspaceId"

    private fun File.resolveInsideWorkspace(relativePath: String): File {
        val root = canonicalFile
        val target = relativePath
            .toNormalizedRepositoryPath()
            .split('/')
            .filter { it.isNotBlank() }
            .fold(root) { current, segment -> current.resolve(segment) }
            .canonicalFile
        require(target.path == root.path || target.path.startsWith(root.path + File.separator)) {
            "导入目标越过工作区边界：$relativePath"
        }
        return target
    }

    private companion object {
        const val PreferencesName = "app_internal_workspace_preferences"
        const val WorkspacesRootDirectory = "workspaces"
        const val RepositoriesDirectory = "repositories"
        const val KeyWorkspaces = "workspaces"
        const val KeySnapshotPrefix = "snapshot:"
    }
}

private fun WorkspaceProject.toJson(): JSONObject {
    return JSONObject()
        .put(KeyWorkspaceId, id)
        .put(KeyWorkspaceName, name)
        .put(KeyWorkspaceRootPath, rootPath)
        .put(KeyWorkspaceCreatedAtMillis, createdAtMillis)
        .put(KeyWorkspaceUpdatedAtMillis, updatedAtMillis)
        .apply {
            remoteBinding?.let { put(KeyWorkspaceRemoteBinding, it.toJson()) }
        }
}

private fun JSONObject.toWorkspaceProject(): WorkspaceProject {
    return WorkspaceProject(
        id = optString(KeyWorkspaceId),
        name = optString(KeyWorkspaceName, "Workspace"),
        rootPath = optString(KeyWorkspaceRootPath),
        createdAtMillis = optLong(KeyWorkspaceCreatedAtMillis, 0L),
        updatedAtMillis = optLong(KeyWorkspaceUpdatedAtMillis, 0L),
        remoteBinding = optJSONObject(KeyWorkspaceRemoteBinding)?.toWorkspaceRemoteBinding()
    )
}

private fun WorkspaceRemoteBinding.toJson(): JSONObject {
    return JSONObject()
        .put(KeyRemoteOwner, owner)
        .put(KeyRemoteRepo, repo)
        .put(KeyRemoteBranch, branch)
        .put(KeyRemotePath, remotePath)
}

private fun JSONObject.toWorkspaceRemoteBinding(): WorkspaceRemoteBinding {
    return WorkspaceRemoteBinding(
        owner = optString(KeyRemoteOwner),
        repo = optString(KeyRemoteRepo),
        branch = optString(KeyRemoteBranch),
        remotePath = optString(KeyRemotePath, "")
    )
}

private fun List<WorkspaceProject>.toJsonArray(): JSONArray {
    return JSONArray().also { array -> forEach { array.put(it.toJson()) } }
}

private fun WorkspaceSnapshot.toJson(): JSONObject {
    return JSONObject()
        .put(KeySnapshotWorkspaceId, workspaceId)
        .put(KeySnapshotCreatedAtMillis, createdAtMillis)
        .put(KeySnapshotFiles, files.toWorkspaceFilesJsonArray())
}

private fun JSONObject.toWorkspaceSnapshot(): WorkspaceSnapshot {
    return WorkspaceSnapshot(
        workspaceId = optString(KeySnapshotWorkspaceId),
        createdAtMillis = optLong(KeySnapshotCreatedAtMillis, 0L),
        files = optJSONArray(KeySnapshotFiles)?.toWorkspaceFiles().orEmpty()
    )
}

private fun List<WorkspaceFile>.toWorkspaceFilesJsonArray(): JSONArray {
    return JSONArray().also { array ->
        forEach { file ->
            array.put(
                JSONObject()
                    .put(KeyFileWorkspaceId, file.workspaceId)
                    .put(KeyFileRelativePath, file.relativePath)
                    .put(KeyFileSizeBytes, file.sizeBytes)
                    .put(KeyFileLastModifiedMillis, file.lastModifiedMillis)
                    .put(KeyFileSha256, file.sha256)
                    .put(KeyFileStatus, file.status.name)
            )
        }
    }
}

private fun JSONArray.toWorkspaceFiles(): List<WorkspaceFile> {
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let { json ->
                add(
                    WorkspaceFile(
                        workspaceId = json.optString(KeyFileWorkspaceId),
                        relativePath = json.optString(KeyFileRelativePath),
                        sizeBytes = json.optLong(KeyFileSizeBytes, 0L),
                        lastModifiedMillis = json.optLong(KeyFileLastModifiedMillis, 0L),
                        sha256 = json.optString(KeyFileSha256),
                        status = runCatching {
                            WorkspaceFileStatus.valueOf(json.optString(KeyFileStatus, WorkspaceFileStatus.Unchanged.name))
                        }.getOrDefault(WorkspaceFileStatus.Unchanged)
                    )
                )
            }
        }
    }
}

private const val KeyWorkspaceId = "id"
private const val KeyWorkspaceName = "name"
private const val KeyWorkspaceRootPath = "root_path"
private const val KeyWorkspaceCreatedAtMillis = "created_at_millis"
private const val KeyWorkspaceUpdatedAtMillis = "updated_at_millis"
private const val KeyWorkspaceRemoteBinding = "remote_binding"
private const val KeyRemoteOwner = "owner"
private const val KeyRemoteRepo = "repo"
private const val KeyRemoteBranch = "branch"
private const val KeyRemotePath = "remote_path"
private const val KeySnapshotWorkspaceId = "workspace_id"
private const val KeySnapshotCreatedAtMillis = "created_at_millis"
private const val KeySnapshotFiles = "files"
private const val KeyFileWorkspaceId = "workspace_id"
private const val KeyFileRelativePath = "relative_path"
private const val KeyFileSizeBytes = "size_bytes"
private const val KeyFileLastModifiedMillis = "last_modified_millis"
private const val KeyFileSha256 = "sha256"
private const val KeyFileStatus = "status"