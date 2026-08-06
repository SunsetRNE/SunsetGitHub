package com.Sunset.REN.GitHub.domain.filemanager

object ArchiveFormatResolver {
    fun resolve(name: String, mimeType: String? = null, sampleBytes: ByteArray? = null): ArchiveFormat? {
        val normalizedName = name.substringAfterLast('/').lowercase()
        val normalizedMimeType = mimeType.orEmpty().lowercase()
        val bySplitName = resolveSplitArchiveName(normalizedName)
        if (bySplitName != null) return bySplitName
        val bySignature = sampleBytes?.let(::resolveBySignature)
        if (bySignature != null) return bySignature
        val byMimeType = resolveByMimeType(normalizedMimeType)
        if (byMimeType != null) return byMimeType
        return ArchiveFormat.entries.firstOrNull { format ->
            format.extensions.any { extension -> normalizedName.endsWith(".$extension") }
        }
    }

    fun isArchiveName(name: String): Boolean = resolve(name) != null

    fun isPreviewSupported(name: String): Boolean = resolve(name)?.supportsPreview == true

    fun isExtractionSupported(name: String): Boolean = resolve(name)?.supportsExtraction == true

    private fun resolveByMimeType(mimeType: String): ArchiveFormat? {
        if (mimeType.isBlank()) return null
        return ArchiveFormat.entries.firstOrNull { format -> mimeType in format.mimeTypes }
    }

    private fun resolveSplitArchiveName(name: String): ArchiveFormat? {
        return when {
            name.matches(Regex(".*\\.part\\d+\\.rar")) -> ArchiveFormat.SplitArchive
            name.matches(Regex(".*\\.r\\d{2,3}")) -> ArchiveFormat.SplitArchive
            name.matches(Regex(".*\\.7z\\.\\d{3}")) -> ArchiveFormat.SplitArchive
            name.matches(Regex(".*\\.zip\\.\\d{3}")) -> ArchiveFormat.SplitArchive
            name.endsWith(".z01") || name.matches(Regex(".*\\.z\\d{2}")) -> ArchiveFormat.SplitArchive
            else -> null
        }
    }

    private fun resolveBySignature(sample: ByteArray): ArchiveFormat? {
        return when {
            sample.startsWithAscii("PK\u0003\u0004") || sample.startsWithAscii("PK\u0005\u0006") || sample.startsWithAscii("PK\u0007\u0008") -> ArchiveFormat.Zip
            sample.startsWith(0x1F, 0x8B) -> ArchiveFormat.Gzip
            sample.startsWithAscii("BZh") -> ArchiveFormat.Bzip2
            sample.startsWith(0xFD, 0x37, 0x7A, 0x58, 0x5A, 0x00) -> ArchiveFormat.Xz
            sample.startsWith(0x28, 0xB5, 0x2F, 0xFD) -> ArchiveFormat.Zstd
            sample.startsWith(0x04, 0x22, 0x4D, 0x18) -> ArchiveFormat.Lz4
            sample.startsWith(0x4C, 0x5A, 0x49, 0x50) -> ArchiveFormat.Lzip
            sample.startsWith(0xFF, 0x06, 0x00, 0x00) || sample.startsWith(0x82, 0x53, 0x4E, 0x41, 0x50, 0x50, 0x59) -> ArchiveFormat.Snappy
            sample.startsWithAscii("7z\u00BC\u00AF\u0027\u001C") -> ArchiveFormat.SevenZip
            sample.startsWithAscii("Rar!\u001A\u0007\u0000") || sample.startsWithAscii("Rar!\u001A\u0007\u0001\u0000") -> ArchiveFormat.Rar
            sample.size >= 265 && sample.copyOfRange(257, 262).toString(Charsets.US_ASCII) == "ustar" -> ArchiveFormat.Tar
            sample.startsWithAscii("070701") || sample.startsWithAscii("070702") || sample.startsWithAscii("070707") -> ArchiveFormat.Cpio
            sample.startsWithAscii("!<arch>\n") -> ArchiveFormat.Ar
            else -> null
        }
    }

    private fun ByteArray.startsWith(vararg bytes: Int): Boolean {
        if (size < bytes.size) return false
        return bytes.indices.all { index -> (this[index].toInt() and 0xFF) == bytes[index] }
    }

