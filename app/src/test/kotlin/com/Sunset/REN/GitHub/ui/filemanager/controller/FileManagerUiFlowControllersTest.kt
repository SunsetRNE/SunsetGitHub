package com.Sunset.REN.GitHub.ui.filemanager.controller

import com.Sunset.REN.GitHub.domain.filemanager.FileEntryCapabilities
import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerDualPaneState
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerPaneId
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerSearchOptions
import com.Sunset.REN.GitHub.ui.filemanager.DrawerTab
import com.Sunset.REN.GitHub.ui.filemanager.ManualTransferOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FileManagerUiFlowControllersTest {
    @Test
    fun drawerLogicBoundsExpandedHeightAndIndicator() {
        assertEquals(160, FileManagerDrawerLogic.expandedHeight(screenHeightPixels = 200, density = 1f))
        assertEquals(280, FileManagerDrawerLogic.expandedHeight(screenHeightPixels = 2000, density = 1f))
        assertEquals(224, FileManagerDrawerLogic.expandedHeight(screenHeightPixels = 800, density = 1f))
        assertEquals("⌃", FileManagerDrawerLogic.handleText(height = 100, expandedHeight = 224))
        assertEquals("⌄", FileManagerDrawerLogic.handleText(height = 113, expandedHeight = 224))
        assertEquals(0f, FileManagerDrawerLogic.tabIndicatorX(DrawerTab.Recent, density = 2f))
        assertEquals(148f, FileManagerDrawerLogic.tabIndicatorX(DrawerTab.Favorite, density = 2f))
    }

    @Test
    fun selectionControllerPrefersRightPaneSelectionWhenPresent() {
        val left = listOf(entry("left-a", "same.txt"), entry("left-b", "left.txt"))
        val right = listOf(entry("right-a", "same.txt"), entry("right-b", "right.txt"))

        val selected = FileManagerSelectionActionController.selectedEntries(
            leftSelectedIds = setOf("left-b"),
            rightSelectedIds = setOf("right-a"),
            leftEntries = left,
            rightEntries = right
        )

        assertTrue(FileManagerSelectionActionController.hasSelection(emptySet(), setOf("right-a")))
        assertEquals(listOf("right-a"), selected.map { it.id })
    }

    @Test
    fun selectionControllerFindsConflictsOnlyForWritableTargets() {
        val source = listOf(entry("source", "exists.txt"), entry("other", "other.txt"))
        val target = listOf(entry("target", "exists.txt"), entry("parent", "..", FileEntryType.Parent))

        val conflicts = FileManagerSelectionActionController.conflictedEntriesForTarget(source, "/tmp", target)
        val rootConflicts = FileManagerSelectionActionController.conflictedEntriesForTarget(source, "root:///tmp", target)

        assertEquals(listOf("exists.txt"), conflicts.map { it.name })
        assertTrue(rootConflicts.isEmpty())
    }

    @Test
    fun transferTargetControllerBuildsDistinctWritableOptions() {
        val options = FileManagerTransferTargetController.buildTransferTargetOptionsFromTargets(
            activePath = "/work",
            includeParent = true,
            parentPathResolver = { "/" },
            dualPaneTargetResolver = { "/target" },
            authorizedTargets = listOf(
                "授权A" to "content://tree/a",
                "授权A重复" to "content://tree/a",
                "RootBlocked" to "root:///blocked"
            ),
            dualPaneEnabled = true
        )

        assertEquals(
            listOf("当前目录：/work", "上级目录：/", "目标窗格：/target", "授权目录：授权A"),
            options.map { it.label }
        )
        assertEquals(4, options.distinctBy { it.path }.size)
    }

    @Test
    fun transferActionControllerBlocksRootWritesAndLabelsOperations() {
        assertTrue(FileManagerTransferActionController.shouldBlockRootWrite("root:///system"))
        assertFalse(FileManagerTransferActionController.canUseManualTarget("root:///system"))
        assertTrue(FileManagerTransferActionController.canUseManualTarget("content://tree/a"))
        assertEquals("复制到…", FileManagerTransferActionController.describeOperation(ManualTransferOperation.Copy))
        assertEquals("移动到…", FileManagerTransferActionController.describeOperation(ManualTransferOperation.Move))
    }

    @Test
    fun pathEditControllerNormalizesSdcardAliasesAndWhitespace() {
        assertEquals("/storage/emulated/0", FileManagerPathEditController.normalizeInlinePath(" /sdcard/ ", "/storage/emulated/0/"))
        assertEquals("/storage/emulated/0/Download", FileManagerPathEditController.normalizeInlinePath("/sdcard/Download/", "/storage/emulated/0"))
        assertEquals("root:///system", FileManagerPathEditController.normalizeInlinePath(" root:///system/ ", "/storage/emulated/0"))
    }

    @Test
    fun searchFlowControllerSelectsFocusedPaneAndBuildsDescription() {
        val left = listOf(entry("left-visible", "Visible.txt"), entry("left-hidden", ".hidden"))
        val right = listOf(entry("right-visible", "Right.txt"), entry("right-dir", "Folder", FileEntryType.Directory))
        val focusedRight = FileManagerDualPaneState(isDualPane = true, focusedPane = FileManagerPaneId.Right)
        val options = FileManagerSearchOptions(query = "right", includeSubdirectories = false, includeFiles = true, includeDirectories = false)

        val source = FileManagerSearchFlowController.currentListSearchSource(focusedRight, leftEntries = left, rightEntries = right)
        val filtered = FileManagerSearchFlowController.filterCurrentList(source, options)

        assertEquals(listOf("right-visible"), filtered.map { it.id })
        assertEquals("当前目录  ·  仅文件", FileManagerSearchFlowController.modeDescription(options))
        assertEquals(
            "递归  ·  文件+文件夹  ·  区分大小写",
            FileManagerSearchFlowController.modeDescription(FileManagerSearchOptions(query = "x", caseSensitive = true))
        )
    }

    private fun entry(id: String, name: String, type: FileEntryType = FileEntryType.Text): FileManagerEntry {
        return FileManagerEntry(
            id = id,
            name = name,
            displayPath = "/tmp/$name",
            type = type,
            source = FileEntrySource.LocalFile(File("/tmp/$name")),
            sizeBytes = if (type == FileEntryType.Directory) null else 1L,
            modifiedAtMillis = null,
            capabilities = FileEntryCapabilities(
                canRead = true,
                canWrite = true,
                canRename = true,
                canDelete = true,
                canCreateChild = type == FileEntryType.Directory,
                canUpload = type != FileEntryType.Directory,
                canAccessContent = type != FileEntryType.Directory,
                canEditAsText = type == FileEntryType.Text
            )
        )
    }
}
