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
        return Err(Error::InvalidData(format!(
            "unsafe archive path: {name}"
        )));
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
}