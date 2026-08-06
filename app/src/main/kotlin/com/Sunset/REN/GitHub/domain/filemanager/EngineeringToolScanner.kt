package com.Sunset.REN.GitHub.domain.filemanager

import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.Locale
import java.util.zip.ZipFile

/** Deterministic local metadata scanner for MT-style engineering entries. */
object EngineeringToolScanner {
    fun scan(entry: FileManagerEntry, toolId: FileToolId): EngineeringToolScanResult {
        val zipFacts = sourceZipFacts(entry)
        val firstBytes = when (toolId) {
            FileToolId.DexTools -> firstBytes(entry, DEX_SCAN_BYTE_LIMIT)
            FileToolId.ArscTools -> arscBytes(entry, ARSC_SCAN_BYTE_LIMIT)
            else -> null
        }
        return EngineeringToolScanResult(
            entry = entry,
            toolId = toolId,
            zipFacts = zipFacts,
            dexHeader = if (toolId == FileToolId.DexTools) scanDexHeader(firstBytes) else null,
            arscHeader = if (toolId == FileToolId.ArscTools) scanArscHeader(firstBytes, zipFacts) else null
        )
    }

    private fun scanDexHeader(bytes: ByteArray?): DexHeaderFacts? {
        bytes ?: return null
        val magicHex = bytes.take(8).joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
        val dexVersion = bytes.takeIf { it.size >= 8 && it[0] == 'd'.code.toByte() && it[1] == 'e'.code.toByte() && it[2] == 'x'.code.toByte() }
            ?.let { String(it.copyOfRange(4, 7)) }
        val stringIds = bytes.takeIf { it.size >= 60 }?.let { readUIntLe(it, 56) }
        val stringIdsOffset = bytes.takeIf { it.size >= 64 }?.let { readUIntLe(it, 60) }
        val typeIds = bytes.takeIf { it.size >= 68 }?.let { readUIntLe(it, 64) }
        val typeIdsOffset = bytes.takeIf { it.size >= 72 }?.let { readUIntLe(it, 68) }
        val fieldIds = bytes.takeIf { it.size >= 84 }?.let { readUIntLe(it, 80) }
        val fieldIdsOffset = bytes.takeIf { it.size >= 88 }?.let { readUIntLe(it, 84) }
        val methodIds = bytes.takeIf { it.size >= 92 }?.let { readUIntLe(it, 88) }
        val methodIdsOffset = bytes.takeIf { it.size >= 96 }?.let { readUIntLe(it, 92) }
        val classDefs = bytes.takeIf { it.size >= 100 }?.let { readUIntLe(it, 96) }
        val classDefsOffset = bytes.takeIf { it.size >= 104 }?.let { readUIntLe(it, 100) }
        return DexHeaderFacts(
            magicHex = magicHex,
            dexVersion = dexVersion,
            fileSize = bytes.takeIf { it.size >= 36 }?.let { readUIntLe(it, 32) },
            headerSize = bytes.takeIf { it.size >= 40 }?.let { readUIntLe(it, 36) },
            stringIds = stringIds,
            stringIdsOffset = stringIdsOffset,
            typeIds = typeIds,
            typeIdsOffset = typeIdsOffset,
            protoIds = bytes.takeIf { it.size >= 76 }?.let { readUIntLe(it, 72) },
            protoIdsOffset = bytes.takeIf { it.size >= 80 }?.let { readUIntLe(it, 76) },
            fieldIds = fieldIds,
            fieldIdsOffset = fieldIdsOffset,
            methodIds = methodIds,
            methodIdsOffset = methodIdsOffset,
            classDefs = classDefs,
            classDefsOffset = classDefsOffset,
            sampleStrings = readDexSampleStrings(bytes, stringIds, stringIdsOffset),
            sampleTypes = readDexSampleTypeDescriptors(bytes, typeIds, typeIdsOffset, stringIds, stringIdsOffset),
            sampleClasses = readDexSampleClassDescriptors(bytes, classDefs, classDefsOffset, typeIds, typeIdsOffset, stringIds, stringIdsOffset),
            sampleFields = readDexSampleFieldNames(bytes, fieldIds, fieldIdsOffset, stringIds, stringIdsOffset),
            sampleMethods = readDexSampleMethodNames(bytes, methodIds, methodIdsOffset, stringIds, stringIdsOffset)
        )
    }

    private fun readDexSampleStrings(bytes: ByteArray, stringIds: Long?, stringIdsOffset: Long?): List<String> {
        val count = stringIds?.coerceAtMost(DEX_SAMPLE_STRING_COUNT.toLong())?.toInt() ?: return emptyList()
        return (0 until count).mapNotNull { index ->
            readDexStringByIndex(bytes, index.toLong(), stringIds, stringIdsOffset)
        }
    }

    private fun readDexSampleTypeDescriptors(
        bytes: ByteArray,
        typeIds: Long?,
        typeIdsOffset: Long?,
        stringIds: Long?,
        stringIdsOffset: Long?
    ): List<String> {
        val count = typeIds?.coerceAtMost(DEX_SAMPLE_TYPE_COUNT.toLong())?.toInt() ?: return emptyList()
        val idsOffset = typeIdsOffset?.toInt() ?: return emptyList()
        if (idsOffset < 0 || idsOffset + count * UINT32_SIZE_INT > bytes.size) return emptyList()
        return (0 until count).mapNotNull { index ->
            val descriptorStringIndex = readUIntLe(bytes, idsOffset + index * UINT32_SIZE_INT)
            readDexStringByIndex(bytes, descriptorStringIndex, stringIds, stringIdsOffset)
        }
    }

    private fun readDexSampleClassDescriptors(
        bytes: ByteArray,
        classDefs: Long?,
        classDefsOffset: Long?,
        typeIds: Long?,
        typeIdsOffset: Long?,
        stringIds: Long?,
        stringIdsOffset: Long?
    ): List<String> {
        val count = classDefs?.coerceAtMost(DEX_SAMPLE_CLASS_COUNT.toLong())?.toInt() ?: return emptyList()
        val defsOffset = classDefsOffset?.toInt() ?: return emptyList()
        if (defsOffset < 0 || defsOffset + count * DEX_CLASS_DEF_ITEM_SIZE > bytes.size) return emptyList()
        return (0 until count).mapNotNull { index ->
            val classTypeIndex = readUIntLe(bytes, defsOffset + index * DEX_CLASS_DEF_ITEM_SIZE)
            readDexTypeDescriptorByIndex(bytes, classTypeIndex, typeIds, typeIdsOffset, stringIds, stringIdsOffset)
        }.distinct()
    }

