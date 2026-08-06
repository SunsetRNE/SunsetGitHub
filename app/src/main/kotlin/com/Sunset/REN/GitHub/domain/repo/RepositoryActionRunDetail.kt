package com.Sunset.REN.GitHub.domain.repo

/** 仓库 Actions workflow run 详情。 */
data class RepositoryActionRunDetail(
    val id: Long,
    val nodeId: String?,
    val name: String,
    val status: String?,
    val conclusion: String?,
    val event: String?,
    val headBranch: String?,
    val headSha: String?,
    val htmlUrl: String?,
    val apiUrl: String?,
    val workflowUrl: String?,
    val jobsUrl: String?,
    val logsUrl: String?,
    val artifactsUrl: String?,
    val cancelUrl: String?,
    val rerunUrl: String?,
    val previousAttemptUrl: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val runStartedAt: String?,
    val runNumber: Int?,
    val runAttempt: Int?,
    val workflowId: Long?,
    val checkSuiteId: Long?,
    val workflowName: String?,
    val actorLogin: String?,
    val triggeringActorLogin: String?,
    val repositoryOwner: String?,
    val repositoryName: String?,
    val headRepositoryFullName: String?,
    val headRepositoryHtmlUrl: String?,
    val path: String?,
    val headCommitMessage: String?,
    val headCommitAuthorName: String?,
    val headCommitAuthorEmail: String?,
    val headCommitTimestamp: String?,
    val sourceZipUrl: String?,
    val sourceTarUrl: String?,
    val pullRequestRefs: List<String> = emptyList(),
    val displayState: String,
    val details: List<String> = emptyList()
)

/** 可从 Actions 运行详情下载的构建产物。 */
data class RepositoryActionArtifact(
    val id: Long,
    val name: String,
    val sizeInBytes: Long,
    val archiveDownloadUrl: String?,
    val expired: Boolean,
    val createdAt: String?,
    val expiresAt: String?
)
