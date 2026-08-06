package com.Sunset.REN.GitHub.domain.filemanager.root

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootSafetyPolicyTest {
    @Test
    fun quoteArgEscapesSingleQuotesAsOneShellArgument() {
        val runner = RootCommandRunner()

        assertEquals("'simple'", runner.quoteArg("simple"))
        assertEquals("'a'\"'\"'b'", runner.quoteArg("a'b"))
        assertEquals("ls '-la' '--' '/data/user/0/a'\"'\"'b'", runner.buildCommand("ls", "-la", "--", "/data/user/0/a'b"))
    }

    @Test
    fun rootRiskLabelsSeparateSystemPathsFromOrdinaryRootPaths() {
        assertEquals("极高风险", RootPathPolicy.riskLabel("/"))
        assertEquals("高风险", RootPathPolicy.riskLabel("/system/bin"))
        assertEquals("高风险", RootPathPolicy.riskLabel("/data/data"))
        assertEquals("普通 Root 路径", RootPathPolicy.riskLabel("/mnt/vendor_rw"))
    }

    @Test
    fun rootWriteRiskPolicyMarksProtectedPrefixes() {
        assertTrue(RootPathPolicy.isRootOnlyPath("/proc/self"))
        assertTrue(RootPathPolicy.isHighRiskWritePath("/vendor/etc"))
        assertFalse(RootPathPolicy.isRootOnlyPath("/sdcard/Download"))
    }
}
