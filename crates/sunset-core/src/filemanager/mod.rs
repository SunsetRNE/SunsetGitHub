//! 本地文件引擎：条目模型、排序、类型识别、大小格式化。
//!
//! 对应原 Kotlin `domain/filemanager` 的纯逻辑部分
//! （FileEntryTypeResolver / FileManagerEntrySorter / FileSizeFormatter）。

pub mod entry;
pub mod sort;

pub use entry::{FileEntry, FileEntryKind};
pub use sort::{FileManagerEntrySorter, SortMode};