package com.Sunset.REN.GitHub.domain.filemanager.capability

import com.Sunset.REN.GitHub.domain.filemanager.FileEntryCapabilities
import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.path.FileManagerPath
import com.Sunset.REN.GitHub.domain.filemanager.root.RootAccessState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileManagerCapabilityResolverRootSafetyTest {
    private val resolver = FileManagerCapabilityResolver()

    @Test
    fun rootDirectoryAdvancedWriteActionsAreVisibleButDisabledUntilSafetyFlowExecutes() {
        val actions = resolver.resolveDirectoryActions(
            currentPath = FileManagerPath.Root("/data"),
            rootState = RootAccessState.Granted,
            isDualPaneEnabled = false,
            showHiddenFiles = false
        ).overflow

        val permission = actions.first { it.id == FileManagerActionId.EditPermission }
        val owner = actions.first { it.id == FileManagerActionId.EditOwner }

        assertTrue(permission.visible)
        assertFalse(permission.enabled)
        assertTrue(permission.disabledReason!!.contains("安全确认"))
        assertTrue(owner.visible)
        assertFalse(owner.enabled)
    }

    @Test
    fun selectedRootEntriesDoNotEnablePrivilegedWriteActions() {
        val actions = resolver.resolveSelectionActions(
            entries = listOf(
                FileManagerEntry(
                    id = "root:/data/local/tmp/test",
                    name = "test",
                    displayPath = "/data/local/tmp/test",
                    type = FileEntryType.Directory,
                    source = FileEntrySource.RootPath("/data/local/tmp/test", isDirectory = true),
                    sizeBytes = null,
                    modifiedAtMillis = null,
                    capabilities = FileEntryCapabilities(
                        canRead = true,
                        canWrite = false,
                        canRename = false,
                        canDelete = false,
                        canCreateChild = false,
                        canUpload = false,
                        canAccessContent = false,
                        canEditAsText = false
                    )
                )
            ),
            rootState = RootAccessState.Granted
        )

        assertFalse(actions.first { it.id == FileManagerActionId.EditPermission }.enabled)
        assertFalse(actions.first { it.id == FileManagerActionId.EditOwner }.enabled)
        assertFalse(actions.first { it.id == FileManagerActionId.Delete }.enabled)
    }
}
