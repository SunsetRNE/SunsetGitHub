package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
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
 * 应用日志页垂直切片（组 D：独立页）。
 *
 * 渲染结构对齐 AppLogScreen：
 * - 标题 + 描述（用于排查 UI 渲染/导航栏边界/运行时异常）；
 * - 复制日志（Primary）+ 刷新（Secondary）按钮行；
 * - 日志正文（等宽 Code 文本，空时显示“暂无日志。”）。
 * 复制由调用端写入剪贴板；刷新由调用端重新读取日志并回写。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：app_log.copy / refresh / shell.back。
 */
object AppLogPage {

    fun schemaFor(
        logText: String,
        onCopyLog: () -> Unit = {},
        onRefresh: () -> String = { "" },
    ): PageSchema {
        val displayText = logText.ifBlank { "暂无日志。" }
        val rows = buildList<RowSchema> {
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "app_log.header",
                            title = "应用日志",
                            subtitle = "用于排查 UI 渲染、导航栏边界和运行时异常。复制后可直接发给开发者。",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "app_log.copy",
                            text = "复制日志",
                            kind = ButtonKind.Primary,
                            enabled = logText.isNotBlank(),
                            action = "app_log.copy",
                        ),
                        span = 6,
                    ),
                    cell(
                        ButtonComponent(
                            id = "app_log.refresh",
                            text = "刷新",
                            kind = ButtonKind.Secondary,
                            action = "app_log.refresh",
                        ),
                        span = 6,
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "app_log.spacer.body", heightDp = 4))))
            add(
                row(
                    cell(
                        TextComponent(
                            id = "app_log.body",
                            text = displayText,
                            style = TextStyle.Code,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )
        }
        return PageSchema(id = "app_log", columns = 12, scrollable = true, rows = rows)
    }

    /** 应用日志页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(): ShellState = ShellState(
        title = "应用日志",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "app_log",
    )
}

/**
 * 应用日志页入口：壳 + 日志 schema。
 * 复制写入剪贴板与刷新读取日志由调用端承载（onRefresh 返回最新日志文本）。
 */
@Composable
fun AppLogPageContent(
    logText: String,
    onCopyLog: () -> Unit = {},
    onRefresh: () -> String = { "" },
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "app_log.copy" -> onCopyLog()
            "app_log.refresh" -> Unit // 刷新结果由调用端以新 logText 回传重组
        }
    }
    AppShell(state = AppLogPage.shellState(), onAction = handleAction) {
        AppLogPage.schemaFor(logText, onCopyLog, onRefresh).renderPage(handleAction)
    }
}