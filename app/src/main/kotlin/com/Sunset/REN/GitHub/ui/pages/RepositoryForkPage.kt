package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryForkUiState
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.FieldComponent
import com.Sunset.REN.GitHub.ui.schema.SectionHeaderComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.StateComponent
import com.Sunset.REN.GitHub.ui.schema.StateKind
import com.Sunset.REN.GitHub.ui.schema.SwitchComponent
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState

/**
 * Fork 仓库页垂直切片（组 A：仓库写入/文件流）。
 *
 * 渲染结构对齐 RepositoryForkScreen（四态 + 表单）：
 * - 状态分支：Loading → Loading / SignedOut → Error（未登录）/ Error → Error / Content → 表单；
 * - Content 表单：状态卡（eligibilityError/existingFork/检查中/创建中/errorMessage/可创建
 *   六态纯函数）+ 源仓库卡（fullName + 默认分支）+ 目标 owner/name（可用性提示）/
 *   description（字数计数）+ 仅默认分支开关 + 账号通知（个人/组织）+ 创建/打开已有 Fork 按钮。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：repo_fork.submit / open_existing / toggle_default_branch / shell.back。
 * 草稿字段（owner/name/description/仅默认分支）由调用端持有；创建成功跳转由调用端承载。
 */
object RepositoryForkPage {

    /** 状态卡消息六态纯函数（原版 message 分支）。 */
    private fun statusMessage(state: RepositoryForkUiState.Content): String = when {
        state.eligibilityError != null -> state.eligibilityError
        state.existingFork != null -> "你已经 Fork 过该仓库：${state.existingFork.fullName}"
        state.isCheckingExistingFork -> "正在检查当前账号是否已有该仓库的 Fork……"
        state.isCreating -> "正在创建 Fork…"
        state.errorMessage != null -> state.errorMessage
        else -> "可以创建 Fork。"
    }

    /** 状态卡是否错误态（红色语义）。 */
    private fun statusIsError(state: RepositoryForkUiState.Content): Boolean =
        state.eligibilityError != null || state.errorMessage != null

    /** 名称可用性提示（原版 nameAvailabilityMessage）。 */
    private fun nameAvailabilityMessage(state: RepositoryForkUiState.Content, name: String): String? = when {
        name.isBlank() -> null
        state.isCheckingName -> "正在检查 $name 是否可用……"
        state.isNameAvailable == true -> "$name 可用"
        state.isNameAvailable == false -> "$name 不可用"
        state.nameCheckError != null -> state.nameCheckError
        else -> null
    }

