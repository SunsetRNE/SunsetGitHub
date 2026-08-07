//! DEX 文件完整解析器。
//!
//! 按 Android DEX 格式规范（<https://source.android.com/docs/core/runtime/dex-format>）
//! 实现全量结构解析，替代原 Kotlin `EngineeringToolScanner` 的"采样扫描"方案：
//! 原实现只读前 512KB、每种表只采样 8 条；本解析器解析全部表项，并深入
//! `class_data_item`（字段/方法编码），失败时返回结构化错误而非静默吞掉。

use crate::error::{Error, Result};

/// DEX magic 前 4 字节（"dex\n"）。
const DEX_MAGIC: [u8; 4] = *b"dex\n";
/// DEX header 标准大小。
const HEADER_SIZE: usize = 112;
/// `NO_INDEX` 哨兵值（用于 superclass/source_file 等可选索引）。
pub const NO_INDEX: u32 = 0xFFFF_FFFF;

// ---------------------------------------------------------------------------
// 数据结构
// ---------------------------------------------------------------------------

/// DEX header 全部字段。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DexHeader {
    pub magic: [u8; 8],
    /// 版本号，如 "035"、"037"。
    pub version: String,
    pub checksum: u32,
    pub signature: [u8; 20],
    pub file_size: u32,
    pub header_size: u32,
    pub endian_tag: u32,
    pub link_size: u32,
    pub link_off: u32,
    pub map_off: u32,
    pub string_ids_size: u32,
    pub string_ids_off: u32,
    pub type_ids_size: u32,
    pub type_ids_off: u32,
    pub proto_ids_size: u32,
    pub proto_ids_off: u32,
    pub field_ids_size: u32,
    pub field_ids_off: u32,
    pub method_ids_size: u32,
    pub method_ids_off: u32,
    pub class_defs_size: u32,
    pub class_defs_off: u32,
    pub data_size: u32,
    pub data_off: u32,
}

/// proto_id_item：方法原型。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ProtoId {
    /// shorty 字符串索引（如 "V"、"Lx;LL;"）。
    pub shorty_idx: u32,
    /// 返回类型（type_ids 索引）。
    pub return_type_idx: u32,
    /// 参数类型索引列表（type_ids 索引）。
    pub parameters: Vec<u32>,
}

/// field_id_item：字段。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct FieldId {
    /// 定义类（type_ids 索引）。
    pub class_idx: u16,
    /// 字段类型（type_ids 索引）。
    pub type_idx: u16,
    /// 字段名（string_ids 索引）。
    pub name_idx: u32,
}

/// method_id_item：方法。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MethodId {
    /// 定义类（type_ids 索引）。
    pub class_idx: u16,
    /// 原型（proto_ids 索引）。
    pub proto_idx: u16,
    /// 方法名（string_ids 索引）。
    pub name_idx: u32,
}

/// encoded_field：class_data 中的字段。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct EncodedField {
    /// field_ids 索引。
    pub field_idx: u32,
    pub access_flags: u32,
}

/// encoded_method：class_data 中的方法。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct EncodedMethod {
    /// method_ids 索引。
    pub method_idx: u32,
    pub access_flags: u32,
    /// code_item 偏移（0 表示无代码）。
    pub code_off: u32,
}

/// class_data_item：类的字段与方法。
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct ClassData {
    pub static_fields: Vec<EncodedField>,
    pub instance_fields: Vec<EncodedField>,
    pub direct_methods: Vec<EncodedMethod>,
    pub virtual_methods: Vec<EncodedMethod>,
}

/// class_def_item：类定义。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ClassDef {
    /// 类类型（type_ids 索引）。
    pub class_idx: u32,
    pub access_flags: u32,
    /// 父类（type_ids 索引，`NO_INDEX` 表示无）。
    pub superclass_idx: u32,
    /// 接口类型索引列表。
    pub interfaces: Vec<u32>,
    /// 源文件名（string_ids 索引，`NO_INDEX` 表示无）。
    pub source_file_idx: u32,
    pub annotations_off: u32,
    pub class_data_off: u32,
    pub static_values_off: u32,
    pub class_data: Option<ClassData>,
}

/// 完整解析后的 DEX 文件。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DexFile {
    pub header: DexHeader,
    /// 全部字符串（按 string_ids 顺序）。
    pub strings: Vec<String>,
    /// type_ids：descriptor 字符串索引。
    pub types: Vec<u32>,
    pub protos: Vec<ProtoId>,
    pub fields: Vec<FieldId>,
    pub methods: Vec<MethodId>,
    pub class_defs: Vec<ClassDef>,
}

