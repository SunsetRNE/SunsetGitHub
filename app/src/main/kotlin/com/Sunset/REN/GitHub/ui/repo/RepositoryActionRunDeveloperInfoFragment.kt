package com.Sunset.REN.GitHub.ui.repo

import android.content.ActivityNotFoundException
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryActionRunDeveloperInfoScreen
import com.google.android.material.snackbar.Snackbar

class RepositoryActionRunDeveloperInfoFragment : Fragment() {

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
            this@RepositoryActionRunDeveloperInfoFragment.rootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryActionRunDeveloperInfoScreen(
                        state = detailState,
                        onRetry = viewModel::load,
                        onOpenActions = ::openActionsInGithub
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.detailState.observe(viewLifecycleOwner) { state -> detailState = state }
        viewModel.prepare(
            owner = arguments?.getString(RepositoryActionRunDetailFragment.ARG_OWNER).orEmpty(),
            repo = arguments?.getString(RepositoryActionRunDetailFragment.ARG_REPO).orEmpty(),
            runId = arguments?.getLong(RepositoryActionRunDetailFragment.ARG_RUN_ID) ?: 0L
        )
    }

    override fun onDestroyView() {
        rootView = null
        super.onDestroyView()
    }

    private fun openActionsInGithub() {
        val url = viewModel.detailState.value?.actionsHtmlUrl ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            rootView?.let { view ->
                Snackbar.make(view, R.string.repository_action_run_detail_open_failed, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}