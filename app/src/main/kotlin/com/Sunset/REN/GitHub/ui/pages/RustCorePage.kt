package com.Sunset.REN.GitHub.ui.pages

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
 * Rust 核心自检页（阶段 6：UniFFI 接入演示）。
 *
 * 展示 sunset-ffi 首批导出面：核心就绪信息、文件大小格式化、
 * 文件分类、Markdown 渲染。验证 UniFFI 桥接链路（Rust cdylib → JNI → Kotlin）。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：rust_core.refresh / shell.back。
 */
object RustCorePage {
    fun schemaFor(
        rustStatus: String,
        sizeLines: List<String>,
        categoryLines: List<String>,
        markdownHtml: String,
        onRefresh: () -> Unit = {},
    ): PageSchema {
        val rows = buildList<RowSchema> {
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "rust_core.header",
                            title = "Rust 核心",
                            subtitle = "UniFFI 桥接自检：crates/sunset-core → sunset-ffi → Kotlin。本页数据全部由 Rust 计算后回传。",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "rust_core.refresh",
                            text = "刷新自检",
                            kind = ButtonKind.Secondary,
                            enabled = true,
                            action = "rust_core.refresh",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "rust_core.status",
                            text = "核心状态：$rustStatus",
                            style = TextStyle.Body,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "rust_core.sizes.header",
                            title = "文件大小格式化",
                            subtitle = "hello() / file_size_label(bytes)",
                        ),
                    ),
                ),
            )
            for (line in sizeLines) {
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "rust_core.size.$line",
                                text = line,
                                style = TextStyle.Body,
                                color = TextColor.Secondary,
                            ),
                        ),
                    ),
                )
            }
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "rust_core.category.header",
                            title = "文件分类",
                            subtitle = "file_category(name)",
                        ),
                    ),
                ),
            )
            for (line in categoryLines) {
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "rust_core.category.$line",
                                text = line,
                                style = TextStyle.Body,
                                color = TextColor.Secondary,
                            ),
                        ),
                    ),
                )
            }
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "rust_core.markdown.header",
                            title = "Markdown 渲染",
                            subtitle = "markdown_to_html(markdown)",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "rust_core.markdown.body",
                            text = markdownHtml.ifBlank { "（空）" },
                            style = TextStyle.Code,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        SpacerComponent(id = "rust_core.bottom_spacer"),
                    ),
                ),
            )
        }
        return PageSchema(
            id = "rust_core",
            columns = 12,
            scrollable = true,
            shell = ShellState(navBar = NavBarMode.Hidden, showBack = true),
            rows = rows,
        )
    }
}