    private fun readDexSampleFieldNames(
        bytes: ByteArray,
        fieldIds: Long?,
        fieldIdsOffset: Long?,
        stringIds: Long?,
        stringIdsOffset: Long?
    ): List<String> {
        val count = fieldIds?.coerceAtMost(DEX_SAMPLE_FIELD_COUNT.toLong())?.toInt() ?: return emptyList()
        val idsOffset = fieldIdsOffset?.toInt() ?: return emptyList()
        if (idsOffset < 0 || idsOffset + count * DEX_FIELD_ID_ITEM_SIZE > bytes.size) return emptyList()
        return (0 until count).mapNotNull { index ->
            val nameIndex = readUIntLe(bytes, idsOffset + index * DEX_FIELD_ID_ITEM_SIZE + DEX_FIELD_NAME_INDEX_OFFSET)
            readDexStringByIndex(bytes, nameIndex, stringIds, stringIdsOffset)
        }.distinct()
    }

    private fun readDexSampleMethodNames(
        bytes: ByteArray,
        methodIds: Long?,
        methodIdsOffset: Long?,
        stringIds: Long?,
        stringIdsOffset: Long?
    ): List<String> {
        val count = methodIds?.coerceAtMost(DEX_SAMPLE_METHOD_COUNT.toLong())?.toInt() ?: return emptyList()
        val idsOffset = methodIdsOffset?.toInt() ?: return emptyList()
        if (idsOffset < 0 || idsOffset + count * DEX_METHOD_ID_ITEM_SIZE > bytes.size) return emptyList()
        return (0 until count).mapNotNull { index ->
            val nameIndex = readUIntLe(bytes, idsOffset + index * DEX_METHOD_ID_ITEM_SIZE + DEX_METHOD_NAME_INDEX_OFFSET)
            readDexStringByIndex(bytes, nameIndex, stringIds, stringIdsOffset)
        }.distinct()
    }

    private fun readDexTypeDescriptorByIndex(
        bytes: ByteArray,
        typeIndex: Long,
        typeIds: Long?,
        typeIdsOffset: Long?,
        stringIds: Long?,
        stringIdsOffset: Long?
    ): String? {
        val typeCount = typeIds ?: return null
        val idsOffset = typeIdsOffset?.toInt() ?: return null
        if (typeIndex < 0 || typeIndex >= typeCount) return null
        val offset = idsOffset + typeIndex.toInt() * UINT32_SIZE_INT
        if (offset < 0 || offset + UINT32_SIZE_INT > bytes.size) return null
        val descriptorStringIndex = readUIntLe(bytes, offset)
        return readDexStringByIndex(bytes, descriptorStringIndex, stringIds, stringIdsOffset)
    }

    private fun readDexStringByIndex(bytes: ByteArray, index: Long, stringIds: Long?, stringIdsOffset: Long?): String? {
        val stringCount = stringIds ?: return null
        val idsOffset = stringIdsOffset?.toInt() ?: return null
        if (index < 0 || index >= stringCount) return null
        val offset = idsOffset + index.toInt() * UINT32_SIZE_INT
        if (offset < 0 || offset + UINT32_SIZE_INT > bytes.size) return null
        val stringDataOffset = readUIntLe(bytes, offset).toInt()
        return readDexStringData(bytes, stringDataOffset)
    }

    private fun readDexStringData(bytes: ByteArray, offset: Int): String? {
        if (offset < 0 || offset >= bytes.size) return null
        val uleb = readUleb128(bytes, offset) ?: return null
        val start = uleb.nextOffset
        if (start >= bytes.size) return ""
        val end = generateSequence(start) { it + 1 }
            .takeWhile { it < bytes.size && bytes[it].toInt() != 0 }
            .lastOrNull()
            ?.plus(1)
            ?: return null
        return runCatching {
            String(bytes.copyOfRange(start, end), Charsets.UTF_8).take(DEX_SAMPLE_STRING_MAX_LENGTH)
        }.getOrNull()
    }

    private fun readUleb128(bytes: ByteArray, offset: Int): Uleb128Read? {
        var result = 0
        var shift = 0
        var cursor = offset
        repeat(5) {
            if (cursor >= bytes.size) return null
            val value = bytes[cursor].toInt() and 0xFF
            result = result or ((value and 0x7F) shl shift)
            cursor++
            if ((value and 0x80) == 0) return Uleb128Read(result, cursor)
            shift += 7
        }
        return null
    }

    private fun scanArscHeader(bytes: ByteArray?, zipFacts: ZipFacts?): ArscHeaderFacts? {
        val arscInZip = zipFacts?.entries?.firstOrNull { it.name.equals("resources.arsc", ignoreCase = true) }
        bytes ?: return arscInZip?.let {
            ArscHeaderFacts(
                source = ArscSource.ApkZipEntry,
                zipEntrySize = it.size,
                zipEntryCompressedSize = it.compressedSize
            )
        }
        val headerType = bytes.takeIf { it.size >= 2 }?.let { readUShortLe(it, 0) }
        val headerSize = bytes.takeIf { it.size >= 4 }?.let { readUShortLe(it, 2) }
        val chunkSize = bytes.takeIf { it.size >= 8 }?.let { readUIntLe(it, 4) }
        val packageCount = bytes.takeIf { it.size >= 12 && headerType == RES_TABLE_TYPE }?.let { readUIntLe(it, 8) }
        val globalStringPool = headerSize?.let { scanArscStringPool(bytes, it) }
        val firstPackage = headerSize
            ?.let { start -> nextChunkOffset(bytes, start) }
            ?.let { offset -> findFirstArscPackage(bytes, offset, globalStringPool) }
        return ArscHeaderFacts(
            source = if (arscInZip != null) ArscSource.ApkZipEntry else ArscSource.DirectBytes,
            headerType = headerType,
            headerSize = headerSize,
            chunkSize = chunkSize,
            packageCount = packageCount,
            globalStringPool = globalStringPool,
            firstPackage = firstPackage,
            zipEntrySize = arscInZip?.size,
            zipEntryCompressedSize = arscInZip?.compressedSize
        )
    }

