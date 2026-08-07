//! 压缩包处理：ZIP 浏览/解压/打包、tar.gz 解压。
//!
//! 基于 zip / tar / flate2（均 MIT/Apache-2.0），替代 Apache Commons
//! Compress 职责。

use std::fs::File;
use std::io::Read;
use std::path::{Path, PathBuf};

use crate::error::{Error, Result};

/// ZIP 内条目。
#[derive(Debug, Clone)]
pub struct ArchiveEntry {
    pub name: String,
    pub is_dir: bool,
    pub size: u64,
    pub compressed_size: u64,
}

/// 压缩包内浏览条目（对齐 Amaze `CompressedObjectParcelable`）。
///
/// - `name`：展示名（如 `sub` / `notes.md`，不含路径前缀）；
/// - `path`：压缩包内相对路径，目录以 `/` 结尾（对齐 Amaze 约定）；
/// - `modified`：Unix 秒（ZIP 本地时间）。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ArchiveDirEntry {
    pub name: String,
    pub path: String,
    pub is_dir: bool,
    pub size: u64,
    pub modified: Option<u64>,
}

/// 支持的压缩包格式。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ArchiveFormat {
    Zip,
    TarGz,
}

/// 按扩展名检测压缩包格式。
pub fn detect_archive(path: &Path) -> Option<ArchiveFormat> {
    let name = path.file_name()?.to_string_lossy().to_ascii_lowercase();
    if name.ends_with(".zip") || name.ends_with(".jar") || name.ends_with(".apk") {
        Some(ArchiveFormat::Zip)
    } else if name.ends_with(".tar.gz") || name.ends_with(".tgz") {
        Some(ArchiveFormat::TarGz)
    } else {
        None
    }
}

/// 列出压缩包内 `rel_dir` 层的条目（对齐 Amaze `changePath` 模型）。
///
/// - `rel_dir` 为空或 `/` 表示压缩包根层；
/// - 目录由路径首段合成并去重（zip 可能无显式目录条目）；
/// - 返回的 `path` 可直接作为下一次 `rel_dir` 传入继续下钻。
pub fn list_archive_dir(path: &Path, rel_dir: &str) -> Result<Vec<ArchiveDirEntry>> {
    match detect_archive(path) {
        Some(ArchiveFormat::Zip) => list_zip_dir(path, rel_dir),
        Some(ArchiveFormat::TarGz) => list_tar_gz_dir(path, rel_dir),
        None => Err(Error::InvalidData(format!(
            "不支持的压缩包格式：{}",
            path.display()
        ))),
    }
}

/// 压缩包内浏览：从全量条目过滤出 `rel_dir` 的直接子项。
///
/// `raw` 为 (路径, 是否目录, 大小, 修改时间) 序列。
fn filter_dir_entries(
    raw: Vec<(String, bool, u64, Option<u64>)>,
    rel_dir: &str,
) -> Result<Vec<ArchiveDirEntry>> {
    let prefix = rel_dir.trim_matches('/');
    let mut seen: Vec<String> = Vec::new();
    let mut result: Vec<ArchiveDirEntry> = Vec::new();

    for (full, is_dir, size, modified) in raw {
        let full = full.trim_start_matches('/');
        if full.is_empty() {
            continue;
        }
        // 是否位于当前层：要么顶层，要么以 prefix/ 开头
        let remainder = if prefix.is_empty() {
            full.to_string()
        } else if let Some(rest) = full.strip_prefix(&format!("{prefix}/")) {
            rest.to_string()
        } else {
            continue;
        };
        if remainder.is_empty() {
            continue;
        }
        // 首段 = 直接子项
        let (first, deeper) = match remainder.split_once('/') {
            Some((head, tail)) => (head.to_string(), !tail.is_empty()),
            None => (remainder.clone(), false),
        };
        let is_dir = is_dir || deeper;
        let key = if is_dir {
            format!("{first}/")
        } else {
            first.clone()
        };
        if seen.contains(&key) {
            continue;
        }
        seen.push(key.clone());
        result.push(ArchiveDirEntry {
            name: first,
            path: key,
            is_dir,
            size: if is_dir { 0 } else { size },
            modified,
        });
    }
    Ok(result)
}

/// 列出 ZIP 内 `rel_dir` 层的条目。
fn list_zip_dir(path: &Path, rel_dir: &str) -> Result<Vec<ArchiveDirEntry>> {
    let file = File::open(path)?;
    let mut archive = zip::ZipArchive::new(file)?;

    let mut raw = Vec::with_capacity(archive.len());
    for i in 0..archive.len() {
        let entry = archive.by_index(i)?;
        let modified = entry.last_modified().map(|dt| {
            msdos_to_unix(
                dt.year(),
                dt.month(),
                dt.day(),
                dt.hour(),
                dt.minute(),
                dt.second(),
            )
        });
        raw.push((
            entry.name().to_string(),
            entry.is_dir(),
            entry.size(),
            modified,
        ));
    }
    filter_dir_entries(raw, rel_dir)
}

