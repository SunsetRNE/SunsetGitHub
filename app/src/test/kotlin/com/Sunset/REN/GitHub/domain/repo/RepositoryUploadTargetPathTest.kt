package com.Sunset.REN.GitHub.domain.repo

import org.junit.Assert.assertEquals
import org.junit.Test

class RepositoryUploadTargetPathTest {
    @Test
    fun resolve_usesDisplayNameWhenTargetIsRoot() {
        assertEquals(
            "README.md",
            RepositoryUploadTargetPath.resolve("/", "README.md")
        )
    }

    @Test
    fun resolve_appendsDisplayNameWhenTargetIsDirectory() {
        assertEquals(
            "docs/README.md",
            RepositoryUploadTargetPath.resolve("/docs/", "README.md")
        )
    }

    @Test
    fun resolve_keepsExplicitFilePath() {
        assertEquals(
            "docs/guide.md",
            RepositoryUploadTargetPath.resolve("/docs/guide.md", "README.md")
        )
    }

    @Test
    fun sanitize_normalizesSeparatorsAndRepeatedSlashes() {
        assertEquals(
            "docs/guide.md",
            RepositoryUploadTargetPath.sanitize("docs\\guide.md")
        )
        assertEquals(
            "/docs/guide.md",
            RepositoryUploadTargetPath.sanitize("//docs///guide.md")
        )
    }

    @Test
    fun defaultDirectoryForDisplayName_preservesSourceDirectory() {
        assertEquals(
            "/docs/guides/",
            RepositoryUploadTargetPath.defaultDirectoryForDisplayName("docs/guides/README.md")
        )
        assertEquals(
            "/",
            RepositoryUploadTargetPath.defaultDirectoryForDisplayName("README.md")
        )
    }

    @Test
    fun normalizeTargetPathAsDirectory_usesParentForExplicitFilePath() {
        assertEquals(
            "/docs/",
            RepositoryUploadTargetPath.normalizeTargetPathAsDirectory("/docs/guide.md")
        )
        assertEquals(
            "/docs/",
            RepositoryUploadTargetPath.normalizeTargetPathAsDirectory("/docs/")
        )
    }

    @Test
    fun expandDirectoryOptions_includesRootAndEachParent() {
        assertEquals(
            listOf("/", "/docs/", "/docs/guides/"),
            RepositoryUploadTargetPath.expandDirectoryOptions("/docs/guides/")
        )
    }
}