impl DexFile {
    /// 解析 DEX 字节流。
    pub fn parse(bytes: &[u8]) -> Result<Self> {
        let header = parse_header(bytes)?;
        let strings = parse_strings(bytes, &header)?;
        let types = parse_types(bytes, &header)?;
        let protos = parse_protos(bytes, &header)?;
        let fields = parse_fields(bytes, &header)?;
        let methods = parse_methods(bytes, &header)?;
        let class_defs = parse_class_defs(bytes, &header)?;
        Ok(Self {
            header,
            strings,
            types,
            protos,
            fields,
            methods,
            class_defs,
        })
    }

    /// 取字符串（越界返回 None）。
    pub fn string(&self, idx: u32) -> Option<&str> {
        self.strings.get(idx as usize).map(String::as_str)
    }

    /// 取类型描述符（如 "Lcom/example/A;"、"V"）。
    pub fn type_descriptor(&self, type_idx: u32) -> Option<&str> {
        let desc_idx = *self.types.get(type_idx as usize)?;
        self.string(desc_idx)
    }

    /// 取类型描述符（proto 的 return_type/参数等场景）。
    pub fn type_descriptor_of(&self, desc_idx: u32) -> Option<&str> {
        self.string(desc_idx)
    }

    /// 类名（去掉 `L`/`;` 包装，如 "com.example.A"）；非类类型返回描述符原样。
    pub fn class_name(&self, type_idx: u32) -> Option<String> {
        let desc = self.type_descriptor(type_idx)?;
        Some(pretty_type_name(desc))
    }

    /// 字段名。
    pub fn field_name(&self, field_idx: u32) -> Option<&str> {
        let field = self.fields.get(field_idx as usize)?;
        self.string(field.name_idx)
    }

    /// 方法名。
    pub fn method_name(&self, method_idx: u32) -> Option<&str> {
        let method = self.methods.get(method_idx as usize)?;
        self.string(method.name_idx)
    }

    /// 方法所属类名。
    pub fn method_class(&self, method_idx: u32) -> Option<String> {
        let method = self.methods.get(method_idx as usize)?;
        self.class_name(method.class_idx as u32)
    }

    /// 遍历全部类名（用于"工程扫描"展示）。
    pub fn all_class_names(&self) -> Vec<String> {
        self.class_defs
            .iter()
            .filter_map(|c| self.class_name(c.class_idx))
            .collect()
    }

    /// 遍历全部方法签名（类名 + 方法名）。
    pub fn all_method_signatures(&self) -> Vec<String> {
        self.methods
            .iter()
            .filter_map(|m| {
                let class = self.class_name(m.class_idx as u32)?;
                let name = self.string(m.name_idx)?;
                Some(format!("{class}::{name}"))
            })
            .collect()
    }
}

/// 将类型描述符转为可读类名："Lcom/example/A;" → "com.example.A"。
pub fn pretty_type_name(descriptor: &str) -> String {
    if let Some(inner) = descriptor
        .strip_prefix('L')
        .and_then(|s| s.strip_suffix(';'))
    {
        inner.replace('/', ".")
    } else {
        descriptor.to_string()
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

fn slice_at<'a>(bytes: &'a [u8], off: usize, len: usize, what: &str) -> Result<&'a [u8]> {
    let end = off
        .checked_add(len)
        .ok_or_else(|| Error::InvalidData(format!("{what}: offset overflow")))?;
    bytes.get(off..end).ok_or_else(|| {
        Error::InvalidData(format!(
            "{what}: range {off}..{end} out of bounds (len {})",
            bytes.len()
        ))
    })
}

/// ULEB128 解码，返回 (值, 下一个偏移)。
fn uleb128_at(bytes: &[u8], off: usize, what: &str) -> Result<(u32, usize)> {
    let mut value = 0u32;
    let mut shift = 0u32;
    let mut cursor = off;
    for _ in 0..5 {
        let b = *bytes
            .get(cursor)
            .ok_or_else(|| Error::InvalidData(format!("{what}: ULEB128 truncated at {cursor}")))?;
        value |= u32::from(b & 0x7F) << shift;
        cursor += 1;
        if b & 0x80 == 0 {
            return Ok((value, cursor));
        }
        shift += 7;
    }
    Err(Error::InvalidData(format!(
        "{what}: ULEB128 too long at {off}"
    )))
}

// ---------------------------------------------------------------------------
// 各表解析
// ---------------------------------------------------------------------------

