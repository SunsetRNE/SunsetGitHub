package com.Sunset.REN.GitHub.ui.compose.screens.repo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.RepositoryBranch
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.repo.RepositoryReleaseAssetDraft
import com.Sunset.REN.GitHub.ui.repo.RepositoryReleaseCreateUiState
import java.util.Locale

@Composable
fun RepositoryReleaseCreateScreen(
    state: RepositoryReleaseCreateUiState,
    tagName: String,
    releaseName: String,
    body: String,
    prerelease: Boolean,
    draft: Boolean,
    makeLatest: Boolean,
    tagError: String?,
    onTagNameChange: (String) -> Unit,
    onReleaseNameChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onPrereleaseChange: (Boolean) -> Unit,
    onDraftChange: (Boolean) -> Unit,
    onMakeLatestChange: (Boolean) -> Unit,
    onSelectBranch: (String) -> Unit,
    onAddAsset: () -> Unit,
    onRemoveAsset: (Int) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.canvas).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ReleaseHeaderCard(state) }
        item {
            ReleaseTypeCard(
                prerelease = prerelease,
                draft = draft,
                makeLatest = makeLatest,
                enabled = !state.isSubmitting,
                onPrereleaseChange = onPrereleaseChange,
                onDraftChange = onDraftChange,
                onMakeLatestChange = onMakeLatestChange
            )
        }
        item {
            ReleaseTextFieldsCard(
                tagName = tagName,
                releaseName = releaseName,
                body = body,
                enabled = !state.isSubmitting,
                tagError = tagError,
                onTagNameChange = onTagNameChange,
                onReleaseNameChange = onReleaseNameChange,
                onBodyChange = onBodyChange
            )
        }
        item { ReleaseBranchCard(state, onSelectBranch) }
        item { ReleaseAssetsCard(state.assets, state.isSubmitting, onAddAsset, onRemoveAsset) }
        item { ReleaseStatusCard(state, draft, onSubmit) }
    }
}

@Composable
private fun ReleaseHeaderCard(state: RepositoryReleaseCreateUiState) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(stringResource(R.string.repository_release_create_title), color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = if (state.owner.isBlank() || state.repo.isBlank()) stringResource(R.string.repository_release_create_subtitle) else "${state.owner}/${state.repo}",
            modifier = Modifier.padding(top = 6.dp),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = previousTagText(state),
            modifier = Modifier.padding(top = 10.dp),
            color = colors.textMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ReleaseTypeCard(
    prerelease: Boolean,
    draft: Boolean,
    makeLatest: Boolean,
    enabled: Boolean,
    onPrereleaseChange: (Boolean) -> Unit,
    onDraftChange: (Boolean) -> Unit,
    onMakeLatestChange: (Boolean) -> Unit
) {
    val latestAllowed = !draft && !prerelease
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.repository_release_create_type_section), color = SunsetGitHubThemeTokens.colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        ReleaseCheckRow(stringResource(R.string.repository_release_create_type_preview), prerelease, enabled && !draft) { checked ->
            onPrereleaseChange(checked)
            if (checked) onMakeLatestChange(false)
        }
        ReleaseCheckRow(stringResource(R.string.repository_release_create_type_draft), draft, enabled) { checked ->
            onDraftChange(checked)
            if (checked) {
                onPrereleaseChange(false)
                onMakeLatestChange(false)
            }
        }
        ReleaseCheckRow(stringResource(R.string.repository_release_create_type_release), makeLatest, enabled && latestAllowed, onMakeLatestChange)
    }
}

@Composable
private fun ReleaseCheckRow(label: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { onCheckedChange(!checked) }.padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), color = SunsetGitHubThemeTokens.colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
        Checkbox(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ReleaseTextFieldsCard(
    tagName: String,
    releaseName: String,
    body: String,
    enabled: Boolean,
    tagError: String?,
    onTagNameChange: (String) -> Unit,
    onReleaseNameChange: (String) -> Unit,
    onBodyChange: (String) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.repository_release_create_tag_section), color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        ReleaseTextField(tagName, onTagNameChange, stringResource(R.string.repository_release_create_tag_hint), enabled, tagError)
        ReleaseTextField(releaseName, onReleaseNameChange, stringResource(R.string.repository_release_create_name_hint), enabled, null)
        ReleaseTextField(body, onBodyChange, stringResource(R.string.repository_release_create_body_hint), enabled, null, minLines = 5)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SunsetSecondaryButton(stringResource(R.string.repository_release_create_chip_markdown), { onBodyChange(body + "\n\n**加粗文本**") }, Modifier.weight(1f), enabled)
            SunsetSecondaryButton(stringResource(R.string.repository_release_create_chip_link), { onBodyChange(body + "\n[链接文本](https://)") }, Modifier.weight(1f), enabled)
            SunsetSecondaryButton(stringResource(R.string.repository_release_create_chip_list), { onBodyChange(body + "\n- 列表项") }, Modifier.weight(1f), enabled)
        }
    }
}

