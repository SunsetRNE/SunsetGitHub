package com.Sunset.REN.GitHub.ui.navigation

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.repo.RepositoryDetailFragment
import com.Sunset.REN.GitHub.ui.repo.RepositorySection

data class NavigationDestinationRule(
    @IdRes val destinationId: Int,
    val mode: NavigationBarMode = NavigationBarMode.Docked,
    val showWhenSignedOut: Boolean = false,
    val repositorySection: RepositorySection? = null,
    val isRepositoryRelated: Boolean = repositorySection != null
)

object NavigationDestinationRegistry {

    private val destinationRules = listOf(
        NavigationDestinationRule(R.id.navigation_home, showWhenSignedOut = true),
        NavigationDestinationRule(R.id.navigation_dashboard, showWhenSignedOut = true),
        NavigationDestinationRule(R.id.navigation_profile, showWhenSignedOut = true),
        NavigationDestinationRule(R.id.navigation_notifications, showWhenSignedOut = true),
        NavigationDestinationRule(R.id.navigation_settings, showWhenSignedOut = true),
        NavigationDestinationRule(R.id.account_fragment, showWhenSignedOut = true),
        NavigationDestinationRule(R.id.repository_detail_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Code),
        NavigationDestinationRule(R.id.repository_file_edit_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Code),
        NavigationDestinationRule(R.id.repository_file_upload_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Code),
        NavigationDestinationRule(R.id.local_file_manager_fragment, mode = NavigationBarMode.Hidden, showWhenSignedOut = true),
        NavigationDestinationRule(R.id.local_file_preview_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Code),
        NavigationDestinationRule(R.id.repository_releases_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Code),
        NavigationDestinationRule(R.id.repository_section_native_stub_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Code),
        NavigationDestinationRule(R.id.repository_issues_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Issues),
        NavigationDestinationRule(R.id.repository_issue_detail_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Issues),
        NavigationDestinationRule(R.id.repository_issue_create_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Issues),
        NavigationDestinationRule(R.id.repository_pull_requests_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.PullRequests),
        NavigationDestinationRule(R.id.repository_actions_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Actions),
        NavigationDestinationRule(R.id.repository_action_run_detail_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Actions),
        NavigationDestinationRule(R.id.repository_action_run_developer_info_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Actions),
        NavigationDestinationRule(R.id.repository_projects_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Projects),
        NavigationDestinationRule(R.id.repository_security_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.SecurityQuality),
        NavigationDestinationRule(R.id.repository_security_alert_detail_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.SecurityQuality),
        NavigationDestinationRule(R.id.repository_insights_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Insights),
        NavigationDestinationRule(R.id.repository_wiki_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Wiki),
        NavigationDestinationRule(R.id.repository_agents_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Agents),
        NavigationDestinationRule(R.id.repository_settings_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Settings),
        NavigationDestinationRule(R.id.repository_branch_settings_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Settings),
        NavigationDestinationRule(R.id.repository_collaborators_settings_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Settings),
        NavigationDestinationRule(R.id.repository_actions_settings_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Settings),
        NavigationDestinationRule(R.id.repository_danger_zone_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Settings),
        NavigationDestinationRule(R.id.repository_webhooks_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Settings),
        NavigationDestinationRule(R.id.repository_rulesets_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Settings),
        NavigationDestinationRule(R.id.repository_deploy_keys_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Settings),
        NavigationDestinationRule(R.id.repository_fork_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Fork),
        NavigationDestinationRule(R.id.repository_release_create_fragment, showWhenSignedOut = true, repositorySection = RepositorySection.Code)
    )

    private val rulesByDestinationId = destinationRules.associateBy { rule -> rule.destinationId }
    private val primaryDestinationsBySection = mapOf(
        RepositorySection.Code to R.id.repository_detail_fragment,
        RepositorySection.Issues to R.id.repository_issues_fragment,
        RepositorySection.PullRequests to R.id.repository_pull_requests_fragment,
        RepositorySection.Actions to R.id.repository_actions_fragment,
        RepositorySection.Projects to R.id.repository_projects_fragment,
        RepositorySection.SecurityQuality to R.id.repository_security_fragment,
        RepositorySection.Insights to R.id.repository_insights_fragment,
        RepositorySection.Wiki to R.id.repository_wiki_fragment,
        RepositorySection.Agents to R.id.repository_agents_fragment,
        RepositorySection.Settings to R.id.repository_settings_fragment,
        RepositorySection.Fork to R.id.repository_fork_fragment
    )

    fun resolveMode(
        destinationId: Int?,
        isAuthorized: Boolean,
        isFloatingNavigationEnabled: Boolean,
        forceHidden: Boolean = false
    ): NavigationBarMode {
        if (forceHidden) return NavigationBarMode.Hidden
        val rule = rulesByDestinationId[destinationId]
        return when {
            rule == null -> NavigationBarMode.Hidden
            !isAuthorized && !rule.showWhenSignedOut -> NavigationBarMode.Hidden
            rule.mode == NavigationBarMode.Docked && isFloatingNavigationEnabled -> NavigationBarMode.FloatingReserved
            else -> rule.mode
        }
    }

    fun resolveRenderState(
        destinationId: Int?,
        isAuthorized: Boolean,
        isFloatingNavigationEnabled: Boolean,
        forceHidden: Boolean = false
    ): NavigationBarRenderState {
        return NavigationBarRenderState.forMode(
            resolveMode(
                destinationId = destinationId,
                isAuthorized = isAuthorized,
                isFloatingNavigationEnabled = isFloatingNavigationEnabled,
                forceHidden = forceHidden
            )
        )
    }

    fun isRepositoryRelatedDestination(destinationId: Int): Boolean {
        return rulesByDestinationId[destinationId]?.isRepositoryRelated == true
    }

    fun hasRepositoryIdentity(arguments: Bundle?): Boolean {
        return !arguments?.getString(RepositoryDetailFragment.ARG_OWNER).isNullOrBlank() &&
            !arguments?.getString(RepositoryDetailFragment.ARG_REPO).isNullOrBlank()
    }

    fun repositoryFullNameFrom(arguments: Bundle?): String {
        val owner = arguments?.getString(RepositoryDetailFragment.ARG_OWNER).orEmpty()
        val repo = arguments?.getString(RepositoryDetailFragment.ARG_REPO).orEmpty()
        return if (owner.isNotBlank() && repo.isNotBlank()) "$owner/$repo" else repo.ifBlank { owner }
    }

    fun sectionForDestination(destinationId: Int?): RepositorySection {
        return rulesByDestinationId[destinationId]?.repositorySection ?: RepositorySection.Code
    }

    fun destinationForSection(section: RepositorySection): Int? {
        return primaryDestinationsBySection[section]
    }

    fun argumentsForSection(section: RepositorySection, currentArguments: Bundle?): Bundle? {
        val owner = currentArguments?.getString(RepositoryDetailFragment.ARG_OWNER).orEmpty()
        val repo = currentArguments?.getString(RepositoryDetailFragment.ARG_REPO).orEmpty()
        if (owner.isBlank() || repo.isBlank()) return null

        return bundleOf(
            RepositoryDetailFragment.ARG_OWNER to owner,
            RepositoryDetailFragment.ARG_REPO to repo
        ).apply {
            if (section == RepositorySection.Code) {
                putString(
                    RepositoryDetailFragment.ARG_FULL_NAME,
                    currentArguments?.getString(RepositoryDetailFragment.ARG_FULL_NAME).orEmpty()
                        .ifBlank { "$owner/$repo" }
                )
            }
        }
    }
}