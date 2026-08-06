package com.Sunset.REN.GitHub.domain.repo

/** 仓库 Actions workflow run 列表项。 */
data class RepositoryActionRun(
    val id: Long,
    val name: String,
    val status: String?,
    val conclusion: String?,
    val event: String,
    val headBranch: String?,
    val headSha: String?,
    val htmlUrl: String?,
    val createdAt: String?,
    val updatedAt: String?
) {
    val displayState: String
        get() = conclusion?.takeIf { it.isNotBlank() }
            ?: status?.takeIf { it.isNotBlank() }
            ?: "unknown"
}