package com.Sunset.REN.GitHub.domain.filemanager

/**
 * Small controller for the manager's secondary/archive side pane.
 *
 * It keeps path refresh/back decisions outside the Fragment and makes archive
 * display paths a first-class concept for future ArchiveSidePane UI expansion.
 */
object ArchiveSidePaneController {
    fun shouldRefresh(currentRightPath: String?, changedPath: String): Boolean {
        return !currentRightPath.isNullOrBlank() && currentRightPath == changedPath
    }

    fun canWriteToPath(path: String): Boolean = OperationSafety.isWritableDirectoryTarget(path)

    fun entryDisplayPath(entry: FileManagerEntry): String {
        return when (val source = entry.source) {
            is FileEntrySource.ParentDirectory -> source.targetPath
            is FileEntrySource.LocalFile -> source.file.absolutePath
            is FileEntrySource.DocumentUri -> source.uri.toString()
            is FileEntrySource.ContentUri -> source.uri.toString()
            is FileEntrySource.ArchiveEntry -> archiveDisplayPath(source.archiveFile.absolutePath, source.innerPath)
            is FileEntrySource.RootPath -> "root://${source.absolutePath}"
        }
    }

    fun archiveDisplayPath(archiveFilePath: String, innerPath: String): String {
        return archiveFilePath + "!/" + innerPath.trimStart('/')
    }

    fun targetEntriesForPath(
        targetPath: String,
        leftPath: String,
        leftEntries: List<FileManagerEntry>,
        rightPath: String,
        rightEntries: List<FileManagerEntry>
    ): List<FileManagerEntry> {
        return when (targetPath) {
            leftPath -> leftEntries
            rightPath -> rightEntries
            else -> emptyList()
        }
    }

    fun conflictedEntriesForTarget(
        entries: List<FileManagerEntry>,
        targetEntries: List<FileManagerEntry>
    ): List<FileManagerEntry> {
        val targetNames = targetEntries
            .asSequence()
            .filterNot { it.type == FileEntryType.Parent }
            .map { it.name }
            .toSet()
        if (targetNames.isEmpty()) return emptyList()
        return entries.filter { entry -> entry.name in targetNames }
    }
}