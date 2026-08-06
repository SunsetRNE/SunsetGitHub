package com.Sunset.REN.GitHub.ui.repo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityAlert
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositorySecurityScreen
import com.Sunset.REN.GitHub.util.BrowserLauncher
import com.google.android.material.snackbar.Snackbar

class RepositorySecurityFragment : RepositorySectionFragment() {

    private val viewModel: RepositorySecurityViewModel by viewModels()
    override var repositoryOwner: String = ""
    override var repositoryName: String = ""
    override val selectedRepositorySection: RepositorySection = RepositorySection.SecurityQuality

    private var securityState by mutableStateOf(RepositorySecurityUiState())
    private var selectedSecurityPanel by mutableStateOf(OverviewPanelKey)
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        repositoryOwner = arguments?.getString(ARG_OWNER).orEmpty()
        repositoryName = arguments?.getString(ARG_REPO).orEmpty()
        securityState = viewModel.securityState.value ?: RepositorySecurityUiState(
            owner = repositoryOwner,
            repo = repositoryName
        )
        return ComposeView(requireContext()).apply {
            this@RepositorySecurityFragment.rootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositorySecurityScreen(
                        state = securityState,
                        selectedPanelKey = selectedSecurityPanel,
                        onRetry = viewModel::refresh,
                        onSelectPanel = ::selectSecurityPanel,
                        onSelectAlertType = ::selectAlertType,
                        onSelectAlertState = viewModel::switchAlertState,
                        onLoadMoreAlerts = viewModel::loadNextAlertPage,
                        onOpenUrl = ::openSecurityUrl,
                        onOpenAlert = ::openAlertDetail
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderRepositorySectionNavigation()
        viewModel.securityState.observe(viewLifecycleOwner) { state ->
            securityState = state
        }
        viewModel.prepare(repositoryOwner, repositoryName)
    }

    override fun onDestroyView() {
        rootView = null
        super.onDestroyView()
    }

    private fun selectSecurityPanel(panelKey: String) {
        selectedSecurityPanel = panelKey
        if (panelKey in AlertPanelKeys) {
            viewModel.switchAlertType(panelKey)
        }
    }

    private fun selectAlertType(alertType: String) {
        selectedSecurityPanel = alertType
        viewModel.switchAlertType(alertType)
    }

    private fun openSecurityUrl(url: String) {
        if (!BrowserLauncher.open(requireContext(), url)) {
            rootView?.let { view ->
                Snackbar.make(view, R.string.repository_section_open_in_github_failed, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun openAlertDetail(alert: RepositorySecurityAlert) {
        val destinationId = resources.getIdentifier(
            SecurityAlertDetailDestinationName,
            ResourceTypeId,
            requireContext().packageName
        )
        if (destinationId == 0) {
            rootView?.let { view ->
                Snackbar.make(
                    view,
                    getString(R.string.repository_security_alert_detail_missing_destination),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            return
        }
        findNavController().navigate(
            destinationId,
            Bundle().apply {
                putString(RepositorySecurityAlertDetailFragment.ARG_OWNER, viewModel.securityState.value?.owner.orEmpty())
                putString(RepositorySecurityAlertDetailFragment.ARG_REPO, viewModel.securityState.value?.repo.orEmpty())
                putString(RepositorySecurityAlertDetailFragment.ARG_ALERT_TYPE, viewModel.securityState.value?.alertFilter?.alertType.orEmpty())
                putInt(RepositorySecurityAlertDetailFragment.ARG_NUMBER, alert.number ?: 0)
                putString(RepositorySecurityAlertDetailFragment.ARG_TITLE, alert.title)
                putString(RepositorySecurityAlertDetailFragment.ARG_SOURCE, alert.source)
                putString(RepositorySecurityAlertDetailFragment.ARG_STATE, alert.state)
                putString(RepositorySecurityAlertDetailFragment.ARG_SEVERITY, alert.severity.orEmpty())
                putString(RepositorySecurityAlertDetailFragment.ARG_CREATED_AT, alert.createdAt.orEmpty())
                putString(RepositorySecurityAlertDetailFragment.ARG_HTML_URL, alert.htmlUrl.orEmpty())
                putStringArrayList(
                    RepositorySecurityAlertDetailFragment.ARG_DETAILS,
                    ArrayList(alert.details)
                )
            }
        )
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
        private const val SecurityAlertDetailDestinationName = "repository_security_alert_detail_fragment"
        private const val ResourceTypeId = "id"
        private const val OverviewPanelKey = "security_overview"
        private val AlertPanelKeys = setOf(
            RepositorySecurityUiState.AlertTypeDependabot,
            RepositorySecurityUiState.AlertTypeCodeScanning,
            RepositorySecurityUiState.AlertTypeSecretScanning
        )
    }
}