    private fun arscBytes(entry: FileManagerEntry, maxBytes: Int): ByteArray? {
        return when (val source = entry.source) {
            is FileEntrySource.LocalFile -> {
                val file = source.file.takeIf(File::isFile) ?: return null
                if (file.extension.equals("apk", ignoreCase = true) || file.extension.equals("zip", ignoreCase = true)) {
                    runCatching {
                        ZipFile(file).use { zip ->
                            val arsc = zip.getEntry("resources.arsc") ?: return@use null
                            zip.getInputStream(arsc).use { it.readAtMostBytes(maxBytes) }
                        }
                    }.getOrNull()
                } else {
                    file.inputStream().use { it.readAtMostBytes(maxBytes) }
                }
            }
            is FileEntrySource.RootPath -> null
            is FileEntrySource.ArchiveEntry -> runCatching {
                ZipFile(source.archiveFile).use { zip ->
                    val zipEntry = zip.getEntry(source.innerPath) ?: return@use null
                    zip.getInputStream(zipEntry).use { it.readAtMostBytes(maxBytes) }
                }
            }.getOrNull()
            else -> null
        }
    }

    private fun scanArscStringPool(
        bytes: ByteArray,
        offset: Int,
        sampleLimit: Int = ARSC_SAMPLE_STRING_COUNT
    ): ArscStringPoolFacts? {
        if (offset < 0 || offset + 28 > bytes.size) return null
        val type = readUShortLe(bytes, offset)
        if (type != RES_STRING_POOL_TYPE) return null
        val headerSize = readUShortLe(bytes, offset + 2)
        val chunkSize = readUIntLe(bytes, offset + 4)
        val stringCount = readUIntLe(bytes, offset + 8)
        val flags = readUIntLe(bytes, offset + 16)
        val stringsStart = readUIntLe(bytes, offset + 20)
        val utf8 = (flags and UTF8_FLAG) != 0L
        return ArscStringPoolFacts(
            offset = offset.toLong(),
            chunkSize = chunkSize,
            stringCount = stringCount,
            styleCount = readUIntLe(bytes, offset + 12),
            flags = flags,
            stringsStart = stringsStart,
            stylesStart = readUIntLe(bytes, offset + 24),
            utf8 = utf8,
            headerSize = headerSize,
            sampleStrings = readArscStringPoolSamples(
                bytes = bytes,
                poolOffset = offset,
                stringCount = stringCount,
                headerSize = headerSize,
                stringsStart = stringsStart,
                utf8 = utf8,
                sampleLimit = sampleLimit
            )
        )
    }

    private fun readArscStringPoolSamples(
        bytes: ByteArray,
        poolOffset: Int,
        stringCount: Long,
        headerSize: Int,
        stringsStart: Long,
        utf8: Boolean,
        sampleLimit: Int = ARSC_SAMPLE_STRING_COUNT
    ): List<String> {
        val count = stringCount.coerceAtMost(sampleLimit.toLong()).toInt()
        if (count <= 0) return emptyList()
        val offsetsStart = poolOffset + headerSize
        if (offsetsStart < 0 || offsetsStart + count * UINT32_SIZE_INT > bytes.size) return emptyList()
        val stringsBase = poolOffset + stringsStart.toInt()
        if (stringsBase < 0 || stringsBase >= bytes.size) return emptyList()
        return (0 until count).mapNotNull { index ->
            val relativeOffset = readUIntLe(bytes, offsetsStart + index * UINT32_SIZE_INT).toInt()
            val stringOffset = stringsBase + relativeOffset
            if (utf8) readArscUtf8String(bytes, stringOffset) else readArscUtf16String(bytes, stringOffset)
        }
    }

    private fun readArscUtf8String(bytes: ByteArray, offset: Int): String? {
        if (offset < 0 || offset >= bytes.size) return null
        val utf16Length = readUleb128(bytes, offset) ?: return null
        val utf8Length = readUleb128(bytes, utf16Length.nextOffset) ?: return null
        val start = utf8Length.nextOffset
        if (start < 0 || start >= bytes.size) return ""
        val end = (start + utf8Length.value).coerceAtMost(bytes.size)
        return runCatching {
            String(bytes.copyOfRange(start, end), Charsets.UTF_8).take(ARSC_SAMPLE_STRING_MAX_LENGTH)
        }.getOrNull()
    }

    private fun readArscUtf16String(bytes: ByteArray, offset: Int): String? {
        if (offset < 0 || offset + 2 > bytes.size) return null
        val length = readArscUtf16Length(bytes, offset) ?: return null
        val start = if ((readUShortLe(bytes, offset) and ARSC_UTF16_LENGTH_EXTENDED_FLAG) != 0) offset + 4 else offset + 2
        val byteLength = length * 2
        if (start < 0 || start + byteLength > bytes.size) return null
        return runCatching {
            String(bytes.copyOfRange(start, start + byteLength), Charsets.UTF_16LE).take(ARSC_SAMPLE_STRING_MAX_LENGTH)
        }.getOrNull()
    }

    private fun readArscUtf16Length(bytes: ByteArray, offset: Int): Int? {
        val first = readUShortLe(bytes, offset)
        return if ((first and ARSC_UTF16_LENGTH_EXTENDED_FLAG) != 0) {
            if (offset + 4 > bytes.size) null else ((first and ARSC_UTF16_LENGTH_VALUE_MASK) shl 16) or readUShortLe(bytes, offset + 2)
        } else {
            first
        }
    }

    private fun nextChunkOffset(bytes: ByteArray, offset: Int): Int? {
        if (offset < 0 || offset + 8 > bytes.size) return null
        val size = readUIntLe(bytes, offset + 4).toInt()
        if (size <= 0) return null
        return (offset + size).takeIf { it in 0..bytes.size }
    }

    private fun findFirstArscPackage(
        bytes: ByteArray,
        startOffset: Int,
        globalStringPool: ArscStringPoolFacts?
    ): ArscPackageFacts? {
        var cursor = startOffset
        while (cursor + 8 <= bytes.size) {
            val type = readUShortLe(bytes, cursor)
            val size = readUIntLe(bytes, cursor + 4).toInt()
            if (type == RES_TABLE_PACKAGE_TYPE) return scanArscPackage(bytes, cursor, globalStringPool)
            if (size <= 0) return null
            cursor += size
        }
        return null
    }

