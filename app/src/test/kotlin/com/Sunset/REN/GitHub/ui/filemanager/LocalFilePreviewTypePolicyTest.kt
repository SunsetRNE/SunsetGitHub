package com.Sunset.REN.GitHub.ui.filemanager

import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFilePreviewTypePolicyTest {
    @Test
    fun markdownPreviewRecognizesMarkdownTypeAndReadmeNames() {
        assertTrue(
            LocalFilePreviewTypePolicy.canPreviewAsMarkdown(
                entryType = FileEntryType.Markdown,
                displayName = "notes.txt",
                displayPath = "/tmp/notes.txt"
            )
        )
        assertTrue(
            LocalFilePreviewTypePolicy.canPreviewAsMarkdown(
                entryType = FileEntryType.Text,
                displayName = "README",
                displayPath = "/repo/README"
            )
        )
        assertTrue(
            LocalFilePreviewTypePolicy.canPreviewAsMarkdown(
                entryType = FileEntryType.Text,
                displayName = "README.mdown",
                displayPath = "/repo/README.mdown"
            )
        )
    }

    @Test
    fun imagePreviewUsesTypeOrCommonImageExtensions() {
        assertTrue(LocalFilePreviewTypePolicy.canPreviewAsImage(FileEntryType.Image, "file.bin", "/tmp/file.bin"))
        assertTrue(LocalFilePreviewTypePolicy.canPreviewAsImage(FileEntryType.Unknown, "avatar.webp", "/tmp/avatar.webp"))
        assertTrue(LocalFilePreviewTypePolicy.canPreviewAsImage(FileEntryType.Unknown, "vector.svg", "/tmp/vector.svg"))
        assertFalse(LocalFilePreviewTypePolicy.canPreviewAsImage(FileEntryType.Text, "notes.txt", "/tmp/notes.txt"))
    }

    @Test
    fun archivePreviewAllowsSupportedArchiveTypes() {
        assertTrue(LocalFilePreviewTypePolicy.canPreviewAsZipArchive(FileEntryType.Archive, "source.zip", "/tmp/source.zip"))
        assertTrue(LocalFilePreviewTypePolicy.canPreviewAsZipArchive(FileEntryType.Archive, "library.aar", "/tmp/library.aar"))
        assertTrue(LocalFilePreviewTypePolicy.canPreviewAsZipArchive(FileEntryType.Archive, "source.tar", "/tmp/source.tar"))
        assertFalse(LocalFilePreviewTypePolicy.canPreviewAsZipArchive(FileEntryType.Unknown, "source.zip", "/tmp/source.zip"))
    }

    @Test
    fun apkPreviewUsesTypeOrApkExtension() {
        assertTrue(LocalFilePreviewTypePolicy.canPreviewAsApk(FileEntryType.Apk, "release.bin", "/tmp/release.bin"))
        assertTrue(LocalFilePreviewTypePolicy.canPreviewAsApk(FileEntryType.Unknown, "release.apk", "/tmp/release.apk"))
        assertFalse(LocalFilePreviewTypePolicy.canPreviewAsApk(FileEntryType.Archive, "release.zip", "/tmp/release.zip"))
    }

    @Test
    fun specializedPreviewIncludesImageApkAndZipArchiveOnly() {
        assertTrue(LocalFilePreviewTypePolicy.isSpecializedPreview(FileEntryType.Image, "image.bin", "/tmp/image.bin"))
        assertTrue(LocalFilePreviewTypePolicy.isSpecializedPreview(FileEntryType.Apk, "release.bin", "/tmp/release.bin"))
        assertTrue(LocalFilePreviewTypePolicy.isSpecializedPreview(FileEntryType.Archive, "bundle.jar", "/tmp/bundle.jar"))
        assertFalse(LocalFilePreviewTypePolicy.isSpecializedPreview(FileEntryType.Markdown, "README.md", "/repo/README.md"))
        assertFalse(LocalFilePreviewTypePolicy.isSpecializedPreview(FileEntryType.Text, "notes.txt", "/tmp/notes.txt"))
    }

    @Test
    fun markdownDefaultPreviewIsDisabledInEditMode() {
        assertTrue(
            LocalFilePreviewTypePolicy.shouldShowMarkdownPreviewByDefault(
                openMode = "preview",
                entryType = FileEntryType.Markdown,
                displayName = "README.md",
                displayPath = "/repo/README.md"
            )
        )
        assertFalse(
            LocalFilePreviewTypePolicy.shouldShowMarkdownPreviewByDefault(
                openMode = LocalFilePreviewTypePolicy.MODE_EDIT,
                entryType = FileEntryType.Markdown,
                displayName = "README.md",
                displayPath = "/repo/README.md"
            )
        )
    }
}