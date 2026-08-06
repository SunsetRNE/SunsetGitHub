package com.Sunset.REN.GitHub.domain.repo

/**
 * 仓库文件写入操作类型。
 */
enum class FileWriteOperation {
    Edit,
    Create,
    Upload,
    Overwrite
}

/**
 * 文件写入来源。
 */
enum class FileWriteSource {
    App,
    Terminal,
    External
}

/**
 * 文件冲突状态。
 */
enum class FileConflictState {
    None,
    PossibleRemoteChanged,
    RemoteRejected
}

/**
 * 文件提交状态。
 */
enum class FileSubmitState {
    Idle,
    Validating,
    Submitting,
    Submitted,
    Failed
}

/**
 * 文件能力判定结果，避免用单一 isBinary 表达全链路能力。
 */
data class FileCapability(
    val canEdit: Boolean = false,
    val canCreate: Boolean = false,
    val canUpload: Boolean = false,
    val canPreview: Boolean = false,
    val canOverwrite: Boolean = false,
    val reason: String? = null
) {
    companion object {
        val EditableText = FileCapability(
            canEdit = true,
            canCreate = true,
            canUpload = true,
            canPreview = true,
            canOverwrite = true
        )

        val UploadOnly = FileCapability(
            canUpload = true,
            canOverwrite = true
        )
    }
}

/**
 * 选中的本地文件。第一阶段 UI 只放入一个元素，但模型预留多文件能力。
 */
data class SelectedRepositoryWriteFile(
    val displayName: String,
    val uri: String,
    val sizeBytes: Long? = null,
    val mimeType: String? = null
)

/**
 * 一次仓库文件写入会话，统一承载编辑、新建、上传、覆盖的上下文。
 */
data class FileWriteSession(
    val repositoryId: String = "",
    val owner: String,
    val repo: String,
    val branch: String? = null,
    val targetPath: String,
    val originalPath: String? = null,
    val operation: FileWriteOperation,
    val source: FileWriteSource = FileWriteSource.App,
    val commitMessage: String,
    val baseSha: String? = null,
    val latestRemoteSha: String? = null,
    val selectedFiles: List<SelectedRepositoryWriteFile> = emptyList(),
    val content: String? = null,
    val capability: FileCapability,
    val conflictState: FileConflictState = FileConflictState.None,
    val submitState: FileSubmitState = FileSubmitState.Idle
)

data class FileContentWriteResult(
    val commitHash: String
)

/**
 * 应用内提交日志条目。第一阶段先作为领域模型，后续接入持久化。
 */
data class CommitLogEntry(
    val repositoryId: String,
    val owner: String,
    val repo: String,
    val branch: String? = null,
    val path: String,
    val commitHash: String,
    val commitTimeMillis: Long,
    val source: FileWriteSource,
    val operation: FileWriteOperation,
    val messageSummary: String
)

/**
 * 最近提交快速索引。只代表应用已知状态，不能替代 GitHub 服务端冲突校验。
 */
data class RecentCommitIndex(
    val repositoryId: String,
    val branch: String? = null,
    val path: String,
    val latestKnownCommitHash: String,
    val latestKnownCommitTimeMillis: Long,
    val source: FileWriteSource
)