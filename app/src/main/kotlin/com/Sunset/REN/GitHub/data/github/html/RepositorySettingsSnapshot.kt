package com.Sunset.REN.GitHub.data.github.html

import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsEditableFieldKey
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsInfoItem
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsInfoKey
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsScreenState
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsStatItem
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsToggleItem
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsToggleKey
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsVisibility
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsVisibilityOption
import org.json.JSONObject

data class RepositorySettingsSnapshot(
    val owner: String,
    val repo: String,
    val name: String,
    val fullName: String,
    val description: String,
    val homepage: String,
    val defaultBranch: String,
    val visibilityLabel: String,
    val permissionLabel: String,
    val licenseLabel: String,
    val languageLabel: String,
    val createdAt: String,
    val updatedAt: String,
    val pushedAt: String,
    val stargazersCount: Int,
    val forksCount: Int,
    val openIssuesCount: Int,
    val hasIssues: Boolean,
    val hasProjects: Boolean,
    val hasWiki: Boolean,
    val hasDiscussions: Boolean,
    val allowForking: Boolean,
    val archived: Boolean,
    val allowSquashMerge: Boolean,
    val allowMergeCommit: Boolean,
    val allowRebaseMerge: Boolean,
    val deleteBranchOnMerge: Boolean,
    val allowAutoMerge: Boolean,
    val canAdmin: Boolean,
    val canPush: Boolean,
    val sourceUrl: String
) {
    fun toScreenState(): RepositorySettingsScreenState {
        val summaryText = description
        return RepositorySettingsScreenState(
            owner = owner,
            repo = repo,
            fullName = fullName,
            summary = summaryText,
            visibilityLabel = visibilityLabel,
            defaultBranch = defaultBranch,
            permissionLabel = permissionLabel,
            stats = listOf(
                RepositorySettingsStatItem("Stars", stargazersCount.toString()),
                RepositorySettingsStatItem("Forks", forksCount.toString()),
                RepositorySettingsStatItem("Issues", openIssuesCount.toString())
            ),
            basicItems = listOf(
                RepositorySettingsInfoItem(RepositorySettingsInfoKey.Source, "数据来源", "GitHub REST API"),
                RepositorySettingsInfoItem(RepositorySettingsInfoKey.Visibility, "当前可见性", visibilityLabel),
                RepositorySettingsInfoItem(RepositorySettingsInfoKey.License, "许可证", licenseLabel),
                RepositorySettingsInfoItem(RepositorySettingsInfoKey.Language, "主要语言", languageLabel),
                RepositorySettingsInfoItem(RepositorySettingsInfoKey.CreatedAt, "创建时间", createdAt),
                RepositorySettingsInfoItem(RepositorySettingsInfoKey.UpdatedAt, "最近更新", updatedAt),
                RepositorySettingsInfoItem(RepositorySettingsInfoKey.PushedAt, "最近推送", pushedAt)
            ),
            visibilityOption = RepositorySettingsVisibilityOption(
                selected = visibilityLabel.toRepositoryVisibility(),
                editable = canAdmin
            ),
            editableItems = listOf(
                com.Sunset.REN.GitHub.ui.repo.RepositorySettingsEditableItem(
                    key = RepositorySettingsEditableFieldKey.Name,
                    label = "仓库名",
                    value = name,
                    helper = "重命名后仓库地址会变化",
                    editable = canAdmin
                ),
                com.Sunset.REN.GitHub.ui.repo.RepositorySettingsEditableItem(
                    key = RepositorySettingsEditableFieldKey.Description,
                    label = "描述",
                    value = description,
                    helper = "仓库简介",
                    editable = canAdmin
                ),
                com.Sunset.REN.GitHub.ui.repo.RepositorySettingsEditableItem(
                    key = RepositorySettingsEditableFieldKey.Homepage,
                    label = "主页",
                    value = homepage,
                    helper = "项目主页 URL",
                    editable = canAdmin
                ),
                com.Sunset.REN.GitHub.ui.repo.RepositorySettingsEditableItem(
                    key = RepositorySettingsEditableFieldKey.DefaultBranch,
                    label = "默认分支",
                    value = defaultBranch,
                    helper = "输入现有分支名",
                    editable = canAdmin
                )
            ),
            featureItems = listOf(
                RepositorySettingsToggleItem(RepositorySettingsToggleKey.HasIssues, "Issues", hasIssues, canAdmin, "跟踪任务、缺陷和讨论事项"),
                RepositorySettingsToggleItem(RepositorySettingsToggleKey.HasProjects, "Projects", hasProjects, canAdmin, "在仓库中使用项目看板和规划视图"),
                RepositorySettingsToggleItem(RepositorySettingsToggleKey.HasWiki, "Wiki", hasWiki, canAdmin, "允许维护仓库 Wiki 文档"),
                RepositorySettingsToggleItem(RepositorySettingsToggleKey.HasDiscussions, "Discussions", hasDiscussions, canAdmin, "启用 GitHub Discussions 社区讨论区"),
                RepositorySettingsToggleItem(RepositorySettingsToggleKey.AllowForking, "Fork", allowForking, canAdmin, "允许其他用户派生此仓库"),
                RepositorySettingsToggleItem(RepositorySettingsToggleKey.Archived, "归档", archived, canAdmin, "归档后仓库变为只读")
            ),
            mergeItems = listOf(
                RepositorySettingsToggleItem(RepositorySettingsToggleKey.AllowSquashMerge, "Squash merge", allowSquashMerge, canAdmin, "将 Pull Request 合并为一个提交"),
                RepositorySettingsToggleItem(RepositorySettingsToggleKey.AllowMergeCommit, "Merge commit", allowMergeCommit, canAdmin, "保留分支提交并创建合并提交"),
                RepositorySettingsToggleItem(RepositorySettingsToggleKey.AllowRebaseMerge, "Rebase merge", allowRebaseMerge, canAdmin, "将提交变基到目标分支"),
                RepositorySettingsToggleItem(RepositorySettingsToggleKey.DeleteBranchOnMerge, "合并后删除分支", deleteBranchOnMerge, canAdmin, "Pull Request 合并后自动删除源分支"),
                RepositorySettingsToggleItem(RepositorySettingsToggleKey.AllowAutoMerge, "Auto merge", allowAutoMerge, canAdmin, "检查通过后自动合并 Pull Request")
            ),
            notices = listOfNotNull(
                if (homepage.isBlank()) "未设置仓库主页。" else "主页：$homepage",
                "权限：$permissionLabel",
                if (canAdmin) "当前账号具备管理员权限，可直接修改本页支持的仓库设置。" else "当前账号没有管理员权限，本页以只读模式展示。"
            ),
            sourceUrl = sourceUrl,
            canEdit = canAdmin
        )
    }
}

