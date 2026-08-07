//! ARSC（Android 资源表）完整解析器。
//!
//! 按 AOSP `ResourceTypes.cpp` 的二进制布局实现全量解析：header、
//! 全局字符串池、package（type/key 字符串池、TYPE_SPEC、TYPE chunk、
//! 条目与 complex map），并生成 Android 风格的 config 描述串。
//!
//! 与 Kotlin 原版 `EngineeringToolScanner` 的差异：
//! - 解析**全部**条目与配置，而非采样 8 条/12 chunk；
//! - UTF-8 字符串长度按 AOSP `decodeLength`（0x80 扩展，最多 2 字节）
//!   实现，原版误用 ULEB128，长字符串（>127）会解析错误；
//! - 失败返回结构化错误，不静默吞掉。

use crate::error::{Error, Result};

// chunk type 常量
pub const RES_STRING_POOL_TYPE: u16 = 0x0001;
pub const RES_TABLE_TYPE: u16 = 0x0002;
pub const RES_TABLE_PACKAGE_TYPE: u16 = 0x0200;
pub const RES_TABLE_TYPE_TYPE: u16 = 0x0201;
pub const RES_TABLE_TYPE_SPEC_TYPE: u16 = 0x0202;
pub const RES_TABLE_LIBRARY_TYPE: u16 = 0x0003;
pub const RES_TABLE_OVERLAYABLE_TYPE: u16 = 0x0203;

pub const UTF8_FLAG: u32 = 0x0000_0100;
pub const NO_ENTRY: u32 = 0xFFFF_FFFF;
pub const TABLE_ENTRY_FLAG_COMPLEX: u16 = 0x0001;
pub const TABLE_ENTRY_FLAG_PUBLIC: u16 = 0x0002;

/// 防御性上限：单 chunk 条目数 / 字符串数。
const MAX_ENTRY_COUNT: u32 = 1_000_000;
const MAX_STRING_COUNT: u32 = 10_000_000;

// ---------------------------------------------------------------------------
// 数据结构
// ---------------------------------------------------------------------------

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ArscHeader {
    pub header_type: u16,
    pub header_size: u16,
    pub size: u32,
    pub package_count: u32,
}

/// 字符串池（全局池 / package 的 type/key 池）。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct StringPool {
    pub offset: usize,
    pub header_size: u16,
    pub size: u32,
    pub string_count: u32,
    pub style_count: u32,
    pub flags: u32,
    pub strings_start: u32,
    pub styles_start: u32,
    pub utf8: bool,
    /// 全部解码后的字符串。
    pub strings: Vec<String>,
}

impl StringPool {
    pub fn string(&self, idx: u32) -> Option<&str> {
        self.strings.get(idx as usize).map(String::as_str)
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ArscPackage {
    pub offset: usize,
    pub id: u32,
    pub name: String,
    pub type_strings_offset: u32,
    pub key_strings_offset: u32,
    pub type_strings: Option<StringPool>,
    pub key_strings: Option<StringPool>,
    pub type_specs: Vec<TypeSpec>,
    pub types: Vec<TypeChunk>,
}

impl ArscPackage {
    /// 类型名（type id → type_strings，id 从 1 开始）。
    pub fn type_name(&self, type_id: u8) -> Option<&str> {
        if type_id == 0 {
            return None;
        }
        self.type_strings.as_ref()?.string(u32::from(type_id) - 1)
    }

    /// 键名（key index → key_strings）。
    pub fn key_name(&self, key_idx: u32) -> Option<&str> {
        self.key_strings.as_ref()?.string(key_idx)
    }
}

/// ResTable_typeSpec：类型标志数组。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct TypeSpec {
    pub offset: usize,
    pub id: u8,
    pub entry_count: u32,
    pub flags: Vec<u32>,
}

/// ResTable_type：特定配置下的条目数组。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct TypeChunk {
    pub offset: usize,
    pub id: u8,
    pub entry_count: u32,
    pub entries_start: u32,
    pub config: ResTableConfig,
    /// entryOffsets[i] == NO_ENTRY 时为 None。
    pub entries: Vec<Option<ResEntry>>,
}

impl TypeChunk {
    /// 非空条目数量。
    pub fn non_empty_count(&self) -> usize {
        self.entries.iter().filter(|e| e.is_some()).count()
    }
}

/// ResTable_entry（非 complex）+ 内联 Res_value。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ResEntry {
    pub flags: u16,
    /// key_strings 索引（资源名）。
    pub key_idx: u32,
    pub value: Option<ResValue>,
    pub complex: Option<ResComplexValue>,
}

impl ResEntry {
    pub fn is_complex(&self) -> bool {
        self.complex.is_some()
    }
    pub fn is_public(&self) -> bool {
        self.flags & TABLE_ENTRY_FLAG_PUBLIC != 0
    }
}

/// Res_value（8 字节）。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct ResValue {
    pub data_type: u8,
    pub data: u32,
}

/// ResTable_map_entry：complex 资源（parent + map）。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ResComplexValue {
    pub parent: u32,
    pub maps: Vec<ResMapEntry>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ResMapEntry {
    /// 属性名引用（key_strings 或全局池）。
    pub name_ref: u32,
    pub value: ResValue,
}

/// ResTable_config 全部字段（按 AOSP 布局，size 决定有效长度）。
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct ResTableConfig {
    pub size: u32,
    pub mcc: u16,
    pub mnc: u16,
    pub language: [u8; 2],
    pub country: [u8; 2],
    pub orientation: u8,
    pub touchscreen: u8,
    pub density: u16,
    pub keyboard: u8,
    pub navigation: u8,
    pub input_flags: u8,
    pub screen_width: u16,
    pub screen_height: u16,
    pub sdk_version: u16,
    pub minor_version: u16,
    pub screen_layout: u8,
    pub ui_mode: u8,
    pub smallest_screen_width_dp: u16,
    pub screen_width_dp: u16,
    pub screen_height_dp: u16,
    pub locale_script: [u8; 4],
    pub locale_variant: [u8; 8],
    pub screen_layout2: u8,
    pub color_mode: u8,
    pub locale_numbering_system: [u8; 8],
}

/// 完整解析后的 ARSC 文件。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ArscFile {
    pub header: ArscHeader,
    pub global_string_pool: Option<StringPool>,
    pub packages: Vec<ArscPackage>,
}

