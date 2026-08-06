package com.Sunset.REN.GitHub.ui.repo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositorySettingsScreen
import com.google.android.material.snackbar.Snackbar

class RepositorySettingsFragment : RepositorySectionFragment() {

    private val viewModel: RepositorySettingsViewModel by viewModels()
    override var repositoryOwner: String = ""
    override var repositoryName: String = ""
    override val selectedRepositorySection: RepositorySection = RepositorySection.Settings
    private var uiState by mutableStateOf(RepositorySettingsUiState())
    private var pendingVisibilitySelection by mutableStateOf<RepositorySettingsVisibility?>(null)
    private var editFieldDialogItem by mutableStateOf<RepositorySettingsEditableItem?>(null)
    private var confirmationDialogState by mutableStateOf<RepositorySettingsConfirmationDialogState?>(null)
    private var lastPendingMessage: String? = null
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositorySettingsScreen(
                        state = uiState,
                        pendingVisibilitySelection = pendingVisibilitySelection,
                        onRetry = { viewModel.refresh() },
                        onVisibilitySelected = ::confirmVisibilitySelection,
                        onEditField = ::showEditFieldDialog,
                        onToggle = ::handleToggleClick,
                        onOpenBranches = ::openBranchSettings,
                        onOpenCollaborators = ::openCollaboratorsSettings,
                        onOpenRulesets = ::openRulesetsSettings,
                        onOpenWebhooks = ::openWebhooksSettings,
                        onOpenDeployKeys = ::openDeployKeysSettings,
                        onOpenActions = ::openActionsSettings,
                        onOpenDangerZone = ::openDangerZoneSettings
                    )
                    RepositorySettingsEditFieldDialogHost(
                        item = editFieldDialogItem,
                        onDismiss = { editFieldDialogItem = null },
                        onSave = { item, value ->
                            editFieldDialogItem = null
                            viewModel.updateField(item.key, value)
                        }
                    )
                    RepositorySettingsConfirmationDialogHost(
                        state = confirmationDialogState,
                        onDismiss = ::dismissConfirmationDialog,
                        onConfirm = ::confirmSettingsDialog
                    )
                }
            }
        }
        rootView = composeView
        return composeView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repositoryOwner = arguments?.getString(ARG_OWNER).orEmpty()
        repositoryName = arguments?.getString(ARG_REPO).orEmpty()
        renderRepositorySectionNavigation()
        viewModel.settingsState.observe(viewLifecycleOwner) { state ->
            uiState = state
            if (!state.isSaving) {
                pendingVisibilitySelection = null
            }
            val pendingMessage = state.pendingMessage?.takeIf { !state.isSaving && it.isNotBlank() }
            if (!pendingMessage.isNullOrBlank() && pendingMessage != lastPendingMessage) {
                lastPendingMessage = pendingMessage
                rootView?.let { Snackbar.make(it, pendingMessage, Snackbar.LENGTH_SHORT).show() }
            }
            if (state.pendingMessage.isNullOrBlank()) {
                lastPendingMessage = null
            }
        }
        viewModel.prepare(repositoryOwner, repositoryName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rootView = null
        lastPendingMessage = null
    }

    private fun showEditFieldDialog(item: RepositorySettingsEditableItem) {
        editFieldDialogItem = item
    }

    private fun confirmVisibilitySelection(next: RepositorySettingsVisibility) {
        val current = uiState.screen?.visibilityOption?.selected ?: return
        if (current == next) return
        pendingVisibilitySelection = next
        confirmVisibilityChange(current, next) { viewModel.updateVisibility(next) }
    }

    private fun confirmVisibilityChange(
        current: RepositorySettingsVisibility,
        next: RepositorySettingsVisibility,
        onConfirmed: () -> Unit
    ) {
        val message = getString(R.string.repository_settings_visibility_confirm_message, current.displayName, next.displayName)
        confirmationDialogState = RepositorySettingsConfirmationDialogState.Visibility(
            title = getString(R.string.repository_settings_visibility_confirm_title),
            message = message,
            confirmText = getString(R.string.repository_settings_save_action),
            onConfirmed = onConfirmed
        )
    }

    private fun handleToggleClick(item: RepositorySettingsToggleItem) {
        val nextChecked = !item.checked
        if (item.key == RepositorySettingsToggleKey.Archived && nextChecked) {
            showArchiveConfirmation(item)
        } else {
            viewModel.updateToggle(item.key, nextChecked)
        }
    }

    private fun showArchiveConfirmation(item: RepositorySettingsToggleItem) {
        confirmationDialogState = RepositorySettingsConfirmationDialogState.Archive(
            title = getString(R.string.repository_settings_archive_title),
            message = getString(R.string.repository_settings_archive_message),
            confirmText = getString(R.string.repository_settings_archive_confirm),
            onConfirmed = { viewModel.updateToggle(item.key, true) }
        )
    }

    private fun dismissConfirmationDialog() {
        if (confirmationDialogState is RepositorySettingsConfirmationDialogState.Visibility) {
            pendingVisibilitySelection = null
        }
        confirmationDialogState = null
    }

    private fun confirmSettingsDialog(state: RepositorySettingsConfirmationDialogState) {
        confirmationDialogState = null
        state.onConfirmed()
    }

    private fun openBranchSettings() = openRepositorySettingsSubPage(RepositorySettingsSubPage.Branches)

    private fun openCollaboratorsSettings() = openRepositorySettingsSubPage(RepositorySettingsSubPage.Collaborators)

    private fun openActionsSettings() = openRepositorySettingsSubPage(RepositorySettingsSubPage.Actions)

    private fun openWebhooksSettings() = openRepositorySettingsSubPage(RepositorySettingsSubPage.Webhooks)

    private fun openRulesetsSettings() = openRepositorySettingsSubPage(RepositorySettingsSubPage.Rulesets)

    private fun openDeployKeysSettings() = openRepositorySettingsSubPage(RepositorySettingsSubPage.DeployKeys)

    private fun openDangerZoneSettings() = openRepositorySettingsSubPage(RepositorySettingsSubPage.DangerZone)

    private fun openRepositorySettingsSubPage(target: RepositorySettingsSubPage) {
        val state = viewModel.settingsState.value
        val owner = state?.owner.orEmpty()
        val repo = state?.repo.orEmpty()
        if (owner.isBlank() || repo.isBlank()) {
            showSnackbar(R.string.repository_section_missing_repository)
            return
        }
        val destinationId = resources.getIdentifier(target.destinationName, ResourceTypeId, requireContext().packageName)
        if (destinationId == 0) {
            showSnackbar(target.missingDestinationMessage)
            return
        }
        findNavController().navigate(
            destinationId,
            Bundle().apply {
                putString(target.argumentOwnerKey, owner)
                putString(target.argumentRepoKey, repo)
            }
        )
    }

    private fun showSnackbar(messageRes: Int) {
        rootView?.let { Snackbar.make(it, messageRes, Snackbar.LENGTH_SHORT).show() }
    }

    private val RepositorySettingsVisibility.displayName: String
        get() = when (this) {
            RepositorySettingsVisibility.Public -> getString(R.string.repository_settings_visibility_public)
            RepositorySettingsVisibility.Internal -> getString(R.string.repository_settings_visibility_internal)
            RepositorySettingsVisibility.Private -> getString(R.string.repository_settings_visibility_private)
        }

    private enum class RepositorySettingsSubPage(
        val destinationName: String,
        val missingDestinationMessage: Int,
        val argumentOwnerKey: String,
        val argumentRepoKey: String
    ) {
        Branches(
            destinationName = "repository_branch_settings_fragment",
            missingDestinationMessage = R.string.repository_branch_settings_missing_destination,
            argumentOwnerKey = RepositoryBranchSettingsFragment.ARG_OWNER,
            argumentRepoKey = RepositoryBranchSettingsFragment.ARG_REPO
        ),
        Collaborators(
            destinationName = "repository_collaborators_settings_fragment",
            missingDestinationMessage = R.string.repository_collaborators_settings_missing_destination,
            argumentOwnerKey = RepositoryCollaboratorsSettingsFragment.ARG_OWNER,
            argumentRepoKey = RepositoryCollaboratorsSettingsFragment.ARG_REPO
        ),
        Actions(
            destinationName = "repository_actions_settings_fragment",
            missingDestinationMessage = R.string.repository_actions_settings_missing_destination,
            argumentOwnerKey = RepositoryActionsSettingsFragment.ARG_OWNER,
            argumentRepoKey = RepositoryActionsSettingsFragment.ARG_REPO
        ),
        Rulesets(
            destinationName = "repository_rulesets_fragment",
            missingDestinationMessage = R.string.repository_settings_missing_destination,
            argumentOwnerKey = RepositoryRulesetsFragment.ARG_OWNER,
            argumentRepoKey = RepositoryRulesetsFragment.ARG_REPO
        ),
        Webhooks(
            destinationName = "repository_webhooks_fragment",
            missingDestinationMessage = R.string.repository_settings_missing_destination,
            argumentOwnerKey = RepositoryWebhooksFragment.ARG_OWNER,
            argumentRepoKey = RepositoryWebhooksFragment.ARG_REPO
        ),
        DeployKeys(
            destinationName = "repository_deploy_keys_fragment",
            missingDestinationMessage = R.string.repository_settings_missing_destination,
            argumentOwnerKey = RepositoryDeployKeysFragment.ARG_OWNER,
            argumentRepoKey = RepositoryDeployKeysFragment.ARG_REPO
        ),
        DangerZone(
            destinationName = "repository_danger_zone_fragment",
            missingDestinationMessage = R.string.repository_section_native_stub_missing_url,
            argumentOwnerKey = RepositoryDangerZoneFragment.ARG_OWNER,
            argumentRepoKey = RepositoryDangerZoneFragment.ARG_REPO
        )
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
        private const val ResourceTypeId = "id"
    }
}

