package com.Sunset.REN.GitHub.domain.filemanager

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileTextEncodingPolicyTest {
    @Test
    fun decodeDetectsUtf8BomAndCrLf() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "a\r\nb\r\n".toByteArray(Charsets.UTF_8)

        val decoded = FileTextEncodingPolicy.decode(bytes)

        assertEquals("a\r\nb\r\n", decoded.content)
        assertEquals(Charsets.UTF_8, decoded.charset)
        assertTrue(decoded.hadBom)
        assertEquals(FileTextEncodingPolicy.LineEnding.CrLf, decoded.lineEnding)
    }

    @Test
    fun decodeDetectsUtf16LeBom() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "hello\n".toByteArray(Charsets.UTF_16LE)

        val decoded = FileTextEncodingPolicy.decode(bytes)

        assertEquals("hello\n", decoded.content)
        assertEquals(Charsets.UTF_16LE, decoded.charset)
        assertTrue(decoded.hadBom)
        assertEquals(FileTextEncodingPolicy.LineEnding.Lf, decoded.lineEnding)
    }

    @Test
    fun encodePreservesBomAndLineEnding() {
        val encoded = FileTextEncodingPolicy.encode(
            content = "a\nb\n",
            charset = Charsets.UTF_8,
            preserveBom = true,
            lineEnding = FileTextEncodingPolicy.LineEnding.CrLf
        )

        assertArrayEquals(
            byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "a\r\nb\r\n".toByteArray(Charsets.UTF_8),
            encoded
        )
    }

    @Test
    fun mixedLineEndingsAreNotNormalizedOnSave() {
        val content = "a\nb\r\nc\r"

        val normalized = FileTextEncodingPolicy.normalizeLineEndings(content, FileTextEncodingPolicy.LineEnding.Mixed)

        assertEquals(content, normalized)
        assertEquals(FileTextEncodingPolicy.LineEnding.Mixed, FileTextEncodingPolicy.detectLineEnding(content))
    }

    @Test
    fun plainUtf8WithoutBomKeepsBomDisabled() {
        val decoded = FileTextEncodingPolicy.decode("hello".toByteArray(Charsets.UTF_8))

        assertFalse(decoded.hadBom)
        assertEquals(Charsets.UTF_8, decoded.charset)
        assertEquals(FileTextEncodingPolicy.LineEnding.None, decoded.lineEnding)
    }
}
