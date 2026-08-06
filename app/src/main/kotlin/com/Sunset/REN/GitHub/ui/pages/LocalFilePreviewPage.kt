package com.Sunset.REN.GitHub.ui.pages

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.FieldComponent
import com.Sunset.REN.GitHub.ui.schema.SwitchComponent
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.filemanager.LocalFilePreviewChromeActions
import com.Sunset.REN.GitHub.ui.filemanager.LocalFilePreviewChromeState
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState

/**
 * 本地文件预览页垂直切片（组 D：独立页）。
 *
 * 混合布局（对齐 LocalFilePreviewScreen，renderPage(fillMaxSize = false) 三段式）：
 * - 顶部 schema 段：文件名（空时用路径）+ 路径 + stateText；
 * - 中部原生预览视图（调用端 factory 创建，weight(1f) 弹性填充）——
 *   图片/归档/Markdown 等特殊预览由调用端 View 承载；
 * - 底部 schema 段：搜索面板（isSearchPanelVisible 时：查询/替换两输入 + Prev/Next/?/
 *   Replace/All 按钮 + Ignore case/Regex 两 Switch + searchStatus）+
 *   操作栏（markdownToggle/searchToggle/convert/saveAs/extract/edit/undo/redo/cancel/save，
 *   仅 visible 动作渲染为 Ghost 按钮）。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：preview.toggle_markdown / toggle_search / convert / save_as / extract / edit /
 *   undo / redo / cancel / save / search_prev / search_next / regex_help / replace_current /
 *   replace_all / toggle_ignore_case / toggle_regex / shell.back。
 * 字段变更（query/replacement）走 FieldComponent.onChange 壳内回调；视图释放由调用端承载。
 */
object LocalFilePreviewPage {

    /** 顶部信息 schema（文件名/路径/状态文本）。 */
    fun headerSchemaFor(state: LocalFilePreviewChromeState): PageSchema {
        val rows = buildList<RowSchema> {
            add(
                row(
                    cell(
                        TextComponent(
                            id = "preview.header.name",
                            text = state.name.ifBlank { state.path },
                            style = TextStyle.Body,
                            color = TextColor.Primary,
                            maxLines = 1,
                            ellipsis = true,
                        ),
                    ),
                ),
            )
            if (state.path.isNotBlank()) {
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "preview.header.path",
                                text = state.path,
                                style = TextStyle.Caption,
                                color = TextColor.Secondary,
                                maxLines = 1,
                                ellipsis = true,
                            ),
                        ),
                    ),
                )
            }
            if (state.stateText.isNotBlank()) {
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "preview.header.state",
                                text = state.stateText,
                                style = TextStyle.Caption,
                                color = TextColor.Secondary,
                                maxLines = 1,
                                ellipsis = true,
                            ),
                        ),
                    ),
                )
            }
        }
        return PageSchema(id = "preview_header", columns = 12, scrollable = false, rows = rows)
    }

    /** 底部 schema（搜索面板 + 操作栏）。 */
    fun bottomSchemaFor(
        state: LocalFilePreviewChromeState,
        onSearchQueryChange: (String) -> Unit,
        onReplacementChange: (String) -> Unit,
    ): PageSchema {
        val rows = buildList<RowSchema> {
            if (state.isSearchPanelVisible) {
                // —— 搜索面板 ——
                add(
                    row(
                        cell(
                            FieldComponent(
                                id = "preview.search.query",
                                value = state.query,
                                hint = "Search",
                                onChange = onSearchQueryChange,
                            ),
                        ),
                        cell(
                            ButtonComponent(
                                id = "preview.search.prev",
                                text = "Prev",
                                kind = ButtonKind.Ghost,
                                enabled = state.canSearchText,
                                action = "preview.search_prev",
                            ),
                        ),
                        cell(
                            ButtonComponent(
                                id = "preview.search.next",
                                text = "Next",
                                kind = ButtonKind.Ghost,
                                enabled = state.canSearchText,
                                action = "preview.search_next",
                            ),
                        ),
                        cell(
                            ButtonComponent(
                                id = "preview.search.regex_help",
                                text = "?",
                                kind = ButtonKind.Ghost,
                                action = "preview.regex_help",
                            ),
                        ),
                    ),
                )
                add(
                    row(
                        cell(
                            FieldComponent(
                                id = "preview.search.replacement",
                                value = state.replacement,
                                hint = "Replace",
                                onChange = onReplacementChange,
                            ),
                        ),
                        cell(
                            ButtonComponent(
                                id = "preview.search.replace_current",
                                text = "Replace",
                                kind = ButtonKind.Ghost,
                                enabled = state.canEditSearchText,
                                action = "preview.replace_current",
                            ),
                        ),
                        cell(
                            ButtonComponent(
                                id = "preview.search.replace_all",
                                text = "All",
                                kind = ButtonKind.Ghost,
                                enabled = state.canEditSearchText,
                                action = "preview.replace_all",
                            ),
                        ),
                    ),
                )
                add(
                    row(
                        cell(
                            SwitchComponent(
                                id = "preview.search.ignore_case",
                                title = "Ignore case",
                                checked = state.isIgnoreCaseEnabled,
                                action = "preview.toggle_ignore_case",
                            ),
                            span = 5,
                        ),
                        cell(
                            SwitchComponent(
                                id = "preview.search.regex",
                                title = "Regex",
                                checked = state.isRegexEnabled,
                                action = "preview.toggle_regex",
                            ),
                            span = 5,
                        ),
                        cell(
                            TextComponent(
                                id = "preview.search.status",
                                text = state.searchStatus,
                                style = TextStyle.Caption,
                                color = TextColor.Secondary,
                                maxLines = 1,
                                ellipsis = true,
                            ),
                            span = 2,
                        ),
                    ),
                )
            }
            // —— 操作栏（仅 visible 动作） ——
            val actions = listOf(
                Triple("preview.toggle_markdown", state.markdownToggle.text, state.markdownToggle) to "preview.toggle_markdown",
                Triple("preview.toggle_search", state.searchToggle.text, state.searchToggle) to "preview.toggle_search",
                Triple("preview.convert", state.convert.text, state.convert) to "preview.convert",
                Triple("preview.save_as", state.saveAs.text, state.saveAs) to "preview.save_as",
                Triple("preview.extract", state.extract.text, state.extract) to "preview.extract",
                Triple("preview.edit", state.edit.text, state.edit) to "preview.edit",
                Triple("preview.undo", state.undo.text, state.undo) to "preview.undo",
                Triple("preview.redo", state.redo.text, state.redo) to "preview.redo",
                Triple("preview.cancel", state.cancel.text, state.cancel) to "preview.cancel",
                Triple("preview.save", state.save.text, state.save) to "preview.save",
            )
            val visibleActions = actions.map { (entry, action) ->
                val (id, text, actionState) = entry
                Triple(id, text, actionState) to action
            }.filter { (entry, _) -> entry.third.visible }
            if (visibleActions.isNotEmpty()) {
                add(
                    row(
                        *visibleActions.map { (entry, action) ->
                            val (id, text, actionState) = entry
                            cell(
                                ButtonComponent(
                                    id = id,
                                    text = text.ifBlank { action },
                                    kind = ButtonKind.Ghost,
                                    enabled = actionState.enabled,
                                    action = action,
                                ),
                            )
                        }.toTypedArray(),
                    ),
                )
            }
        }
        return PageSchema(id = "preview_bottom", columns = 12, scrollable = false, rows = rows)
    }

    /** 本地文件预览页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(): ShellState = ShellState(
        title = "文件预览",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "local_file_preview",
    )
}

/**
 * 本地文件预览页入口：混合布局（schema 头 + 原生预览视图 + schema 底）。
 * 原生预览 View 由调用端 factory 创建，dispose 时经 onDisposePreviewView 释放。
 */
