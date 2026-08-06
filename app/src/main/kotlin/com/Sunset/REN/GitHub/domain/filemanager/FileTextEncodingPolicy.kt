package com.Sunset.REN.GitHub.domain.filemanager

import java.nio.charset.Charset

object FileTextEncodingPolicy {
    enum class LineEnding(val sequence: String) {
        Lf("\n"),
        CrLf("\r\n"),
        Cr("\r"),
        Mixed("\n"),
        None("\n")
    }

    data class DecodedText(
        val content: String,
        val charset: Charset,
        val hadBom: Boolean,
        val lineEnding: LineEnding
    )

    fun decode(bytes: ByteArray, fallbackCharset: Charset = Charsets.UTF_8): DecodedText {
        val detection = detectCharset(bytes, fallbackCharset)
        val contentBytes = if (detection.bomLength > 0) bytes.copyOfRange(detection.bomLength, bytes.size) else bytes
        val content = contentBytes.toString(detection.charset)
        return DecodedText(
            content = content,
            charset = detection.charset,
            hadBom = detection.bomLength > 0,
            lineEnding = detectLineEnding(content)
        )
    }

    fun encode(
        content: String,
        charset: Charset,
        preserveBom: Boolean,
        lineEnding: LineEnding
    ): ByteArray {
        val normalizedContent = normalizeLineEndings(content, lineEnding)
        val contentBytes = normalizedContent.toByteArray(charset)
        val bom = if (preserveBom) bomFor(charset) else ByteArray(0)
        return if (bom.isEmpty()) contentBytes else bom + contentBytes
    }

    fun normalizeLineEndings(content: String, lineEnding: LineEnding): String {
        if (lineEnding == LineEnding.Mixed || lineEnding == LineEnding.None) return content
        val placeholder = '\u0000'
        return content
            .replace("\r\n", placeholder.toString())
            .replace("\r", "\n")
            .replace(placeholder.toString(), "\n")
            .replace("\n", lineEnding.sequence)
    }

    fun detectLineEnding(content: String): LineEnding {
        var crlf = 0
        var lf = 0
        var cr = 0
        var index = 0
        while (index < content.length) {
            when (content[index]) {
                '\r' -> {
                    if (index + 1 < content.length && content[index + 1] == '\n') {
                        crlf += 1
                        index += 2
                    } else {
                        cr += 1
                        index += 1
                    }
                }
                '\n' -> {
                    lf += 1
                    index += 1
                }
                else -> index += 1
            }
        }
        val used = listOf(crlf > 0, lf > 0, cr > 0).count { it }
        return when {
            used == 0 -> LineEnding.None
            used > 1 -> LineEnding.Mixed
            crlf > 0 -> LineEnding.CrLf
            cr > 0 -> LineEnding.Cr
            else -> LineEnding.Lf
        }
    }

    private data class CharsetDetection(
        val charset: Charset,
        val bomLength: Int
    )

    private fun detectCharset(bytes: ByteArray, fallbackCharset: Charset): CharsetDetection {
        return when {
            bytes.startsWith(0xEF, 0xBB, 0xBF) -> CharsetDetection(Charsets.UTF_8, 3)
            bytes.startsWith(0xFE, 0xFF) -> CharsetDetection(Charsets.UTF_16BE, 2)
            bytes.startsWith(0xFF, 0xFE) -> CharsetDetection(Charsets.UTF_16LE, 2)
            looksLikeUtf16Le(bytes) -> CharsetDetection(Charsets.UTF_16LE, 0)
            looksLikeUtf16Be(bytes) -> CharsetDetection(Charsets.UTF_16BE, 0)
            isValidUtf8(bytes) -> CharsetDetection(Charsets.UTF_8, 0)
            fallbackCharset != Charsets.UTF_8 -> CharsetDetection(fallbackCharset, 0)
            else -> detectLegacyCharset(bytes)?.let { CharsetDetection(it, 0) } ?: CharsetDetection(fallbackCharset, 0)
        }
    }

    private fun bomFor(charset: Charset): ByteArray {
        return when (charset.name().uppercase()) {
            "UTF-8" -> byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
            "UTF-16BE" -> byteArrayOf(0xFE.toByte(), 0xFF.toByte())
            "UTF-16LE" -> byteArrayOf(0xFF.toByte(), 0xFE.toByte())
            else -> ByteArray(0)
        }
    }

    private fun isValidUtf8(bytes: ByteArray): Boolean {
        return runCatching {
            val decoder = Charsets.UTF_8.newDecoder()
            decoder.decode(java.nio.ByteBuffer.wrap(bytes))
            true
        }.getOrDefault(false)
    }

    private fun detectLegacyCharset(bytes: ByteArray): Charset? {
        if (bytes.isEmpty() || bytes.all { (it.toInt() and 0x80) == 0 }) return null
        return legacyCharsetCandidates
            .mapNotNull { name -> runCatching { Charset.forName(name) }.getOrNull() }
            .firstOrNull { charset -> bytes.roundTripsWith(charset) }
    }

    private fun ByteArray.roundTripsWith(charset: Charset): Boolean {
        val decoded = toString(charset)
        if (decoded.count { it == '\uFFFD' } > size / 20) return false
        return decoded.toByteArray(charset).contentEquals(this)
    }

    private fun ByteArray.startsWith(vararg values: Int): Boolean {
        if (size < values.size) return false
        return values.indices.all { index -> this[index] == values[index].toByte() }
    }

    private fun looksLikeUtf16Le(bytes: ByteArray): Boolean {
        val pairs = bytes.size / 2
        if (pairs < 4) return false
        val zeroOdd = (1 until bytes.size step 2).count { bytes[it] == 0.toByte() }
        return zeroOdd >= pairs / 2 && zeroOdd > (0 until bytes.size step 2).count { bytes[it] == 0.toByte() } * 2
    }

    private fun looksLikeUtf16Be(bytes: ByteArray): Boolean {
        val pairs = bytes.size / 2
        if (pairs < 4) return false
        val zeroEven = (0 until bytes.size step 2).count { bytes[it] == 0.toByte() }
        return zeroEven >= pairs / 2 && zeroEven > (1 until bytes.size step 2).count { bytes[it] == 0.toByte() } * 2
    }

    private val legacyCharsetCandidates = listOf("GB18030", "GBK", "Shift_JIS")
}
