package com.Sunset.REN.GitHub.data.github

import com.Sunset.REN.GitHub.data.workspace.AppInternalWorkspaceFileScanner
import com.Sunset.REN.GitHub.domain.sync.RemoteRepositoryCommitBackend
import com.Sunset.REN.GitHub.domain.sync.RemoteTreeEntry
import com.Sunset.REN.GitHub.domain.sync.RemoteTreeEntryMode
import com.Sunset.REN.GitHub.domain.sync.RemoteTreeEntryType
import com.Sunset.REN.GitHub.domain.sync.RemoteTreeEntryWrite
import com.Sunset.REN.GitHub.domain.sync.WorkspaceSyncBackend
import com.Sunset.REN.GitHub.domain.sync.WorkspaceSyncBackendCapabilities
import com.Sunset.REN.GitHub.domain.sync.WorkspaceSyncConflict
import com.Sunset.REN.GitHub.domain.sync.WorkspaceSyncConflictType
import com.Sunset.REN.GitHub.domain.sync.WorkspaceSyncExecutionStrategy
import com.Sunset.REN.GitHub.domain.sync.WorkspaceSyncMode
import com.Sunset.REN.GitHub.domain.sync.WorkspaceSyncOperation
import com.Sunset.REN.GitHub.domain.sync.WorkspaceSyncOperationFailure
import com.Sunset.REN.GitHub.domain.sync.WorkspaceSyncPhase
import com.Sunset.REN.GitHub.domain.sync.WorkspaceSyncPlan
import com.Sunset.REN.GitHub.domain.sync.WorkspaceSyncProgress
import com.Sunset.REN.GitHub.domain.sync.WorkspaceSyncRequest
import com.Sunset.REN.GitHub.domain.sync.WorkspaceSyncResult
import com.Sunset.REN.GitHub.domain.workspace.SensitiveFileSeverity
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceFile
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceFileStatus
import com.Sunset.REN.GitHub.domain.workspace.toNormalizedRepositoryPath
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.UUID

/**
 * GitHub Git Data API 工作区同步后端。
 *
 * 第一阶段聚焦 UploadOnly 与 MirrorRemotePath：
 * - UploadOnly：本地存在的文件新增/覆盖到远端，不删除远端多余文件。
 * - MirrorRemotePath：以本地工作区为准镜像 remotePath，可删除远端多余文件。
 */
