package com.Sunset.REN.GitHub.data.filemanager

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.ApkPreview
import com.Sunset.REN.GitHub.domain.filemanager.ArchiveEntryTextPreview
import com.Sunset.REN.GitHub.domain.filemanager.ArchiveExtractSummary
import com.Sunset.REN.GitHub.domain.filemanager.ArchiveFileEntryPreview
import com.Sunset.REN.GitHub.domain.filemanager.ArchiveFormat
import com.Sunset.REN.GitHub.domain.filemanager.ArchiveFormatResolver
import com.Sunset.REN.GitHub.domain.filemanager.ArchivePreview
import com.Sunset.REN.GitHub.domain.filemanager.DexFilePreview
import com.Sunset.REN.GitHub.domain.filemanager.FileCompileCapabilityResolver
import com.Sunset.REN.GitHub.domain.filemanager.FileContentAccessPolicy
import com.Sunset.REN.GitHub.domain.filemanager.FileContentReadResult
import com.Sunset.REN.GitHub.domain.filemanager.FileContentWriteResult
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryTypeResolver
import com.Sunset.REN.GitHub.domain.filemanager.FileSizeFormatter
import com.Sunset.REN.GitHub.domain.filemanager.FileTextEncodingPolicy
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.compressors.CompressorStreamFactory
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