    private fun scanArscPackage(bytes: ByteArray, offset: Int, globalStringPool: ArscStringPoolFacts?): ArscPackageFacts? {
        if (offset < 0 || offset + 288 > bytes.size) return null
        val packageNameBytes = bytes.copyOfRange(offset + 12, offset + 12 + 256)
        val packageName = packageNameBytes.toUShortName()
        val headerSize = readUShortLe(bytes, offset + 2)
        val chunkSize = readUIntLe(bytes, offset + 4)
        val typeStringsOffset = readUIntLe(bytes, offset + 268)
        val keyStringsOffset = readUIntLe(bytes, offset + 276)
        val typeStringPool = scanArscStringPool(
            bytes = bytes,
            offset = offset + typeStringsOffset.toInt(),
            sampleLimit = ARSC_TYPE_STRING_SAMPLE_COUNT
        )
        val keyStringPool = scanArscStringPool(
            bytes = bytes,
            offset = offset + keyStringsOffset.toInt(),
            sampleLimit = ARSC_KEY_STRING_SAMPLE_COUNT
        )
        val chunkFacts = scanArscPackageChildChunks(
            bytes = bytes,
            packageOffset = offset,
            packageHeaderSize = headerSize,
            packageChunkSize = chunkSize,
            typeStringPool = typeStringPool,
            keyStringPool = keyStringPool,
            globalStringPool = globalStringPool
        )
        return ArscPackageFacts(
            offset = offset.toLong(),
            headerSize = headerSize,
            chunkSize = chunkSize,
            id = readUIntLe(bytes, offset + 8),
            name = packageName,
            typeStringsOffset = typeStringsOffset,
            keyStringsOffset = keyStringsOffset,
            typeStringPool = typeStringPool,
            keyStringPool = keyStringPool,
            childChunks = chunkFacts
        )
    }

    private fun scanArscPackageChildChunks(
        bytes: ByteArray,
        packageOffset: Int,
        packageHeaderSize: Int,
        packageChunkSize: Long,
        typeStringPool: ArscStringPoolFacts?,
        keyStringPool: ArscStringPoolFacts?,
        globalStringPool: ArscStringPoolFacts?
    ): List<ArscPackageChildChunkFacts> {
        val packageEnd = (packageOffset + packageChunkSize.toInt()).coerceAtMost(bytes.size)
        var cursor = packageOffset + packageHeaderSize
        val chunks = mutableListOf<ArscPackageChildChunkFacts>()
        while (cursor + RES_CHUNK_HEADER_SIZE <= packageEnd && chunks.size < ARSC_SAMPLE_CHILD_CHUNK_COUNT) {
            val type = readUShortLe(bytes, cursor)
            val headerSize = readUShortLe(bytes, cursor + 2)
            val chunkSize = readUIntLe(bytes, cursor + 4).toInt()
            if (chunkSize <= 0 || cursor + chunkSize > bytes.size) break
            if (type == RES_TABLE_TYPE_SPEC_TYPE || type == RES_TABLE_TYPE_TYPE) {
                val typeId = bytes.getOrNull(cursor + 8)?.toInt()?.and(0xFF)
                val entryCount = bytes.takeIf { cursor + 16 <= it.size }?.let { readUIntLe(it, cursor + 12) }
                val entryStart = if (type == RES_TABLE_TYPE_TYPE && cursor + 20 <= bytes.size) readUIntLe(bytes, cursor + 16) else null
                val sampleEntries = if (type == RES_TABLE_TYPE_TYPE) {
                    readArscEntrySamples(bytes, cursor, headerSize, entryCount, keyStringPool, globalStringPool)
                } else {
                    emptyList()
                }
                val configSummary = if (type == RES_TABLE_TYPE_TYPE) {
                    readArscConfigSummary(bytes, cursor)
                } else {
                    null
                }
                chunks += ArscPackageChildChunkFacts(
                    offset = cursor.toLong(),
                    headerSize = headerSize,
                    chunkSize = chunkSize.toLong(),
                    chunkType = type,
                    typeId = typeId,
                    typeName = typeId?.let { resolveArscTypeName(typeStringPool, it) },
                    entryCount = entryCount,
                    entryStart = entryStart,
                    nonEmptyEntryCount = if (type == RES_TABLE_TYPE_TYPE) countNonEmptyArscEntries(bytes, cursor, headerSize, entryCount) else null,
                    configSize = if (type == RES_TABLE_TYPE_TYPE && cursor + 24 <= bytes.size) readUIntLe(bytes, cursor + 20) else null,
                    configSummary = configSummary,
                    sampleEntries = sampleEntries
                )
            }
            cursor += chunkSize
        }
        return chunks
    }

    private fun readArscConfigSummary(bytes: ByteArray, chunkOffset: Int): String? {
        if (chunkOffset + ARSC_TYPE_CONFIG_OFFSET + 8 > bytes.size) return null
        val configOffset = chunkOffset + ARSC_TYPE_CONFIG_OFFSET
        val size = readUIntLe(bytes, configOffset)
        val imsi = readUIntLe(bytes, configOffset + 4)
        val locale = bytes.takeIf { configOffset + 16 <= it.size }?.let { readUIntLe(it, configOffset + 8) }
        val screenType = bytes.takeIf { configOffset + 20 <= it.size }?.let { readUIntLe(it, configOffset + 16) }
        val density = screenType?.let { (it shr 16) and 0xFFFF }
        return buildString {
            append("size=$size")
            density?.takeIf { it != 0L }?.let { append(" density=${it}dpi") }
            locale?.let(::decodeArscLocale)?.takeIf { it.isNotBlank() }?.let { append(" locale=$it") }
            if (imsi != 0L) append(" imsi=0x${imsi.toString(16)}")
        }
    }