    fun schemaFor(
        state: RepositoryForkUiState,
        targetOwner: String,
        targetName: String,
        description: String,
        defaultBranchOnly: Boolean,
        onOwnerChange: (String) -> Unit = {},
        onNameChange: (String) -> Unit = {},
        onDescriptionChange: (String) -> Unit = {},
    ): PageSchema {
        val rows = buildList<RowSchema> {
            // —— 页面标题 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "repo_fork.title",
                            text = "Fork 仓库",
                            style = TextStyle.Section,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "repo_fork.subtitle",
                            text = "创建自己的副本后即可独立修改代码。",
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
                            id = "repo_fork.required_hint",
                            text = "必填",
                            style = TextStyle.Caption,
                            color = TextColor.Muted,
                        ),
                    ),
                ),
            )
            when (state) {
                RepositoryForkUiState.Loading -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "repo_fork.loading",
                                kind = StateKind.Loading,
                                message = "正在加载…",
                            ),
                        ),
                    ),
                )

                RepositoryForkUiState.SignedOut -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "repo_fork.signed_out",
                                kind = StateKind.Error,
                                message = "尚未登录",
                                detail = "请先登录后再 Fork 仓库。",
                            ),
                        ),
                    ),
                )

                is RepositoryForkUiState.Error -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "repo_fork.error",
                                kind = StateKind.Error,
                                message = state.message,
                            ),
                        ),
                    ),
                )

                is RepositoryForkUiState.Content -> {
                    // —— 状态卡 ——
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "repo_fork.status",
                                    text = statusMessage(state),
                                    style = TextStyle.Body,
                                    color = if (statusIsError(state)) TextColor.Danger else TextColor.Secondary,
                                ),
                            ),
                        ),
                    )
                    add(row(cell(SpacerComponent(id = "repo_fork.spacer.source", heightDp = 8))))
                    // —— 源仓库卡 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "repo_fork.source_header",
                                    title = "源仓库",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "repo_fork.source_full_name",
                                    text = state.sourceRepository.fullName,
                                    style = TextStyle.Body,
                                    color = TextColor.Primary,
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "repo_fork.source_branch",
                                    text = "默认分支 ${state.sourceRepository.defaultBranch.ifBlank { "main" }}",
                                    style = TextStyle.Caption,
                                    color = TextColor.Secondary,
                                ),
                            ),
                        ),
                    )
                    add(row(cell(SpacerComponent(id = "repo_fork.spacer.fields", heightDp = 8))))
                    // —— 目标表单 ——
                    add(
                        row(
                            cell(
                                FieldComponent(
                                    id = "repo_fork.owner",
                                    value = targetOwner,
                                    hint = "所属账号（默认当前登录账号）",
                                    singleLine = true,
                                    enabled = !state.isCreating,
                                    onChange = onOwnerChange,
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                FieldComponent(
                                    id = "repo_fork.name",
                                    value = targetName,
                                    hint = "仓库名称",
                                    singleLine = true,
                                    enabled = !state.isCreating,
                                    supportingText = nameAvailabilityMessage(state, targetName),
                                    onChange = onNameChange,
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "repo_fork.name_helper",
                                    text = "可修改 Fork 后的仓库名称。",
                                    style = TextStyle.Caption,
                                    color = TextColor.Muted,
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                FieldComponent(
                                    id = "repo_fork.description",
                                    value = description,
                                    hint = "仓库描述，可选",
                                    singleLine = false,
                                    enabled = !state.isCreating,
                                    supportingText = "${description.length} / 350",
                                    onChange = onDescriptionChange,
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                SwitchComponent(
                                    id = "repo_fork.default_branch_only",
                                    title = "仅复制默认分支：${state.sourceRepository.defaultBranch.ifBlank { "main" }}",
                                    description = "将基于 ${state.sourceRepository.fullName} 的默认分支创建 Fork。",
                                    checked = defaultBranchOnly,
                                    action = "repo_fork.toggle_default_branch",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "repo_fork.account_notice",
                                    text = if (targetOwner.equals(state.currentAccountLogin, ignoreCase = true)) {
                                        "Fork 将创建到你的账号 ${targetOwner.ifBlank { state.currentAccountLogin }} 下。"
                                    } else {
                                        "Fork 将创建到组织 ${targetOwner.ifBlank { state.currentAccountLogin }} 下。"
                                    },
                                    style = TextStyle.Caption,
                                    color = TextColor.Secondary,
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ButtonComponent(
                                    id = "repo_fork.submit",
                                    text = if (state.isCreating) "正在创建 Fork…" else "创建 Fork",
                                    kind = ButtonKind.Primary,
                                    enabled = state.canCreateFork && !state.isCreating,
                                    action = "repo_fork.submit",
                                ),
                            ),
                        ),
                    )
                    if (state.existingFork != null) {
                        add(
                            row(
                                cell(
                                    ButtonComponent(
                                        id = "repo_fork.open_existing",
                                        text = "打开已有 Fork",
                                        kind = ButtonKind.Secondary,
                                        action = "repo_fork.open_existing",
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }
        }
        return PageSchema(id = "repo_fork", columns = 12, scrollable = true, rows = rows)
    }

    /** Fork 仓库页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(): ShellState = ShellState(
        title = "Fork 仓库",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "repo_fork",
    )
}

/**
 * Fork 仓库页入口：壳 + 四态 schema。
 * 草稿字段由调用端持有；种子初始化（首次拉取 currentAccountLogin/source 名称）、
 * 创建成功跳转由调用端承载。
 */
@Composable
fun RepositoryForkPageContent(
    state: RepositoryForkUiState,
    targetOwner: String = "",
    targetName: String = "",
    description: String = "",
    defaultBranchOnly: Boolean = false,
    onOwnerChange: (String) -> Unit = {},
    onNameChange: (String) -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    onToggleDefaultBranchOnly: () -> Unit = {},
    onSubmit: () -> Unit = {},
    onOpenExisting: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "repo_fork.submit" -> onSubmit()
            "repo_fork.open_existing" -> onOpenExisting()
            "repo_fork.toggle_default_branch" -> onToggleDefaultBranchOnly()
        }
    }
    AppShell(state = RepositoryForkPage.shellState(), onAction = handleAction) {
        RepositoryForkPage.schemaFor(
            state, targetOwner, targetName, description, defaultBranchOnly,
            onOwnerChange, onNameChange, onDescriptionChange,
        ).renderPage(handleAction)
    }
}