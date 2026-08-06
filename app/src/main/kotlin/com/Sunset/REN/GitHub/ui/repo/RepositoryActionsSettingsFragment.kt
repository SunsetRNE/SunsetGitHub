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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.github.html.RepositoryActionsCacheItem
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryActionsSettingsDialogState
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryActionsSettingsScreen
import com.google.android.material.snackbar.Snackbar

class RepositoryActionsSettingsFragment : Fragment() {

    private val viewModel: RepositoryActionsSettingsViewModel by viewModels()
    private var uiState by mutableStateOf(RepositoryActionsSettingsUiState())
    private var dialogState by mutableStateOf<RepositoryActionsSettingsDialogState?>(null)
    private var lastPendingMessage: String? = null
    private var actionsRootView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            actionsRootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryActionsSettingsScreen(
                        state = uiState,
                        onRetry = viewModel::refresh,
                        onSetActionsEnabled = ::confirmActionsEnabled,
                        onSetAllowedActions = viewModel::setAllowedActions,
                        onSetWorkflowPermission = ::setWorkflowPermission,
                        onToggleWorkflowPrApproval = ::toggleWorkflowPullRequestApproval,
                        onEditRetention = ::showRetentionDialog,
                        onRefreshSecretsVariables = viewModel::loadSecretsAndVariables,
                        onSecretClick = ::showSecretActionDialog,
                        onVariableClick = ::showVariableActionDialog,
                        onUpsertSecret = { showSecretDialog("") },
                        onUpsertVariable = { showVariableDialog("", "") },
                        onRefreshCaches = viewModel::loadCaches,
                        onDeleteCachesByKey = ::showDeleteCacheByKeyDialog,
                        onCacheClick = ::showCacheActionDialog,
                        onToggleGithubOwnedSelectedActions = ::toggleGithubOwnedSelectedActions,
                        onToggleVerifiedSelectedActions = ::toggleVerifiedSelectedActions,
                        onEditSelectedPatterns = ::showCurrentSelectedPatternsDialog,
                        dialogState = dialogState,
                        onDismissDialog = ::dismissDialog,
                        onConfirmActionsEnabled = ::setActionsEnabledFromDialog,
                        onConfirmWorkflowWrite = ::setWorkflowWriteFromDialog,
                        onSaveRetention = ::saveRetentionFromDialog,
                        onDeleteCache = ::deleteCacheFromDialog,
                        onDeleteCachesByKeyConfirmed = ::deleteCachesByKeyFromDialog,
                        onSaveSelectedPatterns = ::saveSelectedPatternsFromDialog,
                        onSaveSecret = ::saveSecretFromDialog,
                        onSaveVariable = ::saveVariableFromDialog,
                        onRequestSecretEdit = ::showSecretDialog,
                        onRequestSecretDelete = ::confirmDeleteSecret,
                        onRequestVariableEdit = ::showVariableDialog,
                        onRequestVariableDelete = ::confirmDeleteVariable,
                        onDeleteSecret = ::deleteSecretFromDialog,
                        onDeleteVariable = ::deleteVariableFromDialog
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val owner = arguments?.getString(ARG_OWNER).orEmpty()
        val repo = arguments?.getString(ARG_REPO).orEmpty()
        viewModel.actionsSettingsState.observe(viewLifecycleOwner) { state ->
            uiState = state
            showPendingSnackbarIfNeeded(state)
        }
        viewModel.prepare(owner, repo)
    }

    override fun onDestroyView() {
        actionsRootView = null
        super.onDestroyView()
    }

    private fun showPendingSnackbarIfNeeded(state: RepositoryActionsSettingsUiState) {
        val pending = state.pendingMessage?.takeIf { !state.isSaving && it.isNotBlank() }
        val view = actionsRootView ?: return
        if (!pending.isNullOrBlank() && pending != lastPendingMessage) {
            lastPendingMessage = pending
            Snackbar.make(view, pending, Snackbar.LENGTH_SHORT).show()
        }
        if (state.pendingMessage.isNullOrBlank()) lastPendingMessage = null
    }

    private fun dismissDialog() {
        dialogState = null
    }

    private fun setWorkflowPermission(permission: String) {
        if (permission == "write") {
            confirmWorkflowWrite()
        } else {
            viewModel.setWorkflowDefaultPermission(permission)
        }
    }

