package com.Sunset.REN.GitHub.ui.repo

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.Sunset.REN.GitHub.R

enum class RepositorySection(
    val storageKey: String,
    @StringRes val titleResId: Int,
    @DrawableRes val navigationIconResId: Int,
    @IdRes val menuItemId: Int,
) {
    Code("code", R.string.repository_section_code, R.drawable.ic_code_24, 0x01010001),
    Issues("issues", R.string.repository_section_issues, R.drawable.ic_issue_24, 0x01010002),
    PullRequests("pull_requests", R.string.repository_section_pull_requests, R.drawable.ic_branch_24, 0x01010003),
    Actions("actions", R.string.repository_section_actions, R.drawable.ic_refresh_24, 0x01010004),
    Projects("projects", R.string.repository_section_projects, R.drawable.ic_dashboard_black_24dp, 0x01010005),
    SecurityQuality("security_quality", R.string.repository_section_security_quality, R.drawable.ic_visibility_24, 0x01010006),
    Insights("insights", R.string.repository_section_insights, R.drawable.ic_dashboard_black_24dp, 0x01010007),
    Wiki("wiki", R.string.repository_section_wiki, R.drawable.ic_file_24, 0x01010008),
    Agents("agents", R.string.repository_section_agents, R.drawable.ic_people_24, 0x01010009),
    Settings("settings", R.string.repository_section_settings, R.drawable.ic_settings_black_24dp, 0x0101000A),
    Fork("fork", R.string.repository_section_fork, R.drawable.ic_fork_24, 0x0101000B),
    More("more", R.string.repository_section_more, R.drawable.ic_sort_24, 0x0101000C);

    companion object {
        val FixedSections = listOf(Code, More)
        val DefaultShortcutSections = listOf(Issues, Actions, Settings)
        val ShortcutCandidateSections = listOf(
            Issues,
            PullRequests,
            Actions,
            Projects,
            SecurityQuality,
            Insights,
            Wiki,
            Agents,
            Settings,
            Fork
        )

        fun fromStorageKey(storageKey: String): RepositorySection? {
            return entries.firstOrNull { section -> section.storageKey == storageKey }
        }

        fun fromMenuItemId(menuItemId: Int): RepositorySection? {
            return entries.firstOrNull { section -> section.menuItemId == menuItemId }
        }
    }
}