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
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositorySectionNativeStubScreen
import com.google.android.material.snackbar.Snackbar

/**
 * Shared Compose host for repository sections whose native implementation is backed by GitHub
 * HTML/REST summaries. This keeps old XML-heavy section pages on a common Compose surface while
 * preserving bottom section navigation, retry, debug copy and "open in GitHub" actions.
 */
abstract class RepositoryHtmlSummarySectionFragment : RepositorySectionFragment() {

    private val viewModel: RepositorySectionNativeStubViewModel by viewModels()
    private var sectionState by mutableStateOf(RepositorySectionNativeStubUiState())
    private var rootView: View? = null
    private var sectionUrl: String = ""

    override var repositoryOwner: String = ""
    override var repositoryName: String = ""
    final override val selectedRepositorySection: RepositorySection
        get() = summarySection

    protected abstract val summarySection: RepositorySection

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        repositoryOwner = arguments?.getString(ARG_OWNER).orEmpty()
        repositoryName = arguments?.getString(ARG_REPO).orEmpty()
        sectionUrl = arguments?.getString(ARG_SECTION_URL).orEmpty().ifBlank { defaultSectionUrl() }
        sectionState = viewModel.sectionState.value ?: RepositorySectionNativeStubUiState(
            owner = repositoryOwner,
            repo = repositoryName,
            sectionKey = summarySection.storageKey
        )
        return ComposeView(requireContext()).apply {
            this@RepositoryHtmlSummarySectionFragment.rootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositorySectionNativeStubScreen(
                        state = sectionState,
                        sectionTitle = getString(summarySection.titleResId),
                        sectionFallbackDescription = sectionFallbackDescription(),
                        repositoryLabel = buildRepositoryLabel(),
                        initialSectionUrl = sectionUrl,
                        onRetry = viewModel::loadSection,
                        onOpenInGitHub = ::openSectionInGithub,
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
        viewModel.sectionState.observe(viewLifecycleOwner) { state ->
            sectionState = state
            state.sourceUrl?.takeIf { it.isNotBlank() }?.let { sectionUrl = it }
        }
        viewModel.prepare(repositoryOwner, repositoryName, summarySection.storageKey)
    }

    override fun onDestroyView() {
        rootView = null
        super.onDestroyView()
    }

    private fun sectionFallbackDescription(): String = when (summarySection) {
        RepositorySection.Projects -> getString(R.string.repository_section_native_stub_projects)
        RepositorySection.Insights -> getString(R.string.repository_section_native_stub_insights)
        RepositorySection.Wiki -> getString(R.string.repository_section_native_stub_wiki)
        RepositorySection.Agents -> getString(R.string.repository_section_native_stub_agents)
        RepositorySection.SecurityQuality -> getString(R.string.repository_section_native_stub_security_quality)
        RepositorySection.Settings -> getString(R.string.repository_section_native_stub_settings)
        else -> getString(R.string.repository_section_native_stub_generic)
    }

    private fun buildRepositoryLabel(): String = if (repositoryOwner.isNotBlank() && repositoryName.isNotBlank()) {
        getString(R.string.repository_section_native_stub_repository, repositoryOwner, repositoryName)
    } else {
        getString(R.string.repository_section_native_stub_missing_repository)
    }

    private fun copyDebugText(text: String) {
        if (text.isBlank()) return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.repository_section_native_stub_copy_debug_label), text))
        rootView?.let { view -> Snackbar.make(view, R.string.repository_section_native_stub_copied, Snackbar.LENGTH_SHORT).show() }
    }

    private fun openSectionInGithub(url: String) {
        if (url.isBlank()) {
            rootView?.let { view -> Snackbar.make(view, R.string.repository_section_native_stub_missing_url, Snackbar.LENGTH_SHORT).show() }
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
            actionBar.title = getString(summarySection.titleResId)
            actionBar.subtitle = if (repositoryOwner.isNotBlank() && repositoryName.isNotBlank()) "$repositoryOwner/$repositoryName" else null
        }
    }

    private fun defaultSectionUrl(): String {
        if (repositoryOwner.isBlank() || repositoryName.isBlank()) return ""
        val path = when (summarySection) {
            RepositorySection.Projects -> "projects"
            RepositorySection.Insights -> "pulse"
            RepositorySection.Wiki -> "wiki"
            RepositorySection.Agents -> "agents"
            RepositorySection.SecurityQuality -> "security"
            RepositorySection.Settings -> "settings"
            else -> summarySection.storageKey
        }
        return "https://github.com/$repositoryOwner/$repositoryName/$path"
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
        const val ARG_SECTION_URL = "section_url"
    }
}
