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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositorySectionNativeStubScreen
import com.google.android.material.snackbar.Snackbar

class RepositorySectionNativeStubFragment : Fragment() {

    private val viewModel: RepositorySectionNativeStubViewModel by viewModels()
    private var sectionState by mutableStateOf(RepositorySectionNativeStubUiState())
    private var rootView: View? = null

    private var owner: String = ""
    private var repo: String = ""
    private var sectionKey: String = ""
    private var sectionUrl: String = ""
    private var section: RepositorySection = RepositorySection.Code

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        owner = arguments?.getString(ARG_OWNER).orEmpty()
        repo = arguments?.getString(ARG_REPO).orEmpty()
        sectionKey = arguments?.getString(ARG_SECTION).orEmpty()
        sectionUrl = arguments?.getString(ARG_SECTION_URL).orEmpty()
        section = RepositorySection.fromStorageKey(sectionKey) ?: RepositorySection.Code
        sectionState = viewModel.sectionState.value ?: RepositorySectionNativeStubUiState(
            owner = owner,
            repo = repo,
            sectionKey = sectionKey
        )
        return ComposeView(requireContext()).apply {
            this@RepositorySectionNativeStubFragment.rootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositorySectionNativeStubScreen(
                        state = sectionState,
                        sectionTitle = getString(section.titleResId),
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
        applyToolbarTitle()
        viewModel.sectionState.observe(viewLifecycleOwner) { state ->
            sectionState = state
            state.sourceUrl?.takeIf { it.isNotBlank() }?.let { sectionUrl = it }
        }
        viewModel.prepare(owner, repo, sectionKey)
    }

    override fun onDestroyView() {
        rootView = null
        super.onDestroyView()
    }

    private fun copyDebugText(text: String) {
        if (text.isBlank()) return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.repository_section_native_stub_copy_debug_label), text))
        rootView?.let { view ->
            Snackbar.make(view, R.string.repository_section_native_stub_copied, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun openSectionInGithub(url: String) {
        if (url.isBlank()) {
            rootView?.let { view ->
                Snackbar.make(view, R.string.repository_section_native_stub_missing_url, Snackbar.LENGTH_SHORT).show()
            }
            return
        }
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            rootView?.let { view ->
                Snackbar.make(view, R.string.repository_section_open_in_github_failed, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun sectionFallbackDescription(): String {
        return when (section) {
            RepositorySection.Projects -> getString(R.string.repository_section_native_stub_projects)
            RepositorySection.SecurityQuality -> getString(R.string.repository_section_native_stub_security_quality)
            RepositorySection.Insights -> getString(R.string.repository_section_native_stub_insights)
            RepositorySection.Wiki -> getString(R.string.repository_section_native_stub_wiki)
            RepositorySection.Agents -> getString(R.string.repository_section_native_stub_agents)
            RepositorySection.Settings -> getString(R.string.repository_section_native_stub_settings)
            else -> getString(R.string.repository_section_native_stub_generic)
        }
    }

    private fun buildRepositoryLabel(): String {
        return if (owner.isNotBlank() && repo.isNotBlank()) {
            getString(R.string.repository_section_native_stub_repository, owner, repo)
        } else {
            getString(R.string.repository_section_native_stub_missing_repository)
        }
    }

    private fun applyToolbarTitle() {
        view?.post {
            val actionBar = (activity as? AppCompatActivity)?.supportActionBar ?: return@post
            actionBar.title = getString(section.titleResId)
            actionBar.subtitle = if (owner.isNotBlank() && repo.isNotBlank()) "$owner/$repo" else null
        }
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
        const val ARG_SECTION = "section"
        const val ARG_SECTION_URL = "section_url"
    }
}