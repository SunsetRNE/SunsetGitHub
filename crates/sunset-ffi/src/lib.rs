//! UniFFI 桥接层：为 Kotlin/Swift UI 暴露稳定的 FFI 面。
//!
//! 当前为骨架：直接转发 sunset-core 的纯函数，保证编译链路成立。
//! 后续接入 UniFFI 时，`#[uniffi::export]` 只需标记本层函数，
//! sunset-core 保持纯净（不依赖 uniffi）。

use sunset_core::filemanager::entry::{categorize, format_size, FileCategory};
use sunset_core::filemanager::sort::{FileManagerEntrySorter, SortMode, SortOptions};
use sunset_core::markdown::{render_markdown, MarkdownRenderOptions};
use sunset_core::reverse::{format_apk_facts, ApkFacts};

/// 供 UI 层调用的简单 API 面（阶段 1 演示用）。
pub fn hello() -> String {
    "SunsetGitHub Rust core ready".to_string()
}

/// 文件大小格式化（FFI 面）。
pub fn file_size_label(bytes: u64) -> String {
    format_size(bytes)
}

/// 文件分类（FFI 面）。
pub fn file_category(name: &str) -> String {
    format!("{:?}", categorize(name))
}

/// Markdown 渲染（FFI 面）。
pub fn markdown_to_html(markdown: &str) -> String {
    render_markdown(markdown, &MarkdownRenderOptions::default())
}

/// APK 事实格式化（FFI 面）。
pub fn apk_facts_summary(facts: &ApkFacts) -> String {
    format_apk_facts(facts)
}

// 占位导出，保证类型被使用（后续 UniFFI 将生成正式接口）。
#[allow(dead_code)]
fn _placeholder(_: SortMode, _: SortOptions, _: FileManagerEntrySorter, _: FileCategory) {}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn ffi_surface_works() {
        assert!(hello().contains("ready"));
        assert_eq!(file_size_label(2048), "2.0 KB");
        assert_eq!(file_category("a.md"), "Markdown");
        assert!(markdown_to_html("# hi").contains("<h1>"));
    }
}