fn parse_header(bytes: &[u8]) -> Result<DexHeader> {
    if bytes.len() < HEADER_SIZE {
        return Err(Error::InvalidData(format!(
            "DEX: file too small ({} bytes, need >= {HEADER_SIZE})",
            bytes.len()
        )));
    }
    let magic: [u8; 8] = bytes[0..8].try_into().expect("8 bytes");
    if magic[0..4] != DEX_MAGIC {
        return Err(Error::InvalidData(format!(
            "DEX: bad magic {:02x?} (not a dex file)",
            &magic[0..4]
        )));
    }
    if magic[7] != 0 {
        return Err(Error::InvalidData("DEX: bad magic trailer".into()));
    }
    let version = String::from_utf8_lossy(&magic[4..7]).into_owned();
    let mut signature = [0u8; 20];
    signature.copy_from_slice(slice_at(bytes, 12, 20, "DEX signature")?);
    Ok(DexHeader {
        magic,
        version,
        checksum: u32_at(bytes, 8, "DEX checksum")?,
        signature,
        file_size: u32_at(bytes, 32, "DEX file_size")?,
        header_size: u32_at(bytes, 36, "DEX header_size")?,
        endian_tag: u32_at(bytes, 40, "DEX endian_tag")?,
        link_size: u32_at(bytes, 44, "DEX link_size")?,
        link_off: u32_at(bytes, 48, "DEX link_off")?,
        map_off: u32_at(bytes, 52, "DEX map_off")?,
        string_ids_size: u32_at(bytes, 56, "DEX string_ids_size")?,
        string_ids_off: u32_at(bytes, 60, "DEX string_ids_off")?,
        type_ids_size: u32_at(bytes, 64, "DEX type_ids_size")?,
        type_ids_off: u32_at(bytes, 68, "DEX type_ids_off")?,
        proto_ids_size: u32_at(bytes, 72, "DEX proto_ids_size")?,
        proto_ids_off: u32_at(bytes, 76, "DEX proto_ids_off")?,
        field_ids_size: u32_at(bytes, 80, "DEX field_ids_size")?,
        field_ids_off: u32_at(bytes, 84, "DEX field_ids_off")?,
        method_ids_size: u32_at(bytes, 88, "DEX method_ids_size")?,
        method_ids_off: u32_at(bytes, 92, "DEX method_ids_off")?,
        class_defs_size: u32_at(bytes, 96, "DEX class_defs_size")?,
        class_defs_off: u32_at(bytes, 100, "DEX class_defs_off")?,
        data_size: u32_at(bytes, 104, "DEX data_size")?,
        data_off: u32_at(bytes, 108, "DEX data_off")?,
    })
}

fn parse_strings(bytes: &[u8], header: &DexHeader) -> Result<Vec<String>> {
    let count = header.string_ids_size as usize;
    let off = header.string_ids_off as usize;
    let mut strings = Vec::with_capacity(count);
    for i in 0..count {
        let item_off = off
            .checked_add(i * 4)
            .ok_or_else(|| Error::InvalidData("DEX string_ids offset overflow".into()))?;
        let data_off = u32_at(bytes, item_off, "DEX string_data_off")? as usize;
        strings.push(decode_mutf8_string(bytes, data_off)?);
    }
    Ok(strings)
}

fn parse_types(bytes: &[u8], header: &DexHeader) -> Result<Vec<u32>> {
    let count = header.type_ids_size as usize;
    let off = header.type_ids_off as usize;
    let mut types = Vec::with_capacity(count);
    for i in 0..count {
        let item_off = off
            .checked_add(i * 4)
            .ok_or_else(|| Error::InvalidData("DEX type_ids offset overflow".into()))?;
        types.push(u32_at(bytes, item_off, "DEX type descriptor_idx")?);
    }
    Ok(types)
}

fn parse_protos(bytes: &[u8], header: &DexHeader) -> Result<Vec<ProtoId>> {
    let count = header.proto_ids_size as usize;
    let off = header.proto_ids_off as usize;
    let mut protos = Vec::with_capacity(count);
    for i in 0..count {
        let item_off = off
            .checked_add(i * 12)
            .ok_or_else(|| Error::InvalidData("DEX proto_ids offset overflow".into()))?;
        let shorty_idx = u32_at(bytes, item_off, "DEX proto shorty_idx")?;
        let return_type_idx = u32_at(bytes, item_off + 4, "DEX proto return_type_idx")?;
        let parameters_off = u32_at(bytes, item_off + 8, "DEX proto parameters_off")?;
        let parameters = if parameters_off == 0 {
            Vec::new()
        } else {
            parse_type_list(bytes, parameters_off as usize, "DEX proto parameters")?
        };
        protos.push(ProtoId {
            shorty_idx,
            return_type_idx,
            parameters,
        });
    }
    Ok(protos)
}

