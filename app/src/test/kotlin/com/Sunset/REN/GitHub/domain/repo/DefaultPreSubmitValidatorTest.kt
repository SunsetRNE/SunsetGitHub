package com.Sunset.REN.GitHub.domain.repo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultPreSubmitValidatorTest {
    private val validator = DefaultPreSubmitValidator()

    @Test
    fun validate_allowsUploadWithinRecommendedLimit() {
        val result = validator.validate(
            uploadSession(sizeBytes = GitHubContentApiLimits.RecommendedDirectUploadMaxBytes)
        )

        assertTrue(result.canSubmit)
    }

    @Test
    fun validate_blocksUploadAboveRecommendedLimit() {
        val result = validator.validate(
            uploadSession(sizeBytes = GitHubContentApiLimits.RecommendedDirectUploadMaxBytes + 1L)
        )

        assertFalse(result.canSubmit)
        assertTrue(result.errors.any { it.contains("50 MiB") })
    }

    @Test
    fun validate_blocksFileAboveKnownContentsApiLimit() {
        val result = validator.validate(
            uploadSession(sizeBytes = GitHubContentApiLimits.KnownSingleContentMaxBytes + 1L)
        )

        assertFalse(result.canSubmit)
        assertTrue(result.errors.any { it.contains("GitHub Contents API") })
    }

    @Test
    fun validate_rejectsUnsafeTargetPathSegments() {
        val result = validator.validate(
            uploadSession(targetPath = "docs/../README.md")
        )

        assertFalse(result.canSubmit)
        assertTrue(result.errors.any { it.contains("路径片段") })
    }

    @Test
    fun validate_requiresUploadCapability() {
        val result = validator.validate(
            uploadSession(capability = FileCapability(reason = "no upload"))
        )

        assertFalse(result.canSubmit)
        assertTrue(result.errors.contains("no upload"))
    }

    private fun uploadSession(
        targetPath: String = "docs/README.md",
        sizeBytes: Long = 1024L,
        capability: FileCapability = FileCapability.UploadOnly
    ): FileWriteSession {
        return FileWriteSession(
            repositoryId = "octocat/Hello-World",
            owner = "octocat",
            repo = "Hello-World",
            targetPath = targetPath,
            operation = FileWriteOperation.Upload,
            commitMessage = "Upload README.md",
            selectedFiles = listOf(
                SelectedRepositoryWriteFile(
                    displayName = "README.md",
                    uri = "file:///tmp/README.md",
                    sizeBytes = sizeBytes
                )
            ),
            capability = capability
        )
    }
}