    private fun decodeArscLocale(locale: Long): String {
        val language = charArrayOf((locale and 0xFF).toInt().toChar(), ((locale shr 8) and 0xFF).toInt().toChar())
            .joinToString("")
            .trim { it <= ' ' || it == '\u0000' }
        val region = charArrayOf(((locale shr 16) and 0xFF).toInt().toChar(), ((locale shr 24) and 0xFF).toInt().toChar())
            .joinToString("")
            .trim { it <= ' ' || it == '\u0000' }
        return listOf(language, region).filter { it.isNotBlank() }.joinToString("-")
    }

    private fun readArscEntrySamples(
        bytes: ByteArray,
        chunkOffset: Int,
        headerSize: Int,
        entryCount: Long?,
        keyStringPool: ArscStringPoolFacts?,
        globalStringPool: ArscStringPoolFacts?
    ): List<ArscEntrySampleFacts> {
        val count = entryCount?.coerceAtMost(ARSC_ENTRY_KEY_SCAN_LIMIT.toLong())?.toInt() ?: return emptyList()
        val offsetsStart = chunkOffset + headerSize
        if (offsetsStart < 0 || offsetsStart + count * UINT32_SIZE_INT > bytes.size) return emptyList()
        return (0 until count).mapNotNull { index ->
            val relativeEntryOffset = readUIntLe(bytes, offsetsStart + index * UINT32_SIZE_INT)
            if (relativeEntryOffset == ARSC_NO_ENTRY) return@mapNotNull null
            val entryOffset = offsetsStart + count * UINT32_SIZE_INT + relativeEntryOffset.toInt()
            if (entryOffset < 0 || entryOffset + ARSC_TABLE_ENTRY_MIN_SIZE > bytes.size) return@mapNotNull null
            val flags = readUShortLe(bytes, entryOffset + ARSC_TABLE_ENTRY_FLAGS_OFFSET)
            val keyIndex = readUIntLe(bytes, entryOffset + ARSC_TABLE_ENTRY_KEY_INDEX_OFFSET)
            val complex = (flags and ARSC_TABLE_ENTRY_FLAG_COMPLEX) != 0
            val valueOffset = entryOffset + ARSC_TABLE_ENTRY_MIN_SIZE
            val valueDataType = if (!complex) bytes.getOrNull(valueOffset + ARSC_RES_VALUE_DATA_TYPE_OFFSET)?.toInt()?.and(0xFF) else null
            val valueData = if (!complex) {
                bytes.takeIf { valueOffset + ARSC_RES_VALUE_MIN_SIZE <= it.size }?.let { readUIntLe(it, valueOffset + ARSC_RES_VALUE_DATA_OFFSET) }
            } else {
                null
            }
            ArscEntrySampleFacts(
                entryIndex = index,
                keyName = readArscStringPoolString(keyStringPool, keyIndex),
                flags = flags,
                complex = complex,
                valueDataType = valueDataType,
                valueDataTypeName = valueDataType?.let(::arscValueTypeName),
                valueData = valueData,
                valueDisplay = arscValueDisplay(valueDataType, valueData, globalStringPool),
                complexParent = if (complex && entryOffset + ARSC_TABLE_MAP_ENTRY_MIN_SIZE <= bytes.size) {
                    readUIntLe(bytes, entryOffset + ARSC_TABLE_MAP_ENTRY_PARENT_OFFSET)
                } else {
                    null
                },
                complexMapCount = if (complex && entryOffset + ARSC_TABLE_MAP_ENTRY_MIN_SIZE <= bytes.size) {
                    readUIntLe(bytes, entryOffset + ARSC_TABLE_MAP_ENTRY_COUNT_OFFSET)
                } else {
                    null
                },
                complexMaps = if (complex) {
                    readArscComplexMapSamples(bytes, entryOffset, globalStringPool)
                } else {
                    emptyList()
                }
            )
        }.distinctBy { it.entryIndex }
    }

    private fun readArscComplexMapSamples(
        bytes: ByteArray,
        entryOffset: Int,
        globalStringPool: ArscStringPoolFacts?
    ): List<ArscComplexMapSampleFacts> {
        if (entryOffset + ARSC_TABLE_MAP_ENTRY_MIN_SIZE > bytes.size) return emptyList()
        val mapCount = readUIntLe(bytes, entryOffset + ARSC_TABLE_MAP_ENTRY_COUNT_OFFSET)
            .coerceAtMost(ARSC_COMPLEX_MAP_SAMPLE_COUNT.toLong())
            .toInt()
        val mapsStart = entryOffset + ARSC_TABLE_MAP_ENTRY_MIN_SIZE
        if (mapCount <= 0 || mapsStart < 0 || mapsStart + mapCount * ARSC_TABLE_MAP_ITEM_SIZE > bytes.size) return emptyList()
        return (0 until mapCount).mapNotNull { index ->
            val mapOffset = mapsStart + index * ARSC_TABLE_MAP_ITEM_SIZE
            val nameRef = readUIntLe(bytes, mapOffset)
            val valueOffset = mapOffset + UINT32_SIZE_INT
            val valueType = bytes.getOrNull(valueOffset + ARSC_RES_VALUE_DATA_TYPE_OFFSET)?.toInt()?.and(0xFF)
            val valueData = bytes.takeIf { valueOffset + ARSC_RES_VALUE_MIN_SIZE <= it.size }
                ?.let { readUIntLe(it, valueOffset + ARSC_RES_VALUE_DATA_OFFSET) }
            ArscComplexMapSampleFacts(
                nameRef = nameRef,
                valueDataType = valueType,
                valueDataTypeName = valueType?.let(::arscValueTypeName),
                valueData = valueData,
                valueDisplay = arscValueDisplay(valueType, valueData, globalStringPool)
            )
        }
    }

    private fun arscValueDisplay(type: Int?, data: Long?, globalStringPool: ArscStringPoolFacts?): String? {
        if (type == null || data == null) return null
        return when (type) {
            0x01 -> "@0x${data.toString(16)}"
            0x02 -> "?0x${data.toString(16)}"
            0x03 -> readArscStringPoolString(globalStringPool, data)?.let { "\"$it\"" } ?: "string[$data]"
            0x10 -> data.toString()
            0x11 -> "0x${data.toString(16)}"
            0x12 -> if (data != 0L) "true" else "false"
            in 0x1c..0x1f -> "#${data.toString(16).padStart(8, '0')}"
            else -> "0x${data.toString(16)}"
        }
    }

