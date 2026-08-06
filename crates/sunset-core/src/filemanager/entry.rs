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
///
/// 对齐 Material Files 的 `FileItem` 设计：条目携带一次加载的属性快照
/// （大小/修改时间/隐藏/符号链接/类型），UI 直接消费，无需重复 stat。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct FileEntry {
    pub name: String,
    pub path: String,
    pub kind: FileEntryKind,
    pub size: u64,
    pub is_hidden: bool,
    /// 最后修改时间（Unix 秒）。
    pub modified: Option<u64>,
    /// 符号链接目标（仅 kind == Symlink 时）。
    pub symlink_target: Option<String>,
    /// 链接是否损坏（目标不存在）。
    pub is_symlink_broken: Option<bool>,
    /// MIME 类型（按扩展名推断，如 "image/png"）。
    pub mime_type: String,
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
        let mime_type = mime_type_of(&name).to_string();
        Self {
            name,
            path: path.into(),
            kind,
            size,
            is_hidden,
            modified: None,
            symlink_target: None,
            is_symlink_broken: None,
            mime_type,
        }
    }

    /// 是否为目录或指向目录的链接（UI 双击进入判定）。
    pub fn is_directory(&self) -> bool {
        matches!(self.kind, FileEntryKind::Directory)
    }

    /// 内容类别（按扩展名）。
    pub fn category(&self) -> FileCategory {
        categorize(&self.name)
    }
}

