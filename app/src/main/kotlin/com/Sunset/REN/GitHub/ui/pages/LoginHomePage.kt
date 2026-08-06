package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
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
 * 登录首页（Login Home）垂直切片（步骤 5：认证链路主入口）。
 *
 * 渲染结构对齐 LoginHomeScreen：
 * - LOGIN 标签 + 大标题（登录 GitHub）；
 * - Hero 卡：登录 SunsetGitHub + 状态消息（正在进入首页…/选择一种方式…）；
 * - 两种登录方式卡：设备码登录（Cloud）/ 访问令牌登录（Settings），整卡点击；
 * - 本地凭据说明（凭据本地加密保存）；
 * - 推荐横幅：推荐设备码登录 + Safe OAuth 标记。
 * 壳：Hidden（登录页无底导航无返回）。
 * 路由前缀：login.device_flow / login.token_login。
 * 注：视觉装饰（渐变卡/旋转图标/悬浮 ?）由调用端承载或由渲染层统一风格。
 */
object LoginHomePage {

    /** 状态 → 页面 schema。 */
    fun schemaFor(stateMessage: String = "选择一种方式继续使用 SunsetGitHub。"): PageSchema {
        val rows = buildList<RowSchema> {
            add(
                row(
                    cell(
                        TextComponent(
                            id = "login.brand",
                            text = "LOGIN",
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
                            id = "login.title",
                            text = "登录 GitHub",
                            style = TextStyle.Section,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )

            add(row(cell(SpacerComponent(id = "login.spacer.hero", heightDp = 12))))

            // —— Hero 卡 ——
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "login.hero",
                            title = "登录 SunsetGitHub",
                            description = stateMessage,
                        ),
                    ),
                ),
            )

            add(row(cell(SpacerComponent(id = "login.spacer.methods", heightDp = 8))))

            // —— 登录方式：设备码 ——
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "login.device_flow",
                            title = "设备码登录",
                            description = "在浏览器中完成 GitHub 授权，适合移动设备。",
                            icon = IconId.Cloud,
                            action = "login.device_flow",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "login.token",
                            title = "访问令牌登录",
                            description = "使用 GitHub Personal access token 登录。",
                            icon = IconId.Settings,
                            action = "login.token_login",
                        ),
                    ),
                ),
            )

            // —— 本地凭据说明 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "login.credentials_note",
                            text = "凭据本地加密保存，不会上传到第三方。",
                            style = TextStyle.Caption,
                            color = TextColor.Muted,
                        ),
                    ),
                ),
            )

            // —— 推荐横幅 ——
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "login.recommend",
                            title = "推荐：设备码登录",
                            description = "降低输入成本，减少令牌粘贴错误，授权过程更清晰。",
                            trailing = "Safe OAuth",
                            action = "login.device_flow",
                        ),
                    ),
                ),
            )
        }
        return PageSchema(
            id = "login_home",
            columns = 12,
            scrollable = true,
            rows = rows,
        )
    }

    /** 登录首页壳状态：Hidden（无底导航、无返回）。 */
    fun shellState(): ShellState = ShellState(
        title = "登录",
        showBack = false,
        navBarMode = NavBarMode.Hidden,
        contentKey = "login_home",
    )
}

/**
 * 登录首页垂直切片入口：壳 + schema。
 * 自动进入首页（shouldEnterHome）与导航跳转由调用端承载。
 */
@Composable
fun LoginHomePageContent(
    stateMessage: String = "选择一种方式继续使用 SunsetGitHub。",
    onDeviceFlowClick: () -> Unit = {},
    onTokenLoginClick: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "login.device_flow" -> onDeviceFlowClick()
            "login.token_login" -> onTokenLoginClick()
        }
    }
    AppShell(state = LoginHomePage.shellState(), onAction = handleAction) {
        LoginHomePage.schemaFor(stateMessage).renderPage(handleAction)
    }
}