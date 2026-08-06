//! 文件操作：复制、移动、重命名、删除。
//!
//! 移植自 Kotlin `operation/FileOperation.kt` 与 `LegacyFileOperationRunner.kt`
//! 的事件模型（Started/Progress/ConflictDetected/Completed/Failed/Cancelled），
//! 以同步 + 回调方式执行，便于 FFI 桥接与测试。

use std::fs;
use std::path::{Path, PathBuf};

use crate::filemanager::path::{file_name, is_within, join_path, normalize_path};

/// 文件操作类型。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum OperationKind {
    Copy,
    Move,
    Rename,
    Delete,
}

/// 操作上下文：源条目列表 + 目标目录（删除时目标可为空）。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct OperationContext {
    pub sources: Vec<String>,
    pub target: Option<String>,
    pub title: String,
}

impl OperationContext {
    pub fn new(sources: Vec<String>, target: Option<String>) -> Self {
        Self {
            sources,
            target,
            title: "文件操作".to_string(),
        }
    }
}

/// 操作事件（与 Kotlin FileOperationEvent 对齐）。
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum OperationEvent {
    Started { title: String },
    Progress { current: u64, total: Option<u64>, message: String },
    ConflictDetected { source: String, target: String },
    Completed { summary: String },
    Failed { message: String },
    Cancelled,
}

/// 校验结果。
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum OperationValidation {
    Valid,
    Invalid(String),
}

/// 操作进度回调：参数为（当前完成数，总数）。
pub type ProgressCallback<'a> = dyn FnMut(u64, Option<u64>) + 'a;

/// 文件操作执行器（同步）。
pub struct FileOperationRunner;

impl FileOperationRunner {
    /// 校验操作上下文。
    pub fn validate(kind: OperationKind, context: &OperationContext) -> OperationValidation {
        if context.sources.is_empty() {
            return OperationValidation::Invalid("没有可处理的条目。".to_string());
        }
        for source in &context.sources {
            let normalized = normalize_path(source);
            if normalized == "/" || normalized == "root:///" {
                return OperationValidation::Invalid("不能操作根目录。".to_string());
            }
            if !Path::new(&normalized).exists() {
                return OperationValidation::Invalid(format!("源条目不存在：{source}"));
            }
        }
        match kind {
            OperationKind::Rename => {
                if context.sources.len() != 1 {
                    return OperationValidation::Invalid("重命名仅支持单个条目。".to_string());
                }
                let Some(target) = context.target.as_deref() else {
                    return OperationValidation::Invalid("重命名需要目标路径。".to_string());
                };
                if target.trim().is_empty() {
                    return OperationValidation::Invalid("目标路径为空。".to_string());
                }
                let target_norm = normalize_path(target);
                if target_norm == normalize_path(&context.sources[0]) {
                    return OperationValidation::Invalid("目标与源相同。".to_string());
                }
            }
            OperationKind::Copy | OperationKind::Move => {
                let Some(target) = context.target.as_deref() else {
                    return OperationValidation::Invalid("复制/移动需要目标目录。".to_string());
                };
                let target_norm = normalize_path(target);
                if !Path::new(&target_norm).is_dir() {
                    return OperationValidation::Invalid(format!("目标不是目录：{target}"));
                }
                for source in &context.sources {
                    let src_norm = normalize_path(source);
                    // 阻止复制/移动到自身或其子目录
                    if is_within(&target_norm, &src_norm) {
                        return OperationValidation::Invalid(format!(
                            "不能将条目复制/移动到自身内部：{source}"
                        ));
                    }
                    if kind == OperationKind::Move && src_norm == target_norm {
                        return OperationValidation::Invalid("源与目标相同。".to_string());
                    }
                }
            }
            OperationKind::Delete => {}
        }
        OperationValidation::Valid
    }

