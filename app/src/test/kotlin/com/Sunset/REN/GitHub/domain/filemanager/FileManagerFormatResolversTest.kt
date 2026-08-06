package com.Sunset.REN.GitHub.domain.filemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class FileManagerFormatResolversTest {
    @Test
    fun archiveResolverHandlesCompoundExtensionsAndSignatures() {
        assertEquals(ArchiveFormat.Gzip, ArchiveFormatResolver.resolve("backup.tar.gz"))
        assertEquals(ArchiveFormat.Bzip2, ArchiveFormatResolver.resolve("backup.tar.bz2"))
        assertEquals(ArchiveFormat.Xz, ArchiveFormatResolver.resolve("backup.tar.xz"))
        assertEquals(ArchiveFormat.Ar, ArchiveFormatResolver.resolve("package.deb"))
        assertEquals(ArchiveFormat.Zip, ArchiveFormatResolver.resolve("doc.docx"))
        assertEquals(ArchiveFormat.Zstd, ArchiveFormatResolver.resolve("data.tar.zst"))
        assertEquals(ArchiveFormat.Lz4, ArchiveFormatResolver.resolve("data.lz4"))
        assertEquals(ArchiveFormat.Lzip, ArchiveFormatResolver.resolve("data.lz"))
        assertEquals(ArchiveFormat.Snappy, ArchiveFormatResolver.resolve("data.snappy"))
        assertEquals(ArchiveFormat.SplitArchive, ArchiveFormatResolver.resolve("backup.part01.rar"))
        assertEquals(ArchiveFormat.SplitArchive, ArchiveFormatResolver.resolve("backup.7z.001"))
        assertEquals(ArchiveFormat.SevenZip, ArchiveFormatResolver.resolve("blob", sampleBytes = byteArrayOf(0x37, 0x7A, 0xBC.toByte(), 0xAF.toByte(), 0x27, 0x1C)))
        assertEquals(ArchiveFormat.Rar, ArchiveFormatResolver.resolve("blob", sampleBytes = "Rar!\u001A\u0007\u0000".toByteArray(Charsets.ISO_8859_1)))
        assertEquals(ArchiveFormat.Zstd, ArchiveFormatResolver.resolve("blob", sampleBytes = byteArrayOf(0x28, 0xB5.toByte(), 0x2F, 0xFD.toByte())))
    }

    @Test
    fun typeResolverUsesArchiveResolverAndBinaryExtensions() {
        assertEquals(FileEntryType.Archive, FileEntryTypeResolver.resolve("package.deb"))
        assertEquals(FileEntryType.Archive, FileEntryTypeResolver.resolve("backup.tar.xz"))
        assertEquals(FileEntryType.Archive, FileEntryTypeResolver.resolve("backup.part02.rar"))
        assertEquals(FileEntryType.Archive, FileEntryTypeResolver.resolve("data.zst"))
        assertEquals(FileEntryType.Archive, FileEntryTypeResolver.resolve("sheet.xlsx"))
        assertEquals(FileEntryType.Binary, FileEntryTypeResolver.resolve("movie.mp4"))
        assertEquals(FileEntryType.Image, FileEntryTypeResolver.resolve("icon.tiff"))
    }

    @Test
    fun signatureSnifferRecognizesNoExtensionTextAndBinaryContainers() {
        assertEquals(FileEntryType.Code, FileSignatureSniffer.sniff("#!/usr/bin/env bash\necho ok\n".toByteArray()))
        assertEquals(FileEntryType.Code, FileSignatureSniffer.sniff("<?xml version=\"1.0\"?><root/>".toByteArray()))
        assertEquals(FileEntryType.Text, FileSignatureSniffer.sniff("plain text without suffix\n".toByteArray()))
        assertEquals(FileEntryType.Binary, FileSignatureSniffer.sniff("SQLite format 3\u0000".toByteArray(Charsets.ISO_8859_1)))
        assertEquals(FileEntryType.Binary, FileSignatureSniffer.sniff(byteArrayOf(0, 0, 0, 24) + "ftypisom".toByteArray()))
        assertFalse(FileSignatureSniffer.isLikelyText(byteArrayOf(0, 1, 2, 3, 4, 5)))
    }

    @Test
    fun textEncodingDetectsUtfBomAndLegacyCharsets() {
        val utf8Bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "hello".toByteArray(Charsets.UTF_8)
        val decodedBom = FileTextEncodingPolicy.decode(utf8Bom)
        assertTrue(decodedBom.hadBom)
        assertEquals(Charsets.UTF_8, decodedBom.charset)
        assertEquals("hello", decodedBom.content)

        val gb18030 = Charset.forName("GB18030")
        val chinese = "中文内容".toByteArray(gb18030)
        val decodedChinese = FileTextEncodingPolicy.decode(chinese)
        assertEquals("中文内容", decodedChinese.content)
        assertEquals(gb18030.name(), decodedChinese.charset.name())
    }

    @Test
    fun textConverterHandlesCommonTransforms() {
        assertEquals("a\r\nb\r\n", TextFormatConverter.convert("a\nb\n", TextFormatConverter.Action.LineEndingCrLf).getOrThrow())
        assertEquals("a\n", TextFormatConverter.convert("a", TextFormatConverter.Action.EnsureFinalNewline).getOrThrow())
        assertEquals("hello world", TextFormatConverter.convert("hello%20world", TextFormatConverter.Action.UrlDecode).getOrThrow())
        assertEquals("hello", TextFormatConverter.convert("aGVsbG8=", TextFormatConverter.Action.Base64Decode).getOrThrow())
    }
}
