package com.Sunset.REN.GitHub.ui.filemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveExtractionPolicyTest {
    @Test
    fun `archive base name removes compound suffixes before simple suffixes`() {
        assertEquals("backup", ArchiveExtractionPolicy.archiveBaseName("/sdcard/backup.tar.gz", "archive"))
        assertEquals("backup", ArchiveExtractionPolicy.archiveBaseName("backup.tgz", "archive"))
        assertEquals("app", ArchiveExtractionPolicy.archiveBaseName("app.zip", "archive"))
        assertEquals("archive", ArchiveExtractionPolicy.archiveBaseName("", "archive"))
    }

    @Test
    fun `single compressed entry name maps compressed tar aliases`() {
        assertEquals("backup.tar", ArchiveExtractionPolicy.singleCompressedEntryName("backup.tar.gz", "entry"))
        assertEquals("backup.tar", ArchiveExtractionPolicy.singleCompressedEntryName("backup.tgz", "entry"))
        assertEquals("data", ArchiveExtractionPolicy.singleCompressedEntryName("data.gz", "entry"))
        assertEquals("entry", ArchiveExtractionPolicy.singleCompressedEntryName("", "entry"))
    }

    @Test
    fun `tar like names include tar and compressed tar variants`() {
        assertTrue(ArchiveExtractionPolicy.isTarLikeName("archive.tar"))
        assertTrue(ArchiveExtractionPolicy.isTarLikeName("archive.tar.gz"))
        assertTrue(ArchiveExtractionPolicy.isTarLikeName("archive.tbz2"))
        assertFalse(ArchiveExtractionPolicy.isTarLikeName("archive.zip"))
    }

    @Test
    fun `safe archive path parts reject traversal`() {
        assertEquals(listOf("dir", "file.txt"), ArchiveExtractionPolicy.safeArchivePathParts("dir/file.txt"))
        assertEquals(listOf("dir", "file.txt"), ArchiveExtractionPolicy.safeArchivePathParts("/dir//file.txt"))
    }

    @Test(expected = UnsafeArchiveEntryException::class)
    fun `safe archive path parts rejects parent traversal`() {
        ArchiveExtractionPolicy.safeArchivePathParts("../evil.txt")
    }

    @Test(expected = UnsafeArchiveEntryException::class)
    fun `safe archive path parts rejects nested parent traversal`() {
        ArchiveExtractionPolicy.safeArchivePathParts("safe/../evil.txt")
    }
}
