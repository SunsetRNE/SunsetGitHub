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
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityAlert
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositorySecurityAlertDetailScreen
import com.google.android.material.snackbar.Snackbar

class RepositorySecurityAlertDetailFragment : Fragment() {

    private val viewModel: RepositorySecurityAlertDetailViewModel by viewModels()
    private var detailState by mutableStateOf(RepositorySecurityAlertDetailUiState())
    private lateinit var initialAlert: RepositorySecurityAlert
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initialAlert = createInitialAlert()
        detailState = viewModel.detailState.value ?: RepositorySecurityAlertDetailUiState(alert = initialAlert)
        return ComposeView(requireContext()).apply {
            this@RepositorySecurityAlertDetailFragment.rootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositorySecurityAlertDetailScreen(
                        state = detailState,
                        initialAlert = initialAlert,
                        onRetry = viewModel::load,
                        onOpenInGithub = ::openInGithub
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
            alertType = arguments?.getString(ARG_ALERT_TYPE).orEmpty(),
            number = arguments?.getInt(ARG_NUMBER) ?: 0,
            initialAlert = initialAlert
        )
    }

    override fun onDestroyView() {
        rootView = null
        super.onDestroyView()
    }

    private fun createInitialAlert(): RepositorySecurityAlert = RepositorySecurityAlert(
        number = arguments?.getInt(ARG_NUMBER)?.takeIf { it > 0 },
        source = arguments?.getString(ARG_SOURCE).orEmpty(),
        title = arguments?.getString(ARG_TITLE).orEmpty(),
        state = arguments?.getString(ARG_STATE).orEmpty(),
        severity = arguments?.getString(ARG_SEVERITY)?.takeIf { it.isNotBlank() },
        createdAt = arguments?.getString(ARG_CREATED_AT)?.takeIf { it.isNotBlank() },
        htmlUrl = arguments?.getString(ARG_HTML_URL)?.takeIf { it.isNotBlank() },
        details = arguments?.getStringArrayList(ARG_DETAILS).orEmpty()
    )

    private fun openInGithub(htmlUrl: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(htmlUrl)))
        } catch (_: ActivityNotFoundException) {
            rootView?.let { view ->
                Snackbar.make(view, R.string.repository_security_alert_detail_open_failed, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
        const val ARG_ALERT_TYPE = "alert_type"
        const val ARG_NUMBER = "number"
        const val ARG_TITLE = "title"
        const val ARG_SOURCE = "source"
        const val ARG_STATE = "state"
        const val ARG_SEVERITY = "severity"
        const val ARG_CREATED_AT = "created_at"
        const val ARG_HTML_URL = "html_url"
        const val ARG_DETAILS = "details"
    }
}