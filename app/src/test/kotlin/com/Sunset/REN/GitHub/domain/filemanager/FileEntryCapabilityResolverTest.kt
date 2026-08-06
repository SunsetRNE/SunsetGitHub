package com.Sunset.REN.GitHub.domain.filemanager

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileEntryCapabilityResolverTest {
    @Test
    fun resolveAllowsWritableDirectoryChildCreationButNotUpload() {
        val capabilities = FileEntryCapabilityResolver.resolve(
            type = FileEntryType.Directory,
            isFile = false,
            isDirectory = true,
            canRead = true,
            canWrite = true
        )

        assertTrue(capabilities.canRead)
        assertTrue(capabilities.canWrite)
        assertTrue(capabilities.canRename)
        assertTrue(capabilities.canDelete)
        assertTrue(capabilities.canCreateChild)
        assertFalse(capabilities.canUpload)
        assertFalse(capabilities.canEditAsText)
    }

    @Test
    fun resolveAllowsReadableTextFileUploadAndTextEdit() {
        val capabilities = FileEntryCapabilityResolver.resolve(
            type = FileEntryType.Text,
            isFile = true,
            isDirectory = false,
            canRead = true,
            canWrite = true
        )

        assertTrue(capabilities.canUpload)
        assertTrue(capabilities.canEditAsText)
        assertTrue(capabilities.canRename)
        assertTrue(capabilities.canDelete)
        assertFalse(capabilities.canCreateChild)
    }

    @Test
    fun resolveDisallowsUploadAndEditWhenFileCannotBeRead() {
        val capabilities = FileEntryCapabilityResolver.resolve(
            type = FileEntryType.Text,
            isFile = true,
            isDirectory = false,
            canRead = false,
            canWrite = true
        )

        assertFalse(capabilities.canRead)
        assertTrue(capabilities.canWrite)
        assertFalse(capabilities.canUpload)
        assertFalse(capabilities.canEditAsText)
    }

    @Test
    fun resolveAllowsBinaryUploadButNotTextEdit() {
        val capabilities = FileEntryCapabilityResolver.resolve(
            type = FileEntryType.Binary,
            isFile = true,
            isDirectory = false,
            canRead = true,
            canWrite = false
        )

        assertTrue(capabilities.canUpload)
        assertFalse(capabilities.canEditAsText)
        assertFalse(capabilities.canWrite)
        assertFalse(capabilities.canRename)
        assertFalse(capabilities.canDelete)
    }

    @Test
    fun resolveAllowsSpecializedReadablePreviewButNotTextEdit() {
        listOf(FileEntryType.Image, FileEntryType.Archive, FileEntryType.Apk).forEach { type ->
            val capabilities = FileEntryCapabilityResolver.resolve(
                type = type,
                isFile = true,
                isDirectory = false,
                canRead = true,
                canWrite = false
            )

            assertTrue(capabilities.canAccessContent)
            assertFalse(capabilities.canEditAsText)
            assertTrue(capabilities.canUpload)
        }
    }

    @Test
    fun resolveDisallowsChildCreationForReadOnlyDirectory() {
        val capabilities = FileEntryCapabilityResolver.resolve(
            type = FileEntryType.Directory,
            isFile = false,
            isDirectory = true,
            canRead = true,
            canWrite = false
        )

        assertTrue(capabilities.canRead)
        assertFalse(capabilities.canWrite)
        assertFalse(capabilities.canCreateChild)
        assertFalse(capabilities.canRename)
        assertFalse(capabilities.canDelete)
    }
}