package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRunDetail
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
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState

/**
 * Actions 运行开发信息页垂直切片（组 C：Actions 运行子页）。
 *
 * 渲染结构对齐 RepositoryActionRunDeveloperInfoScreen：
 * - isLoading → 状态文本；errorMessage（无 run）→ 错误 + 重试；
 *   unavailableMessage（无 run）→ 不可用 + 在 GitHub 打开；
 * - Content：DeveloperInfoCard（标题“开发信息” + meta：运行 #n · 第 n 次尝试 · 分支 · sha +
 *   五组信息：运行（状态/原始状态/结论/事件/工作流/编号/尝试/工作流 id/检查套件）、
 *   人员（执行者/触发者）、时间线（创建/开始/更新/提交时间）、
 *   仓库与提交（仓库/源仓库/分支/提交/完整提交/提交信息/作者/关联 PR）、
 *   资源（工作流路径/GitHub 页面/可用 API 摘要），行格式 “label：value”）。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：action_run_dev_info.retry / open_actions / shell.back。
 */
object RepositoryActionRunDeveloperInfoPage {

    fun schemaFor(
        state: RepositoryActionRunDetailUiState,
        onRetry: () -> Unit = {},
        onOpenActions: () -> Unit = {},
    ): PageSchema {
        val actionRun = state.actionRun
        val rows = buildList<RowSchema> {
            if (state.isLoading) {
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "action_run_dev_info.loading",
                                text = "正在加载运行详情…",
                                style = TextStyle.Body,
                                color = TextColor.Secondary,
                            ),
                        ),
                    ),
                )
            }
            state.errorMessage?.takeIf { actionRun == null }?.let { message ->
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "action_run_dev_info.error",
                                text = message,
                                style = TextStyle.Body,
                                color = TextColor.Danger,
                            ),
                        ),
                    ),
                )
                add(
                    row(
                        cell(
                            ButtonComponent(
                                id = "action_run_dev_info.retry",
                                text = "重试",
                                kind = ButtonKind.Primary,
                                action = "action_run_dev_info.retry",
                            ),
                        ),
                    ),
                )
            }
            state.unavailableMessage?.takeIf { actionRun == null }?.let { message ->
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "action_run_dev_info.unavailable",
                                text = message,
                                style = TextStyle.Body,
                                color = TextColor.Secondary,
                            ),
                        ),
                    ),
                )
                if (state.actionsHtmlUrl != null) {
                    add(
                        row(
                            cell(
                                ButtonComponent(
                                    id = "action_run_dev_info.open_actions",
                                    text = "在 GitHub 打开",
                                    kind = ButtonKind.Secondary,
                                    action = "action_run_dev_info.open_actions",
                                ),
                            ),
                        ),
                    )
                }
            }
            actionRun?.let { run ->
                // —— DeveloperInfoCard ——
                add(
                    row(
                        cell(
                            SectionHeaderComponent(
                                id = "action_run_dev_info.card",
                                title = "开发信息",
                            ),
                        ),
                    ),
                )
                actionRunMeta(run)?.let { meta ->
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "action_run_dev_info.meta",
                                    text = meta,
                                    style = TextStyle.Meta,
                                    color = TextColor.Secondary,
                                ),
                            ),
                        ),
                    )
                }
                add(row(cell(SpacerComponent(id = "action_run_dev_info.spacer.groups", heightDp = 4))))
                developerInfoGroups(run).forEachIndexed { groupIndex, group ->
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "action_run_dev_info.group.$groupIndex",
                                    title = group.title,
                                ),
                            ),
                        ),
                    )
                    group.rows.forEachIndexed { rowIndex, row ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "action_run_dev_info.group.$groupIndex.row.$rowIndex",
                                        text = "${row.label}：${row.value}",
                                        style = TextStyle.Body,
                                        color = TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }
        }
        return PageSchema(id = "action_run_dev_info", columns = 12, scrollable = true, rows = rows)
    }

    /** 开发信息页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(): ShellState = ShellState(
        title = "开发信息",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "action_run_dev_info",
    )
}

private data class DeveloperInfoGroup(val title: String, val rows: List<DeveloperInfoRow>)
private data class DeveloperInfoRow(val label: String, val value: String)

private fun developerInfoGroups(run: RepositoryActionRunDetail): List<DeveloperInfoGroup> = listOf(
    DeveloperInfoGroup(
        "运行",
        listOfNotNull(
            developerInfoRow("状态", run.displayState),
            developerInfoRow("原始状态", run.status),
            developerInfoRow("结论", run.conclusion),
            developerInfoRow("事件", run.event),
            developerInfoRow("工作流", run.workflowName),
            developerInfoRow("运行编号", run.runNumber?.let { "#$it" }),
            developerInfoRow("尝试次数", run.runAttempt?.toString()),
            developerInfoRow("工作流 id", run.workflowId?.toString()),
            developerInfoRow("检查套件 id", run.checkSuiteId?.toString())
        )
    ),
    DeveloperInfoGroup(
        "人员",
        listOfNotNull(
            developerInfoRow("执行者", run.actorLogin?.withAtPrefix()),
            developerInfoRow("触发者", run.triggeringActorLogin?.withAtPrefix())
        )
    ),
    DeveloperInfoGroup(
        "时间线",
        listOfNotNull(
            developerInfoRow("创建时间", run.createdAt?.displayTimestamp()),
            developerInfoRow("开始时间", run.runStartedAt?.displayTimestamp()),
            developerInfoRow("更新时间", run.updatedAt?.displayTimestamp()),
            developerInfoRow("提交时间", run.headCommitTimestamp?.displayTimestamp())
        )
    ),
    DeveloperInfoGroup(
        "仓库与提交",
        listOfNotNull(
            developerInfoRow("仓库", listOfNotNull(run.repositoryOwner, run.repositoryName).joinToString("/").ifBlank { null }),
            developerInfoRow("源仓库", run.headRepositoryFullName),
            developerInfoRow("分支", run.headBranch),
            developerInfoRow("提交", run.headSha?.take(7)),
            developerInfoRow("完整提交", run.headSha),
            developerInfoRow("提交信息", run.headCommitMessage?.firstLine()),
            developerInfoRow("提交作者", listOfNotNull(run.headCommitAuthorName, run.headCommitAuthorEmail).joinToString(" · ").ifBlank { null }),
            developerInfoRow("关联 PR", run.pullRequestRefs.joinToString(", ").ifBlank { null })
        )
    ),
    DeveloperInfoGroup(
        "资源",
        listOfNotNull(
            developerInfoRow("工作流路径", run.path),
            developerInfoRow("GitHub 页面", run.htmlUrl?.removePrefix("https://")),
            developerInfoRow("可用 API", availableApiSummary(run))
        )
    )
).filter { it.rows.isNotEmpty() }

private fun developerInfoRow(label: String, value: String?): DeveloperInfoRow? {
    val normalized = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return DeveloperInfoRow(label, normalized)
}

private fun availableApiSummary(run: RepositoryActionRunDetail): String? = listOfNotNull(
    run.jobsUrl?.takeIf { it.isNotBlank() }?.let { "jobs" },
    run.logsUrl?.takeIf { it.isNotBlank() }?.let { "logs" },
    run.artifactsUrl?.takeIf { it.isNotBlank() }?.let { "artifacts" },
    run.rerunUrl?.takeIf { it.isNotBlank() }?.let { "rerun" },
    run.previousAttemptUrl?.takeIf { it.isNotBlank() }?.let { "previous_attempt" }
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

/**
 * Actions 运行开发信息页入口：壳 + 状态/信息卡 schema。
 * 打开 Actions 由调用端承载。
 */
@Composable
fun RepositoryActionRunDeveloperInfoPageContent(
    state: RepositoryActionRunDetailUiState,
    onRetry: () -> Unit = {},
    onOpenActions: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "action_run_dev_info.retry" -> onRetry()
            "action_run_dev_info.open_actions" -> onOpenActions()
        }
    }
    AppShell(state = RepositoryActionRunDeveloperInfoPage.shellState(), onAction = handleAction) {
        RepositoryActionRunDeveloperInfoPage.schemaFor(state, onRetry, onOpenActions).renderPage(handleAction)
    }
}