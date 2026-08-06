package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import android.net.Uri
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import java.io.File
import java.io.IOException

class ArchiveExtractionSourceResolver(
    context: Context,
    private val cacheArchiveEntryForContentAccess: suspend (FileEntrySource.ArchiveEntry, String) -> File
) {
    private val appContext = context.applicationContext

    suspend fun archiveSourceFileForExtraction(entry: FileManagerEntry): File {
        return when (val source = entry.source) {
            is FileEntrySource.LocalFile -> source.file
            is FileEntrySource.DocumentUri -> cacheUriForArchiveExtraction(source.uri, entry.name)
            is FileEntrySource.ContentUri -> cacheUriForArchiveExtraction(source.uri, entry.name)
            is FileEntrySource.ArchiveEntry -> cacheArchiveEntryForContentAccess(source, entry.name)
            is FileEntrySource.ParentDirectory,
            is FileEntrySource.RootPath -> throw IOException(string(R.string.local_file_manager_unzip_no_permission))
        }
    }

    private suspend fun cacheUriForArchiveExtraction(uri: Uri, displayName: String): File {
        val cacheDirectory = File(appContext.cacheDir, ArchiveExtractCacheDirectory).also { it.mkdirs() }
        cleanupOldContentCacheFiles(cacheDirectory)
        val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "archive" }
        val cachedFile = File(cacheDirectory, "${System.currentTimeMillis()}-$safeName")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            cachedFile.outputStream().use { output -> input.copyToCancellable(output) }
        } ?: throw IOException(string(R.string.local_file_manager_unzip_missing))
        return cachedFile
    }

    private fun cleanupOldContentCacheFiles(cacheDirectory: File) {
        cacheDirectory.listFiles().orEmpty().forEach { cached ->
            if (System.currentTimeMillis() - cached.lastModified() > ArchiveExtractCacheMaxAgeMillis) {
                cached.delete()
            }
        }
    }

    private fun string(resId: Int): String = appContext.getString(resId)

    private companion object {
        const val ArchiveExtractCacheDirectory = "archive-extract-cache"
        const val ArchiveExtractCacheMaxAgeMillis = 5 * 60 * 1000L
    }
}