fn parse_fields(bytes: &[u8], header: &DexHeader) -> Result<Vec<FieldId>> {
    let count = header.field_ids_size as usize;
    let off = header.field_ids_off as usize;
    let mut fields = Vec::with_capacity(count);
    for i in 0..count {
        let item_off = off
            .checked_add(i * 8)
            .ok_or_else(|| Error::InvalidData("DEX field_ids offset overflow".into()))?;
        let class_idx = u16_at(bytes, item_off, "DEX field class_idx")?;
        let type_idx = u16_at(bytes, item_off + 2, "DEX field type_idx")?;
        let name_idx = u32_at(bytes, item_off + 4, "DEX field name_idx")?;
        fields.push(FieldId {
            class_idx,
            type_idx,
            name_idx,
        });
    }
    Ok(fields)
}

fn parse_methods(bytes: &[u8], header: &DexHeader) -> Result<Vec<MethodId>> {
    let count = header.method_ids_size as usize;
    let off = header.method_ids_off as usize;
    let mut methods = Vec::with_capacity(count);
    for i in 0..count {
        let item_off = off
            .checked_add(i * 8)
            .ok_or_else(|| Error::InvalidData("DEX method_ids offset overflow".into()))?;
        let class_idx = u16_at(bytes, item_off, "DEX method class_idx")?;
        let proto_idx = u16_at(bytes, item_off + 2, "DEX method proto_idx")?;
        let name_idx = u32_at(bytes, item_off + 4, "DEX method name_idx")?;
        methods.push(MethodId {
            class_idx,
            proto_idx,
            name_idx,
        });
    }
    Ok(methods)
}

/// 解析 type_list（type_item 数组，前置 u32 数量）。
fn parse_type_list(bytes: &[u8], off: usize, what: &str) -> Result<Vec<u32>> {
    let size = u32_at(bytes, off, what)?.min(1_000_000) as usize; // 防御性上限
    let mut list = Vec::with_capacity(size);
    for i in 0..size {
        let item_off = off
            .checked_add(4 + i * 4)
            .ok_or_else(|| Error::InvalidData(format!("{what}: offset overflow")))?;
        list.push(u32_at(bytes, item_off, what)?);
    }
    Ok(list)
}

fn parse_class_defs(bytes: &[u8], header: &DexHeader) -> Result<Vec<ClassDef>> {
    let count = header.class_defs_size as usize;
    let off = header.class_defs_off as usize;
    let mut defs = Vec::with_capacity(count);
    for i in 0..count {
        let item_off = off
            .checked_add(i * 32)
            .ok_or_else(|| Error::InvalidData("DEX class_defs offset overflow".into()))?;
        let class_idx = u32_at(bytes, item_off, "DEX class_idx")?;
        let access_flags = u32_at(bytes, item_off + 4, "DEX class access_flags")?;
        let superclass_idx = u32_at(bytes, item_off + 8, "DEX class superclass_idx")?;
        let interfaces_off = u32_at(bytes, item_off + 12, "DEX class interfaces_off")?;
        let source_file_idx = u32_at(bytes, item_off + 16, "DEX class source_file_idx")?;
        let annotations_off = u32_at(bytes, item_off + 20, "DEX class annotations_off")?;
        let class_data_off = u32_at(bytes, item_off + 24, "DEX class class_data_off")?;
        let static_values_off = u32_at(bytes, item_off + 28, "DEX class static_values_off")?;
        let interfaces = if interfaces_off == 0 {
            Vec::new()
        } else {
            parse_type_list(bytes, interfaces_off as usize, "DEX class interfaces")?
        };
        let class_data = if class_data_off == 0 {
            None
        } else {
            Some(parse_class_data(bytes, class_data_off as usize)?)
        };
        defs.push(ClassDef {
            class_idx,
            access_flags,
            superclass_idx,
            interfaces,
            source_file_idx,
            annotations_off,
            class_data_off,
            static_values_off,
            class_data,
        });
    }
    Ok(defs)
}

