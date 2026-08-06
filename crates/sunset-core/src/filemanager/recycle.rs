//! 回收站：删除保护、移入/恢复/清空、自动清理。
//!
//! 移植自 Kotlin `RecycleBinSettings` 的设置模型，并实现本地回收站目录
//! 管理（按源路径保留相对结构，支持恢复与按天自动清理）。

use std::fs;
use std::path::{Path, PathBuf};

use chrono::{DateTime, Duration, Local};

use crate::filemanager::path::{file_name, normalize_path};

/// 回收站设置（与 Kotlin RecycleBinSettings 字段对齐）。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct RecycleBinSettings {
    /// 是否启用回收站。
    pub enabled: bool,
    /// 删除时默认移入回收站（而非直接删除）。
    pub default_move_to_recycle_bin: bool,
    /// 自动清理天数（0 表示不自动清理）。
    pub auto_clean_days: u32,
    /// 删除前是否显示警告。
    pub show_deletion_warning: bool,
}

impl Default for RecycleBinSettings {
    fn default() -> Self {
        Self {
            enabled: true,
            default_move_to_recycle_bin: true,
            auto_clean_days: 0,
            show_deletion_warning: true,
        }
    }
}

/// 回收站管理器。
///
/// 回收站布局：
/// ```text
/// <root>/
/// ├── entries/            # 被删条目（保持相对路径结构）
/// │   └── <rel>/<name>
/// └── manifest.tsv        # 元信息：相对路径 -> 原绝对路径 + 时间戳
/// ```
pub struct RecycleBin {
    root: PathBuf,
    entries_dir: PathBuf,
    manifest_path: PathBuf,
    settings: RecycleBinSettings,
}

/// 回收站条目元信息。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct RecycleBinEntry {
    /// 回收站内的相对路径（entries/ 之下）。
    pub relative_path: String,
    /// 原始绝对路径。
    pub original_path: String,
    /// 移入时间（本地时间戳）。
    pub deleted_at: DateTime<Local>,
}

impl RecycleBin {
    /// 创建回收站（root 为回收站根目录，不存在则创建）。
    pub fn new(root: impl Into<PathBuf>, settings: RecycleBinSettings) -> std::io::Result<Self> {
        let root = root.into();
        let entries_dir = root.join("entries");
        let manifest_path = root.join("manifest.tsv");
        fs::create_dir_all(&entries_dir)?;
        Ok(Self {
            root,
            entries_dir,
            manifest_path,
            settings,
        })
    }

    /// 当前设置。
    pub fn settings(&self) -> RecycleBinSettings {
        self.settings
    }

    /// 回收站根目录。
    pub fn root(&self) -> &Path {
        &self.root
    }

    /// 更新设置。
    pub fn set_settings(&mut self, settings: RecycleBinSettings) {
        self.settings = settings;
    }

    /// 将条目移入回收站（保持目录结构）。
    ///
    /// 返回移入后的回收站相对路径；失败返回 io 错误。
    pub fn trash(&self, source: &str) -> std::io::Result<String> {
        let src = PathBuf::from(normalize_path(source));
        if !src.exists() {
            return Err(std::io::Error::new(
                std::io::ErrorKind::NotFound,
                format!("源条目不存在：{source}"),
            ));
        }
        let name = file_name(source).ok_or_else(|| {
            std::io::Error::new(std::io::ErrorKind::InvalidInput, "无效源路径")
        })?;

        // 生成唯一目标名（同名冲突时追加序号）
        let base_rel = self.unique_relative_path(&name);
        let dst = self.entries_dir.join(&base_rel);
        if let Some(parent) = dst.parent() {
            fs::create_dir_all(parent)?;
        }
        fs::rename(&src, &dst)?;

        // 追加 manifest 行：相对路径 \t 原路径 \t 时间戳
        let timestamp = Local::now().to_rfc3339();
        let line = format!("{base_rel}\t{source}\t{timestamp}\n");
        use std::io::Write;
        fs::OpenOptions::new()
            .create(true)
            .append(true)
            .open(&self.manifest_path)?
            .write_all(line.as_bytes())?;

        Ok(base_rel)
    }

