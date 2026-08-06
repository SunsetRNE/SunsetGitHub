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
import com.Sunset.REN.GitHub.ui.workspace.WorkspaceSyncUiState

/**
 * 工作区推送页垂直切片（组 D：独立页）。
 *
 * 渲染结构对齐 WorkspacePushScreen：
 * - 标题 + 描述；
 * - 工作区卡（工作区名输入 + 创建工作区按钮）；
 * - 导入卡（来源路径/目标目录两输入 + 导入按钮）；
 * - 同步目标卡（owner/repo/branch/remotePath/commitMessage 五输入 +
 *   mirrorMode/destructiveConfirmed/allowOverwriteRemoteChanges 三 Switch +
 *   预演 Secondary + 执行推送 Primary）；
 * - 日志区（等宽 Code 文本，来自 state.log）。
 * 字段变更走 FieldComponent.onChange 壳内回调；交互动作走路由。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：workspace_push.create_workspace / import / dry_run / execute / shell.back。
 * 执行动作参数（WorkspaceSyncInput）由调用端从当前字段状态组装。
 */
object WorkspacePushPage {

    /** 页面字段状态（调用端持有，经 onFieldChange 回写）。 */
    data class Fields(
        val workspaceName: String = "",
        val importPath: String = "",
        val importTarget: String = "",
        val owner: String = "",
        val repo: String = "",
        val branch: String = "",
        val remotePath: String = "",
        val commitMessage: String = "",
        val mirrorMode: Boolean = false,
        val destructiveConfirmed: Boolean = false,
        val allowOverwriteRemoteChanges: Boolean = false,
    )

    fun schemaFor(
        state: WorkspaceSyncUiState,
        fields: Fields,
        onFieldChange: (String, String) -> Unit,
        onToggle: (String, Boolean) -> Unit,
    ): PageSchema {
        val rows = buildList<RowSchema> {
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "workspace_push.header",
                            title = "推送工作区",
                            subtitle = "把 App 工作区内文件提交到 GitHub 仓库。",
                        ),
                    ),
                ),
            )
            // —— 工作区卡 ——
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "workspace_push.workspace_header",
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
                            id = "workspace_push.workspace_name",
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
                            id = "workspace_push.create_workspace",
                            text = "创建工作区",
                            kind = ButtonKind.Secondary,
                            enabled = fields.workspaceName.isNotBlank(),
                            action = "workspace_push.create_workspace",
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "workspace_push.spacer.import", heightDp = 8))))
            // —— 导入卡 ——
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "workspace_push.import_header",
                            title = "导入文件",
                            subtitle = "把本地文件导入工作区，再随推送一并提交。",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "workspace_push.import_path",
                            value = fields.importPath,
                            hint = "来源路径（本地文件/目录）",
                            onChange = { onFieldChange("importPath", it) },
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "workspace_push.import_target",
                            value = fields.importTarget,
                            hint = "工作区目标目录",
                            onChange = { onFieldChange("importTarget", it) },
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "workspace_push.import",
                            text = "导入路径",
                            kind = ButtonKind.Secondary,
                            enabled = fields.importPath.isNotBlank(),
                            action = "workspace_push.import",
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "workspace_push.spacer.target", heightDp = 8))))
            // —— 同步目标卡 ——
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "workspace_push.target_header",
                            title = "同步目标",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "workspace_push.owner",
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
                            id = "workspace_push.repo",
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
                            id = "workspace_push.branch",
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
                            id = "workspace_push.remote_path",
                            value = fields.remotePath,
                            hint = "远端路径（留空推送整个仓库）",
                            onChange = { onFieldChange("remotePath", it) },
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "workspace_push.commit_message",
                            value = fields.commitMessage,
                            hint = "提交信息（留空使用默认）",
                            singleLine = false,
                            onChange = { onFieldChange("commitMessage", it) },
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        SwitchComponent(
                            id = "workspace_push.mirror_mode",
                            title = "镜像模式",
                            description = "远端与工作区完全一致，删除工作区中不存在的远端文件。",
                            checked = fields.mirrorMode,
                            action = "workspace_push.toggle.mirror_mode",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        SwitchComponent(
                            id = "workspace_push.destructive_confirmed",
                            title = "确认删除操作",
                            description = "已理解镜像/覆盖可能删除远端文件。",
                            checked = fields.destructiveConfirmed,
                            action = "workspace_push.toggle.destructive_confirmed",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        SwitchComponent(
                            id = "workspace_push.allow_overwrite_remote",
                            title = "允许覆盖远端改动",
                            description = "远端存在本地未拉取的改动时仍强制推送。",
                            checked = fields.allowOverwriteRemoteChanges,
                            action = "workspace_push.toggle.allow_overwrite_remote",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "workspace_push.dry_run",
                            text = "预演（不实际提交）",
                            kind = ButtonKind.Secondary,
                            enabled = fields.owner.isNotBlank() && fields.repo.isNotBlank(),
                            action = "workspace_push.dry_run",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "workspace_push.execute",
                            text = "执行推送",
                            kind = ButtonKind.Primary,
                            enabled = fields.owner.isNotBlank() && fields.repo.isNotBlank(),
                            action = "workspace_push.execute",
                        ),
                    ),
                ),
            )
            // —— 日志 ——
            if (state.log.isNotBlank()) {
                add(row(cell(SpacerComponent(id = "workspace_push.spacer.log", heightDp = 8))))
                add(
                    row(
                        cell(
                            SectionHeaderComponent(
                                id = "workspace_push.log_header",
                                title = "日志",
                            ),
                        ),
                    ),
                )
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "workspace_push.log",
                                text = state.log,
                                style = TextStyle.Code,
                                color = TextColor.Primary,
                            ),
                        ),
                    ),
                )
            }
        }
        return PageSchema(id = "workspace_push", columns = 12, scrollable = true, rows = rows)
    }

    /** 推送页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(): ShellState = ShellState(
        title = "推送工作区",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "workspace_push",
    )
}

/**
 * 工作区推送页入口：壳 + 表单 schema。
 * 字段状态由调用端经 [WorkspacePushPage.Fields] 持有；执行动作由调用端组装输入并触发。
 */
@Composable
fun WorkspacePushPageContent(
    state: WorkspaceSyncUiState,
    fields: WorkspacePushPage.Fields,
    onFieldChange: (String, String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onCreateWorkspace: (String) -> Unit,
    onImportPath: () -> Unit,
    onDryRun: () -> Unit,
    onExecuteSync: () -> Unit,
    onBack: () -> Unit,
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "workspace_push.create_workspace" -> onCreateWorkspace(fields.workspaceName)
            "workspace_push.import" -> onImportPath()
            "workspace_push.dry_run" -> onDryRun()
            "workspace_push.execute" -> onExecuteSync()
            "workspace_push.toggle.mirror_mode" -> onToggle("mirrorMode", !fields.mirrorMode)
            "workspace_push.toggle.destructive_confirmed" -> onToggle("destructiveConfirmed", !fields.destructiveConfirmed)
            "workspace_push.toggle.allow_overwrite_remote" -> onToggle("allowOverwriteRemoteChanges", !fields.allowOverwriteRemoteChanges)
        }
    }
    AppShell(state = WorkspacePushPage.shellState(), onAction = handleAction) {
        WorkspacePushPage.schemaFor(state, fields, onFieldChange, onToggle).renderPage(handleAction)
    }
}