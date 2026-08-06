package com.Sunset.REN.GitHub.data.filemanager.provider

import com.Sunset.REN.GitHub.domain.filemanager.path.FileManagerPath
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileListResult
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileReadResult
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileStatResult
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileSystemCapabilities
import com.Sunset.REN.GitHub.domain.filemanager.root.RootAccessManager
import com.Sunset.REN.GitHub.domain.filemanager.root.RootAccessState
import com.Sunset.REN.GitHub.domain.filemanager.root.RootCommandRunner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RootFileSystemProviderTest {
    @Test
    fun rootCapabilitiesExposeReadOnlyAfterGrant() = runBlocking {
        val provider = providerWithGrantedRoot(commandOutputs = emptyMap())

        assertEquals(FileSystemCapabilities.RootGranted, provider.capabilities)
        assertTrue(provider.capabilities.canList)
        assertTrue(provider.capabilities.canStat)
        assertTrue(provider.capabilities.canRead)
        assertEquals(false, provider.capabilities.canWrite)
        assertEquals(false, provider.capabilities.canDelete)
    }

    @Test
    fun listAndStatRequireGrantedRoot() = runBlocking {
        val provider = RootFileSystemProvider(RootAccessManager(FakeRootCommandRunner()))

        assertTrue(provider.list(FileManagerPath.Root("/data")) is FileListResult.Failed)
        assertTrue(provider.stat(FileManagerPath.Root("/data")) is FileStatResult.Failed)
        assertTrue(provider.read(FileManagerPath.Root("/data/config.txt")) is FileReadResult.Failed)
    }

    @Test
    fun readSmallRootFileUsesStatAndDd() = runBlocking {
        val provider = providerWithGrantedRoot(
            commandOutputs = mapOf(
                "ls '-ld' '--' '/system/build.prop'" to "-rw-r--r-- 1 root root 11 2026-01-01 00:00 /system/build.prop",
                "dd 'if=/system/build.prop' 'bs=1' 'count=11'" to "hello root!"
            )
        )

        val result = provider.read(FileManagerPath.Root("/system/build.prop"))

        assertTrue(result is FileReadResult.Success)
        assertArrayEquals("hello root!".toByteArray(), (result as FileReadResult.Success).bytes)
    }

    @Test
    fun readRejectsLargeRootFileBeforeDd() = runBlocking {
        val runner = FakeRootCommandRunner(
            commandOutputs = mapOf(
                "ls '-ld' '--' '/data/large.bin'" to "-rw-r--r-- 1 root root 65537 2026-01-01 00:00 /data/large.bin"
            )
        )
        val provider = providerWithGrantedRoot(runner = runner)

        val result = provider.read(FileManagerPath.Root("/data/large.bin"))

        assertTrue(result is FileReadResult.Failed)
        assertEquals(listOf("id", "ls '-ld' '--' '/data/large.bin'"), runner.commands)
    }

    @Test
    fun rootListingMarksRegularFilesAsReadOnlyContentAccessible() {
        val entries = RootDirectoryListingParser.parseDirectory(
            "/system",
            "-rw-r--r-- 1 root root 42 2026-01-01 00:00 build.prop"
        )

        val entry = entries.single()
        assertTrue(entry.capabilities.canRead)
        assertTrue(entry.capabilities.canAccessContent)
        assertEquals(false, entry.capabilities.canWrite)
        assertEquals(false, entry.capabilities.canEditAsText)
    }

    private suspend fun providerWithGrantedRoot(
        commandOutputs: Map<String, String> = emptyMap(),
        runner: FakeRootCommandRunner = FakeRootCommandRunner(commandOutputs)
    ): RootFileSystemProvider {
        val manager = RootAccessManager(runner)
        assertEquals(RootAccessState.Granted, manager.requestAccess())
        return RootFileSystemProvider(manager, runner)
    }

    private class FakeRootCommandRunner(
        private val commandOutputs: Map<String, String> = emptyMap(),
        private val failures: Map<String, String> = emptyMap()
    ) : RootCommandRunner() {
        val commands = mutableListOf<String>()

        override suspend fun hasSuBinary(): Boolean = true

        override suspend fun run(command: String, timeoutMillis: Long): Result<String> {
            commands += command
            failures[command]?.let { return Result.failure(IllegalStateException(it)) }
            return Result.success(commandOutputs[command] ?: if (command == "id") "uid=0(root) gid=0(root)" else "")
        }

        override fun runBlocking(command: String, timeoutMillis: Long): Result<String> {
            commands += command
            failures[command]?.let { return Result.failure(IllegalStateException(it)) }
            return Result.success(commandOutputs[command] ?: if (command == "id") "uid=0(root) gid=0(root)" else "")
        }
    }
}
