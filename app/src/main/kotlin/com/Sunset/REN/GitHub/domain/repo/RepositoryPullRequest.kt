package com.Sunset.REN.GitHub.domain.repo

/** 仓库 Pull Request 列表项。 */
data class RepositoryPullRequest(
    val number: Int,
    val title: String,
    val state: String,
    val authorLogin: String,
    val commentCount: Int,
    val createdAt: String?,
    val updatedAt: String?,
    val closedAt: String?,
    val mergedAt: String?,
    val draft: Boolean,
    val baseRef: String,
    val headRef: String,
    val htmlUrl: String?
) {
    val isMerged: Boolean
        get() = !mergedAt.isNullOrBlank()
}