    private fun toggleWorkflowPullRequestApproval() {
        val approve = uiState.snapshot?.workflowPermissions?.canApprovePullRequestReviews == true
        viewModel.setWorkflowPullRequestApproval(!approve)
    }

    private fun toggleGithubOwnedSelectedActions() {
        val selected = uiState.snapshot?.selectedActions ?: return
        viewModel.setSelectedActions(!selected.githubOwnedAllowed, selected.verifiedAllowed, selected.patternsAllowed)
    }

    private fun toggleVerifiedSelectedActions() {
        val selected = uiState.snapshot?.selectedActions ?: return
        viewModel.setSelectedActions(selected.githubOwnedAllowed, !selected.verifiedAllowed, selected.patternsAllowed)
    }

    private fun showCurrentSelectedPatternsDialog() {
        val selected = uiState.snapshot?.selectedActions ?: return
        showSelectedPatternsDialog(selected.githubOwnedAllowed, selected.verifiedAllowed, selected.patternsAllowed)
    }

    private fun confirmActionsEnabled(enabled: Boolean) {
        dialogState = RepositoryActionsSettingsDialogState.ConfirmActionsEnabled(enabled)
    }

    private fun setActionsEnabledFromDialog(enabled: Boolean) {
        dismissDialog()
        viewModel.setActionsEnabled(enabled)
    }

    private fun confirmWorkflowWrite() {
        dialogState = RepositoryActionsSettingsDialogState.ConfirmWorkflowWrite
    }

    private fun setWorkflowWriteFromDialog() {
        dismissDialog()
        viewModel.setWorkflowDefaultPermission("write")
    }

    private fun showRetentionDialog(currentDays: Int?) {
        dialogState = RepositoryActionsSettingsDialogState.Retention(currentDays)
    }

    private fun saveRetentionFromDialog(days: String) {
        dismissDialog()
        viewModel.setRetentionDays(days)
    }

    private fun showCacheActionDialog(cache: RepositoryActionsCacheItem) {
        dialogState = RepositoryActionsSettingsDialogState.CacheAction(cache)
    }

    private fun deleteCacheFromDialog(cache: RepositoryActionsCacheItem) {
        dismissDialog()
        viewModel.deleteCache(cache)
    }

    private fun showDeleteCacheByKeyDialog() {
        dialogState = RepositoryActionsSettingsDialogState.DeleteCacheByKey
    }

    private fun deleteCachesByKeyFromDialog(key: String, ref: String) {
        dismissDialog()
        viewModel.deleteCachesByKey(key, ref)
    }

    private fun showSelectedPatternsDialog(githubOwned: Boolean, verified: Boolean, patterns: List<String>) {
        dialogState = RepositoryActionsSettingsDialogState.SelectedPatterns(githubOwned, verified, patterns)
    }

    private fun saveSelectedPatternsFromDialog(githubOwned: Boolean, verified: Boolean, patternsText: String) {
        dismissDialog()
        viewModel.setSelectedActions(githubOwned, verified, patternsText.lines())
    }

    private fun showVariableDialog(name: String, value: String) {
        dialogState = RepositoryActionsSettingsDialogState.VariableEditor(name, value)
    }

    private fun saveVariableFromDialog(name: String, value: String) {
        dismissDialog()
        viewModel.upsertVariable(name, value)
    }

    private fun showSecretDialog(name: String) {
        dialogState = RepositoryActionsSettingsDialogState.SecretEditor(name)
    }

    private fun saveSecretFromDialog(name: String, value: String) {
        dismissDialog()
        viewModel.upsertSecret(name, value)
    }

    private fun showSecretActionDialog(name: String) {
        dialogState = RepositoryActionsSettingsDialogState.SecretAction(name)
    }

    private fun showVariableActionDialog(name: String, value: String) {
        dialogState = RepositoryActionsSettingsDialogState.VariableAction(name, value)
    }

    private fun confirmDeleteSecret(name: String) {
        dialogState = RepositoryActionsSettingsDialogState.DeleteSecret(name)
    }

    private fun deleteSecretFromDialog(name: String) {
        dismissDialog()
        viewModel.deleteSecret(name)
    }

    private fun confirmDeleteVariable(name: String) {
        dialogState = RepositoryActionsSettingsDialogState.DeleteVariable(name)
    }

    private fun deleteVariableFromDialog(name: String) {
        dismissDialog()
        viewModel.deleteVariable(name)
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
    }
}
