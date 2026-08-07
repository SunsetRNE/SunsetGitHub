//! 递归搜索：目录树遍历 + 匹配选项。
//!
//! 移植自 Kotlin `FileManagerSearchOptions` 的匹配语义
//! （query / includeSubdirectories / includeFiles / includeDirectories /
//! caseSensitive / includeHiddenFiles），并实现递归目录遍历。

use std::fs;
use std::path::{Path, PathBuf};

use crate::filemanager::entry::{load_entry, FileEntry, FileEntryKind};

/// 搜索选项（与 Kotlin FileManagerSearchOptions 字段对齐）。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SearchOptions {
    pub query: String,
    /// 是否递归子目录。
    pub include_subdirectories: bool,
    /// 是否匹配文件。
    pub include_files: bool,
    /// 是否匹配目录。
    pub include_directories: bool,
    pub case_sensitive: bool,
    pub include_hidden_files: bool,
}

impl SearchOptions {
    /// 规范化查询（去除首尾空白）。
    pub fn normalized_query(&self) -> &str {
        self.query.trim()
    }

    /// 是否有有效的目标类型（文件或目录至少一个）。
    pub fn has_valid_target_type(&self) -> bool {
        self.include_files || self.include_directories
    }

    /// 匹配单一条目（与 Kotlin `matches` 行为一致）。
    ///
    /// `display_path` 为相对搜索根目录的展示路径（如 `sub/notes.md`），
    /// 避免临时目录等物理前缀干扰匹配。
    pub fn matches(&self, entry: &FileEntry, display_path: &str) -> bool {
        if entry.kind == FileEntryKind::Parent {
            return false;
        }
        if !self.include_hidden_files && entry.is_hidden {
            return false;
        }
        if !self.matches_type(entry) {
            return false;
        }
        let query = self.normalized_query();
        if query.is_empty() {
            return true;
        }
        if self.case_sensitive {
            entry.name.contains(query) || display_path.contains(query)
        } else {
            let query_lower = query.to_lowercase();
            entry.name.to_lowercase().contains(&query_lower)
                || display_path.to_lowercase().contains(&query_lower)
        }
    }

    /// 类型过滤（与 Kotlin `matchesType` 行为一致）。
    pub fn matches_type(&self, entry: &FileEntry) -> bool {
        match entry.kind {
            FileEntryKind::Parent => false,
            FileEntryKind::Directory => self.include_directories,
            _ => self.include_files,
        }
    }
}

impl Default for SearchOptions {
    fn default() -> Self {
        Self {
            query: String::new(),
            include_subdirectories: true,
            include_files: true,
            include_directories: true,
            case_sensitive: false,
            include_hidden_files: false,
        }
    }
}

/// 搜索命中条目（带相对根目录的展示路径）。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SearchHit {
    pub entry: FileEntry,
    /// 相对根目录的路径（用于 UI 展示层级）。
    pub relative_path: String,
}

/// 递归搜索执行器。
pub struct RecursiveSearcher;

impl RecursiveSearcher {
    /// 在 `root` 目录下递归搜索，返回匹配条目（不排序，按遍历顺序）。
    ///
    /// 目录本身也会被检查是否匹配（与 Kotlin 语义一致）。
    pub fn search(root: &str, options: &SearchOptions) -> std::io::Result<Vec<SearchHit>> {
        let mut results = Vec::new();
        let root_path = PathBuf::from(root);
        if !root_path.is_dir() {
            return Err(std::io::Error::new(
                std::io::ErrorKind::NotFound,
                format!("搜索根目录不存在：{root}"),
            ));
        }
        Self::walk(&root_path, "", options, &mut results)?;
        Ok(results)
    }

