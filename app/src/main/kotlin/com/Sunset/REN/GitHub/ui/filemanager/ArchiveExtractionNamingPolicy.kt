package com.Sunset.REN.GitHub.ui.filemanager

import androidx.documentfile.provider.DocumentFile
import java.io.File

class ArchiveExtractionNamingPolicy(
    private val defaultArchiveName: String
) {
    fun nextAvailableLocalUnzipDirectory(parent: File, archiveName: String): File {
        val baseName = ArchiveExtractionPolicy.archiveBaseName(archiveName, defaultArchiveName)
        var index = 1
        while (true) {
            val candidate = File(parent, if (index == 1) baseName else "$baseName $index")
            if (!candidate.exists()) return candidate
            index++
        }
    }

    fun nextAvailableSafUnzipDirectoryName(parent: DocumentFile, archiveName: String): String {
        val baseName = ArchiveExtractionPolicy.archiveBaseName(archiveName, defaultArchiveName)
        var index = 1
        while (true) {
            val candidate = if (index == 1) baseName else "$baseName $index"
            if (parent.findFile(candidate) == null) return candidate
            index++
        }
    }
}