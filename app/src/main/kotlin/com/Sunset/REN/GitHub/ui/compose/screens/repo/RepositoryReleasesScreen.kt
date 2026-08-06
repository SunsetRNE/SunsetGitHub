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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.RepositoryRelease
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetEmptyState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetErrorState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetLoadingState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.repo.RepositoryReleasesUiState
import java.util.Locale

@Composable
fun RepositoryReleasesScreen(
    state: RepositoryReleasesUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenRelease: (String) -> Unit,
    onDownload: (url: String, fileName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    when {
        state.isInitialLoad -> SunsetLoadingState(modifier.fillMaxSize().background(colors.canvas), stringResource(R.string.repository_releases_loading))
        state.errorMessage != null && state.releases.isEmpty() -> SunsetErrorState(
            title = stringResource(R.string.repository_releases_failed, ""),
            message = state.errorMessage,
            modifier = modifier.fillMaxSize().background(colors.canvas),
            action = { SunsetPrimaryButton(stringResource(R.string.repository_releases_retry), onRetry) }
        )
        state.isEmpty -> SunsetEmptyState(
            title = stringResource(R.string.repository_releases_empty),
            modifier = modifier.fillMaxSize().background(colors.canvas)
        )
        else -> ReleasesList(state, onLoadMore, onOpenRelease, onDownload, modifier)
    }
}

@Composable
private fun ReleasesList(
    state: RepositoryReleasesUiState,
    onLoadMore: () -> Unit,
    onOpenRelease: (String) -> Unit,
    onDownload: (String, String) -> Unit,
    modifier: Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.canvas).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SunsetCard(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.title_repository_releases), color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(R.string.repository_releases_context, "${state.owner}/${state.repo}", state.releases.size),
                    modifier = Modifier.padding(top = 4.dp),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                if (state.isShowingStaleContent) Text(stringResource(R.string.repository_releases_loading), modifier = Modifier.padding(top = 10.dp), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                state.errorMessage?.takeIf { state.releases.isNotEmpty() }?.let { message ->
                    Text(stringResource(R.string.repository_releases_failed, message), modifier = Modifier.padding(top = 10.dp), color = colors.danger, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        items(state.releases, key = { release -> "${release.tagName}:${release.htmlUrl}" }) { release ->
            ReleaseCard(release, state.repo, onOpenRelease, onDownload)
        }
        if (state.hasMore) item {
            SunsetPrimaryButton(
                text = if (state.isLoadingMore) stringResource(R.string.repository_releases_loading_more) else stringResource(R.string.repository_releases_load_more),
                onClick = onLoadMore,
                enabled = !state.isLoadingMore,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun ReleaseCard(
    release: RepositoryRelease,
    repositoryName: String,
    onOpenRelease: (String) -> Unit,
    onDownload: (String, String) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(release.name.ifBlank { release.tagName }, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(release.tagName.ifBlank { "-" }, modifier = Modifier.padding(top = 6.dp), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
            releaseBadge(release)?.let { Text(it, color = colors.accent, style = MaterialTheme.typography.labelMedium) }
        }
        releaseMeta(release)?.let { Text(it, modifier = Modifier.padding(top = 6.dp), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall) }
        Text(
            text = release.bodySummary ?: release.body?.plainReleaseBody().orEmpty().ifBlank { stringResource(R.string.repository_releases_no_body) },
            modifier = Modifier.padding(top = 12.dp),
            color = colors.textPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
        release.htmlUrl?.takeIf { it.isNotBlank() }?.let { url ->
            SunsetSecondaryButton(stringResource(R.string.repository_releases_open_in_github), { onOpenRelease(url) }, Modifier.padding(top = 12.dp))
        }
        Text(
            text = if (release.assets.isEmpty()) stringResource(R.string.repository_releases_assets_title) else stringResource(R.string.repository_releases_assets_count, release.assets.size),
            modifier = Modifier.padding(top = 16.dp), color = colors.textPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold
        )
        release.assets.forEach { asset ->
            DownloadRow(asset.name, stringResource(R.string.repository_releases_asset_meta, asset.sizeBytes.toFileSize(), asset.downloadCount), asset.browserDownloadUrl, asset.name, onDownload)
        }
        val sourceItems = listOfNotNull(
            release.zipballUrl?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.repository_releases_source_zip) to it },
            release.tarballUrl?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.repository_releases_source_tar) to it }
        )
        if (sourceItems.isNotEmpty()) {
            Text(stringResource(R.string.repository_releases_source_code_title), modifier = Modifier.padding(top = 14.dp), color = colors.textPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            sourceItems.forEach { (title, url) ->
                val extension = if (title.contains("tar")) ".tar.gz" else ".zip"
                DownloadRow(title, release.tagName, url, "${repositoryName.ifBlank { "source" }}-${release.tagName.ifBlank { "release" }}$extension", onDownload)
            }
        }
    }
}

@Composable
private fun DownloadRow(title: String, meta: String, url: String?, fileName: String, onDownload: (String, String) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
            Text(meta, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = stringResource(R.string.repository_releases_download_action),
            modifier = Modifier.clickable(enabled = !url.isNullOrBlank()) { onDownload(url.orEmpty(), fileName) }.padding(8.dp),
            color = if (url.isNullOrBlank()) colors.textMuted else colors.accent,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

private fun releaseBadge(release: RepositoryRelease): String? = when {
    release.isDraft -> "草稿"
    release.isLatest -> "Latest"
    release.isPrerelease -> "预发布"
    else -> null
}

private fun releaseMeta(release: RepositoryRelease): String? {
    val date = (release.publishedAt ?: release.createdAt)?.substringBefore('T').orEmpty()
    val author = release.authorLogin?.takeIf { it.isNotBlank() }
    return when {
        author != null && date.isNotBlank() -> "$author 发布于 $date"
        date.isNotBlank() -> "发布于 $date"
        else -> author
    }
}

private fun String.plainReleaseBody(): String = replace(Regex("[`*_>#]"), "").replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1").trim()
private fun Long.toFileSize(): String {
    if (this <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var value = toDouble(); var index = 0
    while (value >= 1024 && index < units.lastIndex) { value /= 1024; index++ }
    return if (index == 0) "${value.toLong()} ${units[index]}" else String.format(Locale.US, "%.1f %s", value, units[index])
}