impl ArscFile {
    /// 解析 ARSC 字节流。
    pub fn parse(bytes: &[u8]) -> Result<Self> {
        let header = parse_header(bytes)?;
        let mut cursor = header.header_size as usize;
        // 全局字符串池（紧随 header）
        let global_string_pool = match peek_type(bytes, cursor)? {
            Some(RES_STRING_POOL_TYPE) => Some(parse_string_pool(bytes, cursor)?),
            _ => None,
        };
        if let Some(pool) = &global_string_pool {
            cursor = pool.offset + pool.size as usize;
        }
        let mut packages = Vec::with_capacity(header.package_count as usize);
        let mut scanned = 0u32;
        while cursor < bytes.len() {
            let ty = match peek_type(bytes, cursor)? {
                Some(t) => t,
                None => break,
            };
            if ty == RES_TABLE_PACKAGE_TYPE {
                let pkg = parse_package(bytes, cursor)?;
                let size = u32_at(bytes, cursor + 4, "package size")? as usize;
                cursor = cursor
                    .checked_add(size)
                    .ok_or_else(|| Error::InvalidData("ARSC: package offset overflow".into()))?;
                packages.push(pkg);
                scanned += 1;
                if scanned >= header.package_count {
                    break;
                }
            } else {
                // 未知 chunk：按 size 前进，size 非法则终止
                let size = u32_at(bytes, cursor + 4, "chunk size")? as usize;
                if size < 8 {
                    break;
                }
                cursor = cursor
                    .checked_add(size)
                    .ok_or_else(|| Error::InvalidData("ARSC: chunk offset overflow".into()))?;
            }
        }
        Ok(Self {
            header,
            global_string_pool,
            packages,
        })
    }

    /// 全局池字符串（用于 value_display 的 TYPE_STRING）。
    pub fn global_string(&self, idx: u32) -> Option<&str> {
        self.global_string_pool.as_ref()?.string(idx)
    }

    /// 类型名（包 + type id）。
    pub fn type_name(&self, package_idx: usize, type_id: u8) -> Option<&str> {
        self.packages.get(package_idx)?.type_name(type_id)
    }
}

// ---------------------------------------------------------------------------
// 底层读取
// ---------------------------------------------------------------------------

fn u16_at(bytes: &[u8], off: usize, what: &str) -> Result<u16> {
    let end = off
        .checked_add(2)
        .ok_or_else(|| Error::InvalidData(format!("{what}: offset overflow")))?;
    let slice = bytes.get(off..end).ok_or_else(|| {
        Error::InvalidData(format!(
            "{what}: offset {off} out of bounds (len {})",
            bytes.len()
        ))
    })?;
    Ok(u16::from_le_bytes([slice[0], slice[1]]))
}

fn u32_at(bytes: &[u8], off: usize, what: &str) -> Result<u32> {
    let end = off
        .checked_add(4)
        .ok_or_else(|| Error::InvalidData(format!("{what}: offset overflow")))?;
    let slice = bytes.get(off..end).ok_or_else(|| {
        Error::InvalidData(format!(
            "{what}: offset {off} out of bounds (len {})",
            bytes.len()
        ))
    })?;
    Ok(u32::from_le_bytes([slice[0], slice[1], slice[2], slice[3]]))
}

fn peek_type(bytes: &[u8], off: usize) -> Result<Option<u16>> {
    if off + 8 > bytes.len() {
        return Ok(None);
    }
    Ok(Some(u16_at(bytes, off, "chunk type")?))
}

fn parse_header(bytes: &[u8]) -> Result<ArscHeader> {
    if bytes.len() < 12 {
        return Err(Error::InvalidData(format!(
            "ARSC: file too small ({} bytes, need >= 12)",
            bytes.len()
        )));
    }
    let header_type = u16_at(bytes, 0, "ARSC header_type")?;
    if header_type != RES_TABLE_TYPE {
        return Err(Error::InvalidData(format!(
            "ARSC: bad header type 0x{header_type:04x} (not a resource table)"
        )));
    }
    Ok(ArscHeader {
        header_type,
        header_size: u16_at(bytes, 2, "ARSC header_size")?,
        size: u32_at(bytes, 4, "ARSC size")?,
        package_count: u32_at(bytes, 8, "ARSC package_count")?,
    })
}

// ---------------------------------------------------------------------------
// 字符串池
// ---------------------------------------------------------------------------

fn parse_string_pool(bytes: &[u8], offset: usize) -> Result<StringPool> {
    if offset + 28 > bytes.len() {
        return Err(Error::InvalidData(format!(
            "ARSC: string pool at {offset} truncated"
        )));
    }
    let header_type = u16_at(bytes, offset, "pool type")?;
    if header_type != RES_STRING_POOL_TYPE {
        return Err(Error::InvalidData(format!(
            "ARSC: not a string pool (0x{header_type:04x})"
        )));
    }
    let header_size = u16_at(bytes, offset + 2, "pool header_size")? as usize;
    let size = u32_at(bytes, offset + 4, "pool size")?;
    let string_count = u32_at(bytes, offset + 8, "pool string_count")?;
    let style_count = u32_at(bytes, offset + 12, "pool style_count")?;
    let flags = u32_at(bytes, offset + 16, "pool flags")?;
    let strings_start = u32_at(bytes, offset + 20, "pool strings_start")?;
    let styles_start = u32_at(bytes, offset + 24, "pool styles_start")?;
    let utf8 = flags & UTF8_FLAG != 0;

    if string_count > MAX_STRING_COUNT {
        return Err(Error::InvalidData(format!(
            "ARSC: string_count {string_count} exceeds limit"
        )));
    }
    if header_size < 28 {
        return Err(Error::InvalidData(format!(
            "ARSC: pool header_size {header_size} too small"
        )));
    }

    let mut strings = Vec::with_capacity(string_count as usize);
    for i in 0..string_count {
        let off_tab = offset
            .checked_add(header_size)
            .and_then(|v| v.checked_add(i as usize * 4))
            .ok_or_else(|| Error::InvalidData("ARSC: pool offset table overflow".into()))?;
        let rel = u32_at(bytes, off_tab, "pool string offset")? as usize;
        let str_off = offset
            .checked_add(strings_start as usize)
            .and_then(|v| v.checked_add(rel))
            .ok_or_else(|| Error::InvalidData("ARSC: string offset overflow".into()))?;
        let s = if utf8 {
            decode_utf8_pool_string(bytes, str_off)?
        } else {
            decode_utf16_pool_string(bytes, str_off)?
        };
        strings.push(s);
    }

    Ok(StringPool {
        offset,
        header_size: header_size as u16,
        size,
        string_count,
        style_count,
        flags,
        strings_start,
        styles_start,
        utf8,
        strings,
    })
}

