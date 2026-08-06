package com.Sunset.REN.GitHub.domain.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceSensitiveFilePolicyTest {
    @Test
    fun inspectBlocksLocalConfigurationFiles() {
        val result = WorkspaceSensitiveFilePolicy.inspect("android/local.properties")

        assertEquals(SensitiveFileSeverity.Blocking, result?.severity)
        assertEquals("android/local.properties", result?.relativePath)
        assertTrue(result?.reason.orEmpty().contains("本地配置"))
    }

    @Test
    fun inspectBlocksEnvironmentAndTokenNames() {
        assertEquals(
            SensitiveFileSeverity.Blocking,
            WorkspaceSensitiveFilePolicy.inspect(".env.production")?.severity
        )
        assertEquals(
            SensitiveFileSeverity.Blocking,
            WorkspaceSensitiveFilePolicy.inspect("config/github_token.txt")?.severity
        )
    }

    @Test
    fun inspectBlocksSigningAndPrivateKeyExtensions() {
        assertEquals(
            SensitiveFileSeverity.Blocking,
            WorkspaceSensitiveFilePolicy.inspect("release/sunsetgithub.jks")?.severity
        )
        assertEquals(
            SensitiveFileSeverity.Blocking,
            WorkspaceSensitiveFilePolicy.inspect("certs/private.pem")?.severity
        )
    }

    @Test
    fun inspectWarnsForGeneratedDirectoriesAndArtifacts() {
        assertEquals(
            SensitiveFileSeverity.Warning,
            WorkspaceSensitiveFilePolicy.inspect("app/build/outputs/apk/debug/app-debug.apk")?.severity
        )
        assertEquals(
            SensitiveFileSeverity.Warning,
            WorkspaceSensitiveFilePolicy.inspect(".git/config")?.severity
        )
        assertEquals(
            SensitiveFileSeverity.Warning,
            WorkspaceSensitiveFilePolicy.inspect("logs/run.log")?.severity
        )
    }

    @Test
    fun inspectIgnoresNormalSourceFiles() {
        assertNull(WorkspaceSensitiveFilePolicy.inspect("app/src/main/kotlin/MainActivity.kt"))
        assertNull(WorkspaceSensitiveFilePolicy.inspect("README.md"))
    }

    @Test
    fun inspectNormalizesSeparatorsAndDotSegments() {
        val result = WorkspaceSensitiveFilePolicy.inspect("app\\..\\local.properties")

        assertEquals(SensitiveFileSeverity.Blocking, result?.severity)
        assertEquals("app/../local.properties".toNormalizedRepositoryPath(), result?.relativePath)
    }

    @Test
    fun inspectAllReturnsOnlySensitiveFiles() {
        val results = WorkspaceSensitiveFilePolicy.inspectAll(
            listOf(
                "README.md",
                "local.properties",
                "app/build/tmp/cache.bin",
                "src/Main.kt"
            )
        )

        assertEquals(2, results.size)
        assertEquals(
            listOf(SensitiveFileSeverity.Blocking, SensitiveFileSeverity.Warning),
            results.map { it.severity }
        )
    }
}
