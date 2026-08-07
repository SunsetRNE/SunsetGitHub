//! AXML（Android 二进制 XML）解码与 Manifest 事实提取。
//!
//! 基于 `rusty-axml`（Apache-2.0，3 万+ 下载）做二进制解析，本模块提供：
//! - [`axml_to_xml`]：AXML → 可读 XML 文本；
//! - [`manifest_facts`]：从 AndroidManifest.xml 字节提取结构化事实；
//! - [`manifest_from_apk`]：直接从 APK 文件读取并解析 Manifest。
//!
//! 相比 Kotlin 原版（仅探测 `AndroidManifest.xml` 是否存在），本模块
//! 提供完整解码与组件级事实提取。

use std::io::Cursor;
use std::path::Path;

use rusty_axml::parser::Axml;

use crate::archive::read_zip_file;
use crate::error::{Error, Result};

/// AXML → XML 文本（带声明、4 空格缩进）。
pub fn axml_to_xml(bytes: &[u8]) -> Result<String> {
    let axml = parse(bytes)?;
    axml.to_string()
        .map_err(|e| Error::InvalidData(format!("axml: {e}")))
}

fn parse(bytes: &[u8]) -> Result<Axml> {
    // AXML 文件以 XML chunk（type=0x0003, headerSize=0x0008）开头。
    // rusty-axml 上游对垃圾输入会 panic（chunk_types.rs unwrap），
    // 这里先做 magic 校验避免进入其 panic 路径。
    if bytes.len() < 8
        || bytes[0] != 0x03
        || bytes[1] != 0x00
        || bytes[2] != 0x08
        || bytes[3] != 0x00
    {
        return Err(Error::InvalidData(
            "axml: bad header (not a binary XML file)".into(),
        ));
    }
    rusty_axml::parse_from_reader(Cursor::new(bytes))
        .map_err(|e| Error::InvalidData(format!("axml: {e}")))
}

/// Manifest 中的组件信息。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ComponentInfo {
    /// android:name（完整类名）。
    pub name: String,
    pub exported: Option<String>,
    pub enabled: Option<String>,
    pub permission: Option<String>,
    pub process: Option<String>,
    pub has_intent_filter: bool,
}

/// AndroidManifest.xml 结构化事实。
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct ManifestFacts {
    pub package: Option<String>,
    pub version_name: Option<String>,
    pub version_code: Option<String>,
    pub min_sdk: Option<String>,
    pub target_sdk: Option<String>,
    pub application_label: Option<String>,
    pub application_icon: Option<String>,
    pub application_name: Option<String>,
    pub debuggable: Option<String>,
    pub permissions: Vec<String>,
    pub features: Vec<String>,
    pub activities: Vec<ComponentInfo>,
    pub services: Vec<ComponentInfo>,
    pub receivers: Vec<ComponentInfo>,
    pub providers: Vec<ComponentInfo>,
}

impl ManifestFacts {
    /// 组件总数。
    pub fn component_count(&self) -> usize {
        self.activities.len() + self.services.len() + self.receivers.len() + self.providers.len()
    }

    /// 是否为 debuggable 构建（"true" 且非 "false"）。
    pub fn is_debuggable(&self) -> bool {
        matches!(self.debuggable.as_deref(), Some(v) if v.eq_ignore_ascii_case("true"))
    }
}

fn comp_info(node: &rusty_axml::parser::XmlNode) -> Option<ComponentInfo> {
    let el = node.borrow();
    let name = el.get_name()?.to_string();
    let has_intent_filter = el
        .children()
        .iter()
        .any(|c| c.borrow().element_type() == "intent-filter");
    Some(ComponentInfo {
        name,
        exported: el.get_attr("android:exported").map(str::to_string),
        enabled: el.get_attr("android:enabled").map(str::to_string),
        permission: el.get_attr("android:permission").map(str::to_string),
        process: el.get_attr("android:process").map(str::to_string),
        has_intent_filter,
    })
}

/// 净化 rusty-axml 的属性值：
/// - 整数属性输出 `(type 0x10) 0x18dce4` → 十进制 `1629924`
/// - 十六进制类型 `(type 0x11)` → 保留 `0x...`
/// - 字符串等其它类型原样返回。
fn clean_attr(v: &str) -> Option<String> {
    if let Some(hex) = v.strip_prefix("(type 0x10) 0x") {
        return u32::from_str_radix(hex, 16).ok().map(|n| n.to_string());
    }
    if let Some(hex) = v.strip_prefix("(type 0x11) 0x") {
        return Some(format!("0x{hex}"));
    }
    Some(v.to_string())
}

