package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.domain.auth.GitHubAccount
import com.Sunset.REN.GitHub.domain.auth.RememberedAccount
import com.Sunset.REN.GitHub.domain.auth.RememberedAccountLoginType
import com.Sunset.REN.GitHub.ui.auth.AuthUiState
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.IconId
import com.Sunset.REN.GitHub.ui.schema.ItemAction
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.ListComponent
import com.Sunset.REN.GitHub.ui.schema.SectionHeaderComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState

/**
 * 账户页（Account）垂直切片（步骤 5：账户管理页）。
 *
 * 与原 AccountScreen 渲染结构对齐：
 * - 当前账户卡片（登录态/刷新态/未登录 + 退出登录）；
 * - 设备流 / Access Token 两组已保存账户（切换 + 移除行内动作）；
 * - 添加新账户入口。
 * 壳模式：Hidden 导航 + 返回（Settings 页"打开账号页面"的目标页）。
 * 路由前缀：account.add / account.sign_out / account.switch.{login} / account.remove.{login} / shell.back。
 * 注：原删除确认 AlertDialog 由调用端承载（组件库暂不引入 Dialog 组件）。
 */
object AccountPage {

    /** 状态 → 页面 schema。 */
    fun schemaFor(
        authState: AuthUiState,
        rememberedAccounts: List<RememberedAccount>,
        isCurrentAccount: (GitHubAccount) -> Boolean,
    ): PageSchema {
        val currentLogin = (authState as? AuthUiState.Authorized)?.login
        val isLoading = authState == AuthUiState.Loading
        val rows = buildList<com.Sunset.REN.GitHub.ui.layout.RowSchema> {
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "account.header",
                            title = "账户",
                            subtitle = "管理登录账号与访问凭据。",
                        ),
                    ),
                ),
            )

            // —— 当前账户 ——
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "account.current",
                            title = when {
                                isLoading -> "正在刷新…"
                                currentLogin != null -> currentLogin
                                else -> "未登录"
                            },
                            subtitle = if (currentLogin != null) "当前已登录" else "尚未登录任何账户",
                            icon = IconId.Person,
                            badge = if (currentLogin != null) "当前" else null,
                        ),
                    ),
                ),
            )
            if (currentLogin != null) {
                add(
                    row(
                        cell(
                            ButtonComponent(
                                id = "account.sign_out",
                                text = "退出登录",
                                kind = ButtonKind.Secondary,
                                action = "account.sign_out",
                            ),
                        ),
                    ),
                )
            }

            add(row(cell(SpacerComponent(id = "account.spacer.device", heightDp = 8))))

            // —— 设备流账户组 ——
            val deviceAccounts = rememberedAccounts.filter { it.loginType == RememberedAccountLoginType.DeviceFlow }
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "account.device_header",
                            title = "设备流账户",
                            subtitle = if (deviceAccounts.isEmpty()) "暂无已保存账户" else "已保存 ${deviceAccounts.size} 个账户",
                        ),
                    ),
                ),
            )
            if (deviceAccounts.isNotEmpty()) {
                add(
                    row(
                        cell(
                            ListComponent(
                                id = "account.device_list",
                                items = deviceAccounts.map { accountFor(it, isCurrentAccount(it.account)) },
                            ),
                        ),
                    ),
                )
            }

            add(row(cell(SpacerComponent(id = "account.spacer.token", heightDp = 8))))

            // —— Access Token 账户组 ——
            val tokenAccounts = rememberedAccounts.filter { it.loginType == RememberedAccountLoginType.AccessToken }
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "account.token_header",
                            title = "Access Token 账户",
                            subtitle = if (tokenAccounts.isEmpty()) "暂无已保存账户" else "已保存 ${tokenAccounts.size} 个账户",
                        ),
                    ),
                ),
            )
            if (tokenAccounts.isNotEmpty()) {
                add(
                    row(
                        cell(
                            ListComponent(
                                id = "account.token_list",
                                items = tokenAccounts.map { accountFor(it, isCurrentAccount(it.account)) },
                            ),
                        ),
                    ),
                )
            }

            add(row(cell(SpacerComponent(id = "account.spacer.add", heightDp = 8))))

            // —— 添加账户 ——
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "account.add",
                            text = "添加新账户",
                            kind = ButtonKind.Primary,
                            icon = IconId.Person,
                            action = "account.add",
                        ),
                    ),
                ),
            )
        }
        return PageSchema(
            id = "account",
            columns = 12,
            scrollable = true,
            rows = rows,
        )
    }

    /** 已保存账户 → 列表条目：登录信息 + 登录类型 + 当前标记 + 切换/移除。 */
    private fun accountFor(remembered: RememberedAccount, isCurrent: Boolean): ItemComponent {
        val account = remembered.account
        return ItemComponent(
            id = "account.row.${account.login}",
            title = account.login,
            subtitle = account.name?.takeIf { it.isNotBlank() } ?: "@${account.login}",
            meta = listOf(
                when (remembered.loginType) {
                    RememberedAccountLoginType.DeviceFlow -> "设备流登录"
                    RememberedAccountLoginType.AccessToken -> "Access Token"
                },
            ),
            icon = IconId.Person,
            badge = if (isCurrent) "当前" else null,
            actions = listOf(
                ItemAction(
                    id = "account.remove.${account.login}",
                    icon = IconId.Close,
                    contentDescription = "移除账户",
                    action = "account.remove.${account.login}",
                ),
            ),
            action = if (isCurrent) "" else "account.switch.${account.login}",
        )
    }

    /** 账户页壳状态：次级页面（返回 + 无底导航）。 */
    fun shellState(): ShellState = ShellState(
        title = "账户",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "account",
    )
}

/**
 * 账户页垂直切片入口：壳 + schema。
 * 同一路由服务壳（返回）与页面组件（添加/退出/切换/移除）。
 */
@Composable
fun AccountPageContent(
    authState: AuthUiState,
    rememberedAccounts: List<RememberedAccount>,
    isCurrentAccount: (GitHubAccount) -> Boolean = { false },
    onAddAccount: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onSwitchAccount: (GitHubAccount) -> Unit = {},
    onRemoveAccount: (GitHubAccount) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "shell.back" -> onBack()
            action == "account.add" -> onAddAccount()
            action == "account.sign_out" -> onSignOut()
            action.startsWith("account.switch.") -> {
                val login = action.removePrefix("account.switch.")
                rememberedAccounts.firstOrNull { it.account.login == login }?.let {
                    onSwitchAccount(it.account)
                }
            }
            action.startsWith("account.remove.") -> {
                val login = action.removePrefix("account.remove.")
                rememberedAccounts.firstOrNull { it.account.login == login }?.let {
                    onRemoveAccount(it.account)
                }
            }
        }
    }
    AppShell(state = AccountPage.shellState(), onAction = handleAction) {
        AccountPage.schemaFor(
            authState = authState,
            rememberedAccounts = rememberedAccounts,
            isCurrentAccount = isCurrentAccount,
        ).renderPage(handleAction)
    }
}