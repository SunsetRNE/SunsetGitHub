//! Root（su）命令生成器：为 Kotlin 壳层生成经校验的 shell 命令。
//!
//! 对齐 Amaze `filesystem/root/` 命令集（Copy/Move/Rename/Delete/List/Mkdir/
//! Touch/Chmod/Find/Concatenate/Mount），并提供两处改进：
//! - 路径转义使用 POSIX 单引号规则（Amaze 用白名单字符过滤，会把路径中的
//!   特殊字符直接删掉，导致路径损坏）；这里只生成命令文本，不负责执行，
//!   执行由壳层走 topjohnwu/superuser 或 `su -c` 完成。
//! - 目录写入前自动包装 `mount -o rw,remount`，完成后恢复 `ro`（对齐
//!   `MountPathCommand` 语义）。

/// POSIX shell 单引号转义：`'` → `'\''`，其余字符原样。
///
/// 结果可直接嵌入 `su -c '...'` 或 `"..."` 中作为单引号字面量。
pub fn shell_quote(path: &str) -> String {
    format!("'{}'", path.replace('\'', "'\\''"))
}

/// 权限位 → `rwx` 风格字符串（如 `drwxr-xr-x`）。
pub fn permission_string(mode: u32, is_dir: bool) -> String {
    let mut s = String::with_capacity(10);
    s.push(if is_dir { 'd' } else { '-' });
    for shift in [6, 3, 0] {
        let bits = (mode >> shift) & 0b111;
        s.push(if bits & 0b100 != 0 { 'r' } else { '-' });
        s.push(if bits & 0b010 != 0 { 'w' } else { '-' });
        s.push(if bits & 0b001 != 0 { 'x' } else { '-' });
    }
    s
}

/// 权限字符串 → 八进制模式（如 `rwxr-xr-x` → 0o755）。
///
/// 首字符（类型位）会被跳过，仅解析后 9 位：每三位一组按八进制
/// 位权累加（owner: ×0o100，group: ×0o010，other: ×0o001）。
pub fn permission_from_string(perms: &str) -> Option<u32> {
    let chars: Vec<char> = perms.chars().collect();
    if chars.len() < 10 {
        return None;
    }
    let mut mode = 0u32;
    for (i, ch) in chars.iter().enumerate().skip(1).take(9) {
        let weight: u32 = match *ch {
            'r' => 4,
            'w' => 2,
            'x' | 's' | 't' => 1,
            _ => 0,
        };
        // i-1 ∈ 0..8；组位权 0o100 >> (3 * 组号)
        let group = (i - 1) / 3;
        let place: u32 = 0o100 >> (3 * group);
        mode += weight * place;
    }
    Some(mode)
}

/// Root 命令类型（对齐 Amaze root 命令集）。
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum RootCommand {
    /// `cp -r src dst`
    Copy { source: String, destination: String },
    /// `mv src dst`
    Move { source: String, destination: String },
    /// `mv old new`（同目录改名）
    Rename { old_path: String, new_path: String },
    /// `rm -rf path`
    Delete { path: String },
    /// `ls -la "path"`
    List { path: String, show_hidden: bool },
    /// `mkdir -p path`
    MakeDirectory { path: String },
    /// `touch path`
    MakeFile { path: String },
    /// `chmod [-R] <octal> "path"`
    Chmod { path: String, mode: u32, recursive: bool },
    /// `find "path"`（判断存在性）
    Find { path: String },
    /// `cat "a" "b" > "out"`（拼接）
    Concatenate { sources: Vec<String>, output: String },
    /// `mount -o rw,remount path`
    Mount { path: String, read_write: bool },
}

/// 生成的命令序列（可能含 mount 包装）。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct RootCommandPlan {
    /// 依次执行的命令（最后一条为恢复 ro 的 mount 时其失败可忽略）。
    pub commands: Vec<String>,
    /// 是否需要 root（所有命令都经由 su 执行，恒为 true，保留字段供 UI 判断）。
    pub requires_root: bool,
}

