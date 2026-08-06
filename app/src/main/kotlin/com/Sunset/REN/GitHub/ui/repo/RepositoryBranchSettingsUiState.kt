package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.data.github.html.RepositoryBranchProtectionSnapshot
import com.Sunset.REN.GitHub.data.github.html.RepositoryBranchSettingsSnapshot

data class RepositoryBranchSettingsUiState(
    val owner: String = "",
    val repo: String = "",
    val snapshot: RepositoryBranchSettingsSnapshot? = null,
    val selectedBranch: String? = null,
    val selectedProtection: RepositoryBranchProtectionSnapshot? = null,
    val isLoading: Boolean = false,
    val isLoadingProtection: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val pendingMessage: String? = null,
    val sourceUrl: String? = null,
    val isShowingStaleContent: Boolean = false
) {
    val isInitialLoad: Boolean get() = snapshot == null && isLoading
    val branches: List<RepositoryBranchSettingsRow> get() = snapshot?.toRows().orEmpty()
}

data class RepositoryBranchSettingsRow(
    val name: String,
    val sha: String,
    val isDefault: Boolean,
    val isProtected: Boolean,
    val protectionSummary: String,
    val canEdit: Boolean
)

fun RepositoryBranchSettingsSnapshot.toRows(): List<RepositoryBranchSettingsRow> {
    return branches.map { branch ->
        RepositoryBranchSettingsRow(
            name = branch.name,
            sha = branch.sha.take(ShortShaLength),
            isDefault = branch.isDefault,
            isProtected = branch.protected,
            protectionSummary = branch.protection?.toBranchProtectionSummary()
                ?: if (branch.protected) "已保护，详情待加载" else "未保护",
            canEdit = canAdmin
        )
    }
}

fun RepositoryBranchProtectionSnapshot.toBranchProtectionSummary(): String {
    return buildList {
        if (requiredStatusChecks != null) {
            val checksCount = requiredStatusChecks.contexts.size + requiredStatusChecks.checks.size
            add(if (checksCount > 0) "状态检查 $checksCount 项" else "要求状态检查")
        }
        if (requiredPullRequestReviews != null) {
            val count = requiredPullRequestReviews.requiredApprovingReviewCount
            add(if (count > 0) "PR 审查 $count 人" else "要求 PR 审查")
            if (requiredPullRequestReviews.requireCodeOwnerReviews) add("Code Owners")
        }
        if (enforceAdmins) add("包含管理员")
        if (requiredLinearHistory) add("线性历史")
        if (requiredConversationResolution) add("解决对话")
        if (requiredSignatures) add("签名提交")
        if (restrictions != null && (restrictions.users.isNotEmpty() || restrictions.teams.isNotEmpty() || restrictions.apps.isNotEmpty())) {
            add("限制推送")
        }
        if (allowForcePushes) add("允许强推")
        if (allowDeletions) add("允许删除")
    }.ifEmpty { listOf("已启用保护规则") }.joinToString(" · ")
}

private const val ShortShaLength = 7
