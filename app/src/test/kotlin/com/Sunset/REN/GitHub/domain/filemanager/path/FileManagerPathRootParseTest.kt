package com.Sunset.REN.GitHub.domain.filemanager.path

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileManagerPathRootParseTest {
    @Test
    fun parsesRootSchemeAsTypedRootPath() {
        val parsed = FileManagerPath.parse("root:///system/bin")

        assertTrue(parsed is FileManagerPath.Root)
        assertEquals("/system/bin", (parsed as FileManagerPath.Root).absolutePath)
    }

    @Test
    fun parsesBlankRootSchemeAsRootDirectory() {
        val parsed = FileManagerPath.parse("root://")

        assertTrue(parsed is FileManagerPath.Root)
        assertEquals("/", (parsed as FileManagerPath.Root).absolutePath)
    }
}
