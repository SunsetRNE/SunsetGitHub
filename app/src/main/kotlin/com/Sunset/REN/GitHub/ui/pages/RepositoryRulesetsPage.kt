package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryRulesetsUiState
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

/**
 * 仓库 Rulesets 页垂直切片（组 B：仓库设置/管理）。
 *
 * 渲染结构对齐 RepositoryRulesetsScreen（只读视图）：
 * - 初始加载 → Loading；错误 → Error + 重试；
 * - Content：HeaderCard（标题+副标题）→ SummaryCard（owner/repo + 可管理/只读）→
 *   刷新按钮 → 空态 EmptyCard 或 RulesetCard 列表
 *   （名称/目标·执行·来源/规则数量·类型/条件摘要逐行/更新时间）。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：rulesets.refresh / retry / shell.back。
 */
object RepositoryRulesetsPage {

    fun schemaFor(
        state: RepositoryRulesetsUiState,
        onRefresh: () -> Unit = {},
    ): PageSchema {
        val rows = buildList<RowSchema> {
            when {
                state.isInitialLoad -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "rulesets.loading",
                                kind = StateKind.Loading,
                                message = "正在加载 Rulesets…",
                            ),
                        ),
                    ),
                )

                state.errorMessage != null && state.snapshot == null -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "rulesets.error",
                                kind = StateKind.Error,
                                message = "Rulesets 暂时不可用",
                                detail = state.errorMessage,
                                retryAction = "rulesets.retry",
                            ),
                        ),
                    ),
                )

                state.snapshot != null -> {
                    val snapshot = state.snapshot
                    // —— HeaderCard ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "rulesets.header",
                                    title = "Rulesets",
                                    subtitle = "仓库规则集用于统一约束分支、标签、推送和合并行为。当前页面先提供只读查看，编辑请继续使用 GitHub 网页端。",
                                ),
                            ),
                        ),
                    )
                    // —— 错误 inline ——
                    state.errorMessage?.let { message ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "rulesets.error.inline",
                                        text = message,
                                        style = TextStyle.Body,
                                        color = TextColor.Danger,
                                    ),
                                ),
                            ),
                        )
                    }
                    // —— SummaryCard ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "rulesets.summary",
                                    title = "${snapshot.owner}/${snapshot.repo}",
                                    subtitle = if (snapshot.canAdmin) "可管理" else "只读",
                                ),
                            ),
                        ),
                    )
                    // —— 刷新 ——
                    add(
                        row(
                            cell(
                                ButtonComponent(
                                    id = "rulesets.refresh",
                                    text = "刷新",
                                    kind = ButtonKind.Secondary,
                                    action = "rulesets.refresh",
                                ),
                            ),
                        ),
                    )
                    // —— 列表 / 空态 ——
                    if (snapshot.rulesets.isEmpty()) {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "rulesets.empty",
                                        text = "暂无仓库 Ruleset。",
                                        style = TextStyle.Body,
                                        color = TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "rulesets.empty.hint",
                                        text = "规则集创建和编辑请继续使用 GitHub 网页端。",
                                        style = TextStyle.Caption,
                                        color = TextColor.Muted,
                                    ),
                                ),
                            ),
                        )
                    } else {
                        snapshot.rulesets.forEach { item ->
                            add(
                                row(
                                    cell(
                                        SectionHeaderComponent(
                                            id = "rulesets.item.${item.id}",
                                            title = item.name.ifBlank { "#${item.id}" },
                                        ),
                                    ),
                                ),
                            )
                            add(
                                row(
                                    cell(
                                        TextComponent(
                                            id = "rulesets.item.${item.id}.meta",
                                            text = "目标：${item.target.ifBlank { "未知" }} · 执行：${item.enforcement.ifBlank { "未知" }} · 来源：${item.sourceType.ifBlank { "未知" }}",
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
                                            id = "rulesets.item.${item.id}.rules",
                                            text = "规则数量：${item.rulesCount} · 类型：${item.ruleTypes.joinToString().ifBlank { "未返回" }}",
                                            style = TextStyle.Meta,
                                            color = TextColor.Secondary,
                                        ),
                                    ),
                                ),
                            )
                            item.conditionsSummary.forEach { line ->
                                add(
                                    row(
                                        cell(
                                            TextComponent(
                                                id = "rulesets.item.${item.id}.cond.$line",
                                                text = line,
                                                style = TextStyle.Meta,
                                                color = TextColor.Secondary,
                                            ),
                                        ),
                                    ),
                                )
                            }
                            add(
                                row(
                                    cell(
                                        TextComponent(
                                            id = "rulesets.item.${item.id}.updated",
                                            text = "更新：${item.updatedAt.ifBlank { item.createdAt.ifBlank { "未知" } }}",
                                            style = TextStyle.Caption,
                                            color = TextColor.Muted,
                                        ),
                                    ),
                                ),
                            )
                            add(row(cell(SpacerComponent(id = "rulesets.item.${item.id}.spacer", heightDp = 10))))
                        }
                    }
                }
            }
        }
        return PageSchema(id = "rulesets", columns = 12, scrollable = true, rows = rows)
    }

    /** Rulesets 页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(): ShellState = ShellState(
        title = "Rulesets",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "rulesets",
    )
}

/**
 * 仓库 Rulesets 页入口：壳 + 三分支 schema。
 * 只读视图，无写操作 Dialog。
 */
@Composable
fun RepositoryRulesetsPageContent(
    state: RepositoryRulesetsUiState,
    onRefresh: () -> Unit = {},
    onRetry: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "rulesets.retry" -> onRetry()
            "rulesets.refresh" -> onRefresh()
        }
    }
    AppShell(state = RepositoryRulesetsPage.shellState(), onAction = handleAction) {
        RepositoryRulesetsPage.schemaFor(state, onRefresh).renderPage(handleAction)
    }
}
