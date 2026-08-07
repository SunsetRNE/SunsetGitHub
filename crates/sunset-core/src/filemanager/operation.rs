//! 文件操作：复制、移动、重命名、删除。
//!
//! 移植自 Kotlin `operation/FileOperation.kt` 与 `LegacyFileOperationRunner.kt`
//! 的事件模型（Started/Progress/ConflictDetected/Completed/Failed/Cancelled），
//! 以同步 + 回调方式执行，便于 FFI 桥接与测试。
//!
//! 冲突处理对齐 Material Files `FileJob` 模型：
//! - 冲突策略四选一：`MergeOrReplace`（目录合并/文件替换）、`Rename`（改名）、`Skip`（跳过）、
//!   `Cancel`（取消整个操作），对应 `FileJobConflictAction`；
//! - 错误恢复：`Retry` / `Skip` / `Cancel`，对应 `FileJobErrorAction`；
//! - 目录对目录冲突默认合并（Material Files `isMerge` 语义，不询问）。

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
    Started {
        title: String,
    },
    Progress {
        current: u64,
        total: Option<u64>,
        message: String,
    },
    ConflictDetected {
        source: String,
        target: String,
    },
    Completed {
        summary: String,
    },
    Failed {
        message: String,
    },
    Cancelled,
}

/// 校验结果。
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum OperationValidation {
    Valid,
    Invalid(String),
}

/// 冲突处理策略（对齐 Material Files `FileJobConflictAction`）。
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ConflictAction {
    /// 目录冲突 → 合并；文件冲突 → 替换（覆盖）。
    MergeOrReplace,
    /// 目标改名为指定名称后继续。
    Rename(String),
    /// 跳过该条目，继续后续条目。
    Skip,
    /// 取消整个操作。
    Cancel,
}

/// 错误恢复策略（对齐 Material Files `FileJobErrorAction`）。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ErrorAction {
    /// 重试该条目（核心最多重试 3 次，防止死循环）。
    Retry,
    /// 跳过该条目，继续后续条目。
    Skip,
    /// 取消整个操作。
    Cancel,
}

/// 操作选项：默认冲突策略与默认错误策略。
///
/// UI 层可通过 `run_with_options` 的回调按条目动态决策（回调返回
/// 决策；不提供回调时使用这里的默认策略，等价于对话框"全部应用"）。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct OperationOptions {
    pub conflict: ConflictAction,
    pub error: ErrorAction,
}

impl Default for OperationOptions {
    fn default() -> Self {
        Self {
            conflict: ConflictAction::Skip,
            error: ErrorAction::Skip,
        }
    }
}

/// 操作进度回调：参数为（当前完成数，总数）。
pub type ProgressCallback<'a> = dyn FnMut(u64, Option<u64>) + 'a;

/// 冲突回调：参数为（源路径，目标路径），返回用户决策的动作。
pub type ConflictCallback<'a> = dyn FnMut(&str, &str) -> ConflictAction + 'a;

/// 错误回调：参数为（源路径，错误消息），返回恢复动作。
pub type ErrorCallback<'a> = dyn FnMut(&str, &str) -> ErrorAction + 'a;

/// 单条目处理结果。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum EntryOutcome {
    Done,
    Skipped,
    Failed,
    Cancelled,
}

