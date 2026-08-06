package com.Sunset.REN.GitHub.ui.compose.screens.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSectionCard
import com.Sunset.REN.GitHub.ui.workspace.WorkspaceSyncInput
import com.Sunset.REN.GitHub.ui.workspace.WorkspaceSyncUiState

@Composable
fun WorkspacePushScreen(
    state: WorkspaceSyncUiState,
    onCreateWorkspace: (String) -> Unit,
    onImportPath: (sourcePath: String, targetDirectory: String) -> Unit,
    onDryRun: (WorkspaceSyncInput) -> Unit,
    onExecuteSync: (WorkspaceSyncInput) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val spacing = SunsetGitHubThemeTokens.spacing
    var workspaceName by rememberSaveable { mutableStateOf("") }
    var importPath by rememberSaveable { mutableStateOf("") }
    var importTarget by rememberSaveable { mutableStateOf("") }
    var owner by rememberSaveable { mutableStateOf("") }
    var repo by rememberSaveable { mutableStateOf("") }
    var branch by rememberSaveable { mutableStateOf("") }
    var remotePath by rememberSaveable { mutableStateOf("") }
    var commitMessage by rememberSaveable { mutableStateOf("") }
    var mirrorMode by rememberSaveable { mutableStateOf(false) }
    var destructiveConfirmed by rememberSaveable { mutableStateOf(false) }
    var allowOverwriteRemoteChanges by rememberSaveable { mutableStateOf(false) }

    fun input() = WorkspaceSyncInput(
        owner = owner.trim(),
        repo = repo.trim(),
        branch = branch.trim(),
        remotePath = remotePath.trim(),
        commitMessage = commitMessage.trim(),
        mirrorMode = mirrorMode,
        destructiveConfirmed = destructiveConfirmed,
        allowOverwriteRemoteChanges = allowOverwriteRemoteChanges
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .padding(bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        WorkspacePageScaffold(
            title = stringResource(R.string.title_workspace_push),
            description = stringResource(R.string.workspace_push_description)
        ) {
            WorkspaceProjectCard(
                workspaceName = workspaceName,
                selectedWorkspace = state.selectedWorkspace,
                onWorkspaceNameChange = { workspaceName = it },
                onCreateWorkspace = { onCreateWorkspace(workspaceName) }
            )

            SunsetSectionCard(title = stringResource(R.string.workspace_push_import_title)) {
                WorkspaceTextField(
                    value = importPath,
                    onValueChange = { importPath = it },
                    label = stringResource(R.string.workspace_push_import_source_hint),
                    modifier = Modifier.padding(top = 12.dp)
                )
                WorkspaceTextField(
                    value = importTarget,
                    onValueChange = { importTarget = it },
                    label = stringResource(R.string.workspace_push_import_target_hint),
                    modifier = Modifier.padding(top = 8.dp)
                )
                SunsetSecondaryButton(
                    text = stringResource(R.string.workspace_push_import_button),
                    onClick = { onImportPath(importPath, importTarget) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            SunsetSectionCard(title = stringResource(R.string.workspace_push_target_title)) {
                WorkspaceTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = stringResource(R.string.workspace_repo_owner_hint),
                    modifier = Modifier.padding(top = 12.dp)
                )
                WorkspaceTextField(
                    value = repo,
                    onValueChange = { repo = it },
                    label = stringResource(R.string.workspace_repo_name_hint),
                    modifier = Modifier.padding(top = 8.dp)
                )
                WorkspaceTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = stringResource(R.string.workspace_branch_hint),
                    modifier = Modifier.padding(top = 8.dp)
                )
                WorkspaceTextField(
                    value = remotePath,
                    onValueChange = { remotePath = it },
                    label = stringResource(R.string.workspace_remote_path_hint),
                    modifier = Modifier.padding(top = 8.dp)
                )
                WorkspaceTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    label = stringResource(R.string.workspace_push_commit_message_hint),
                    modifier = Modifier.padding(top = 8.dp),
                    minLines = 2
                )
                WorkspaceCheckboxRow(
                    checked = mirrorMode,
                    onCheckedChange = { mirrorMode = it },
                    label = stringResource(R.string.workspace_push_mirror_mode)
                )
                WorkspaceCheckboxRow(
                    checked = destructiveConfirmed,
                    onCheckedChange = { destructiveConfirmed = it },
                    label = stringResource(R.string.workspace_push_confirm_delete)
                )
                WorkspaceCheckboxRow(
                    checked = allowOverwriteRemoteChanges,
                    onCheckedChange = { allowOverwriteRemoteChanges = it },
                    label = stringResource(R.string.workspace_push_allow_overwrite_remote)
                )
                SunsetSecondaryButton(
                    text = stringResource(R.string.workspace_push_dry_run),
                    onClick = { onDryRun(input()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                SunsetPrimaryButton(
                    text = stringResource(R.string.workspace_push_execute),
                    onClick = { onExecuteSync(input()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            WorkspaceLogText(log = state.log)
        }
    }
}