fn parse_class_data(bytes: &[u8], off: usize) -> Result<ClassData> {
    let (static_size, mut cursor) = uleb128_at(bytes, off, "class_data static_fields_size")?;
    let (instance_size, c2) = uleb128_at(bytes, cursor, "class_data instance_fields_size")?;
    cursor = c2;
    let (direct_size, c3) = uleb128_at(bytes, cursor, "class_data direct_methods_size")?;
    cursor = c3;
    let (virtual_size, c4) = uleb128_at(bytes, cursor, "class_data virtual_methods_size")?;
    cursor = c4;

    // encoded_field：field_idx_diff + access_flags
    let mut static_fields = Vec::with_capacity(static_size as usize);
    let mut field_idx = 0u32;
    for _ in 0..static_size {
        let (diff, c) = uleb128_at(bytes, cursor, "class_data static field_idx_diff")?;
        cursor = c;
        field_idx = field_idx
            .checked_add(diff)
            .ok_or_else(|| Error::InvalidData("class_data field_idx overflow".into()))?;
        let (flags, c) = uleb128_at(bytes, cursor, "class_data static field access_flags")?;
        cursor = c;
        static_fields.push(EncodedField {
            field_idx,
            access_flags: flags,
        });
    }

    let mut instance_fields = Vec::with_capacity(instance_size as usize);
    field_idx = 0;
    for _ in 0..instance_size {
        let (diff, c) = uleb128_at(bytes, cursor, "class_data instance field_idx_diff")?;
        cursor = c;
        field_idx = field_idx
            .checked_add(diff)
            .ok_or_else(|| Error::InvalidData("class_data field_idx overflow".into()))?;
        let (flags, c) = uleb128_at(bytes, cursor, "class_data instance field access_flags")?;
        cursor = c;
        instance_fields.push(EncodedField {
            field_idx,
            access_flags: flags,
        });
    }

    // encoded_method：method_idx_diff + access_flags + code_off
    let mut direct_methods = Vec::with_capacity(direct_size as usize);
    let mut method_idx = 0u32;
    for _ in 0..direct_size {
        let (diff, c) = uleb128_at(bytes, cursor, "class_data direct method_idx_diff")?;
        cursor = c;
        method_idx = method_idx
            .checked_add(diff)
            .ok_or_else(|| Error::InvalidData("class_data method_idx overflow".into()))?;
        let (flags, c) = uleb128_at(bytes, cursor, "class_data direct method access_flags")?;
        cursor = c;
        let (code_off, c) = uleb128_at(bytes, cursor, "class_data direct method code_off")?;
        cursor = c;
        direct_methods.push(EncodedMethod {
            method_idx,
            access_flags: flags,
            code_off,
        });
    }

    let mut virtual_methods = Vec::with_capacity(virtual_size as usize);
    method_idx = 0;
    for _ in 0..virtual_size {
        let (diff, c) = uleb128_at(bytes, cursor, "class_data virtual method_idx_diff")?;
        cursor = c;
        method_idx = method_idx
            .checked_add(diff)
            .ok_or_else(|| Error::InvalidData("class_data method_idx overflow".into()))?;
        let (flags, c) = uleb128_at(bytes, cursor, "class_data virtual method access_flags")?;
        cursor = c;
        let (code_off, c) = uleb128_at(bytes, cursor, "class_data virtual method code_off")?;
        cursor = c;
        virtual_methods.push(EncodedMethod {
            method_idx,
            access_flags: flags,
            code_off,
        });
    }

    Ok(ClassData {
        static_fields,
        instance_fields,
        direct_methods,
        virtual_methods,
    })
}

// ---------------------------------------------------------------------------
// MUTF-8 解码
// ---------------------------------------------------------------------------

