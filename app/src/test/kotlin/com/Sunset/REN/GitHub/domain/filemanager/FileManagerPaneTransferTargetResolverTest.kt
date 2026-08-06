package com.Sunset.REN.GitHub.domain.filemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileManagerPaneTransferTargetResolverTest {
    @Test
    fun `returns null when single pane mode has no explicit target pane`() {
        val target = FileManagerPaneTransferTargetResolver.resolve(
            paneState = FileManagerDualPaneState(isDualPane = false),
            navigationState = FileManagerDualPaneNavigationState.roots(
                leftPath = "/workspace",
                rightPath = "/sdcard/Download"
            )
        )

        assertNull(target)
    }

    @Test
    fun `uses opposite pane as explicit transfer target in dual pane mode`() {
        val target = FileManagerPaneTransferTargetResolver.resolve(
            paneState = FileManagerDualPaneState(isDualPane = true),
            navigationState = FileManagerDualPaneNavigationState.roots(
                leftPath = "/workspace/app",
                rightPath = "/sdcard/Download"
            )
        )

        requireNotNull(target)
        assertEquals(FileManagerPaneId.Left, target.sourcePane)
        assertEquals(FileManagerPaneId.Right, target.targetPane)
        assertEquals("/workspace/app", target.sourcePath)
        assertEquals("/sdcard/Download", target.targetPath)
        assertTrue(target.isExplicitDualPaneTarget)
        assertTrue(target.hasDistinctTargetPath)
    }

    @Test
    fun `source pane switch flips transfer target pane`() {
        val target = FileManagerPaneTransferTargetResolver.resolve(
            paneState = FileManagerDualPaneState(isDualPane = true)
                .withSourcePane(FileManagerPaneId.Right),
            navigationState = FileManagerDualPaneNavigationState.roots(
                leftPath = "/workspace/app",
                rightPath = "/sdcard/Download"
            )
        )

        requireNotNull(target)
        assertEquals(FileManagerPaneId.Right, target.sourcePane)
        assertEquals(FileManagerPaneId.Left, target.targetPane)
        assertEquals("/sdcard/Download", target.sourcePath)
        assertEquals("/workspace/app", target.targetPath)
    }

    @Test
    fun `same source and target path is explicit but not distinct`() {
        val target = FileManagerPaneTransferTargetResolver.resolve(
            paneState = FileManagerDualPaneState(isDualPane = true),
            navigationState = FileManagerDualPaneNavigationState.roots(
                leftPath = "/workspace/app",
                rightPath = "/workspace/app"
            )
        )

        requireNotNull(target)
        assertFalse(target.hasDistinctTargetPath)
    }
}