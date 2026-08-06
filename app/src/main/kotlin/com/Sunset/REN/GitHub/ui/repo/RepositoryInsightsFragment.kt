package com.Sunset.REN.GitHub.ui.repo

class RepositoryInsightsFragment : RepositoryHtmlSummarySectionFragment() {
    override val summarySection: RepositorySection = RepositorySection.Insights

    companion object {
        const val ARG_OWNER = RepositoryHtmlSummarySectionFragment.ARG_OWNER
        const val ARG_REPO = RepositoryHtmlSummarySectionFragment.ARG_REPO
    }
}
