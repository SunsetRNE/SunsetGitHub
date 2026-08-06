package com.Sunset.REN.GitHub.ui.repo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryAgentsScreen
import com.google.android.material.snackbar.Snackbar

class RepositoryAgentsOfficialFragment : RepositorySectionFragment() {

    private val viewModel: RepositoryAgentsViewModel by viewModels()
    private var agentsState by mutableStateOf(RepositoryAgentsUiState())
    private var rootView: View? = null

    override var repositoryOwner: String = ""
    override var repositoryName: String = ""
    override val selectedRepositorySection: RepositorySection = RepositorySection.Agents

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        repositoryOwner = arguments?.getString(ARG_OWNER).orEmpty()
        repositoryName = arguments?.getString(ARG_REPO).orEmpty()
        agentsState = viewModel.agentsState.value ?: RepositoryAgentsUiState(owner = repositoryOwner, repo = repositoryName)
        return ComposeView(requireContext()).apply {
            this@RepositoryAgentsOfficialFragment.rootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryAgentsScreen(
                        state = agentsState,
                        repositoryLabel = buildRepositoryLabel(),
                        fallbackUrl = defaultAgentsUrl(),
                        onRetry = viewModel::refresh,
                        onOpenInGitHub = ::openAgentsInGithub,
                        onCopyDebug = ::copyDebugText
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderRepositorySectionNavigation()
        applyToolbarTitle()
        viewModel.agentsState.observe(viewLifecycleOwner) { state -> agentsState = state }
        viewModel.prepare(repositoryOwner, repositoryName)
    }

    override fun onDestroyView() {
        rootView = null
        super.onDestroyView()
    }

    private fun buildRepositoryLabel(): String {
        return if (repositoryOwner.isNotBlank() && repositoryName.isNotBlank()) {
            getString(R.string.repository_agents_repository_label, repositoryOwner, repositoryName)
        } else {
            getString(R.string.repository_section_native_stub_missing_repository)
        }
    }

    private fun defaultAgentsUrl(): String {
        if (repositoryOwner.isBlank() || repositoryName.isBlank()) return ""
        return "https://github.com/$repositoryOwner/$repositoryName/copilot"
    }

    private fun copyDebugText(text: String) {
        if (text.isBlank()) return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.repository_agents_section_diagnostics), text))
        rootView?.let { view -> Snackbar.make(view, R.string.repository_section_native_stub_copied, Snackbar.LENGTH_SHORT).show() }
    }

    private fun openAgentsInGithub(url: String) {
        if (url.isBlank()) {
            rootView?.let { view -> Snackbar.make(view, R.string.repository_agents_missing_destination, Snackbar.LENGTH_SHORT).show() }
            return
        }
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure {
                rootView?.let { view -> Snackbar.make(view, R.string.repository_section_open_in_github_failed, Snackbar.LENGTH_SHORT).show() }
            }
    }

    private fun applyToolbarTitle() {
        view?.post {
            val actionBar = (activity as? AppCompatActivity)?.supportActionBar ?: return@post
            actionBar.title = getString(R.string.repository_section_agents)
            actionBar.subtitle = if (repositoryOwner.isNotBlank() && repositoryName.isNotBlank()) "$repositoryOwner/$repositoryName" else null
        }
    }

    companion object {
        const val ARG_OWNER = RepositoryHtmlSummarySectionFragment.ARG_OWNER
        const val ARG_REPO = RepositoryHtmlSummarySectionFragment.ARG_REPO
    }
}
