package com.Sunset.REN.GitHub.ui.compose.screens.repo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRunDetail
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.repo.RepositoryActionRunDetailUiState

@Composable
fun RepositoryActionRunDeveloperInfoScreen(
    state: RepositoryActionRunDetailUiState,
    onRetry: () -> Unit,
    onOpenActions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val actionRun = state.actionRun

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.isLoading) {
            item {
                StateMessage(stringResource(R.string.repository_action_run_detail_loading))
            }
        }
        state.errorMessage?.takeIf { actionRun == null }?.let { message ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StateMessage(message)
                    SunsetPrimaryButton(
                        text = stringResource(R.string.repository_action_run_detail_retry),
                        onClick = onRetry
                    )
                }
            }
        }
        state.unavailableMessage?.takeIf { actionRun == null }?.let { message ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StateMessage(message)
                    if (state.actionsHtmlUrl != null) {
                        SunsetSecondaryButton(
                            text = stringResource(R.string.repository_action_run_detail_open_in_github),
                            onClick = onOpenActions
                        )
                    }
                }
            }
        }
        actionRun?.let { run ->
            item {
                DeveloperInfoCard(run)
            }
        }
    }
}

@Composable
private fun StateMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(top = 16.dp),
        color = SunsetGitHubThemeTokens.colors.textSecondary,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun DeveloperInfoCard(actionRun: RepositoryActionRunDetail) {
    val colors = SunsetGitHubThemeTokens.colors
    val groups = developerInfoGroups(actionRun)
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.repository_action_run_developer_info_title),
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        actionRunMeta(actionRun)?.let { meta ->
            Text(
                text = meta,
                modifier = Modifier.padding(top = 8.dp),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        groups.forEach { group ->
            Text(
                text = group.title,
                modifier = Modifier.padding(top = 14.dp),
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            group.rows.forEach { row ->
                Text(
                    text = "${row.label}：${row.value}",
                    modifier = Modifier.padding(top = 4.dp),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private data class DeveloperInfoGroup(val title: String, val rows: List<DeveloperInfoRow>)
private data class DeveloperInfoRow(val label: String, val value: String)

@Composable
private fun developerInfoGroups(run: RepositoryActionRunDetail): List<DeveloperInfoGroup> = listOf(
    DeveloperInfoGroup(
        stringResource(R.string.repository_action_run_detail_group_run),
        listOfNotNull(
            developerInfoRow(R.string.repository_action_run_detail_label_status, run.displayState),
            developerInfoRow(R.string.repository_action_run_detail_label_status_raw, run.status),
            developerInfoRow(R.string.repository_action_run_detail_label_conclusion, run.conclusion),
            developerInfoRow(R.string.repository_action_run_detail_label_event, run.event),
            developerInfoRow(R.string.repository_action_run_detail_label_workflow, run.workflowName),
            developerInfoRow(R.string.repository_action_run_detail_label_run, run.runNumber?.let { "#$it" }),
            developerInfoRow(R.string.repository_action_run_detail_label_attempt, run.runAttempt?.toString()),
            developerInfoRow(R.string.repository_action_run_detail_label_workflow_id, run.workflowId?.toString()),
            developerInfoRow(R.string.repository_action_run_detail_label_check_suite, run.checkSuiteId?.toString())
        )
    ),
    DeveloperInfoGroup(
        stringResource(R.string.repository_action_run_detail_group_people),
        listOfNotNull(
            developerInfoRow(R.string.repository_action_run_detail_label_actor, run.actorLogin?.withAtPrefix()),
            developerInfoRow(R.string.repository_action_run_detail_label_triggered_by, run.triggeringActorLogin?.withAtPrefix())
        )
    ),
    DeveloperInfoGroup(
        stringResource(R.string.repository_action_run_detail_group_timeline),
        listOfNotNull(
            developerInfoRow(R.string.repository_action_run_detail_label_created, run.createdAt?.displayTimestamp()),
            developerInfoRow(R.string.repository_action_run_detail_label_started, run.runStartedAt?.displayTimestamp()),
            developerInfoRow(R.string.repository_action_run_detail_label_updated, run.updatedAt?.displayTimestamp()),
            developerInfoRow(R.string.repository_action_run_detail_label_commit_time, run.headCommitTimestamp?.displayTimestamp())
        )
    ),
    DeveloperInfoGroup(
        stringResource(R.string.repository_action_run_detail_group_repository_commit),
        listOfNotNull(
            developerInfoRow(R.string.repository_action_run_detail_label_repository, listOfNotNull(run.repositoryOwner, run.repositoryName).joinToString("/").ifBlank { null }),
            developerInfoRow(R.string.repository_action_run_detail_label_head_repository, run.headRepositoryFullName),
            developerInfoRow(R.string.repository_action_run_detail_label_branch, run.headBranch),
            developerInfoRow(R.string.repository_action_run_detail_label_commit, run.headSha?.take(7)),
            developerInfoRow(R.string.repository_action_run_detail_label_full_commit, run.headSha),
            developerInfoRow(R.string.repository_action_run_detail_label_commit_message, run.headCommitMessage?.firstLine()),
            developerInfoRow(R.string.repository_action_run_detail_label_commit_author, listOfNotNull(run.headCommitAuthorName, run.headCommitAuthorEmail).joinToString(" · ").ifBlank { null }),
            developerInfoRow(R.string.repository_action_run_detail_label_pull_requests, run.pullRequestRefs.joinToString(", ").ifBlank { null })
        )
    ),
    DeveloperInfoGroup(
        stringResource(R.string.repository_action_run_detail_group_resources),
        listOfNotNull(
            developerInfoRow(R.string.repository_action_run_detail_label_workflow_path, run.path),
            developerInfoRow(R.string.repository_action_run_detail_label_github_page, run.htmlUrl?.removePrefix("https://")),
            developerInfoRow(R.string.repository_action_run_detail_label_available_apis, availableApiSummary(run))
        )
    )
).filter { it.rows.isNotEmpty() }

@Composable
private fun developerInfoRow(labelRes: Int, value: String?): DeveloperInfoRow? {
    val normalized = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return DeveloperInfoRow(stringResource(labelRes), normalized)
}

@Composable
private fun availableApiSummary(run: RepositoryActionRunDetail): String? = listOfNotNull(
    run.jobsUrl?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.repository_action_run_detail_api_jobs) },
    run.logsUrl?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.repository_action_run_detail_api_logs) },
    run.artifactsUrl?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.repository_action_run_detail_api_artifacts) },
    run.rerunUrl?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.repository_action_run_detail_api_rerun) },
    run.previousAttemptUrl?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.repository_action_run_detail_api_previous_attempt) }
).takeIf { it.isNotEmpty() }?.joinToString(" · ")

private fun actionRunMeta(run: RepositoryActionRunDetail): String? = listOfNotNull(
    run.runNumber?.let { "运行 #$it" },
    run.runAttempt?.let { "第 $it 次尝试" },
    run.headBranch?.takeIf { it.isNotBlank() },
    run.headSha?.takeIf { it.isNotBlank() }?.take(7)
).takeIf { it.isNotEmpty() }?.joinToString(" · ")

private fun String.withAtPrefix(): String = if (startsWith("@")) this else "@$this"
private fun String.displayTimestamp(): String = replace("T", " ").removeSuffix("Z")
private fun String.firstLine(): String = lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { this }
