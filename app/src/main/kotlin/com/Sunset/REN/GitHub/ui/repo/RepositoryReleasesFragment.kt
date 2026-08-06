package com.Sunset.REN.GitHub.ui.repo

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryReleasesScreen
import com.google.android.material.snackbar.Snackbar

class RepositoryReleasesFragment : Fragment() {

    private lateinit var viewModel: RepositoryReleasesViewModel
    private var releasesState by mutableStateOf(RepositoryReleasesUiState())
    private var repositoryOwner: String = ""
    private var repositoryName: String = ""
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[RepositoryReleasesViewModel::class.java]
        repositoryOwner = requireArguments().getString(ARG_OWNER).orEmpty()
        repositoryName = requireArguments().getString(ARG_REPO).orEmpty()
        releasesState = viewModel.releasesState.value ?: RepositoryReleasesUiState(owner = repositoryOwner, repo = repositoryName)
        return ComposeView(requireContext()).apply {
            this@RepositoryReleasesFragment.rootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryReleasesScreen(
                        state = releasesState,
                        onRetry = viewModel::loadFirstPage,
                        onLoadMore = viewModel::loadNextPage,
                        onOpenRelease = ::openReleaseInGithub,
                        onDownload = ::startDownload
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.releasesState.observe(viewLifecycleOwner) { state -> releasesState = state }
        viewModel.prepare(repositoryOwner, repositoryName)
    }

    override fun onDestroyView() {
        rootView = null
        super.onDestroyView()
    }

    private fun openReleaseInGithub(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { rootView?.let { Snackbar.make(it, R.string.repository_releases_missing_destination, Snackbar.LENGTH_SHORT).show() } }
    }

    private fun startDownload(url: String, rawFileName: String) {
        if (url.isBlank()) return
        val fileName = rawFileName.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "download" }
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .addRequestHeader("Accept", "application/vnd.github+json")
                .addRequestHeader("X-GitHub-Api-Version", "2026-03-10")
                .addRequestHeader("User-Agent", "SunsetGitHub-Android")
            viewModel.currentAuthorizationHeader()?.let { authorization ->
                request.addRequestHeader("Authorization", authorization)
            }
            val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(requireContext(), getString(R.string.repository_releases_download_started, fileName), Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            rootView?.let { Snackbar.make(it, R.string.repository_releases_download_failed, Snackbar.LENGTH_SHORT).show() }
        }
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
    }
}