package com.Sunset.REN.GitHub.domain.filemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileEntryTypeResolverTest {
    @Test
    fun resolveReturnsDirectoryWhenEntryIsDirectory() {
        val type = FileEntryTypeResolver.resolve(
            name = "README.md",
            mimeType = "text/markdown",
            isDirectory = true
        )

        assertEquals(FileEntryType.Directory, type)
    }

    @Test
    fun resolvePrefersMimeTypeForTextAndImageContent() {
        assertEquals(
            FileEntryType.Text,
            FileEntryTypeResolver.resolve(name = "unknown", mimeType = "text/plain")
        )
        assertEquals(
            FileEntryType.Image,
            FileEntryTypeResolver.resolve(name = "unknown", mimeType = "image/png")
        )
    }

    @Test
    fun resolveClassifiesCommonRepositoryFileExtensions() {
        assertEquals(FileEntryType.Markdown, FileEntryTypeResolver.resolve("README.md"))
        assertEquals(FileEntryType.Code, FileEntryTypeResolver.resolve("MainActivity.kt"))
        assertEquals(FileEntryType.Text, FileEntryTypeResolver.resolve("gradle.properties"))
        assertEquals(FileEntryType.Image, FileEntryTypeResolver.resolve("avatar.webp"))
        assertEquals(FileEntryType.Archive, FileEntryTypeResolver.resolve("source.tar"))
        assertEquals(FileEntryType.Apk, FileEntryTypeResolver.resolve("release.apk"))
        assertEquals(FileEntryType.Binary, FileEntryTypeResolver.resolve("classes.dex"))
        assertEquals(FileEntryType.Text, FileEntryTypeResolver.resolve("LICENSE"))
        assertEquals(FileEntryType.Code, FileEntryTypeResolver.resolve("build.gradle.kts"))
        assertEquals(FileEntryType.Text, FileEntryTypeResolver.resolve(".gitignore"))
        assertEquals(FileEntryType.Code, FileEntryTypeResolver.resolve("Dockerfile"))
    }

    @Test
    fun resolveUsesSignatureWhenNameAndMimeTypeAreUnknown() {
        assertEquals(
            FileEntryType.Image,
            FileEntryTypeResolver.resolve("unknown", sampleBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
        )
        assertEquals(
            FileEntryType.Archive,
            FileEntryTypeResolver.resolve("unknown", sampleBytes = byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 3, 4))
        )
        assertEquals(
            FileEntryType.Binary,
            FileEntryTypeResolver.resolve("unknown", sampleBytes = byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte()))
        )
        assertEquals(
            FileEntryType.Text,
            FileEntryTypeResolver.resolve("unknown", sampleBytes = "hello\nworld".toByteArray())
        )
    }

    @Test
    fun verifiedResolverAllowsBinarySignatureToOverrideTextLikeName() {
        assertEquals(
            FileEntryType.Binary,
            FileEntryTypeResolver.resolveVerified(
                name = "fake.txt",
                sampleBytes = byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())
            )
        )
        assertEquals(
            FileEntryType.Text,
            FileEntryTypeResolver.resolveVerified(
                name = "unknown",
                sampleBytes = "plain text".toByteArray()
            )
        )
        assertEquals(
            FileEntryType.Code,
            FileEntryTypeResolver.resolveVerified(
                name = "build.gradle.kts",
                sampleBytes = "plugins { kotlin(\"android\") }".toByteArray()
            )
        )
    }

    @Test
    fun contentPolicyVerifiesSamplesBeforeTextAccess() {
        assertFalse(
            FileContentAccessPolicy.canTreatSampleAsText(
                declaredType = FileEntryType.Text,
                displayName = "fake.txt",
                sampleBytes = byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())
            )
        )
        assertTrue(
            FileContentAccessPolicy.canTreatSampleAsText(
                declaredType = FileEntryType.Unknown,
                displayName = "unknown",
                sampleBytes = "plain text".toByteArray()
            )
        )
        assertTrue(
            FileContentAccessPolicy.canTreatSampleAsText(
                declaredType = FileEntryType.Text,
                displayName = "utf16.txt",
                sampleBytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "plain text".toByteArray(Charsets.UTF_16LE)
            )
        )
        assertFalse(
            FileContentAccessPolicy.canTreatSampleAsText(
                declaredType = FileEntryType.Image,
                displayName = "image.png",
                sampleBytes = "not actually image".toByteArray()
            )
        )
    }

    @Test
    fun editableTypesOnlyIncludeTextLikeTypes() {
        assertTrue(FileEntryType.Text in FileContentAccessPolicy.inlineTextTypes)
        assertTrue(FileEntryType.Markdown in FileContentAccessPolicy.inlineTextTypes)
        assertTrue(FileEntryType.Code in FileContentAccessPolicy.inlineTextTypes)
        assertFalse(FileEntryType.Image in FileContentAccessPolicy.inlineTextTypes)
        assertFalse(FileEntryType.Binary in FileContentAccessPolicy.inlineTextTypes)
    }
}
