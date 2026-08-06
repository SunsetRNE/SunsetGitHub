package com.Sunset.REN.GitHub.ui.repo

class RepositoryProjectsFragment : RepositoryHtmlSummarySectionFragment() {
    override val summarySection: RepositorySection = RepositorySection.Projects

    companion object {
        const val ARG_OWNER = RepositoryHtmlSummarySectionFragment.ARG_OWNER
        const val ARG_REPO = RepositoryHtmlSummarySectionFragment.ARG_REPO
        const val ARG_PAGE_MODE = "page_mode"
    }
}

enum class ProjectsPageMode {
    Overview,
    NewProject;

    companion object {
        fun from(raw: String?): ProjectsPageMode = entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: Overview
    }
}
