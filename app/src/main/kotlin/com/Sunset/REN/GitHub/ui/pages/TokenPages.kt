package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.domain.auth.TokenPermissionStatus
import com.Sunset.REN.GitHub.ui.auth.TokenPermissionReviewUiState
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.FieldComponent
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
 * Token 登录流程页族（步骤 5：认证链路收尾）。
 *
 * 三个页面：
 * - TokenLoginChoicePage：选择页（我已有 Token / 我需要生成 Token 两张卡 + 安全说明）；
 * - TokenGuidePage：指引页（三步卡 + 建议权限 + 安全提醒 + 打开浏览器/我已获取 Token 两按钮）；
 * - TokenPermissionReviewPage：权限复核页（Token 输入 + 重新检查 + 状态行 + 账号卡 +
 *   检查结果列表 + 确认/取消/打开 Token 页面三按钮）。
 * 壳：Hidden + showBack（认证次级页）。
 * 路由前缀：token.choice.have / token.choice.need / token.guide.open / token.guide.acquired /
 * token.review.recheck / token.review.confirm / token.review.cancel / token.review.open_page / shell.back。
 * Dialog（风险确认/重新生成选项）由调用端承载。
 */

/** Token 登录方式选择页。 */
object TokenLoginChoicePage {

    fun schemaFor(): PageSchema {
        val rows = buildList<RowSchema> {
            add(row(cell(TextComponent(id = "choice.title", text = "访问令牌登录", style = TextStyle.Section, color = TextColor.Primary))))
            add(
                row(
                    cell(
                        TextComponent(
                            id = "choice.subtitle",
                            text = "你可以粘贴已有 Token，或先打开 GitHub 生成一个新的 Token。",
                            style = TextStyle.Body,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "choice.spacer.have", heightDp = 8))))
            // —— 我已有 Token ——
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "choice.have_card",
                            title = "我已有 Token",
                            description = "直接进入令牌检查，确认账号和权限后保存登录。",
                            icon = IconId.Check,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "choice.have_action",
                            text = "我已有 Token",
                            kind = ButtonKind.Primary,
                            action = "token.choice.have",
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "choice.spacer.need", heightDp = 8))))
            // —— 我需要生成 Token ——
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "choice.need_card",
                            title = "我需要生成 Token",
                            description = "查看需要的权限，并打开 GitHub Token 设置页面。",
                            icon = IconId.Settings,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "choice.need_action",
                            text = "查看生成指引",
                            kind = ButtonKind.Secondary,
                            action = "token.choice.need",
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "choice.spacer.note", heightDp = 8))))
            add(
                row(
                    cell(
                        TextComponent(
                            id = "choice.security_note",
                            text = "请只使用你信任的 Token。应用不会把 Token 写入源码。",
                            style = TextStyle.Caption,
                            color = TextColor.Muted,
                        ),
                    ),
                ),
            )
        }
        return PageSchema(id = "token_login_choice", columns = 12, scrollable = true, rows = rows)
    }

    fun shellState(): ShellState = ShellState(
        title = "访问令牌登录",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "token_login_choice",
    )
}

/** Token 登录方式选择页入口。 */
@Composable
fun TokenLoginChoicePageContent(
    onHaveTokenClick: () -> Unit = {},
    onNeedTokenGuideClick: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "token.choice.have" -> onHaveTokenClick()
            "token.choice.need" -> onNeedTokenGuideClick()
        }
    }
    AppShell(state = TokenLoginChoicePage.shellState(), onAction = handleAction) {
        TokenLoginChoicePage.schemaFor().renderPage(handleAction)
    }
}

/** Token 获取指引页。 */
object TokenGuidePage {