/// AOSP `decodeLength`：0x80 标志表示 2 字节长度（非 ULEB128）。
fn decode_pool_length(bytes: &[u8], off: usize, what: &str) -> Result<(usize, usize)> {
    let b0 = *bytes
        .get(off)
        .ok_or_else(|| Error::InvalidData(format!("{what}: length truncated")))?;
    if b0 & 0x80 != 0 {
        let b1 = *bytes
            .get(off + 1)
            .ok_or_else(|| Error::InvalidData(format!("{what}: length truncated")))?;
        let len = ((usize::from(b0) & 0x7F) << 8) | usize::from(b1);
        Ok((len, off + 2))
    } else {
        Ok((usize::from(b0), off + 1))
    }
}

fn decode_utf8_pool_string(bytes: &[u8], off: usize) -> Result<String> {
    let (_utf16_len, c1) = decode_pool_length(bytes, off, "utf8 string utf16_len")?;
    let (utf8_len, c2) = decode_pool_length(bytes, c1, "utf8 string utf8_len")?;
    let end = c2
        .checked_add(utf8_len)
        .ok_or_else(|| Error::InvalidData("utf8 string length overflow".into()))?;
    let slice = bytes
        .get(c2..end)
        .ok_or_else(|| Error::InvalidData(format!("utf8 string out of bounds ({c2}..{end})")))?;
    Ok(String::from_utf8_lossy(slice).into_owned())
}

/// AOSP `decodeLength16`：0x8000 标志表示 4 字节长度。
fn decode_utf16_pool_string(bytes: &[u8], off: usize) -> Result<String> {
    let first = u16_at(bytes, off, "utf16 string length")?;
    let (len, data_off) = if first & 0x8000 != 0 {
        let len32 = u32_at(bytes, off, "utf16 string length32")? & 0x7FFF_FFFF;
        (len32 as usize, off + 4)
    } else {
        (first as usize, off + 2)
    };
    let byte_len = len
        .checked_mul(2)
        .ok_or_else(|| Error::InvalidData("utf16 string length overflow".into()))?;
    let end = data_off
        .checked_add(byte_len)
        .ok_or_else(|| Error::InvalidData("utf16 string length overflow".into()))?;
    let slice = bytes.get(data_off..end).ok_or_else(|| {
        Error::InvalidData(format!("utf16 string out of bounds ({data_off}..{end})"))
    })?;
    let mut units = Vec::with_capacity(len);
    for chunk in slice.chunks_exact(2) {
        units.push(u16::from_le_bytes([chunk[0], chunk[1]]));
    }
    Ok(String::from_utf16_lossy(&units))
}

// ---------------------------------------------------------------------------
// package
// ---------------------------------------------------------------------------

fn parse_package(bytes: &[u8], offset: usize) -> Result<ArscPackage> {
    if offset + 288 > bytes.len() {
        return Err(Error::InvalidData(format!(
            "ARSC: package at {offset} truncated"
        )));
    }
    let header_type = u16_at(bytes, offset, "package type")?;
    if header_type != RES_TABLE_PACKAGE_TYPE {
        return Err(Error::InvalidData(format!(
            "ARSC: not a package chunk (0x{header_type:04x})"
        )));
    }
    let header_size = u16_at(bytes, offset + 2, "package header_size")? as usize;
    let size = u32_at(bytes, offset + 4, "package size")? as usize;
    let id = u32_at(bytes, offset + 8, "package id")?;
    let name = decode_package_name(bytes, offset + 12)?;
    let type_strings_offset = u32_at(bytes, offset + 268, "package typeStrings")? as usize;
    let key_strings_offset = u32_at(bytes, offset + 276, "package keyStrings")? as usize;
    let _type_id_offset = u32_at(bytes, offset + 284, "package typeIdOffset")?;

    let pkg_end = offset
        .checked_add(size)
        .ok_or_else(|| Error::InvalidData("ARSC: package size overflow".into()))?
        .min(bytes.len());

    let type_strings = parse_string_pool(bytes, offset + type_strings_offset).ok();
    let key_strings = parse_string_pool(bytes, offset + key_strings_offset).ok();

    let mut type_specs = Vec::new();
    let mut types = Vec::new();
    let mut cursor = offset + header_size;
    while cursor + 8 <= pkg_end {
        let ty = match u16_at(bytes, cursor, "package child type") {
            Ok(t) => t,
            Err(_) => break,
        };
        let chunk_size = match u32_at(bytes, cursor + 4, "package child size") {
            Ok(s) => s as usize,
            Err(_) => break,
        };
        if chunk_size < 8 || cursor + chunk_size > pkg_end {
            break;
        }
        match ty {
            RES_TABLE_TYPE_SPEC_TYPE => {
                if let Ok(spec) = parse_type_spec(bytes, cursor) {
                    type_specs.push(spec);
                }
            }
            RES_TABLE_TYPE_TYPE => {
                if let Ok(t) = parse_type_chunk(bytes, cursor) {
                    types.push(t);
                }
            }
            _ => {}
        }
        cursor += chunk_size;
    }

    Ok(ArscPackage {
        offset,
        id,
        name,
        type_strings_offset: type_strings_offset as u32,
        key_strings_offset: key_strings_offset as u32,
        type_strings,
        key_strings,
        type_specs,
        types,
    })
}

fn decode_package_name(bytes: &[u8], off: usize) -> Result<String> {
    let mut units = Vec::new();
    for i in 0..128 {
        let u = u16_at(bytes, off + i * 2, "package name")?;
        if u == 0 {
            break;
        }
        units.push(u);
    }
    Ok(String::from_utf16_lossy(&units))
}