    private fun arscValueTypeName(type: Int): String = when (type) {
        0x00 -> "TYPE_NULL"
        0x01 -> "TYPE_REFERENCE"
        0x02 -> "TYPE_ATTRIBUTE"
        0x03 -> "TYPE_STRING"
        0x04 -> "TYPE_FLOAT"
        0x05 -> "TYPE_DIMENSION"
        0x06 -> "TYPE_FRACTION"
        0x10 -> "TYPE_INT_DEC"
        0x11 -> "TYPE_INT_HEX"
        0x12 -> "TYPE_INT_BOOLEAN"
        0x1c -> "TYPE_INT_COLOR_ARGB8"
        0x1d -> "TYPE_INT_COLOR_RGB8"
        0x1e -> "TYPE_INT_COLOR_ARGB4"
        0x1f -> "TYPE_INT_COLOR_RGB4"
        else -> "TYPE_0x${type.toString(16)}"
    }

    private fun readArscStringPoolString(pool: ArscStringPoolFacts?, index: Long): String? {
        pool ?: return null
        if (index < 0 || index >= pool.stringCount) return null
        return pool.sampleStrings.getOrNull(index.toInt())
    }

    private fun countNonEmptyArscEntries(bytes: ByteArray, chunkOffset: Int, headerSize: Int, entryCount: Long?): Long? {
        val count = entryCount?.coerceAtMost(ARSC_ENTRY_OFFSET_SCAN_LIMIT.toLong())?.toInt() ?: return null
        if (count <= 0) return 0
        val offsetsStart = chunkOffset + headerSize
        if (offsetsStart < 0 || offsetsStart + count * UINT32_SIZE_INT > bytes.size) return null
        return (0 until count).count { index ->
            readUIntLe(bytes, offsetsStart + index * UINT32_SIZE_INT) != ARSC_NO_ENTRY
        }.toLong()
    }

    private fun resolveArscTypeName(typeStringPool: ArscStringPoolFacts?, typeId: Int): String? {
        if (typeId <= 0) return null
        return typeStringPool?.sampleStrings?.getOrNull(typeId - 1)
    }

    private fun ByteArray.toUShortName(): String {
        val chars = mutableListOf<Char>()
        var index = 0
        while (index + 1 < size) {
            val code = (this[index].toInt() and 0xFF) or ((this[index + 1].toInt() and 0xFF) shl 8)
            if (code == 0) break
            chars += code.toChar()
            index += 2
        }
        return chars.joinToString("")
    }

    private fun sourceZipFacts(entry: FileManagerEntry): ZipFacts? {
        val file = (entry.source as? FileEntrySource.LocalFile)?.file?.takeIf { it.isFile } ?: return null
        return runCatching {
            ZipFile(file).use { zip ->
                val entries = zip.entries().asSequence().toList()
                val names = entries.map { it.name }
                ZipFacts(
                    entryCount = entries.size,
                    entryNames = names,
                    entries = entries.map { ZipEntryFact(it.name, it.size.coerceAtLeast(0L), it.compressedSize.coerceAtLeast(0L)) },
                    hasDex = names.any { it.matches(Regex("classes(\\d*)\\.dex", RegexOption.IGNORE_CASE)) },
                    hasArsc = names.any { it.equals("resources.arsc", ignoreCase = true) },
                    hasV1Manifest = names.any { it.equals("META-INF/MANIFEST.MF", ignoreCase = true) },
                    v1SignatureFiles = names.filter { name ->
                        val upper = name.uppercase(Locale.ROOT)
                        upper.startsWith("META-INF/") && upper.endsWith(".SF")
                    },
                    v1CertificateFiles = names.filter { name ->
                        val upper = name.uppercase(Locale.ROOT)
                        upper.startsWith("META-INF/") && (upper.endsWith(".RSA") || upper.endsWith(".DSA") || upper.endsWith(".EC"))
                    },
                    apkSigningBlock = scanApkSigningBlock(file)
                )
            }
        }.getOrNull()
    }

    private fun firstBytes(entry: FileManagerEntry, maxBytes: Int): ByteArray? {
        return when (val source = entry.source) {
            is FileEntrySource.LocalFile -> source.file.takeIf(File::isFile)?.inputStream()?.use { it.readAtMostBytes(maxBytes) }
            is FileEntrySource.ArchiveEntry -> runCatching {
                ZipFile(source.archiveFile).use { zip ->
                    val zipEntry = zip.getEntry(source.innerPath) ?: return@use null
                    zip.getInputStream(zipEntry).use { it.readAtMostBytes(maxBytes) }
                }
            }.getOrNull()
            else -> null
        }
    }

    private fun InputStream.readAtMostBytes(maxBytes: Int): ByteArray {
        if (maxBytes <= 0) return ByteArray(0)
        val buffer = ByteArray(maxBytes)
        var offset = 0
        while (offset < maxBytes) {
            val read = read(buffer, offset, maxBytes - offset)
            if (read <= 0) break
            offset += read
        }
        return if (offset == maxBytes) buffer else buffer.copyOf(offset)
    }

    private fun scanApkSigningBlock(file: File): ApkSigningBlockFacts? {
        return runCatching {
            RandomAccessFile(file, "r").use { raf ->
                val fileLength = raf.length()
                val eocd = findEocd(raf, fileLength) ?: return@use ApkSigningBlockFacts(present = false)
                val centralDirectoryOffset = eocd.centralDirectoryOffset
                if (centralDirectoryOffset < APK_SIG_BLOCK_FOOTER_SIZE) {
                    return@use ApkSigningBlockFacts(present = false, centralDirectoryOffset = centralDirectoryOffset)
                }
                raf.seek(centralDirectoryOffset - APK_SIG_BLOCK_MAGIC_SIZE)
                val magic = ByteArray(APK_SIG_BLOCK_MAGIC_SIZE)
                raf.readFully(magic)
                if (!magic.contentEquals(APK_SIG_BLOCK_MAGIC)) {
                    return@use ApkSigningBlockFacts(present = false, centralDirectoryOffset = centralDirectoryOffset)
                }
                raf.seek(centralDirectoryOffset - APK_SIG_BLOCK_FOOTER_SIZE)
                val blockSize = raf.readLongLe()
                val blockOffset = centralDirectoryOffset - blockSize - U64_SIZE
                if (blockSize < APK_SIG_BLOCK_FOOTER_SIZE || blockOffset < 0) {
                    return@use ApkSigningBlockFacts(present = true, centralDirectoryOffset = centralDirectoryOffset)
                }
                val ids = readApkSigningBlockIds(raf, blockOffset, centralDirectoryOffset)
                ApkSigningBlockFacts(
                    present = true,
                    markerOffset = centralDirectoryOffset - APK_SIG_BLOCK_MAGIC_SIZE,
                    centralDirectoryOffset = centralDirectoryOffset,
                    blockOffset = blockOffset,
                    schemeIds = ids,
                    hasV2Id = APK_SIGNATURE_SCHEME_V2_BLOCK_ID in ids,
                    hasV3Id = APK_SIGNATURE_SCHEME_V3_BLOCK_ID in ids,
                    hasV31Id = APK_SIGNATURE_SCHEME_V31_BLOCK_ID in ids,
                    hasV4Marker = file.resolveSibling(file.name + ".idsig").exists()
                )
            }
        }.getOrNull()
    }