    fun schemaFor(): PageSchema {
        val rows = buildList<RowSchema> {
            add(row(cell(TextComponent(id = "guide.title", text = "获取访问令牌", style = TextStyle.Section, color = TextColor.Primary))))
            add(
                row(
                    cell(
                        TextComponent(
                            id = "guide.subtitle",
                            text = "按照步骤在 GitHub 中生成 Token，然后回到应用继续检查权限。",
                            style = TextStyle.Body,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "guide.spacer.steps", heightDp = 8))))
            // —— 三步卡 ——
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "guide.step1",
                            title = "1. 打开 GitHub Token 页面",
                            description = "使用浏览器登录 GitHub，并进入 Personal access tokens 设置。",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "guide.step2",
                            title = "2. 选择权限",
                            description = "仓库浏览、文件编辑、Issues 等能力需要对应权限。",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "guide.step3",
                            title = "3. 复制 Token",
                            description = "Token 只会显示一次。复制后回到应用点击“我已获取 Token”。",
                        ),
                    ),
                ),
            )
            // —— 建议权限 + 安全提醒 ——
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "guide.permissions",
                            title = "建议权限",
                            description = "如果需要浏览私有仓库、编辑文件或管理 Issues，请按实际用途授予对应权限。",
                            icon = IconId.Check,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "guide.security",
                            title = "安全提醒",
                            description = "不要把 Token 分享给他人；如果怀疑泄露，请立刻在 GitHub 中撤销。",
                            icon = IconId.Warning,
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "guide.spacer.actions", heightDp = 8))))
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "guide.open_browser",
                            text = "打开 GitHub Token 页面",
                            kind = ButtonKind.Primary,
                            action = "token.guide.open",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "guide.acquired",
                            text = "我已获取 Token",
                            kind = ButtonKind.Secondary,
                            action = "token.guide.acquired",
                        ),
                    ),
                ),
            )
        }
        return PageSchema(id = "token_guide", columns = 12, scrollable = true, rows = rows)
    }

    fun shellState(): ShellState = ShellState(
        title = "获取访问令牌",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "token_guide",
    )
}

/** Token 获取指引页入口。 */
@Composable
fun TokenGuidePageContent(
    onOpenBrowserClick: () -> Unit = {},
    onTokenAcquiredClick: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "token.guide.open" -> onOpenBrowserClick()
            "token.guide.acquired" -> onTokenAcquiredClick()
        }
    }
    AppShell(state = TokenGuidePage.shellState(), onAction = handleAction) {
        TokenGuidePage.schemaFor().renderPage(handleAction)
    }
}

/** Token 权限复核页（状态驱动：loading/saving/ready/waiting + 检查结果列表）。 */
object TokenPermissionReviewPage {

    /** 状态 → 状态行文案。 */
    private fun statusText(state: TokenPermissionReviewUiState): String = when {
        state.isLoading -> "正在检查 Token…"
        state.isSaving -> "正在保存 Token…"
        state.account != null -> "Token 可用"
        else -> "等待检查结果"
    }

    /** 状态 → 账号文案。 */
    private fun accountText(state: TokenPermissionReviewUiState): String {
        val account = state.account ?: return "无法识别当前账号"
        return account.name?.takeIf { it.isNotBlank() }
            ?.let { name -> "${account.login} · $name" }
            ?: account.login
    }

    /** 状态 → 权限范围文案。 */
    private fun scopesText(state: TokenPermissionReviewUiState): String = if (state.scopes.isEmpty()) {
        "未返回权限范围"
    } else {
        "权限范围：${state.scopes.joinToString()}"
    }

    /** 检查状态 → 标记字符。 */
    private fun mark(status: TokenPermissionStatus): String = when (status) {
        TokenPermissionStatus.Granted -> "✓"
        TokenPermissionStatus.Missing -> "!"
        TokenPermissionStatus.Unknown -> "?"
    }

    /** 检查状态 → 徽章颜色。 */
    private fun markColor(status: TokenPermissionStatus): TextColor = when (status) {
        TokenPermissionStatus.Granted -> TextColor.Success
        TokenPermissionStatus.Missing -> TextColor.Danger
        TokenPermissionStatus.Unknown -> TextColor.Muted
    }

