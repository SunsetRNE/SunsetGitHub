//! Markdown 渲染（GFM 支持）。
//!
//! 基于 comrak（MIT/Apache-2.0），替代原 Kotlin Markwon 栈
//! （tables / tasklist / strikethrough / html 扩展全部覆盖）。

use comrak::{markdown_to_html, ComrakOptions};

/// 渲染选项。
#[derive(Debug, Clone, Copy)]
pub struct MarkdownRenderOptions {
    /// 启用表格扩展（GFM tables）。
    pub tables: bool,
    /// 启用任务列表。
    pub tasklists: bool,
    /// 启用删除线。
    pub strikethrough: bool,
    /// 渲染为 GitHub 风格 HTML。
    pub github_style: bool,
}

impl Default for MarkdownRenderOptions {
    fn default() -> Self {
        Self {
            tables: true,
            tasklists: true,
            strikethrough: true,
            github_style: true,
        }
    }
}

/// 将 Markdown 渲染为 HTML。
pub fn render_markdown(markdown: &str, options: &MarkdownRenderOptions) -> String {
    let mut comrak_options = ComrakOptions::default();
    comrak_options.extension.table = options.tables;
    comrak_options.extension.tasklist = options.tasklists;
    comrak_options.extension.strikethrough = options.strikethrough;
    comrak_options.render.unsafe_ = false;

    markdown_to_html(markdown, &comrak_options)
}

/// 提取纯文本（用于搜索/摘要，去掉 Markdown 标记的粗略实现）。
pub fn plain_text(markdown: &str) -> String {
    // 去除代码围栏
    let no_fences = markdown
        .lines()
        .filter(|line| !line.trim_start().starts_with("```"))
        .collect::<Vec<_>>()
        .join("\n");
    // 去除标题/列表/引用标记
    let cleaned = no_fences
        .lines()
        .map(|line| {
            line.trim_start_matches(['#', '>', '-', '*', '+', ' '])
                .trim()
        })
        .collect::<Vec<_>>()
        .join(" ");
    cleaned.trim().to_string()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn renders_table() {
        let html = render_markdown(
            "| a | b |\n|---|---|\n| 1 | 2 |",
            &MarkdownRenderOptions::default(),
        );
        assert!(html.contains("<table>"), "table should be rendered: {html}");
    }

    #[test]
    fn renders_tasklist() {
        let html = render_markdown("- [x] done\n- [ ] todo", &MarkdownRenderOptions::default());
        assert!(
            html.contains("type=\"checkbox\""),
            "tasklist checkbox missing: {html}"
        );
    }

    #[test]
    fn renders_strikethrough() {
        let html = render_markdown("~~gone~~", &MarkdownRenderOptions::default());
        assert!(html.contains("<del>"), "strikethrough missing: {html}");
    }

    #[test]
    fn plain_text_strips_markers() {
        let text = plain_text("# Title\n- item\n> quote\n```\ncode\n```");
        assert!(!text.contains('#'));
        assert!(!text.contains("```"));
    }
}
