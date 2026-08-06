package com.Sunset.REN.GitHub.domain.filemanager.capability

import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry

object FileActionVisibilityPolicy {
    fun isArchiveContext(entries: List<FileManagerEntry>): Boolean = entries.any { it.source is FileEntrySource.ArchiveEntry }
    fun isSafContext(entries: List<FileManagerEntry>): Boolean = entries.any { it.source is FileEntrySource.DocumentUri }

    fun selectionCapabilities(entries: List<FileManagerEntry>): FileManagerCapabilitySet {
        if (entries.isEmpty()) return FileManagerCapabilitySet()
        val archiveContext = isArchiveContext(entries)
        return FileManagerCapabilitySet(
            canOpen = entries.size == 1 && entries.first().capabilities.canRead,
            canPreview = entries.size == 1 && entries.first().capabilities.canAccessContent,
            canRename = entries.size == 1 && entries.first().capabilities.canRename && !archiveContext,
            canDelete = entries.all { it.capabilities.canDelete } && !archiveContext,
            canCopy = entries.all { it.capabilities.canRead || it.capabilities.canAccessContent },
            canMove = entries.all { it.capabilities.canDelete && it.capabilities.canRead } && !archiveContext,
            canCompress = entries.all { it.capabilities.canRead } && !archiveContext,
            canExtract = entries.any { it.source is FileEntrySource.ArchiveEntry || it.name.endsWith(".zip", true) || it.name.endsWith(".apk", true) },
            canCreateChild = false,
            canEditPermission = false,
            canEditOwner = false
        )
    }
}