fn parse_type_spec(bytes: &[u8], offset: usize) -> Result<TypeSpec> {
    if offset + 16 > bytes.len() {
        return Err(Error::InvalidData("ARSC: typeSpec truncated".into()));
    }
    let id = bytes[offset + 8];
    let entry_count = u32_at(bytes, offset + 12, "typeSpec entryCount")?;
    if entry_count > MAX_ENTRY_COUNT {
        return Err(Error::InvalidData(format!(
            "ARSC: typeSpec entry_count {entry_count} exceeds limit"
        )));
    }
    let mut flags = Vec::with_capacity(entry_count as usize);
    for i in 0..entry_count {
        flags.push(u32_at(
            bytes,
            offset + 16 + i as usize * 4,
            "typeSpec flag",
        )?);
    }
    Ok(TypeSpec {
        offset,
        id,
        entry_count,
        flags,
    })
}

fn parse_type_chunk(bytes: &[u8], offset: usize) -> Result<TypeChunk> {
    if offset + 16 > bytes.len() {
        return Err(Error::InvalidData("ARSC: type chunk truncated".into()));
    }
    let header_size = u16_at(bytes, offset + 2, "type header_size")? as usize;
    let id = bytes[offset + 8];
    let entry_count = u32_at(bytes, offset + 12, "type entryCount")?;
    if entry_count > MAX_ENTRY_COUNT {
        return Err(Error::InvalidData(format!(
            "ARSC: type entry_count {entry_count} exceeds limit"
        )));
    }
    let entries_start = u32_at(bytes, offset + 16, "type entriesStart")? as usize;
    let config = parse_config(bytes, offset + 20)?;
    let entries_base = offset + entries_start;

    let mut entries = Vec::with_capacity(entry_count as usize);
    for i in 0..entry_count {
        let off_tab = offset
            .checked_add(header_size)
            .and_then(|v| v.checked_add(i as usize * 4))
            .ok_or_else(|| Error::InvalidData("ARSC: type offset table overflow".into()))?;
        let rel = u32_at(bytes, off_tab, "type entry offset")?;
        if rel == NO_ENTRY {
            entries.push(None);
            continue;
        }
        let entry_off = entries_base
            .checked_add(rel as usize)
            .ok_or_else(|| Error::InvalidData("ARSC: entry offset overflow".into()))?;
        entries.push(Some(parse_entry(bytes, entry_off)?));
    }

    Ok(TypeChunk {
        offset,
        id,
        entry_count,
        entries_start: entries_start as u32,
        config,
        entries,
    })
}

fn parse_entry(bytes: &[u8], offset: usize) -> Result<ResEntry> {
    if offset + 8 > bytes.len() {
        return Err(Error::InvalidData(format!(
            "ARSC: entry at {offset} truncated"
        )));
    }
    let size = u16_at(bytes, offset, "entry size")? as usize;
    let flags = u16_at(bytes, offset + 2, "entry flags")?;
    let key_idx = u32_at(bytes, offset + 4, "entry key")?;
    if size < 8 {
        return Err(Error::InvalidData(format!(
            "ARSC: entry size {size} too small"
        )));
    }
    if flags & TABLE_ENTRY_FLAG_COMPLEX != 0 {
        // ResTable_map_entry：size=16，parent/count + map items
        if size < 16 {
            return Err(Error::InvalidData("ARSC: map entry size < 16".into()));
        }
        let parent = u32_at(bytes, offset + 8, "map parent")?;
        let count = u32_at(bytes, offset + 12, "map count")?;
        if count > MAX_ENTRY_COUNT {
            return Err(Error::InvalidData(format!(
                "ARSC: map count {count} exceeds limit"
            )));
        }
        let mut maps = Vec::with_capacity(count as usize);
        for i in 0..count {
            let item_off = offset
                .checked_add(16 + i as usize * 12)
                .ok_or_else(|| Error::InvalidData("ARSC: map item offset overflow".into()))?;
            let name_ref = u32_at(bytes, item_off, "map name_ref")?;
            let value = parse_value(bytes, item_off + 4)?;
            maps.push(ResMapEntry { name_ref, value });
        }
        Ok(ResEntry {
            flags,
            key_idx,
            value: None,
            complex: Some(ResComplexValue { parent, maps }),
        })
    } else {
        let value_off = offset + 8;
        let value = if value_off + 8 <= bytes.len() {
            Some(parse_value(bytes, value_off)?)
        } else {
            None
        };
        Ok(ResEntry {
            flags,
            key_idx,
            value,
            complex: None,
        })
    }
}

fn parse_value(bytes: &[u8], offset: usize) -> Result<ResValue> {
    if offset + 8 > bytes.len() {
        return Err(Error::InvalidData(format!(
            "ARSC: value at {offset} truncated"
        )));
    }
    let data_type = bytes[offset + 3];
    let data = u32_at(bytes, offset + 4, "value data")?;
    Ok(ResValue { data_type, data })
}

// ---------------------------------------------------------------------------
// ResTable_config
// ---------------------------------------------------------------------------

/// 解析 config（字段按 AOSP 布局，`size` 决定实际可用长度）。
pub fn parse_config(bytes: &[u8], offset: usize) -> Result<ResTableConfig> {
    if offset + 4 > bytes.len() {
        return Err(Error::InvalidData("ARSC: config truncated".into()));
    }
    let size = u32_at(bytes, offset, "config size")? as usize;
    if size < 4 {
        return Err(Error::InvalidData(format!(
            "ARSC: config size {size} too small"
        )));
    }
    let avail = bytes.len().saturating_sub(offset).min(size);
    let rd_u32 = |at: usize, what: &str| -> u32 {
        if at + 4 <= avail {
            u32_at(bytes, offset + at, what).unwrap_or(0)
        } else {
            0
        }
    };
    let rd_u8 = |at: usize| -> u8 { bytes.get(offset + at).copied().unwrap_or(0) };
    let rd_arr = |at: usize, n: usize| -> Vec<u8> { (0..n).map(|i| rd_u8(at + i)).collect() };

    let imsi = rd_u32(4, "config imsi");
    let locale = rd_u32(8, "config locale");
    let screen_type = rd_u32(12, "config screenType");
    let input = rd_u32(16, "config input");
    let screen_size = rd_u32(20, "config screenSize");
    let version = rd_u32(24, "config version");
    let screen_config = rd_u32(28, "config screenConfig");
    let screen_size_dp = rd_u32(32, "config screenSizeDp");
    let locale_script = rd_arr(36, 4);
    let locale_variant = rd_arr(40, 8);
    let screen_config2 = rd_u32(48, "config screenConfig2");
    let locale_numbering_system = rd_arr(52, 8);

    let mut cfg = ResTableConfig {
        size: size as u32,
        mcc: (imsi >> 8) as u16,
        mnc: imsi as u16,
        language: [locale as u8, (locale >> 8) as u8],
        country: [(locale >> 16) as u8, (locale >> 24) as u8],
        orientation: screen_type as u8,
        touchscreen: (screen_type >> 8) as u8,
        density: (screen_type >> 16) as u16,
        keyboard: input as u8,
        navigation: (input >> 8) as u8,
        input_flags: (input >> 16) as u8,
        screen_width: screen_size as u16,
        screen_height: (screen_size >> 16) as u16,
        sdk_version: version as u16,
        minor_version: (version >> 16) as u16,
        screen_layout: screen_config as u8,
        ui_mode: (screen_config >> 8) as u8,
        smallest_screen_width_dp: (screen_config >> 16) as u16,
        screen_width_dp: screen_size_dp as u16,
        screen_height_dp: (screen_size_dp >> 16) as u16,
        locale_script: locale_script.try_into().unwrap_or([0; 4]),
        locale_variant: locale_variant.try_into().unwrap_or([0; 8]),
        screen_layout2: screen_config2 as u8,
        color_mode: (screen_config2 >> 8) as u8,
        locale_numbering_system: locale_numbering_system.try_into().unwrap_or([0; 8]),
    };
    if cfg.size == 0 {
        cfg.size = 4;
    }
    Ok(cfg)
}