/// 列出 tar.gz 内 `rel_dir` 层的条目（仅读头部，不解压内容）。
fn list_tar_gz_dir(path: &Path, rel_dir: &str) -> Result<Vec<ArchiveDirEntry>> {
    let file = File::open(path)?;
    let gz = flate2::read::GzDecoder::new(file);
    let mut archive = tar::Archive::new(gz);

    let mut raw = Vec::new();
    for entry in archive.entries()? {
        let entry = entry?;
        let entry_path = entry.path()?.to_string_lossy().into_owned();
        let is_dir = entry.header().entry_type().is_dir();
        let size = entry.header().size().unwrap_or(0);
        let modified = entry.header().mtime().ok();
        raw.push((entry_path, is_dir, size, modified));
    }
    filter_dir_entries(raw, rel_dir)
}

/// 将 ZIP MS-DOS 时间转换为 Unix 秒（本地时间，与 Android zipfile 一致）。
fn msdos_to_unix(year: u16, month: u8, day: u8, hour: u8, minute: u8, second: u8) -> u64 {
    let days = days_from_civil(year as i64, month as i64, day as i64);
    (days * 86400 + hour as i64 * 3600 + minute as i64 * 60 + second as i64) as u64
}

/// 公历日期 → 自 1970-01-01 的天数（Howard Hinnant 算法）。
fn days_from_civil(y: i64, m: i64, d: i64) -> i64 {
    let y = if m <= 2 { y - 1 } else { y };
    let era = if y >= 0 { y } else { y - 399 } / 400;
    let yoe = y - era * 400; // [0, 399]
    let mp = (m + 9) % 12; // [0, 11]
    let doy = (153 * mp + 2) / 5 + d - 1; // [0, 365]
    let doe = yoe * 365 + yoe / 4 - yoe / 100 + doy; // [0, 146096]
    era * 146097 + doe - 719468
}

/// 列出 ZIP 内容。
pub fn list_zip(path: &Path) -> Result<Vec<ArchiveEntry>> {
    let file = File::open(path)?;
    let mut archive = zip::ZipArchive::new(file)?;

    let mut entries = Vec::with_capacity(archive.len());
    for i in 0..archive.len() {
        let entry = archive.by_index(i)?;
        entries.push(ArchiveEntry {
            name: entry.name().to_string(),
            is_dir: entry.is_dir(),
            size: entry.size(),
            compressed_size: entry.compressed_size(),
        });
    }
    Ok(entries)
}

/// 读取 ZIP 内单个文件的原始字节。
pub fn read_zip_file(path: &Path, entry_name: &str) -> Result<Vec<u8>> {
    let file = File::open(path)?;
    let mut archive = zip::ZipArchive::new(file)?;
    let mut entry = archive.by_name(entry_name)?;
    let mut buf = Vec::with_capacity(entry.size() as usize);
    entry.read_to_end(&mut buf)?;
    Ok(buf)
}

/// 解压 ZIP 到目标目录（覆盖同名文件）。
pub fn extract_zip(path: &Path, dest: &Path) -> Result<usize> {
    let file = File::open(path)?;
    let mut archive = zip::ZipArchive::new(file)?;
    let mut count = 0usize;

    for i in 0..archive.len() {
        let mut entry = archive.by_index(i)?;
        let entry_path = sanitize_join(dest, entry.name())?;

        if entry.is_dir() {
            std::fs::create_dir_all(&entry_path)?;
        } else {
            if let Some(parent) = entry_path.parent() {
                std::fs::create_dir_all(parent)?;
            }
            let mut out = File::create(&entry_path)?;
            std::io::copy(&mut entry, &mut out)?;
            count += 1;
        }
    }
    Ok(count)
}

/// 打包目录为 ZIP（保持相对路径）。
pub fn zip_directory(source: &Path, output: &Path) -> Result<usize> {
    let file = File::create(output)?;
    let mut zip = zip::ZipWriter::new(file);
    let options = zip::write::SimpleFileOptions::default()
        .compression_method(zip::CompressionMethod::Deflated);

    let mut count = 0usize;
    walk_dir(source, source, &mut zip, &options, &mut count)?;
    zip.finish()?;
    Ok(count)
}

