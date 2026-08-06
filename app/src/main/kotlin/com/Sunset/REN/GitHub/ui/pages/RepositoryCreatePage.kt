package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryCreateUiState
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.FieldComponent
import com.Sunset.REN.GitHub.ui.schema.FieldKeyboard
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.SwitchComponent
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState

/**
 * 新建仓库页垂直切片（组 A：仓库写入/文件流）。
 *
 * 渲染结构对齐 RepositoryCreateScreen（Hero 卡 + 表单卡）：
 * - Hero：Owner 徽章 + 标题/副标题 + 必填徽章 + 状态徽章（Idle/Submitting/SignedOut/
 *   ValidationError/Error/Success 六态纯函数）；
 * - 表单卡-基础信息：名称* / 描述 / 主页地址（Url 键盘）+ 主页说明；
 * - 表单卡-可见性：公开/私有双选项（选中 ✓ 徽章）；
 * - 表单卡-初始化：添加 README 开关 + 初始化文件提示 + .gitignore/许可证双模板按钮；
 * - 表单卡-仓库功能：Issues/Projects/Wiki 三开关 + 功能说明；
 * - 提交按钮（提交中三态）。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：repo_create.visibility.* / toggle_* / pick_gitignore / pick_license / submit / shell.back。
 * 模板选择 Dialog（CompactOptionPickerDialog）、表单脏标记/放弃确认、创建成功跳转由调用端承载。
 */
object RepositoryCreatePage {

    /** 表单纯数据（调用端持有草稿）。 */
    data class FormState(
        val name: String = "",
        val description: String = "",
        val homepage: String = "",
        val isPrivate: Boolean = false,
        val createReadme: Boolean = false,
        val hasIssues: Boolean = true,
        val hasProjects: Boolean = true,
        val hasWiki: Boolean = true,
        val gitignoreLabel: String = "无",
        val licenseLabel: String = "无",
    )

    /** 状态徽章六态纯函数（原版 repositoryCreateStatusText）。 */
    fun statusText(state: RepositoryCreateUiState): String = when (state) {
        RepositoryCreateUiState.Idle -> "填写信息后即可创建仓库。"
        RepositoryCreateUiState.Submitting -> "正在创建仓库……"
        RepositoryCreateUiState.SignedOut -> "尚未登录，请先完成 GitHub 登录。"
        is RepositoryCreateUiState.ValidationError -> state.message
        is RepositoryCreateUiState.Error -> "创建失败：${state.message}"
        is RepositoryCreateUiState.Success -> "仓库已创建：${state.repository.fullName}"
    }

    /** 状态徽章是否错误态（红色语义）。 */
    private fun statusIsError(state: RepositoryCreateUiState): Boolean = when (state) {
        is RepositoryCreateUiState.ValidationError, is RepositoryCreateUiState.Error -> true
        else -> false
    }

