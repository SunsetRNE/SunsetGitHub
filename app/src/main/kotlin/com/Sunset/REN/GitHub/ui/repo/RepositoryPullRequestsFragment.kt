package com.Sunset.REN.GitHub.ui.repo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.RepositoryPullRequest
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryPullRequestsScreen
import com.google.android.material.snackbar.Snackbar

class RepositoryPullRequestsFragment : RepositorySectionFragment() {

    private lateinit var viewModel: RepositoryPullRequestsViewModel
    private var pullRequestsState by mutableStateOf(RepositoryPullRequestsUiState())
    private var rootView: View? = null

    override var repositoryOwner: String = ""
    override var repositoryName: String = ""
    override val selectedRepositorySection: RepositorySection = RepositorySection.PullRequests

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[RepositoryPullRequestsViewModel::class.java]
        repositoryOwner = requireArguments().getString(ARG_OWNER).orEmpty()
        repositoryName = requireArguments().getString(ARG_REPO).orEmpty()
        pullRequestsState = viewModel.pullRequestsState.value ?: RepositoryPullRequestsUiState(owner = repositoryOwner, repo = repositoryName)
        return ComposeView(requireContext()).apply {
            this@RepositoryPullRequestsFragment.rootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryPullRequestsScreen(
                        state = pullRequestsState,
                        onStateSelected = viewModel::switchState,
                        onRetry = viewModel::loadFirstPage,
                        onLoadMore = viewModel::loadNextPage,
                        onOpenPullRequest = ::openPullRequestInGithub
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.pullRequestsState.observe(viewLifecycleOwner) { state -> pullRequestsState = state }
        viewModel.prepare(repositoryOwner, repositoryName)
        renderRepositorySectionNavigation()
    }

    override fun onDestroyView() {
        rootView = null
        super.onDestroyView()
    }

    private fun openPullRequestInGithub(pullRequest: RepositoryPullRequest) {
        val url = pullRequest.htmlUrl?.takeIf { it.isNotBlank() }
            ?: "https://github.com/$repositoryOwner/$repositoryName/pull/${pullRequest.number}"
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            showOpenFailedMessage()
        }
    }

    private fun showOpenFailedMessage() {
        rootView?.let { view ->
            Snackbar.make(view, R.string.repository_pull_requests_open_failed, Snackbar.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
    }
}