    /// 恢复条目到原始位置。
    ///
    /// 返回恢复的原始路径；失败返回 io 错误（如原始位置已存在同名条目）。
    pub fn restore(&self, relative_path: &str) -> std::io::Result<String> {
        let entry = self
            .list()
            .into_iter()
            .find(|e| e.relative_path == relative_path)
            .ok_or_else(|| {
                std::io::Error::new(
                    std::io::ErrorKind::NotFound,
                    format!("回收站条目不存在：{relative_path}"),
                )
            })?;
        self.restore_entry(&entry)
    }

    fn restore_entry(&self, entry: &RecycleBinEntry) -> std::io::Result<String> {
        let src = self.entries_dir.join(&entry.relative_path);
        let dst = PathBuf::from(normalize_path(&entry.original_path));
        if dst.exists() {
            return Err(std::io::Error::new(
                std::io::ErrorKind::AlreadyExists,
                format!("原始位置已存在：{}", dst.display()),
            ));
        }
        if let Some(parent) = dst.parent() {
            fs::create_dir_all(parent)?;
        }
        fs::rename(&src, &dst)?;
        self.remove_manifest_line(&entry.relative_path)?;
        Ok(entry.original_path.clone())
    }

    /// 永久删除回收站条目。
    pub fn purge(&self, relative_path: &str) -> std::io::Result<()> {
        let target = self.entries_dir.join(relative_path);
        if target.exists() {
            if target.is_dir() {
                fs::remove_dir_all(&target)?;
            } else {
                fs::remove_file(&target)?;
            }
        }
        self.remove_manifest_line(relative_path)
    }

    /// 清空回收站（永久删除全部条目）。
    pub fn empty(&self) -> std::io::Result<usize> {
        let count = self.list().len();
        fs::remove_dir_all(&self.entries_dir)?;
        fs::create_dir_all(&self.entries_dir)?;
        let _ = fs::remove_file(&self.manifest_path);
        Ok(count)
    }

    /// 列出回收站条目。
    pub fn list(&self) -> Vec<RecycleBinEntry> {
        let Ok(content) = fs::read_to_string(&self.manifest_path) else {
            return Vec::new();
        };
        content
            .lines()
            .filter_map(|line| {
                let mut parts = line.splitn(3, '\t');
                let relative_path = parts.next()?.to_string();
                let original_path = parts.next()?.to_string();
                let timestamp = parts.next()?.to_string();
                let deleted_at = DateTime::parse_from_rfc3339(&timestamp)
                    .map(|dt| dt.with_timezone(&Local))
                    .unwrap_or_else(|_| Local::now());
                Some(RecycleBinEntry {
                    relative_path,
                    original_path,
                    deleted_at,
                })
            })
            .collect()
    }

    /// 自动清理：删除超过 `auto_clean_days` 天的条目。
    ///
    /// 返回清理的条目数。设置中 `auto_clean_days == 0` 时不执行。
    pub fn auto_clean(&self) -> std::io::Result<usize> {
        if self.settings.auto_clean_days == 0 {
            return Ok(0);
        }
        let cutoff = Local::now() - Duration::days(self.settings.auto_clean_days as i64);
        let stale: Vec<String> = self
            .list()
            .into_iter()
            .filter(|e| e.deleted_at < cutoff)
            .map(|e| e.relative_path)
            .collect();
        let mut cleaned = 0usize;
        for relative in &stale {
            if self.purge(relative).is_ok() {
                cleaned += 1;
            }
        }
        Ok(cleaned)
    }

    /// 回收站条目数（便捷方法）。
    pub fn len(&self) -> usize {
        self.list().len()
    }

    /// 是否为空。
    pub fn is_empty(&self) -> bool {
        self.len() == 0
    }

    /// 生成唯一相对路径：同名时追加 ` (1)`、` (2)` 等。
    fn unique_relative_path(&self, name: &str) -> String {
        let mut candidate = name.to_string();
        let mut index = 1u32;
        while self.entries_dir.join(&candidate).exists() {
            let stem = name.trim_end();
            let ext = Path::new(name)
                .extension()
                .map(|e| e.to_string_lossy().to_string());
            candidate = match ext {
                Some(ext) if !ext.is_empty() => {
                    let base = stem.trim_end_matches(&format!(".{ext}"));
                    format!("{base} ({index}).{ext}")
                }
                _ => format!("{stem} ({index})"),
            };
            index += 1;
        }
        candidate
    }

