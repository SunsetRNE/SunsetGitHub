//! APK / Dex / ARSC / AXML 逆向工具。
//!
//! - [`dex`]：完整 DEX 解析器（全量表项 + class_data）
//! - [`arsc`]：完整 ARSC 资源表解析器
//! - [`axml`]：AXML 解码（基于 rusty-axml）
//! - 本模块：APK 结构探测 + 统一入口

mod arsc;
mod axml;
mod dex;

pub use arsc::*;
pub use axml::*;
pub use dex::*;

use std::path::Path;

use crate::archive::{list_zip, read_zip_file, ArchiveEntry};
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
        format!(
            "AndroidManifest.xml: {}",
            if facts.has_manifest { "有" } else { "无" }
        ),
        format!("classes.dex: {}", if facts.has_dex { "有" } else { "无" }),
        format!(
            "resources.arsc: {}",
            if facts.has_resources_arsc {
                "有"
            } else {
                "无"
            }
        ),
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

// ---------------------------------------------------------------------------
// 统一入口（文件级）
// ---------------------------------------------------------------------------

/// 读取文件并解析为 DEX。
pub fn scan_dex_file(path: &Path) -> Result<DexFile> {
    let bytes = std::fs::read(path)?;
    DexFile::parse(&bytes)
}

/// 读取并解析 ARSC：APK/ZIP 会先取内部 `resources.arsc`，否则直接读文件。
pub fn scan_arsc_file(path: &Path) -> Result<ArscFile> {
    let lower = path
        .extension()
        .map(|e| e.to_string_lossy().to_ascii_lowercase())
        .unwrap_or_default();
    let bytes = if lower == "apk" || lower == "zip" {
        read_zip_file(path, "resources.arsc")?
    } else {
        std::fs::read(path)?
    };
    ArscFile::parse(&bytes)
}

/// 从 APK 提取并解析 AndroidManifest.xml。
pub fn scan_manifest_file(path: &Path) -> Result<ManifestFacts> {
    manifest_from_apk(path)
}

/// DEX 事实面板文本（对应原 UI 的工程扫描摘要）。
pub fn format_dex_facts(dex: &DexFile) -> String {
    let h = &dex.header;
    let mut lines = vec![
        format!("DEX 版本: {}", h.version),
        format!("文件大小: {} 字节", h.file_size),
        format!(
            "字符串: {}  类型: {}  原型: {}",
            h.string_ids_size, h.type_ids_size, h.proto_ids_size
        ),
        format!(
            "字段: {}  方法: {}  类: {}",
            h.field_ids_size, h.method_ids_size, h.class_defs_size
        ),
    ];
    let classes = dex.all_class_names();
    if !classes.is_empty() {
        lines.push(format!(
            "类示例: {}",
            classes
                .iter()
                .take(6)
                .cloned()
                .collect::<Vec<_>>()
                .join(", ")
        ));
    }
    let methods = dex.all_method_signatures();
    if !methods.is_empty() {
        lines.push(format!(
            "方法示例: {}",
            methods
                .iter()
                .take(6)
                .cloned()
                .collect::<Vec<_>>()
                .join(", ")
        ));
    }
    lines.join("\n")
}

/// ARSC 事实面板文本。
pub fn format_arsc_facts(arsc: &ArscFile) -> String {
    let mut lines = vec![format!("资源包数: {}", arsc.header.package_count)];
    for pkg in &arsc.packages {
        lines.push(format!("包: id=0x{:02x} name={}", pkg.id, pkg.name));
        let entry_count: usize = pkg.types.iter().map(|t| t.non_empty_count()).sum();
        lines.push(format!(
            "  类型 chunk: {}  条目: {}  配置变体: {}",
            pkg.type_specs.len(),
            entry_count,
            pkg.types.len()
        ));
        for ty in pkg.types.iter().take(6) {
            let name = pkg.type_name(ty.id).unwrap_or("?");
            lines.push(format!(
                "    {}[{}]: {} 条目, config={}",
                name,
                ty.id,
                ty.non_empty_count(),
                config_summary(&ty.config)
            ));
        }
    }
    if let Some(gs) = &arsc.global_string_pool {
        lines.push(format!("全局字符串池: {} 条", gs.string_count));
    }
    lines.join("\n")
}