/// 从 AndroidManifest.xml 字节提取事实。
pub fn manifest_facts(bytes: &[u8]) -> Result<ManifestFacts> {
    let axml = parse(bytes)?;
    let mut facts = ManifestFacts::default();

    for node in axml.iter() {
        let el = node.borrow();
        let ty = el.element_type();
        match ty {
            "manifest" => {
                facts.package = el.get_attr("package").map(str::to_string);
                facts.version_name = el.get_attr("android:versionName").map(str::to_string);
                facts.version_code = el.get_attr("android:versionCode").and_then(clean_attr);
            }
            "uses-sdk" => {
                facts.min_sdk = el.get_attr("android:minSdkVersion").and_then(clean_attr);
                facts.target_sdk = el.get_attr("android:targetSdkVersion").and_then(clean_attr);
            }
            "application" => {
                facts.application_label = el.get_attr("android:label").map(str::to_string);
                facts.application_icon = el.get_attr("android:icon").map(str::to_string);
                facts.application_name = el.get_attr("android:name").map(str::to_string);
                facts.debuggable = el.get_attr("android:debuggable").map(str::to_string);
            }
            "uses-permission" => {
                if let Some(name) = el.get_attr("android:name") {
                    facts.permissions.push(name.to_string());
                }
            }
            "uses-feature" => {
                if let Some(name) = el.get_attr("android:name") {
                    facts.features.push(name.to_string());
                }
            }
            "activity" => {
                if let Some(info) = comp_info(&node) {
                    facts.activities.push(info);
                }
            }
            "service" => {
                if let Some(info) = comp_info(&node) {
                    facts.services.push(info);
                }
            }
            "receiver" => {
                if let Some(info) = comp_info(&node) {
                    facts.receivers.push(info);
                }
            }
            "provider" => {
                if let Some(info) = comp_info(&node) {
                    facts.providers.push(info);
                }
            }
            _ => {}
        }
    }
    Ok(facts)
}

/// 从 APK 文件直接解析 Manifest。
pub fn manifest_from_apk(path: &Path) -> Result<ManifestFacts> {
    let bytes = read_zip_file(path, "AndroidManifest.xml")?;
    manifest_facts(&bytes)
}

/// 从 APK 文件直接解码 AXML 为 XML 文本。
pub fn axml_from_apk(path: &Path) -> Result<String> {
    let bytes = read_zip_file(path, "AndroidManifest.xml")?;
    axml_to_xml(&bytes)
}

// ---------------------------------------------------------------------------
// 测试
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn rejects_non_axml() {
        let err = axml_to_xml(b"this is not axml").unwrap_err();
        assert!(err.to_string().contains("axml"));
    }

    #[test]
    fn manifest_facts_rejects_garbage() {
        assert!(manifest_facts(&[0u8; 64]).is_err());
    }

    #[test]
    fn manifest_from_non_apk_fails() {
        let err = manifest_from_apk(Path::new("/nonexistent/file.apk")).unwrap_err();
        // ZipError 或 IoError 均可
        assert!(err.to_string().contains("zip") || err.to_string().contains("No such file"));
    }

    /// 集成测试：对真实 APK 解析 Manifest（设置 SUNSET_TEST_APK 环境变量运行）。
    #[test]
    #[ignore = "需要 SUNSET_TEST_APK 指向真实 APK"]
    fn parses_real_apk_manifest() {
        let path = std::env::var("SUNSET_TEST_APK").expect("SUNSET_TEST_APK not set");
        let facts = manifest_from_apk(Path::new(&path)).expect("parse manifest");
        eprintln!("package: {:?}", facts.package);
        eprintln!("versionName: {:?}", facts.version_name);
        eprintln!("versionCode: {:?}", facts.version_code);
        eprintln!(
            "minSdk: {:?} targetSdk: {:?}",
            facts.min_sdk, facts.target_sdk
        );
        eprintln!("permissions: {}", facts.permissions.len());
        eprintln!("activities: {}", facts.activities.len());
        eprintln!("services: {}", facts.services.len());
        eprintln!("receivers: {}", facts.receivers.len());
        eprintln!("providers: {}", facts.providers.len());
        assert!(facts.package.is_some(), "package should be present");
        assert!(!facts.activities.is_empty(), "activities should be present");
    }
}
