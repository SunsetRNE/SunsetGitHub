package com.Sunset.REN.GitHub.domain.repo

/**
 * V0.1 仓库列表与详情首屏所需的最小仓库模型。
 */
data class GitHubRepository(
    val id: Long,
    val name: String,
    val fullName: String,
    val ownerLogin: String,
    val description: String? = null,
    val isPrivate: Boolean,
    val fork: Boolean,
    val archived: Boolean,
    val defaultBranch: String,
    val stargazersCount: Int,
    val watchersCount: Int,
    val forksCount: Int,
    val openIssuesCount: Int,
    val language: String? = null,
    val languages: List<RepositoryLanguage> = emptyList(),
    val ownerAvatarUrl: String? = null,
    val ownerName: String? = null,
    val ownerType: String? = null,
    val parentFullName: String? = null,
    val parentDefaultBranch: String? = null,
    val sourceFullName: String? = null,
    val updatedAt: String? = null,
    val pushedAt: String? = null,
    val htmlUrl: String
)