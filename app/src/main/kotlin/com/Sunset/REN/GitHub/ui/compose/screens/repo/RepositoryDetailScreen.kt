package com.Sunset.REN.GitHub.ui.compose.screens.repo

import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.RepositoryBranch
import com.Sunset.REN.GitHub.domain.repo.RepositoryContentItem
import com.Sunset.REN.GitHub.domain.repo.RepositoryContributor
import com.Sunset.REN.GitHub.domain.repo.RepositoryFilePreview
import com.Sunset.REN.GitHub.domain.repo.RepositoryLanguage
import com.Sunset.REN.GitHub.domain.repo.RepositoryRelease
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.repo.RepositoryDetailUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryFilePreviewTextFormatter
import com.Sunset.REN.GitHub.ui.repo.RepositorySection
import com.Sunset.REN.GitHub.ui.repo.setRepositoryMarkdown

@Composable
fun RepositoryDetailScreen(
    state: RepositoryDetailUiState,
    onRetry: () -> Unit,
    onRetryContents: () -> Unit,
    onRetryPreview: () -> Unit,
    onOpenGitHub: (String) -> Unit,
    onOpenOwner: (String) -> Unit,
    onOpenFile: (RepositoryContentItem.File) -> Unit,
    onOpenDirectory: (RepositoryContentItem.Directory) -> Unit,
    onSelectBranch: (String) -> Unit,
    onRefreshBranches: () -> Unit,
    onSelectReadmePreview: (RepositoryContentItem.File) -> Unit,
    onEditPreviewFile: (RepositoryFilePreview) -> Unit,
    onCreateFile: () -> Unit,
    onUploadFile: () -> Unit,
    onToggleStar: () -> Unit,
    onToggleWatch: () -> Unit,
    onOpenSection: (RepositorySection) -> Unit,
    onOpenReleases: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    LazyColumn(
        modifier = modifier.background(colors.canvas),
        contentPadding = PaddingValues(start = 10.dp, top = 8.dp, end = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (state) {
            RepositoryDetailUiState.Loading -> item { StateCard("正在加载仓库详情…") }
            RepositoryDetailUiState.SignedOut -> item { StateCard("登录后可查看仓库详情。") }
            is RepositoryDetailUiState.Error -> item { StateCard("仓库详情加载失败：${state.message}", "重试", onRetry) }
            is RepositoryDetailUiState.Content -> {
                val licenseFile = state.contents.firstOrNull { item -> item is RepositoryContentItem.File && item.name.isLicenseLikeName() } as? RepositoryContentItem.File
                val readmeCandidates = readmePreviewCandidates(state.contents)
                val shouldShowReadme = readmeCandidates.isNotEmpty() || state.filePreview != null || state.filePreviewError != null || state.isFilePreviewLoading
                item { BasicInfoCard(state.repository, state, onOpenGitHub, onOpenOwner, onToggleStar, onToggleWatch) }
                item { RootContentsCard(state, onRetryContents, onOpenFile, onOpenDirectory, onSelectBranch, onRefreshBranches, onCreateFile, onUploadFile) }
                if (shouldShowReadme) item { ReadmeCard(state.filePreview, readmeCandidates, state.filePreviewError, state.isFilePreviewLoading, state.canPush, onRetryPreview, onSelectReadmePreview, onEditPreviewFile, onOpenGitHub) }
                item { ReleasesCard(state.sidebarInfo?.releases.orEmpty(), onOpenReleases, onOpenGitHub) }
                item { LanguagesCard(resolveLanguages(state)) }
                item { ContributorsCard(state.sidebarInfo?.contributors.orEmpty(), onOpenSection, onOpenGitHub) }
                item { LicenseCard(licenseFile, onOpenFile) }
                state.sidebarInfo?.error?.let { error -> item { StateCard("仓库侧栏信息部分加载失败：$error") } }
            }
        }
    }
}

@Composable
private fun StateCard(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(message, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null && onAction != null) SunsetSecondaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
private fun BasicInfoCard(repository: GitHubRepository, state: RepositoryDetailUiState.Content, onOpenGitHub: (String) -> Unit, onOpenOwner: (String) -> Unit, onToggleStar: () -> Unit, onToggleWatch: () -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Surface(Modifier.size(28.dp), color = colors.subtleBackground, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, colors.border)) {
                    Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_package_24), null, tint = colors.textSecondary, modifier = Modifier.size(16.dp)) }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(repository.fullName, color = colors.accent, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(repository.ownerLogin, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable { onOpenOwner(repository.ownerLogin) })
                }
                CompactBadge(if (repository.isPrivate) "私有" else "公开")
            }
            Text(repository.description?.takeIf { it.isNotBlank() } ?: "暂无描述", color = colors.textPrimary, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                MetaChip(R.drawable.ic_star_filled_24, compactCount(repository.stargazersCount))
                MetaChip(R.drawable.ic_fork_24, compactCount(repository.forksCount))
                MetaChip(R.drawable.ic_visibility_24, compactCount(repository.watchersCount))
                repository.language?.takeIf { it.isNotBlank() }?.let { LanguageChip(it) }
                if (repository.fork) CompactBadge("派生")
                if (repository.archived) CompactBadge("归档")
                if (state.isRefreshingFromCache) CompactBadge("刷新中")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                TinyIconButton(R.drawable.ic_star_filled_24, if (state.isStarred == true) "已收藏" else "收藏", onClick = onToggleStar, enabled = !state.isLightManagementLoading, modifier = Modifier.weight(1f))
                TinyIconButton(R.drawable.ic_visibility_24, if (state.isWatching == true) "已关注" else "关注", onClick = onToggleWatch, enabled = !state.isLightManagementLoading, modifier = Modifier.weight(1f))
                if (repository.htmlUrl.isNotBlank()) TinyPrimaryIconButton(R.drawable.ic_open_in_new_24, "打开", onClick = { onOpenGitHub(repository.htmlUrl) }, modifier = Modifier.weight(1f))
            }
            state.lightManagementStateError?.let { Text(it, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun RootContentsCard(state: RepositoryDetailUiState.Content, onRetry: () -> Unit, onOpenFile: (RepositoryContentItem.File) -> Unit, onOpenDirectory: (RepositoryContentItem.Directory) -> Unit, onSelectBranch: (String) -> Unit, onRefreshBranches: () -> Unit, onCreateFile: () -> Unit, onUploadFile: () -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    var showBranchDialog by remember { mutableStateOf(false) }
    if (showBranchDialog) BranchSelectorDialog(state, onDismiss = { showBranchDialog = false }, onRefresh = onRefreshBranches, onSelect = { branch -> showBranchDialog = false; onSelectBranch(branch) })
    SunsetCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Column {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BranchPill(state.selectedBranch.ifBlank { state.repository.defaultBranch }, onClick = { showBranchDialog = true; if (state.branches.isEmpty() && !state.isBranchesLoading) onRefreshBranches() })
                    Text("/${state.currentPath.ifBlank { "根目录" }}", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    TinyPrimaryIconButton(R.drawable.ic_code_24, "代码", onClick = onUploadFile, enabled = state.canPush, modifier = Modifier.width(82.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp).background(colors.subtleBackground).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(colors.done))
                Column(Modifier.weight(1f)) {
                    Text("最近提交与根目录", color = colors.textPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${state.contents.size} 个条目 · ${state.repository.pushedAt ?: state.repository.updatedAt ?: "等待刷新"}", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TinyIconButton(R.drawable.ic_add_24, "新建", onClick = onCreateFile, enabled = state.canPush, modifier = Modifier.width(76.dp))
            }
            when {
                state.isContentsLoading -> Text("正在加载目录…", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                state.contentsError != null -> Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("目录加载失败：${state.contentsError}", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall); SunsetSecondaryButton("重试", onClick = onRetry) }
                state.contents.isEmpty() -> Text("该目录为空。", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                else -> state.contents.forEach { item -> ContentRow(item, onOpenFile, onOpenDirectory) }
            }
        }
    }
}

@Composable
private fun ContentRow(item: RepositoryContentItem, onOpenFile: (RepositoryContentItem.File) -> Unit, onOpenDirectory: (RepositoryContentItem.Directory) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    val icon = when (item) { is RepositoryContentItem.Directory -> R.drawable.ic_folder_24; is RepositoryContentItem.File -> R.drawable.ic_file_24; is RepositoryContentItem.Unsupported -> R.drawable.ic_block_24 }
    val iconTint = if (item is RepositoryContentItem.Directory) Color(0xFF54AEFF) else colors.textSecondary
    val subtitle = when (item) { is RepositoryContentItem.Directory -> item.path; is RepositoryContentItem.File -> "${item.path} · ${formatBytes(item.sizeBytes)}"; is RepositoryContentItem.Unsupported -> item.reason }
    Row(Modifier.fillMaxWidth().clickable { when (item) { is RepositoryContentItem.File -> onOpenFile(item); is RepositoryContentItem.Directory -> onOpenDirectory(item); is RepositoryContentItem.Unsupported -> Unit } }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(painterResource(icon), null, tint = iconTint, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) { Text(item.name, color = colors.accent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(subtitle, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
    HorizontalDivider(color = colors.divider)
}

@Composable
private fun ReadmeCard(preview: RepositoryFilePreview?, candidates: List<RepositoryContentItem.File>, error: String?, loading: Boolean, canEdit: Boolean, onRetry: () -> Unit, onSelectPreview: (RepositoryContentItem.File) -> Unit, onEdit: (RepositoryFilePreview) -> Unit, onOpenGitHub: (String) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Column {
            Row(Modifier.fillMaxWidth().background(colors.subtleBackground).padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(R.drawable.ic_book_24), null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                Column(Modifier.weight(1f)) {
                    Text("自述文件", color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    preview?.name?.let { Text(it, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                preview?.let { p -> TinyIconButton(R.drawable.ic_open_in_new_24, "原文", onClick = { p.htmlUrl?.let(onOpenGitHub) }, enabled = p.htmlUrl?.isNotBlank() == true, modifier = Modifier.width(68.dp)); TinyIconButton(R.drawable.ic_file_24, if (canEdit) "编辑" else "查看", onClick = { onEdit(p) }, modifier = Modifier.width(68.dp)) }
            }
            if (candidates.size > 1) {
                HorizontalDivider(color = colors.border)
                FlowRow(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    candidates.forEach { file -> ReadmeVariantChip(file, selected = preview?.path?.equals(file.path, ignoreCase = true) == true, onClick = { onSelectPreview(file) }) }
                }
            }
            HorizontalDivider(color = colors.border)
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when { loading -> Text("正在加载自述文件…", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall); error != null -> { Text("自述文件加载失败：$error", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall); SunsetSecondaryButton("重试", onClick = onRetry) }; preview != null -> PreviewContent(preview); else -> Text("未检测到 README / README_zh / README_cn 等自述文件。", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun PreviewContent(preview: RepositoryFilePreview) { val formatted = RepositoryFilePreviewTextFormatter.format(preview, 320); if (formatted.isMarkdown) MarkdownPreviewText(formatted.text, preview.htmlUrl) else CodePreviewText(formatted.text) }

@Composable
private fun MarkdownPreviewText(markdown: String, baseHtmlUrl: String?) {
    SelectionContainer {
        AndroidView(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            factory = { c -> TextView(c).apply { textSize = 14f; setLineSpacing(4f, 1f); includeFontPadding = false } },
            update = { it.setRepositoryMarkdown(markdown, baseHtmlUrl, "") }
        )
    }
}

@Composable
private fun CodePreviewText(text: String) { val colors = SunsetGitHubThemeTokens.colors; SelectionContainer { Text(text, Modifier.fillMaxWidth().background(colors.subtleBackground, RoundedCornerShape(10.dp)).border(BorderStroke(1.dp, colors.border), RoundedCornerShape(10.dp)).padding(12.dp), color = colors.textPrimary, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) } }

@Composable
private fun ReleasesCard(releases: List<RepositoryRelease>, onOpenReleases: () -> Unit, onOpenUrl: (String) -> Unit) = SectionCard(R.drawable.ic_tag_24, "发布版", "全部", onOpenReleases) {
    val colors = SunsetGitHubThemeTokens.colors; val latest = releases.firstOrNull()
    if (latest == null) Text("暂无发布版。", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall) else Column(Modifier.clickable { latest.htmlUrl?.let(onOpenUrl) }, verticalArrangement = Arrangement.spacedBy(6.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Text(latest.tagName.ifBlank { latest.name }, color = colors.success, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold); if (latest.isLatest) CompactBadge("Latest"); if (latest.isPrerelease) CompactBadge("Pre-release") }; Text(latest.name.ifBlank { "Release" }, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); latest.bodySummary?.takeIf { it.isNotBlank() }?.let { Text(it, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis) }; FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { latest.assets.take(3).forEach { CompactBadge(it.name) }; latest.publishedAt?.let { CompactBadge(it.take(10)) } } }
}

@Composable
private fun LanguagesCard(languages: List<RepositoryLanguage>) = SectionCard(R.drawable.ic_dashboard_black_24dp, "计算机语言比例") {
    val colors = SunsetGitHubThemeTokens.colors
    if (languages.isEmpty()) Text("暂无语言统计。", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall) else { Row(Modifier.fillMaxWidth().height(9.dp).clip(CircleShape).background(colors.divider)) { languages.take(6).forEach { Box(Modifier.weight(it.percentage.coerceAtLeast(1).toFloat()).height(9.dp).background(languageColor(it.name))) } }; Spacer(Modifier.height(10.dp)); FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { languages.take(6).forEach { LanguageLegend(it) } } }
}

@Composable
private fun ContributorsCard(contributors: List<RepositoryContributor>, onOpenSection: (RepositorySection) -> Unit, onOpenUrl: (String) -> Unit) = SectionCard(R.drawable.ic_people_24, "贡献者排行", "查看", { onOpenSection(RepositorySection.Insights) }) {
    val colors = SunsetGitHubThemeTokens.colors
    if (contributors.isEmpty()) Text("暂无贡献者数据。", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall) else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { contributors.take(5).forEachIndexed { index, c -> Row(Modifier.fillMaxWidth().clickable { c.htmlUrl?.let(onOpenUrl) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("${index + 1}", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(18.dp)); Box(Modifier.size(30.dp).clip(CircleShape).background(contributorColor(index)), contentAlignment = Alignment.Center) { Text(c.login.firstOrNull()?.uppercase().orEmpty(), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }; Column(Modifier.weight(1f)) { Text(c.login, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("核心贡献者", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall) }; Text("${c.contributions} commits", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall) } } }
}

@Composable
private fun LicenseCard(licenseFile: RepositoryContentItem.File?, onOpenFile: (RepositoryContentItem.File) -> Unit) = SectionCard(R.drawable.ic_info_24, "许可证") {
    val colors = SunsetGitHubThemeTokens.colors
    if (licenseFile == null) Text("未检测到 LICENSE / COPYING 文件。", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall) else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { CompactBadge(resolveLicenseLabel(licenseFile.name)); Text("允许范围以仓库许可证正文为准", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f)) }; Text("检测到 ${licenseFile.name}，点击可预览或进入编辑流程。", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall); SunsetSecondaryButton("查看 LICENSE", onClick = { onOpenFile(licenseFile) }) }
}

@Composable
private fun SectionCard(icon: Int, title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) { Column { Row(Modifier.fillMaxWidth().background(colors.subtleBackground).padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(painterResource(icon), null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)); Text(title, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); if (actionLabel != null && onAction != null) SunsetSecondaryButton(actionLabel, onClick = onAction) }; HorizontalDivider(color = colors.border); Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() } } }
}


@Composable
private fun BranchSelectorDialog(
    state: RepositoryDetailUiState.Content,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSelect: (String) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    val fallbackBranch = state.repository.defaultBranch.ifBlank { "main" }
    val currentBranch = state.selectedBranch.ifBlank { fallbackBranch }
    val branches = state.branches.takeIf { it.isNotEmpty() }
        ?: listOf(RepositoryBranch(name = currentBranch, isDefault = currentBranch == fallbackBranch))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("切换分支", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("选择后会回到仓库根目录，并按该分支重新加载文件与 README。", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                if (state.isBranchesLoading) {
                    Text("正在读取分支列表…", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                }
                state.branchesError?.takeIf { it.isNotBlank() }?.let { error ->
                    Text("分支列表加载失败：$error", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    branches.take(30).forEach { branch ->
                        BranchSelectorRow(
                            branch = branch,
                            selected = branch.name == currentBranch,
                            isDefault = branch.name == fallbackBranch || branch.isDefault,
                            enabled = !state.isBranchesLoading,
                            onClick = { onSelect(branch.name) }
                        )
                    }
                }
            }
        },
        confirmButton = { SunsetSecondaryButton("刷新", onClick = onRefresh) },
        dismissButton = { SunsetSecondaryButton("取消", onClick = onDismiss) }
    )
}

@Composable
private fun BranchSelectorRow(
    branch: RepositoryBranch,
    selected: Boolean,
    isDefault: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Surface(
        modifier = Modifier.fillMaxWidth().then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) colors.accentSoft else colors.surface,
        border = BorderStroke(1.dp, if (selected) colors.accent else colors.border)
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_branch_24), null, tint = if (selected) colors.accent else colors.textSecondary, modifier = Modifier.size(16.dp))
            Text(branch.name, color = if (selected) colors.accent else colors.textPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (isDefault) CompactBadge("默认")
            if (branch.isProtected) CompactBadge("保护")
        }
    }
}

@Composable private fun BranchPill(branch: String, onClick: () -> Unit) { val colors = SunsetGitHubThemeTokens.colors; Surface(modifier = Modifier.height(32.dp).clickable(onClick = onClick), shape = RoundedCornerShape(8.dp), color = colors.surface, border = BorderStroke(1.dp, colors.border)) { Row(Modifier.padding(horizontal = 9.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) { Icon(painterResource(R.drawable.ic_branch_24), null, tint = colors.textSecondary, modifier = Modifier.size(15.dp)); Text(branch, color = colors.textPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1); Text("切换", color = colors.accent, style = MaterialTheme.typography.labelSmall, maxLines = 1) } } }
@Composable private fun MetaChip(icon: Int, text: String) { val colors = SunsetGitHubThemeTokens.colors; Surface(shape = CircleShape, color = colors.surface, border = BorderStroke(1.dp, colors.border)) { Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(painterResource(icon), null, tint = colors.textSecondary, modifier = Modifier.size(14.dp)); Text(text, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall) } } }
@Composable private fun LanguageChip(name: String) { val colors = SunsetGitHubThemeTokens.colors; Surface(shape = CircleShape, color = colors.surface, border = BorderStroke(1.dp, colors.border)) { Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(9.dp).clip(CircleShape).background(languageColor(name))); Text(name, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall) } } }
@Composable private fun ReadmeVariantChip(file: RepositoryContentItem.File, selected: Boolean, onClick: () -> Unit) { val colors = SunsetGitHubThemeTokens.colors; Surface(modifier = Modifier.clickable(onClick = onClick), shape = CircleShape, color = if (selected) colors.accentSoft else colors.surface, border = BorderStroke(1.dp, if (selected) colors.accent else colors.border)) { Text(file.name, color = if (selected) colors.accent else colors.textSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis) } }
@Composable private fun LanguageLegend(language: RepositoryLanguage) { val colors = SunsetGitHubThemeTokens.colors; Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Box(Modifier.size(9.dp).clip(CircleShape).background(languageColor(language.name))); Text(language.name, color = colors.textPrimary, style = MaterialTheme.typography.labelSmall); Text("${language.percentage}%", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall) } }
@Composable private fun CompactBadge(text: String) { val colors = SunsetGitHubThemeTokens.colors; Surface(color = colors.accentSoft, shape = CircleShape) { Text(text, color = colors.accent, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), maxLines = 1) } }

@Composable private fun TinyPrimaryIconButton(icon: Int, text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) { val colors = SunsetGitHubThemeTokens.colors; Surface(modifier = modifier.height(30.dp).then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier), color = if (enabled) colors.accent else colors.divider, shape = CircleShape) { Row(Modifier.padding(horizontal = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Icon(painterResource(icon), null, tint = Color.White, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text(text, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1) } } }
@Composable private fun TinyIconButton(icon: Int, text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) { val colors = SunsetGitHubThemeTokens.colors; Surface(modifier = modifier.height(30.dp).then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier), color = colors.surface, shape = CircleShape, border = BorderStroke(1.dp, if (enabled) colors.border else colors.divider)) { Row(Modifier.padding(horizontal = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Icon(painterResource(icon), null, tint = if (enabled) colors.accent else colors.textSecondary, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text(text, color = if (enabled) colors.accent else colors.textSecondary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1) } } }

private fun resolveLanguages(state: RepositoryDetailUiState.Content): List<RepositoryLanguage> = state.sidebarInfo?.languages?.takeIf { it.isNotEmpty() } ?: state.repository.languages.takeIf { it.isNotEmpty() } ?: state.repository.language?.takeIf { it.isNotBlank() }?.let { listOf(RepositoryLanguage(it, 0L, 100)) } ?: emptyList()
private fun readmePreviewCandidates(contents: List<RepositoryContentItem>): List<RepositoryContentItem.File> = contents.filterIsInstance<RepositoryContentItem.File>().filter { it.name.isReadmeVariantName() }.sortedWith(compareBy<RepositoryContentItem.File> { readmeVariantRank(it.name) }.thenBy { it.name.lowercase() })
private fun String.isLicenseLikeName(): Boolean { val lower = substringAfterLast('/').lowercase(); return lower == "license" || lower.startsWith("license.") || lower == "copying" || lower.startsWith("copying.") || lower == "licence" || lower.startsWith("licence.") }
private fun String.isReadmeVariantName(): Boolean { val lower = substringAfterLast('/').lowercase(); if (lower == "readme") return true; if (!lower.startsWith("readme")) return false; val suffix = lower.removePrefix("readme"); return suffix.isEmpty() || suffix.first() in listOf('.', '_', '-') }
private fun readmeVariantRank(name: String): Int { val lower = name.substringAfterLast('/').lowercase(); return when { lower == "readme.md" -> 0; lower == "readme_zh.md" || lower == "readme-zh.md" || lower == "readme.zh.md" -> 1; lower == "readme_cn.md" || lower == "readme-cn.md" || lower == "readme.cn.md" -> 2; lower.startsWith("readme") && lower.endsWith(".md") -> 3; lower == "readme" -> 4; else -> 5 } }
private fun resolveLicenseLabel(name: String): String { val lower = name.lowercase(); return when { "mit" in lower -> "MIT License"; "apache" in lower -> "Apache License"; "gpl" in lower -> "GPL License"; else -> "License" } }
private fun formatBytes(bytes: Long): String = when { bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"; bytes >= 1024 -> "${bytes / 1024} KB"; else -> "$bytes B" }
private fun compactCount(count: Int): String = when { count >= 1_000_000 -> "${count / 1_000_000}M"; count >= 1_000 -> "${count / 1_000}K"; else -> count.toString() }
private fun languageColor(name: String): Color = when (name.lowercase()) { "kotlin" -> Color(0xFFA97BFF); "java" -> Color(0xFFB07219); "xml" -> Color(0xFF0060AC); "gradle", "groovy" -> Color(0xFF02303A); "markdown" -> Color(0xFF083FA1); "shell", "bash" -> Color(0xFF89E051); "python" -> Color(0xFF3572A5); "javascript" -> Color(0xFFF1E05A); "typescript" -> Color(0xFF3178C6); else -> Color(0xFF6E7781) }
private fun contributorColor(index: Int): Color = listOf(Color(0xFF8250DF), Color(0xFF1A7F37), Color(0xFFFB8500), Color(0xFF0969DA), Color(0xFFCF222E))[index % 5]