impl RootCommand {
    /// 目标目录写入前是否需要 remount rw（对齐 Amaze 写操作先 mount rw）。
    fn needs_remount(&self) -> bool {
        matches!(
            self,
            RootCommand::Copy { .. }
                | RootCommand::Move { .. }
                | RootCommand::Rename { .. }
                | RootCommand::Delete { .. }
                | RootCommand::MakeDirectory { .. }
                | RootCommand::MakeFile { .. }
                | RootCommand::Chmod { .. }
                | RootCommand::Concatenate { .. }
        )
    }

    /// 生成命令序列。
    pub fn build(&self) -> RootCommandPlan {
        let mut commands = Vec::new();
        let requires_root = true;

        // 写操作先尝试 remount rw（失败不阻塞，Amaze 同样容忍）
        if self.needs_remount() {
            if let Some(dir) = self.target_dir() {
                commands.push(format!(
                    "mount -o rw,remount {} 2>/dev/null || true",
                    shell_quote(&dir)
                ));
            }
        }

        commands.push(self.primary_command());

        // 写操作后恢复 ro（失败可忽略）
        if self.needs_remount() {
            if let Some(dir) = self.target_dir() {
                commands.push(format!(
                    "mount -o ro,remount {} 2>/dev/null || true",
                    shell_quote(&dir)
                ));
            }
        }

        RootCommandPlan {
            commands,
            requires_root,
        }
    }

    /// 主命令文本。
    pub fn primary_command(&self) -> String {
        match self {
            RootCommand::Copy { source, destination } => format!(
                "cp -r {} {}",
                shell_quote(source),
                shell_quote(destination)
            ),
            RootCommand::Move { source, destination } => format!(
                "mv {} {}",
                shell_quote(source),
                shell_quote(destination)
            ),
            RootCommand::Rename { old_path, new_path } => format!(
                "mv {} {}",
                shell_quote(old_path),
                shell_quote(new_path)
            ),
            RootCommand::Delete { path } => format!("rm -rf {}", shell_quote(path)),
            RootCommand::List { path, show_hidden } => {
                let flags = if *show_hidden { "-la" } else { "-l" };
                format!("ls {flags} {}", shell_quote(path))
            }
            RootCommand::MakeDirectory { path } => {
                format!("mkdir -p {}", shell_quote(path))
            }
            RootCommand::MakeFile { path } => format!("touch {}", shell_quote(path)),
            RootCommand::Chmod {
                path,
                mode,
                recursive,
            } => {
                let flag = if *recursive { "-R" } else { "" };
                format!("chmod {flag} {mode:o} {}", shell_quote(path))
            }
            RootCommand::Find { path } => format!("find {}", shell_quote(path)),
            RootCommand::Concatenate { sources, output } => {
                let parts: Vec<String> =
                    sources.iter().map(|s| shell_quote(s)).collect();
                format!("cat {} > {}", parts.join(" "), shell_quote(output))
            }
            RootCommand::Mount { path, read_write } => {
                let mode = if *read_write { "rw" } else { "ro" };
                format!("mount -o {mode},remount {}", shell_quote(path))
            }
        }
    }

    /// 写操作涉及的父目录（用于 remount 目标）。
    fn target_dir(&self) -> Option<String> {
        match self {
            RootCommand::Copy { destination, .. }
            | RootCommand::Move { destination, .. } => Some(destination.clone()),
            RootCommand::Rename { new_path, .. } => Some(new_path.clone()),
            RootCommand::Delete { path } | RootCommand::MakeFile { path } => {
                Some(parent_of(path))
            }
            RootCommand::MakeDirectory { path } => Some(path.clone()),
            RootCommand::Chmod { path, .. } => Some(parent_of(path)),
            RootCommand::Concatenate { output, .. } => Some(parent_of(output)),
            _ => None,
        }
    }
}

