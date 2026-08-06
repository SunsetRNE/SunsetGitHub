package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.FieldComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState
import com.Sunset.REN.GitHub.ui.terminal.TerminalUiState

/**
 * 工作区终端页（Terminal）垂直切片（任务 4：终端/工作区同步）。
 *
 * 渲染结构对齐 TerminalScreen：
 * - 标题 + 状态行（选中工作区 → "工作区：name · /dir"，否则"未连接工作区"）；
 * - 运行中：进度文本（percent% · text，isRunning 驱动按钮禁用）；
 * - 描述 + 两行快捷按钮（帮助/状态/预演 + 选择工作区/命令面板/导出管理 span4×4）；
 * - 输出区（Code 等宽；output 空 → 占位文本）；
 * - 历史/输出行（上一条/下一条/复制 span4×4 + 导出/分享 span6+6）；
 * - 命令输入（span9 + 执行 span3）。
 * 壳：Hidden + showBack。
 * 路由前缀：terminal.run / terminal.quick.* / terminal.select_workspace / terminal.command_panel /
 * terminal.manage_exports / terminal.history_previous / terminal.history_next / terminal.copy_output /
 * terminal.export_output / terminal.share_output / shell.back。
 * 工作区选择/命令面板/导出管理 Dialog 由调用端承载。
 */

/** 工作区终端页。 */
object TerminalPage {

    /** 状态行文案（工作区/未连接两态纯函数）。 */
    private fun statusText(state: TerminalUiState): String {
        val workspace = state.selectedWorkspace ?: return "未连接工作区"
        return "工作区：${workspace.name} · /${state.currentDirectory}"
    }

    /** 进度文本（percent · text 组合，空则 null）。 */
    private fun progressText(state: TerminalUiState): String? {
        if (!state.isCommandRunning) return null
        val parts = listOfNotNull(
            state.commandProgressPercent?.let { "$it%" },
            state.commandProgressText,
        )
        return parts.joinToString(" · ").ifBlank { null }
    }

