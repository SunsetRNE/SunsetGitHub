package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.ui.auth.device.DeviceFlowUiState
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
 * 设备码登录流程页族（步骤 5：认证链路）。
 *
 * 两个页面：
 * - DeviceFlowIntroPage：介绍页（标题 + 设备授权卡 + 3 步卡 + 开始按钮，静态）；
 * - DeviceFlowCodePage：验证码页（状态驱动：RequestingCode/CodeReady/Error/SignedIn/Cancelled
 *   五态 → 码文本/状态/详情/按钮可用性全映射）。
 * 壳：Hidden + showBack（认证次级页）。
 * 路由前缀：device_flow.next / copy_or_retry / open_browser / cancel / shell.back。
 */

/** 设备码登录介绍页。 */
object DeviceFlowIntroPage {

    fun schemaFor(): PageSchema {
        val rows = buildList<RowSchema> {
            add(row(cell(TextComponent(id = "intro.title", text = "设备码登录", style = TextStyle.Section, color = TextColor.Primary))))
            add(
                row(
                    cell(
                        TextComponent(
                            id = "intro.subtitle",
                            text = "应用会生成一次性验证码，你在浏览器中确认后即可登录。",
                            style = TextStyle.Body,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "intro.spacer.card", heightDp = 8))))
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "intro.browser_card",
                            title = "GitHub 设备授权",
                            icon = IconId.Cloud,
                        ),
                    ),
                ),
            )
            // —— 3 步卡 ——
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "intro.step1",
                            title = "1. 生成验证码",
                            description = "应用会生成一个 GitHub 一次性设备验证码。",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "intro.step2",
                            title = "2. 打开浏览器",
                            description = "打开 GitHub 授权页面，输入验证码并确认授权。",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "intro.step3",
                            title = "3. 完成授权",
                            description = "你在 GitHub 页面确认后，应用自动保存账号并进入首页。",
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "intro.spacer.button", heightDp = 8))))
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "intro.next",
                            text = "开始设备码登录",
                            kind = ButtonKind.Primary,
                            action = "device_flow.next",
                        ),
                    ),
                ),
            )
        }
        return PageSchema(id = "device_flow_intro", columns = 12, scrollable = true, rows = rows)
    }

    fun shellState(): ShellState = ShellState(
        title = "设备码登录",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "device_flow_intro",
    )
}

/** 设备码登录介绍页入口。 */
@Composable
fun DeviceFlowIntroPageContent(
    onNextClick: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "device_flow.next" -> onNextClick()
            "shell.back" -> onBack()
        }
    }
    AppShell(state = DeviceFlowIntroPage.shellState(), onAction = handleAction) {
        DeviceFlowIntroPage.schemaFor().renderPage(handleAction)
    }
}

/** 设备码验证码页（状态驱动五态映射）。 */
object DeviceFlowCodePage {

    /** 状态 → 码文本。 */
    private fun codeText(state: DeviceFlowUiState): String = when (state) {
        DeviceFlowUiState.RequestingCode -> "正在生成验证码…"
        is DeviceFlowUiState.CodeReady -> state.userCode
        is DeviceFlowUiState.Error -> "生成失败"
        is DeviceFlowUiState.SignedIn -> state.account.login
        DeviceFlowUiState.Cancelled -> "已取消"
    }

    /** 状态 → 状态行文案。 */
    private fun statusText(state: DeviceFlowUiState): String = when (state) {
        DeviceFlowUiState.RequestingCode -> "正在向 GitHub 申请验证码……"
        is DeviceFlowUiState.CodeReady -> "请打开浏览器，在 GitHub 页面输入验证码。"
        is DeviceFlowUiState.Error -> "无法生成验证码"
        is DeviceFlowUiState.SignedIn -> "登录成功，正在进入首页……"
        DeviceFlowUiState.Cancelled -> "已取消设备码登录。"
    }

    /** 状态 → 详情文案。 */
    private fun detailText(state: DeviceFlowUiState): String = when (state) {
        DeviceFlowUiState.RequestingCode -> "如果长时间停留，请检查网络连接后重试。"
        is DeviceFlowUiState.CodeReady -> state.message
        is DeviceFlowUiState.Error -> "GitHub 设备码请求失败：${state.message.ifBlank { "网络请求失败" }}\n请确认网络、VPN 或代理可访问 github.com，然后点击“重试生成”。"
        is DeviceFlowUiState.SignedIn -> "账号 ${state.account.login} 已保存。"
        DeviceFlowUiState.Cancelled -> "返回上一页。"
    }

    fun schemaFor(state: DeviceFlowUiState): PageSchema {
        val canCopyOrRetry = state is DeviceFlowUiState.CodeReady || state is DeviceFlowUiState.Error
        val canOpenBrowser = state is DeviceFlowUiState.CodeReady
        val copyOrRetryText = if (state is DeviceFlowUiState.Error) "重试生成" else "复制验证码"
        val rows = buildList<RowSchema> {
            add(row(cell(TextComponent(id = "code.title", text = "输入设备验证码", style = TextStyle.Section, color = TextColor.Primary))))
            add(row(cell(TextComponent(id = "code.status", text = statusText(state), style = TextStyle.Body, color = TextColor.Secondary))))
            add(row(cell(SpacerComponent(id = "code.spacer.card", heightDp = 8))))
            // —— 验证码卡 ——
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "code.card",
                            title = codeText(state),
                            subtitle = "GitHub 验证码",
                        ),
                    ),
                ),
            )
            // —— 复制/重试 + 打开浏览器 ——
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "code.copy_or_retry",
                            text = copyOrRetryText,
                            kind = ButtonKind.Secondary,
                            enabled = canCopyOrRetry,
                            action = "device_flow.copy_or_retry",
                        ),
                        span = 6,
                    ),
                    cell(
                        ButtonComponent(
                            id = "code.open_browser",
                            text = "打开浏览器",
                            kind = ButtonKind.Primary,
                            enabled = canOpenBrowser,
                            action = "device_flow.open_browser",
                        ),
                        span = 6,
                    ),
                ),
            )
            // —— 详情卡 ——
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "code.detail",
                            title = "等待授权",
                            description = detailText(state),
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "code.spacer.cancel", heightDp = 8))))
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "code.cancel",
                            text = "取消登录",
                            kind = ButtonKind.Secondary,
                            action = "device_flow.cancel",
                        ),
                    ),
                ),
            )
        }
        return PageSchema(id = "device_flow_code", columns = 12, scrollable = true, rows = rows)
    }

    fun shellState(): ShellState = ShellState(
        title = "输入设备验证码",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "device_flow_code",
    )
}

/** 设备码验证码页入口（SignedIn 跳转/Cancelled 返回由调用端承载）。 */
@Composable
fun DeviceFlowCodePageContent(
    state: DeviceFlowUiState,
    onCopyOrRetryClick: () -> Unit = {},
    onOpenBrowserClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "device_flow.copy_or_retry" -> onCopyOrRetryClick()
            "device_flow.open_browser" -> onOpenBrowserClick()
            "device_flow.cancel" -> onCancelClick()
        }
    }
    AppShell(state = DeviceFlowCodePage.shellState(), onAction = handleAction) {
        DeviceFlowCodePage.schemaFor(state).renderPage(handleAction)
    }
}