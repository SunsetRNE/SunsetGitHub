package com.Sunset.REN.GitHub.domain.filemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileSignatureSnifferTest {
    @Test
    fun detectsImageSignatures() {
        assertEquals(FileEntryType.Image, FileSignatureSniffer.sniff(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47), "image"))
        assertEquals(FileEntryType.Image, FileSignatureSniffer.sniff(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()), "image"))
        assertEquals(FileEntryType.Image, FileSignatureSniffer.sniff("GIF89a".toByteArray(), "image"))
    }

    @Test
    fun detectsArchiveAndApkSignatures() {
        val zipHeader = byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 3, 4)
        assertEquals(FileEntryType.Archive, FileSignatureSniffer.sniff(zipHeader, "archive.zip"))
        assertEquals(FileEntryType.Apk, FileSignatureSniffer.sniff(zipHeader, "app.apk"))
    }

    @Test
    fun detectsBinarySignaturesAndTextSamples() {
        assertEquals(FileEntryType.Binary, FileSignatureSniffer.sniff(byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte()), "native"))
        assertEquals(FileEntryType.Binary, FileSignatureSniffer.sniff("dex\n035".toByteArray(), "classes.dex"))
        assertEquals(FileEntryType.Text, FileSignatureSniffer.sniff("hello\nworld".toByteArray(), "unknown"))
    }

    @Test
    fun likelyTextRejectsNulBytesAndAllowsNormalControls() {
        assertTrue(FileSignatureSniffer.isLikelyText("line1\nline2\tvalue".toByteArray()))
        assertFalse(FileSignatureSniffer.isLikelyText(byteArrayOf('a'.code.toByte(), 0, 'b'.code.toByte())))
    }
}
