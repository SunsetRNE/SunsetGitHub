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
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryConfirmDialog
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryDangerZoneScreen
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryTextInputDialog
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryTwoTextInputDialog
import com.google.android.material.snackbar.Snackbar

class RepositoryDangerZoneFragment : Fragment() {

    private val viewModel: RepositoryDangerZoneViewModel by viewModels()
    private var uiState by mutableStateOf(RepositoryDangerZoneUiState())
    private var lastPendingMessage: String? = null
    private var dangerZoneRootView: View? = null
    private var dialogState by mutableStateOf<DangerZoneDialogState?>(null)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            dangerZoneRootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryDangerZoneScreen(
                        state = uiState,
                        onRetry = viewModel::refresh,
                        onArchiveClick = ::confirmArchive,
                        onTransferClick = ::showTransferConfirmation,
                        onDeleteClick = ::showDeleteConfirmation
                    )
                    DangerZoneDialogHost()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.state.observe(viewLifecycleOwner) { state ->
            uiState = state
            showPendingSnackbarIfNeeded(state)
        }
        viewModel.prepare(arguments?.getString(ARG_OWNER).orEmpty(), arguments?.getString(ARG_REPO).orEmpty())
    }

    override fun onDestroyView() {
        dangerZoneRootView = null
        super.onDestroyView()
    }

    private fun showPendingSnackbarIfNeeded(state: RepositoryDangerZoneUiState) {
        val pending = state.pendingMessage?.takeIf { !state.isSaving && it.isNotBlank() }
        val view = dangerZoneRootView ?: return
        if (!pending.isNullOrBlank() && pending != lastPendingMessage) {
            lastPendingMessage = pending
            Snackbar.make(view, pending, Snackbar.LENGTH_SHORT).show()
        }
        if (state.pendingMessage.isNullOrBlank()) lastPendingMessage = null
    }

    private fun confirmArchive(archive: Boolean) {
        dialogState = DangerZoneDialogState.Archive(archive)
    }

    private fun showTransferConfirmation(fullName: String) {
        dialogState = DangerZoneDialogState.Transfer(fullName)
    }

    private fun showDeleteConfirmation(fullName: String) {
        dialogState = DangerZoneDialogState.Delete(fullName)
    }

    @androidx.compose.runtime.Composable
    private fun DangerZoneDialogHost() {
        when (val dialog = dialogState) {
            null -> Unit
            is DangerZoneDialogState.Archive -> RepositoryConfirmDialog(
                title = getString(
                    if (dialog.archive) {
                        R.string.repository_danger_zone_archive_action
                    } else {
                        R.string.repository_danger_zone_unarchive_action
                    }
                ),
                message = getString(
                    if (dialog.archive) {
                        R.string.repository_danger_zone_archive_confirm_message
                    } else {
                        R.string.repository_danger_zone_unarchive_confirm_message
                    }
                ),
                confirmText = getString(
                    if (dialog.archive) {
                        R.string.repository_danger_zone_archive_confirm_positive
                    } else {
                        R.string.repository_danger_zone_unarchive_confirm_positive
                    }
                ),
                dismissText = getString(android.R.string.cancel),
                onDismiss = { dialogState = null },
                onConfirm = {
                    dialogState = null
                    viewModel.setArchived(dialog.archive)
                }
            )
            is DangerZoneDialogState.Transfer -> RepositoryTwoTextInputDialog(
                title = getString(R.string.repository_danger_zone_transfer_action),
                message = getString(R.string.repository_danger_zone_transfer_confirm_message),
                firstLabel = getString(R.string.repository_danger_zone_transfer_owner_label),
                secondLabel = getString(R.string.repository_danger_zone_confirm_full_name_label, dialog.fullName),
                confirmText = getString(R.string.repository_danger_zone_transfer_confirm_positive),
                dismissText = getString(android.R.string.cancel),
                firstRequiredErrorText = getString(R.string.repository_danger_zone_transfer_owner_hint),
                secondRequiredErrorText = dialog.fullName,
                onDismiss = { dialogState = null },
                onConfirm = { newOwner, confirmedFullName ->
                    dialogState = null
                    viewModel.transferRepository(newOwner, confirmedFullName)
                }
            )
            is DangerZoneDialogState.Delete -> RepositoryTextInputDialog(
                title = getString(R.string.repository_danger_zone_delete_action),
                label = dialog.fullName,
                helperText = getString(R.string.repository_danger_zone_delete_confirm_message, dialog.fullName),
                initialValue = "",
                confirmText = getString(R.string.repository_danger_zone_delete_confirm_positive),
                dismissText = getString(android.R.string.cancel),
                requiredErrorText = getString(R.string.repository_danger_zone_delete_confirm_message, dialog.fullName),
                onDismiss = { dialogState = null },
                onConfirm = { confirmation ->
                    dialogState = null
                    viewModel.deleteRepository(confirmation)
                }
            )
        }
    }

    private sealed interface DangerZoneDialogState {
        data class Archive(val archive: Boolean) : DangerZoneDialogState
        data class Transfer(val fullName: String) : DangerZoneDialogState
        data class Delete(val fullName: String) : DangerZoneDialogState
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
    }
}