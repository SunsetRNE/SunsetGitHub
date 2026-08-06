package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.data.github.html.RepositoryActionsCacheItem
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryActionsSettingsUiState
import com.Sunset.REN.GitHub.ui.repo.toAllowedActionsText
import com.Sunset.REN.GitHub.ui.repo.toReadableBytes
import com.Sunset.REN.GitHub.ui.repo.toWorkflowPermissionText
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.SectionHeaderComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.StateComponent
import com.Sunset.REN.GitHub.ui.schema.StateKind
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState

/**
 * 仓库 Actions 设置页垂直切片（组 B：仓库设置/管理）。
 *
 * 渲染结构对齐 RepositoryActionsSettingsScreen：
 * - 初始加载 → Loading；错误（无 snapshot）→ Error + 重试；
 * - Content：inline 消息 → 启用/禁用标题卡（开启 accent / 关闭 danger +
 *   摘要：开关态 · 工作流默认权限 · 可修改/只读）→ 概览卡（九指标 label/value 行）→
 *   权限卡（enable_all/disable_all + allowed_actions 三选 + 选中 Actions 子区：
 *   githubOwned/verified/patterns/编辑白名单）→ 工作流卡（read/write/PR 审批三选）→
 *   保留期卡 → 存储卡（secret/variable 指标 + 增删改查行）→ 缓存卡（用量/刷新/按 key 删除/列表）。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：actions_settings.set_enabled.{true,false} / set_allowed.{all,local_only,selected} /
 *   toggle_github_owned / toggle_verified / edit_patterns / set_workflow.{read,write} /
 *   toggle_pr_approval / edit_retention / refresh_secrets_variables / secret.click.{name} /
 *   variable.click.{name} / upsert_secret / upsert_variable / refresh_caches /
 *   delete_caches_by_key / cache.click.{id} / retry / shell.back。
 * 全部确认/编辑 Dialog（启用禁用/写权限/保留期/缓存/白名单/secret/variable）由调用端承载。
 */
object RepositoryActionsSettingsPage {

