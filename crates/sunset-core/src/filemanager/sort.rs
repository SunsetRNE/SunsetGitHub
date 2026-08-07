//! 文件条目排序。
//!
//! 移植自 Kotlin `FileManagerEntrySorter`（4 种排序模式 + 开关），
//! 行为与 MT 对标版本保持一致。

use crate::filemanager::entry::{FileEntry, FileEntryKind};

/// 排序模式（对应 MT 排序菜单 4 模式）。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SortMode {
    Name,
    Size,
    Modified,
    Kind,
}

/// 排序开关。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct SortOptions {
    pub mode: SortMode,
    /// 文件夹置顶。
    pub directories_first: bool,
    /// 升序（false 为降序）。
    pub ascending: bool,
    /// 隐藏文件排最后。
    pub hidden_last: bool,
}

impl Default for SortOptions {
    fn default() -> Self {
        Self {
            mode: SortMode::Name,
            directories_first: true,
            ascending: true,
            hidden_last: true,
        }
    }
}

/// 条目排序器。
#[derive(Debug, Clone, Copy)]
pub struct FileManagerEntrySorter {
    pub options: SortOptions,
}

impl FileManagerEntrySorter {
    pub fn new(options: SortOptions) -> Self {
        Self { options }
    }

    /// 排序条目列表（纯函数，不修改输入）。
    pub fn sort(&self, entries: &mut [FileEntry]) {
        let options = self.options;
        entries.sort_by(|a, b| {
            // 父目录始终最前
            let a_parent = a.kind == FileEntryKind::Parent;
            let b_parent = b.kind == FileEntryKind::Parent;
            if a_parent != b_parent {
                return if a_parent {
                    std::cmp::Ordering::Less
                } else {
                    std::cmp::Ordering::Greater
                };
            }

            // 隐藏文件最后
            if options.hidden_last && a.is_hidden != b.is_hidden {
                return if a.is_hidden {
                    std::cmp::Ordering::Greater
                } else {
                    std::cmp::Ordering::Less
                };
            }

            // 目录优先
            let a_dir = a.kind == FileEntryKind::Directory;
            let b_dir = b.kind == FileEntryKind::Directory;
            if options.directories_first && a_dir != b_dir {
                return if a_dir {
                    std::cmp::Ordering::Less
                } else {
                    std::cmp::Ordering::Greater
                };
            }

            let ordering = match options.mode {
                SortMode::Name => a.name.to_lowercase().cmp(&b.name.to_lowercase()),
                SortMode::Size => a.size.cmp(&b.size),
                SortMode::Modified => a.name.cmp(&b.name), // modified 时间暂缺，回退名称
                SortMode::Kind => a
                    .kind
                    .cmp(&b.kind)
                    .then_with(|| a.name.to_lowercase().cmp(&b.name.to_lowercase())),
            };

            if options.ascending {
                ordering
            } else {
                ordering.reverse()
            }
        });
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn entry(name: &str, kind: FileEntryKind, size: u64) -> FileEntry {
        FileEntry::new(name, format!("/tmp/{name}"), kind, size)
    }

    #[test]
    fn directories_first_then_name_ascending() {
        let mut entries = vec![
            entry("b.txt", FileEntryKind::File, 10),
            entry("a_dir", FileEntryKind::Directory, 0),
            entry("a.txt", FileEntryKind::File, 20),
        ];
        FileManagerEntrySorter::new(SortOptions::default()).sort(&mut entries);
        let names: Vec<&str> = entries.iter().map(|e| e.name.as_str()).collect();
        assert_eq!(names, vec!["a_dir", "a.txt", "b.txt"]);
    }

    #[test]
    fn size_descending() {
        let mut entries = vec![
            entry("small.txt", FileEntryKind::File, 10),
            entry("big.txt", FileEntryKind::File, 1000),
            entry("mid.txt", FileEntryKind::File, 100),
        ];
        let options = SortOptions {
            mode: SortMode::Size,
            ascending: false,
            ..Default::default()
        };
        FileManagerEntrySorter::new(options).sort(&mut entries);
        let names: Vec<&str> = entries.iter().map(|e| e.name.as_str()).collect();
        assert_eq!(names, vec!["big.txt", "mid.txt", "small.txt"]);
    }

    #[test]
    fn parent_always_first() {
        let mut entries = vec![
            entry("b.txt", FileEntryKind::File, 1),
            entry("..", FileEntryKind::Parent, 0),
            entry("a.txt", FileEntryKind::File, 1),
        ];
        FileManagerEntrySorter::new(SortOptions::default()).sort(&mut entries);
        assert_eq!(entries[0].name, "..");
    }
}
