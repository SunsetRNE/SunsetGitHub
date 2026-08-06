//! 文件路径工具：规范化、父目录、拼接与安全校验。
//!
//! 移植自 Kotlin `FileManagerPaneNavigationState` 的路径辅助逻辑
//! （normalizePath / parentPathOrNull），并补充安全操作所需校验。

/// 规范化路径：统一分隔符、去除尾部斜杠、识别根路径与 root:// 协议路径。
pub fn normalize_path(path: &str) -> String {
    let trimmed = path.trim().replace('\\', "/");
    if trimmed.is_empty() {
        return "/".to_string();
    }
    if trimmed.starts_with("root://") {
        let root_path = trimmed
            .strip_prefix("root://")
            .unwrap_or(trimmed.as_str())
            .trim_end_matches('/');
        let absolute_root_path = if root_path.starts_with('/') {
            root_path.to_string()
        } else {
            format!("/{root_path}")
        };
        return if absolute_root_path == "/" {
            "root:///".to_string()
        } else {
            format!("root://{absolute_root_path}")
        };
    }
    if trimmed == "/" || trimmed.starts_with("content://") {
        return trimmed;
    }
    trimmed.trim_end_matches('/').to_string()
}

/// 返回父路径；根路径或非法路径返回 `None`。
pub fn parent_path(path: &str) -> Option<String> {
    let normalized = normalize_path(path);
    if normalized == "/" || normalized == "root:///" {
        return None;
    }
    if normalized.starts_with("root://") {
        let root_path = normalized.strip_prefix("root://").unwrap_or("");
        let index = root_path.rfind('/')?;
        return Some(if index == 0 {
            "root:///".to_string()
        } else {
            format!("root://{}", &root_path[..index])
        });
    }
    let index = normalized.rfind('/')?;
    Some(if index == 0 {
        "/".to_string()
    } else {
        normalized[..index].to_string()
    })
}

/// 路径拼接：保证根路径正确处理。
pub fn join_path(base: &str, name: &str) -> String {
    let base = normalize_path(base);
    let name = name.trim_start_matches('/');
    if base.ends_with('/') {
        format!("{base}{name}")
    } else {
        format!("{base}/{name}")
    }
}

/// 判断 `child` 是否位于 `parent` 目录之内（含等于）。
///
/// 先做词法级路径清理（解析 `.` / `..` 段），再比较前缀，
/// 用于阻止"复制到自身子目录"等危险操作。
pub fn is_within(child: &str, parent: &str) -> bool {
    let child = lexical_clean(normalize_path(child));
    let parent = lexical_clean(normalize_path(parent));
    if child == parent {
        return true;
    }
    let parent = parent.trim_end_matches('/');
    child.starts_with(&format!("{parent}/"))
}

/// 词法级清理路径：解析 `.` 与 `..` 段（不访问文件系统）。
fn lexical_clean(path: String) -> String {
    let mut segments: Vec<&str> = Vec::new();
    for segment in path.split('/') {
        match segment {
            "" | "." => continue,
            ".." => {
                segments.pop();
            }
            other => segments.push(other),
        }
    }
    if path.starts_with('/') {
        format!("/{}", segments.join("/"))
    } else {
        segments.join("/")
    }
}

/// 提取路径最后一段（文件名或目录名）。
pub fn file_name(path: &str) -> Option<String> {
    let normalized = normalize_path(path);
    if normalized == "/" || normalized == "root:///" {
        return None;
    }
    let trimmed = normalized.trim_end_matches('/');
    trimmed.rsplit('/').next().map(|s| s.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn normalizes_separators_and_trailing_slashes() {
        assert_eq!(normalize_path("/a/b/"), "/a/b");
        assert_eq!(normalize_path("\\a\\b\\"), "/a/b");
        assert_eq!(normalize_path("/"), "/");
        assert_eq!(normalize_path("  /a  "), "/a");
    }

    #[test]
    fn handles_root_protocol() {
        assert_eq!(normalize_path("root:///sdcard"), "root:///sdcard");
        assert_eq!(normalize_path("root://sdcard"), "root:///sdcard");
        assert_eq!(normalize_path("root:///"), "root:///");
        assert_eq!(parent_path("root:///sdcard/Download"), Some("root:///sdcard".into()));
        assert_eq!(parent_path("root:///"), None);
    }

    #[test]
    fn parent_of_absolute_paths() {
        assert_eq!(parent_path("/a/b/c"), Some("/a/b".into()));
        assert_eq!(parent_path("/a"), Some("/".into()));
        assert_eq!(parent_path("/"), None);
        assert_eq!(parent_path("content://doc/1"), Some("content://doc".into()));
    }

    #[test]
    fn join_and_within() {
        assert_eq!(join_path("/a", "b"), "/a/b");
        assert_eq!(join_path("/a/", "b"), "/a/b");
        assert!(is_within("/a/b/c", "/a"));
        assert!(is_within("/a", "/a"));
        assert!(!is_within("/ab", "/a"));
        assert!(!is_within("/a/../x", "/a"));
    }

    #[test]
    fn extracts_file_name() {
        assert_eq!(file_name("/a/b.txt"), Some("b.txt".into()));
        assert_eq!(file_name("/a/"), Some("a".into()));
        assert_eq!(file_name("/"), None);
    }
}
