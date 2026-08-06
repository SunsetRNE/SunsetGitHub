package com.Sunset.REN.GitHub.data.filemanager.provider

import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootDirectoryListingParserTest {
    @Test
    fun parsesDirectoryListingAsReadOnlyRootEntries() {
        val entries = RootDirectoryListingParser.parseDirectory(
            "/data",
            """
                total 8
                drwxr-xr-x  2 root root 4096 2026-01-01 00:00 local
                -rw-r--r--  1 root root   12 2026-01-01 00:00 config.txt
                lrwxrwxrwx  1 root root   10 2026-01-01 00:00 link -> /system/bin
            """.trimIndent()
        )

        val local = entries.first { it.name == "local" }
        val config = entries.first { it.name == "config.txt" }

        assertEquals("/data/local", local.displayPath)
        assertEquals(FileEntryType.Directory, local.type)
        assertTrue(local.source is FileEntrySource.RootPath)
        assertTrue(local.capabilities.canRead)
        assertFalse(local.capabilities.canDelete)
        assertTrue(config.capabilities.canRead)
        assertTrue(config.capabilities.canAccessContent)
        assertEquals(12L, config.sizeBytes)
    }

    @Test
    fun parsesStatOutputWithAbsolutePath() {
        val entry = RootDirectoryListingParser.parseStat(
            "/system/build.prop",
            "-rw-r--r-- 1 root root 42 2026-01-01 00:00 /system/build.prop"
        )!!

        assertEquals("build.prop", entry.name)
        assertEquals("/system/build.prop", entry.displayPath)
        assertEquals(42L, entry.sizeBytes)
    }

    @Test
    fun ignoresMalformedTotalAndDotEntries() {
        val entries = RootDirectoryListingParser.parseDirectory(
            "/",
            """
                total 12
                drwxr-xr-x 2 root root 4096 2026-01-01 00:00 .
                drwxr-xr-x 2 root root 4096 2026-01-01 00:00 ..
                not-enough-columns
                drwxr-xr-x 2 root root 4096 2026-01-01 00:00 data
            """.trimIndent()
        )

        assertEquals(listOf("data"), entries.map { it.name })
        assertEquals("/data", entries.single().displayPath)
    }

    @Test
    fun parsesSymlinkNameWithoutOpeningRootWriteCapabilities() {
        val entry = RootDirectoryListingParser.parseLine(
            "/system",
            "lrwxrwxrwx 1 root root 10 2026-01-01 00:00 bin -> /system/bin"
        )!!

        assertEquals("bin", entry.name)
        assertEquals("/system/bin", entry.displayPath)
        assertFalse(entry.capabilities.canRead)
        assertFalse(entry.capabilities.canWrite)
        assertFalse(entry.capabilities.canDelete)
    }
}