    fun schemaFor(state: TerminalUiState, commandText: String): PageSchema {
        val isRunning = state.isCommandRunning
        val hasHistory = state.history.isNotEmpty()
        val hasOutput = state.output.isNotBlank()
        val progress = progressText(state)
        val rows = buildList<RowSchema> {
            add(row(cell(TextComponent(id = "terminal.title", text = "工作区终端", style = TextStyle.Section, color = TextColor.Primary))))
            add(
                row(
                    cell(
                        TextComponent(
                            id = "terminal.status",
                            text = statusText(state),
                            style = TextStyle.Caption,
                            color = TextColor.Muted,
                        ),
                    ),
                ),
            )
            // —— 进度区 ——
            if (progress != null) {
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "terminal.progress",
                                text = progress,
                                style = TextStyle.Caption,
                                color = TextColor.Muted,
                            ),
                        ),
                    ),
                )
            }
            add(
                row(
                    cell(
                        TextComponent(
                            id = "terminal.description",
                            text = "轻量工作区命令终端，不是系统 shell。支持文件查看/写入、工作区信息、远端绑定、同步预演和执行同步。输入 help（帮助）查看全部命令。",
                            style = TextStyle.Body,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "terminal.spacer.quick1", heightDp = 4))))
            // —— 快捷按钮行 1 ——
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "terminal.quick_help",
                            text = "帮助",
                            kind = ButtonKind.Secondary,
                            enabled = !isRunning,
                            action = "terminal.quick.help",
                        ),
                        span = 4,
                    ),
                    cell(
                        ButtonComponent(
                            id = "terminal.quick_status",
                            text = "状态",
                            kind = ButtonKind.Secondary,
                            enabled = !isRunning,
                            action = "terminal.quick.status",
                        ),
                        span = 4,
                    ),
                    cell(
                        ButtonComponent(
                            id = "terminal.quick_dry_run",
                            text = "预演",
                            kind = ButtonKind.Secondary,
                            enabled = !isRunning,
                            action = "terminal.quick.dry_run",
                        ),
                        span = 4,
                    ),
                ),
            )
            // —— 快捷按钮行 2 ——
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "terminal.select_workspace",
                            text = "选择工作区",
                            kind = ButtonKind.Secondary,
                            enabled = !isRunning && state.workspaces.isNotEmpty(),
                            action = "terminal.select_workspace",
                        ),
                        span = 4,
                    ),
                    cell(
                        ButtonComponent(
                            id = "terminal.command_panel",
                            text = "命令面板",
                            kind = ButtonKind.Secondary,
                            enabled = !isRunning,
                            action = "terminal.command_panel",
                        ),
                        span = 4,
                    ),
                    cell(
                        ButtonComponent(
                            id = "terminal.manage_exports",
                            text = "导出管理",
                            kind = ButtonKind.Secondary,
                            enabled = !isRunning,
                            action = "terminal.manage_exports",
                        ),
                        span = 4,
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "terminal.spacer.output", heightDp = 4))))
            // —— 输出区 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "terminal.output",
                            text = state.output.ifBlank { "终端输出会显示在这里。" },
                            style = TextStyle.Code,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "terminal.spacer.actions", heightDp = 4))))
            // —— 历史/输出行 1 ——
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "terminal.history_previous",
                            text = "上一条",
                            kind = ButtonKind.Secondary,
                            enabled = hasHistory && !isRunning,
                            action = "terminal.history_previous",
                        ),
                        span = 4,
                    ),
                    cell(
                        ButtonComponent(
                            id = "terminal.history_next",
                            text = "下一条",
                            kind = ButtonKind.Secondary,
                            enabled = hasHistory && !isRunning,
                            action = "terminal.history_next",
                        ),
                        span = 4,
                    ),
                    cell(
                        ButtonComponent(
                            id = "terminal.copy_output",
                            text = "复制",
                            kind = ButtonKind.Secondary,
                            enabled = hasOutput,
                            action = "terminal.copy_output",
                        ),
                        span = 4,
                    ),
                ),
            )
            // —— 历史/输出行 2 ——
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "terminal.export_output",
                            text = "导出",
                            kind = ButtonKind.Secondary,
                            enabled = hasOutput,
                            action = "terminal.export_output",
                        ),
                        span = 6,
                    ),
                    cell(
                        ButtonComponent(
                            id = "terminal.share_output",
                            text = "分享",
                            kind = ButtonKind.Secondary,
                            enabled = hasOutput,
                            action = "terminal.share_output",
                        ),
                        span = 6,
                    ),
                ),
            )
            // —— 命令输入行 ——
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "terminal.command_input",
                            value = commandText,
                            hint = "输入命令，例如 help（帮助）",
                            singleLine = true,
                            enabled = !isRunning,
                        ),
                        span = 9,
                    ),
                    cell(
                        ButtonComponent(
                            id = "terminal.run",
                            text = "执行",
                            kind = ButtonKind.Primary,
                            enabled = !isRunning,
                            action = "terminal.run",
                        ),
                        span = 3,
                    ),
                ),
            )
        }
        return PageSchema(id = "terminal", columns = 12, scrollable = true, rows = rows)
    }

    fun shellState(): ShellState = ShellState(
        title = "工作区终端",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "terminal",
    )
}

/** 工作区终端页入口：壳 + 终端 schema。Dialog（工作区选择/命令面板/导出管理）由调用端承载。 */
@Composable
fun TerminalPageContent(
    state: TerminalUiState,
    commandText: String,
    onCommandTextChange: (String) -> Unit = {},
    onRunCommand: () -> Unit = {},
    onQuickHelp: () -> Unit = {},
    onQuickStatus: () -> Unit = {},
    onQuickDryRun: () -> Unit = {},
    onSelectWorkspace: () -> Unit = {},
    onOpenCommandPanel: () -> Unit = {},
    onManageExports: () -> Unit = {},
    onHistoryPrevious: () -> Unit = {},
    onHistoryNext: () -> Unit = {},
    onCopyOutput: () -> Unit = {},
    onExportOutput: () -> Unit = {},
    onShareOutput: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "terminal.run" -> onRunCommand()
            "terminal.quick.help" -> onQuickHelp()
            "terminal.quick.status" -> onQuickStatus()
            "terminal.quick.dry_run" -> onQuickDryRun()
            "terminal.select_workspace" -> onSelectWorkspace()
            "terminal.command_panel" -> onOpenCommandPanel()
            "terminal.manage_exports" -> onManageExports()
            "terminal.history_previous" -> onHistoryPrevious()
            "terminal.history_next" -> onHistoryNext()
            "terminal.copy_output" -> onCopyOutput()
            "terminal.export_output" -> onExportOutput()
            "terminal.share_output" -> onShareOutput()
        }
    }
    AppShell(state = TerminalPage.shellState(), onAction = handleAction) {
        TerminalPage.schemaFor(state, commandText).renderPage(handleAction)
    }
}