    private fun findEocd(raf: RandomAccessFile, fileLength: Long): EocdFacts? {
        val maxCommentLength = 0xFFFF
        val readSize = minOf(fileLength, (maxCommentLength + EOCD_MIN_SIZE).toLong()).toInt()
        if (readSize < EOCD_MIN_SIZE) return null
        val buffer = ByteArray(readSize)
        raf.seek(fileLength - readSize)
        raf.readFully(buffer)
        for (i in readSize - EOCD_MIN_SIZE downTo 0) {
            if (readUIntLe(buffer, i) == EOCD_SIGNATURE) {
                val commentLength = readUShortLe(buffer, i + 20)
                if (i + EOCD_MIN_SIZE + commentLength == readSize) {
                    return EocdFacts(
                        centralDirectoryOffset = readUIntLe(buffer, i + 16),
                        centralDirectorySize = readUIntLe(buffer, i + 12)
                    )
                }
            }
        }
        return null
    }

    private fun readApkSigningBlockIds(raf: RandomAccessFile, blockOffset: Long, centralDirectoryOffset: Long): List<Long> {
        val ids = mutableListOf<Long>()
        var cursor = blockOffset + U64_SIZE
        val pairsEnd = centralDirectoryOffset - APK_SIG_BLOCK_FOOTER_SIZE
        while (cursor + U64_SIZE + UINT32_SIZE <= pairsEnd) {
            raf.seek(cursor)
            val pairSize = raf.readLongLe()
            if (pairSize < UINT32_SIZE || cursor + U64_SIZE + pairSize > pairsEnd) break
            val id = raf.readIntLe().toLong() and 0xFFFFFFFFL
            ids += id
            cursor += U64_SIZE + pairSize
        }
        return ids
    }

    private fun RandomAccessFile.readLongLe(): Long {
        val bytes = ByteArray(8)
        readFully(bytes)
        return (bytes[0].toLong() and 0xFF) or
            ((bytes[1].toLong() and 0xFF) shl 8) or
            ((bytes[2].toLong() and 0xFF) shl 16) or
            ((bytes[3].toLong() and 0xFF) shl 24) or
            ((bytes[4].toLong() and 0xFF) shl 32) or
            ((bytes[5].toLong() and 0xFF) shl 40) or
            ((bytes[6].toLong() and 0xFF) shl 48) or
            ((bytes[7].toLong() and 0xFF) shl 56)
    }

    private fun RandomAccessFile.readIntLe(): Int {
        val bytes = ByteArray(4)
        readFully(bytes)
        return (bytes[0].toInt() and 0xFF) or
            ((bytes[1].toInt() and 0xFF) shl 8) or
            ((bytes[2].toInt() and 0xFF) shl 16) or
            ((bytes[3].toInt() and 0xFF) shl 24)
    }

    private fun readUShortLe(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun readUIntLe(bytes: ByteArray, offset: Int): Long {
        return (bytes[offset].toLong() and 0xFF) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 3].toLong() and 0xFF) shl 24)
    }
}

data class EngineeringToolScanResult(
    val entry: FileManagerEntry,
    val toolId: FileToolId,
    val zipFacts: ZipFacts? = null,
    val dexHeader: DexHeaderFacts? = null,
    val arscHeader: ArscHeaderFacts? = null
)

data class ZipFacts(
    val entryCount: Int,
    val entryNames: List<String>,
    val entries: List<ZipEntryFact>,
    val hasDex: Boolean,
    val hasArsc: Boolean,
    val hasV1Manifest: Boolean,
    val v1SignatureFiles: List<String>,
    val v1CertificateFiles: List<String>,
    val apkSigningBlock: ApkSigningBlockFacts?
)

data class ZipEntryFact(val name: String, val size: Long, val compressedSize: Long)

data class ApkSigningBlockFacts(
    val present: Boolean,
    val markerOffset: Long? = null,
    val centralDirectoryOffset: Long? = null,
    val blockOffset: Long? = null,
    val schemeIds: List<Long> = emptyList(),
    val hasV2Id: Boolean = false,
    val hasV3Id: Boolean = false,
    val hasV31Id: Boolean = false,
    val hasV4Marker: Boolean = false
)

data class DexHeaderFacts(
    val magicHex: String,
    val dexVersion: String?,
    val fileSize: Long?,
    val headerSize: Long?,
    val stringIds: Long?,
    val stringIdsOffset: Long?,
    val typeIds: Long?,
    val typeIdsOffset: Long?,
    val protoIds: Long?,
    val protoIdsOffset: Long?,
    val fieldIds: Long?,
    val fieldIdsOffset: Long?,
    val methodIds: Long?,
    val methodIdsOffset: Long?,
    val classDefs: Long?,
    val classDefsOffset: Long?,
    val sampleStrings: List<String>,
    val sampleTypes: List<String>,
    val sampleClasses: List<String>,
    val sampleFields: List<String>,
    val sampleMethods: List<String>
)

data class ArscHeaderFacts(
    val source: ArscSource,
    val headerType: Int? = null,
    val headerSize: Int? = null,
    val chunkSize: Long? = null,
    val packageCount: Long? = null,
    val globalStringPool: ArscStringPoolFacts? = null,
    val firstPackage: ArscPackageFacts? = null,
    val zipEntrySize: Long? = null,
    val zipEntryCompressedSize: Long? = null
)