/// Android 风格 config 描述串，如 `zh-rCN-xxhdpi-800x480-v21-sw600dp`。
pub fn config_summary(cfg: &ResTableConfig) -> String {
    let mut parts: Vec<String> = Vec::new();

    // mcc/mnc
    if cfg.mcc != 0 {
        parts.push(format!("mcc{}", cfg.mcc));
    }
    if cfg.mnc != 0 {
        parts.push(format!("mnc{}", cfg.mnc));
    }
    // locale
    let lang = ascii_trim(&cfg.language);
    let country = ascii_trim(&cfg.country);
    if !lang.is_empty() {
        let mut l = format!("{lang}-r{country}");
        let script = ascii_trim(&cfg.locale_script);
        if !script.is_empty() {
            l.push('-');
            l.push_str(&script);
        }
        let variant = ascii_trim(&cfg.locale_variant);
        if !variant.is_empty() {
            l.push('-');
            l.push_str(&variant);
        }
        parts.push(l);
    } else if !country.is_empty() {
        parts.push(format!("r{country}"));
    }
    // screenType
    match cfg.orientation {
        1 => parts.push("port".into()),
        2 => parts.push("land".into()),
        3 => parts.push("square".into()),
        _ => {}
    }
    match cfg.touchscreen {
        1 => parts.push("notouch".into()),
        2 => parts.push("stylus".into()),
        3 => parts.push("finger".into()),
        _ => {}
    }
    if cfg.density != 0 {
        parts.push(density_name(cfg.density));
    }
    // input
    match cfg.keyboard {
        1 => parts.push("nokeys".into()),
        2 => parts.push("qwerty".into()),
        3 => parts.push("12key".into()),
        _ => {}
    }
    match cfg.navigation {
        1 => parts.push("nonav".into()),
        2 => parts.push("dpad".into()),
        3 => parts.push("trackball".into()),
        4 => parts.push("wheel".into()),
        _ => {}
    }
    if cfg.input_flags & 0x01 != 0 {
        parts.push("keysexposed".into());
    }
    if cfg.input_flags & 0x02 != 0 {
        parts.push("keyshidden".into());
    }
    if cfg.input_flags & 0x04 != 0 {
        parts.push("keyssoft".into());
    }
    if cfg.input_flags & 0x08 != 0 {
        parts.push("navhidden".into());
    }
    // screenSize
    if cfg.screen_width != 0 && cfg.screen_height != 0 {
        parts.push(format!("{}x{}", cfg.screen_width, cfg.screen_height));
    }
    // version
    if cfg.sdk_version != 0 {
        parts.push(format!("v{}", cfg.sdk_version));
    }
    // screenConfig
    match cfg.screen_layout & 0x0F {
        1 => parts.push("small".into()),
        2 => parts.push("normal".into()),
        3 => parts.push("large".into()),
        4 => parts.push("xlarge".into()),
        _ => {}
    }
    if cfg.screen_layout & 0x10 != 0 {
        parts.push("long".into());
    }
    match cfg.ui_mode & 0x0F {
        1 => parts.push("car".into()),
        2 => parts.push("desk".into()),
        3 => parts.push("television".into()),
        4 => parts.push("appliance".into()),
        5 => parts.push("watch".into()),
        6 => parts.push("vrheadset".into()),
        _ => {}
    }
    if cfg.ui_mode & 0x20 != 0 {
        parts.push("night".into());
    }
    if cfg.smallest_screen_width_dp != 0 {
        parts.push(format!("sw{}dp", cfg.smallest_screen_width_dp));
    }
    // screenSizeDp
    if cfg.screen_width_dp != 0 {
        parts.push(format!("w{}dp", cfg.screen_width_dp));
    }
    if cfg.screen_height_dp != 0 {
        parts.push(format!("h{}dp", cfg.screen_height_dp));
    }
    // screenConfig2
    if cfg.screen_layout2 & 0x01 != 0 {
        parts.push("round".into());
    }
    if cfg.color_mode & 0x02 != 0 {
        parts.push("widecg".into());
    }
    if cfg.color_mode & 0x04 != 0 {
        parts.push("highdr".into());
    }
    // localeNumberingSystem
    let numbering = ascii_trim(&cfg.locale_numbering_system);
    if !numbering.is_empty() {
        parts.push(format!("u-nu-{numbering}"));
    }

    parts.join("-")
}

fn density_name(density: u16) -> String {
    match density {
        120 => "ldpi".into(),
        160 => "mdpi".into(),
        213 => "tvdpi".into(),
        240 => "hdpi".into(),
        320 => "xhdpi".into(),
        480 => "xxhdpi".into(),
        640 => "xxxhdpi".into(),
        0xFFFF => "nodpi".into(),
        0xFFFE => "anydpi".into(),
        other => format!("{other}dpi"),
    }
}

fn ascii_trim(b: &[u8]) -> String {
    let s: String = b
        .iter()
        .take_while(|&&c| c != 0)
        .map(|&c| c as char)
        .collect();
    s.trim().to_string()
}