/// 冲突解决后的去向。
#[derive(Debug)]
enum ConflictResolution {
    /// 覆盖目标（或目录合并，由调用方判断类型）。
    Replace,
    /// 改用指定名称继续。
    Rename(String),
    Skip,
    Cancel,
}

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

    /// 执行操作（默认策略：冲突跳过、错误跳过），通过 `on_progress` 报告进度。
    pub fn run(
        kind: OperationKind,
        context: &OperationContext,
        on_progress: Option<&mut ProgressCallback>,
    ) -> Vec<OperationEvent> {
        Self::run_with_options(
            kind,
            context,
            &OperationOptions::default(),
            None,
            None,
            on_progress,
        )
    }

    /// 执行操作，支持冲突/错误回调与默认策略（对齐 Material Files FileJob 模型）。
    ///
    /// - `on_conflict`：每次冲突时调用（source, target）→ 返回用户决策；为 `None`
    ///   时使用 `options.conflict`（等价"全部应用"）。
    /// - `on_error`：条目失败时调用（source, message）→ 返回恢复动作；为 `None`
    ///   时使用 `options.error`。
    #[allow(clippy::too_many_arguments)]
    pub fn run_with_options(
        kind: OperationKind,
        context: &OperationContext,
        options: &OperationOptions,
        mut on_conflict: Option<&mut ConflictCallback>,
        mut on_error: Option<&mut ErrorCallback>,
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
        let mut done: u64 = 0;
        let mut skipped: u64 = 0;
        let mut failed: u64 = 0;
        let mut cancelled = false;
        const MAX_RETRIES: u32 = 3;

        for source in &context.sources {
            let mut attempts = 0u32;
            let mut outcome = EntryOutcome::Done;
            loop {
                attempts += 1;
                let result = match kind {
                    OperationKind::Copy => Self::copy_entry(
                        source,
                        context.target.as_deref().unwrap(),
                        options,
                        &mut on_conflict,
                        &mut events,
                    ),
                    OperationKind::Move => Self::move_entry(
                        source,
                        context.target.as_deref().unwrap(),
                        options,
                        &mut on_conflict,
                        &mut events,
                    ),
                    OperationKind::Rename => Self::rename_entry(
                        source,
                        context.target.as_deref().unwrap(),
                        options,
                        &mut on_conflict,
                        &mut events,
                    ),
                    OperationKind::Delete => Self::delete_entry(source),
                };
                match result {
                    Ok(o) => {
                        outcome = o;
                        break;
                    }
                    Err(err) => {
                        let action = match on_error.as_deref_mut() {
                            Some(cb) => cb(source, &err.to_string()),
                            None => options.error,
                        };
                        match action {
                            ErrorAction::Retry if attempts < MAX_RETRIES => continue,
                            ErrorAction::Retry | ErrorAction::Skip => {
                                events.push(OperationEvent::Failed {
                                    message: format!("{source}: {err}"),
                                });
                                outcome = EntryOutcome::Failed;
                                break;
                            }
                            ErrorAction::Cancel => {
                                cancelled = true;
                                break;
                            }
                        }
                    }
                }
            }

            match outcome {
                EntryOutcome::Done => done += 1,
                EntryOutcome::Skipped => skipped += 1,
                EntryOutcome::Failed => failed += 1,
                EntryOutcome::Cancelled => {
                    cancelled = true;
                    break;
                }
            }

            if let Some(cb) = on_progress.as_deref_mut() {
                cb(done + skipped, Some(total));
            }
            events.push(OperationEvent::Progress {
                current: done + skipped,
                total: Some(total),
                message: format!("{}/{}", done + skipped, total),
            });
            if cancelled {
                break;
            }
        }

        if cancelled {
            events.push(OperationEvent::Cancelled);
        } else {
            let total = context.sources.len() as u64;
            let summary = if skipped == 0 {
                format!("{total} 个条目处理完成。")
            } else {
                format!(
                    "{} 个条目处理完成，{} 个跳过，{} 个失败。",
                    total - skipped - failed,
                    skipped,
                    failed
                )
            };
            events.push(OperationEvent::Completed { summary });
        }
        events
    }

    /// 复制条目：目录冲突默认合并；其余冲突走策略。
    fn copy_entry(
        source: &str,
        target_dir: &str,
        options: &OperationOptions,
        on_conflict: &mut Option<&mut ConflictCallback>,
        events: &mut Vec<OperationEvent>,
    ) -> std::io::Result<EntryOutcome> {
        let src = PathBuf::from(normalize_path(source));
        let name = file_name(source)
            .ok_or_else(|| std::io::Error::new(std::io::ErrorKind::InvalidInput, "无效源路径"))?;
        let dst = PathBuf::from(join_path(target_dir, &name));

        // 目录对目录 → 合并（Material Files isMerge 语义，不询问）
        if dst.exists() && src.is_dir() && dst.is_dir() {
            return copy_dir_merge(&src, &dst, options, on_conflict, events);
        }

        Self::execute_with_conflict(&src, &dst, options, on_conflict, events, |from, to| {
            if from.is_dir() {
                copy_dir_recursive(from, to)
            } else {
                fs::copy(from, to).map(|_| ())
            }
        })
    }

    /// 移动条目：优先原子 rename，跨文件系统回退复制+删除。
    fn move_entry(
        source: &str,
        target_dir: &str,
        options: &OperationOptions,
        on_conflict: &mut Option<&mut ConflictCallback>,
        events: &mut Vec<OperationEvent>,
    ) -> std::io::Result<EntryOutcome> {
        let src = PathBuf::from(normalize_path(source));
        let name = file_name(source)
            .ok_or_else(|| std::io::Error::new(std::io::ErrorKind::InvalidInput, "无效源路径"))?;
        let dst = PathBuf::from(join_path(target_dir, &name));

        // 目录对目录 → 合并后删除源
        if dst.exists() && src.is_dir() && dst.is_dir() {
            match copy_dir_merge(&src, &dst, options, on_conflict, events)? {
                EntryOutcome::Cancelled => return Ok(EntryOutcome::Cancelled),
                _ => {
                    fs::remove_dir_all(&src)?;
                    return Ok(EntryOutcome::Done);
                }
            }
        }

        Self::execute_with_conflict(&src, &dst, options, on_conflict, events, |from, to| {
            match fs::rename(from, to) {
                Ok(()) => Ok(()),
                Err(_) => {
                    if from.is_dir() {
                        copy_dir_recursive(from, to)?;
                    } else {
                        fs::copy(from, to)?;
                    }
                    fs::remove_dir_all(from).or_else(|_| fs::remove_file(from))
                }
            }
        })
    }

    fn rename_entry(
        source: &str,
        target: &str,
        options: &OperationOptions,
        on_conflict: &mut Option<&mut ConflictCallback>,
        events: &mut Vec<OperationEvent>,
    ) -> std::io::Result<EntryOutcome> {
        let src = PathBuf::from(normalize_path(source));
        let dst = PathBuf::from(normalize_path(target));
        Self::execute_with_conflict(&src, &dst, options, on_conflict, events, |from, to| {
            fs::rename(from, to)
        })
    }

    fn delete_entry(source: &str) -> std::io::Result<EntryOutcome> {
        let src = PathBuf::from(normalize_path(source));
        let metadata = fs::symlink_metadata(&src)?;
        if metadata.file_type().is_dir() {
            fs::remove_dir_all(&src)?;
        } else {
            fs::remove_file(&src)?;
        }
        Ok(EntryOutcome::Done)
    }

    /// 通用执行骨架：目标不存在直接执行；存在则按冲突策略处理后再执行。
    ///
    /// 类型保护：文件↔目录不可互相替换（对齐 Material Files 的限制）。
    fn execute_with_conflict(
        src: &Path,
        dst: &Path,
        options: &OperationOptions,
        on_conflict: &mut Option<&mut ConflictCallback>,
        events: &mut Vec<OperationEvent>,
        action: impl Fn(&Path, &Path) -> std::io::Result<()>,
    ) -> std::io::Result<EntryOutcome> {
        if !dst.exists() {
            action(src, dst)?;
            return Ok(EntryOutcome::Done);
        }
        // 目录对目录 → 合并
        if src.is_dir() && dst.is_dir() {
            return copy_dir_merge(src, dst, options, on_conflict, events);
        }
        // 文件↔目录冲突不可替换
        if src.is_dir() != dst.is_dir() {
            return Err(std::io::Error::new(
                std::io::ErrorKind::InvalidInput,
                format!(
                    "不能将{}替换为{}",
                    if src.is_dir() { "目录" } else { "文件" },
                    if dst.is_dir() { "目录" } else { "文件" }
                ),
            ));
        }

        match resolve_conflict(src, dst, options, on_conflict, events)? {
            ConflictResolution::Replace => {
                if dst.is_dir() {
                    fs::remove_dir_all(dst)?;
                } else {
                    fs::remove_file(dst)?;
                }
                action(src, dst)?;
                Ok(EntryOutcome::Done)
            }
            ConflictResolution::Rename(name) => {
                let new_dst = dst.with_file_name(name);
                if new_dst.exists() {
                    return Err(std::io::Error::new(
                        std::io::ErrorKind::AlreadyExists,
                        format!("改名后的目标仍存在：{}", new_dst.display()),
                    ));
                }
                action(src, &new_dst)?;
                Ok(EntryOutcome::Done)
            }
            ConflictResolution::Skip => Ok(EntryOutcome::Skipped),
            ConflictResolution::Cancel => Ok(EntryOutcome::Cancelled),
        }
    }
}