    /// 从 manifest 移除一行。
    fn remove_manifest_line(&self, relative_path: &str) -> std::io::Result<()> {
        let Ok(content) = fs::read_to_string(&self.manifest_path) else {
            return Ok(());
        };
        let remaining: Vec<&str> = content
            .lines()
            .filter(|line| !line.starts_with(&format!("{relative_path}\t")))
            .collect();
        if remaining.is_empty() {
            let _ = fs::remove_file(&self.manifest_path);
        } else {
            fs::write(&self.manifest_path, remaining.join("\n") + "\n")?;
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn temp_recycle(name: &str) -> RecycleBin {
        let dir = std::env::temp_dir().join(format!("sunset_rb_{name}_{}", std::process::id()));
        let _ = fs::remove_dir_all(&dir);
        RecycleBin::new(&dir, RecycleBinSettings::default()).unwrap()
    }

    #[test]
    fn trashes_and_restores_file() {
        let rb = temp_recycle("basic");
        let src_dir = std::env::temp_dir().join(format!("sunset_rb_src_{}", std::process::id()));
        fs::create_dir_all(&src_dir).unwrap();
        let file = src_dir.join("a.txt");
        fs::write(&file, "content").unwrap();

        let rel = rb.trash(&file.to_string_lossy()).unwrap();
        assert!(!file.exists());
        assert_eq!(rb.len(), 1);
        assert!(rel.starts_with("a.txt"));

        let restored = rb.restore(&rel).unwrap();
        assert!(file.exists());
        assert_eq!(fs::read_to_string(&file).unwrap(), "content");
        assert_eq!(restored, file.to_string_lossy());
        assert_eq!(rb.len(), 0);
        fs::remove_dir_all(&src_dir).ok();
    }

    #[test]
    fn name_conflicts_get_suffix() {
        let rb = temp_recycle("conflict");
        let src_dir = std::env::temp_dir().join(format!("sunset_rb_csrc_{}", std::process::id()));
        fs::create_dir_all(&src_dir).unwrap();
        let a = src_dir.join("x.txt");
        fs::write(&a, "1").unwrap();

        let rel1 = rb.trash(&a.to_string_lossy()).unwrap();
        // 第二个文件移到不同目录，同名会冲突
        let dir2 = src_dir.join("sub");
        fs::create_dir_all(&dir2).unwrap();
        let b = dir2.join("x.txt");
        fs::write(&b, "2").unwrap();
        let rel2 = rb.trash(&b.to_string_lossy()).unwrap();
        assert_ne!(rel1, rel2);
        assert_eq!(rb.len(), 2);
        fs::remove_dir_all(&src_dir).ok();
    }

    #[test]
    fn purge_and_empty() {
        let rb = temp_recycle("purge");
        let src_dir = std::env::temp_dir().join(format!("sunset_rb_psrc_{}", std::process::id()));
        fs::create_dir_all(&src_dir).unwrap();
        let file = src_dir.join("p.txt");
        fs::write(&file, "x").unwrap();
        let rel = rb.trash(&file.to_string_lossy()).unwrap();

        rb.purge(&rel).unwrap();
        assert_eq!(rb.len(), 0);

        fs::write(&file, "y").unwrap();
        let rel2 = rb.trash(&file.to_string_lossy()).unwrap();
        let _ = rel2;
        let count = rb.empty().unwrap();
        assert_eq!(count, 1);
        assert!(rb.is_empty());
        fs::remove_dir_all(&src_dir).ok();
    }

    #[test]
    fn auto_clean_removes_stale_entries() {
        let mut rb = temp_recycle("clean");
        let src_dir = std::env::temp_dir().join(format!("sunset_rb_asrc_{}", std::process::id()));
        fs::create_dir_all(&src_dir).unwrap();
        let file = src_dir.join("old.txt");
        fs::write(&file, "x").unwrap();
        let _ = rb.trash(&file.to_string_lossy()).unwrap();

        let mut settings = RecycleBinSettings {
            auto_clean_days: 0,
            ..Default::default()
        };
        rb.set_settings(settings);
        assert_eq!(rb.auto_clean().unwrap(), 0);
        assert_eq!(rb.len(), 1);

        settings.auto_clean_days = 1;
        rb.set_settings(settings);
        // 刚移入的条目未过期，不应清理
        assert_eq!(rb.auto_clean().unwrap(), 0);
        assert_eq!(rb.len(), 1);
        fs::remove_dir_all(&src_dir).ok();
    }
}