package com.Sunset.REN.GitHub.domain.filemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FileManagerPaneNavigationStateTest {
    @Test
    fun `normalizes paths when creating root`() {
        val state = FileManagerPaneNavigationState.root(FileManagerPaneId.Left, " /storage/emulated/0/Download/ ")

        assertEquals("/storage/emulated/0/Download", state.currentPath)
        assertFalse(state.canGoBack)
        assertTrue(state.canGoUp)
    }

    @Test
    fun `enter pushes previous path to back stack`() {
        val state = FileManagerPaneNavigationState.root(FileManagerPaneId.Left, "/workspace")
            .enter("/workspace/app")
            .enter("/workspace/app/src")

        assertEquals("/workspace/app/src", state.currentPath)
        assertEquals(listOf("/workspace", "/workspace/app"), state.backStack)
        assertTrue(state.canGoBack)
    }

    @Test
    fun `entering same path is no-op`() {
        val state = FileManagerPaneNavigationState.root(FileManagerPaneId.Left, "/workspace")
        val updated = state.enter("/workspace/")

        assertSame(state, updated)
    }

    @Test
    fun `go back restores last pane path only`() {
        val state = FileManagerPaneNavigationState.root(FileManagerPaneId.Left, "/workspace")
            .enter("/workspace/app")
            .enter("/workspace/app/src")
            .goBack()

        assertEquals("/workspace/app", state.currentPath)
        assertEquals(listOf("/workspace"), state.backStack)
    }

    @Test
    fun `replace clears back stack`() {
        val state = FileManagerPaneNavigationState.root(FileManagerPaneId.Left, "/workspace")
            .enter("/workspace/app")
            .replace("/sdcard/Download")

        assertEquals("/sdcard/Download", state.currentPath)
        assertFalse(state.canGoBack)
    }

    @Test
    fun `go up navigates to parent and records previous path`() {
        val state = FileManagerPaneNavigationState.root(FileManagerPaneId.Left, "/workspace/app/src")
            .goUp()

        assertEquals("/workspace/app", state.currentPath)
        assertEquals(listOf("/workspace/app/src"), state.backStack)
    }

    @Test
    fun `root cannot go up`() {
        val state = FileManagerPaneNavigationState.root(FileManagerPaneId.Left, "/")

        assertFalse(state.canGoUp)
        assertSame(state, state.goUp())
    }

    @Test
    fun `dual pane navigation keeps left and right stacks independent`() {
        val state = FileManagerDualPaneNavigationState.roots(
            leftPath = "/workspace",
            rightPath = "/sdcard"
        )
            .enter(FileManagerPaneId.Left, "/workspace/app")
            .enter(FileManagerPaneId.Right, "/sdcard/Download")
            .goBack(FileManagerPaneId.Left)

        assertEquals("/workspace", state.left.currentPath)
        assertEquals(emptyList<String>(), state.left.backStack)
        assertEquals("/sdcard/Download", state.right.currentPath)
        assertEquals(listOf("/sdcard"), state.right.backStack)
    }

    @Test
    fun `dual pane replace only resets requested pane`() {
        val state = FileManagerDualPaneNavigationState.roots(
            leftPath = "/workspace",
            rightPath = "/sdcard"
        )
            .enter(FileManagerPaneId.Left, "/workspace/app")
            .enter(FileManagerPaneId.Right, "/sdcard/Download")
            .replace(FileManagerPaneId.Right, "/storage/emulated/0/Pictures")

        assertEquals("/workspace/app", state.left.currentPath)
        assertEquals(listOf("/workspace"), state.left.backStack)
        assertEquals("/storage/emulated/0/Pictures", state.right.currentPath)
        assertEquals(emptyList<String>(), state.right.backStack)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank path is rejected`() {
        FileManagerPaneNavigationState.root(FileManagerPaneId.Left, " ")
    }
}