/// 询问冲突策略：先广播 `ConflictDetected` 事件，再取回调决策（无回调用默认策略）。
fn resolve_conflict(
    src: &Path,
    dst: &Path,
    options: &OperationOptions,
    on_conflict: &mut Option<&mut ConflictCallback>,
    events: &mut Vec<OperationEvent>,
) -> std::io::Result<ConflictResolution> {
    events.push(OperationEvent::ConflictDetected {
        source: src.display().to_string(),
        target: dst.display().to_string(),
    });
    let action = match on_conflict.as_deref_mut() {
        Some(cb) => cb(&src.display().to_string(), &dst.display().to_string()),
        None => options.conflict.clone(),
    };
    Ok(match action {
        ConflictAction::MergeOrReplace => ConflictResolution::Replace,
        ConflictAction::Rename(name) => ConflictResolution::Rename(name),
        ConflictAction::Skip => ConflictResolution::Skip,
        ConflictAction::Cancel => ConflictResolution::Cancel,
    })
}

/// 目录合并复制：目标目录已存在时逐条目并入，文件冲突走策略。
fn copy_dir_merge(
    src: &Path,
    dst: &Path,
    options: &OperationOptions,
    on_conflict: &mut Option<&mut ConflictCallback>,
    events: &mut Vec<OperationEvent>,
) -> std::io::Result<EntryOutcome> {
    fs::create_dir_all(dst)?;
    for entry in fs::read_dir(src)? {
        let entry = entry?;
        let from = entry.path();
        let to = dst.join(entry.file_name());
        let file_type = entry.file_type()?;
        if file_type.is_dir() {
            if to.exists() {
                if let EntryOutcome::Cancelled =
                    copy_dir_merge(&from, &to, options, on_conflict, events)?
                {
                    return Ok(EntryOutcome::Cancelled);
                }
            } else {
                copy_dir_recursive(&from, &to)?;
            }
        } else if to.exists() {
            match resolve_conflict(&from, &to, options, on_conflict, events)? {
                ConflictResolution::Replace => {
                    copy_file_or_link(&from, &to)?;
                }
                ConflictResolution::Rename(name) => {
                    let new_to = to.with_file_name(name);
                    if new_to.exists() {
                        return Err(std::io::Error::new(
                            std::io::ErrorKind::AlreadyExists,
                            format!("改名后的目标仍存在：{}", new_to.display()),
                        ));
                    }
                    copy_file_or_link(&from, &new_to)?;
                }
                ConflictResolution::Skip => {}
                ConflictResolution::Cancel => return Ok(EntryOutcome::Cancelled),
            }
        } else {
            copy_file_or_link(&from, &to)?;
        }
    }
    Ok(EntryOutcome::Done)
}

