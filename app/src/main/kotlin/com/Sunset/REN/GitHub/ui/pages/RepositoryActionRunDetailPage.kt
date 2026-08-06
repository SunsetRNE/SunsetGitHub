package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionArtifact
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRunDetail
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRunLogPreview
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryActionRunDetailUiState
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Actions 运行详情页垂直切片（组 C：Actions 运行子页）。
 *
 * 渲染结构对齐 RepositoryActionRunDetailScreen：
 * - Loading（无 run）/ Error（无 run + 重试）/ Unavailable（无 run + 可在 GitHub 打开）/
 *   空态（缺少目标）四分支；Content 五卡：
 * - StatusMessage 卡（刷新中/错误+重试/不可用/缓存时间）→ Hero 卡（标题 #run + 状态徽章
 *   OK/!/.../? 按状态着色 + meta/摘要 + 总时长·产物数双统计 + 开始/更新时间）→
 *   Workflow 卡（工作流名 + 事件 + 路径）→ Logs 卡（logMeta + 刷新 + 等宽预览 18 行 + 下载）→
 *   Artifacts 卡（数量 + 状态 + ArtifactRow：名称/大小/下载·已过期）→ 在 GitHub 打开。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：action_run_detail.retry / open_actions / open_run / refresh_logs / download_logs /
 *   download_artifact.{id} / shell.back。下载/打开由调用端承载。
 */
object RepositoryActionRunDetailPage {

