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
import com.Sunset.REN.GitHub.ui.schema.SectionHeaderComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.SwitchComponent
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState
import com.Sunset.REN.GitHub.ui.workspace.WorkspacePullUiState

/**
 * 工作区拉取页垂直切片（组 D：独立页）。
 *
 * 渲染结构对齐 WorkspacePullScreen：
 * - 标题 + 描述；
 * - 工作区卡（工作区名输入 + 创建工作区按钮）；
 * - 远端源卡（owner/repo/branch/remotePath/localTarget 五输入 + 覆盖本地 Switch +
 *   预览远端 Secondary + 执行拉取 Primary）；
 * - 日志区（等宽 Code 文本，来自 state.log）。
 * 字段变更走 FieldComponent.onChange 壳内回调；交互动作走路由。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：workspace_pull.create_workspace / preview / execute / shell.back。
 * 执行动作参数（WorkspacePullInput）由调用端从当前字段状态组装。
 */
object WorkspacePullPage {

    /**
     * 页面字段状态（调用端持有，经 onFieldChange 回写）。
     */
    data class Fields(
        val workspaceName: String = "",
        val owner: String = "",
        val repo: String = "",
        val branch: String = "",
        val remotePath: String = "",
        val localTarget: String = "",
        val overwriteLocal: Boolean = false,
    )

    fun schemaFor(
        state: WorkspacePullUiState,
        fields: Fields,
        onFieldChange: (String, String) -> Unit,
        onToggleOverwrite: (Boolean) -> Unit,
    ): PageSchema {
        val rows = buildList<RowSchema> {
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "workspace_pull.header",
                            title = "拉取工作区",
                            subtitle = "从 GitHub 仓库拉取文件，写入 App 私有工作区。",
                        ),
                    ),
                ),
            )
            // —— 工作区卡 ——
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "workspace_pull.workspace_header",
                            title = "工作区",
                            subtitle = state.selectedWorkspace?.let { "当前：${it.name}" } ?: "未选择工作区",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "workspace_pull.workspace_name",
                            value = fields.workspaceName,
                            hint = "工作区名称",
                            onChange = { onFieldChange("workspaceName", it) },
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "workspace_pull.create_workspace",
                            text = "创建工作区",
                            kind = ButtonKind.Secondary,
                            enabled = fields.workspaceName.isNotBlank(),
                            action = "workspace_pull.create_workspace",
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "workspace_pull.spacer.remote", heightDp = 8))))
            // —— 远端源卡 ——
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "workspace_pull.remote_header",
                            title = "远端源",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "workspace_pull.owner",
                            value = fields.owner,
                            hint = "仓库所有者",
                            onChange = { onFieldChange("owner", it) },
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "workspace_pull.repo",
                            value = fields.repo,
                            hint = "仓库名称",
                            onChange = { onFieldChange("repo", it) },
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "workspace_pull.branch",
                            value = fields.branch,
                            hint = "分支（默认 main）",
                            onChange = { onFieldChange("branch", it) },
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "workspace_pull.remote_path",
                            value = fields.remotePath,
                            hint = "远端路径（留空拉取整个仓库）",
                            onChange = { onFieldChange("remotePath", it) },
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "workspace_pull.local_target",
                            value = fields.localTarget,
                            hint = "本地目标目录（留空用仓库名）",
                            onChange = { onFieldChange("localTarget", it) },
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        SwitchComponent(
                            id = "workspace_pull.overwrite_local",
                            title = "覆盖本地已有文件",
                            description = "目标文件已存在时直接覆盖，不询问。",
                            checked = fields.overwriteLocal,
                            action = "workspace_pull.toggle_overwrite",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "workspace_pull.preview",
                            text = "预览远端内容",
                            kind = ButtonKind.Secondary,
                            enabled = fields.owner.isNotBlank() && fields.repo.isNotBlank(),
                            action = "workspace_pull.preview",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "workspace_pull.execute",
                            text = "执行拉取",
                            kind = ButtonKind.Primary,
                            enabled = fields.owner.isNotBlank() && fields.repo.isNotBlank(),
                            action = "workspace_pull.execute",
                        ),
                    ),
                ),
            )
            // —— 日志 ——
            if (state.log.isNotBlank()) {
                add(row(cell(SpacerComponent(id = "workspace_pull.spacer.log", heightDp = 8))))
                add(
                    row(
                        cell(
                            SectionHeaderComponent(
                                id = "workspace_pull.log_header",
                                title = "日志",
                            ),
                        ),
                    ),
                )
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "workspace_pull.log",
                                text = state.log,
                                style = TextStyle.Code,
                                color = TextColor.Primary,
                            ),
                        ),
                    ),
                )
            }
        }
        return PageSchema(id = "workspace_pull", columns = 12, scrollable = true, rows = rows)
    }

    /** 拉取页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(): ShellState = ShellState(
        title = "拉取工作区",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "workspace_pull",
    )
}

/**
 * 工作区拉取页入口：壳 + 表单 schema。
 * 字段状态由调用端经 [WorkspacePullPage.Fields] 持有；执行动作由调用端组装输入并触发。
 */
@Composable
fun WorkspacePullPageContent(
    state: WorkspacePullUiState,
    fields: WorkspacePullPage.Fields,
    onFieldChange: (String, String) -> Unit,
    onToggleOverwrite: (Boolean) -> Unit,
    onCreateWorkspace: (String) -> Unit,
    onPreviewPull: () -> Unit,
    onPullRemote: () -> Unit,
    onBack: () -> Unit,
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "workspace_pull.create_workspace" -> onCreateWorkspace(fields.workspaceName)
            "workspace_pull.toggle_overwrite" -> onToggleOverwrite(!fields.overwriteLocal)
            "workspace_pull.preview" -> onPreviewPull()
            "workspace_pull.execute" -> onPullRemote()
        }
    }
    AppShell(state = WorkspacePullPage.shellState(), onAction = handleAction) {
        WorkspacePullPage.schemaFor(state, fields, onFieldChange, onToggleOverwrite).renderPage(handleAction)
    }
}