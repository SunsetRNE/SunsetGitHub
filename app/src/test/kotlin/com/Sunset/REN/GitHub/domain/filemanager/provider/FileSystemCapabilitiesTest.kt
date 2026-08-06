package com.Sunset.REN.GitHub.domain.filemanager.provider

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileSystemCapabilitiesTest {
    @Test
    fun rootGrantedExposesReadOnlyBrowsingUntilPrivilegedOperationsAreOpened() {
        val capabilities = FileSystemCapabilities.RootGranted

        assertTrue(capabilities.canList)
        assertTrue(capabilities.canStat)
        assertTrue(capabilities.canRead)
        assertFalse(capabilities.canWrite)
        assertFalse(capabilities.canCreateDirectory)
        assertFalse(capabilities.canRename)
        assertFalse(capabilities.canDelete)
        assertFalse(capabilities.canEditPermission)
        assertFalse(capabilities.canEditOwner)
    }
}
