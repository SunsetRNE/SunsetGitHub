package com.Sunset.REN.GitHub.data.github.html

class GitHubHtmlDocument(private val html: String) {

    val title: String?
        get() = firstTagText("title")?.substringBefore("· GitHub")?.trimToNull()

    val plainText: String by lazy {
        html
            .replace(ScriptRegex, " ")
            .replace(StyleRegex, " ")
            .replace(TagRegex, " ")
            .decodeHtmlEntities()
            .normalizeWhitespace()
    }

    val attributeText: String by lazy {
        AttributeTextRegex.findAll(html)
            .mapNotNull { match -> match.groupValues.getOrNull(2) }
            .joinToString(" ")
            .decodeHtmlEntities()
            .normalizeWhitespace()
    }

    val searchableText: String by lazy {
        listOf(plainText, attributeText)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .normalizeWhitespace()
    }

    fun containsAny(vararg needles: String, ignoreCase: Boolean = true): Boolean {
        return needles.any { needle -> html.contains(needle, ignoreCase) || searchableText.contains(needle, ignoreCase) }
    }

    fun firstMetaContent(propertyOrName: String): String? {
        val escaped = Regex.escape(propertyOrName)
        val regexes = listOf(
            Regex("""<meta[^>]+property=['\"]$escaped['\"][^>]+content=['\"]([^'\"]*)['\"][^>]*>""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+name=['\"]$escaped['\"][^>]+content=['\"]([^'\"]*)['\"][^>]*>""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+content=['\"]([^'\"]*)['\"][^>]+property=['\"]$escaped['\"][^>]*>""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+content=['\"]([^'\"]*)['\"][^>]+name=['\"]$escaped['\"][^>]*>""", RegexOption.IGNORE_CASE)
        )
        return regexes.firstNotNullOfOrNull { regex ->
            regex.find(html)?.groupValues?.getOrNull(1)?.decodeHtmlEntities()?.trimToNull()
        }
    }

    fun firstTagText(tagName: String): String? {
        val regex = Regex(
            """<$tagName\b[^>]*>(.*?)</$tagName>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        return regex.find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(TagRegex, " ")
            ?.decodeHtmlEntities()
            ?.normalizeWhitespace()
            ?.trimToNull()
    }

    fun textsNear(keyword: String, radius: Int = 160): List<String> {
        if (keyword.isBlank()) return emptyList()
        val source = searchableText
        val results = mutableListOf<String>()
        var startIndex = 0
        while (true) {
            val index = source.indexOf(keyword, startIndex, ignoreCase = true)
            if (index < 0) break
            val from = (index - radius).coerceAtLeast(0)
            val to = (index + keyword.length + radius).coerceAtMost(source.length)
            results += source.substring(from, to).normalizeWhitespace()
            startIndex = index + keyword.length
        }
        return results.distinct()
    }

    fun isGitHubTransientErrorPage(): Boolean {
        val normalizedTitle = title.orEmpty().lowercase()
        if (normalizedTitle in TransientErrorTitles) return true
        return containsAny(
            "This page is taking too long to load",
            "This page is unavailable",
            "Server Error",
            "The server is temporarily unable to service your request",
            "timeout",
            ignoreCase = true
        ) && !containsAny("Repository name", "Danger Zone", "Projects", "Wiki", "Pulse", "Copilot")
    }

    companion object {
        private val ScriptRegex = Regex("""<script\b[^>]*>.*?</script>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val StyleRegex = Regex("""<style\b[^>]*>.*?</style>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val AttributeTextRegex = Regex("""\b(aria-label|title|alt|data-content|data-hovercard-type)=['\"]([^'\"]+)['\"]""", RegexOption.IGNORE_CASE)
        private val TagRegex = Regex("""<[^>]+>""")
        private val TransientErrorTitles = setOf(
            "timeout",
            "server error",
            "github · timeout",
            "github · server error"
        )
    }
}

fun String.decodeHtmlEntities(): String {
    return replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&" + "quot;", Char(34).toString())
        .replace("&#39;", "'")
        .replace("&" + "apos;", "'")
        .replace(NumericEntityRegex) { match ->
            val raw = match.groupValues[1]
            val codePoint = if (raw.startsWith("x", ignoreCase = true)) {
                raw.drop(1).toIntOrNull(16)
            } else {
                raw.toIntOrNull()
            }
            codePoint?.takeIf { it > 0 }?.let { String(Character.toChars(it)) } ?: match.value
        }
}

fun String.normalizeWhitespace(): String = replace(WhitespaceRegex, " ").trim()

fun String.trimToNull(): String? = trim().takeIf { it.isNotEmpty() }

private val WhitespaceRegex = Regex("\\s+")
private val NumericEntityRegex = Regex("&#(x?[0-9a-fA-F]+);")