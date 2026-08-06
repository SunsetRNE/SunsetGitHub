package com.Sunset.REN.GitHub.ui.filemanager

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * Shared path relationship helpers used by file-manager operation coordinators.
 *
 * These helpers intentionally live outside LocalFileManagerViewModel so extracted
 * coordinators can keep using the same local-file/SAF safety checks without
 * depending on a giant UI state holder.
 */
internal fun File.isSameOrDescendantOf(ancestor: File): Boolean {
    val selfPath = canonicalFile.toPath().normalize()
    val ancestorPath = ancestor.canonicalFile.toPath().normalize()
    return selfPath == ancestorPath || selfPath.startsWith(ancestorPath)
}

internal fun DocumentFile.isLocalDocumentAncestorOf(targetDirectory: File): Boolean {
    val sourceFile = localFileOrNull() ?: return false
    return targetDirectory.isSameOrDescendantOf(sourceFile)
}

internal fun DocumentFile.isLocalDocumentInside(sourceDirectory: File): Boolean {
    val targetFile = localFileOrNull() ?: return false
    return targetFile.isSameOrDescendantOf(sourceDirectory)
}

private fun DocumentFile.localFileOrNull(): File? {
    return uri.localFileFromExternalStorageDocumentIdOrNull()
}

private fun Uri.localFileFromExternalStorageDocumentIdOrNull(): File? {
    val documentId = runCatching { DocumentsContract.getDocumentId(this) }.getOrNull() ?: return null
    val type = documentId.substringBefore(':', missingDelimiterValue = "")
    val relativePath = documentId.substringAfter(':', missingDelimiterValue = "")
    val root = when (type.lowercase()) {
        "primary" -> Environment.getExternalStorageDirectory()
        "home" -> File(Environment.getExternalStorageDirectory(), "Documents")
        else -> return null
    }
    return if (relativePath.isBlank()) root else File(root, relativePath)
}

internal fun Uri.isSameOrDescendantDocumentOf(ancestor: Uri): Boolean {
    val selfId = runCatching { DocumentsContract.getDocumentId(this) }.getOrNull()?.normalizeDocumentIdPath() ?: return false
    val ancestorId = runCatching { DocumentsContract.getDocumentId(ancestor) }.getOrNull()?.normalizeDocumentIdPath() ?: return false
    return selfId == ancestorId || selfId.startsWith("$ancestorId/")
}

private fun String.normalizeDocumentIdPath(): String {
    return trim().replace('\\', '/').trimEnd('/')
}