class FileContentAccessRepository(
    private val context: Context
) {
    fun readText(
        sourceUri: Uri,
        displayName: String,
        entryType: FileEntryType,
        declaredSizeBytes: Long?,
        maxBytes: Long = DefaultMaxTextPreviewBytes,
        sniffBytes: Int = DefaultSniffBytes,
        charset: Charset = Charsets.UTF_8
    ): FileContentReadResult {
        val knownSize = declaredSizeBytes?.takeIf { it >= 0L }
        val knownOversized = knownSize != null && knownSize > maxBytes
        if (entryType in blockedTextTypes) {
            return FileContentReadResult.BinaryBlocked(context.getString(R.string.local_file_preview_binary_blocked))
        }

        return runCatching {
            openInputStream(sourceUri).use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0L
                var sample = ByteArray(0)
                var truncated = knownOversized
                while (total < maxBytes) {
                    val maxRead = minOf(buffer.size.toLong(), maxBytes - total).toInt()
                    val read = input.read(buffer, 0, maxRead)
                    if (read < 0) break
                    total += read
                    if (sample.size < sniffBytes) {
                        val remainingSampleSize = sniffBytes - sample.size
                        sample += buffer.copyOfRange(0, read.coerceAtMost(remainingSampleSize))
                    }
                    output.write(buffer, 0, read)
                }
                if (!truncated) {
                    truncated = input.read() >= 0
                }

                if (!FileContentAccessPolicy.canTreatSampleAsText(
                        declaredType = entryType,
                        displayName = displayName,
                        sampleBytes = sample
                    )
                ) {
                    return FileContentReadResult.BinaryBlocked(context.getString(R.string.local_file_preview_binary_blocked))
                }

                val decoded = FileTextEncodingPolicy.decode(output.toByteArray(), charset)
                FileContentReadResult.Text(
                    content = decoded.content,
                    charset = decoded.charset,
                    truncated = truncated,
                    sizeBytes = knownSize ?: total,
                    hadBom = decoded.hadBom,
                    lineEnding = decoded.lineEnding
                )
            }
        }.getOrElse { error ->
            FileContentReadResult.Failed(
                error.message?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.local_file_preview_read_failed)
            )
        }
    }

    fun readPdfText(
        sourceUri: Uri,
        displayName: String,
        declaredSizeBytes: Long?,
        maxBytes: Long = DefaultMaxTextPreviewBytes
    ): FileContentReadResult {
        return runCatching {
            PDFBoxResourceLoader.init(context)
            val tempFile = File.createTempFile("sunset_pdf_preview_", ".pdf", context.cacheDir)
            try {
                openInputStream(sourceUri).use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                }
                PDDocument.load(tempFile).use { document ->
                    val stripper = PDFTextStripper()
                    val extracted = stripper.getText(document).trimEnd()
                    if (extracted.isBlank()) {
                        throw IllegalArgumentException(context.getString(R.string.local_file_preview_binary_blocked))
                    }
                    val textBytes = extracted.toByteArray(Charsets.UTF_8)
                    if (textBytes.size > maxBytes) {
                        val clipped = extracted.take(maxBytes.toInt())
                        return FileContentReadResult.Text(
                            content = clipped,
                            charset = Charsets.UTF_8,
                            truncated = true,
                            sizeBytes = declaredSizeBytes?.takeIf { it >= 0L } ?: tempFile.length(),
                            hadBom = false,
                            lineEnding = FileTextEncodingPolicy.LineEnding.Lf
                        )
                    }
                    FileContentReadResult.Text(
                        content = extracted,
                        charset = Charsets.UTF_8,
                        truncated = false,
                        sizeBytes = declaredSizeBytes?.takeIf { it >= 0L } ?: tempFile.length(),
                        hadBom = false,
                        lineEnding = FileTextEncodingPolicy.LineEnding.Lf
                    )
                }
            } finally {
                tempFile.delete()
            }
        }.getOrElse { error ->
            FileContentReadResult.Failed(
                error.message?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.local_file_preview_read_failed)
            )
        }
    }

    fun readDocxText(
        sourceUri: Uri,
        displayName: String,
        declaredSizeBytes: Long?,
        maxBytes: Long = DefaultMaxTextPreviewBytes
    ): FileContentReadResult {
        return runCatching {
            val knownSize = declaredSizeBytes?.takeIf { it >= 0L }
            var documentXmlBytes: ByteArray? = null
            ZipInputStream(openInputStream(sourceUri)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory && entry.name == DocxDocumentXmlPath) {
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8 * 1024)
                        var total = 0L
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            total += read
                            if (total > maxBytes) {
                                throw IllegalStateException(context.getString(R.string.local_file_preview_too_large, formatBytes(total)))
                            }
                            output.write(buffer, 0, read)
                        }
                        documentXmlBytes = output.toByteArray()
                        break
                    }
                    zip.closeEntry()
                }
            }
            val xmlBytes = documentXmlBytes
                ?: throw IllegalArgumentException(context.getString(R.string.local_file_preview_docx_missing_document))
            val extracted = extractDocxDocumentText(xmlBytes)
            FileContentReadResult.Text(
                content = extracted,
                charset = Charsets.UTF_8,
                truncated = false,
                sizeBytes = knownSize ?: xmlBytes.size.toLong(),
                hadBom = false,
                lineEnding = FileTextEncodingPolicy.LineEnding.Lf
            )
        }.getOrElse { error ->
            FileContentReadResult.Failed(
                error.message?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.local_file_preview_read_failed)
            )
        }
    }

    fun readZipPreview(
        sourceUri: Uri,
        displayName: String,
        declaredSizeBytes: Long?,
        maxEntries: Int = DefaultMaxArchivePreviewEntries
    ): Result<ArchivePreview> {
        return runCatching {
            val knownSize = declaredSizeBytes?.takeIf { it >= 0L }
            val format = ArchiveFormatResolver.resolve(displayName)
                ?: throw IllegalArgumentException(context.getString(R.string.local_file_manager_unzip_not_zip))
            if (!format.supportsPreview) {
                throw IllegalArgumentException(context.getString(R.string.local_file_manager_archive_format_unsupported, format.displayName))
            }
            val entries = mutableListOf<ArchiveFileEntryPreview>()
            var entryCount = 0
            var directoryCount = 0
            var fileCount = 0
            var truncated = false
            openArchiveStream(sourceUri, displayName).use { archive ->
                if (archive == null) {
                    val entryName = singleCompressedEntryName(displayName)
                    entries += buildArchiveEntryPreview(entryName, isDirectory = false, sizeBytes = null)
                    entryCount = 1
                    fileCount = 1
                } else {
                    while (true) {
                        val entry = archive.nextEntry ?: break
                        if (!archive.canReadEntryData(entry)) continue
                        entryCount += 1
                        if (entry.isDirectory) {
                            directoryCount += 1
                        } else {
                            fileCount += 1
                        }
                        if (entries.size < maxEntries) {
                            entries += buildArchiveEntryPreview(
                                name = entry.name,
                                isDirectory = entry.isDirectory,
                                sizeBytes = entry.size.takeIf { it >= 0L }
                            )
                        } else {
                            truncated = true
                        }
                    }
                }
            }
            ArchivePreview(
                displayName = displayName,
                sizeBytes = knownSize,
                entryCount = entryCount,
                directoryCount = directoryCount,
                fileCount = fileCount,
                entries = entries,
                truncated = truncated
            )
        }
    }

    fun readApkPreview(
        sourceUri: Uri,
        displayName: String,
        declaredSizeBytes: Long?,
        maxEntries: Int = DefaultMaxArchivePreviewEntries
    ): Result<ApkPreview> {
        return runCatching {
            val knownSize = declaredSizeBytes?.takeIf { it >= 0L }
            val entries = mutableListOf<ArchiveFileEntryPreview>()
            val nativeArchitectures = linkedSetOf<String>()
            val certificateEntries = mutableListOf<String>()
            val dexFiles = mutableListOf<DexFilePreview>()
            val dexParseFailures = mutableListOf<String>()
            var entryCount = 0
            var hasManifest = false
            var hasClassesDex = false
            var dexCount = 0
            var hasResourcesArsc = false
            var truncated = false
            ZipInputStream(openInputStream(sourceUri)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    entryCount += 1
                    when {
                        name == "AndroidManifest.xml" -> hasManifest = true
                        name == "resources.arsc" -> hasResourcesArsc = true
                        name == "classes.dex" -> {
                            hasClassesDex = true
                            dexCount += 1
                            parseDexPreview(name, zip, entry.size).fold(
                                onSuccess = { dexFiles += it },
                                onFailure = { dexParseFailures += "$name: ${it.message.orEmpty()}" }
                            )
                        }
                        name.matches(Regex("classes\\d*\\.dex")) -> {
                            dexCount += 1
                            parseDexPreview(name, zip, entry.size).fold(
                                onSuccess = { dexFiles += it },
                                onFailure = { dexParseFailures += "$name: ${it.message.orEmpty()}" }
                            )
                        }
                        name.startsWith("lib/") -> name.split('/').getOrNull(1)?.takeIf { it.isNotBlank() }?.let(nativeArchitectures::add)
                        name.startsWith("META-INF/", ignoreCase = true) &&
                            (name.endsWith(".RSA", ignoreCase = true) || name.endsWith(".DSA", ignoreCase = true) || name.endsWith(".EC", ignoreCase = true)) -> certificateEntries += name
                    }
                    if (entries.size < maxEntries) {
                        entries += buildArchiveEntryPreview(
                            name = name,
                            isDirectory = entry.isDirectory,
                            sizeBytes = entry.size.takeIf { it >= 0L }
                        )
                    } else {
                        truncated = true
                    }
                    zip.closeEntry()
                }
            }
            ApkPreview(
                displayName = displayName,
                sizeBytes = knownSize,
                entryCount = entryCount,
                hasManifest = hasManifest,
                hasClassesDex = hasClassesDex,
                dexCount = dexCount,
                hasResourcesArsc = hasResourcesArsc,
                nativeArchitectures = nativeArchitectures.toList().sorted(),
                certificateEntries = certificateEntries.sorted(),
                dexFiles = dexFiles,
                dexParseFailures = dexParseFailures,
                entries = entries,
                truncated = truncated
            )
        }
    }

    fun readArchiveEntryText(
        sourceUri: Uri,
        archiveDisplayName: String,
        entryName: String,
        maxBytes: Long = DefaultMaxTextPreviewBytes,
        charset: Charset = Charsets.UTF_8
    ): Result<ArchiveEntryTextPreview> {
        return runCatching {
            requireSafeArchiveEntryName(entryName)
            openArchiveStream(sourceUri, archiveDisplayName).use { archive ->
                if (archive == null) {
                    if (entryName != singleCompressedEntryName(archiveDisplayName)) {
                        throw IllegalArgumentException(context.getString(R.string.local_file_preview_archive_entry_missing, entryName))
                    }
                    return@runCatching readArchiveTextFromStream(entryName, openDecompressedInputStream(sourceUri, archiveDisplayName), maxBytes, charset)
                }
                while (true) {
                    val entry = archive.nextEntry ?: break
                    if (!entry.isDirectory && archive.canReadEntryData(entry) && entry.name == entryName) {
                        return@runCatching readArchiveTextFromStream(entryName, archive, maxBytes, charset)
                    }
                }
            }
            throw IllegalArgumentException(context.getString(R.string.local_file_preview_archive_entry_missing, entryName))
        }
    }

    fun extractArchive(
        sourceUri: Uri,
        archiveDisplayName: String,
        targetTreeUri: Uri
    ): Result<ArchiveExtractSummary> {
        return runCatching {
            val targetRoot = DocumentFile.fromTreeUri(context, targetTreeUri)
                ?: throw IllegalStateException(context.getString(R.string.local_file_manager_unzip_no_permission))
            if (!targetRoot.isDirectory || !targetRoot.canWrite()) {
                throw IllegalStateException(context.getString(R.string.local_file_manager_unzip_no_permission))
            }
            val format = ArchiveFormatResolver.resolve(archiveDisplayName)
                ?: throw IllegalArgumentException(context.getString(R.string.local_file_manager_unzip_not_zip))
            if (!format.supportsExtraction) {
                throw IllegalArgumentException(context.getString(R.string.local_file_manager_archive_format_unsupported, format.displayName))
            }
            val outputRootName = archiveOutputDirectoryName(archiveDisplayName)
            val outputRoot = ensureDirectory(targetRoot, outputRootName)
            var directoryCount = 0
            var fileCount = 0
            var skippedEntryCount = 0
            openArchiveStream(sourceUri, archiveDisplayName).use { archive ->
                if (archive == null) {
                    val entryName = singleCompressedEntryName(archiveDisplayName)
                    writeDocumentFile(outputRoot, entryName, openDecompressedInputStream(sourceUri, archiveDisplayName))
                    fileCount += 1
                } else {
                    while (true) {
                        val entry = archive.nextEntry ?: break
                        if (!archive.canReadEntryData(entry)) {
                            skippedEntryCount += 1
                            continue
                        }
                        val safeName = safeArchiveEntryNameOrNull(entry.name)
                            ?: throw IllegalArgumentException(context.getString(R.string.local_file_manager_unzip_unsafe_entry))
                        if (safeName.isBlank()) continue
                        if (entry.isDirectory) {
                            ensureDirectoryPath(outputRoot, safeName)
                            directoryCount += 1
                        } else {
                            writeDocumentFile(outputRoot, safeName, archive)
                            fileCount += 1
                        }
                    }
                }
            }
            ArchiveExtractSummary(
                targetName = outputRoot.name ?: outputRootName,
                directoryCount = directoryCount,
                fileCount = fileCount,
                skippedEntryCount = skippedEntryCount
            )
        }
    }

    fun writeText(
        sourceUri: Uri,
        content: String,
        charset: Charset = Charsets.UTF_8,
        preserveBom: Boolean = false,
        lineEnding: FileTextEncodingPolicy.LineEnding = FileTextEncodingPolicy.LineEnding.Lf,
        expectedLastModifiedMillis: Long? = null
    ): Result<FileContentWriteResult> {
        return runCatching {
            if (expectedLastModifiedMillis != null) {
                val currentLastModified = lastModifiedMillis(sourceUri)
                if (currentLastModified != null && currentLastModified != expectedLastModifiedMillis) {
                    throw IllegalStateException(context.getString(R.string.local_file_preview_save_conflict_message))
                }
            }
            val bytes = FileTextEncodingPolicy.encode(
                content = content,
                charset = charset,
                preserveBom = preserveBom,
                lineEnding = lineEnding
            )
            openOutputStream(sourceUri).use { output ->
                output.write(bytes)
            }
            FileContentWriteResult(
                sizeBytes = bytes.size.toLong(),
                lastModifiedMillis = lastModifiedMillis(sourceUri)
            )
        }
    }

    fun lastModifiedMillis(sourceUri: Uri): Long? {
        return runCatching {
            if (sourceUri.scheme == "file") {
                sourceUri.path?.let(::File)?.lastModified()?.takeIf { it > 0L }
            } else {
                val projection = arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                context.contentResolver.query(sourceUri, projection, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    if (index >= 0 && cursor.moveToFirst()) cursor.getLong(index).takeIf { it > 0L } else null
                }
            }
        }.getOrNull()
    }

    private fun buildArchiveEntryPreview(name: String, isDirectory: Boolean, sizeBytes: Long?): ArchiveFileEntryPreview {
        val type = FileEntryTypeResolver.resolve(name = name, isDirectory = isDirectory)
        return ArchiveFileEntryPreview(
            name = name,
            isDirectory = isDirectory,
            sizeBytes = sizeBytes,
            type = type,
            canPreviewText = !isDirectory && type in FileContentAccessPolicy.sniffableTypes,
            compileCapability = FileCompileCapabilityResolver.resolve(name, type, isDirectory)
        )
    }

    private fun writeDocumentFile(root: DocumentFile, relativePath: String, input: InputStream) {
        val safeName = safeArchiveEntryNameOrNull(relativePath)
            ?: throw IllegalArgumentException(context.getString(R.string.local_file_manager_unzip_unsafe_entry))
        val segments = safeName.split('/').filter { it.isNotBlank() }
        if (segments.isEmpty()) return
        val parent = segments.dropLast(1).fold(root) { directory, segment -> ensureDirectory(directory, segment) }
        val fileName = segments.last()
        parent.findFile(fileName)?.delete()
        val target = parent.createFile(mimeTypeForName(fileName), fileName)
            ?: throw IllegalStateException(context.getString(R.string.local_file_manager_unzip_failed))
        context.contentResolver.openOutputStream(target.uri, "wt").use { output ->
            val sink = output ?: throw IllegalStateException(context.getString(R.string.local_file_manager_unzip_failed))
            input.copyTo(sink)
        }
    }

    private fun ensureDirectoryPath(root: DocumentFile, relativePath: String): DocumentFile {
        val safeName = safeArchiveEntryNameOrNull(relativePath)
            ?: throw IllegalArgumentException(context.getString(R.string.local_file_manager_unzip_unsafe_entry))
        return safeName.split('/').filter { it.isNotBlank() }.fold(root) { directory, segment ->
            ensureDirectory(directory, segment)
        }
    }

    private fun ensureDirectory(parent: DocumentFile, name: String): DocumentFile {
        val existing = parent.findFile(name)
        if (existing != null) {
            if (existing.isDirectory) return existing
            existing.delete()
        }
        return parent.createDirectory(name)
            ?: throw IllegalStateException(context.getString(R.string.local_file_manager_unzip_failed))
    }

    private fun archiveOutputDirectoryName(displayName: String): String {
        val normalized = displayName.substringAfterLast('/').ifBlank { context.getString(R.string.local_file_preview_unknown_name) }
        val withoutCompoundExtension = listOf(".tar.gz", ".tar.bz2", ".tar.xz").firstOrNull { normalized.endsWith(it, ignoreCase = true) }
            ?.let { normalized.dropLast(it.length) }
        if (!withoutCompoundExtension.isNullOrBlank()) return withoutCompoundExtension
        return normalized.substringBeforeLast('.', normalized).ifBlank { normalized }
    }

    private fun safeArchiveEntryNameOrNull(entryName: String): String? {
        val normalized = entryName.replace('\\', '/').trim('/')
        if (normalized.isBlank()) return ""
        if (normalized.split('/').any { it.isBlank() || it == "." || it == ".." }) return null
        return normalized
    }

    private fun mimeTypeForName(name: String): String {
        return when (FileEntryTypeResolver.resolve(name)) {
            FileEntryType.Text, FileEntryType.Markdown, FileEntryType.Code -> "text/plain"
            FileEntryType.Image -> "image/*"
            FileEntryType.Apk -> "application/vnd.android.package-archive"
            FileEntryType.Archive -> "application/octet-stream"
            else -> "application/octet-stream"
        }
    }

    private fun openArchiveStream(sourceUri: Uri, displayName: String): ArchiveInputStream<out ArchiveEntry>? {
        val format = ArchiveFormatResolver.resolve(displayName)
            ?: throw IllegalArgumentException(context.getString(R.string.local_file_manager_unzip_not_zip))
        if (!format.supportsPreview) {
            throw IllegalArgumentException(context.getString(R.string.local_file_manager_archive_format_unsupported, format.displayName))
        }
        val decompressed = openDecompressedInputStream(sourceUri, displayName)
        return when {
            isTarLikeName(displayName) -> ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.TAR, decompressed)
            format == ArchiveFormat.Gzip || format == ArchiveFormat.Bzip2 || format == ArchiveFormat.Xz -> null
            format == ArchiveFormat.SevenZip -> throw IllegalArgumentException(context.getString(R.string.local_file_manager_archive_streaming_unsupported, format.displayName))
            else -> ArchiveStreamFactory().createArchiveInputStream(decompressed)
        }
    }

    private fun openDecompressedInputStream(sourceUri: Uri, displayName: String): BufferedInputStream {
        val raw = openInputStream(sourceUri).buffered()
        val format = ArchiveFormatResolver.resolve(displayName) ?: return raw
        val stream = when (format) {
            ArchiveFormat.Gzip -> CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.GZIP, raw)
            ArchiveFormat.Bzip2 -> CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.BZIP2, raw)
            ArchiveFormat.Xz -> CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.XZ, raw)
            else -> raw
        }
        return stream.buffered()
    }

    private fun extractDocxDocumentText(xmlBytes: ByteArray): String {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(xmlBytes.inputStream(), Charsets.UTF_8.name())
        val output = StringBuilder()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name.substringAfter(':')) {
                    "t" -> output.append(parser.nextText())
                    "tab" -> output.append('\t')
                    "br", "cr" -> appendLineBreak(output)
                    "p" -> if (output.isNotEmpty() && !output.endsWithLineBreak()) appendLineBreak(output)
                    "tc" -> if (output.isNotEmpty() && !output.endsWithCellSeparator() && !output.endsWithLineBreak()) output.append('\t')
                }
            }
            eventType = parser.next()
        }
        return output.toString().trim()
    }

    private fun appendLineBreak(output: StringBuilder) {
        if (!output.endsWithLineBreak()) output.append('\n')
    }

    private fun StringBuilder.endsWithLineBreak(): Boolean = isNotEmpty() && last() == '\n'

    private fun StringBuilder.endsWithCellSeparator(): Boolean = isNotEmpty() && last() == '\t'

    private fun readArchiveTextFromStream(
        entryName: String,
        input: InputStream,
        maxBytes: Long,
        charset: Charset
    ): ArchiveEntryTextPreview {
        input.use { source ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0L
            while (true) {
                val read = source.read(buffer)
                if (read < 0) break
                total += read
                if (total > maxBytes) {
                    throw IllegalStateException(context.getString(R.string.local_file_preview_too_large, formatBytes(total)))
                }
                output.write(buffer, 0, read)
            }
            val bytes = output.toByteArray()
            val sample = bytes.take(DefaultSniffBytes).toByteArray()
            val type = FileEntryTypeResolver.resolveVerified(name = entryName, sampleBytes = sample)
            if (!FileContentAccessPolicy.canTreatSampleAsText(type, entryName, sample)) {
                throw IllegalStateException(context.getString(R.string.local_file_preview_binary_blocked))
            }
            val decoded = FileTextEncodingPolicy.decode(bytes, charset)
            return ArchiveEntryTextPreview(
                entryName = entryName,
                content = decoded.content,
                charset = decoded.charset,
                sizeBytes = total
            )
        }
    }

    private fun singleCompressedEntryName(displayName: String): String {
        val normalized = displayName.substringAfterLast('/').ifBlank { context.getString(R.string.local_file_preview_unknown_name) }
        return when {
            normalized.endsWith(".tar.gz", ignoreCase = true) -> normalized.dropLast(3)
            normalized.endsWith(".tgz", ignoreCase = true) -> normalized.dropLast(4) + ".tar"
            normalized.endsWith(".tar.bz2", ignoreCase = true) -> normalized.dropLast(4)
            normalized.endsWith(".tbz2", ignoreCase = true) -> normalized.dropLast(5) + ".tar"
            normalized.endsWith(".tbz", ignoreCase = true) -> normalized.dropLast(4) + ".tar"
            normalized.endsWith(".tar.xz", ignoreCase = true) -> normalized.dropLast(3)
            normalized.endsWith(".txz", ignoreCase = true) -> normalized.dropLast(4) + ".tar"
            normalized.endsWith(".gzip", ignoreCase = true) -> normalized.dropLast(5)
            normalized.endsWith(".gz", ignoreCase = true) -> normalized.dropLast(3)
            normalized.endsWith(".bzip2", ignoreCase = true) -> normalized.dropLast(6)
            normalized.endsWith(".bz2", ignoreCase = true) -> normalized.dropLast(4)
            normalized.endsWith(".xz", ignoreCase = true) -> normalized.dropLast(3)
            else -> normalized
        }.ifBlank { context.getString(R.string.local_file_preview_unknown_name) }
    }

    private fun isTarLikeName(displayName: String): Boolean {
        val normalized = displayName.lowercase()
        return normalized.endsWith(".tar") ||
            normalized.endsWith(".tar.gz") ||
            normalized.endsWith(".tgz") ||
            normalized.endsWith(".tar.bz2") ||
            normalized.endsWith(".tbz") ||
            normalized.endsWith(".tbz2") ||
            normalized.endsWith(".tar.xz") ||
            normalized.endsWith(".txz")
    }

    private fun requireSafeArchiveEntryName(entryName: String) {
        val normalized = entryName.replace('\\', '/')
        if (normalized.isBlank() || normalized.startsWith('/') || normalized.split('/').any { it == ".." }) {
            throw IllegalArgumentException(context.getString(R.string.local_file_preview_archive_entry_unsafe))
        }
    }

    private fun parseDexPreview(
        name: String,
        input: InputStream,
        declaredSizeBytes: Long
    ): Result<DexFilePreview> = runCatching {
        val bytes = input.readBytes()
        if (bytes.size < DexHeaderSize) {
            throw IllegalArgumentException(context.getString(R.string.local_file_preview_apk_dex_header_too_short))
        }
        val magic = bytes.copyOfRange(0, 8)
        val magicText = magic.toString(Charsets.US_ASCII)
        if (!magicText.startsWith("dex\n") || magic[7] != 0.toByte()) {
            throw IllegalArgumentException(context.getString(R.string.local_file_preview_apk_dex_header_invalid_magic))
        }
        val version = magic.copyOfRange(4, 7).toString(Charsets.US_ASCII)
        val endianTagValue = readDexUInt(bytes, DexEndianTagOffset, ByteOrder.LITTLE_ENDIAN)
        val order = when (endianTagValue) {
            DexLittleEndianTag -> ByteOrder.LITTLE_ENDIAN
            DexReverseEndianTag -> ByteOrder.BIG_ENDIAN
            else -> throw IllegalArgumentException(context.getString(R.string.local_file_preview_apk_dex_header_invalid_endian))
        }
        DexFilePreview(
            name = name,
            version = version,
            fileSizeBytes = readDexUInt(bytes, DexFileSizeOffset, order)
                .takeIf { it > 0L }
                ?: declaredSizeBytes.takeIf { it >= 0L }
                ?: bytes.size.toLong(),
            headerSizeBytes = readDexUInt(bytes, DexHeaderSizeOffset, order),
            endianTag = "0x${endianTagValue.toString(16).padStart(8, '0')}",
            stringCount = readDexUInt(bytes, DexStringIdsSizeOffset, order).toInt(),
            typeCount = readDexUInt(bytes, DexTypeIdsSizeOffset, order).toInt(),
            protoCount = readDexUInt(bytes, DexProtoIdsSizeOffset, order).toInt(),
            fieldCount = readDexUInt(bytes, DexFieldIdsSizeOffset, order).toInt(),
            methodCount = readDexUInt(bytes, DexMethodIdsSizeOffset, order).toInt(),
            classCount = readDexUInt(bytes, DexClassDefsSizeOffset, order).toInt()
        )
    }

    private fun readDexUInt(bytes: ByteArray, offset: Int, order: ByteOrder): Long {
        if (offset + Int.SIZE_BYTES > bytes.size) {
            throw IllegalArgumentException(context.getString(R.string.local_file_preview_apk_dex_header_too_short))
        }
        return ByteBuffer.wrap(bytes, offset, Int.SIZE_BYTES)
            .order(order)
            .int
            .toLong() and 0xffffffffL
    }

    private fun formatBytes(bytes: Long): String = FileSizeFormatter.format(bytes)

    private fun openInputStream(sourceUri: Uri): InputStream {
        return if (sourceUri.scheme == "file") {
            sourceUri.path?.let { FileInputStream(File(it)) }
        } else {
            context.contentResolver.openInputStream(sourceUri)
        } ?: throw IllegalStateException(context.getString(R.string.local_file_preview_read_failed))
    }

    private fun openOutputStream(sourceUri: Uri): java.io.OutputStream {
        return if (sourceUri.scheme == "file") {
            val file = sourceUri.path?.let { File(it) }
                ?: throw IllegalStateException(context.getString(R.string.local_file_preview_save_failed))
            FileOutputStream(file, false)
        } else {
            context.contentResolver.openOutputStream(sourceUri, "wt")
        } ?: throw IllegalStateException(context.getString(R.string.local_file_preview_save_failed))
    }

    companion object {
        const val DefaultMaxTextPreviewBytes = 1L * 1024L * 1024L
        const val DefaultSniffBytes = 4096
        const val DefaultMaxArchivePreviewEntries = 100
        private const val DocxDocumentXmlPath = "word/document.xml"
        private const val DexHeaderSize = 112
        private const val DexFileSizeOffset = 32
        private const val DexHeaderSizeOffset = 36
        private const val DexEndianTagOffset = 40
        private const val DexStringIdsSizeOffset = 56
        private const val DexTypeIdsSizeOffset = 64
        private const val DexProtoIdsSizeOffset = 72
        private const val DexFieldIdsSizeOffset = 80
        private const val DexMethodIdsSizeOffset = 88
        private const val DexClassDefsSizeOffset = 96
        private const val DexLittleEndianTag = 0x12345678L
        private const val DexReverseEndianTag = 0x78563412L

        private val blockedTextTypes = setOf(
            FileEntryType.Directory,
            FileEntryType.Image,
            FileEntryType.Archive,
            FileEntryType.Apk,
            FileEntryType.Binary
        )
    }
}