@Composable
private fun ReleaseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    error: String?,
    minLines: Int = 1
) {
    val colors = SunsetGitHubThemeTokens.colors
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        minLines = minLines,
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.border,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedLabelColor = colors.accent,
            unfocusedLabelColor = colors.textSecondary,
            cursorColor = colors.accent
        )
    )
}

@Composable
private fun ReleaseBranchCard(state: RepositoryReleaseCreateUiState, onSelectBranch: (String) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.repository_release_create_target_section), color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(branchText(state), modifier = Modifier.padding(top = 6.dp), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        state.branchErrorMessage?.let { Text(it, modifier = Modifier.padding(top = 6.dp), color = colors.danger, style = MaterialTheme.typography.bodySmall) }
        state.branches.take(5).forEach { branch ->
            BranchRow(branch, branch.name == state.selectedBranchName, !state.isSubmitting, onSelectBranch)
        }
    }
}

@Composable
private fun BranchRow(branch: RepositoryBranch, selected: Boolean, enabled: Boolean, onSelectBranch: (String) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { onSelectBranch(branch.name) }.padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (selected) "OK" else "", modifier = Modifier.weight(0.25f), color = colors.success, style = MaterialTheme.typography.bodySmall)
        Text(branch.name, modifier = Modifier.weight(1f), color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(branchMeta(branch), color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReleaseAssetsCard(assets: List<RepositoryReleaseAssetDraft>, isSubmitting: Boolean, onAddAsset: () -> Unit, onRemoveAsset: (Int) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.repository_release_create_assets_section), color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (assets.isEmpty()) {
            Text(stringResource(R.string.repository_action_run_detail_assets_empty), modifier = Modifier.padding(top = 8.dp), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        assets.forEachIndexed { index, asset -> AssetDraftRow(index, asset, !isSubmitting, onRemoveAsset) }
        SunsetSecondaryButton(stringResource(R.string.repository_release_create_add_asset), onAddAsset, Modifier.fillMaxWidth().padding(top = 12.dp), !isSubmitting)
    }
}

@Composable
private fun AssetDraftRow(index: Int, asset: RepositoryReleaseAssetDraft, enabled: Boolean, onRemoveAsset: (Int) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(asset.fileName, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${asset.sizeBytes.formatFileSize()} · ${asset.mimeType}", color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = stringResource(R.string.repository_release_create_remove_asset),
            modifier = Modifier.clickable(enabled = enabled) { onRemoveAsset(index) }.padding(8.dp),
            color = if (enabled) colors.danger else colors.textMuted,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun ReleaseStatusCard(state: RepositoryReleaseCreateUiState, draft: Boolean, onSubmit: () -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        val message = when {
            !state.errorMessage.isNullOrBlank() -> stringResource(R.string.repository_release_create_failed, state.errorMessage)
            !state.statusMessage.isNullOrBlank() -> state.statusMessage
            else -> stringResource(R.string.repository_release_create_subtitle)
        }
        Text(message.orEmpty(), color = if (state.errorMessage != null) colors.danger else colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        SunsetPrimaryButton(
            text = when {
                state.isSubmitting -> stringResource(R.string.repository_release_create_submitting)
                draft -> stringResource(R.string.repository_release_create_save_draft)
                else -> stringResource(R.string.repository_release_create_submit)
            },
            onClick = onSubmit,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
    }
}

@Composable
private fun previousTagText(state: RepositoryReleaseCreateUiState): String = when {
    state.isLoadingPreviousTag -> stringResource(R.string.repository_release_create_previous_tag_loading)
    !state.previousTagName.isNullOrBlank() -> stringResource(R.string.repository_release_create_previous_tag_value, state.previousTagName)
    else -> stringResource(R.string.repository_release_create_previous_tag_empty)
}

@Composable
private fun branchText(state: RepositoryReleaseCreateUiState): String = when {
    state.isLoadingBranches -> stringResource(R.string.repository_release_create_branch_loading)
    state.selectedBranchName.isNotBlank() -> state.selectedBranchName
    else -> stringResource(R.string.repository_release_create_branch_default_fallback)
}

private fun branchMeta(branch: RepositoryBranch): String = listOfNotNull(
    "default".takeIf { branch.isDefault },
    "protected".takeIf { branch.isProtected },
    "ready"
).joinToString(" · ")

private fun Long.formatFileSize(): String {
    if (this < 1024L) return "$this B"
    val units = listOf("KiB", "MiB", "GiB")
    var value = toDouble() / 1024.0
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return "%.1f %s".format(Locale.US, value, units[unitIndex])
}