    fun schemaFor(state: TokenPermissionReviewUiState): PageSchema {
        val busy = state.isLoading || state.isSaving
        val rows = buildList<RowSchema> {
            add(row(cell(TextComponent(id = "review.title", text = "Token 检查", style = TextStyle.Section, color = TextColor.Primary))))
            add(
                row(
                    cell(
                        TextComponent(
                            id = "review.subtitle",
                            text = "确认 Token 对应账号和关键权限，再保存登录。",
                            style = TextStyle.Body,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            // —— Token 输入框 ——
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "review.token_input",
                            value = state.token,
                            hint = "请输入认证令牌",
                            singleLine = false,
                            enabled = !busy,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "review.recheck",
                            text = "重新检查",
                            kind = ButtonKind.Primary,
                            enabled = state.token.isNotBlank() && !busy,
                            action = "token.review.recheck",
                        ),
                    ),
                ),
            )
            // —— 状态行 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "review.status",
                            text = statusText(state),
                            style = TextStyle.Caption,
                            color = TextColor.Muted,
                        ),
                    ),
                ),
            )
            // —— 账号卡 ——
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "review.account",
                            title = "Token 对应账号",
                            description = "${accountText(state)}\n${scopesText(state)}",
                            icon = IconId.Person,
                        ),
                    ),
                ),
            )
            // —— 检查结果列表 ——
            if (state.checks.isEmpty()) {
                add(
                    row(
                        cell(
                            ItemComponent(
                                id = "review.checks_empty",
                                title = "检查结果",
                                description = "暂无需要处理的检查项",
                                icon = IconId.Check,
                            ),
                        ),
                    ),
                )
            } else {
                state.checks.forEachIndexed { index, check ->
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "review.check.$index",
                                    title = check.title,
                                    description = check.description,
                                    meta = listOf(check.detail),
                                    badge = mark(check.status),
                                    badgeColor = markColor(check.status),
                                ),
                            ),
                        ),
                    )
                }
            }
            // —— 错误信息 ——
            if (!state.errorMessage.isNullOrBlank()) {
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "review.error",
                                text = state.errorMessage.orEmpty(),
                                style = TextStyle.Body,
                                color = TextColor.Danger,
                            ),
                        ),
                    ),
                )
            }
            add(row(cell(SpacerComponent(id = "review.spacer.actions", heightDp = 8))))
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "review.confirm",
                            text = "确认并进入首页",
                            kind = ButtonKind.Primary,
                            enabled = state.account != null && !busy,
                            action = "token.review.confirm",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "review.cancel",
                            text = "取消",
                            kind = ButtonKind.Secondary,
                            enabled = !state.isSaving,
                            action = "token.review.cancel",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "review.open_page",
                            text = "打开 Token 页面",
                            kind = ButtonKind.Secondary,
                            enabled = !state.isSaving,
                            action = "token.review.open_page",
                        ),
                    ),
                ),
            )
        }
        return PageSchema(id = "token_permission_review", columns = 12, scrollable = true, rows = rows)
    }

    fun shellState(): ShellState = ShellState(
        title = "Token 检查",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "token_permission_review",
    )
}

/** Token 权限复核页入口（signedInLogin 跳转/风险确认 Dialog 由调用端承载）。 */
@Composable
fun TokenPermissionReviewPageContent(
    state: TokenPermissionReviewUiState,
    onTokenInputChange: (String) -> Unit = {},
    onRecheckClick: () -> Unit = {},
    onConfirmClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onRegenerateClick: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "token.review.recheck" -> onRecheckClick()
            "token.review.confirm" -> onConfirmClick()
            "token.review.cancel" -> onCancelClick()
            "token.review.open_page" -> onRegenerateClick()
        }
    }
    AppShell(state = TokenPermissionReviewPage.shellState(), onAction = handleAction) {
        TokenPermissionReviewPage.schemaFor(state).renderPage(handleAction)
    }
}