/// 取路径父目录（无父目录时返回路径本身，remount 容错）。
fn parent_of(path: &str) -> String {
    let trimmed = path.trim_end_matches('/');
    if trimmed.is_empty() {
        return "/".to_string();
    }
    match trimmed.rfind('/') {
        Some(0) => "/".to_string(),
        Some(idx) => trimmed[..idx].to_string(),
        None => trimmed.to_string(),
    }
}

/// `ls -l` 输出中的单条解析结果。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct RootLsEntry {
    /// 权限串（含类型位，如 `drwxr-xr-x`）。
    pub permissions: String,
    /// 链接数。
    pub links: u64,
    pub owner: String,
    pub group: String,
    /// 字节大小（目录为 0）。
    pub size: u64,
    /// `MMM dd HH:mm` 或 `MMM dd yyyy`。
    pub modified: String,
    pub name: String,
    pub is_dir: bool,
    pub is_symlink: bool,
}

/// 解析 `ls -la` 输出（对齐 Amaze `ListFilesCommand` 使用 `ls -l` 的模型）。
///
/// 兼容两种时间格式（当年：`Aug  6 10:30`；往年：`Aug  6 2025`），
/// 跳过 `total N` 行与 `.` / `..`。
pub fn parse_ls_output(output: &str) -> Vec<RootLsEntry> {
    let mut entries = Vec::new();
    for line in output.lines() {
        let line = line.trim_end_matches('\r');
        if line.is_empty() || line.starts_with("total ") {
            continue;
        }
        if let Some(entry) = parse_ls_line(line) {
            if entry.name != "." && entry.name != ".." {
                entries.push(entry);
            }
        }
    }
    entries
}

