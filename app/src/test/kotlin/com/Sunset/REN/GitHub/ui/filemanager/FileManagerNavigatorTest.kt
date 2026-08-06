package com.Sunset.REN.GitHub.ui.filemanager

import com.Sunset.REN.GitHub.ui.filemanager.FileManagerNavigator.DirectoryPathTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileManagerNavigatorTest {
    private val navigator = FileManagerNavigator()

    @Test
    fun `parse root display path requires root prefix and absolute path`() {
        assertEquals("/data/local/tmp", navigator.parseRootDisplayPath("root:///data/local/tmp"))
        assertEquals("/", navigator.parseRootDisplayPath("root:///"))
        assertNull(navigator.parseRootDisplayPath("root://relative/path"))
        assertNull(navigator.parseRootDisplayPath("/data/local/tmp"))
    }

    @Test
    fun `normalize root path accepts prefixed and unprefixed values`() {
        assertEquals("/system", navigator.normalizeRootPath("root:///system/"))
        assertEquals("/system/bin", navigator.normalizeRootPath("system/bin"))
        assertEquals("/", navigator.normalizeRootPath(""))
    }

    @Test
    fun `parse archive display path normalizes inner path`() {
        val target = navigator.parseArchiveDisplayPath("/sdcard/archive.zip!//dir//nested/file.txt")

        requireNotNull(target)
        assertEquals("/sdcard/archive.zip", target.archiveFile.absolutePath)
        assertEquals("dir/nested/file.txt", target.innerPath)
    }

    @Test
    fun `archive parent inner path returns parent or null for root`() {
        assertEquals("dir/nested", navigator.archiveParentInnerPath("dir/nested/file.txt"))
        assertEquals("", navigator.archiveParentInnerPath("file.txt"))
        assertNull(navigator.archiveParentInnerPath("/"))
    }

    @Test
    fun `parse directory path recognizes archive before local path`() {
        val target = navigator.parseDirectoryPath("/sdcard/archive.zip!/assets/readme.md")

        assertTrue(target is DirectoryPathTarget.Archive)
        target as DirectoryPathTarget.Archive
        assertEquals("/sdcard/archive.zip", target.archiveFile.absolutePath)
        assertEquals("assets/readme.md", target.innerPath)
    }

    @Test
    fun `right pane lookup preserves explicit root display path`() {
        assertEquals("root:///system/etc", navigator.rightPaneLookupPath("root:///system/etc", isRootGranted = true))
        assertEquals("root:///system/etc", navigator.rightPaneLookupPath("root:///system/etc", isRootGranted = false))
    }
}