@Composable
fun LocalFilePreviewPageContent(
    chromeState: LocalFilePreviewChromeState,
    chromeActions: LocalFilePreviewChromeActions,
    previewViewFactory: () -> View,
    onDisposePreviewView: (View) -> Unit,
    onBack: () -> Unit,
) {
    val previewView = remember(previewViewFactory) { previewViewFactory() }
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "preview.toggle_markdown" -> chromeActions.onToggleMarkdown()
            "preview.toggle_search" -> chromeActions.onToggleSearch()
            "preview.convert" -> chromeActions.onConvert()
            "preview.save_as" -> chromeActions.onSaveAs()
            "preview.extract" -> chromeActions.onExtract()
            "preview.edit" -> chromeActions.onEdit()
            "preview.undo" -> chromeActions.onUndo()
            "preview.redo" -> chromeActions.onRedo()
            "preview.cancel" -> chromeActions.onCancel()
            "preview.save" -> chromeActions.onSave()
            "preview.search_prev" -> chromeActions.onFindPrevious()
            "preview.search_next" -> chromeActions.onFindNext()
            "preview.regex_help" -> chromeActions.onRegexHelp()
            "preview.replace_current" -> chromeActions.onReplaceCurrent()
            "preview.replace_all" -> chromeActions.onReplaceAll()
            "preview.toggle_ignore_case" -> chromeActions.onIgnoreCaseChange(!chromeState.isIgnoreCaseEnabled)
            "preview.toggle_regex" -> chromeActions.onRegexChange(!chromeState.isRegexEnabled)
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        LocalFilePreviewPage.headerSchemaFor(chromeState).renderPage(handleAction, fillMaxSize = false)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens.colors.surface),
        ) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView })
        }
        LocalFilePreviewPage.bottomSchemaFor(chromeState, chromeActions.onSearchQueryChange, chromeActions.onReplacementChange)
            .renderPage(handleAction, fillMaxSize = false)
    }
    DisposableEffect(previewView) {
        onDispose {
            onDisposePreviewView(previewView)
        }
    }
}