    fun schemaFor(
        state: RepositoryActionsSettingsUiState,
        onRetry: () -> Unit = {},
    ): PageSchema {
        val rows = buildList<RowSchema> {
            when {
                state.isInitialLoad -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "actions_settings.loading",
                                kind = StateKind.Loading,
                                message = "正在加载 Actions 设置…",
                            ),
                        ),
                    ),
                )

                state.errorMessage != null && state.snapshot == null -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "actions_settings.error",
                                kind = StateKind.Error,
                                message = "Actions 设置暂时不可用",
                                detail = state.errorMessage,
                                retryAction = "actions_settings.retry",
                            ),
                        ),
                    ),
                )

                state.snapshot != null -> {
                    val snapshot = state.snapshot
                    val enabled = snapshot.actionsPermissions?.enabled == true
                    val canEdit = snapshot.canAdmin && !state.isSaving
                    // —— inline 消息 ——
                    val inlineMessage = when {
                        state.isSaving -> state.pendingMessage.orEmpty()
                        !state.errorMessage.isNullOrBlank() -> state.errorMessage.orEmpty()
                        else -> ""
                    }
                    if (inlineMessage.isNotBlank()) {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "actions_settings.inline",
                                        text = inlineMessage,
                                        style = TextStyle.Body,
                                        color = if (!state.errorMessage.isNullOrBlank()) TextColor.Danger else TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                    }
                    // —— 启用/禁用标题卡 ——
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "actions_settings.title",
                                    text = if (enabled) "Actions 已启用" else "Actions 已禁用",
                                    style = TextStyle.Section,
                                    color = if (enabled) TextColor.Accent else TextColor.Danger,
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "actions_settings.summary",
                                    text = "状态：${if (enabled) "开启" else "关闭"} · 工作流默认权限：${snapshot.workflowPermissions?.defaultWorkflowPermissions.toWorkflowPermissionText()} · ${if (snapshot.canAdmin) "可修改" else "只读"}",
                                    style = TextStyle.Body,
                                    color = TextColor.Secondary,
                                ),
                            ),
                        ),
                    )
                    // —— 概览卡：九指标 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "actions_settings.overview",
                                    title = "概览",
                                ),
                            ),
                        ),
                    )
                    state.metrics.forEachIndexed { index, metric ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "actions_settings.metric.$index.label",
                                        text = metric.label,
                                        style = TextStyle.Meta,
                                        color = TextColor.Muted,
                                    ),
                                    span = 5,
                                ),
                                cell(
                                    TextComponent(
                                        id = "actions_settings.metric.$index.value",
                                        text = metric.value,
                                        style = TextStyle.Body,
                                        color = TextColor.Primary,
                                    ),
                                    span = 7,
                                ),
                            ),
                        )
                    }
                    add(row(cell(SpacerComponent(id = "actions_settings.spacer.permissions", heightDp = 8))))
                    // —— 权限卡 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "actions_settings.permissions_header",
                                    title = "权限",
                                    subtitle = "控制哪些工作流可以在本仓库运行。",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "actions_settings.enable_all",
                                    title = "启用所有操作",
                                    badge = if (enabled) "当前" else null,
                                    badgeColor = TextColor.Success,
                                    action = if (canEdit && !enabled) "actions_settings.set_enabled.true" else "",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "actions_settings.disable_all",
                                    title = "禁用所有操作",
                                    badge = if (!enabled) "当前" else null,
                                    badgeColor = TextColor.Danger,
                                    action = if (canEdit && enabled) "actions_settings.set_enabled.false" else "",
                                ),
                            ),
                        ),
                    )
                    val allowed = snapshot.actionsPermissions?.allowedActions ?: "unknown"
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "actions_settings.allowed_summary",
                                    text = "允许运行范围：${allowed.toAllowedActionsText()}",
                                    style = TextStyle.Caption,
                                    color = TextColor.Muted,
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "actions_settings.allowed.all",
                                    title = "允许所有操作和可复用工作流",
                                    badge = if (allowed == "all") "当前" else null,
                                    badgeColor = TextColor.Success,
                                    action = if (canEdit && allowed != "all") "actions_settings.set_allowed.all" else "",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "actions_settings.allowed.local_only",
                                    title = "仅允许本仓库内的操作和可复用工作流",
                                    badge = if (allowed == "local_only") "当前" else null,
                                    badgeColor = TextColor.Success,
                                    action = if (canEdit && allowed != "local_only") "actions_settings.set_allowed.local_only" else "",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "actions_settings.allowed.selected",
                                    title = "仅允许选定操作和可复用工作流",
                                    badge = if (allowed == "selected") "当前" else null,
                                    badgeColor = TextColor.Success,
                                    action = if (canEdit && allowed != "selected") "actions_settings.set_allowed.selected" else "",
                                ),
                            ),
                        ),
                    )
                    snapshot.selectedActions?.let { selected ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "actions_settings.selected_header",
                                        text = "允许列表",
                                        style = TextStyle.Subtitle,
                                        color = TextColor.Primary,
                                    ),
                                ),
                            ),
                        )
                        add(
                            row(
                                cell(
                                    ItemComponent(
                                        id = "actions_settings.selected.github_owned",
                                        title = "允许 GitHub 拥有操作",
                                        badge = if (selected.githubOwnedAllowed) "开" else null,
                                        badgeColor = TextColor.Success,
                                        action = if (canEdit) "actions_settings.toggle_github_owned" else "",
                                    ),
                                ),
                            ),
                        )
                        add(
                            row(
                                cell(
                                    ItemComponent(
                                        id = "actions_settings.selected.verified",
                                        title = "允许已验证 Marketplace 操作",
                                        badge = if (selected.verifiedAllowed) "开" else null,
                                        badgeColor = TextColor.Success,
                                        action = if (canEdit) "actions_settings.toggle_verified" else "",
                                    ),
                                ),
                            ),
                        )
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "actions_settings.selected.patterns",
                                        text = "允许的模式：${selected.patternsAllowed.joinToString().ifBlank { "（空）" }}",
                                        style = TextStyle.Caption,
                                        color = TextColor.Muted,
                                    ),
                                ),
                            ),
                        )
                        add(
                            row(
                                cell(
                                    ItemComponent(
                                        id = "actions_settings.selected.edit_patterns",
                                        title = "编辑允许模式",
                                        action = if (canEdit) "actions_settings.edit_patterns" else "",
                                    ),
                                ),
                            ),
                        )
                    }
                    add(row(cell(SpacerComponent(id = "actions_settings.spacer.workflow", heightDp = 8))))
                    // —— 工作流卡 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "actions_settings.workflow_header",
                                    title = "工作流",
                                    subtitle = "工作流默认读写权限与 PR 审批设置。",
                                ),
                            ),
                        ),
                    )
                    val workflowPermission = snapshot.workflowPermissions?.defaultWorkflowPermissions ?: "read"
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "actions_settings.workflow.read",
                                    title = "只读仓库内容",
                                    badge = if (workflowPermission == "read") "当前" else null,
                                    badgeColor = TextColor.Success,
                                    action = if (canEdit && workflowPermission != "read") "actions_settings.set_workflow.read" else "",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "actions_settings.workflow.write",
                                    title = "读写仓库内容",
                                    badge = if (workflowPermission == "write") "当前" else null,
                                    badgeColor = TextColor.Success,
                                    action = if (canEdit && workflowPermission != "write") "actions_settings.set_workflow.write" else "",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "actions_settings.workflow.pr_approval",
                                    title = "允许工作流批准拉取请求",
                                    badge = if (snapshot.workflowPermissions?.canApprovePullRequestReviews == true) "开" else null,
                                    badgeColor = TextColor.Success,
                                    action = if (canEdit) "actions_settings.toggle_pr_approval" else "",
                                ),
                            ),
                        ),
                    )
                    add(row(cell(SpacerComponent(id = "actions_settings.spacer.retention", heightDp = 8))))
                    // —— 保留期卡 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "actions_settings.retention_header",
                                    title = "保留期",
                                    subtitle = snapshot.retentionDays?.let { "产物与日志保留 $it 天" } ?: "保留期不可读",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "actions_settings.retention.edit",
                                    title = "编辑保留天数",
                                    action = if (canEdit) "actions_settings.edit_retention" else "",
                                ),
                            ),
                        ),
                    )
                    add(row(cell(SpacerComponent(id = "actions_settings.spacer.storage", heightDp = 8))))
                    // —— 存储卡 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "actions_settings.storage_header",
                                    title = "存储",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "actions_settings.storage.secrets_metric",
                                    text = "密钥数量",
                                    style = TextStyle.Meta,
                                    color = TextColor.Muted,
                                ),
                                span = 5,
                            ),
                            cell(
                                TextComponent(
                                    id = "actions_settings.storage.secrets_metric_value",
                                    text = snapshot.secretsCount?.toString() ?: "不可读",
                                    style = TextStyle.Body,
                                    color = TextColor.Primary,
                                ),
                                span = 7,
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "actions_settings.storage.variables_metric",
                                    text = "变量数量",
                                    style = TextStyle.Meta,
                                    color = TextColor.Muted,
                                ),
                                span = 5,
                            ),
                            cell(
                                TextComponent(
                                    id = "actions_settings.storage.variables_metric_value",
                                    text = snapshot.variablesCount?.toString() ?: "不可读",
                                    style = TextStyle.Body,
                                    color = TextColor.Primary,
                                ),
                                span = 7,
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "actions_settings.storage.refresh",
                                    title = "刷新密钥与变量列表",
                                    action = "actions_settings.refresh_secrets_variables",
                                ),
                            ),
                        ),
                    )
                    state.secrets.forEachIndexed { index, secret ->
                        add(
                            row(
                                cell(
                                    ItemComponent(
                                        id = "actions_settings.storage.secret.$index",
                                        title = "密钥 ${secret.name}",
                                        subtitle = "更新：${secret.updatedAt.ifBlank { "未知" }}",
                                        action = if (canEdit) "actions_settings.secret.click.${secret.name}" else "",
                                    ),
                                ),
                            ),
                        )
                    }
                    state.variables.forEachIndexed { index, variable ->
                        add(
                            row(
                                cell(
                                    ItemComponent(
                                        id = "actions_settings.storage.variable.$index",
                                        title = "变量 ${variable.name}",
                                        subtitle = "值：${variable.value.ifBlank { "（空）" }} · 更新：${variable.updatedAt.ifBlank { "未知" }}",
                                        action = if (canEdit) "actions_settings.variable.click.${variable.name}" else "",
                                    ),
                                ),
                            ),
                        )
                    }
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "actions_settings.storage.upsert_secret",
                                    title = "新增/更新密钥",
                                    action = if (canEdit) "actions_settings.upsert_secret" else "",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "actions_settings.storage.upsert_variable",
                                    title = "新增/更新变量",
                                    action = if (canEdit) "actions_settings.upsert_variable" else "",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "actions_settings.storage.note",
                                    text = "密钥值加密存储，仅可覆盖或删除，不可回读。",
                                    style = TextStyle.Caption,
                                    color = TextColor.Muted,
                                ),
                            ),
                        ),
                    )
                    add(row(cell(SpacerComponent(id = "actions_settings.spacer.cache", heightDp = 8))))
                    // —— 缓存卡 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "actions_settings.cache_header",
                                    title = "缓存",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "actions_settings.cache.usage",
                                    text = "当前用量：${snapshot.cacheUsage?.let { "${it.activeCachesCount} 个 · ${it.activeCachesSizeInBytes.toReadableBytes()}" } ?: "不可读"}",
                                    style = TextStyle.Body,
                                    color = TextColor.Secondary,
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "actions_settings.cache.refresh",
                                    title = "刷新缓存列表",
                                    action = "actions_settings.refresh_caches",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "actions_settings.cache.delete_by_key",
                                    title = "按 key 删除缓存",
                                    action = if (canEdit) "actions_settings.delete_caches_by_key" else "",
                                ),
                            ),
                        ),
                    )
                    if (state.caches.isEmpty()) {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "actions_settings.cache.empty",
                                        text = "暂无缓存条目（点击上方刷新加载）。",
                                        style = TextStyle.Caption,
                                        color = TextColor.Muted,
                                    ),
                                ),
                            ),
                        )
                    }
                    state.caches.forEachIndexed { index, cache ->
                        add(
                            row(
                                cell(
                                    ItemComponent(
                                        id = "actions_settings.cache.item.$index",
                                        title = cache.key.ifBlank { "#${cache.id}" },
                                        subtitle = "ref：${cache.ref.ifBlank { "未知" }} · ${cache.sizeInBytes.toReadableBytes()} · 最近访问：${cache.lastAccessedAt.ifBlank { "未知" }}",
                                        action = if (canEdit) "actions_settings.cache.click.${cache.id}" else "",
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }
        }
        return PageSchema(id = "actions_settings", columns = 12, scrollable = true, rows = rows)
    }

    /** Actions 设置页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(): ShellState = ShellState(
        title = "Actions 设置",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "actions_settings",
    )
}

/**
 * 仓库 Actions 设置页入口：壳 + 三分支 schema。
 * 全部确认/编辑 Dialog（启用禁用/写权限/保留期/缓存/白名单/secret/variable）由调用端承载。
 */
@Composable
fun RepositoryActionsSettingsPageContent(
    state: RepositoryActionsSettingsUiState,
    onRetry: () -> Unit = {},
    onSetActionsEnabled: (Boolean) -> Unit = {},
    onSetAllowedActions: (String) -> Unit = {},
    onToggleGithubOwned: () -> Unit = {},
    onToggleVerified: () -> Unit = {},
    onEditPatterns: () -> Unit = {},
    onSetWorkflowPermission: (String) -> Unit = {},
    onTogglePrApproval: () -> Unit = {},
    onEditRetention: () -> Unit = {},
    onRefreshSecretsVariables: () -> Unit = {},
    onSecretClick: (String) -> Unit = {},
    onVariableClick: (String) -> Unit = {},
    onUpsertSecret: () -> Unit = {},
    onUpsertVariable: () -> Unit = {},
    onRefreshCaches: () -> Unit = {},
    onDeleteCachesByKey: () -> Unit = {},
    onCacheClick: (RepositoryActionsCacheItem) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "shell.back" -> onBack()
            action == "actions_settings.retry" -> onRetry()
            action == "actions_settings.set_enabled.true" -> onSetActionsEnabled(true)
            action == "actions_settings.set_enabled.false" -> onSetActionsEnabled(false)
            action == "actions_settings.set_allowed.all" -> onSetAllowedActions("all")
            action == "actions_settings.set_allowed.local_only" -> onSetAllowedActions("local_only")
            action == "actions_settings.set_allowed.selected" -> onSetAllowedActions("selected")
            action == "actions_settings.toggle_github_owned" -> onToggleGithubOwned()
            action == "actions_settings.toggle_verified" -> onToggleVerified()
            action == "actions_settings.edit_patterns" -> onEditPatterns()
            action == "actions_settings.set_workflow.read" -> onSetWorkflowPermission("read")
            action == "actions_settings.set_workflow.write" -> onSetWorkflowPermission("write")
            action == "actions_settings.toggle_pr_approval" -> onTogglePrApproval()
            action == "actions_settings.edit_retention" -> onEditRetention()
            action == "actions_settings.refresh_secrets_variables" -> onRefreshSecretsVariables()
            action == "actions_settings.upsert_secret" -> onUpsertSecret()
            action == "actions_settings.upsert_variable" -> onUpsertVariable()
            action == "actions_settings.refresh_caches" -> onRefreshCaches()
            action == "actions_settings.delete_caches_by_key" -> onDeleteCachesByKey()
            action.startsWith("actions_settings.secret.click.") -> onSecretClick(action.removePrefix("actions_settings.secret.click."))
            action.startsWith("actions_settings.variable.click.") -> onVariableClick(action.removePrefix("actions_settings.variable.click."))
            action.startsWith("actions_settings.cache.click.") -> {
                val id = action.removePrefix("actions_settings.cache.click.").toLongOrNull()
                state.caches.firstOrNull { it.id == id }?.let(onCacheClick)
            }
        }
    }
    AppShell(state = RepositoryActionsSettingsPage.shellState(), onAction = handleAction) {
        RepositoryActionsSettingsPage.schemaFor(state, onRetry).renderPage(handleAction)
    }
}