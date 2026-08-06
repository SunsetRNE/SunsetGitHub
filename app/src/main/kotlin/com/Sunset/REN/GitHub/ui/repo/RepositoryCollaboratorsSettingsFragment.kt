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
import com.Sunset.REN.GitHub.data.github.html.RepositoryCollaboratorPermission
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryCollaboratorsSettingsScreen
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryConfirmDialog
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryPermissionPickerDialog
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryTextInputDialog
import com.google.android.material.snackbar.Snackbar

class RepositoryCollaboratorsSettingsFragment : Fragment() {

    private val viewModel: RepositoryCollaboratorsSettingsViewModel by viewModels()
    private var uiState by mutableStateOf(RepositoryCollaboratorsSettingsUiState())
    private var rootView: View? = null
    private var lastPendingMessage: String? = null
    private var dialogState by mutableStateOf<CollaboratorsDialogState?>(null)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            this@RepositoryCollaboratorsSettingsFragment.rootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryCollaboratorsSettingsScreen(
                        state = uiState,
                        onRetry = viewModel::refresh,
                        onInvite = { showInviteDialog() },
                        onSelectCollaborator = viewModel::selectCollaborator,
                        onChangePermission = ::showPermissionDialog,
                        onRemoveCollaborator = ::confirmRemove,
                        onCancelInvitation = ::confirmCancelInvitation
                    )
                    CollaboratorsDialogHost()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val owner = arguments?.getString(ARG_OWNER).orEmpty()
        val repo = arguments?.getString(ARG_REPO).orEmpty()
        viewModel.collaboratorsState.observe(viewLifecycleOwner) { state ->
            uiState = state
            showPendingSnackbarIfNeeded(state)
        }
        viewModel.prepare(owner, repo)
    }

    override fun onDestroyView() {
        rootView = null
        super.onDestroyView()
    }

    private fun showPendingSnackbarIfNeeded(state: RepositoryCollaboratorsSettingsUiState) {
        val pending = state.pendingMessage?.takeIf { !state.isSaving && it.isNotBlank() }
        val view = rootView ?: return
        if (!pending.isNullOrBlank() && pending != lastPendingMessage) {
            lastPendingMessage = pending
            Snackbar.make(view, pending, Snackbar.LENGTH_SHORT).show()
        }
        if (state.pendingMessage.isNullOrBlank()) lastPendingMessage = null
    }

    private fun showInviteDialog(usernamePrefill: String = "") {
        dialogState = CollaboratorsDialogState.InviteUsername(usernamePrefill)
    }

    private fun showInvitePermissionDialog(username: String) {
        dialogState = CollaboratorsDialogState.InvitePermission(username)
    }

    private fun showInviteConfirmDialog(username: String, permission: RepositoryCollaboratorPermission) {
        dialogState = CollaboratorsDialogState.InviteConfirm(username, permission)
    }

    private fun showPermissionDialog(login: String) {
        val current = viewModel.collaboratorsState.value?.snapshot?.collaborators?.firstOrNull { it.login == login }?.permission
        dialogState = CollaboratorsDialogState.ChangePermission(login, current)
    }

    private fun confirmRemove(login: String) {
        dialogState = CollaboratorsDialogState.RemoveCollaborator(login)
    }

    private fun confirmCancelInvitation(row: RepositoryCollaboratorInvitationRow) {
        dialogState = CollaboratorsDialogState.CancelInvitation(row)
    }

    @androidx.compose.runtime.Composable
    private fun CollaboratorsDialogHost() {
        val permissions = RepositoryCollaboratorPermission.values().toList()
        when (val dialog = dialogState) {
            null -> Unit
            is CollaboratorsDialogState.InviteUsername -> RepositoryTextInputDialog(
                title = getString(R.string.repository_collaborators_settings_invite),
                label = getString(R.string.repository_collaborators_settings_username_hint),
                helperText = getString(R.string.repository_collaborators_settings_username_helper),
                initialValue = dialog.prefill,
                confirmText = getString(R.string.repository_collaborators_settings_next),
                dismissText = getString(android.R.string.cancel),
                requiredErrorText = getString(R.string.repository_collaborators_settings_username_required),
                normalizeValue = { it.trim().trimStart('@') },
                onDismiss = { dialogState = null },
                onConfirm = ::showInvitePermissionDialog
            )
            is CollaboratorsDialogState.InvitePermission -> RepositoryPermissionPickerDialog(
                title = getString(R.string.repository_collaborators_settings_invite_permission_title, dialog.username),
                message = getString(R.string.repository_collaborators_settings_invite_permission_message),
                permissions = permissions,
                initialPermission = RepositoryCollaboratorPermission.Push,
                confirmText = getString(R.string.repository_collaborators_settings_next),
                dismissText = getString(R.string.repository_collaborators_settings_back),
                permissionDescription = { it.description() },
                onDismiss = { showInviteDialog(dialog.username) },
                onConfirm = { showInviteConfirmDialog(dialog.username, it) }
            )
            is CollaboratorsDialogState.InviteConfirm -> RepositoryConfirmDialog(
                title = getString(R.string.repository_collaborators_settings_invite_confirm_title),
                message = getString(R.string.repository_collaborators_settings_invite_confirm_message, dialog.username, uiState.owner, uiState.repo, dialog.permission.displayName),
                confirmText = getString(R.string.repository_collaborators_settings_send_invite),
                dismissText = getString(R.string.repository_collaborators_settings_back),
                onDismiss = { showInvitePermissionDialog(dialog.username) },
                onConfirm = {
                    dialogState = null
                    viewModel.inviteCollaborator(dialog.username, dialog.permission)
                }
            )
            is CollaboratorsDialogState.ChangePermission -> RepositoryPermissionPickerDialog(
                title = getString(R.string.repository_collaborators_settings_change_permission_title, dialog.login),
                permissions = permissions,
                initialPermission = dialog.current ?: RepositoryCollaboratorPermission.Pull,
                confirmText = getString(R.string.repository_collaborators_settings_confirm),
                dismissText = getString(android.R.string.cancel),
                permissionDescription = { it.description() },
                onDismiss = { dialogState = null },
                onConfirm = { permission ->
                    dialogState = null
                    if (permission != dialog.current) {
                        viewModel.updateSelectedPermission(permission)
                    } else {
                        rootView?.let { Snackbar.make(it, R.string.repository_collaborators_settings_permission_unchanged, Snackbar.LENGTH_SHORT).show() }
                    }
                }
            )
            is CollaboratorsDialogState.RemoveCollaborator -> RepositoryConfirmDialog(
                title = getString(R.string.repository_collaborators_settings_remove_confirm_title),
                message = getString(R.string.repository_collaborators_settings_remove_confirm_message, dialog.login),
                confirmText = getString(R.string.repository_collaborators_settings_remove),
                dismissText = getString(android.R.string.cancel),
                onDismiss = { dialogState = null },
                onConfirm = {
                    dialogState = null
                    viewModel.removeCollaborator(dialog.login)
                }
            )
            is CollaboratorsDialogState.CancelInvitation -> RepositoryConfirmDialog(
                title = getString(R.string.repository_collaborators_settings_cancel_invitation_title),
                message = getString(R.string.repository_collaborators_settings_cancel_invitation_message, dialog.row.displayName),
                confirmText = getString(R.string.repository_collaborators_settings_cancel_invitation_action),
                dismissText = getString(android.R.string.cancel),
                onDismiss = { dialogState = null },
                onConfirm = {
                    dialogState = null
                    viewModel.cancelInvitation(dialog.row.id)
                }
            )
        }
    }

    private fun RepositoryCollaboratorPermission.description(): String = when (this) {
        RepositoryCollaboratorPermission.Pull -> getString(R.string.repository_collaborators_settings_permission_pull_description)
        RepositoryCollaboratorPermission.Triage -> getString(R.string.repository_collaborators_settings_permission_triage_description)
        RepositoryCollaboratorPermission.Push -> getString(R.string.repository_collaborators_settings_permission_push_description)
        RepositoryCollaboratorPermission.Maintain -> getString(R.string.repository_collaborators_settings_permission_maintain_description)
        RepositoryCollaboratorPermission.Admin -> getString(R.string.repository_collaborators_settings_permission_admin_description)
    }

    private sealed interface CollaboratorsDialogState {
        data class InviteUsername(val prefill: String) : CollaboratorsDialogState
        data class InvitePermission(val username: String) : CollaboratorsDialogState
        data class InviteConfirm(val username: String, val permission: RepositoryCollaboratorPermission) : CollaboratorsDialogState
        data class ChangePermission(val login: String, val current: RepositoryCollaboratorPermission?) : CollaboratorsDialogState
        data class RemoveCollaborator(val login: String) : CollaboratorsDialogState
        data class CancelInvitation(val row: RepositoryCollaboratorInvitationRow) : CollaboratorsDialogState
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
    }
}