/// 从文件系统加载条目（对齐 Material Files `Path.loadFileItem()`）。
///
/// - 符号链接：读取目标并解析目标属性，判定是否损坏；
/// - 隐藏：以 `.` 开头；
/// - MIME：按扩展名推断。
pub fn load_entry(path: &Path) -> std::io::Result<FileEntry> {
    let metadata = std::fs::symlink_metadata(path)?;
    let name = path
        .file_name()
        .map(|n| n.to_string_lossy().into_owned())
        .unwrap_or_else(|| path.to_string_lossy().into_owned());
    let kind = resolve_kind(&metadata);
    let size = if metadata.file_type().is_symlink() {
        // 链接本身大小无意义，尝试读目标大小
        std::fs::metadata(path).map(|m| m.len()).unwrap_or(0)
    } else {
        metadata.len()
    };
    let modified = metadata
        .modified()
        .ok()
        .and_then(|t| t.duration_since(std::time::UNIX_EPOCH).ok())
        .map(|d| d.as_secs());

    let mut entry = FileEntry::new(name, path.to_string_lossy().into_owned(), kind, size);
    entry.modified = modified;

    if entry.kind == FileEntryKind::Symlink {
        entry.symlink_target = std::fs::read_link(path)
            .ok()
            .map(|t| t.to_string_lossy().into_owned());
        entry.is_symlink_broken = Some(std::fs::metadata(path).is_err());
    }
    Ok(entry)
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

/// 依据扩展名推断 MIME 类型（对齐 Material Files `MimeType` 的常用映射）。
pub fn mime_type_of(name: &str) -> &'static str {
    let lower = name.to_ascii_lowercase();
    let ext = Path::new(&lower)
        .extension()
        .map(|e| e.to_string_lossy().to_string())
        .unwrap_or_default();
    match ext.as_str() {
        "txt" | "log" | "ini" | "cfg" | "conf" => "text/plain",
        "md" | "markdown" => "text/markdown",
        "html" | "htm" => "text/html",
        "css" => "text/css",
        "csv" => "text/csv",
        "json" => "application/json",
        "xml" => "application/xml",
        "yml" | "yaml" => "application/yaml",
        "toml" => "application/toml",
        "pdf" => "application/pdf",
        "apk" => "application/vnd.android.package-archive",
        "aab" => "application/x-authorware-bin",
        "dex" => "application/x-dex",
        "jar" | "zip" => "application/zip",
        "7z" => "application/x-7z-compressed",
        "rar" => "application/vnd.rar",
        "tar" => "application/x-tar",
        "gz" => "application/gzip",
        "bz2" => "application/x-bzip2",
        "xz" => "application/x-xz",
        "png" => "image/png",
        "jpg" | "jpeg" => "image/jpeg",
        "gif" => "image/gif",
        "webp" => "image/webp",
        "bmp" => "image/bmp",
        "svg" => "image/svg+xml",
        "ico" => "image/x-icon",
        "mp4" => "video/mp4",
        "mkv" => "video/x-matroska",
        "avi" => "video/x-msvideo",
        "mov" => "video/quicktime",
        "webm" => "video/webm",
        "flv" => "video/x-flv",
        "mp3" => "audio/mpeg",
        "wav" => "audio/wav",
        "flac" => "audio/flac",
        "aac" => "audio/aac",
        "ogg" => "audio/ogg",
        "m4a" => "audio/mp4",
        "kt" | "java" | "rs" | "py" | "js" | "ts" | "c" | "cpp" | "h" | "go" | "swift"
        | "php" | "rb" | "sql" | "gradle" | "sh" => "text/x-source",
        _ => "application/octet-stream",
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

    #[test]
    fn new_entry_field_defaults() {
        let entry = FileEntry::new("photo.png", "/tmp/photo.png", FileEntryKind::File, 42);
        assert_eq!(entry.modified, None);
        assert_eq!(entry.symlink_target, None);
        assert_eq!(entry.is_symlink_broken, None);
        // MIME 按扩展名自动推断
        assert_eq!(entry.mime_type, "image/png");
        // 目录不因名称带点误判为隐藏（隐藏判定只看首字符）
        let dir = FileEntry::new("project.v2", "/tmp/project.v2", FileEntryKind::Directory, 0);
        assert!(!dir.is_hidden);
        assert_eq!(dir.mime_type, "application/octet-stream");
    }

    #[test]
    fn is_directory_and_category() {
        let dir = FileEntry::new("docs", "/tmp/docs", FileEntryKind::Directory, 0);
        assert!(dir.is_directory());
        assert_eq!(dir.category(), FileCategory::Binary);

        let file = FileEntry::new("notes.md", "/tmp/notes.md", FileEntryKind::File, 10);
        assert!(!file.is_directory());
        assert_eq!(file.category(), FileCategory::Markdown);

        // 指向目录的链接不算可进入（Material Files 中链接需再解析）
        let link = FileEntry::new("link", "/tmp/link", FileEntryKind::Symlink, 0);
        assert!(!link.is_directory());
    }

    #[test]
    fn mime_type_mapping() {
        assert_eq!(mime_type_of("a.txt"), "text/plain");
        assert_eq!(mime_type_of("README.MD"), "text/markdown");
        assert_eq!(mime_type_of("app-debug.apk"), "application/vnd.android.package-archive");
        assert_eq!(mime_type_of("classes.dex"), "application/x-dex");
        assert_eq!(mime_type_of("archive.zip"), "application/zip");
        assert_eq!(mime_type_of("song.flac"), "audio/flac");
        assert_eq!(mime_type_of("video.mkv"), "video/x-matroska");
        assert_eq!(mime_type_of("MainActivity.kt"), "text/x-source");
        assert_eq!(mime_type_of("noext"), "application/octet-stream");
        assert_eq!(mime_type_of("archive.tar.gz"), "application/gzip");
    }

    #[test]
    fn load_entry_from_filesystem() {
        let dir = std::env::temp_dir().join("sunset_entry_test");
        std::fs::create_dir_all(&dir).unwrap();
        let file = dir.join("hello.txt");
        std::fs::write(&file, b"hello world").unwrap();

        let entry = load_entry(&file).unwrap();
        assert_eq!(entry.name, "hello.txt");
        assert_eq!(entry.kind, FileEntryKind::File);
        assert_eq!(entry.size, 11);
        assert_eq!(entry.mime_type, "text/plain");
        assert!(entry.modified.is_some());

        let dir_entry = load_entry(&dir).unwrap();
        assert_eq!(dir_entry.kind, FileEntryKind::Directory);
        assert!(dir_entry.is_directory());

        std::fs::remove_dir_all(&dir).unwrap();
    }

    #[cfg(unix)]
    #[test]
    fn load_entry_resolves_symlinks() {
        use std::os::unix::fs::symlink;

        let dir = std::env::temp_dir().join("sunset_symlink_test");
        let _ = std::fs::remove_dir_all(&dir);
        std::fs::create_dir_all(&dir).unwrap();
        let target = dir.join("target.txt");
        std::fs::write(&target, b"data").unwrap();
        let link = dir.join("link.txt");
        let broken = dir.join("broken.txt");
        symlink(&target, &link).unwrap();
        symlink(dir.join("missing.txt"), &broken).unwrap();

        let entry = load_entry(&link).unwrap();
        assert_eq!(entry.kind, FileEntryKind::Symlink);
        assert_eq!(
            entry.symlink_target.as_deref(),
            Some(target.to_string_lossy().as_ref())
        );
        assert_eq!(entry.is_symlink_broken, Some(false));
        // 链接大小读目标大小
        assert_eq!(entry.size, 4);

        let broken_entry = load_entry(&broken).unwrap();
        assert_eq!(broken_entry.kind, FileEntryKind::Symlink);
        assert_eq!(broken_entry.is_symlink_broken, Some(true));
        assert_eq!(broken_entry.size, 0);

        std::fs::remove_dir_all(&dir).unwrap();
    }

    #[test]
    fn load_entry_missing_path_errors() {
        let missing = std::env::temp_dir().join("sunset_missing_xyz");
        assert!(load_entry(&missing).is_err());
    }
}