package com.Sunset.REN.GitHub.domain.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceSyncExecutionStrategyTest {
    @Test
    fun resolveUsesCurrentHeadWhenRemoteDidNotChange() {
        val base = WorkspaceSyncExecutionStrategy.resolve(
            planBaseRevision = "abc",
            currentHeadSha = "abc",
            allowOverwriteRemoteChanges = false
        )

        assertEquals("abc", base.treeBaseCommitSha)
        assertEquals("abc", base.commitParentSha)
        assertFalse(base.forceUpdate)
        assertFalse(base.remoteChangedSincePlan)
        assertFalse(base.overwritingRemoteChanges)
    }

    @Test
    fun resolveBlocksChangedRemoteByDefault() {
        val result = runCatching {
            WorkspaceSyncExecutionStrategy.resolve(
                planBaseRevision = "old",
                currentHeadSha = "new",
                allowOverwriteRemoteChanges = false
            )
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("远端分支已变化"))
    }

    @Test
    fun resolveAllowsChangedRemoteWhenOverwriteIsExplicit() {
        val base = WorkspaceSyncExecutionStrategy.resolve(
            planBaseRevision = "old",
            currentHeadSha = "new",
            allowOverwriteRemoteChanges = true
        )

        assertEquals("new", base.treeBaseCommitSha)
        assertEquals("new", base.commitParentSha)
        assertFalse(base.forceUpdate)
        assertTrue(base.remoteChangedSincePlan)
        assertTrue(base.overwritingRemoteChanges)
    }

    @Test
    fun resolveTreatsMissingPlanBaseAsCurrentHeadSync() {
        val base = WorkspaceSyncExecutionStrategy.resolve(
            planBaseRevision = null,
            currentHeadSha = "head",
            allowOverwriteRemoteChanges = false
        )

        assertEquals("head", base.treeBaseCommitSha)
        assertFalse(base.remoteChangedSincePlan)
        assertFalse(base.overwritingRemoteChanges)
    }
}