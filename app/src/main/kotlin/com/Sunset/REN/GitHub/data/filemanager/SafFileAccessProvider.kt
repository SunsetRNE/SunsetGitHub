package com.Sunset.REN.GitHub.data.filemanager

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.Sunset.REN.GitHub.domain.filemanager.FileCompileCapabilityResolver
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryCapabilityResolver
import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryTypeResolver
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntrySorter
import java.io.IOException

class SafFileAccessProvider(
    private val context: Context
) {
    fun treeFromUri(uri: Uri): DocumentFile? {
        return DocumentFile.fromTreeUri(context, uri)
    }

    fun documentFromUri(uri: Uri): DocumentFile? {
        return DocumentFile.fromSingleUri(context, uri)
    }

    fun listDirectory(directoryUri: Uri): Result<List<FileManagerEntry>> {
        return runCatching {
            listDirectoryByUri(directoryUri)
                ?: listDirectory(
                    DocumentFile.fromTreeUri(context, directoryUri)
                        ?: throw IOException("Cannot open authorized directory: $directoryUri")
                )
        }
    }

    fun listDirectoryByUri(directoryUri: Uri): List<FileManagerEntry>? {
        val documentId = runCatching { DocumentsContract.getDocumentId(directoryUri) }.getOrNull()
            ?: return null
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(directoryUri, documentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS
        )
        val entries = mutableListOf<FileManagerEntry>()
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val documentIdIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeTypeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            val flagsIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS)
            while (cursor.moveToNext()) {
                val childDocumentId = cursor.getStringOrNull(documentIdIndex) ?: continue
                val name = cursor.getStringOrNull(nameIndex) ?: childDocumentId.substringAfterLast(':').substringAfterLast('/')
                val mimeType = cursor.getStringOrNull(mimeTypeIndex)
                val childUri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, childDocumentId)
                val flags = cursor.getIntOrZero(flagsIndex)
                entries += buildEntry(
                    uri = childUri,
                    name = name.ifBlank { childUri.toString() },
                    mimeType = mimeType,
                    sizeBytes = cursor.getLongOrNull(sizeIndex),
                    modifiedAtMillis = cursor.getLongOrNull(modifiedIndex),
                    flags = flags
                )
            }
        } ?: throw IOException("Cannot query authorized directory: $directoryUri")
        return FileManagerEntrySorter.sort(entries)
    }

    fun createFile(directoryUri: Uri, mimeType: String, name: String): Boolean {
        val directory = documentFromUri(directoryUri) ?: treeFromUri(directoryUri)
        return directory?.takeIf { it.exists() && it.isDirectory }
            ?.createFile(mimeType, name) != null
    }

    fun createDirectory(directoryUri: Uri, name: String): Boolean {
        val directory = documentFromUri(directoryUri) ?: treeFromUri(directoryUri)
        return directory?.takeIf { it.exists() && it.isDirectory }
            ?.createDirectory(name) != null
    }

    fun findChild(directoryUri: Uri, name: String): Boolean {
        val directory = documentFromUri(directoryUri) ?: treeFromUri(directoryUri)
        return directory?.takeIf { it.exists() && it.isDirectory }
            ?.findFile(name) != null
    }

    fun canWriteDirectory(directoryUri: Uri): Boolean {
        val directory = documentFromUri(directoryUri) ?: treeFromUri(directoryUri)
        return directory?.takeIf { it.exists() && it.isDirectory }
            ?.canWrite() == true
    }

    fun parentUriFor(uri: Uri): Uri? {
        documentFromUri(uri)
            ?.parentFile
            ?.takeIf { it.exists() && it.isDirectory }
            ?.uri
            ?.let { return it }
        val documentId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
            ?: return null
        val separatorIndex = documentId.lastIndexOf('/')
        if (separatorIndex <= 0) return null
        val parentDocumentId = documentId.substring(0, separatorIndex)
        if (parentDocumentId.isBlank()) return null
        return DocumentsContract.buildDocumentUriUsingTree(uri, parentDocumentId)
    }

    fun listDirectory(directory: DocumentFile): List<FileManagerEntry> {
        if (!directory.exists()) throw IOException("Directory does not exist: ${directory.uri}")
        if (!directory.isDirectory) throw IOException("Document is not a directory: ${directory.uri}")
        return FileManagerEntrySorter.sort(
            directory.listFiles()
                .filter { it.exists() }
                .map { buildEntry(it) }
        )
    }

    fun buildEntry(document: DocumentFile): FileManagerEntry {
        return buildEntry(
            uri = document.uri,
            name = document.name?.takeIf { it.isNotBlank() }
                ?: document.uri.lastPathSegment.orEmpty().ifBlank { document.uri.toString() },
            mimeType = document.type,
            sizeBytes = document.length().takeIf { document.isFile && it >= 0L },
            modifiedAtMillis = document.lastModified().takeIf { it > 0L },
            canRead = document.canRead(),
            canWrite = document.canWrite(),
            isDirectory = document.isDirectory,
            isFile = document.isFile
        )
    }

    private fun buildEntry(
        uri: Uri,
        name: String,
        mimeType: String?,
        sizeBytes: Long?,
        modifiedAtMillis: Long?,
        flags: Int
    ): FileManagerEntry {
        val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
        val canRead = true
        val canWrite = flags.hasFlag(DocumentsContract.Document.FLAG_SUPPORTS_WRITE) ||
            flags.hasFlag(DocumentsContract.Document.FLAG_SUPPORTS_RENAME) ||
            flags.hasFlag(DocumentsContract.Document.FLAG_SUPPORTS_DELETE)
        return buildEntry(
            uri = uri,
            name = name,
            mimeType = mimeType,
            sizeBytes = sizeBytes?.takeIf { !isDirectory && it >= 0L },
            modifiedAtMillis = modifiedAtMillis?.takeIf { it > 0L },
            canRead = canRead,
            canWrite = canWrite,
            isDirectory = isDirectory,
            isFile = !isDirectory
        )
    }

    private fun buildEntry(
        uri: Uri,
        name: String,
        mimeType: String?,
        sizeBytes: Long?,
        modifiedAtMillis: Long?,
        canRead: Boolean,
        canWrite: Boolean,
        isDirectory: Boolean,
        isFile: Boolean
    ): FileManagerEntry {
        val guessedMimeType = mimeType ?: guessMimeType(name)
        val declaredType = FileEntryTypeResolver.resolve(
            name = name,
            mimeType = guessedMimeType,
            isDirectory = isDirectory
        )
        val sampleBytes = sampleDocumentHeader(
            uri = uri,
            name = name,
            type = declaredType,
            canRead = canRead,
            isFile = isFile
        )
        val type = if (sampleBytes != null) {
            FileEntryTypeResolver.resolveVerified(
                name = name,
                mimeType = guessedMimeType,
                isDirectory = isDirectory,
                sampleBytes = sampleBytes
            )
        } else {
            declaredType
        }
        return FileManagerEntry(
            id = uri.toString(),
            name = name,
            displayPath = uri.toString(),
            type = type,
            source = FileEntrySource.DocumentUri(uri),
            sizeBytes = sizeBytes,
            modifiedAtMillis = modifiedAtMillis,
            capabilities = FileEntryCapabilityResolver.resolve(
                type = type,
                isFile = isFile,
                isDirectory = isDirectory,
                canRead = canRead,
                canWrite = canWrite
            ),
            compileCapability = FileCompileCapabilityResolver.resolve(name, type, isDirectory)
        )
    }

    private fun sampleDocumentHeader(
        uri: Uri,
        name: String,
        type: FileEntryType,
        canRead: Boolean,
        isFile: Boolean
    ): ByteArray? {
        if (!isFile || !canRead) return null
        val hasExtension = name.substringAfterLast('/', name).substringAfterLast('.', missingDelimiterValue = "").isNotBlank()
        if (hasExtension && type != FileEntryType.Unknown) return null
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(FileTypeSampleBytes)
                val read = input.read(buffer)
                if (read > 0) buffer.copyOf(read) else null
            }
        }.getOrNull()
    }

    private fun Cursor.getStringOrNull(index: Int): String? {
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private fun Cursor.getLongOrNull(index: Int): Long? {
        return if (index >= 0 && !isNull(index)) getLong(index) else null
    }

    private fun Cursor.getIntOrZero(index: Int): Int {
        return if (index >= 0 && !isNull(index)) getInt(index) else 0
    }

    private fun Int.hasFlag(flag: Int): Boolean = this and flag != 0

    private fun guessMimeType(name: String): String? {
        val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        if (extension.isBlank()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    fun resolveDisplayName(uri: Uri): String {
        val queriedName = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()
        return queriedName?.takeIf { it.isNotBlank() }
            ?: DocumentFile.fromTreeUri(context, uri)?.name?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: uri.toString()
    }

    private companion object {
        const val FileTypeSampleBytes = 4096
    }
}