    /// 执行操作，通过 `on_progress` 报告进度，返回事件序列。
    pub fn run(
        kind: OperationKind,
        context: &OperationContext,
        mut on_progress: Option<&mut ProgressCallback>,
    ) -> Vec<OperationEvent> {
        let mut events = Vec::new();
        let title = context.title.clone();
        events.push(OperationEvent::Started { title });

        if let OperationValidation::Invalid(message) = Self::validate(kind, context) {
            events.push(OperationEvent::Failed { message });
            return events;
        }

        let total = context.sources.len() as u64;
        let mut completed: u64 = 0;
        let mut failed = 0usize;

        for source in &context.sources {
            let result = match kind {
                OperationKind::Copy => Self::copy_entry(source, context.target.as_deref().unwrap()),
                OperationKind::Move => {
                    Self::move_entry(source, context.target.as_deref().unwrap())
                }
                OperationKind::Rename => {
                    Self::rename_entry(source, context.target.as_deref().unwrap())
                }
                OperationKind::Delete => Self::delete_entry(source),
            };
            completed += 1;
            match result {
                Ok(()) => {}
                Err(err) => {
                    failed += 1;
                    events.push(OperationEvent::Failed {
                        message: format!("{source}: {err}"),
                    });
                }
            }
            if let Some(cb) = on_progress.as_deref_mut() {
                cb(completed, Some(total));
            }
            events.push(OperationEvent::Progress {
                current: completed,
                total: Some(total),
                message: format!("{completed}/{total}"),
            });
        }

        if failed == 0 {
            events.push(OperationEvent::Completed {
                summary: format!("{} 个条目处理完成。", context.sources.len()),
            });
        } else {
            events.push(OperationEvent::Completed {
                summary: format!(
                    "{} 个条目处理完成，{} 个失败。",
                    context.sources.len() - failed,
                    failed
                ),
            });
        }
        events
    }

    fn copy_entry(source: &str, target_dir: &str) -> std::io::Result<()> {
        let src = PathBuf::from(normalize_path(source));
        let name = file_name(source).ok_or_else(|| {
            std::io::Error::new(std::io::ErrorKind::InvalidInput, "无效源路径")
        })?;
        let dst = PathBuf::from(join_path(target_dir, &name));
        if dst.exists() {
            return Err(std::io::Error::new(
                std::io::ErrorKind::AlreadyExists,
                format!("目标已存在：{}", dst.display()),
            ));
        }
        if src.is_dir() {
            copy_dir_recursive(&src, &dst)
        } else {
            fs::copy(&src, &dst).map(|_| ())
        }
    }

    fn move_entry(source: &str, target_dir: &str) -> std::io::Result<()> {
        let src = PathBuf::from(normalize_path(source));
        let name = file_name(source).ok_or_else(|| {
            std::io::Error::new(std::io::ErrorKind::InvalidInput, "无效源路径")
        })?;
        let dst = PathBuf::from(join_path(target_dir, &name));
        if dst.exists() {
            return Err(std::io::Error::new(
                std::io::ErrorKind::AlreadyExists,
                format!("目标已存在：{}", dst.display()),
            ));
        }
        // 先尝试原子 rename（同文件系统）；失败则回退复制+删除
        match fs::rename(&src, &dst) {
            Ok(()) => Ok(()),
            Err(_) => {
                if src.is_dir() {
                    copy_dir_recursive(&src, &dst)?;
                } else {
                    fs::copy(&src, &dst)?;
                }
                fs::remove_dir_all(&src).or_else(|_| fs::remove_file(&src))
            }
        }
    }

    fn rename_entry(source: &str, target: &str) -> std::io::Result<()> {
        let src = PathBuf::from(normalize_path(source));
        let dst = PathBuf::from(normalize_path(target));
        if dst.exists() {
            return Err(std::io::Error::new(
                std::io::ErrorKind::AlreadyExists,
                format!("目标已存在：{}", dst.display()),
            ));
        }
        fs::rename(&src, &dst)
    }

    fn delete_entry(source: &str) -> std::io::Result<()> {
        let src = PathBuf::from(normalize_path(source));
        let metadata = fs::symlink_metadata(&src)?;
        if metadata.file_type().is_dir() {
            fs::remove_dir_all(&src)
        } else {
            fs::remove_file(&src)
        }
    }
}

