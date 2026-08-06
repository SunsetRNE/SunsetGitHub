//! 本地文件引擎：条目模型、排序、类型识别、大小格式化、路径工具、
//! 文件操作（复制/移动/删除/重命名）、回收站、递归搜索、双栏状态机。
//!
//! 对应原 Kotlin `domain/filemanager` 的纯逻辑部分
//! （FileEntryTypeResolver / FileManagerEntrySorter / FileSizeFormatter /
//! FileOperation / RecycleBinSettings / FileManagerSearchOptions /
//! FileManagerDualPaneState / FileManagerPaneNavigationState 等）。

pub mod entry;
pub mod operation;
pub mod pane;
pub mod path;
pub mod recycle;
pub mod root;
pub mod search;
pub mod sort;

pub use entry::{FileEntry, FileEntryKind};
pub use operation::{
    ConflictAction, ErrorAction, FileOperationRunner, OperationContext, OperationEvent,
    OperationKind, OperationOptions,
};
pub use pane::{
    DualPaneNavigationState, DualPaneState, PaneId, PaneNavigationState, PaneTransferTarget,
    PaneTransferTargetResolver,
};
pub use path::{file_name, is_within, join_path, normalize_path, parent_path};
pub use recycle::{RecycleBin, RecycleBinEntry, RecycleBinSettings};
pub use search::{RecursiveSearcher, SearchHit, SearchOptions};
pub use sort::{FileManagerEntrySorter, SortMode, SortOptions};