//! 文件条目模型与类型识别。
//!
//! 移植自 Kotlin `FileEntryTypeResolver` 的判定逻辑，保持行为一致。

use std::path::Path;

/// 文件条目类型。
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub enum FileEntryKind {
    Directory,
    File,
    Symlink,
    /// 特殊条目（父目录返回等）。
    Parent,
    /// 未知/不可识别。
    Unknown,
}

/// 文件条目（UI 层展示所需的最小模型）。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct FileEntry {
    pub name: String,
    pub path: String,
    pub kind: FileEntryKind,
    pub size: u64,
    pub is_hidden: bool,
}

impl FileEntry {
    pub fn new(
        name: impl Into<String>,
        path: impl Into<String>,
        kind: FileEntryKind,
        size: u64,
    ) -> Self {
        let name = name.into();
        let is_hidden = name.starts_with('.');
        Self {
            name,
            path: path.into(),
            kind,
            size,
            is_hidden,
        }
    }
}

/// 根据路径元信息判定条目类型。
pub fn resolve_kind(metadata: &std::fs::Metadata) -> FileEntryKind {
    let file_type = metadata.file_type();
    if file_type.is_dir() {
        FileEntryKind::Directory
    } else if file_type.is_symlink() {
        FileEntryKind::Symlink
    } else if file_type.is_file() {
        FileEntryKind::File
    } else {
        FileEntryKind::Unknown
    }
}

/// 依据扩展名判断文件大类（用于 UI 图标/预览分发）。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum FileCategory {
    Text,
    Code,
    Markdown,
    Image,
    Video,
    Audio,
    Pdf,
    Archive,
    Apk,
    Dex,
    Binary,
}

/// 常见文本/代码扩展名集合。
const TEXT_EXTENSIONS: &[&str] = &[
    "txt", "log", "ini", "cfg", "conf", "md", "markdown", "kt", "java", "rs", "py", "js",
    "ts", "json", "xml", "html", "css", "toml", "yaml", "yml", "sh", "gradle", "sql",
];

/// 依据文件名判定内容类别。
pub fn categorize(name: &str) -> FileCategory {
    let lower = name.to_ascii_lowercase();
    let ext = Path::new(&lower)
        .extension()
        .map(|e| e.to_string_lossy().to_string())
        .unwrap_or_default();

    match ext.as_str() {
        "md" | "markdown" => FileCategory::Markdown,
        "apk" | "aab" => FileCategory::Apk,
        "dex" => FileCategory::Dex,
        "zip" | "7z" | "rar" | "tar" | "gz" | "bz2" | "xz" | "jar" => FileCategory::Archive,
        "png" | "jpg" | "jpeg" | "gif" | "webp" | "bmp" | "svg" | "ico" => FileCategory::Image,
        "mp4" | "mkv" | "avi" | "mov" | "webm" | "flv" => FileCategory::Video,
        "mp3" | "wav" | "flac" | "aac" | "ogg" | "m4a" => FileCategory::Audio,
        "pdf" => FileCategory::Pdf,
        "kt" | "java" | "rs" | "py" | "js" | "ts" | "c" | "cpp" | "h" | "go" | "swift"
        | "php" | "rb" | "sql" | "gradle" | "sh" | "toml" | "yaml" | "yml" | "json"
        | "xml" | "html" | "css" => FileCategory::Code,
        _ if TEXT_EXTENSIONS.contains(&ext.as_str()) => FileCategory::Text,
        _ => FileCategory::Binary,
    }
}

/// 人类可读的文件大小格式化（与 Kotlin FileSizeFormatter 行为一致）。
pub fn format_size(bytes: u64) -> String {
    const KB: f64 = 1024.0;
    const MB: f64 = KB * 1024.0;
    const GB: f64 = MB * 1024.0;
    let b = bytes as f64;

    if bytes < 1024 {
        format!("{bytes} B")
    } else if b < MB {
        format!("{:.1} KB", b / KB)
    } else if b < GB {
        format!("{:.2} MB", b / MB)
    } else {
        format!("{:.2} GB", b / GB)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn categorizes_by_extension() {
        assert_eq!(categorize("readme.md"), FileCategory::Markdown);
        assert_eq!(categorize("app-debug.apk"), FileCategory::Apk);
        assert_eq!(categorize("classes.dex"), FileCategory::Dex);
        assert_eq!(categorize("photo.PNG"), FileCategory::Image);
        assert_eq!(categorize("MainActivity.kt"), FileCategory::Code);
        assert_eq!(categorize("archive.zip"), FileCategory::Archive);
        assert_eq!(categorize("notes.txt"), FileCategory::Text);
        assert_eq!(categorize("file.unknown_ext"), FileCategory::Binary);
    }

    #[test]
    fn formats_sizes() {
        assert_eq!(format_size(512), "512 B");
        assert_eq!(format_size(2048), "2.0 KB");
        assert_eq!(format_size(5 * 1024 * 1024), "5.00 MB");
        assert_eq!(format_size(2 * 1024 * 1024 * 1024), "2.00 GB");
    }

    #[test]
    fn hidden_detection() {
        let entry = FileEntry::new(".gitignore", "/tmp/.gitignore", FileEntryKind::File, 0);
        assert!(entry.is_hidden);
        let entry = FileEntry::new("README.md", "/tmp/README.md", FileEntryKind::File, 0);
        assert!(!entry.is_hidden);
    }
}