//! UniFFI 桥接层：为 Kotlin/Swift UI 暴露稳定的 FFI 面。
//!
//! 阶段 6（2026-08-07）：接入 UniFFI 0.32。
//! 首批导出纯函数面（markdown / 文件大小 / 分类 / 版本自检），
//! 由 `uniffi-bindgen generate --library` 生成 Kotlin 绑定。
//! sunset-core 保持纯净（不依赖 uniffi）。

use sunset_core::filemanager::entry::{categorize, format_size, FileCategory};
use sunset_core::filemanager::sort::{FileManagerEntrySorter, SortMode, SortOptions};
use sunset_core::markdown::{render_markdown, MarkdownRenderOptions};
use sunset_core::reverse::{format_apk_facts, ApkFacts};

uniffi::setup_scaffolding!();

/// 供 UI 层调用的简单 API 面（阶段 1 演示用）。
#[uniffi::export]
pub fn hello() -> String {
    "SunsetGitHub Rust core ready".to_string()
}

/// 文件大小格式化（FFI 面）。
#[uniffi::export]
pub fn file_size_label(bytes: u64) -> String {
    format_size(bytes)
}

/// 文件分类（FFI 面）。
#[uniffi::export]
pub fn file_category(name: &str) -> String {
    format!("{:?}", categorize(name))
}

/// Markdown 渲染（FFI 面）。
#[uniffi::export]
pub fn markdown_to_html(markdown: &str) -> String {
    render_markdown(markdown, &MarkdownRenderOptions::default())
}

/// APK 事实格式化（Rust API；ApkFacts 为 sunset-core 类型，留待后续迭代导出）。
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
