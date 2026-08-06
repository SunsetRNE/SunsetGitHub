package com.Sunset.REN.GitHub.ui.filemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFileEditorConfigPolicyTest {
    @Test
    fun languageModeRecognizesRepositoryCodeFiles() {
        assertEquals("kotlin", LocalFileEditorConfigPolicy.resolveEditorLanguageMode("MainActivity.kt"))
        assertEquals("kotlin", LocalFileEditorConfigPolicy.resolveEditorLanguageMode("build.gradle.kts"))
        assertEquals("java", LocalFileEditorConfigPolicy.resolveEditorLanguageMode("Main.java"))
        assertEquals("javascript", LocalFileEditorConfigPolicy.resolveEditorLanguageMode("index.mjs"))
        assertEquals("typescript", LocalFileEditorConfigPolicy.resolveEditorLanguageMode("component.tsx"))
        assertEquals("python", LocalFileEditorConfigPolicy.resolveEditorLanguageMode("script.py"))
        assertEquals("cpp", LocalFileEditorConfigPolicy.resolveEditorLanguageMode("include/header.hpp"))
        assertEquals("shell", LocalFileEditorConfigPolicy.resolveEditorLanguageMode("setup.sh"))
    }

    @Test
    fun languageModeRecognizesMarkupAndConfigFiles() {
        assertEquals("markdown", LocalFileEditorConfigPolicy.resolveEditorLanguageMode("README"))
        assertEquals("markdown", LocalFileEditorConfigPolicy.resolveEditorLanguageMode("docs/README.mdown"))
        assertEquals("json", LocalFileEditorConfigPolicy.resolveEditorLanguageMode("package.json"))
        assertEquals("xml", LocalFileEditorConfigPolicy.resolveEditorLanguageMode("layout.xml"))
        assertEquals("yaml", LocalFileEditorConfigPolicy.resolveEditorLanguageMode("workflow.yml"))
        assertNull(LocalFileEditorConfigPolicy.resolveEditorLanguageMode("archive.zip"))
    }

    @Test
    fun softWrapIsEnabledForMarkdownTextAndLogsOnly() {
        assertTrue(LocalFileEditorConfigPolicy.shouldUseSoftWrap("README.md"))
        assertTrue(LocalFileEditorConfigPolicy.shouldUseSoftWrap("notes.txt"))
        assertTrue(LocalFileEditorConfigPolicy.shouldUseSoftWrap("debug.log"))
        assertFalse(LocalFileEditorConfigPolicy.shouldUseSoftWrap("MainActivity.kt"))
        assertFalse(LocalFileEditorConfigPolicy.shouldUseSoftWrap("data.json"))
    }
}