    fun schemaFor(
        state: RepositoryActionRunDetailUiState,
        onRetry: () -> Unit = {},
        onOpenActions: () -> Unit = {},
        onOpenRun: (String) -> Unit = {},
        onRefreshLogs: () -> Unit = {},
        onDownloadLogs: () -> Unit = {},
        onDownloadArtifact: (RepositoryActionArtifact) -> Unit = {},
    ): PageSchema {
        val actionRun = state.actionRun
        val rows = buildList<RowSchema> {
            when {
                state.isLoading && actionRun == null -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "action_run_detail.loading",
                                kind = StateKind.Loading,
                                message = "正在加载运行详情…",
                            ),
                        ),
                    ),
                )

                actionRun == null && !state.errorMessage.isNullOrBlank() -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "action_run_detail.error",
                                kind = StateKind.Error,
                                message = state.errorMessage,
                                detail = null,
                                retryAction = "action_run_detail.retry",
                            ),
                        ),
                    ),
                )

                actionRun == null && !state.unavailableMessage.isNullOrBlank() -> {
                    add(
                        row(
                            cell(
                                StateComponent(
                                    id = "action_run_detail.unavailable",
                                    kind = StateKind.Empty,
                                    message = state.unavailableMessage,
                                ),
                            ),
                        ),
                    )
                    if (state.actionsHtmlUrl != null) {
                        add(
                            row(
                                cell(
                                    ButtonComponent(
                                        id = "action_run_detail.open_actions",
                                        text = "在 GitHub 打开",
                                        kind = ButtonKind.Secondary,
                                        action = "action_run_detail.open_actions",
                                    ),
                                ),
                            ),
                        )
                    }
                }

                actionRun == null -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "action_run_detail.missing",
                                kind = StateKind.Empty,
                                message = "缺少运行目标，无法展示详情。",
                            ),
                        ),
                    ),
                )

                else -> {
                    // —— StatusMessage 卡 ——
                    val statusMessage = when {
                        state.isRefreshing -> "正在刷新运行状态…"
                        !state.errorMessage.isNullOrBlank() -> state.errorMessage
                        !state.unavailableMessage.isNullOrBlank() -> state.unavailableMessage
                        state.refreshedAtMillis > 0L -> "缓存于 ${state.refreshedAtMillis.formatEpochMillis()}"
                        else -> null
                    }
                    if (!statusMessage.isNullOrBlank()) {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "action_run_detail.status_message",
                                        text = statusMessage,
                                        style = TextStyle.Meta,
                                        color = if (!state.errorMessage.isNullOrBlank()) TextColor.Danger else TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                        if (!state.errorMessage.isNullOrBlank()) {
                            add(
                                row(
                                    cell(
                                        ButtonComponent(
                                            id = "action_run_detail.status_retry",
                                            text = "重试",
                                            kind = ButtonKind.Primary,
                                            action = "action_run_detail.retry",
                                        ),
                                    ),
                                ),
                            )
                        }
                        if (!state.unavailableMessage.isNullOrBlank() && state.actionsHtmlUrl != null) {
                            add(
                                row(
                                    cell(
                                        ButtonComponent(
                                            id = "action_run_detail.status_open",
                                            text = "在 GitHub 打开",
                                            kind = ButtonKind.Secondary,
                                            action = "action_run_detail.open_actions",
                                        ),
                                    ),
                                ),
                            )
                        }
                    }
                    // —— Hero 卡 ——
                    val title = actionRun.name.ifBlank { "未知工作流" }
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "action_run_detail.hero",
                                    title = actionRun.runNumber?.let { "$title #$it" } ?: title,
                                ),
                            ),
                            cell(
                                TextComponent(
                                    id = "action_run_detail.hero.badge",
                                    text = actionRun.statusBadgeText(),
                                    style = TextStyle.Section,
                                    color = actionRun.statusTextColor(),
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "action_run_detail.hero.meta",
                                    text = actionRun.headerMeta(),
                                    style = TextStyle.Meta,
                                    color = TextColor.Secondary,
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "action_run_detail.hero.status",
                                    text = "状态：${actionRun.displayState.localizedStatus()}",
                                    style = TextStyle.Body,
                                    color = TextColor.Primary,
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "action_run_detail.hero.summary",
                                    text = actionRun.runSummary(),
                                    style = TextStyle.Body,
                                    color = TextColor.Secondary,
                                ),
                            ),
                        ),
                    )
                    // —— 双统计 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "action_run_detail.stat.duration",
                                    title = "总时长 ${actionRun.durationText()}",
                                    subtitle = actionRun.displayState.localizedStatus(),
                                ),
                                span = 6,
                            ),
                            cell(
                                SectionHeaderComponent(
                                    id = "action_run_detail.stat.assets",
                                    title = "产物 ${if (state.isLoadingArtifacts) "-" else state.artifacts.size}",
                                    subtitle = "可下载构建产物",
                                ),
                                span = 6,
                            ),
                        ),
                    )
                    actionRun.timeText()?.let { timeText ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "action_run_detail.hero.time",
                                        text = timeText,
                                        style = TextStyle.Caption,
                                        color = TextColor.Muted,
                                    ),
                                ),
                            ),
                        )
                    }
                    // —— Workflow 卡 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "action_run_detail.workflow",
                                    title = actionRun.path?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                                        ?: actionRun.workflowName?.takeIf { it.isNotBlank() }
                                        ?: "未知工作流",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "action_run_detail.workflow.event",
                                    text = "触发事件：${actionRun.event.localizedEvent()}",
                                    style = TextStyle.Body,
                                    color = TextColor.Secondary,
                                ),
                            ),
                        ),
                    )
                    actionRun.path?.takeIf { it.isNotBlank() }?.let { path ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "action_run_detail.workflow.path",
                                        text = path,
                                        style = TextStyle.Caption,
                                        color = TextColor.Muted,
                                    ),
                                ),
                            ),
                        )
                    }
                    // —— Logs 卡 ——
                    val preview = state.logPreview
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "action_run_detail.logs_header",
                                    title = "日志",
                                    subtitle = preview.logMeta(),
                                    actionText = "刷新",
                                    action = "action_run_detail.refresh_logs",
                                ),
                            ),
                        ),
                    )
                    val logsStateText = when {
                        state.isLoadingLogs -> "正在加载日志…"
                        !state.logsErrorMessage.isNullOrBlank() -> state.logsErrorMessage
                        preview == null -> "暂无日志预览"
                        else -> null
                    }
                    logsStateText?.let { text ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "action_run_detail.logs.state",
                                        text = text,
                                        style = TextStyle.Body,
                                        color = if (!state.logsErrorMessage.isNullOrBlank()) TextColor.Danger else TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                    }
                    preview?.let {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "action_run_detail.logs.preview",
                                        text = it.text,
                                        style = TextStyle.Code,
                                        color = TextColor.Primary,
                                        maxLines = 18,
                                        ellipsis = true,
                                    ),
                                ),
                            ),
                        )
                    }
                    add(
                        row(
                            cell(
                                ButtonComponent(
                                    id = "action_run_detail.logs.download",
                                    text = "下载日志",
                                    kind = ButtonKind.Secondary,
                                    enabled = actionRun.logsUrl?.isNotBlank() == true,
                                    action = "action_run_detail.download_logs",
                                ),
                            ),
                        ),
                    )
                    // —— Artifacts 卡 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "action_run_detail.artifacts_header",
                                    title = "产物（${state.artifacts.size}）",
                                ),
                            ),
                        ),
                    )
                    val artifactsStateText = when {
                        state.isLoadingArtifacts -> "正在加载产物…"
                        !state.artifactsErrorMessage.isNullOrBlank() -> state.artifactsErrorMessage
                        state.artifacts.isEmpty() -> "暂无产物"
                        else -> null
                    }
                    artifactsStateText?.let { text ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "action_run_detail.artifacts.state",
                                        text = text,
                                        style = TextStyle.Body,
                                        color = if (!state.artifactsErrorMessage.isNullOrBlank()) TextColor.Danger else TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                    }
                    state.artifacts.forEach { artifact ->
                        val enabled = !artifact.expired && !artifact.archiveDownloadUrl.isNullOrBlank()
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "action_run_detail.artifact.${artifact.id}.name",
                                        text = artifact.name,
                                        style = TextStyle.Body,
                                        color = TextColor.Primary,
                                        maxLines = 2,
                                        ellipsis = true,
                                    ),
                                    span = 8,
                                ),
                                cell(
                                    TextComponent(
                                        id = "action_run_detail.artifact.${artifact.id}.size",
                                        text = artifact.sizeInBytes.formatBytes(),
                                        style = TextStyle.Meta,
                                        color = TextColor.Muted,
                                    ),
                                    span = 4,
                                ),
                            ),
                        )
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "action_run_detail.artifact.${artifact.id}.download",
                                        text = if (artifact.expired) "已过期" else "下载",
                                        style = TextStyle.Meta,
                                        color = if (enabled) TextColor.Accent else TextColor.Muted,
                                        action = if (enabled) "action_run_detail.download_artifact.${artifact.id}" else "",
                                    ),
                                ),
                            ),
                        )
                    }
                    // —— 在 GitHub 打开 ——
                    actionRun.htmlUrl?.takeIf { it.isNotBlank() }?.let { _ ->
                        add(row(cell(SpacerComponent(id = "action_run_detail.spacer.open", heightDp = 6))))
                        add(
                            row(
                                cell(
                                    ButtonComponent(
                                        id = "action_run_detail.open_run",
                                        text = "在 GitHub 打开",
                                        kind = ButtonKind.Secondary,
                                        action = "action_run_detail.open_run",
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }
        }
        return PageSchema(id = "action_run_detail", columns = 12, scrollable = true, rows = rows)
    }

    /** 运行详情页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(): ShellState = ShellState(
        title = "运行详情",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "action_run_detail",
    )
}

/** 状态徽章文本：OK / ! / ... / ?。 */
private fun RepositoryActionRunDetail.statusBadgeText(): String = when (displayState.lowercase(Locale.US)) {
    "success" -> "OK"
    "failure", "cancelled", "timed_out" -> "!"
    "in_progress", "queued", "waiting", "requested" -> "..."
    else -> "?"
}

/** 状态徽章颜色：成功绿 / 失败红 / 进行中强调 / 其他次要。 */
private fun RepositoryActionRunDetail.statusTextColor(): TextColor = when (displayState.lowercase(Locale.US)) {
    "success" -> TextColor.Success
    "failure", "cancelled", "timed_out" -> TextColor.Danger
    "in_progress", "queued", "waiting", "requested" -> TextColor.Accent
    else -> TextColor.Secondary
}

/** 状态本地化纯函数（与 ActionsPage 同源语义，页面私有实现）。 */
private fun String.localizedStatus(): String = when (lowercase(Locale.US)) {
    "success" -> "成功"
    "failure" -> "失败"
    "cancelled" -> "已取消"
    "timed_out" -> "超时"
    "in_progress" -> "进行中"
    "queued" -> "排队中"
    "waiting" -> "等待中"
    "requested" -> "已请求"
    else -> ifBlank { "未知" }
}

/** 事件本地化纯函数。 */
private fun String?.localizedEvent(): String = when (orEmpty().lowercase(Locale.US)) {
    "push" -> "push"
    "pull_request" -> "pull_request"
    "schedule" -> "schedule"
    "workflow_dispatch" -> "workflow_dispatch"
    "" -> "-"
    else -> orEmpty()
}

private fun RepositoryActionRunDetail.headerMeta(): String = listOfNotNull(
    event?.takeIf { it.isNotBlank() },
    actorLogin?.withAtPrefix(),
    headSha?.shortSha(),
    headBranch?.takeIf { it.isNotBlank() }
).joinToString(" · ").ifBlank { "暂无运行信息" }

private fun RepositoryActionRunDetail.runSummary(): String = listOfNotNull(
    (triggeringActorLogin ?: actorLogin)?.withAtPrefix(),
    headSha?.shortSha(),
    headBranch?.takeIf { it.isNotBlank() }
).joinToString(" · ").ifBlank { "暂无运行信息" }

private fun RepositoryActionRunDetail.durationText(): String {
    val startedAt = runStartedAt?.parseGithubTimestamp() ?: return "-"
    val endedAt = updatedAt?.parseGithubTimestamp() ?: return "-"
    val totalSeconds = ((endedAt - startedAt) / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0L -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

private fun RepositoryActionRunDetail.timeText(): String? = listOfNotNull(
    runStartedAt?.let { "开始：${it.displayTimestamp()}" },
    updatedAt?.let { "更新：${it.displayTimestamp()}" }
).joinToString("\n").takeIf { it.isNotBlank() }

private fun RepositoryActionRunLogPreview?.logMeta(): String {
    val preview = this ?: return ""
    return if (preview.truncated) {
        "${preview.fileCount} 个文件（已截断）"
    } else {
        "${preview.fileCount} 个文件 · OK"
    }
}

private fun String.parseGithubTimestamp(): Long? = runCatching { GithubTimestampFormat.parse(this)?.time }.getOrNull()
private fun String.displayTimestamp(): String = replace("T", " ").removeSuffix("Z")
private fun Long.formatEpochMillis(): String = runCatching { DisplayTimestampFormat.format(java.util.Date(this)) }.getOrDefault("-")
private fun String.shortSha(): String? = takeIf { it.isNotBlank() }?.take(7)
private fun String.withAtPrefix(): String = if (startsWith("@")) this else "@$this"

private fun Long.formatBytes(): String {
    if (this <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var value = toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return if (unitIndex == 0) {
        "${value.toLong()} ${units[unitIndex]}"
    } else {
        String.format(Locale.US, "%.1f %s", value, units[unitIndex])
    }
}

private val GithubTimestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}
private val DisplayTimestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

/**
 * Actions 运行详情页入口：壳 + 多分支 schema。
 * 打开 Actions/运行页与下载日志/产物由调用端承载。
 */
@Composable
fun RepositoryActionRunDetailPageContent(
    state: RepositoryActionRunDetailUiState,
    onRetry: () -> Unit = {},
    onOpenActions: () -> Unit = {},
    onOpenRun: (String) -> Unit = {},
    onRefreshLogs: () -> Unit = {},
    onDownloadLogs: () -> Unit = {},
    onDownloadArtifact: (RepositoryActionArtifact) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "shell.back" -> onBack()
            action == "action_run_detail.retry" -> onRetry()
            action == "action_run_detail.open_actions" -> onOpenActions()
            action == "action_run_detail.open_run" -> state.actionRun?.htmlUrl?.takeIf { it.isNotBlank() }?.let(onOpenRun)
            action == "action_run_detail.refresh_logs" -> onRefreshLogs()
            action == "action_run_detail.download_logs" -> onDownloadLogs()
            action.startsWith("action_run_detail.download_artifact.") -> {
                val id = action.removePrefix("action_run_detail.download_artifact.").toLongOrNull()
                state.artifacts.firstOrNull { it.id == id }?.let(onDownloadArtifact)
            }
        }
    }
    AppShell(state = RepositoryActionRunDetailPage.shellState(), onAction = handleAction) {
        RepositoryActionRunDetailPage.schemaFor(
            state, onRetry, onOpenActions, onOpenRun, onRefreshLogs, onDownloadLogs, onDownloadArtifact
        ).renderPage(handleAction)
    }
}