    fun schemaFor(
        state: RepositoryCreateUiState,
        form: FormState,
        initialFilesHint: String,
        onFieldChange: (String, String) -> Unit = { _, _ -> },
    ): PageSchema {
        val isSubmitting = state is RepositoryCreateUiState.Submitting
        val rows = buildList<RowSchema> {
            // —— Hero 卡 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "repo_create.hero_badge",
                            text = "● 当前登录账号 · Owner",
                            style = TextStyle.Caption,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "repo_create.hero_title",
                            text = "创建新的仓库",
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
                            id = "repo_create.hero_subtitle",
                            text = "仓库包含项目所有文件、修订历史和协作设置。当前版本会创建到当前登录账号下。",
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
                            id = "repo_create.hero_required",
                            text = "必填：仓库名称",
                            style = TextStyle.Caption,
                            color = TextColor.Accent,
                        ),
                        span = 6,
                    ),
                    cell(
                        TextComponent(
                            id = "repo_create.hero_status",
                            text = statusText(state),
                            style = TextStyle.Caption,
                            color = if (statusIsError(state)) TextColor.Danger else TextColor.Success,
                        ),
                        span = 6,
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "repo_create.spacer.form", heightDp = 8))))
            // —— 基础信息 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "repo_create.basic_section",
                            text = "基础信息",
                            style = TextStyle.Subtitle,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "repo_create.name",
                            value = form.name,
                            hint = "仓库名称 *（例如 sunset-demo）",
                            singleLine = true,
                            enabled = !isSubmitting,
                            onChange = { onFieldChange("name", it) },
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "repo_create.description",
                            value = form.description,
                            hint = "仓库描述（可选）",
                            singleLine = false,
                            enabled = !isSubmitting,
                            onChange = { onFieldChange("description", it) },
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "repo_create.homepage",
                            value = form.homepage,
                            hint = "主页地址，例如 https://example.com（可选）",
                            singleLine = true,
                            keyboard = FieldKeyboard.Url,
                            enabled = !isSubmitting,
                            onChange = { onFieldChange("homepage", it) },
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "repo_create.homepage_desc",
                            text = "可填写项目文档、演示站点或发布页链接。",
                            style = TextStyle.Caption,
                            color = TextColor.Muted,
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "repo_create.spacer.visibility", heightDp = 8))))
            // —— 可见性 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "repo_create.visibility_section",
                            text = "可见性",
                            style = TextStyle.Subtitle,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ItemComponent(
                            id = "repo_create.visibility_public",
                            title = "公开",
                            description = "任何人都可以看到这个仓库，你可以选择谁能提交。",
                            badge = if (!form.isPrivate) "✓" else null,
                            badgeColor = TextColor.Success,
                            action = "repo_create.visibility.public",
                        ),
                        span = 6,
                    ),
                    cell(
                        ItemComponent(
                            id = "repo_create.visibility_private",
                            title = "私有",
                            description = "你可以选择谁可以看到并提交到这个仓库。",
                            badge = if (form.isPrivate) "✓" else null,
                            badgeColor = TextColor.Success,
                            action = "repo_create.visibility.private",
                        ),
                        span = 6,
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "repo_create.spacer.init", heightDp = 8))))
            // —— 初始化 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "repo_create.init_section",
                            text = "初始化仓库",
                            style = TextStyle.Subtitle,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        SwitchComponent(
                            id = "repo_create.readme",
                            title = "添加 README 文件",
                            checked = form.createReadme,
                            action = "repo_create.toggle_readme",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "repo_create.init_hint",
                            text = initialFilesHint,
                            style = TextStyle.Meta,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "repo_create.gitignore",
                            text = ".gitignore 模板  ·  ${form.gitignoreLabel}  ⌄",
                            kind = ButtonKind.Secondary,
                            enabled = !isSubmitting,
                            action = "repo_create.pick_gitignore",
                        ),
                        span = 6,
                    ),
                    cell(
                        ButtonComponent(
                            id = "repo_create.license",
                            text = "许可证  ·  ${form.licenseLabel}  ⌄",
                            kind = ButtonKind.Secondary,
                            enabled = !isSubmitting,
                            action = "repo_create.pick_license",
                        ),
                        span = 6,
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "repo_create.spacer.features", heightDp = 8))))
            // —— 仓库功能 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "repo_create.features_section",
                            text = "仓库功能",
                            style = TextStyle.Subtitle,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        SwitchComponent(
                            id = "repo_create.feature_issues",
                            title = "启用 Issues",
                            checked = form.hasIssues,
                            action = "repo_create.toggle_issues",
                        ),
                        span = 4,
                    ),
                    cell(
                        SwitchComponent(
                            id = "repo_create.feature_projects",
                            title = "启用 Projects",
                            checked = form.hasProjects,
                            action = "repo_create.toggle_projects",
                        ),
                        span = 4,
                    ),
                    cell(
                        SwitchComponent(
                            id = "repo_create.feature_wiki",
                            title = "启用 Wiki",
                            checked = form.hasWiki,
                            action = "repo_create.toggle_wiki",
                        ),
                        span = 4,
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "repo_create.features_desc",
                            text = "这些设置创建后仍可在 GitHub 仓库设置中修改。",
                            style = TextStyle.Caption,
                            color = TextColor.Muted,
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "repo_create.spacer.submit", heightDp = 8))))
            // —— 提交 ——
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "repo_create.submit",
                            text = if (isSubmitting) "正在创建仓库……" else "创建仓库",
                            kind = ButtonKind.Primary,
                            enabled = !isSubmitting,
                            action = "repo_create.submit",
                        ),
                    ),
                ),
            )
        }
        return PageSchema(id = "repo_create", columns = 12, scrollable = true, rows = rows)
    }

    /** 新建仓库页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(): ShellState = ShellState(
        title = "创建新的仓库",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "repo_create",
    )
}

/**
 * 新建仓库页入口：壳 + 表单 schema。
 * 草稿字段由调用端持有（FormState 回写）；模板选择 Dialog、脏标记/放弃确认、
 * 创建成功跳转由调用端承载。
 */
@Composable
fun RepositoryCreatePageContent(
    state: RepositoryCreateUiState,
    form: RepositoryCreatePage.FormState,
    initialFilesHint: String,
    onFieldChange: (String, String) -> Unit = { _, _ -> },
    onPickGitignore: () -> Unit = {},
    onPickLicense: () -> Unit = {},
    onSubmit: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "repo_create.submit" -> onSubmit()
            "repo_create.pick_gitignore" -> onPickGitignore()
            "repo_create.pick_license" -> onPickLicense()
            "repo_create.visibility.public" -> onFieldChange("isPrivate", "false")
            "repo_create.visibility.private" -> onFieldChange("isPrivate", "true")
            "repo_create.toggle_readme" -> onFieldChange("createReadme", (!form.createReadme).toString())
            "repo_create.toggle_issues" -> onFieldChange("hasIssues", (!form.hasIssues).toString())
            "repo_create.toggle_projects" -> onFieldChange("hasProjects", (!form.hasProjects).toString())
            "repo_create.toggle_wiki" -> onFieldChange("hasWiki", (!form.hasWiki).toString())
        }
    }
    AppShell(state = RepositoryCreatePage.shellState(), onAction = handleAction) {
        RepositoryCreatePage.schemaFor(state, form, initialFilesHint, onFieldChange).renderPage(handleAction)
    }
}