/// 解码 string_data_item：ULEB128 utf16 长度 + MUTF-8 字节（0 终止，不含）。
fn decode_mutf8_string(bytes: &[u8], off: usize) -> Result<String> {
    let (_utf16_len, cursor) = uleb128_at(bytes, off, "string utf16_size")?;
    let mut result = String::new();
    let mut i = cursor;
    while i < bytes.len() {
        let b = bytes[i];
        if b == 0 {
            break;
        }
        // MUTF-8 解码（支持编码 NUL 0xC0 0x80 与 surrogate pair）
        let (cp, next) = if b < 0x80 {
            (u32::from(b), i + 1)
        } else if b >> 5 == 0b110 {
            let b2 = *bytes
                .get(i + 1)
                .ok_or_else(|| Error::InvalidData("MUTF-8 truncated".into()))?;
            (((u32::from(b) & 0x1F) << 6) | (u32::from(b2) & 0x3F), i + 2)
        } else if b >> 4 == 0b1110 {
            let b2 = *bytes
                .get(i + 1)
                .ok_or_else(|| Error::InvalidData("MUTF-8 truncated".into()))?;
            let b3 = *bytes
                .get(i + 2)
                .ok_or_else(|| Error::InvalidData("MUTF-8 truncated".into()))?;
            (
                ((u32::from(b) & 0x0F) << 12)
                    | ((u32::from(b2) & 0x3F) << 6)
                    | (u32::from(b3) & 0x3F),
                i + 3,
            )
        } else {
            let b2 = *bytes
                .get(i + 1)
                .ok_or_else(|| Error::InvalidData("MUTF-8 truncated".into()))?;
            let b3 = *bytes
                .get(i + 2)
                .ok_or_else(|| Error::InvalidData("MUTF-8 truncated".into()))?;
            let b4 = *bytes
                .get(i + 3)
                .ok_or_else(|| Error::InvalidData("MUTF-8 truncated".into()))?;
            (
                ((u32::from(b) & 0x07) << 18)
                    | ((u32::from(b2) & 0x3F) << 12)
                    | ((u32::from(b3) & 0x3F) << 6)
                    | (u32::from(b4) & 0x3F),
                i + 4,
            )
        };
        // surrogate pair 组合（MUTF-8 用 3 字节编码 UTF-16 surrogate）
        if (0xD800..=0xDBFF).contains(&cp) {
            // 高代理：必须紧跟低代理
            if next >= bytes.len() || bytes[next] == 0 {
                return Err(Error::InvalidData("MUTF-8: lone high surrogate".into()));
            }
            let (low_cp, low_next) = if bytes[next] >> 4 == 0b1110 {
                let b2 = *bytes
                    .get(next + 1)
                    .ok_or_else(|| Error::InvalidData("MUTF-8 truncated".into()))?;
                let b3 = *bytes
                    .get(next + 2)
                    .ok_or_else(|| Error::InvalidData("MUTF-8 truncated".into()))?;
                (
                    ((u32::from(bytes[next]) & 0x0F) << 12)
                        | ((u32::from(b2) & 0x3F) << 6)
                        | (u32::from(b3) & 0x3F),
                    next + 3,
                )
            } else {
                return Err(Error::InvalidData("MUTF-8: bad surrogate pair".into()));
            };
            if !(0xDC00..=0xDFFF).contains(&low_cp) {
                return Err(Error::InvalidData("MUTF-8: bad low surrogate".into()));
            }
            let combined = 0x10000 + ((cp - 0xD800) << 10) + (low_cp - 0xDC00);
            let ch = char::from_u32(combined)
                .ok_or_else(|| Error::InvalidData("MUTF-8: invalid code point".into()))?;
            result.push(ch);
            i = low_next;
        } else {
            let ch = char::from_u32(cp)
                .ok_or_else(|| Error::InvalidData("MUTF-8: invalid code point".into()))?;
            result.push(ch);
            i = next;
        }
    }
    Ok(result)
}