/// 递归复制目录。
fn copy_dir_recursive(src: &Path, dst: &Path) -> std::io::Result<()> {
    fs::create_dir_all(dst)?;
    for entry in fs::read_dir(src)? {
        let entry = entry?;
        let file_type = entry.file_type()?;
        let from = entry.path();
        let to = dst.join(entry.file_name());
        if file_type.is_dir() {
            copy_dir_recursive(&from, &to)?;
        } else if file_type.is_symlink() {
            #[cfg(unix)]
            {
                let link = fs::read_link(&from)?;
                std::os::unix::fs::symlink(&link, &to)?;
            }
            #[cfg(not(unix))]
            {
                let _ = fs::copy(&from, &to)?;
            }
        } else {
            fs::copy(&from, &to)?;
        }
    }
    Ok(())
}
#[cfg(test)]
mod tests {
    use super::*;

    fn temp_dir(name: &str) -> PathBuf {
        let dir = std::env::temp_dir().join(format!("sunset_fm_{name}_{}", std::process::id()));
        let _ = fs::remove_dir_all(&dir);
        fs::create_dir_all(&dir).unwrap();
        dir
    }

    #[test]
    fn copies_file_to_target_dir() {
        let base = temp_dir("copy");
        let src = base.join("a.txt");
        fs::write(&src, "hello").unwrap();
        let dst_dir = base.join("out");
        fs::create_dir_all(&dst_dir).unwrap();

        let events = FileOperationRunner::run(
            OperationKind::Copy,
            &OperationContext::new(
                vec![src.to_string_lossy().into_owned()],
                Some(dst_dir.to_string_lossy().into_owned()),
            ),
            None,
        );
        assert!(events.iter().any(|e| matches!(e, OperationEvent::Completed { .. })));
        assert_eq!(fs::read_to_string(dst_dir.join("a.txt")).unwrap(), "hello");
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn moves_directory_recursively() {
        let base = temp_dir("move");
        let src = base.join("dir");
        fs::create_dir_all(src.join("sub")).unwrap();
        fs::write(src.join("sub/f.txt"), "x").unwrap();
        let dst_dir = base.join("dest");
        fs::create_dir_all(&dst_dir).unwrap();

        let events = FileOperationRunner::run(
            OperationKind::Move,
            &OperationContext::new(
                vec![src.to_string_lossy().into_owned()],
                Some(dst_dir.to_string_lossy().into_owned()),
            ),
            None,
        );
        assert!(events.iter().any(|e| matches!(e, OperationEvent::Completed { .. })));
        assert!(dst_dir.join("dir/sub/f.txt").exists());
        assert!(!src.exists());
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn rejects_copy_into_self() {
        let base = temp_dir("selfcopy");
        let src = base.join("dir");
        fs::create_dir_all(&src).unwrap();
        let validation = FileOperationRunner::validate(
            OperationKind::Copy,
            &OperationContext::new(
                vec![src.to_string_lossy().into_owned()],
                Some(src.join("inner").to_string_lossy().into_owned()),
            ),
        );
        assert!(matches!(validation, OperationValidation::Invalid(_)));
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn deletes_file() {
        let base = temp_dir("delete");
        let src = base.join("a.txt");
        fs::write(&src, "x").unwrap();
        let events = FileOperationRunner::run(
            OperationKind::Delete,
            &OperationContext::new(vec![src.to_string_lossy().into_owned()], None),
            None,
        );
        assert!(events.iter().any(|e| matches!(e, OperationEvent::Completed { .. })));
        assert!(!src.exists());
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn rename_requires_single_source() {
        let base = temp_dir("rename");
        let a = base.join("a.txt");
        let b = base.join("b.txt");
        fs::write(&a, "x").unwrap();
        let events = FileOperationRunner::run(
            OperationKind::Rename,
            &OperationContext::new(
                vec![a.to_string_lossy().into_owned()],
                Some(b.to_string_lossy().into_owned()),
            ),
            None,
        );
        assert!(events.iter().any(|e| matches!(e, OperationEvent::Completed { .. })));
        assert!(b.exists());
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn missing_source_fails() {
        let validation = FileOperationRunner::validate(
            OperationKind::Delete,
            &OperationContext::new(vec!["/nonexistent/path".into()], None),
        );
        assert!(matches!(validation, OperationValidation::Invalid(_)));
    }
}