class GitHubApiWorkspaceSyncBackend(
    private val workspaceRootResolver: suspend (workspaceId: String) -> File,
    private val remoteCommitBackend: RemoteRepositoryCommitBackend,
    private val fileScanner: AppInternalWorkspaceFileScanner = AppInternalWorkspaceFileScanner()
) : WorkspaceSyncBackend {
    override val id: String = "github_git_data_api"
    override val displayName: String = "GitHub Git Data API"
    override val capabilities: WorkspaceSyncBackendCapabilities = WorkspaceSyncBackendCapabilities(
        supportsIncrementalUpload = true,
        supportsMirrorRemotePath = true,
        supportsRemoteDeletes = true,
        supportsConflictDetection = true,
        supportsDryRun = true,
        maxRecommendedFilesPerCommit = RecommendedMaxFilesPerCommit
    )

    override suspend fun buildPlan(request: WorkspaceSyncRequest): WorkspaceSyncPlan {
        val rootDirectory = workspaceRootResolver(request.workspaceId).canonicalFile
        val scanResult = fileScanner.scan(
            workspaceId = request.workspaceId,
            rootDirectory = rootDirectory,
            options = request.options.toWorkspaceScanOptions()
        )
        val localFiles = scanResult.files
            .filter { request.options.includeIgnored || it.status != WorkspaceFileStatus.Ignored }
            .associateBy { it.relativePath.toNormalizedRepositoryPath() }

        val branchHead = remoteCommitBackend.getBranchHead(
            owner = request.remote.owner,
            repo = request.remote.repo,
            branch = request.remote.branch
        )
        val baseCommit = remoteCommitBackend.getCommit(
            owner = request.remote.owner,
            repo = request.remote.repo,
            commitSha = branchHead.commitSha
        )
        val baseTree = remoteCommitBackend.getTree(
            owner = request.remote.owner,
            repo = request.remote.repo,
            treeSha = baseCommit.treeSha,
            recursive = true
        )
        val remoteFiles = baseTree.entries
            .filter { it.type == RemoteTreeEntryType.Blob }
            .mapNotNull { entry -> entry.toScopedRemoteFile(request.remote.normalizedRemotePath) }
            .associateBy { it.relativePath }

        val operations = mutableListOf<WorkspaceSyncOperation>()
        val conflicts = mutableListOf<WorkspaceSyncConflict>()

        localFiles.values.forEach { localFile ->
            val relativePath = localFile.relativePath.toNormalizedRepositoryPath()
            val remotePath = buildRemotePath(request.remote.normalizedRemotePath, relativePath)
            val remoteFile = remoteFiles[relativePath]
            val localGitBlobSha = rootDirectory.resolveRepositoryFile(relativePath).gitBlobSha1()
            when {
                remoteFile == null -> operations += WorkspaceSyncOperation.Add(
                    operationId = buildOperationId("add", relativePath),
                    relativePath = relativePath,
                    remotePath = remotePath,
                    sizeBytes = localFile.sizeBytes,
                    sha256 = localFile.sha256
                )

                remoteFile.entry.sha != localGitBlobSha -> operations += WorkspaceSyncOperation.Modify(
                    operationId = buildOperationId("modify", relativePath),
                    relativePath = relativePath,
                    remotePath = remotePath,
                    sizeBytes = localFile.sizeBytes,
                    sha256 = localFile.sha256,
                    remoteSha = remoteFile.entry.sha
                )
            }
        }

        if (request.mode == WorkspaceSyncMode.MirrorRemotePath) {
            remoteFiles.values
                .filter { remoteFile -> localFiles[remoteFile.relativePath] == null }
                .forEach { remoteFile ->
                    if (request.options.allowDeletes) {
                        operations += WorkspaceSyncOperation.Delete(
                            operationId = buildOperationId("delete", remoteFile.relativePath),
                            relativePath = remoteFile.relativePath,
                            remotePath = remoteFile.entry.path,
                            remoteSha = remoteFile.entry.sha
                        )
                    } else {
                        conflicts += WorkspaceSyncConflict(
                            relativePath = remoteFile.relativePath,
                            remotePath = remoteFile.entry.path,
                            type = WorkspaceSyncConflictType.DeleteWouldRemoveRemoteFile,
                            message = "远端文件不在本地工作区中，镜像同步会删除它：${remoteFile.entry.path}"
                        )
                    }
                }
        }

        val warnings = buildList {
            if (baseTree.truncated) {
                add("GitHub 返回的递归 tree 被截断，计划可能不完整；建议缩小 remotePath 或分批同步。")
            }
            if (request.mode == WorkspaceSyncMode.Incremental) {
                add("当前 GitHub API 后端将 Incremental 按 UploadOnly 处理；后续接入快照后再做精确增量。")
            }
            request.options.maxFilesPerCommit?.let { maxFiles ->
                if (operations.size > maxFiles) {
                    add("本次计划包含 ${operations.size} 个操作，超过 maxFilesPerCommit=$maxFiles；当前后端暂未自动拆分提交。")
                }
            }
        }

        return WorkspaceSyncPlan(
            planId = UUID.randomUUID().toString(),
            workspaceId = request.workspaceId,
            remote = request.remote,
            baseRevision = branchHead.commitSha,
            createdAtMillis = System.currentTimeMillis(),
            commitMessage = request.commitMessage,
            mode = request.mode,
            options = request.options,
            operations = operations.sortedBy { it.remotePath },
            conflicts = conflicts.sortedBy { it.remotePath },
            sensitiveFiles = scanResult.sensitivePaths,
            warnings = warnings
        )
    }

    override suspend fun executePlan(
        plan: WorkspaceSyncPlan,
        progress: suspend (WorkspaceSyncProgress) -> Unit
    ): WorkspaceSyncResult {
        progress(WorkspaceSyncProgress(WorkspaceSyncPhase.Preparing, 0, plan.operations.size, "准备执行同步计划"))

        if (plan.options.blockOnSensitiveFiles && plan.sensitiveFiles.any { it.severity == SensitiveFileSeverity.Blocking }) {
            throw IllegalStateException("检测到阻断级敏感文件，请移除或调整规则后再同步。")
        }
        if (plan.hasBlockingIssues) {
            throw IllegalStateException("同步计划存在冲突或阻断问题，请先处理后再执行。")
        }
        if (plan.requiresDestructiveConfirmation && !plan.options.destructiveOperationConfirmed) {
            throw IllegalStateException("同步计划包含删除/镜像类危险操作，需要 UI 明确二次确认。")
        }
        if (plan.options.dryRun) {
            progress(WorkspaceSyncProgress(WorkspaceSyncPhase.Completed, 0, plan.operations.size, "Dry run 完成，未写入远端。"))
            return WorkspaceSyncResult(
                planId = plan.planId,
                workspaceId = plan.workspaceId,
                remote = plan.remote,
                commitHash = null,
                appliedOperations = emptyList(),
                skippedOperations = plan.operations,
                completedAtMillis = System.currentTimeMillis()
            )
        }
        if (plan.operations.isEmpty()) {
            progress(WorkspaceSyncProgress(WorkspaceSyncPhase.Completed, 0, 0, "没有需要同步的变更。"))
            return WorkspaceSyncResult(
                planId = plan.planId,
                workspaceId = plan.workspaceId,
                remote = plan.remote,
                commitHash = null,
                appliedOperations = emptyList(),
                completedAtMillis = System.currentTimeMillis()
            )
        }

        val branchHead = remoteCommitBackend.getBranchHead(plan.remote.owner, plan.remote.repo, plan.remote.branch)
        val executionBase = WorkspaceSyncExecutionStrategy.resolve(
            planBaseRevision = plan.baseRevision,
            currentHeadSha = branchHead.commitSha,
            allowOverwriteRemoteChanges = plan.options.allowOverwriteRemoteChanges
        )
        if (executionBase.overwritingRemoteChanges) {
            progress(
                WorkspaceSyncProgress(
                    WorkspaceSyncPhase.Preparing,
                    0,
                    plan.operations.size,
                    "远端分支已变化，将以本地计划覆盖远端同路径内容。"
                )
            )
        }
        val baseCommit = remoteCommitBackend.getCommit(plan.remote.owner, plan.remote.repo, executionBase.treeBaseCommitSha)
        val rootDirectory = workspaceRootResolver(plan.workspaceId).canonicalFile
        val treeEntries = mutableListOf<RemoteTreeEntryWrite>()
        val failedOperations = mutableListOf<WorkspaceSyncOperationFailure>()

        plan.operations.forEachIndexed { index, operation ->
            progress(
                WorkspaceSyncProgress(
                    phase = WorkspaceSyncPhase.UploadingObjects,
                    completedOperations = index,
                    totalOperations = plan.operations.size,
                    message = "准备 ${operation.remotePath}"
                )
            )
            runCatching {
                operation.toRemoteTreeEntryWrite(rootDirectory, plan.remote.owner, plan.remote.repo)
            }.onSuccess { entry ->
                treeEntries += entry
                progress(
                    WorkspaceSyncProgress(
                        phase = WorkspaceSyncPhase.UploadingObjects,
                        completedOperations = index + 1,
                        totalOperations = plan.operations.size,
                        message = "已处理 ${operation.remotePath}"
                    )
                )
            }.onFailure { error ->
                failedOperations += WorkspaceSyncOperationFailure(
                    operation = operation,
                    reason = error.message ?: error::class.java.simpleName,
                    recoverable = true
                )
            }
        }

        if (failedOperations.isNotEmpty()) {
            progress(WorkspaceSyncProgress(WorkspaceSyncPhase.Failed, treeEntries.size, plan.operations.size, "部分文件处理失败，未创建提交。"))
            return WorkspaceSyncResult(
                planId = plan.planId,
                workspaceId = plan.workspaceId,
                remote = plan.remote,
                commitHash = null,
                appliedOperations = emptyList(),
                failedOperations = failedOperations,
                completedAtMillis = System.currentTimeMillis()
            )
        }

        progress(WorkspaceSyncProgress(WorkspaceSyncPhase.CreatingCommit, plan.operations.size, plan.operations.size, "创建 Git tree"))
        val newTree = remoteCommitBackend.createTree(
            owner = plan.remote.owner,
            repo = plan.remote.repo,
            baseTreeSha = baseCommit.treeSha,
            entries = treeEntries
        )

        progress(WorkspaceSyncProgress(WorkspaceSyncPhase.CreatingCommit, plan.operations.size, plan.operations.size, "创建 Git commit"))
        val newCommit = remoteCommitBackend.createCommit(
            owner = plan.remote.owner,
            repo = plan.remote.repo,
            message = planCommitMessage(plan),
            treeSha = newTree.sha,
            parentCommitSha = executionBase.commitParentSha
        )

        progress(WorkspaceSyncProgress(WorkspaceSyncPhase.UpdatingReference, plan.operations.size, plan.operations.size, "更新分支 ${plan.remote.branch}"))
        remoteCommitBackend.updateBranchHead(
            owner = plan.remote.owner,
            repo = plan.remote.repo,
            branch = plan.remote.branch,
            commitSha = newCommit.sha,
            force = executionBase.forceUpdate
        )

        progress(WorkspaceSyncProgress(WorkspaceSyncPhase.Completed, plan.operations.size, plan.operations.size, "同步完成：${newCommit.sha}"))
        return WorkspaceSyncResult(
            planId = plan.planId,
            workspaceId = plan.workspaceId,
            remote = plan.remote,
            commitHash = newCommit.sha,
            appliedOperations = plan.operations,
            completedAtMillis = System.currentTimeMillis()
        )
    }

    private suspend fun WorkspaceSyncOperation.toRemoteTreeEntryWrite(
        rootDirectory: File,
        owner: String,
        repo: String
    ): RemoteTreeEntryWrite {
        return when (this) {
            is WorkspaceSyncOperation.Add -> toBlobTreeEntry(rootDirectory, owner, repo)
            is WorkspaceSyncOperation.Modify -> toBlobTreeEntry(rootDirectory, owner, repo)
            is WorkspaceSyncOperation.Delete -> RemoteTreeEntryWrite(
                path = remotePath,
                mode = RemoteTreeEntryMode.File,
                type = RemoteTreeEntryType.Blob,
                sha = null
            )
        }
    }

    private suspend fun WorkspaceSyncOperation.toBlobTreeEntry(
        rootDirectory: File,
        owner: String,
        repo: String
    ): RemoteTreeEntryWrite {
        val file = rootDirectory.resolveRepositoryFile(relativePath)
        val blob = remoteCommitBackend.createBlob(owner = owner, repo = repo, contentBytes = file.readBytes())
        return RemoteTreeEntryWrite(
            path = remotePath,
            mode = RemoteTreeEntryMode.File,
            type = RemoteTreeEntryType.Blob,
            sha = blob.sha
        )
    }

    private fun RemoteTreeEntry.toScopedRemoteFile(remotePathPrefix: String): ScopedRemoteFile? {
        val normalizedPath = path.toNormalizedRepositoryPath()
        val normalizedPrefix = remotePathPrefix.toNormalizedRepositoryPath()
        val relativePath = when {
            normalizedPrefix.isBlank() -> normalizedPath
            normalizedPath == normalizedPrefix -> return null
            normalizedPath.startsWith("$normalizedPrefix/") -> normalizedPath.removePrefix("$normalizedPrefix/")
            else -> return null
        }
        return ScopedRemoteFile(relativePath = relativePath, entry = this)
    }

    private fun buildRemotePath(remotePathPrefix: String, relativePath: String): String {
        val prefix = remotePathPrefix.toNormalizedRepositoryPath()
        val normalizedRelativePath = relativePath.toNormalizedRepositoryPath()
        return listOf(prefix, normalizedRelativePath)
            .filter { it.isNotBlank() }
            .joinToString("/")
    }

    private fun buildOperationId(type: String, relativePath: String): String {
        return "$type:${relativePath.toNormalizedRepositoryPath()}"
    }

    private fun planCommitMessage(plan: WorkspaceSyncPlan): String {
        return plan.commitMessage.takeIf { it.isNotBlank() } ?: when (plan.mode) {
            WorkspaceSyncMode.MirrorRemotePath -> "Mirror workspace to ${plan.remote.repositoryFullName}"
            WorkspaceSyncMode.UploadOnly -> "Upload workspace to ${plan.remote.repositoryFullName}"
            WorkspaceSyncMode.Incremental -> "Sync workspace to ${plan.remote.repositoryFullName}"
        }
    }

    private data class ScopedRemoteFile(
        val relativePath: String,
        val entry: RemoteTreeEntry
    )

    private companion object {
        const val RecommendedMaxFilesPerCommit = 500
    }
}

