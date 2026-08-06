package com.Sunset.REN.GitHub.ui.repo

import android.app.DownloadManager
import android.content.ActivityNotFoundException
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
import androidx.fragment.app.viewModels
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionArtifact
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryActionRunDetailScreen
import com.google.android.material.snackbar.Snackbar

class RepositoryActionRunDetailFragment : Fragment() {

    private val viewModel: RepositoryActionRunDetailViewModel by viewModels()
    private var detailState by mutableStateOf(RepositoryActionRunDetailUiState())
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        detailState = viewModel.detailState.value ?: RepositoryActionRunDetailUiState()
        return ComposeView(requireContext()).apply {
            this@RepositoryActionRunDetailFragment.rootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryActionRunDetailScreen(
                        state = detailState,
                        onRetry = viewModel::load,
                        onOpenActions = ::openActionsInGithub,
                        onOpenRun = ::openInGithub,
                        onRefreshLogs = viewModel::refreshLogs,
                        onDownloadLogs = ::downloadLogsArchive,
                        onDownloadArtifact = ::downloadArtifact
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.detailState.observe(viewLifecycleOwner) { state -> detailState = state }
        viewModel.prepare(
            owner = arguments?.getString(ARG_OWNER).orEmpty(),
            repo = arguments?.getString(ARG_REPO).orEmpty(),
            runId = arguments?.getLong(ARG_RUN_ID) ?: 0L
        )
    }

    override fun onDestroyView() {
        rootView = null
        super.onDestroyView()
    }

    private fun downloadArtifact(artifact: RepositoryActionArtifact) {
        val fileName = sanitizeDownloadFileName(artifact.name).let { name ->
            if (name.endsWith(".zip", ignoreCase = true)) name else "$name.zip"
        }
        startDownload(artifact.archiveDownloadUrl.orEmpty(), fileName, artifact.name)
    }

    private fun startDownload(url: String, rawFileName: String, description: String) {
        if (url.isBlank()) return
        val fileName = sanitizeDownloadFileName(rawFileName)
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription(description)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .addRequestHeader("Accept", "application/vnd.github+json")
                .addRequestHeader("X-GitHub-Api-Version", "2026-03-10")
                .addRequestHeader("User-Agent", "SunsetGitHub-Android")
            viewModel.currentAuthorizationHeader()?.let { authHeader ->
                request.addRequestHeader("Authorization", authHeader)
            }
            val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(
                requireContext(),
                getString(R.string.repository_action_run_detail_download_started, fileName),
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {
            rootView?.let { view ->
                Snackbar.make(view, R.string.repository_action_run_detail_download_failed, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun sanitizeDownloadFileName(raw: String): String {
        return raw.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "artifact" }
    }

    private fun openInGithub(htmlUrl: String) {
        if (htmlUrl.isBlank()) return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(htmlUrl)))
        } catch (_: ActivityNotFoundException) {
            rootView?.let { view ->
                Snackbar.make(view, R.string.repository_action_run_detail_open_failed, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun openActionsInGithub() {
        val url = viewModel.detailState.value?.actionsHtmlUrl ?: return
        openInGithub(url)
    }

    private fun downloadLogsArchive() {
        val state = viewModel.detailState.value ?: return
        val actionRun = state.actionRun ?: return
        val url = actionRun.logsUrl?.takeIf { it.isNotBlank() } ?: return
        val fileName = "actions-run-${state.runId}-logs.zip"
        startDownload(url, fileName, getString(R.string.repository_action_run_detail_logs_title))
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
        const val ARG_RUN_ID = "run_id"
    }
}
