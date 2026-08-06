package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.domain.repo.RepositoryIssueComment
import com.Sunset.REN.GitHub.domain.repo.RepositoryIssueDetail
import com.Sunset.REN.GitHub.domain.repo.RepositoryLabel

/** 问题详情页状态：正文 + 评论分页 + 写操作能力。 */
data class RepositoryIssueDetailUiState(
    val owner: String = "",
    val repo: String = "",
    val number: Int = 0,
    val issue: RepositoryIssueDetail? = null,
    val comments: List<RepositoryIssueComment> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMoreComments: Boolean = false,
    val errorMessage: String? = null,
    val hasMoreComments: Boolean = false,
    val loadedCommentPages: Int = 0,
    // 写操作能力相关
    val canPush: Boolean = false,
    val currentUserLogin: String = "",
    val availableLabels: List<RepositoryLabel> = emptyList(),
    val isMutating: Boolean = false,
    val statusMessage: String? = null
) {
    val isInitialLoad: Boolean
        get() = isLoading && issue == null

    /** 是否登录：决定能否发表评论。 */
    val isSignedIn: Boolean
        get() = currentUserLogin.isNotBlank()

    /** 是否可以开关 Issue：有写权限或本人是作者。 */
    val canToggleState: Boolean
        get() = issue != null &&
            (canPush || (currentUserLogin.isNotBlank() && currentUserLogin == issue.authorLogin))

    /** 是否可以编辑标签：需要写权限，且有可选标签。 */
    val canEditLabels: Boolean
        get() = canPush && issue != null

    /** 是否可以编辑/删除某条评论：本人评论或有写权限。 */
    fun canManageComment(authorLogin: String): Boolean {
        return canPush || (currentUserLogin.isNotBlank() && currentUserLogin == authorLogin)
    }
}