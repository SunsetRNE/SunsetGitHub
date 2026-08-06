package com.Sunset.REN.GitHub.domain.sync

/**
 * 预留的本地 Git 抽象。当前阶段不强制实现，避免过早绑定 JGit/libgit2/native git。
 * 当工作区需要真正 clone/pull/push/branch/merge 语义时，由具体后端接入。
 */
interface LocalGitBackend {
    val id: String
    val displayName: String
    val capabilities: LocalGitCapabilities

    suspend fun isAvailable(): Boolean

    suspend fun cloneRepository(request: LocalGitCloneRequest): LocalGitRepository

    suspend fun status(repositoryPath: String): LocalGitStatus

    suspend fun diff(repositoryPath: String, relativePath: String? = null): LocalGitDiff

    suspend fun commit(request: LocalGitCommitRequest): LocalGitCommitResult

    suspend fun pull(request: LocalGitPullRequest): LocalGitPullResult

    suspend fun push(request: LocalGitPushRequest): LocalGitPushResult
}

data class LocalGitCapabilities(
    val supportsClone: Boolean = false,
    val supportsStatus: Boolean = false,
    val supportsDiff: Boolean = false,
    val supportsCommit: Boolean = false,
    val supportsPull: Boolean = false,
    val supportsPush: Boolean = false,
    val supportsBranches: Boolean = false,
    val supportsMerge: Boolean = false
)

data class LocalGitCloneRequest(
    val remoteUrl: String,
    val targetPath: String,
    val branch: String? = null
)

data class LocalGitRepository(
    val path: String,
    val currentBranch: String?,
    val headRevision: String?
)

data class LocalGitStatus(
    val added: List<String> = emptyList(),
    val modified: List<String> = emptyList(),
    val deleted: List<String> = emptyList(),
    val untracked: List<String> = emptyList(),
    val conflicts: List<String> = emptyList()
)

data class LocalGitDiff(
    val entries: List<LocalGitDiffEntry> = emptyList()
)

data class LocalGitDiffEntry(
    val relativePath: String,
    val patch: String? = null,
    val oldSha: String? = null,
    val newSha: String? = null
)

data class LocalGitCommitRequest(
    val repositoryPath: String,
    val message: String,
    val paths: List<String> = emptyList()
)

data class LocalGitCommitResult(
    val commitHash: String
)

data class LocalGitPullRequest(
    val repositoryPath: String,
    val remote: String = "origin",
    val branch: String? = null
)

data class LocalGitPullResult(
    val headRevision: String?,
    val changedFiles: List<String> = emptyList()
)

data class LocalGitPushRequest(
    val repositoryPath: String,
    val remote: String = "origin",
    val branch: String? = null
)

data class LocalGitPushResult(
    val remoteRevision: String?
)

object NoOpLocalGitBackend : LocalGitBackend {
    override val id: String = "noop"
    override val displayName: String = "No local Git backend"
    override val capabilities: LocalGitCapabilities = LocalGitCapabilities()

    override suspend fun isAvailable(): Boolean = false

    override suspend fun cloneRepository(request: LocalGitCloneRequest): LocalGitRepository = unavailable()

    override suspend fun status(repositoryPath: String): LocalGitStatus = unavailable()

    override suspend fun diff(repositoryPath: String, relativePath: String?): LocalGitDiff = unavailable()

    override suspend fun commit(request: LocalGitCommitRequest): LocalGitCommitResult = unavailable()

    override suspend fun pull(request: LocalGitPullRequest): LocalGitPullResult = unavailable()

    override suspend fun push(request: LocalGitPushRequest): LocalGitPushResult = unavailable()

    private fun unavailable(): Nothing {
        throw UnsupportedOperationException("Local Git backend is not configured")
    }
}