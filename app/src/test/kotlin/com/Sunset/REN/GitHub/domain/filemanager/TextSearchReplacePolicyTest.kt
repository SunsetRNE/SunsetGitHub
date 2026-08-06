package com.Sunset.REN.GitHub.domain.filemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextSearchReplacePolicyTest {
    @Test
    fun findNextSearchesFromSelectionAndWrapsToStart() {
        assertEquals(
            TextSearchReplacePolicy.Match(start = 8, end = 11),
            TextSearchReplacePolicy.findNext("one two one", "one", fromIndex = 3)
        )
        assertEquals(
            TextSearchReplacePolicy.Match(start = 0, end = 3),
            TextSearchReplacePolicy.findNext("one two one", "one", fromIndex = 11)
        )
    }

    @Test
    fun findNextSupportsCaseInsensitiveMatching() {
        assertEquals(
            TextSearchReplacePolicy.Match(start = 0, end = 3),
            TextSearchReplacePolicy.findNext("One two", "one", fromIndex = 0, ignoreCase = true)
        )
        assertNull(TextSearchReplacePolicy.findNext("One two", "one", fromIndex = 0, ignoreCase = false))
    }

    @Test
    fun findNextReturnsNullForEmptyOrMissingQuery() {
        assertNull(TextSearchReplacePolicy.findNext("content", "", fromIndex = 0))
        assertNull(TextSearchReplacePolicy.findNext("content", "missing", fromIndex = 0))
    }

    @Test
    fun countMatchesDoesNotCountOverlappingMatches() {
        assertEquals(2, TextSearchReplacePolicy.countMatches("aaaa", "aa"))
        assertEquals(0, TextSearchReplacePolicy.countMatches("aaaa", ""))
    }

    @Test
    fun countMatchesSupportsCaseInsensitiveMatching() {
        assertEquals(3, TextSearchReplacePolicy.countMatches("One one ONE", "one", ignoreCase = true))
        assertEquals(1, TextSearchReplacePolicy.countMatches("One one ONE", "one", ignoreCase = false))
    }

    @Test
    fun replaceAllReplacesEveryNonOverlappingMatch() {
        val result = TextSearchReplacePolicy.replaceAll("one two one", "one", "1")

        assertEquals("1 two 1", result.content)
        assertEquals(2, result.count)
    }

    @Test
    fun replaceAllSupportsCaseInsensitiveMatching() {
        val result = TextSearchReplacePolicy.replaceAll("One one ONE", "one", "1", ignoreCase = true)

        assertEquals("1 1 1", result.content)
        assertEquals(3, result.count)
    }

    @Test
    fun replaceAllNoMatchLeavesContentUnchanged() {
        val result = TextSearchReplacePolicy.replaceAll("content", "missing", "x")

        assertEquals("content", result.content)
        assertEquals(0, result.count)
    }

    @Test
    fun regexFindNextCountAndReplaceAllAreSupported() {
        assertEquals(
            TextSearchReplacePolicy.Match(start = 4, end = 7),
            TextSearchReplacePolicy.findNext("abc a12", "a\\d+", fromIndex = 1, isRegex = true)
        )
        assertEquals(2, TextSearchReplacePolicy.countMatches("a1 a22 b", "a\\d+", isRegex = true))

        val result = TextSearchReplacePolicy.replaceAll("a1 a22 b", "a(\\d+)", "n-$1", isRegex = true)

        assertEquals("n-1 n-22 b", result.content)
        assertEquals(2, result.count)
    }

    @Test
    fun regexMatchingSupportsCaseInsensitiveOption() {
        assertEquals(2, TextSearchReplacePolicy.countMatches("A1 a2", "a\\d", ignoreCase = true, isRegex = true))
        assertEquals(1, TextSearchReplacePolicy.countMatches("A1 a2", "a\\d", ignoreCase = false, isRegex = true))
    }

    @Test
    fun invalidRegexIsReportedWithoutChangingContent() {
        assertEquals(false, TextSearchReplacePolicy.isValidRegex("["))

        val result = TextSearchReplacePolicy.replaceAll("content", "[", "x", isRegex = true)

        assertEquals("content", result.content)
        assertEquals(0, result.count)
        assertEquals(true, result.invalidPattern)
    }

    @Test
    fun invalidRegexReplacementIsReportedWithoutChangingContent() {
        val result = TextSearchReplacePolicy.replaceAll("a1", "a(\\d)", "$2", isRegex = true)

        assertEquals("a1", result.content)
        assertEquals(0, result.count)
        assertEquals(true, result.invalidReplacement)
    }
}