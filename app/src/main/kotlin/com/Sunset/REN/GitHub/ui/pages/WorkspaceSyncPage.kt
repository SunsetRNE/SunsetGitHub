package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.IconId
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState

/**
 * 工作区同步入口页（Workspace Sync）垂直切片（任务 4：终端/工作区同步）。
 *
 * 渲染结构对齐 WorkspaceSyncScreen：
 * - 标题 + 描述；
 * - 拉取卡（远端同步本地：标题/描述/进入按钮）；
 * - 推送卡（本地同步远端：标题/描述/进入按钮）。
 * 壳：Hidden + showBack。
 * 路由前缀：workspace_sync.open_pull / workspace_sync.open_push / shell.back。
 * Pull/Push 子页导航由调用端承载。
 */

/** 工作区同步入口页。 */
object WorkspaceSyncPage {

    fun schemaFor(): PageSchema {
        val rows = buildList<RowSchema> {
            add(row(cell(TextComponent(id = "workspace_sync.title", text = "工作区同步", style = TextStyle.Section, color = TextColor.Primary))))
            add(
                row(
                    cell(
                        TextComponent(
                            id = "workspace_sync.description",
                            text = "请选择同步方向。远端同步本地用于从 GitHub 拉取到 App 工作区；本地同步远端用于把工作区提交到 GitHub。",
                            style = TextStyle.Body,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "workspace_sync.spacer.pull", heightDp = 8))))
            // —— 拉取卡 ——
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "workspace_sync.pull_card",
                            title = "远端同步本地",
                            description = "从 GitHub 仓库拉取文件，写入 App 私有工作区。适合先把远端内容带到本地再编辑。",
                            icon = IconId.Download,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "workspace_sync.open_pull",
                            text = "进入远端同步本地",
                            kind = ButtonKind.Primary,
                            action = "workspace_sync.open_pull",
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "workspace_sync.spacer.push", heightDp = 8))))
            // —— 推送卡 ——
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "workspace_sync.push_card",
                            title = "本地同步远端",
                            description = "把 App 工作区内文件提交到 GitHub 仓库。适合确认本地改动后上传、覆盖或镜像。",
                            icon = IconId.Cloud,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "workspace_sync.open_push",
                            text = "进入本地同步远端",
                            kind = ButtonKind.Primary,
                            action = "workspace_sync.open_push",
                        ),
                    ),
                ),
            )
        }
        return PageSchema(id = "workspace_sync", columns = 12, scrollable = true, rows = rows)
    }

    fun shellState(): ShellState = ShellState(
        title = "工作区同步",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "workspace_sync",
    )
}

/** 工作区同步入口：壳 + 方向选择 schema。Pull/Push 子页导航由调用端承载。 */
@Composable
fun WorkspaceSyncPageContent(
    onOpenPull: () -> Unit = {},
    onOpenPush: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "workspace_sync.open_pull" -> onOpenPull()
            "workspace_sync.open_push" -> onOpenPush()
        }
    }
    AppShell(state = WorkspaceSyncPage.shellState(), onAction = handleAction) {
        WorkspaceSyncPage.schemaFor().renderPage(handleAction)
    }
}