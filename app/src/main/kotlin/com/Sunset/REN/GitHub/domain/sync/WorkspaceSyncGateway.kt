package com.Sunset.REN.GitHub.domain.sync

/**
 * 工作区到远端仓库的同步抽象。
 * 初始实现可走 GitHub Git Data API；后续可替换为 JGit、libgit2 或 native git。
 */
interface WorkspaceSyncGateway {
    suspend fun buildPlan(request: WorkspaceSyncRequest): WorkspaceSyncPlan

    suspend fun executePlan(
        plan: WorkspaceSyncPlan,
        progress: suspend (WorkspaceSyncProgress) -> Unit = {}
    ): WorkspaceSyncResult
}

/**
 * 可插拔同步后端能力描述，供 UI 决定是否展示镜像覆盖、删除远端文件等危险选项。
 */
data class WorkspaceSyncBackendCapabilities(
    val supportsIncrementalUpload: Boolean,
    val supportsMirrorRemotePath: Boolean,
    val supportsRemoteDeletes: Boolean,
    val supportsConflictDetection: Boolean,
    val supportsDryRun: Boolean,
    val maxRecommendedFilesPerCommit: Int? = null
)

interface WorkspaceSyncBackend {
    val id: String
    val displayName: String
    val capabilities: WorkspaceSyncBackendCapabilities

    suspend fun buildPlan(request: WorkspaceSyncRequest): WorkspaceSyncPlan

    suspend fun executePlan(
        plan: WorkspaceSyncPlan,
        progress: suspend (WorkspaceSyncProgress) -> Unit = {}
    ): WorkspaceSyncResult
}