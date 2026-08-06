package com.Sunset.REN.GitHub.domain.sync

/**
 * 远端仓库提交后端，抽象 GitHub Git Data API 的最小提交能力。
 * WorkspaceSyncBackend 负责把工作区同步计划翻译为这里的 tree/blob/commit/ref 操作。
 */
interface RemoteRepositoryCommitBackend {
    suspend fun getBranchHead(
        owner: String,
        repo: String,
        branch: String
    ): RemoteBranchHead

    suspend fun getCommit(
        owner: String,
        repo: String,
        commitSha: String
    ): RemoteCommit

    suspend fun getTree(
        owner: String,
        repo: String,
        treeSha: String,
        recursive: Boolean = true
    ): RemoteTree

    suspend fun createBlob(
        owner: String,
        repo: String,
        contentBytes: ByteArray
    ): RemoteBlob

    suspend fun createTree(
        owner: String,
        repo: String,
        baseTreeSha: String?,
        entries: List<RemoteTreeEntryWrite>
    ): RemoteTree

    suspend fun createCommit(
        owner: String,
        repo: String,
        message: String,
        treeSha: String,
        parentCommitSha: String
    ): RemoteCommit

    suspend fun updateBranchHead(
        owner: String,
        repo: String,
        branch: String,
        commitSha: String,
        force: Boolean = false
    )
}

data class RemoteBranchHead(
    val branch: String,
    val commitSha: String,
    val ref: String = "refs/heads/$branch"
)

data class RemoteCommit(
    val sha: String,
    val treeSha: String,
    val parentShas: List<String> = emptyList(),
    val message: String? = null
)

data class RemoteTree(
    val sha: String,
    val entries: List<RemoteTreeEntry> = emptyList(),
    val truncated: Boolean = false
)

data class RemoteTreeEntry(
    val path: String,
    val mode: RemoteTreeEntryMode,
    val type: RemoteTreeEntryType,
    val sha: String?,
    val sizeBytes: Long? = null
)

data class RemoteTreeEntryWrite(
    val path: String,
    val mode: RemoteTreeEntryMode = RemoteTreeEntryMode.File,
    val type: RemoteTreeEntryType = RemoteTreeEntryType.Blob,
    /** null sha 表示删除该路径；新增/修改时传 createBlob 返回的 sha。 */
    val sha: String? = null,
    /** 预留给支持内联 content 的后端；GitHub 批量上传路径优先使用 blob sha。 */
    val content: String? = null
)

data class RemoteBlob(
    val sha: String
)

enum class RemoteTreeEntryMode(val wireValue: String) {
    File("100644"),
    Executable("100755"),
    Directory("040000"),
    Submodule("160000"),
    Symlink("120000")
}

enum class RemoteTreeEntryType(val wireValue: String) {
    Blob("blob"),
    Tree("tree"),
    Commit("commit")
}