fn parse_ls_line(line: &str) -> Option<RootLsEntry> {
    let mut parts = line.split_whitespace();
    let permissions = parts.next()?.to_string();
    if permissions.len() < 10 || !permissions.starts_with(['-', 'd', 'l', 'c', 'b', 's']) {
        return None;
    }
    let links = parts.next()?.parse().ok()?;
    let owner = parts.next()?.to_string();
    let group = parts.next()?.to_string();
    let size = parts.next()?.parse().ok()?;
    let mon = parts.next()?;
    let day = parts.next()?;
    let time = parts.next()?;
    // 名字可能含空格，剩余部分全部拼接
    let mut name = parts.collect::<Vec<_>>().join(" ");
    if name.is_empty() {
        return None;
    }
    let is_dir = permissions.starts_with('d');
    let is_symlink = permissions.starts_with('l');
    // 符号链接行含 ` -> target`，仅保留链接名
    if is_symlink {
        if let Some(idx) = name.find(" -> ") {
            name.truncate(idx);
        }
    }
    Some(RootLsEntry {
        permissions,
        links,
        owner,
        group,
        size: if is_dir { 0 } else { size },
        modified: format!("{mon} {day} {time}"),
        name,
        is_dir,
        is_symlink,
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn quotes_shell_paths() {
        assert_eq!(shell_quote("/data/app"), "'/data/app'");
        assert_eq!(shell_quote("/a b/c"), "'/a b/c'");
        assert_eq!(shell_quote("/it's"), "'/it'\\''s'");
        assert_eq!(shell_quote(""), "''");
    }

    #[test]
    fn builds_primary_commands() {
        assert_eq!(
            RootCommand::Copy {
                source: "/sdcard/a.txt".into(),
                destination: "/data/x/".into(),
            }
            .primary_command(),
            "cp -r '/sdcard/a.txt' '/data/x/'"
        );
        assert_eq!(
            RootCommand::Delete { path: "/a b".into() }.primary_command(),
            "rm -rf '/a b'"
        );
        assert_eq!(
            RootCommand::Chmod {
                path: "/x".into(),
                mode: 0o644,
                recursive: false,
            }
            .primary_command(),
            "chmod  644 '/x'"
        );
        assert_eq!(
            RootCommand::Chmod {
                path: "/x".into(),
                mode: 0o755,
                recursive: true,
            }
            .primary_command(),
            "chmod -R 755 '/x'"
        );
        assert_eq!(
            RootCommand::List {
                path: "/".into(),
                show_hidden: true,
            }
            .primary_command(),
            "ls -la '/'"
        );
        assert_eq!(
            RootCommand::MakeDirectory { path: "/a/b".into() }.primary_command(),
            "mkdir -p '/a/b'"
        );
        assert_eq!(
            RootCommand::Mount {
                path: "/system".into(),
                read_write: true,
            }
            .primary_command(),
            "mount -o rw,remount '/system'"
        );
    }

    #[test]
    fn build_wraps_with_remount_for_writes() {
        let plan = RootCommand::Copy {
            source: "/sdcard/a".into(),
            destination: "/data/dst/".into(),
        }
        .build();
        // mount rw → cp → mount ro
        assert_eq!(plan.commands.len(), 3);
        assert!(plan.commands[0].starts_with("mount -o rw,remount '/data/dst/'"));
        assert_eq!(plan.commands[1], "cp -r '/sdcard/a' '/data/dst/'");
        assert!(plan.commands[2].starts_with("mount -o ro,remount '/data/dst/'"));
        assert!(plan.requires_root);
    }

    #[test]
    fn read_commands_have_no_remount() {
        let plan = RootCommand::List {
            path: "/".into(),
            show_hidden: false,
        }
        .build();
        assert_eq!(plan.commands.len(), 1);
        assert_eq!(plan.commands[0], "ls -l '/'");
    }

    #[test]
    fn permission_string_roundtrip() {
        assert_eq!(permission_string(0o755, true), "drwxr-xr-x");
        assert_eq!(permission_string(0o644, false), "-rw-r--r--");
        assert_eq!(permission_string(0o777, false), "-rwxrwxrwx");
        assert_eq!(permission_from_string("drwxr-xr-x"), Some(0o755));
        assert_eq!(permission_from_string("-rw-r--r--"), Some(0o644));
        assert_eq!(permission_from_string("-rwxrwxrwx"), Some(0o777));
        assert_eq!(permission_from_string("bad"), None);
    }

    #[test]
    fn parses_ls_output() {
        let out = "\
total 12
drwxr-xr-x 2 root root 4096 Aug  6 10:30 .
drwxr-xr-x 5 root root 4096 Aug  6 10:29 ..
-rw-r--r-- 1 root root  123 Aug  6 10:31 notes.txt
lrwxrwxrwx 1 root root    9 Aug  6 10:32 link -> target.txt
-rw-r--r-- 1 root root  456 Aug  6 2025 old.txt
";
        let entries = parse_ls_output(out);
        assert_eq!(entries.len(), 3);
        assert_eq!(entries[0].name, "notes.txt");
        assert_eq!(entries[0].size, 123);
        assert!(!entries[0].is_dir);
        assert_eq!(entries[0].modified, "Aug 6 10:31");
        assert_eq!(entries[1].name, "link");
        assert!(entries[1].is_symlink);
        assert_eq!(entries[2].name, "old.txt");
        assert_eq!(entries[2].modified, "Aug 6 2025");
    }

    #[test]
    fn parses_names_with_spaces() {
        let out = "-rw-r--r-- 1 root root 7 Aug  6 10:00 my file.txt\n";
        let entries = parse_ls_output(out);
        assert_eq!(entries.len(), 1);
        assert_eq!(entries[0].name, "my file.txt");
    }

    #[test]
    fn parent_of_handles_roots() {
        assert_eq!(parent_of("/a/b/c.txt"), "/a/b");
        assert_eq!(parent_of("/a"), "/");
        assert_eq!(parent_of("/"), "/");
        assert_eq!(parent_of("relative/path"), "relative");
    }
}