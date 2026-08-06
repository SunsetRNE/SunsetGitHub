//! APK / Dex / ARSC 逆向工具（骨架）。
//!
//! 对应原 Kotlin `EngineeringToolScanner`。当前先实现 APK 结构
//! 探测（ZIP 条目 + 常见文件识别），Dex/ARSC 深度解析后续迭代。

use std::path::Path;

use crate::archive::{list_zip, ArchiveEntry};
use crate::error::Result;

/// APK 结构探测结果。
#[derive(Debug, Clone, Default)]
pub struct ApkFacts {
    pub has_manifest: bool,
    pub has_dex: bool,
    pub has_resources_arsc: bool,
    pub native_libs: Vec<String>,
    pub entry_count: usize,
}

/// 探测 APK 结构事实（不解析内容，只做条目级扫描）。
pub fn scan_apk(path: &Path) -> Result<ApkFacts> {
    let entries = list_zip(path)?;
    let mut facts = ApkFacts {
        entry_count: entries.len(),
        ..Default::default()
    };

    for entry in &entries {
        let name = entry.name.to_ascii_lowercase();
        if name == "androidmanifest.xml" {
            facts.has_manifest = true;
        } else if name.starts_with("classes") && name.ends_with(".dex") {
            facts.has_dex = true;
        } else if name == "resources.arsc" {
            facts.has_resources_arsc = true;
        } else if name.starts_with("lib/") && !entry.is_dir {
            // lib/<abi>/<name>.so
            if let Some(abi) = name.split('/').nth(1) {
                let so = name.rsplit('/').next().unwrap_or("");
                if !facts.native_libs.iter().any(|s| s == so) {
                    facts.native_libs.push(so.to_string());
                }
                let _ = abi;
            }
        }
    }

    Ok(facts)
}

/// 将扫描结果格式化为多行摘要（对应原 UI 的"事实面板"）。
pub fn format_apk_facts(facts: &ApkFacts) -> String {
    let mut lines = vec![
        format!("条目总数: {}", facts.entry_count),
        format!("AndroidManifest.xml: {}", if facts.has_manifest { "有" } else { "无" }),
        format!("classes.dex: {}", if facts.has_dex { "有" } else { "无" }),
        format!("resources.arsc: {}", if facts.has_resources_arsc { "有" } else { "无" }),
    ];
    if !facts.native_libs.is_empty() {
        lines.push(format!("native so: {}", facts.native_libs.join(", ")));
    }
    lines.join("\n")
}

/// 归档条目包装（便于后续 UI 直接消费）。
pub fn apk_entries(path: &Path) -> Result<Vec<ArchiveEntry>> {
    list_zip(path)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn facts_default_empty() {
        let facts = ApkFacts::default();
        assert!(!facts.has_manifest);
        assert_eq!(facts.entry_count, 0);
    }

    #[test]
    fn formats_facts_text() {
        let facts = ApkFacts {
            has_manifest: true,
            has_dex: true,
            has_resources_arsc: true,
            native_libs: vec!["libfoo.so".into()],
            entry_count: 42,
        };
        let text = format_apk_facts(&facts);
        assert!(text.contains("42"));
        assert!(text.contains("libfoo.so"));
    }
}