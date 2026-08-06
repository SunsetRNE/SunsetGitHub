package com.Sunset.REN.GitHub.domain.repo

/** 问题（Issue）列表项。GitHub /issues 接口会混入 PR，用 isPullRequest 标记后在列表层过滤。 */
data class RepositoryIssue(
    val number: Int,
    val title: String,
    val state: String,
    val authorLogin: String,
    val commentCount: Int = 0,
    val labels: List<RepositoryIssueLabel> = emptyList(),
    val createdAt: String? = null,
    val htmlUrl: String? = null,
    val isPullRequest: Boolean = false
)

/** Issue / PR 上的标签。color 是 GitHub 返回的 6 位 hex（不含 #）。 */
data class RepositoryIssueLabel(
    val name: String,
    val color: String
)

/** 问题详情，比列表项多出正文 body。 */
data class RepositoryIssueDetail(
    val number: Int,
    val title: String,
    val state: String,
    val authorLogin: String,
    val body: String,
    val commentCount: Int = 0,
    val labels: List<RepositoryIssueLabel> = emptyList(),
    val createdAt: String? = null,
    val htmlUrl: String? = null
)

/** 问题下的单条评论。 */
data class RepositoryIssueComment(
    val id: Long,
    val authorLogin: String,
    val body: String,
    val createdAt: String? = null,
    val htmlUrl: String? = null
)

/** 仓库标签，用于贴/撕标签时的候选列表。color 是 GitHub 返回的 6 位 hex（不含 #）。 */
data class RepositoryLabel(
    val name: String,
    val color: String,
    val description: String? = null
)

/** 当前用户对该仓库的权限。push 为写权限标志，决定是否显示写操作入口。 */
data class RepositoryPermissions(
    val canPush: Boolean = false,
    val isAdmin: Boolean = false
)