    private fun ByteArray.startsWithAscii(prefix: String): Boolean {
        val bytes = prefix.toByteArray(Charsets.ISO_8859_1)
        if (size < bytes.size) return false
        return bytes.indices.all { index -> this[index] == bytes[index] }
    }
}

enum class ArchiveFormat(
    val displayName: String,
    val extensions: Set<String>,
    val mimeTypes: Set<String>,
    val supportsPreview: Boolean,
    val supportsExtraction: Boolean,
    val isSingleFileCompression: Boolean = false
) {
    Zip(
        displayName = "ZIP",
        extensions = setOf("zip", "jar", "aar", "war", "ear", "xpi", "docx", "xlsx", "pptx", "odt", "ods", "odp"),
        mimeTypes = setOf(
            "application/zip",
            "application/x-zip-compressed",
            "application/java-archive",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        ),
        supportsPreview = true,
        supportsExtraction = true
    ),
    Tar(
        displayName = "TAR",
        extensions = setOf("tar"),
        mimeTypes = setOf("application/x-tar", "application/tar"),
        supportsPreview = true,
        supportsExtraction = true
    ),
    Gzip(
        displayName = "GZIP",
        extensions = setOf("gz", "gzip", "tgz", "tar.gz"),
        mimeTypes = setOf("application/gzip", "application/x-gzip"),
        supportsPreview = true,
        supportsExtraction = true,
        isSingleFileCompression = true
    ),
    Bzip2(
        displayName = "BZIP2",
        extensions = setOf("bz2", "bzip2", "tbz", "tbz2", "tar.bz2"),
        mimeTypes = setOf("application/x-bzip2", "application/x-bzip"),
        supportsPreview = true,
        supportsExtraction = true,
        isSingleFileCompression = true
    ),
    Xz(
        displayName = "XZ",
        extensions = setOf("xz", "txz", "tar.xz"),
        mimeTypes = setOf("application/x-xz"),
        supportsPreview = true,
        supportsExtraction = true,
        isSingleFileCompression = true
    ),
    Zstd(
        displayName = "Zstandard",
        extensions = setOf("zst", "zstd", "tzst", "tar.zst"),
        mimeTypes = setOf("application/zstd", "application/x-zstd"),
        supportsPreview = false,
        supportsExtraction = false,
        isSingleFileCompression = true
    ),
    Lz4(
        displayName = "LZ4",
        extensions = setOf("lz4", "tlz4", "tar.lz4"),
        mimeTypes = setOf("application/x-lz4"),
        supportsPreview = false,
        supportsExtraction = false,
        isSingleFileCompression = true
    ),
    Lzip(
        displayName = "LZIP",
        extensions = setOf("lz", "lzip", "tlz", "tar.lz"),
        mimeTypes = setOf("application/x-lzip"),
        supportsPreview = false,
        supportsExtraction = false,
        isSingleFileCompression = true
    ),
    Snappy(
        displayName = "Snappy",
        extensions = setOf("sz", "snappy"),
        mimeTypes = setOf("application/x-snappy-framed"),
        supportsPreview = false,
        supportsExtraction = false,
        isSingleFileCompression = true
    ),
    SevenZip(
        displayName = "7Z",
        extensions = setOf("7z"),
        mimeTypes = setOf("application/x-7z-compressed"),
        supportsPreview = true,
        supportsExtraction = true
    ),
    Cpio(
        displayName = "CPIO",
        extensions = setOf("cpio"),
        mimeTypes = setOf("application/x-cpio"),
        supportsPreview = true,
        supportsExtraction = true
    ),
    Ar(
        displayName = "AR",
        extensions = setOf("ar", "deb"),
        mimeTypes = setOf("application/x-archive", "application/vnd.debian.binary-package"),
        supportsPreview = true,
        supportsExtraction = true
    ),
    Rar(
        displayName = "RAR",
        extensions = setOf("rar"),
        mimeTypes = setOf("application/vnd.rar", "application/x-rar-compressed"),
        supportsPreview = false,
        supportsExtraction = false
    ),
    SplitArchive(
        displayName = "Split archive",
        extensions = emptySet(),
        mimeTypes = emptySet(),
        supportsPreview = false,
        supportsExtraction = false
    )
}
