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
import com.Sunset.REN.GitHub.ui.workspace.WorkspacePullInput
import com.Sunset.REN.GitHub.ui.workspace.WorkspacePullUiState

@Composable
fun WorkspacePullScreen(
    state: WorkspacePullUiState,
    onCreateWorkspace: (String) -> Unit,
    onPreviewPull: (WorkspacePullInput) -> Unit,
    onPullRemote: (WorkspacePullInput) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val spacing = SunsetGitHubThemeTokens.spacing
    var workspaceName by rememberSaveable { mutableStateOf("") }
    var owner by rememberSaveable { mutableStateOf("") }
    var repo by rememberSaveable { mutableStateOf("") }
    var branch by rememberSaveable { mutableStateOf("") }
    var remotePath by rememberSaveable { mutableStateOf("") }
    var localTarget by rememberSaveable { mutableStateOf("") }
    var overwriteLocal by rememberSaveable { mutableStateOf(false) }

    fun input() = WorkspacePullInput(
        owner = owner.trim(),
        repo = repo.trim(),
        branch = branch.trim(),
        remotePath = remotePath.trim(),
        localTarget = localTarget.trim(),
        overwriteLocal = overwriteLocal
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
            title = stringResource(R.string.title_workspace_pull),
            description = stringResource(R.string.workspace_pull_description)
        ) {
            WorkspaceProjectCard(
                workspaceName = workspaceName,
                selectedWorkspace = state.selectedWorkspace,
                onWorkspaceNameChange = { workspaceName = it },
                onCreateWorkspace = { onCreateWorkspace(workspaceName) }
            )

            SunsetSectionCard(
                title = stringResource(R.string.workspace_remote_source_title)
            ) {
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
                    value = localTarget,
                    onValueChange = { localTarget = it },
                    label = stringResource(R.string.workspace_pull_local_target_hint),
                    modifier = Modifier.padding(top = 8.dp)
                )
                WorkspaceCheckboxRow(
                    checked = overwriteLocal,
                    onCheckedChange = { overwriteLocal = it },
                    label = stringResource(R.string.workspace_pull_overwrite_local)
                )
                SunsetSecondaryButton(
                    text = stringResource(R.string.workspace_pull_preview_remote),
                    onClick = { onPreviewPull(input()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                SunsetPrimaryButton(
                    text = stringResource(R.string.workspace_pull_execute),
                    onClick = { onPullRemote(input()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            WorkspaceLogText(log = state.log)
        }
    }
}