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
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryBranchProtectionEditorDialog
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryBranchProtectionEditorLabels
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryBranchSettingsScreen
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryConfirmDialog
import com.google.android.material.snackbar.Snackbar

class RepositoryBranchSettingsFragment : Fragment() {

    private val viewModel: RepositoryBranchSettingsViewModel by viewModels()
    private var uiState by mutableStateOf(RepositoryBranchSettingsUiState())
    private var rootView: View? = null
    private var lastPendingMessage: String? = null
    private var dialogState by mutableStateOf<BranchSettingsDialogState?>(null)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            this@RepositoryBranchSettingsFragment.rootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryBranchSettingsScreen(
                        state = uiState,
                        onRetry = viewModel::refresh,
                        onSelectBranch = viewModel::loadProtection,
                        onEditProtection = ::showProtectionEditor,
                        onDeleteProtection = ::confirmDeleteProtection
                    )
                    BranchSettingsDialogHost()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val owner = arguments?.getString(ARG_OWNER).orEmpty()
        val repo = arguments?.getString(ARG_REPO).orEmpty()
        viewModel.branchSettingsState.observe(viewLifecycleOwner) { state ->
            uiState = state
            showPendingSnackbarIfNeeded(state)
        }
        viewModel.prepare(owner, repo)
    }

    override fun onDestroyView() {
        rootView = null
        super.onDestroyView()
    }

    private fun showPendingSnackbarIfNeeded(state: RepositoryBranchSettingsUiState) {
        val pendingMessage = state.pendingMessage?.takeIf { !state.isSaving && it.isNotBlank() }
        val view = rootView ?: return
        if (!pendingMessage.isNullOrBlank() && pendingMessage != lastPendingMessage) {
            lastPendingMessage = pendingMessage
            Snackbar.make(view, pendingMessage, Snackbar.LENGTH_SHORT).show()
        }
        if (state.pendingMessage.isNullOrBlank()) lastPendingMessage = null
    }

    private fun showProtectionEditor(branch: String) {
        dialogState = BranchSettingsDialogState.ProtectionEditor(branch)
    }

    private fun confirmDeleteProtection(branch: String) {
        dialogState = BranchSettingsDialogState.DeleteProtection(branch)
    }

    @androidx.compose.runtime.Composable
    private fun BranchSettingsDialogHost() {
        when (val dialog = dialogState) {
            null -> Unit
            is BranchSettingsDialogState.ProtectionEditor -> RepositoryBranchProtectionEditorDialog(
                branch = dialog.branch,
                current = viewModel.branchSettingsState.value?.selectedProtection,
                title = getString(R.string.repository_branch_settings_editor_title, dialog.branch),
                message = getString(R.string.repository_branch_settings_editor_message),
                saveText = getString(R.string.repository_settings_save_action),
                cancelText = getString(android.R.string.cancel),
                labels = branchProtectionEditorLabels(),
                onDismiss = { dialogState = null },
                onSave = { request ->
                    dialogState = null
                    viewModel.updateProtection(dialog.branch, request)
                }
            )
            is BranchSettingsDialogState.DeleteProtection -> RepositoryConfirmDialog(
                title = getString(R.string.repository_branch_settings_delete_confirm_title),
                message = getString(R.string.repository_branch_settings_delete_confirm_message, dialog.branch),
                confirmText = getString(R.string.repository_branch_settings_delete_protection),
                dismissText = getString(android.R.string.cancel),
                onDismiss = { dialogState = null },
                onConfirm = {
                    dialogState = null
                    viewModel.deleteProtection(dialog.branch)
                }
            )
        }
    }

    private fun branchProtectionEditorLabels(): RepositoryBranchProtectionEditorLabels {
        return RepositoryBranchProtectionEditorLabels(
            reviews = getString(R.string.repository_branch_settings_reviews_hint),
            checks = getString(R.string.repository_branch_settings_checks_hint),
            users = getString(R.string.repository_branch_settings_users_hint),
            teams = getString(R.string.repository_branch_settings_teams_hint),
            apps = getString(R.string.repository_branch_settings_apps_hint),
            enforceAdmins = getString(R.string.repository_branch_settings_enforce_admins),
            dismissStaleReviews = getString(R.string.repository_branch_settings_dismiss_stale_reviews),
            requireCodeOwnerReviews = getString(R.string.repository_branch_settings_require_code_owner_reviews),
            requireLastPushApproval = getString(R.string.repository_branch_settings_require_last_push_approval),
            requireLinearHistory = getString(R.string.repository_branch_settings_require_linear_history),
            requireConversationResolution = getString(R.string.repository_branch_settings_require_conversation_resolution),
            requireSignedCommits = getString(R.string.repository_branch_settings_require_signed_commits),
            allowForcePushes = getString(R.string.repository_branch_settings_allow_force_pushes),
            allowDeletions = getString(R.string.repository_branch_settings_allow_deletions)
        )
    }

    private sealed interface BranchSettingsDialogState {
        data class ProtectionEditor(val branch: String) : BranchSettingsDialogState
        data class DeleteProtection(val branch: String) : BranchSettingsDialogState
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
    }
}