/// Manifest 事实面板文本。
pub fn format_manifest_facts(f: &ManifestFacts) -> String {
    let mut lines = vec![
        format!("包名: {}", f.package.as_deref().unwrap_or("?")),
        format!(
            "版本: {} ({})",
            f.version_name.as_deref().unwrap_or("?"),
            f.version_code.as_deref().unwrap_or("?")
        ),
    ];
    if let Some(label) = &f.application_label {
        lines.push(format!("应用名: {label}"));
    }
    lines.push(format!(
        "SDK: min {} / target {}",
        f.min_sdk.as_deref().unwrap_or("?"),
        f.target_sdk.as_deref().unwrap_or("?")
    ));
    if f.is_debuggable() {
        lines.push("⚠ debuggable=true".to_string());
    }
    lines.push(format!("权限: {} 项", f.permissions.len()));
    lines.push(format!(
        "组件: activity {} / service {} / receiver {} / provider {}",
        f.activities.len(),
        f.services.len(),
        f.receivers.len(),
        f.providers.len()
    ));
    if let Some(first) = f.activities.first() {
        lines.push(format!("入口 Activity: {}", first.name));
    }
    lines.join("\n")
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

    #[test]
    fn formats_manifest_facts() {
        let facts = ManifestFacts {
            package: Some("com.example".into()),
            version_name: Some("1.0".into()),
            version_code: Some("1".into()),
            min_sdk: Some("21".into()),
            target_sdk: Some("35".into()),
            activities: vec![ComponentInfo {
                name: "com.example.MainActivity".into(),
                exported: None,
                enabled: None,
                permission: None,
                process: None,
                has_intent_filter: true,
            }],
            ..Default::default()
        };
        let text = format_manifest_facts(&facts);
        assert!(text.contains("com.example"));
        assert!(text.contains("MainActivity"));
        assert_eq!(facts.component_count(), 1);
    }

    #[test]
    fn debuggable_detection() {
        let debug = ManifestFacts {
            debuggable: Some("true".into()),
            ..Default::default()
        };
        assert!(debug.is_debuggable());
        let release = ManifestFacts {
            debuggable: Some("false".into()),
            ..Default::default()
        };
        assert!(!release.is_debuggable());
    }

    /// 集成测试：真实 APK 的 DEX + ARSC 全量解析（SUNSET_TEST_APK 环境变量）。
    #[test]
    #[ignore = "需要 SUNSET_TEST_APK 指向真实 APK"]
    fn parses_real_apk_dex_and_arsc() {
        let path = std::env::var("SUNSET_TEST_APK").expect("SUNSET_TEST_APK not set");
        // DEX：取第一个 classes*.dex
        let entries = list_zip(Path::new(&path)).unwrap();
        let dex_name = entries
            .iter()
            .find(|e| e.name.starts_with("classes") && e.name.ends_with(".dex"))
            .map(|e| e.name.clone())
            .expect("dex entry");
        let bytes = read_zip_file(Path::new(&path), &dex_name).unwrap();
        let dex = DexFile::parse(&bytes).expect("parse dex");
        eprintln!("=== DEX ({dex_name}) ===");
        eprintln!("{}", format_dex_facts(&dex));
        assert!(!dex.class_defs.is_empty(), "class_defs should be non-empty");

        // ARSC：直接解析 APK 内 resources.arsc
        let arsc = scan_arsc_file(Path::new(&path)).expect("parse arsc");
        eprintln!("=== ARSC ===");
        eprintln!("{}", format_arsc_facts(&arsc));
        assert!(!arsc.packages.is_empty(), "packages should be non-empty");
    }
}
