package com.Sunset.REN.GitHub.ui.repo

class RepositoryWikiFragment : RepositoryHtmlSummarySectionFragment() {
    override val summarySection: RepositorySection = RepositorySection.Wiki

    companion object {
        const val ARG_OWNER = RepositoryHtmlSummarySectionFragment.ARG_OWNER
        const val ARG_REPO = RepositoryHtmlSummarySectionFragment.ARG_REPO
    }
}
