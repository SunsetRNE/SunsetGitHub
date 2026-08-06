package com.Sunset.REN.GitHub.domain.sync

import com.Sunset.REN.GitHub.domain.workspace.SensitiveFileSeverity
import com.Sunset.REN.GitHub.domain.workspace.SensitiveWorkspaceFile
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceRemoteBinding

/**
 * 工作区同步模型面向“覆盖性整个仓库更新”和批量上传场景，
 * 不要求底层一定是真本地 Git，可由 GitHub API、JGit 或 native git 后端实现。
 */
data class WorkspaceSyncRequest(
    val workspaceId: String,
    val remote: WorkspaceRemoteBinding,
    val commitMessage: String,
    val mode: WorkspaceSyncMode = WorkspaceSyncMode.Incremental,
    val options: WorkspaceSyncOptions = WorkspaceSyncOptions()
)

enum class WorkspaceSyncMode {
    /** 只提交工作区相对上次快照/远程状态的增删改。 */
    Incremental,

    /** 以工作区内容为准更新目标 remotePath，可删除远端多余文件。 */
    MirrorRemotePath,

    /** 只新增/覆盖本地存在文件，不删除远端多余文件。 */
    UploadOnly
}

data class WorkspaceSyncOptions(
    val dryRun: Boolean = false,
    val allowDeletes: Boolean = false,
    val allowOverwriteRemoteChanges: Boolean = false,
    val blockOnSensitiveFiles: Boolean = true,
    val maxFilesPerCommit: Int? = null,
    val includeIgnored: Boolean = false,
    /** MirrorRemotePath 或任何可能删除远端文件的操作必须由 UI 明确二次确认。 */
    val destructiveOperationConfirmed: Boolean = false
)

data class WorkspaceSyncPlan(
    val planId: String,
    val workspaceId: String,
    val remote: WorkspaceRemoteBinding,
    val baseRevision: String? = null,
    val createdAtMillis: Long,
    val commitMessage: String,
    val mode: WorkspaceSyncMode,
    val options: WorkspaceSyncOptions,
    val operations: List<WorkspaceSyncOperation>,
    val conflicts: List<WorkspaceSyncConflict> = emptyList(),
    val sensitiveFiles: List<SensitiveWorkspaceFile> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    val hasBlockingIssues: Boolean
        get() = conflicts.isNotEmpty() || sensitiveFiles.any { it.severity == SensitiveFileSeverity.Blocking }

    val requiresDestructiveConfirmation: Boolean
        get() = mode == WorkspaceSyncMode.MirrorRemotePath || operations.any { it is WorkspaceSyncOperation.Delete }
}

sealed class WorkspaceSyncOperation {
    abstract val operationId: String
    abstract val relativePath: String
    abstract val remotePath: String

    data class Add(
        override val operationId: String,
        override val relativePath: String,
        override val remotePath: String,
        val sizeBytes: Long,
        val sha256: String
    ) : WorkspaceSyncOperation()

    data class Modify(
        override val operationId: String,
        override val relativePath: String,
        override val remotePath: String,
        val sizeBytes: Long,
        val sha256: String,
        val remoteSha: String? = null
    ) : WorkspaceSyncOperation()

    data class Delete(
        override val operationId: String,
        override val relativePath: String,
        override val remotePath: String,
        val remoteSha: String? = null
    ) : WorkspaceSyncOperation()
}

data class WorkspaceSyncConflict(
    val relativePath: String,
    val remotePath: String,
    val type: WorkspaceSyncConflictType,
    val message: String
)

enum class WorkspaceSyncConflictType {
    RemoteChanged,
    DeleteWouldRemoveRemoteFile,
    PathTypeMismatch,
    MissingPermission,
    UnsupportedFile
}

data class WorkspaceSyncProgress(
    val phase: WorkspaceSyncPhase,
    val completedOperations: Int,
    val totalOperations: Int,
    val message: String
)

enum class WorkspaceSyncPhase {
    Preparing,
    Scanning,
    Planning,
    UploadingObjects,
    CreatingCommit,
    UpdatingReference,
    SavingSnapshot,
    Completed,
    Failed
}

data class WorkspaceSyncResult(
    val planId: String,
    val workspaceId: String,
    val remote: WorkspaceRemoteBinding,
    val commitHash: String?,
    val appliedOperations: List<WorkspaceSyncOperation>,
    val skippedOperations: List<WorkspaceSyncOperation> = emptyList(),
    val failedOperations: List<WorkspaceSyncOperationFailure> = emptyList(),
    val completedAtMillis: Long
)

data class WorkspaceSyncOperationFailure(
    val operation: WorkspaceSyncOperation,
    val reason: String,
    val recoverable: Boolean = true
)