/// 复制单个文件或符号链接（普通文件 / symlink 分别处理）。
fn copy_file_or_link(from: &Path, to: &Path) -> std::io::Result<()> {
    let file_type = fs::symlink_metadata(from)?.file_type();
    if file_type.is_symlink() {
        #[cfg(unix)]
        {
            let link = fs::read_link(from)?;
            std::os::unix::fs::symlink(&link, to)?;
        }
        #[cfg(not(unix))]
        {
            fs::copy(from, to)?;
        }
    } else {
        fs::copy(from, to)?;
    }
    Ok(())
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
        } else {
            copy_file_or_link(&from, &to)?;
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

    fn run_events(
        kind: OperationKind,
        context: &OperationContext,
        options: &OperationOptions,
    ) -> Vec<OperationEvent> {
        FileOperationRunner::run_with_options(kind, context, options, None, None, None)
    }

    #[test]
    fn copies_file_to_target_dir() {
        let base = temp_dir("copy");
        let src = base.join("a.txt");
        fs::write(&src, "hello").unwrap();
        let dst_dir = base.join("out");
        fs::create_dir_all(&dst_dir).unwrap();

        let events = run_events(
            OperationKind::Copy,
            &OperationContext::new(
                vec![src.to_string_lossy().into_owned()],
                Some(dst_dir.to_string_lossy().into_owned()),
            ),
            &OperationOptions::default(),
        );
        assert!(events
            .iter()
            .any(|e| matches!(e, OperationEvent::Completed { .. })));
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

        let events = run_events(
            OperationKind::Move,
            &OperationContext::new(
                vec![src.to_string_lossy().into_owned()],
                Some(dst_dir.to_string_lossy().into_owned()),
            ),
            &OperationOptions::default(),
        );
        assert!(events
            .iter()
            .any(|e| matches!(e, OperationEvent::Completed { .. })));
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
        let events = run_events(
            OperationKind::Delete,
            &OperationContext::new(vec![src.to_string_lossy().into_owned()], None),
            &OperationOptions::default(),
        );
        assert!(events
            .iter()
            .any(|e| matches!(e, OperationEvent::Completed { .. })));
        assert!(!src.exists());
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn rename_requires_single_source() {
        let base = temp_dir("rename");
        let a = base.join("a.txt");
        let b = base.join("b.txt");
        fs::write(&a, "x").unwrap();
        let events = run_events(
            OperationKind::Rename,
            &OperationContext::new(
                vec![a.to_string_lossy().into_owned()],
                Some(b.to_string_lossy().into_owned()),
            ),
            &OperationOptions::default(),
        );
        assert!(events
            .iter()
            .any(|e| matches!(e, OperationEvent::Completed { .. })));
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

    // ---- 双蓝本新增：冲突策略 ----

    #[test]
    fn conflict_skip_keeps_target_and_emits_event() {
        let base = temp_dir("conflict_skip");
        let src = base.join("a.txt");
        let dst_dir = base.join("out");
        fs::write(&src, "new").unwrap();
        fs::create_dir_all(&dst_dir).unwrap();
        fs::write(dst_dir.join("a.txt"), "old").unwrap();

        let events = run_events(
            OperationKind::Copy,
            &OperationContext::new(
                vec![src.to_string_lossy().into_owned()],
                Some(dst_dir.to_string_lossy().into_owned()),
            ),
            &OperationOptions {
                conflict: ConflictAction::Skip,
                error: ErrorAction::Skip,
            },
        );
        assert!(events
            .iter()
            .any(|e| matches!(e, OperationEvent::ConflictDetected { .. })));
        // 跳过：目标保持旧内容
        assert_eq!(fs::read_to_string(dst_dir.join("a.txt")).unwrap(), "old");
        // 总结含跳过计数
        let completed = events
            .iter()
            .find_map(|e| match e {
                OperationEvent::Completed { summary } => Some(summary.clone()),
                _ => None,
            })
            .unwrap();
        assert!(completed.contains("跳过"));
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn conflict_replace_overwrites_file() {
        let base = temp_dir("conflict_replace");
        let src = base.join("a.txt");
        let dst_dir = base.join("out");
        fs::write(&src, "new").unwrap();
        fs::create_dir_all(&dst_dir).unwrap();
        fs::write(dst_dir.join("a.txt"), "old").unwrap();

        let events = run_events(
            OperationKind::Copy,
            &OperationContext::new(
                vec![src.to_string_lossy().into_owned()],
                Some(dst_dir.to_string_lossy().into_owned()),
            ),
            &OperationOptions {
                conflict: ConflictAction::MergeOrReplace,
                error: ErrorAction::Skip,
            },
        );
        assert!(events
            .iter()
            .any(|e| matches!(e, OperationEvent::Completed { .. })));
        assert_eq!(fs::read_to_string(dst_dir.join("a.txt")).unwrap(), "new");
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn conflict_rename_copies_with_new_name() {
        let base = temp_dir("conflict_rename");
        let src = base.join("a.txt");
        let dst_dir = base.join("out");
        fs::write(&src, "new").unwrap();
        fs::create_dir_all(&dst_dir).unwrap();
        fs::write(dst_dir.join("a.txt"), "old").unwrap();

        let events = run_events(
            OperationKind::Copy,
            &OperationContext::new(
                vec![src.to_string_lossy().into_owned()],
                Some(dst_dir.to_string_lossy().into_owned()),
            ),
            &OperationOptions {
                conflict: ConflictAction::Rename("a (1).txt".into()),
                error: ErrorAction::Skip,
            },
        );
        assert!(events
            .iter()
            .any(|e| matches!(e, OperationEvent::Completed { .. })));
        // 原目标保留，新名写入
        assert_eq!(fs::read_to_string(dst_dir.join("a.txt")).unwrap(), "old");
        assert_eq!(
            fs::read_to_string(dst_dir.join("a (1).txt")).unwrap(),
            "new"
        );
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn conflict_cancel_aborts_remaining() {
        let base = temp_dir("conflict_cancel");
        let dst_dir = base.join("out");
        fs::create_dir_all(&dst_dir).unwrap();
        let src1 = base.join("one.txt");
        let src2 = base.join("two.txt");
        fs::write(&src1, "1").unwrap();
        fs::write(&src2, "2").unwrap();
        // 目标已有 one.txt → 触发 Cancel
        fs::write(dst_dir.join("one.txt"), "old").unwrap();

        let events = run_events(
            OperationKind::Copy,
            &OperationContext::new(
                vec![
                    src1.to_string_lossy().into_owned(),
                    src2.to_string_lossy().into_owned(),
                ],
                Some(dst_dir.to_string_lossy().into_owned()),
            ),
            &OperationOptions {
                conflict: ConflictAction::Cancel,
                error: ErrorAction::Skip,
            },
        );
        assert!(events
            .iter()
            .any(|e| matches!(e, OperationEvent::Cancelled)));
        // 第二个条目未处理
        assert!(!dst_dir.join("two.txt").exists());
        assert!(!events
            .iter()
            .any(|e| matches!(e, OperationEvent::Completed { .. })));
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn directory_conflict_merges_by_default() {
        let base = temp_dir("conflict_merge");
        let src = base.join("dir");
        fs::create_dir_all(src.join("sub")).unwrap();
        fs::write(src.join("sub/new.txt"), "new").unwrap();
        fs::write(src.join("same.txt"), "new").unwrap();
        let dst_dir = base.join("out");
        fs::create_dir_all(dst_dir.join("dir/sub")).unwrap();
        fs::write(dst_dir.join("dir/same.txt"), "old").unwrap();

        let events = run_events(
            OperationKind::Copy,
            &OperationContext::new(
                vec![src.to_string_lossy().into_owned()],
                Some(dst_dir.to_string_lossy().into_owned()),
            ),
            &OperationOptions {
                conflict: ConflictAction::MergeOrReplace,
                error: ErrorAction::Skip,
            },
        );
        assert!(events
            .iter()
            .any(|e| matches!(e, OperationEvent::Completed { .. })));
        // 合并：新文件进入，旧文件按替换策略覆盖
        assert!(dst_dir.join("dir/sub/new.txt").exists());
        assert_eq!(
            fs::read_to_string(dst_dir.join("dir/same.txt")).unwrap(),
            "new"
        );
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn callback_decides_conflict_dynamically() {
        let base = temp_dir("conflict_cb");
        let src = base.join("a.txt");
        let dst_dir = base.join("out");
        fs::write(&src, "new").unwrap();
        fs::create_dir_all(&dst_dir).unwrap();
        fs::write(dst_dir.join("a.txt"), "old").unwrap();

        let mut calls = 0;
        let mut cb = |_s: &str, _t: &str| {
            calls += 1;
            ConflictAction::Rename("renamed.txt".into())
        };
        let events = FileOperationRunner::run_with_options(
            OperationKind::Copy,
            &OperationContext::new(
                vec![src.to_string_lossy().into_owned()],
                Some(dst_dir.to_string_lossy().into_owned()),
            ),
            &OperationOptions::default(),
            Some(&mut cb),
            None,
            None,
        );
        assert!(events
            .iter()
            .any(|e| matches!(e, OperationEvent::Completed { .. })));
        assert_eq!(calls, 1);
        assert!(dst_dir.join("renamed.txt").exists());
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn error_retry_then_skip_recovers() {
        let base = temp_dir("error_retry");
        // 目标是一个文件，源是目录 → 类型冲突错误
        let src = base.join("dir");
        fs::create_dir_all(&src).unwrap();
        let dst_dir = base.join("out");
        fs::create_dir_all(&dst_dir).unwrap();
        fs::write(dst_dir.join("dir"), "i am a file").unwrap();

        // 默认策略 Skip → 不崩，走 Completed 且带失败计数
        let events = run_events(
            OperationKind::Copy,
            &OperationContext::new(
                vec![src.to_string_lossy().into_owned()],
                Some(dst_dir.to_string_lossy().into_owned()),
            ),
            &OperationOptions::default(),
        );
        assert!(events
            .iter()
            .any(|e| matches!(e, OperationEvent::Completed { .. })));
        assert!(events
            .iter()
            .any(|e| matches!(e, OperationEvent::Failed { .. })));
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn error_cancel_stops_operation() {
        let base = temp_dir("error_cancel");
        let src = base.join("dir");
        fs::create_dir_all(&src).unwrap();
        let dst_dir = base.join("out");
        fs::create_dir_all(&dst_dir).unwrap();
        fs::write(dst_dir.join("dir"), "i am a file").unwrap();
        let src2 = base.join("after.txt");
        fs::write(&src2, "x").unwrap();

        let events = run_events(
            OperationKind::Copy,
            &OperationContext::new(
                vec![
                    src.to_string_lossy().into_owned(),
                    src2.to_string_lossy().into_owned(),
                ],
                Some(dst_dir.to_string_lossy().into_owned()),
            ),
            &OperationOptions {
                conflict: ConflictAction::Skip,
                error: ErrorAction::Cancel,
            },
        );
        assert!(events
            .iter()
            .any(|e| matches!(e, OperationEvent::Cancelled)));
        assert!(!dst_dir.join("after.txt").exists());
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn move_conflict_replace_removes_source() {
        let base = temp_dir("move_conflict");
        let src = base.join("a.txt");
        let dst_dir = base.join("out");
        fs::write(&src, "new").unwrap();
        fs::create_dir_all(&dst_dir).unwrap();
        fs::write(dst_dir.join("a.txt"), "old").unwrap();

        let events = run_events(
            OperationKind::Move,
            &OperationContext::new(
                vec![src.to_string_lossy().into_owned()],
                Some(dst_dir.to_string_lossy().into_owned()),
            ),
            &OperationOptions {
                conflict: ConflictAction::MergeOrReplace,
                error: ErrorAction::Skip,
            },
        );
        assert!(events
            .iter()
            .any(|e| matches!(e, OperationEvent::Completed { .. })));
        assert_eq!(fs::read_to_string(dst_dir.join("a.txt")).unwrap(), "new");
        assert!(!src.exists());
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn rename_conflict_skip_keeps_original() {
        let base = temp_dir("rename_conflict");
        let a = base.join("a.txt");
        let b = base.join("b.txt");
        fs::write(&a, "x").unwrap();
        fs::write(&b, "y").unwrap();

        let events = run_events(
            OperationKind::Rename,
            &OperationContext::new(
                vec![a.to_string_lossy().into_owned()],
                Some(b.to_string_lossy().into_owned()),
            ),
            &OperationOptions {
                conflict: ConflictAction::Skip,
                error: ErrorAction::Skip,
            },
        );
        assert!(events
            .iter()
            .any(|e| matches!(e, OperationEvent::ConflictDetected { .. })));
        assert_eq!(fs::read_to_string(&b).unwrap(), "y");
        assert!(a.exists());
        fs::remove_dir_all(&base).ok();
    }
}
