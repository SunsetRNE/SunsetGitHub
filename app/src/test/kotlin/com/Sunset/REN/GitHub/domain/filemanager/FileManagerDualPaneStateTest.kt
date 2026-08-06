package com.Sunset.REN.GitHub.domain.filemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileManagerDualPaneStateTest {
    @Test
    fun `uses single pane on phone portrait`() {
        val state = FileManagerDualPaneState.fromConfiguration(
            screenWidthDp = 393,
            isLandscape = false
        )

        assertFalse(state.isDualPane)
        assertEquals(FileManagerPaneId.Left, state.focusedPane)
    }

    @Test
    fun `uses single pane on narrow landscape`() {
        val state = FileManagerDualPaneState.fromConfiguration(
            screenWidthDp = 599,
            isLandscape = true
        )

        assertFalse(state.isDualPane)
    }

    @Test
    fun `uses dual pane on wide landscape when enabled`() {
        val state = FileManagerDualPaneState.fromConfiguration(
            screenWidthDp = 600,
            isLandscape = true,
            userEnabledDualPane = true
        )

        assertTrue(state.isDualPane)
        assertEquals(FileManagerPaneId.Left, state.sourcePane)
        assertEquals(FileManagerPaneId.Right, state.targetPane)
    }

    @Test
    fun `respects user disabled dual pane preference`() {
        val state = FileManagerDualPaneState.fromConfiguration(
            screenWidthDp = 840,
            isLandscape = true,
            userEnabledDualPane = false
        )

        assertFalse(state.isDualPane)
    }

    @Test
    fun `toggles focus only in dual pane`() {
        val dualPaneState = FileManagerDualPaneState(isDualPane = true)
        val singlePaneState = FileManagerDualPaneState(isDualPane = false)

        assertEquals(FileManagerPaneId.Right, dualPaneState.toggleFocus().focusedPane)
        assertEquals(FileManagerPaneId.Left, singlePaneState.toggleFocus().focusedPane)
    }

    @Test
    fun `source pane change updates opposite target pane`() {
        val state = FileManagerDualPaneState(isDualPane = true)
            .withSourcePane(FileManagerPaneId.Right)

        assertEquals(FileManagerPaneId.Right, state.sourcePane)
        assertEquals(FileManagerPaneId.Left, state.targetPane)
        assertEquals(FileManagerPaneId.Right, state.focusedPane)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `source and target pane cannot be the same`() {
        FileManagerDualPaneState(
            isDualPane = true,
            sourcePane = FileManagerPaneId.Left,
            targetPane = FileManagerPaneId.Left
        )
    }
}
