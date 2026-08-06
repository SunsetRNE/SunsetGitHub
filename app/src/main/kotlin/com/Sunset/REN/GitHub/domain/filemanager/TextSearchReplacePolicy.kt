package com.Sunset.REN.GitHub.domain.filemanager

object TextSearchReplacePolicy {
    data class Match(
        val start: Int,
        val end: Int
    )

    data class ReplaceAllResult(
        val content: String,
        val count: Int,
        val invalidPattern: Boolean = false,
        val invalidReplacement: Boolean = false
    )

    fun isValidRegex(query: String): Boolean {
        if (query.isEmpty()) return false
        return buildRegex(query, ignoreCase = false) != null
    }

    fun findNext(
        content: String,
        query: String,
        fromIndex: Int,
        ignoreCase: Boolean = false,
        isRegex: Boolean = false
    ): Match? {
        if (query.isEmpty()) return null
        if (isRegex) return findNextRegex(content, query, fromIndex, ignoreCase)
        val safeFromIndex = fromIndex.coerceIn(0, content.length)
        val first = content.indexOf(query, safeFromIndex, ignoreCase = ignoreCase)
        val index = if (first >= 0) first else content.indexOf(query, 0, ignoreCase = ignoreCase)
        return index.takeIf { it >= 0 }?.let { Match(start = it, end = it + query.length) }
    }

    fun countMatches(
        content: String,
        query: String,
        ignoreCase: Boolean = false,
        isRegex: Boolean = false
    ): Int {
        return findAll(content, query, ignoreCase, isRegex).size
    }

    fun findAll(
        content: String,
        query: String,
        ignoreCase: Boolean = false,
        isRegex: Boolean = false
    ): List<Match> {
        if (query.isEmpty()) return emptyList()
        if (isRegex) {
            val regex = buildRegex(query, ignoreCase) ?: return emptyList()
            return regex.findAll(content).map { match ->
                Match(start = match.range.first, end = match.range.last + 1)
            }.toList()
        }
        val matches = mutableListOf<Match>()
        var index = 0
        while (index <= content.length) {
            val found = content.indexOf(query, index, ignoreCase = ignoreCase)
            if (found < 0) break
            matches += Match(start = found, end = found + query.length)
            index = found + query.length.coerceAtLeast(1)
        }
        return matches
    }

    fun replaceAll(
        content: String,
        query: String,
        replacement: String,
        ignoreCase: Boolean = false,
        isRegex: Boolean = false
    ): ReplaceAllResult {
        if (query.isEmpty()) return ReplaceAllResult(content = content, count = 0)
        if (isRegex) {
            val regex = buildRegex(query, ignoreCase)
                ?: return ReplaceAllResult(content = content, count = 0, invalidPattern = true)
            val count = regex.findAll(content).count()
            if (count == 0) return ReplaceAllResult(content = content, count = 0)
            val updated = runCatching { regex.replace(content, replacement) }.getOrNull()
                ?: return ReplaceAllResult(content = content, count = 0, invalidReplacement = true)
            return ReplaceAllResult(content = updated, count = count)
        }
        val builder = StringBuilder(content.length)
        var count = 0
        var index = 0
        while (index < content.length) {
            val found = content.indexOf(query, index, ignoreCase = ignoreCase)
            if (found < 0) {
                builder.append(content, index, content.length)
                break
            }
            builder.append(content, index, found)
            builder.append(replacement)
            count += 1
            index = found + query.length
        }
        if (index == content.length) {
            // No-op. The loop has consumed the full content exactly at the last match boundary.
        }
        return ReplaceAllResult(content = builder.toString(), count = count)
    }

    fun replaceMatchedText(
        matchedText: String,
        query: String,
        replacement: String,
        ignoreCase: Boolean = false,
        isRegex: Boolean = false
    ): ReplaceAllResult {
        if (!isRegex) return ReplaceAllResult(content = replacement, count = 1)
        val regex = buildRegex(query, ignoreCase)
            ?: return ReplaceAllResult(content = matchedText, count = 0, invalidPattern = true)
        val updated = runCatching { regex.replace(matchedText, replacement) }.getOrNull()
            ?: return ReplaceAllResult(content = matchedText, count = 0, invalidReplacement = true)
        return ReplaceAllResult(content = updated, count = 1)
    }

    private fun findNextRegex(content: String, query: String, fromIndex: Int, ignoreCase: Boolean): Match? {
        val regex = buildRegex(query, ignoreCase) ?: return null
        val safeFromIndex = fromIndex.coerceIn(0, content.length)
        val first = regex.find(content, safeFromIndex)
        val match = first ?: regex.find(content, 0)
        return match?.let { Match(start = it.range.first, end = it.range.last + 1) }
    }

    private fun buildRegex(query: String, ignoreCase: Boolean): Regex? {
        val options = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
        return runCatching { Regex(query, options) }.getOrNull()
    }
}