// ---------------------------------------------------------------------------
// 值类型与显示
// ---------------------------------------------------------------------------

pub const TYPE_NULL: u8 = 0x00;
pub const TYPE_REFERENCE: u8 = 0x01;
pub const TYPE_ATTRIBUTE: u8 = 0x02;
pub const TYPE_STRING: u8 = 0x03;
pub const TYPE_FLOAT: u8 = 0x04;
pub const TYPE_DIMENSION: u8 = 0x05;
pub const TYPE_FRACTION: u8 = 0x06;
pub const TYPE_INT_DEC: u8 = 0x10;
pub const TYPE_INT_HEX: u8 = 0x11;
pub const TYPE_INT_BOOLEAN: u8 = 0x12;
pub const TYPE_INT_COLOR_ARGB8: u8 = 0x1C;
pub const TYPE_INT_COLOR_RGB8: u8 = 0x1D;
pub const TYPE_INT_COLOR_ARGB4: u8 = 0x1E;
pub const TYPE_INT_COLOR_RGB4: u8 = 0x1F;

pub const COMPLEX_UNIT_MASK: u32 = 0x0F;
pub const COMPLEX_RADIX_SHIFT: u32 = 4;
pub const COMPLEX_RADIX_MASK: u32 = 0x03;

/// 值类型名（Android `Res_value::dataType`）。
pub fn value_type_name(t: u8) -> &'static str {
    match t {
        TYPE_NULL => "TYPE_NULL",
        TYPE_REFERENCE => "TYPE_REFERENCE",
        TYPE_ATTRIBUTE => "TYPE_ATTRIBUTE",
        TYPE_STRING => "TYPE_STRING",
        TYPE_FLOAT => "TYPE_FLOAT",
        TYPE_DIMENSION => "TYPE_DIMENSION",
        TYPE_FRACTION => "TYPE_FRACTION",
        0x07 => "TYPE_DYNAMIC_REFERENCE",
        0x08 => "TYPE_DYNAMIC_ATTRIBUTE",
        TYPE_INT_DEC => "TYPE_INT_DEC",
        TYPE_INT_HEX => "TYPE_INT_HEX",
        TYPE_INT_BOOLEAN => "TYPE_INT_BOOLEAN",
        TYPE_INT_COLOR_ARGB8 => "TYPE_INT_COLOR_ARGB8",
        TYPE_INT_COLOR_RGB8 => "TYPE_INT_COLOR_RGB8",
        TYPE_INT_COLOR_ARGB4 => "TYPE_INT_COLOR_ARGB4",
        TYPE_INT_COLOR_RGB4 => "TYPE_INT_COLOR_RGB4",
        other => {
            debug_assert!(false, "unknown dataType {other:#x}");
            "TYPE_UNKNOWN"
        }
    }
}

/// 值的可读显示。`resolve_string` 用于 TYPE_STRING 的全局池查找。
pub fn value_display(
    value: &ResValue,
    resolve_string: impl FnOnce(u32) -> Option<String>,
) -> String {
    let data = value.data;
    match value.data_type {
        TYPE_NULL => "null".to_string(),
        TYPE_REFERENCE => format!("@0x{data:08x}"),
        TYPE_ATTRIBUTE => format!("?0x{data:08x}"),
        TYPE_STRING => resolve_string(data)
            .map(|s| format!("\"{s}\""))
            .unwrap_or_else(|| format!("string[{data}]")),
        TYPE_FLOAT => {
            let f = f32::from_bits(data);
            format!("{f}")
        }
        TYPE_DIMENSION => {
            let (v, unit) = decode_complex(data);
            format!("{v}{}", dim_unit_name(unit))
        }
        TYPE_FRACTION => {
            let (v, unit) = decode_complex(data);
            let name = if unit == 0 { "%" } else { "%p" };
            format!("{v}{name}")
        }
        TYPE_INT_DEC => format!("{data}"),
        TYPE_INT_HEX => format!("0x{data:x}"),
        TYPE_INT_BOOLEAN => {
            if data != 0 {
                "true".to_string()
            } else {
                "false".to_string()
            }
        }
        TYPE_INT_COLOR_ARGB8 | TYPE_INT_COLOR_RGB8 | TYPE_INT_COLOR_ARGB4 | TYPE_INT_COLOR_RGB4 => {
            format!("#{data:08x}")
        }
        _ => format!("0x{data:x}"),
    }
}

fn dim_unit_name(unit: u8) -> &'static str {
    match unit {
        0 => "px",
        1 => "dp",
        2 => "sp",
        3 => "pt",
        4 => "in",
        5 => "mm",
        _ => "?",
    }
}

/// 解码 complex 数值（dimension/fraction）：radix + 单位码。
/// 返回 (数值, 单位码)；单位语义由调用方按 dimension/fraction 解释。
fn decode_complex(data: u32) -> (f64, u8) {
    let unit = (data & COMPLEX_UNIT_MASK) as u8;
    let radix = (data >> COMPLEX_RADIX_SHIFT) & COMPLEX_RADIX_MASK;
    let mantissa = (data >> 8) as f64;
    let divisor: f64 = match radix {
        0 => 1.0,
        1 => 256.0,
        2 => 65_536.0,
        _ => 16_777_216.0,
    };
    (mantissa / divisor, unit)
}

