package com.Sunset.REN.GitHub.domain.filemanager

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class FileManagerEntrySorterTest {
    @Test
    fun sortPlacesDirectoriesBeforeFiles() {
        val sorted = FileManagerEntrySorter.sort(
            listOf(
                entry("zeta.txt", FileEntryType.Text),
                entry("beta", FileEntryType.Directory),
                entry("alpha.txt", FileEntryType.Text),
                entry("alpha", FileEntryType.Directory)
            )
        )

        assertEquals(listOf("alpha", "beta", "alpha.txt", "zeta.txt"), sorted.map { it.name })
    }

    @Test
    fun sortComparesNamesIgnoringCaseWithinSameType() {
        val sorted = FileManagerEntrySorter.sort(
            listOf(
                entry("gamma.txt", FileEntryType.Text),
                entry("Beta.txt", FileEntryType.Text),
                entry("alpha.txt", FileEntryType.Text)
            )
        )

        assertEquals(listOf("alpha.txt", "Beta.txt", "gamma.txt"), sorted.map { it.name })
    }

    private fun entry(name: String, type: FileEntryType): FileManagerEntry {
        return FileManagerEntry(
            id = name,
            name = name,
            displayPath = name,
            type = type,
            source = FileEntrySource.LocalFile(File(name)),
            sizeBytes = if (type == FileEntryType.Directory) null else 1L,
            modifiedAtMillis = null,
            capabilities = FileEntryCapabilities(
                canRead = true,
                canWrite = true,
                canRename = true,
                canDelete = true,
                canCreateChild = type == FileEntryType.Directory,
                canUpload = type != FileEntryType.Directory,
                canAccessContent = FileContentAccessPolicy.canAccessContent(
                    type = type,
                    isFile = type != FileEntryType.Directory,
                    canRead = true
                ),
                canEditAsText = type in FileContentAccessPolicy.inlineTextTypes
            )
        )
    }
}