private fun String.toRepositoryVisibility(): RepositorySettingsVisibility {
    return when (lowercase()) {
        "private" -> RepositorySettingsVisibility.Private
        "internal" -> RepositorySettingsVisibility.Internal
        else -> RepositorySettingsVisibility.Public
    }
}

data class RepositorySettingsUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val homepage: String? = null,
    val defaultBranch: String? = null,
    val visibility: String? = null,
    val hasIssues: Boolean? = null,
    val hasProjects: Boolean? = null,
    val hasWiki: Boolean? = null,
    val hasDiscussions: Boolean? = null,
    val allowForking: Boolean? = null,
    val archived: Boolean? = null,
    val allowSquashMerge: Boolean? = null,
    val allowMergeCommit: Boolean? = null,
    val allowRebaseMerge: Boolean? = null,
    val deleteBranchOnMerge: Boolean? = null,
    val allowAutoMerge: Boolean? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            name?.let { put("name", it) }
            description?.let { put("description", it) }
            homepage?.let { put("homepage", it) }
            defaultBranch?.let { put("default_branch", it) }
            visibility?.let { put("visibility", it) }
            hasIssues?.let { put("has_issues", it) }
            hasProjects?.let { put("has_projects", it) }
            hasWiki?.let { put("has_wiki", it) }
            hasDiscussions?.let { put("has_discussions", it) }
            allowForking?.let { put("allow_forking", it) }
            archived?.let { put("archived", it) }
            allowSquashMerge?.let { put("allow_squash_merge", it) }
            allowMergeCommit?.let { put("allow_merge_commit", it) }
            allowRebaseMerge?.let { put("allow_rebase_merge", it) }
            deleteBranchOnMerge?.let { put("delete_branch_on_merge", it) }
            allowAutoMerge?.let { put("allow_auto_merge", it) }
        }
    }
}