fn walk_dir(
    root: &Path,
    dir: &Path,
    zip: &mut zip::ZipWriter<File>,
    options: &zip::write::SimpleFileOptions,
    count: &mut usize,
) -> Result<()> {
    for entry in std::fs::read_dir(dir)? {
        let entry = entry?;
        let path = entry.path();
        let relative = path.strip_prefix(root).unwrap_or(&path);
        let name = relative.to_string_lossy().replace('\\', "/");

        if path.is_dir() {
            zip.add_directory(format!("{name}/"), *options)?;
            walk_dir(root, &path, zip, options, count)?;
        } else {
            let mut f = File::open(&path)?;
            zip.start_file(name, *options)?;
            std::io::copy(&mut f, zip)?;
            *count += 1;
        }
    }
    Ok(())
}

/// 解压 tar.gz。
pub fn extract_tar_gz(path: &Path, dest: &Path) -> Result<usize> {
    let file = File::open(path)?;
    let gz = flate2::read::GzDecoder::new(file);
    let mut archive = tar::Archive::new(gz);
    let mut count = 0usize;
    for entry in archive.entries()? {
        let mut entry = entry?;
        let entry_path = sanitize_join(dest, entry.path()?.to_string_lossy().as_ref())?;
        if entry.header().entry_type().is_dir() {
            std::fs::create_dir_all(&entry_path)?;
        } else {
            if let Some(parent) = entry_path.parent() {
                std::fs::create_dir_all(parent)?;
            }
            entry.unpack(&entry_path)?;
            count += 1;
        }
    }
    Ok(count)
}

/// 防止 zip-slip：目标路径必须位于 dest 之下。
///
/// 使用词法级路径规范化（不依赖文件系统状态），
/// 对 `..` 组件做折叠后再做前缀校验。
fn sanitize_join(dest: &Path, name: &str) -> Result<PathBuf> {
    let candidate = normalize_path(&dest.join(name));
    let dest_norm = normalize_path(dest);
    if !candidate.starts_with(&dest_norm) {
        return Err(Error::InvalidData(format!("unsafe archive path: {name}")));
    }
    Ok(candidate)
}

