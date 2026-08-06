package com.Sunset.REN.GitHub.domain.filemanager

/**
 * Lightweight file signature and text/binary detector used by the in-app file manager.
 *
 * This class deliberately avoids Android dependencies so the rules can be covered by
 * local unit tests and reused by both local File and SAF/content Uri readers.
 */
object FileSignatureSniffer {
    private const val MaxSuspiciousControlBytePercent = 10

    fun sniff(sample: ByteArray, fileName: String = "", mimeType: String? = null): FileEntryType? {
        if (sample.isEmpty()) return null
        val normalizedMimeType = mimeType.orEmpty().lowercase()
        when {
            normalizedMimeType.startsWith("image/") -> return FileEntryType.Image
            normalizedMimeType == "application/pdf" -> return FileEntryType.Binary
            normalizedMimeType in archiveMimeTypes -> return archiveTypeFor(fileName)
            normalizedMimeType == "application/vnd.android.package-archive" -> return FileEntryType.Apk
        }

        return when {
            sample.startsWith(0x89, 0x50, 0x4E, 0x47) -> FileEntryType.Image // PNG
            sample.startsWith(0xFF, 0xD8, 0xFF) -> FileEntryType.Image // JPEG
            sample.startsWithAscii("GIF87a") || sample.startsWithAscii("GIF89a") -> FileEntryType.Image
            sample.startsWithAscii("RIFF") && sample.size >= 12 && sample.copyOfRange(8, 12).toAsciiString() == "WEBP" -> FileEntryType.Image
            sample.startsWithAscii("BM") -> FileEntryType.Image
            sample.startsWithAscii("ftyp", offset = 4) -> FileEntryType.Binary // MP4/HEIF/AVIF container family
            sample.startsWithAscii("%PDF") -> FileEntryType.Binary
            sample.startsWith(0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1) -> FileEntryType.Binary // OLE2 Office
            sample.startsWithAscii("PK\u0003\u0004") || sample.startsWithAscii("PK\u0005\u0006") || sample.startsWithAscii("PK\u0007\u0008") -> archiveTypeFor(fileName)
            sample.startsWith(0x1F, 0x8B) -> FileEntryType.Archive
            sample.startsWithAscii("BZh") -> FileEntryType.Archive
            sample.startsWith(0xFD, 0x37, 0x7A, 0x58, 0x5A, 0x00) -> FileEntryType.Archive
            sample.startsWith(0x28, 0xB5, 0x2F, 0xFD) -> FileEntryType.Archive
            sample.startsWith(0x04, 0x22, 0x4D, 0x18) -> FileEntryType.Archive
            sample.startsWith(0x4C, 0x5A, 0x49, 0x50) -> FileEntryType.Archive
            sample.startsWith(0xFF, 0x06, 0x00, 0x00) || sample.startsWith(0x82, 0x53, 0x4E, 0x41, 0x50, 0x50, 0x59) -> FileEntryType.Archive
            sample.startsWithAscii("7z\u00BC\u00AF\u0027\u001C") -> FileEntryType.Archive
            sample.startsWithAscii("Rar!\u001A\u0007\u0000") || sample.startsWithAscii("Rar!\u001A\u0007\u0001\u0000") -> FileEntryType.Archive
            sample.size >= 265 && sample.copyOfRange(257, 262).toString(Charsets.US_ASCII) == "ustar" -> FileEntryType.Archive
            sample.startsWithAscii("070701") || sample.startsWithAscii("070702") || sample.startsWithAscii("070707") -> FileEntryType.Archive
            sample.startsWithAscii("!<arch>\n") -> FileEntryType.Archive
            sample.startsWith(0x7F, 0x45, 0x4C, 0x46) -> FileEntryType.Binary // ELF
            sample.startsWithAscii("dex\n") -> FileEntryType.Binary
            sample.startsWithAscii("SQLite format 3\u0000") -> FileEntryType.Binary
            sample.startsWithAscii("ID3") || sample.startsWith(0xFF, 0xFB) || sample.startsWith(0xFF, 0xF3) || sample.startsWith(0xFF, 0xF2) -> FileEntryType.Binary
            sample.startsWithAscii("OggS") || sample.startsWithAscii("fLaC") || sample.startsWithAscii("MThd") -> FileEntryType.Binary
            sample.startsWithAscii("RIFF") && sample.size >= 12 && sample.copyOfRange(8, 12).toAsciiString() in setOf("WAVE", "AVI ") -> FileEntryType.Binary
            sample.startsWithAscii("\u0000asm") -> FileEntryType.Binary
            sample.startsWithAscii("wOFF") || sample.startsWithAscii("wOF2") || sample.startsWith(0x00, 0x01, 0x00, 0x00) || sample.startsWithAscii("OTTO") -> FileEntryType.Binary
            looksLikeCodeText(sample) -> FileEntryType.Code
            isLikelyText(sample) -> FileEntryType.Text
            else -> FileEntryType.Binary
        }
    }

    fun isLikelyText(sample: ByteArray): Boolean {
        if (sample.isEmpty()) return true
        if (sample.startsWith(0xEF, 0xBB, 0xBF)) return true
        if (sample.startsWith(0xFE, 0xFF) || sample.startsWith(0xFF, 0xFE)) return true
        var suspiciousControlBytes = 0
        sample.forEach { byte ->
            val value = byte.toInt() and 0xFF
            if (value == 0) return false
            val isAllowedControl = value == 9 || value == 10 || value == 12 || value == 13
            if (value < 32 && !isAllowedControl) suspiciousControlBytes++
        }
        return suspiciousControlBytes * 100 <= sample.size * MaxSuspiciousControlBytePercent
    }

    private fun looksLikeCodeText(sample: ByteArray): Boolean {
        if (!isLikelyText(sample)) return false
        val prefix = sample
            .take(256)
            .toByteArray()
            .toString(Charsets.UTF_8)
            .trimStart('\uFEFF', ' ', '\t', '\r', '\n')
            .lowercase()
        return prefix.startsWith("#!") ||
            prefix.startsWith("<?xml") ||
            prefix.startsWith("<!doctype html") ||
            prefix.startsWith("<html") ||
            prefix.startsWith("#!/usr/bin/env") ||
            prefix.startsWith("#!/bin/") ||
            prefix.startsWith("#!/usr/bin/")
    }

    private fun archiveTypeFor(fileName: String): FileEntryType {
        return if (fileName.endsWith(".apk", ignoreCase = true)) FileEntryType.Apk else FileEntryType.Archive
    }

    private fun ByteArray.startsWith(vararg bytes: Int): Boolean {
        if (size < bytes.size) return false
        return bytes.indices.all { index -> (this[index].toInt() and 0xFF) == bytes[index] }
    }

    private fun ByteArray.startsWithAscii(prefix: String, offset: Int = 0): Boolean {
        val bytes = prefix.toByteArray(Charsets.ISO_8859_1)
        if (size < offset + bytes.size) return false
        return bytes.indices.all { index -> this[offset + index] == bytes[index] }
    }

    private fun ByteArray.toAsciiString(): String = toString(Charsets.US_ASCII)

    private val archiveMimeTypes = setOf(
        "application/zip",
        "application/x-zip-compressed",
        "application/java-archive",
        "application/x-tar",
        "application/gzip",
        "application/x-gzip",
        "application/x-7z-compressed",
        "application/vnd.rar"
    )
}
