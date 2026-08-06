package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.Sunset.REN.GitHub.R
import java.io.File
import java.io.IOException

enum class TransferConflictPolicy {
    Fail,
    KeepBoth,
    Replace
}

class FileTransferNamingPolicy(
    private val context: Context
) {
    private val appContext = context.applicationContext

    fun nextAvailableLocalCopyTarget(parent: File, originalName: String): File {
        var index = 1
        while (true) {
            val candidate = File(parent, copyNameFor(originalName, index))
            if (!candidate.exists()) return candidate
            index++
        }
    }

    fun nextAvailableSafCopyName(parent: DocumentFile, originalName: String): String {
        var index = 1
        while (true) {
            val candidate = copyNameFor(originalName, index)
            if (parent.findFile(candidate) == null) return candidate
            index++
        }
    }

    suspend fun resolveLocalTransferTarget(
        directory: File,
        desiredName: String,
        policy: TransferConflictPolicy,
        deleteExisting: suspend (File) -> Unit
    ): File {
        val directTarget = File(directory, desiredName)
        if (!directTarget.exists()) return directTarget
        return when (policy) {
            TransferConflictPolicy.Fail -> throw IOException(appContext.getString(R.string.local_file_manager_batch_copy_exists))
            TransferConflictPolicy.KeepBoth -> nextAvailableLocalCopyTarget(directory, desiredName)
            TransferConflictPolicy.Replace -> {
                deleteExisting(directTarget)
                directTarget
            }
        }
    }

    suspend fun resolveSafTransferName(
        directory: DocumentFile,
        desiredName: String,
        policy: TransferConflictPolicy,
        deleteExisting: suspend (DocumentFile) -> Unit
    ): String {
        val existing = directory.findFile(desiredName) ?: return desiredName
        return when (policy) {
            TransferConflictPolicy.Fail -> throw IOException(appContext.getString(R.string.local_file_manager_batch_copy_exists))
            TransferConflictPolicy.KeepBoth -> nextAvailableSafCopyName(directory, desiredName)
            TransferConflictPolicy.Replace -> {
                deleteExisting(existing)
                desiredName
            }
        }
    }

    private fun copyNameFor(originalName: String, index: Int): String {
        val suffix = if (index == 1) {
            appContext.getString(R.string.local_file_manager_copy_name_suffix)
        } else {
            appContext.getString(R.string.local_file_manager_copy_name_suffix_numbered, index)
        }
        val dotIndex = originalName.lastIndexOf('.').takeIf { it > 0 && it < originalName.lastIndex }
        return if (dotIndex == null) {
            "$originalName $suffix"
        } else {
            originalName.substring(0, dotIndex) + " $suffix" + originalName.substring(dotIndex)
        }
    }
}

object FileMimeTypePolicy {
    fun mimeTypeForName(name: String): String {
        return when (name.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
            "txt", "log", "md", "markdown", "json", "xml", "html", "css", "js", "ts", "kt", "kts", "java", "properties", "yml", "yaml", "sh" -> "text/plain"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }
}