private fun com.Sunset.REN.GitHub.domain.sync.WorkspaceSyncOptions.toWorkspaceScanOptions(): com.Sunset.REN.GitHub.domain.workspace.WorkspaceScanOptions {
    return com.Sunset.REN.GitHub.domain.workspace.WorkspaceScanOptions(
        includeIgnored = includeIgnored,
        detectSensitiveFiles = blockOnSensitiveFiles,
        maxFileSizeBytes = null
    )
}

private fun File.resolveRepositoryFile(relativePath: String): File {
    val root = canonicalFile
    val target = relativePath
        .toNormalizedRepositoryPath()
        .split('/')
        .filter { it.isNotBlank() }
        .fold(root) { current, segment -> current.resolve(segment) }
        .canonicalFile
    require(target.path == root.path || target.path.startsWith(root.path + File.separator)) {
        "文件路径越过工作区边界：$relativePath"
    }
    require(target.isFile) { "工作区文件不存在：$relativePath" }
    return target
}

private fun File.gitBlobSha1(): String {
    val digest = MessageDigest.getInstance("SHA-1")
    val header = "blob ${length()}\u0000".toByteArray(Charsets.UTF_8)
    digest.update(header)
    FileInputStream(this).use { input ->
        val buffer = ByteArray(GitHashBufferSize)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private const val GitHashBufferSize = 64 * 1024