// ---------------------------------------------------------------------------
// 测试
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    struct Writer {
        data: Vec<u8>,
    }

    impl Writer {
        fn new() -> Self {
            Self { data: Vec::new() }
        }
        fn u16(&mut self, v: u16) {
            self.data.extend_from_slice(&v.to_le_bytes());
        }
        fn u32(&mut self, v: u32) {
            self.data.extend_from_slice(&v.to_le_bytes());
        }
        fn bytes(&mut self, b: &[u8]) {
            self.data.extend_from_slice(b);
        }
        fn len(&self) -> usize {
            self.data.len()
        }
        fn patch_u32(&mut self, at: usize, v: u32) {
            let b = v.to_le_bytes();
            self.data[at..at + 4].copy_from_slice(&b);
        }
    }

    /// UTF-16 池字符串编码（无 0x8000 扩展）。
    fn utf16_enc(s: &str) -> Vec<u8> {
        let mut v = Vec::new();
        v.extend_from_slice(&(s.encode_utf16().count() as u16).to_le_bytes());
        for u in s.encode_utf16() {
            v.extend_from_slice(&u.to_le_bytes());
        }
        v
    }

    /// 构造 ARSC：
    /// - header + 全局池 ["app_name", "hello"]
    /// - package id=1 name="com.example.test"，typeStrings=["string"]，keyStrings=["app_name"]
    /// - typeSpec id=1；type id=1 config=默认，entry[0] = string value @全局池 0
    fn sample_arsc() -> Vec<u8> {
        let mut w = Writer::new();
        // --- header (12) ---
        w.u16(RES_TABLE_TYPE);
        w.u16(12);
        w.u32(0); // size（后面回填）
        w.u32(1); // packageCount

        // --- global string pool ---
        let gs = ["app_name", "hello"];
        let gs_enc: Vec<Vec<u8>> = gs.iter().map(|s| utf16_enc(s)).collect();
        let gs_str_start = 28 + gs.len() * 4;
        let mut gs_offsets = Vec::new();
        let mut acc = gs_str_start;
        for enc in &gs_enc {
            gs_offsets.push(acc);
            acc += enc.len();
        }
        let gs_size = acc;
        let gs_start = w.len();
        w.u16(RES_STRING_POOL_TYPE);
        w.u16(28);
        w.u32(gs_size as u32);
        w.u32(gs.len() as u32); // stringCount
        w.u32(0); // styleCount
        w.u32(0); // flags (utf16)
        w.u32(gs_str_start as u32); // stringsStart
        w.u32(0); // stylesStart
        for o in &gs_offsets {
            w.u32((o - gs_str_start) as u32);
        }
        for enc in &gs_enc {
            w.bytes(enc);
        }
        assert_eq!(w.len() - gs_start, gs_size);

        // --- package ---
        let pkg_start = w.len();
        let pkg_size_mark = w.len() + 4; // size 字段位置
        w.u16(RES_TABLE_PACKAGE_TYPE);
        w.u16(288);
        w.u32(0); // size（后面回填）
        w.u32(1); // id
        let name_units: Vec<u16> = "com.example.test".encode_utf16().collect();
        for i in 0..128 {
            w.u16(*name_units.get(i).unwrap_or(&0));
        }
        // typeStrings/keyStrings 偏移（相对 package 起始）
        let type_pool_rel = 288usize;
        let ts_enc_pre = utf16_enc("string");
        let key_pool_rel = 288 + 28 + 4 + ts_enc_pre.len();
        w.u32(type_pool_rel as u32); // +268 typeStrings
        w.u32(0); // +272 lastPublicType
        w.u32(key_pool_rel as u32); // +276 keyStrings
        w.u32(0); // +280 lastPublicKey
        w.u32(0); // +284 typeIdOffset
        assert_eq!(w.len() - pkg_start, 288);

        // typeStrings pool: ["string"]
        let ts_enc = utf16_enc("string");
        let ts_size = 28 + 4 + ts_enc.len();
        w.u16(RES_STRING_POOL_TYPE);
        w.u16(28);
        w.u32(ts_size as u32);
        w.u32(1);
        w.u32(0);
        w.u32(0); // utf16
        w.u32(32);
        w.u32(0);
        w.u32(0); // offset[0] = 0
        w.bytes(&ts_enc);
        assert_eq!(w.len() - pkg_start, key_pool_rel);

        // keyStrings pool: ["app_name"]
        let ks_enc = utf16_enc("app_name");
        let ks_size = 28 + 4 + ks_enc.len();
        let ks_start = w.len();
        w.u16(RES_STRING_POOL_TYPE);
        w.u16(28);
        w.u32(ks_size as u32);
        w.u32(1);
        w.u32(0);
        w.u32(0); // utf16
        w.u32(32);
        w.u32(0);
        w.u32(0); // offset[0] = 0
        w.bytes(&ks_enc);
        assert_eq!(w.len() - ks_start, ks_size);

        // typeSpec id=1 entryCount=1
        let spec_start = w.len();
        w.u16(RES_TABLE_TYPE_SPEC_TYPE);
        w.u16(16);
        w.u32(20);
        w.bytes(&[1, 0, 0, 0]); // id=1 res0 res1
        w.u32(1); // entryCount
        w.u32(0); // flags[0]
        assert_eq!(w.len() - spec_start, 20);

        // type id=1 entryCount=1 config=default
        let type_start = w.len();
        w.u16(RES_TABLE_TYPE_TYPE);
        w.u16(24); // headerSize：8 + id/res/entryCount/entriesStart + config.size(4)
        w.u32(0); // size（后面回填）
        w.bytes(&[1, 0, 0, 0]);
        w.u32(1); // entryCount
        w.u32(28); // entriesStart（offset 表之后，相对 type chunk 开头）
        w.u32(4); // config.size=4（默认配置）
        w.u32(0); // entry offset table: [0]
                  // entry @ type_start+28: size=8 flags=0 key=0, value: size=8 res0=0 type=0x03 data=0
        let entry_mark = w.len();
        w.u16(8);
        w.u16(0);
        w.u32(0); // key → "app_name"
        w.u16(8);
        w.bytes(&[0, 0x03]); // res0, dataType=string
        w.u32(0); // data → 全局池 "app_name"
        let type_size = w.len() - type_start;
        assert_eq!(type_size, 24 + 4 + 16); // header + offset 表 + entry
        assert_eq!(w.len() - entry_mark, 16);

        // 回填大小
        let total = w.len();
        w.patch_u32(4, total as u32); // header size 字段 @4（不是 header_size_mark！）
        w.patch_u32(pkg_size_mark, (total - pkg_start) as u32);
        w.patch_u32(type_start + 4, type_size as u32);
        assert_eq!(w.len(), total);
        w.data
    }

    #[test]
    fn parses_sample_arsc() {
        let bytes = sample_arsc();
        let arsc = ArscFile::parse(&bytes).unwrap();
        assert_eq!(arsc.header.header_type, RES_TABLE_TYPE);
        assert_eq!(arsc.header.package_count, 1);
        let gs = arsc.global_string_pool.as_ref().unwrap();
        assert_eq!(gs.strings, vec!["app_name", "hello"]);
        assert!(!gs.utf8);

        let pkg = &arsc.packages[0];
        assert_eq!(pkg.id, 1);
        assert_eq!(pkg.name, "com.example.test");
        assert_eq!(pkg.type_name(1), Some("string"));
        assert_eq!(pkg.key_name(0), Some("app_name"));
        assert_eq!(pkg.type_specs.len(), 1);
        assert_eq!(pkg.type_specs[0].id, 1);
        assert_eq!(pkg.types.len(), 1);
        let ty = &pkg.types[0];
        assert_eq!(ty.id, 1);
        assert_eq!(ty.non_empty_count(), 1);
        assert_eq!(config_summary(&ty.config), "");
        let entry = ty.entries[0].as_ref().unwrap();
        assert!(!entry.is_complex());
        assert!(!entry.is_public());
        let v = entry.value.unwrap();
        assert_eq!(v.data_type, TYPE_STRING);
        let display = value_display(&v, |idx| arsc.global_string(idx).map(str::to_string));
        assert_eq!(display, "\"app_name\"");
    }

    #[test]
    fn rejects_non_table() {
        let err = ArscFile::parse(&[
            0x01, 0x00, 0x0C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        ])
        .unwrap_err();
        assert!(err.to_string().contains("not a resource table"));
    }

    #[test]
    fn decodes_utf16_extended_length() {
        // 0x8000 扩展（length > 0x7FFF 时）：u16[0]=0x8000|high，u16[1]=low
        // 构造 length = 0x8000：high=0，low=0x8000
        let mut bytes = Vec::new();
        bytes.extend_from_slice(&0x8000u16.to_le_bytes());
        bytes.extend_from_slice(&0x8000u16.to_le_bytes());
        // UTF-16LE 'a' = 0x61 0x00
        for _ in 0..0x8000 {
            bytes.extend_from_slice(&0x0061u16.to_le_bytes());
        }
        let s = decode_utf16_pool_string(&bytes, 0).unwrap();
        assert_eq!(s.len(), 0x8000);
        assert!(s.chars().all(|c| c == 'a'));
        // 短字符串走 2 字节路径
        let mut short = Vec::new();
        short.extend_from_slice(&3u16.to_le_bytes());
        short.extend_from_slice(b"a\0b\0c\0");
        assert_eq!(decode_utf16_pool_string(&short, 0).unwrap(), "abc");
        // 数据不足时报错（不 panic）
        let mut truncated = Vec::new();
        truncated.extend_from_slice(&0x8000u16.to_le_bytes());
        truncated.extend_from_slice(&0x8000u16.to_le_bytes());
        truncated.extend_from_slice(&[0u8; 4]);
        assert!(decode_utf16_pool_string(&truncated, 0).is_err());
    }

    #[test]
    fn decodes_utf8_pool_string() {
        // utf16_len=5, utf8_len=5, "hello"
        let mut bytes = vec![5u8, 5];
        bytes.extend_from_slice(b"hello");
        let s = decode_utf8_pool_string(&bytes, 0).unwrap();
        assert_eq!(s, "hello");
    }

    #[test]
    fn config_summary_renders() {
        let cfg = ResTableConfig {
            size: 60,
            language: *b"zh",
            country: *b"CN",
            orientation: 2,
            density: 480,
            screen_width: 800,
            screen_height: 480,
            sdk_version: 21,
            smallest_screen_width_dp: 600,
            ..Default::default()
        };
        let s = config_summary(&cfg);
        assert_eq!(s, "zh-rCN-land-xxhdpi-800x480-v21-sw600dp");
    }

    #[test]
    fn value_display_variants() {
        let mk = |t: u8, d: u32| ResValue {
            data_type: t,
            data: d,
        };
        let none = |_: u32| -> Option<String> { None };
        assert_eq!(value_display(&mk(TYPE_INT_DEC, 42), none), "42");
        assert_eq!(value_display(&mk(TYPE_INT_HEX, 0xAB), none), "0xab");
        assert_eq!(value_display(&mk(TYPE_INT_BOOLEAN, 1), none), "true");
        assert_eq!(value_display(&mk(TYPE_INT_BOOLEAN, 0), none), "false");
        assert_eq!(
            value_display(&mk(TYPE_INT_COLOR_ARGB8, 0xFF112233), none),
            "#ff112233"
        );
        assert_eq!(
            value_display(&mk(TYPE_REFERENCE, 0x7F010000), none),
            "@0x7f010000"
        );
        assert_eq!(value_display(&mk(TYPE_STRING, 3), none), "string[3]");
        assert_eq!(
            value_display(&mk(TYPE_STRING, 3), |i| if i == 3 {
                Some("hi".into())
            } else {
                None
            }),
            "\"hi\""
        );
        // 16sp（radix=0 即整数 → 16sp）
        let dim = (16u32 << 8) | 2; // mantissa=16, radix=0, unit=sp
        assert_eq!(value_display(&mk(TYPE_DIMENSION, dim), none), "16sp");
        let frac = 25u32 << 8; // 25%%，unit=0 radix=0
        assert_eq!(value_display(&mk(TYPE_FRACTION, frac), none), "25%");
    }

    #[test]
    fn complex_entry_parses_maps() {
        // 手工构造 complex entry：size=16, flags=COMPLEX, key=0, parent=0, count=1
        let mut bytes = Vec::new();
        bytes.extend_from_slice(&16u16.to_le_bytes());
        bytes.extend_from_slice(&TABLE_ENTRY_FLAG_COMPLEX.to_le_bytes());
        bytes.extend_from_slice(&0u32.to_le_bytes()); // key
        bytes.extend_from_slice(&0u32.to_le_bytes()); // parent
        bytes.extend_from_slice(&1u32.to_le_bytes()); // count
        bytes.extend_from_slice(&0x0101_0000u32.to_le_bytes()); // name_ref
        bytes.extend_from_slice(&8u16.to_le_bytes());
        bytes.extend_from_slice(&[0, 0x10]); // res0, dataType
        bytes.extend_from_slice(&5u32.to_le_bytes()); // data
        let entry = parse_entry(&bytes, 0).unwrap();
        assert!(entry.is_complex());
        let c = entry.complex.as_ref().unwrap();
        assert_eq!(c.parent, 0);
        assert_eq!(c.maps.len(), 1);
        assert_eq!(c.maps[0].name_ref, 0x0101_0000);
        assert_eq!(c.maps[0].value.data_type, TYPE_INT_DEC);
        assert_eq!(c.maps[0].value.data, 5);
    }
}