data class ArscStringPoolFacts(
    val offset: Long,
    val chunkSize: Long,
    val stringCount: Long,
    val styleCount: Long,
    val flags: Long,
    val stringsStart: Long,
    val stylesStart: Long,
    val utf8: Boolean,
    val headerSize: Int,
    val sampleStrings: List<String> = emptyList()
)

data class ArscPackageFacts(
    val offset: Long,
    val headerSize: Int,
    val chunkSize: Long,
    val id: Long,
    val name: String,
    val typeStringsOffset: Long,
    val keyStringsOffset: Long,
    val typeStringPool: ArscStringPoolFacts? = null,
    val keyStringPool: ArscStringPoolFacts? = null,
    val childChunks: List<ArscPackageChildChunkFacts> = emptyList()
)

data class ArscPackageChildChunkFacts(
    val offset: Long,
    val headerSize: Int,
    val chunkSize: Long,
    val chunkType: Int,
    val typeId: Int?,
    val typeName: String?,
    val entryCount: Long?,
    val entryStart: Long?,
    val nonEmptyEntryCount: Long? = null,
    val configSize: Long? = null,
    val configSummary: String? = null,
    val sampleEntries: List<ArscEntrySampleFacts> = emptyList()
)

data class ArscEntrySampleFacts(
    val entryIndex: Int,
    val keyName: String?,
    val flags: Int,
    val complex: Boolean,
    val valueDataType: Int?,
    val valueDataTypeName: String?,
    val valueData: Long?,
    val valueDisplay: String?,
    val complexParent: Long? = null,
    val complexMapCount: Long? = null,
    val complexMaps: List<ArscComplexMapSampleFacts> = emptyList()
)

data class ArscComplexMapSampleFacts(
    val nameRef: Long,
    val valueDataType: Int?,
    val valueDataTypeName: String?,
    val valueData: Long?,
    val valueDisplay: String?
)

enum class ArscSource { DirectBytes, ApkZipEntry }

private data class Uleb128Read(
    val value: Int,
    val nextOffset: Int
)

private data class EocdFacts(
    val centralDirectoryOffset: Long,
    val centralDirectorySize: Long
)

private const val DEX_SCAN_BYTE_LIMIT = 512 * 1024
private const val ARSC_SCAN_BYTE_LIMIT = 512 * 1024
private const val DEX_SAMPLE_STRING_COUNT = 8
private const val DEX_SAMPLE_TYPE_COUNT = 8
private const val DEX_SAMPLE_CLASS_COUNT = 8
private const val DEX_SAMPLE_FIELD_COUNT = 8
private const val DEX_SAMPLE_METHOD_COUNT = 8
private const val DEX_SAMPLE_STRING_MAX_LENGTH = 80
private const val DEX_FIELD_ID_ITEM_SIZE = 8
private const val DEX_METHOD_ID_ITEM_SIZE = 8
private const val DEX_FIELD_NAME_INDEX_OFFSET = 4
private const val DEX_METHOD_NAME_INDEX_OFFSET = 4
private const val DEX_CLASS_DEF_ITEM_SIZE = 32
private const val UINT32_SIZE_INT = 4
private const val RES_STRING_POOL_TYPE = 0x0001
private const val RES_TABLE_TYPE = 0x0002
private const val RES_TABLE_PACKAGE_TYPE = 0x0200
private const val RES_TABLE_TYPE_TYPE = 0x0201
private const val RES_TABLE_TYPE_SPEC_TYPE = 0x0202
private const val RES_CHUNK_HEADER_SIZE = 8
private const val ARSC_TYPE_CONFIG_OFFSET = 20
private const val UTF8_FLAG = 0x00000100L
private const val ARSC_SAMPLE_STRING_COUNT = 8
private const val ARSC_TYPE_STRING_SAMPLE_COUNT = 64
private const val ARSC_KEY_STRING_SAMPLE_COUNT = 256
private const val ARSC_SAMPLE_CHILD_CHUNK_COUNT = 12
private const val ARSC_COMPLEX_MAP_SAMPLE_COUNT = 4
private const val ARSC_ENTRY_OFFSET_SCAN_LIMIT = 4096
private const val ARSC_ENTRY_KEY_SCAN_LIMIT = 64
private const val ARSC_TABLE_ENTRY_MIN_SIZE = 8
private const val ARSC_TABLE_ENTRY_FLAG_COMPLEX = 0x0001
private const val ARSC_TABLE_ENTRY_FLAGS_OFFSET = 2
private const val ARSC_TABLE_ENTRY_KEY_INDEX_OFFSET = 4
private const val ARSC_TABLE_MAP_ENTRY_MIN_SIZE = 16
private const val ARSC_TABLE_MAP_ENTRY_PARENT_OFFSET = 8
private const val ARSC_TABLE_MAP_ENTRY_COUNT_OFFSET = 12
private const val ARSC_TABLE_MAP_ITEM_SIZE = 12
private const val ARSC_RES_VALUE_MIN_SIZE = 8
private const val ARSC_RES_VALUE_DATA_TYPE_OFFSET = 3
private const val ARSC_RES_VALUE_DATA_OFFSET = 4
private const val ARSC_NO_ENTRY = 0xFFFFFFFFL
private const val ARSC_SAMPLE_STRING_MAX_LENGTH = 80
private const val ARSC_UTF16_LENGTH_EXTENDED_FLAG = 0x8000
private const val ARSC_UTF16_LENGTH_VALUE_MASK = 0x7FFF
private const val EOCD_SIGNATURE = 0x06054B50L
private const val EOCD_MIN_SIZE = 22
private const val U64_SIZE = 8L
private const val UINT32_SIZE = 4L
private const val APK_SIG_BLOCK_MAGIC_SIZE = 16
private const val APK_SIG_BLOCK_FOOTER_SIZE = 24L
private val APK_SIG_BLOCK_MAGIC = "APK Sig Block 42".toByteArray(Charsets.US_ASCII)
private const val APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 0x7109871aL
private const val APK_SIGNATURE_SCHEME_V3_BLOCK_ID = 0xf05368c0L
private const val APK_SIGNATURE_SCHEME_V31_BLOCK_ID = 0x1b93ad61L

