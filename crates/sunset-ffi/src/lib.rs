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

/// P2 OAuth client_id 混淆密钥（构建期 Gradle 侧需保持同步：app/build.gradle.kts 的 oauthKey）。
const OAUTH_KEY: &[u8] = b"SunsetGitHub::OAuth::2026::v1";

/// 解码构建期混淆的 OAuth client_id（P2 安全设计）。
///
/// 密文 = 明文 ^ salt ^ OAUTH_KEY，salt 为每次构建随机生成（BuildConfig 注入）。
/// 解码结果通过格式校验（GitHub client_id 为 10..=40 位字母数字）后才返回，
/// 否则返回空字符串——"验证式访问"的客户端闸门：不合规即拒绝发起登录。
///
/// 逆向门槛说明：Kotlin 层只有密文 + 盐；明文只在 Rust(.so) 内短暂出现；
/// 认真逆向仍需反汇编 .so，属"防顺手提取"而非对抗专业逆向。
#[uniffi::export]
pub fn resolve_oauth_client_id(obfuscated: &str, salt: &str) -> String {
    let obf = match hex_decode(obfuscated) {
        Some(bytes) => bytes,
        None => return String::new(),
    };
    let salt_bytes = match hex_decode(salt) {
        Some(bytes) if !bytes.is_empty() => bytes,
        _ => return String::new(),
    };
    let mut plain = Vec::with_capacity(obf.len());
    for (i, byte) in obf.iter().enumerate() {
        plain.push(byte ^ salt_bytes[i % salt_bytes.len()] ^ OAUTH_KEY[i % OAUTH_KEY.len()]);
    }
    match String::from_utf8(plain) {
        Ok(text) if is_plausible_client_id(&text) => text,
        _ => String::new(),
    }
}

/// GitHub client_id 形态：字母数字，10..=40 位。
fn is_plausible_client_id(value: &str) -> bool {
    (10..=40).contains(&value.len()) && value.bytes().all(|b| b.is_ascii_alphanumeric())
}

fn hex_decode(hex: &str) -> Option<Vec<u8>> {
    if !hex.len().is_multiple_of(2) {
        return None;
    }
    (0..hex.len())
        .step_by(2)
        .map(|i| u8::from_str_radix(&hex[i..i + 2], 16).ok())
        .collect()
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

    fn xor_encode(plain: &str, salt_hex: &str, key: &[u8]) -> String {
        let salt = hex_decode(salt_hex).unwrap();
        plain
            .bytes()
            .enumerate()
            .map(|(i, b)| format!("{:02x}", b ^ salt[i % salt.len()] ^ key[i % key.len()]))
            .collect()
    }

    #[test]
    fn oauth_client_id_roundtrip() {
        let salt = "0123456789abcdef";
        let obf = xor_encode("Ov23liE5BpZg8oMqUUri", salt, OAUTH_KEY);
        assert_ne!(obf, "Ov23liE5BpZg8oMqUUri");
        assert_eq!(resolve_oauth_client_id(&obf, salt), "Ov23liE5BpZg8oMqUUri");
    }

    #[test]
    fn oauth_client_id_rejects_bad_input() {
        assert_eq!(resolve_oauth_client_id("", "0123456789abcdef"), "");
        assert_eq!(resolve_oauth_client_id("zzzz", "0123456789abcdef"), ""); // 非法 hex
        assert_eq!(resolve_oauth_client_id("00", "0"), ""); // 盐为空/过短
                                                            // 解码出非字母数字内容（如含符号）→ 拒绝
        let weird = xor_encode("not!a@client#id", "0123456789abcdef", OAUTH_KEY);
        assert_eq!(resolve_oauth_client_id(&weird, "0123456789abcdef"), "");
    }
}