private sealed class RepositorySettingsConfirmationDialogState(
    open val title: String,
    open val message: String,
    open val confirmText: String,
    open val onConfirmed: () -> Unit
) {
    data class Visibility(
        override val title: String,
        override val message: String,
        override val confirmText: String,
        override val onConfirmed: () -> Unit
    ) : RepositorySettingsConfirmationDialogState(title, message, confirmText, onConfirmed)

    data class Archive(
        override val title: String,
        override val message: String,
        override val confirmText: String,
        override val onConfirmed: () -> Unit
    ) : RepositorySettingsConfirmationDialogState(title, message, confirmText, onConfirmed)
}

@Composable
private fun RepositorySettingsEditFieldDialogHost(
    item: RepositorySettingsEditableItem?,
    onDismiss: () -> Unit,
    onSave: (RepositorySettingsEditableItem, String) -> Unit
) {
    item ?: return
    var value by remember(item) { mutableStateOf(item.value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = item.label) },
        text = {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                onValueChange = { value = it },
                label = { Text(text = item.label) },
                supportingText = item.helper?.takeIf { it.isNotBlank() }?.let { helper ->
                    { Text(text = helper) }
                },
                singleLine = false
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(item, value) }) {
                Text(text = stringResource(R.string.repository_settings_save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun RepositorySettingsConfirmationDialogHost(
    state: RepositorySettingsConfirmationDialogState?,
    onDismiss: () -> Unit,
    onConfirm: (RepositorySettingsConfirmationDialogState) -> Unit
) {
    state ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = state.title) },
        text = { Text(text = state.message) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state) }) {
                Text(text = state.confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}