// ---------------------------------------------------------------------------
// 测试
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    /// 构造最小 DEX：1 个字符串 "Lx;"、1 个 type、1 个 class_def（无 class_data）。
    fn minimal_dex() -> Vec<u8> {
        let mut buf = Vec::new();
        // header (112)
        buf.extend_from_slice(b"dex\n035\0");
        buf.extend_from_slice(&[0u8; 24]); // checksum + signature
        let file_size: u32 = 157;
        buf.extend_from_slice(&file_size.to_le_bytes()); // 32
        let header_size: u32 = 0x70;
        buf.extend_from_slice(&header_size.to_le_bytes()); // 36
        let endian: u32 = 0x1234_5678;
        buf.extend_from_slice(&endian.to_le_bytes()); // 40
        buf.extend_from_slice(&0u32.to_le_bytes()); // link_size 44
        buf.extend_from_slice(&0u32.to_le_bytes()); // link_off 48
        buf.extend_from_slice(&0u32.to_le_bytes()); // map_off 52
        buf.extend_from_slice(&1u32.to_le_bytes()); // string_ids_size 56
        buf.extend_from_slice(&112u32.to_le_bytes()); // string_ids_off 60
        buf.extend_from_slice(&1u32.to_le_bytes()); // type_ids_size 64
        buf.extend_from_slice(&116u32.to_le_bytes()); // type_ids_off 68
        buf.extend_from_slice(&0u32.to_le_bytes()); // proto_ids_size 72
        buf.extend_from_slice(&0u32.to_le_bytes()); // proto_ids_off 76
        buf.extend_from_slice(&0u32.to_le_bytes()); // field_ids_size 80
        buf.extend_from_slice(&0u32.to_le_bytes()); // field_ids_off 84
        buf.extend_from_slice(&0u32.to_le_bytes()); // method_ids_size 88
        buf.extend_from_slice(&0u32.to_le_bytes()); // method_ids_off 92
        buf.extend_from_slice(&1u32.to_le_bytes()); // class_defs_size 96
        buf.extend_from_slice(&120u32.to_le_bytes()); // class_defs_off 100
        buf.extend_from_slice(&5u32.to_le_bytes()); // data_size 104
        buf.extend_from_slice(&152u32.to_le_bytes()); // data_off 108
                                                      // string_ids[1] @112
        buf.extend_from_slice(&152u32.to_le_bytes());
        // type_ids[1] @116
        buf.extend_from_slice(&0u32.to_le_bytes());
        // class_defs[1] @120
        buf.extend_from_slice(&0u32.to_le_bytes()); // class_idx
        buf.extend_from_slice(&1u32.to_le_bytes()); // access_flags = public
        buf.extend_from_slice(&NO_INDEX.to_le_bytes()); // superclass_idx
        buf.extend_from_slice(&0u32.to_le_bytes()); // interfaces_off
        buf.extend_from_slice(&0u32.to_le_bytes()); // source_file_idx
        buf.extend_from_slice(&0u32.to_le_bytes()); // annotations_off
        buf.extend_from_slice(&0u32.to_le_bytes()); // class_data_off
        buf.extend_from_slice(&0u32.to_le_bytes()); // static_values_off
                                                    // string_data @152: utf16_size=3, "Lx;", NUL
        buf.push(3);
        buf.extend_from_slice(b"Lx;");
        buf.push(0);
        debug_assert_eq!(buf.len(), 157);
        buf
    }

    #[test]
    fn parses_minimal_dex() {
        let dex = DexFile::parse(&minimal_dex()).unwrap();
        assert_eq!(dex.header.version, "035");
        assert_eq!(dex.header.string_ids_size, 1);
        assert_eq!(dex.strings, vec!["Lx;"]);
        assert_eq!(dex.types, vec![0]);
        assert_eq!(dex.class_defs.len(), 1);
        let class = &dex.class_defs[0];
        assert_eq!(class.class_idx, 0);
        assert_eq!(class.access_flags, 1);
        assert_eq!(class.superclass_idx, NO_INDEX);
        assert!(class.class_data.is_none());
        assert_eq!(dex.class_name(0).as_deref(), Some("x"));
    }

    #[test]
    fn rejects_bad_magic() {
        let mut bytes = minimal_dex();
        bytes[0] = b'X';
        let err = DexFile::parse(&bytes).unwrap_err();
        assert!(err.to_string().contains("magic"));
    }

    #[test]
    fn rejects_truncated_file() {
        let bytes = minimal_dex();
        let err = DexFile::parse(&bytes[..60]).unwrap_err();
        assert!(err.to_string().contains("too small"));
    }

    #[test]
    fn decodes_mutf8_nul_and_surrogates() {
        // "a\u{0}b" + emoji 😀 (U+1F600 → surrogate pair D83D DE00 → MUTF-8 ED A0 BD ED B8 80)
        let mut bytes = vec![
            0x05, // utf16_size = 5
            b'a', 0xC0, 0x80, b'b', // a NUL b
            0xED, 0xA0, 0xBD, 0xED, 0xB8, 0x80, // 😀
            0x00, // 终止
        ];
        let _ = &mut bytes;
        let result = decode_mutf8_string(&bytes, 0).unwrap();
        assert_eq!(result, "a\u{0}b\u{1F600}");
    }

    #[test]
    fn pretty_names() {
        assert_eq!(pretty_type_name("Lcom/example/A;"), "com.example.A");
        assert_eq!(pretty_type_name("V"), "V");
        assert_eq!(pretty_type_name("[I"), "[I");
    }

    /// 构造带 class_data 的 DEX：类 A 有一个直接方法 run()V。
    #[test]
    fn parses_class_data() {
        // 布局：
        //   strings: 0="Lcom/example/A;" 1="run" 2="V" 3="()V"（shorty）
        //   types:   0→str0 1→str2
        //   proto:   shorty=3 return=1 params=0
        //   method:  class=0 proto=0 name=1
        //   class_def: class_data → static=0 instance=0 direct=1 virtual=0
        //   class_data @X: uleb(0) uleb(0) uleb(1) uleb(0) | method: diff=0 flags=1 code=0
        let strings = [
            "Lcom/example/A;".as_bytes().to_vec(),
            b"run".to_vec(),
            b"V".to_vec(),
            b"()V".to_vec(),
        ];
        let string_data: Vec<Vec<u8>> = strings
            .iter()
            .map(|s| {
                let mut v = vec![s.len() as u8]; // utf16_size（ASCII 场景等于字节数）
                v.extend_from_slice(s);
                v.push(0);
                v
            })
            .collect();

        // 偏移布局
        let header_end = 112usize;
        let string_ids_off = header_end; // 112
        let type_ids_off = string_ids_off + 4 * strings.len(); // 128
        let proto_ids_off = type_ids_off + 4 * 2; // 136
        let method_ids_off = proto_ids_off + 12; // 148
        let class_defs_off = method_ids_off + 8; // 156
        let mut data_off = class_defs_off + 32; // 188

        let mut string_offs = Vec::new();
        for sd in &string_data {
            string_offs.push(data_off as u32);
            data_off += sd.len();
        }
        let class_data_off = data_off; // 188 + sum
        let class_data = [0u8, 0, 1, 0, 0, 1, 0]; // sizes + diff=0 flags=1 code=0
        let total = class_data_off + class_data.len();

        let mut buf = Vec::with_capacity(total);
        buf.extend_from_slice(b"dex\n035\0");
        buf.extend_from_slice(&[0u8; 24]);
        buf.extend_from_slice(&(total as u32).to_le_bytes()); // file_size
        buf.extend_from_slice(&0x70u32.to_le_bytes()); // header_size
        buf.extend_from_slice(&0x1234_5678u32.to_le_bytes()); // endian
        buf.extend_from_slice(&[0u8; 12]); // link_size + link_off + map_off
        buf.extend_from_slice(&4u32.to_le_bytes()); // string_ids_size
        buf.extend_from_slice(&(string_ids_off as u32).to_le_bytes());
        buf.extend_from_slice(&2u32.to_le_bytes()); // type_ids_size
        buf.extend_from_slice(&(type_ids_off as u32).to_le_bytes());
        buf.extend_from_slice(&1u32.to_le_bytes()); // proto_ids_size
        buf.extend_from_slice(&(proto_ids_off as u32).to_le_bytes());
        buf.extend_from_slice(&0u32.to_le_bytes()); // field_ids_size
        buf.extend_from_slice(&0u32.to_le_bytes());
        buf.extend_from_slice(&1u32.to_le_bytes()); // method_ids_size
        buf.extend_from_slice(&(method_ids_off as u32).to_le_bytes());
        buf.extend_from_slice(&1u32.to_le_bytes()); // class_defs_size
        buf.extend_from_slice(&(class_defs_off as u32).to_le_bytes());
        buf.extend_from_slice(&0u32.to_le_bytes()); // data_size
        buf.extend_from_slice(&(data_off as u32).to_le_bytes());
        assert_eq!(buf.len(), 112);

        for off in &string_offs {
            buf.extend_from_slice(&off.to_le_bytes());
        }
        buf.extend_from_slice(&0u32.to_le_bytes()); // type 0 → str0
        buf.extend_from_slice(&2u32.to_le_bytes()); // type 1 → str2 "V"
        buf.extend_from_slice(&3u32.to_le_bytes()); // proto shorty → str3
        buf.extend_from_slice(&1u32.to_le_bytes()); // proto return → type1
        buf.extend_from_slice(&0u32.to_le_bytes()); // proto parameters_off
        buf.extend_from_slice(&0u16.to_le_bytes()); // method class_idx
        buf.extend_from_slice(&0u16.to_le_bytes()); // method proto_idx
        buf.extend_from_slice(&1u32.to_le_bytes()); // method name → str1 "run"
        buf.extend_from_slice(&0u32.to_le_bytes()); // class_idx
        buf.extend_from_slice(&1u32.to_le_bytes()); // access_flags
        buf.extend_from_slice(&NO_INDEX.to_le_bytes()); // superclass
        buf.extend_from_slice(&0u32.to_le_bytes()); // interfaces
        buf.extend_from_slice(&0u32.to_le_bytes()); // source_file
        buf.extend_from_slice(&0u32.to_le_bytes()); // annotations
        buf.extend_from_slice(&(class_data_off as u32).to_le_bytes());
        buf.extend_from_slice(&0u32.to_le_bytes()); // static_values
        assert_eq!(buf.len(), 188); // class_defs 结束位置（string_data 起点）

        for sd in &string_data {
            buf.extend_from_slice(sd);
        }
        assert_eq!(buf.len(), class_data_off);
        buf.extend_from_slice(&class_data);
        assert_eq!(buf.len(), total);

        let dex = DexFile::parse(&buf).unwrap();
        assert_eq!(dex.strings[0], "Lcom/example/A;");
        assert_eq!(dex.class_name(0).as_deref(), Some("com.example.A"));
        assert_eq!(dex.method_name(0), Some("run"));
        assert_eq!(dex.method_class(0).as_deref(), Some("com.example.A"));
        let class = &dex.class_defs[0];
        let cd = class.class_data.as_ref().expect("class_data present");
        assert!(cd.static_fields.is_empty());
        assert!(cd.virtual_methods.is_empty());
        assert_eq!(cd.direct_methods.len(), 1);
        assert_eq!(cd.direct_methods[0].method_idx, 0);
        assert_eq!(cd.direct_methods[0].access_flags, 1);
        assert_eq!(dex.all_class_names(), vec!["com.example.A"]);
        assert_eq!(dex.all_method_signatures(), vec!["com.example.A::run"]);
    }
}
