package com.Sunset.REN.GitHub.ui.compose.screens.repo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.data.github.html.RepositoryDeployKeyItem
import com.Sunset.REN.GitHub.data.github.html.RepositoryRulesetItem
import com.Sunset.REN.GitHub.data.github.html.RepositoryWebhookItem
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetLoadingState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.repo.RepositoryDeployKeysUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryRulesetsUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryWebhooksUiState

@Composable
fun RepositoryRulesetsScreen(
    state: RepositoryRulesetsUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    RepositoryAdminLazyColumn(modifier) {
        item {
            RepositoryAdminHeaderCard(
                title = "Rulesets",
                subtitle = "仓库规则集用于统一约束分支、标签、推送和合并行为。当前页面先提供只读查看，编辑请继续使用 GitHub 网页端。"
            )
        }
        if (state.isLoading) {
            item { SunsetLoadingState(message = "正在加载 Rulesets……") }
        }
        state.errorMessage?.let { message -> item { RepositoryAdminInfoCard(message, isError = true) } }
        state.snapshot?.let { snapshot ->
            item {
                RepositoryAdminSummaryCard(
                    title = "${snapshot.owner}/${snapshot.repo}",
                    subtitle = if (snapshot.canAdmin) "可管理" else "只读"
                )
            }
            item {
                SunsetSecondaryButton(
                    text = "刷新",
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (snapshot.rulesets.isEmpty()) {
                item { RepositoryAdminEmptyCard("暂无仓库 Ruleset。", "规则集创建和编辑请继续使用 GitHub 网页端。") }
            } else {
                items(snapshot.rulesets, key = { it.id }) { item -> RulesetCard(item) }
            }
        }
        item { RepositoryAdminBottomSpacer() }
    }
}

@Composable
fun RepositoryDeployKeysScreen(
    state: RepositoryDeployKeysUiState,
    onRefresh: () -> Unit,
    onAddKey: () -> Unit,
    onDeleteKey: (RepositoryDeployKeyItem) -> Unit,
    modifier: Modifier = Modifier
) {
    RepositoryAdminLazyColumn(modifier) {
        item {
            RepositoryAdminHeaderCard(
                title = "部署密钥",
                subtitle = "为部署服务器或自动化系统授予仓库级 SSH 访问权限。建议优先使用只读密钥，谨慎开放写权限。"
            )
        }
        if (state.isLoading) {
            item { SunsetLoadingState(message = "正在加载部署密钥……") }
        }
        state.errorMessage?.let { message -> item { RepositoryAdminInfoCard(message, isError = true) } }
        state.pendingMessage?.takeIf { !state.isSaving }?.let { message -> item { RepositoryAdminInfoCard(message) } }
        state.snapshot?.let { snapshot ->
            val canEdit = snapshot.canAdmin && !state.isSaving
            item {
                RepositoryAdminSummaryCard(
                    title = "${snapshot.owner}/${snapshot.repo}",
                    subtitle = if (snapshot.canAdmin) "管理员权限 · 可管理部署密钥" else "只读模式 · 无管理员权限"
                )
            }
            item {
                RepositoryAdminActionRow {
                    SunsetSecondaryButton("刷新", onRefresh, Modifier.weight(1f))
                    SunsetPrimaryButton("新增部署密钥", onAddKey, Modifier.weight(1f), enabled = canEdit)
                }
            }
            if (snapshot.keys.isEmpty()) {
                item { RepositoryAdminEmptyCard("暂无部署密钥", "点击“新增部署密钥”添加 SSH 公钥。") }
            } else {
                items(snapshot.keys, key = { it.id }) { key -> DeployKeyCard(key, canEdit, onDeleteKey) }
            }
        }
        item { RepositoryAdminBottomSpacer() }
    }
}

@Composable
fun RepositoryWebhooksScreen(
    state: RepositoryWebhooksUiState,
    onRefresh: () -> Unit,
    onCreateWebhook: () -> Unit,
    onPingWebhook: (RepositoryWebhookItem) -> Unit,
    onDeleteWebhook: (RepositoryWebhookItem) -> Unit,
    modifier: Modifier = Modifier
) {
    RepositoryAdminLazyColumn(modifier) {
        item {
            RepositoryAdminHeaderCard(
                title = "Webhooks",
                subtitle = "向外部服务发送仓库事件通知，适合 CI、部署、机器人和审计集成。"
            )
        }
        if (state.isLoading) {
            item { SunsetLoadingState(message = "正在加载 Webhooks……") }
        }
        state.errorMessage?.let { message -> item { RepositoryAdminInfoCard(message, isError = true) } }
        state.pendingMessage?.takeIf { !state.isSaving }?.let { message -> item { RepositoryAdminInfoCard(message) } }
        state.snapshot?.let { snapshot ->
            val canEdit = snapshot.canAdmin && !state.isSaving
            item {
                RepositoryAdminSummaryCard(
                    title = "${snapshot.owner}/${snapshot.repo}",
                    subtitle = if (snapshot.canAdmin) "管理员权限 · 可管理 Webhook" else "只读模式 · 无管理员权限"
                )
            }
            item {
                RepositoryAdminActionRow {
                    SunsetSecondaryButton("刷新", onRefresh, Modifier.weight(1f))
                    SunsetPrimaryButton("新增 Webhook", onCreateWebhook, Modifier.weight(1f), enabled = canEdit)
                }
            }
            if (snapshot.hooks.isEmpty()) {
                item { RepositoryAdminEmptyCard("暂无 Webhook", "点击“新增 Webhook”配置 Payload URL 和事件列表。") }
            } else {
                items(snapshot.hooks, key = { it.id }) { hook -> WebhookCard(hook, canEdit, onPingWebhook, onDeleteWebhook) }
            }
        }
        item { RepositoryAdminBottomSpacer() }
    }
}

@Composable
private fun RepositoryAdminLazyColumn(
    modifier: Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun RepositoryAdminHeaderCard(title: String, subtitle: String) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(title, color = colors.textPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(subtitle, modifier = Modifier.padding(top = 8.dp), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RepositoryAdminSummaryCard(title: String, subtitle: String) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(Modifier.fillMaxWidth()) {
        Text("仓库", color = colors.textMuted, style = MaterialTheme.typography.labelMedium)
        Text(title, modifier = Modifier.padding(top = 4.dp), color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, modifier = Modifier.padding(top = 6.dp), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RepositoryAdminInfoCard(message: String, isError: Boolean = false) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(Modifier.fillMaxWidth()) {
        Text(message, color = if (isError) colors.danger else colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RepositoryAdminEmptyCard(title: String, subtitle: String) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, modifier = Modifier.padding(top = 6.dp), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RepositoryAdminActionRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
private fun RulesetCard(item: RepositoryRulesetItem) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(Modifier.fillMaxWidth()) {
        Text(item.name.ifBlank { "#${item.id}" }, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        AdminBody("目标：${item.target.ifBlank { "未知" }} · 执行：${item.enforcement.ifBlank { "未知" }} · 来源：${item.sourceType.ifBlank { "未知" }}")
        AdminBody("规则数量：${item.rulesCount} · 类型：${item.ruleTypes.joinToString().ifBlank { "未返回" }}")
        item.conditionsSummary.forEach { AdminBody(it) }
        AdminBody("更新：${item.updatedAt.ifBlank { item.createdAt.ifBlank { "未知" } }}")
    }
}

@Composable
private fun DeployKeyCard(item: RepositoryDeployKeyItem, enabled: Boolean, onDelete: (RepositoryDeployKeyItem) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(Modifier.fillMaxWidth()) {
        Text(if (item.readOnly) "只读" else "读写", color = if (item.readOnly) colors.textMuted else colors.danger, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(item.title.ifBlank { "#${item.id}" }, modifier = Modifier.padding(top = 4.dp), color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        AdminBody("验证状态：${if (item.verified) "已验证" else "未验证"} · 创建：${item.createdAt.ifBlank { "未知" }}")
        AdminBody(item.key.take(96).ifBlank { "未返回公钥内容" })
        SunsetSecondaryButton("删除", { onDelete(item) }, Modifier.fillMaxWidth().padding(top = 10.dp), enabled = enabled)
    }
}

@Composable
private fun WebhookCard(
    item: RepositoryWebhookItem,
    enabled: Boolean,
    onPing: (RepositoryWebhookItem) -> Unit,
    onDelete: (RepositoryWebhookItem) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(Modifier.fillMaxWidth()) {
        Text(if (item.active) "启用" else "停用", color = if (item.active) colors.accent else colors.textMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text("#${item.id}  ${item.url.ifBlank { "未返回 Payload URL" }}", modifier = Modifier.padding(top = 4.dp), color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        AdminBody("事件：${item.events.joinToString().ifBlank { "未返回" }}")
        AdminBody("内容类型：${item.contentType.ifBlank { "json" }} · SSL：${if (item.insecureSsl) "允许不安全 SSL" else "验证 SSL"}")
        AdminBody("最近响应：${listOf(item.lastResponseCode, item.lastResponseStatus, item.lastResponseMessage).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "暂无" }}")
        RepositoryAdminActionRow {
            SunsetSecondaryButton("Ping", { onPing(item) }, Modifier.weight(1f), enabled = enabled)
            SunsetSecondaryButton("删除", { onDelete(item) }, Modifier.weight(1f), enabled = enabled)
        }
    }
}

@Composable
private fun AdminBody(text: String) {
    val colors = SunsetGitHubThemeTokens.colors
    Text(text, modifier = Modifier.padding(top = 6.dp), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun RepositoryAdminBottomSpacer() {
    androidx.compose.foundation.layout.Box(Modifier.padding(bottom = 20.dp))
}