/// 词法级路径规范化：折叠 `.` 与 `..` 组件。
fn normalize_path(path: &Path) -> PathBuf {
    use std::path::Component;

    let mut result = PathBuf::new();
    for component in path.components() {
        match component {
            Component::CurDir => {}
            Component::ParentDir => {
                if !result.pop() {
                    // 已到根，忽略多余的 ..
                }
            }
            other => result.push(other.as_os_str()),
        }
    }
    result
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs;
    use std::io::Write;

    fn temp_dir(name: &str) -> PathBuf {
        let dir = std::env::temp_dir().join(format!("sunset_arc_{name}_{}", std::process::id()));
        let _ = std::fs::remove_dir_all(&dir);
        std::fs::create_dir_all(&dir).unwrap();
        dir
    }

    #[test]
    fn sanitize_rejects_escape() {
        let dest = Path::new("/tmp/out");
        let result = sanitize_join(dest, "../../etc/passwd");
        assert!(result.is_err());
    }

    #[test]
    fn sanitize_accepts_normal() {
        let dest = Path::new("/tmp/out");
        let result = sanitize_join(dest, "sub/file.txt");
        assert!(result.is_ok());
        assert_eq!(result.unwrap(), PathBuf::from("/tmp/out/sub/file.txt"));
    }

    // ---- 双蓝本新增：压缩包内浏览（Amaze showcontents 模型） ----

    #[test]
    fn detects_archive_format() {
        assert_eq!(
            detect_archive(Path::new("/a/b.zip")),
            Some(ArchiveFormat::Zip)
        );
        assert_eq!(
            detect_archive(Path::new("/a/app.apk")),
            Some(ArchiveFormat::Zip)
        );
        assert_eq!(
            detect_archive(Path::new("/a/lib.jar")),
            Some(ArchiveFormat::Zip)
        );
        assert_eq!(
            detect_archive(Path::new("/a/b.tar.gz")),
            Some(ArchiveFormat::TarGz)
        );
        assert_eq!(
            detect_archive(Path::new("/a/b.tgz")),
            Some(ArchiveFormat::TarGz)
        );
        assert_eq!(detect_archive(Path::new("/a/b.txt")), None);
    }

    #[test]
    fn browses_zip_root_with_synthesized_dirs() {
        let base = temp_dir("browse");
        let zip_path = base.join("test.zip");
        let file = File::create(&zip_path).unwrap();
        let mut zip = zip::ZipWriter::new(file);
        let options = zip::write::SimpleFileOptions::default();
        zip.start_file("readme.md", options).unwrap();
        zip.write_all(b"# hi").unwrap();
        zip.start_file("sub/notes.txt", options).unwrap();
        zip.write_all(b"notes").unwrap();
        zip.start_file("sub/deep/data.bin", options).unwrap();
        zip.write_all(b"\x00\x01").unwrap();
        zip.start_file("top.txt", options).unwrap();
        zip.write_all(b"top").unwrap();
        zip.finish().unwrap();

        // 根层：readme.md、sub/（合成目录）、top.txt
        let entries = list_archive_dir(&zip_path, "").unwrap();
        let names: Vec<(&str, bool)> = entries
            .iter()
            .map(|e| (e.name.as_str(), e.is_dir))
            .collect();
        assert!(names.contains(&("readme.md", false)));
        assert!(names.contains(&("sub", true)));
        assert!(names.contains(&("top.txt", false)));
        // 合成目录 path 以 / 结尾
        let sub = entries.iter().find(|e| e.name == "sub").unwrap();
        assert_eq!(sub.path, "sub/");
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn browses_zip_subdirectory() {
        let base = temp_dir("browse_sub");
        let zip_path = base.join("test.zip");
        let file = File::create(&zip_path).unwrap();
        let mut zip = zip::ZipWriter::new(file);
        let options = zip::write::SimpleFileOptions::default();
        zip.start_file("sub/notes.txt", options).unwrap();
        zip.write_all(b"notes").unwrap();
        zip.start_file("sub/deep/data.bin", options).unwrap();
        zip.write_all(b"\x00\x01").unwrap();
        zip.start_file("other/x.txt", options).unwrap();
        zip.write_all(b"x").unwrap();
        zip.finish().unwrap();

        // 下钻 sub/：notes.txt、deep/
        let entries = list_archive_dir(&zip_path, "sub/").unwrap();
        let names: Vec<(&str, bool)> = entries
            .iter()
            .map(|e| (e.name.as_str(), e.is_dir))
            .collect();
        assert!(names.contains(&("notes.txt", false)));
        assert!(names.contains(&("deep", true)));
        // other/ 不在 sub 下，不出现
        assert!(!names.iter().any(|(n, _)| *n == "other"));
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn browses_tar_gz() {
        let base = temp_dir("browse_tgz");
        let tgz_path = base.join("test.tar.gz");
        let file = File::create(&tgz_path).unwrap();
        let enc = flate2::write::GzEncoder::new(file, flate2::Compression::default());
        let mut tar = tar::Builder::new(enc);
        let mut header = tar::Header::new_gnu();
        header.set_size(4);
        header.set_mode(0o644);
        header.set_cksum();
        tar.append_data(&mut header, "a/b.txt", std::io::Cursor::new(b"data"))
            .unwrap();
        let mut dir_header = tar::Header::new_gnu();
        dir_header.set_entry_type(tar::EntryType::Directory);
        dir_header.set_size(0);
        dir_header.set_mode(0o755);
        dir_header.set_cksum();
        tar.append_data(&mut dir_header, "a/", std::io::empty())
            .unwrap();
        tar.into_inner().unwrap().finish().unwrap();

        let entries = list_archive_dir(&tgz_path, "").unwrap();
        assert_eq!(entries.len(), 1);
        assert_eq!(entries[0].name, "a");
        assert!(entries[0].is_dir);
        assert_eq!(entries[0].path, "a/");

        let sub = list_archive_dir(&tgz_path, "a/").unwrap();
        assert_eq!(sub.len(), 1);
        assert_eq!(sub[0].name, "b.txt");
        assert!(!sub[0].is_dir);
        assert_eq!(sub[0].size, 4);
        fs::remove_dir_all(&base).ok();
    }

    #[test]
    fn msdos_date_conversion() {
        // 2020-01-02 03:04:05 → Unix 秒
        let secs = msdos_to_unix(2020, 1, 2, 3, 4, 5);
        assert_eq!(secs, 1577934245);
        // 1970-01-01 00:00:00 → 0
        assert_eq!(msdos_to_unix(1970, 1, 1, 0, 0, 0), 0);
        // 2000-03-01 12:00:00 → 951912000
        assert_eq!(msdos_to_unix(2000, 3, 1, 12, 0, 0), 951912000);
    }

    #[test]
    fn unsupported_format_errors() {
        let base = temp_dir("browse_err");
        let plain = base.join("notes.txt");
        std::fs::write(&plain, "x").unwrap();
        let result = list_archive_dir(&plain, "");
        assert!(result.is_err());
        fs::remove_dir_all(&base).ok();
    }
}