    fn walk(
        dir: &Path,
        rel_prefix: &str,
        options: &SearchOptions,
        results: &mut Vec<SearchHit>,
    ) -> std::io::Result<()> {
        for entry in fs::read_dir(dir)? {
            let entry = entry?;
            let file_name = entry.file_name().to_string_lossy().into_owned();
            let file_type = entry.file_type()?;

            let kind = if file_type.is_dir() {
                FileEntryKind::Directory
            } else if file_type.is_symlink() {
                FileEntryKind::Symlink
            } else {
                FileEntryKind::File
            };

            // 跳过 . 与 .. 伪条目（read_dir 不会产生，防御性处理）
            if file_name == "." || file_name == ".." {
                continue;
            }

            let path = entry.path().to_string_lossy().into_owned();
            let rel_path = if rel_prefix.is_empty() {
                file_name.clone()
            } else {
                format!("{rel_prefix}/{file_name}")
            };

            let fe = load_entry(&entry.path())?;

            if options.matches(&fe, &rel_path) {
                results.push(SearchHit {
                    entry: fe,
                    relative_path: rel_path.clone(),
                });
            }

            if kind == FileEntryKind::Directory && options.include_subdirectories {
                Self::walk(Path::new(&path), &rel_path, options, results)?;
            }
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn temp_tree(name: &str) -> PathBuf {
        let base =
            std::env::temp_dir().join(format!("sunset_search_{name}_{}", std::process::id()));
        let _ = fs::remove_dir_all(&base);
        fs::create_dir_all(base.join("sub/deep")).unwrap();
        fs::write(base.join("readme.md"), "# hi").unwrap();
        fs::write(base.join("main.rs"), "fn main() {}").unwrap();
        fs::write(base.join("sub/data.txt"), "data").unwrap();
        fs::write(base.join("sub/deep/notes.md"), "deep").unwrap();
        fs::write(base.join(".hidden.txt"), "secret").unwrap();
        base
    }

    #[test]
    fn finds_by_name_recursively() {
        let base = temp_tree("name");
        let options = SearchOptions {
            query: "readme".into(),
            ..Default::default()
        };
        let hits = RecursiveSearcher::search(&base.to_string_lossy(), &options).unwrap();
        assert_eq!(hits.len(), 1);
        assert_eq!(hits[0].entry.name, "readme.md");
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn finds_md_anywhere_case_insensitive() {
        let base = temp_tree("md");
        let options = SearchOptions {
            query: ".MD".into(),
            ..Default::default()
        };
        let hits = RecursiveSearcher::search(&base.to_string_lossy(), &options).unwrap();
        let names: Vec<&str> = hits.iter().map(|h| h.entry.name.as_str()).collect();
        assert_eq!(names.len(), 2);
        assert!(names.contains(&"readme.md"));
        assert!(names.contains(&"notes.md"));
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn hidden_files_excluded_by_default() {
        let base = temp_tree("hidden");
        let options = SearchOptions {
            query: "hidden".into(),
            ..Default::default()
        };
        let hits = RecursiveSearcher::search(&base.to_string_lossy(), &options).unwrap();
        assert!(hits.is_empty());

        let options = SearchOptions {
            query: "hidden".into(),
            include_hidden_files: true,
            ..Default::default()
        };
        let hits = RecursiveSearcher::search(&base.to_string_lossy(), &options).unwrap();
        assert_eq!(hits.len(), 1);
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn can_limit_to_directories_only() {
        let base = temp_tree("dirs");
        let options = SearchOptions {
            query: "sub".into(),
            include_files: false,
            include_directories: true,
            ..Default::default()
        };
        let hits = RecursiveSearcher::search(&base.to_string_lossy(), &options).unwrap();
        assert!(!hits.is_empty());
        assert!(hits
            .iter()
            .all(|h| h.entry.kind == FileEntryKind::Directory));
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn non_recursive_only_scans_top_level() {
        let base = temp_tree("flat");
        let options = SearchOptions {
            query: "".into(),
            include_subdirectories: false,
            include_hidden_files: true,
            ..Default::default()
        };
        let hits = RecursiveSearcher::search(&base.to_string_lossy(), &options).unwrap();
        // 顶层：readme.md、main.rs、.hidden.txt、sub（不含 sub 内部）
        let names: Vec<&str> = hits.iter().map(|h| h.entry.name.as_str()).collect();
        assert!(names.contains(&"readme.md"));
        assert!(names.contains(&"main.rs"));
        assert!(!names.contains(&"data.txt"));
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn blank_query_matches_everything_in_scope() {
        let base = temp_tree("blank");
        let options = SearchOptions {
            query: "   ".into(),
            include_hidden_files: true,
            ..Default::default()
        };
        let hits = RecursiveSearcher::search(&base.to_string_lossy(), &options).unwrap();
        // readme.md、main.rs、.hidden.txt、sub、data.txt、deep、notes.md
        assert_eq!(hits.len(), 7);
        fs::remove_dir_all(&base).ok();
    }
}
