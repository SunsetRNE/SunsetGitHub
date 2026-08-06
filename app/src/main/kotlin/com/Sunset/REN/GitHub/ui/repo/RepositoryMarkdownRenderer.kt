package com.Sunset.REN.GitHub.ui.repo

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.util.Log
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.signature.ObjectKey
import com.bumptech.glide.request.target.Target
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.AsyncDrawableScheduler
import io.noties.markwon.image.ImageSizeResolverDef
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin

fun TextView.setRepositoryMarkdown(
    markdown: String,
    baseHtmlUrl: String? = null,
    imageAccessToken: String = "",
    badgeMetadata: RepositoryMarkdownBadgeMetadata? = null,
    onLinkClick: ((String) -> Boolean)? = null
) {
    setTextColor(GitHubTextPrimaryColor)
    setLinkTextColor(GitHubAccentColor)
    RepositoryMarkdownRenderer.imageAccessToken = imageAccessToken
    RepositoryMarkdownRenderer.badgeMetadata = badgeMetadata
    RepositoryMarkdownRenderer.get(this).setMarkdown(this, markdown.toRepositoryCompatibleMarkdown(baseHtmlUrl))
    movementMethod = if (onLinkClick != null) {
        RepositoryMarkdownLinkMovementMethod(onLinkClick)
    } else {
        android.text.method.LinkMovementMethod.getInstance()
    }
}

data class RepositoryMarkdownBadgeMetadata(
    val repositoryFullName: String,
    val stargazersCount: Int,
    val forksCount: Int,
    val openIssuesCount: Int
)

private fun String.toRepositoryCompatibleMarkdown(baseHtmlUrl: String?): String {
    val linkBase = RepositoryMarkdownLinkBase.from(baseHtmlUrl)
    val result = StringBuilder()
    val pendingText = StringBuilder()
    var isInFencedCodeBlock = false

    fun flushPendingText() {
        if (pendingText.isEmpty()) return
        result.append(pendingText.toString().toRepositoryCompatibleMarkdownSegment(linkBase))
        pendingText.clear()
    }

    lineSequence().forEach { line ->
        val isFenceLine = line.trimStart().isMarkdownFenceLine()
        if (isFenceLine) {
            flushPendingText()
            result.append(line).append('\n')
            isInFencedCodeBlock = !isInFencedCodeBlock
        } else if (isInFencedCodeBlock) {
            result.append(line).append('\n')
        } else {
            pendingText.append(line).append('\n')
        }
    }
    flushPendingText()
    return result.toString().trimEnd('\n')
}

private fun String.isMarkdownFenceLine(): Boolean {
    return startsWith("```") || startsWith("~~~")
}

private fun String.toRepositoryCompatibleMarkdownSegment(linkBase: RepositoryMarkdownLinkBase?): String {
    return normalizeHtmlComments()
        .normalizeHtmlDetails()
        .normalizeZeroSizeHtmlImageContainers()
        .normalizeLinkedMarkdownImages(linkBase)
        .normalizeHtmlImageAnchors(linkBase)
        .normalizeHtmlAnchorTags(linkBase)
        .normalizeInlineHtmlFormatting()
        .replace(WhitespaceOnlyLinesPattern, "")
        .replace(ExcessiveBlankLinesPattern, "\n\n")
        .lineSequence()
        .joinToString(separator = "\n") { line -> line.toRepositoryCompatibleMarkdownLine(linkBase) }
}

private fun String.toRepositoryCompatibleMarkdownLine(linkBase: RepositoryMarkdownLinkBase?): String {
    return this
        .replace(HtmlImageWrapperPattern, "")
        .replace(HtmlBreakPattern, "  ")
        .replace(HtmlHorizontalRulePattern, "---")
        .replace(NonBreakingSpacePattern, " ")
        .normalizeGitHubAlert()
        .normalizeMarkdownImages(linkBase)
        .normalizeHtmlImages(linkBase)
        .normalizeMarkdownLinks(linkBase)
        .normalizeHtmlLinks(linkBase)
        .normalizeGitHubReferences(linkBase)
}

private fun String.normalizeGitHubAlert(): String {
    val match = GitHubAlertPattern.matchEntire(this) ?: return this
    val marker = when (match.groupValues[2].uppercase()) {
        "NOTE" -> "ℹ️ 备注"
        "TIP" -> "💡 提示"
        "IMPORTANT" -> "❗ 重要"
        "WARNING" -> "⚠️ 警告"
        "CAUTION" -> "🚨 注意"
        else -> match.groupValues[2]
    }
    return "${match.groupValues[1]}> **$marker**"
}

private fun String.normalizeHtmlComments(): String {
    return replace(HtmlCommentPattern, "")
}

private fun String.normalizeHtmlDetails(): String {
    return replace(HtmlDetailsPattern, "")
        .replace(HtmlSummaryPattern) { match ->
            val content = match.groupValues[1].trim().decodeCommonHtmlEntities()
            "**▾ $content**\n\n"
        }
}

private fun String.normalizeZeroSizeHtmlImageContainers(): String {
    return replace(ZeroSizeHtmlImageContainerPattern, "")
        .replace(ZeroSizeHtmlImagePattern, "")
}

private fun String.normalizeHtmlAnchorTags(base: RepositoryMarkdownLinkBase?): String {
    return HtmlAnchorPattern.replace(this) { match ->
        val tag = match.groupValues[1]
        val href = tag.findHtmlAttribute("href") ?: return@replace match.value
        val label = match.groupValues[2].decodeCommonHtmlEntities().trim()
        val normalized = base?.resolve(href, raw = false) ?: href
        if (label.isBlank()) normalized else "[$label]($normalized)"
    }
}

private fun String.normalizeHtmlImageAnchors(base: RepositoryMarkdownLinkBase?): String {
    return HtmlImageAnchorPattern.replace(this) { match ->
        val anchorTag = match.groupValues[1]
        val href = anchorTag.findHtmlAttribute("href") ?: return@replace match.value
        val imageTag = match.groupValues[2]
        val src = imageTag.findHtmlAttribute("src") ?: return@replace match.value
        val alt = imageTag.findHtmlAttribute("alt").orEmpty().decodeCommonHtmlEntities()
        val normalizedHref = base?.resolve(href, raw = false) ?: href
        val normalizedSrc = (base?.resolve(src, raw = true) ?: src).normalizeRemoteBadgeImageUrl()
        "[![$alt]($normalizedSrc)]($normalizedHref)"
    }
}

private fun String.normalizeInlineHtmlFormatting(): String {
    return this
        .replace(HtmlKeyboardPattern) { match -> "`${match.groupValues[1].trim().decodeCommonHtmlEntities()}`" }
        .replace(HtmlCodePattern) { match -> "`${match.groupValues[1].trim().decodeCommonHtmlEntities()}`" }
        .replace(HtmlMarkPattern) { match -> "**${match.groupValues[1].trim().decodeCommonHtmlEntities()}**" }
        .replace(HtmlStrongPattern) { match -> "**${match.groupValues[1].trim().decodeCommonHtmlEntities()}**" }
        .replace(HtmlEmphasisPattern) { match -> "*${match.groupValues[1].trim().decodeCommonHtmlEntities()}*" }
        .replace(HtmlDeletePattern) { match -> "~~${match.groupValues[1].trim().decodeCommonHtmlEntities()}~~" }
        .replace(HtmlUnderlinePattern) { match -> match.groupValues[1].trim().decodeCommonHtmlEntities() }
        .replace(HtmlSmallPattern) { match -> match.groupValues[1].trim().decodeCommonHtmlEntities() }
        .replace(HtmlSupPattern) { match -> match.groupValues[1].trim().decodeCommonHtmlEntities().toSuperscriptText() }
        .replace(HtmlSubPattern) { match -> match.groupValues[1].trim().decodeCommonHtmlEntities().toSubscriptText() }
}

private fun String.decodeCommonHtmlEntities(): String {
    return replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&" + "quot;", 34.toChar().toString())
        .replace("&#39;", "'")
        .replace("&" + "apos;", "'")
}

private fun String.toSuperscriptText(): String = map { SuperscriptCharacters[it] ?: it }.joinToString("")

private fun String.toSubscriptText(): String = map { SubscriptCharacters[it] ?: it }.joinToString("")

private fun String.normalizeLinkedMarkdownImages(base: RepositoryMarkdownLinkBase?): String {
    return LinkedMarkdownImagePattern.replace(this) { match ->
        val label = match.groupValues[1]
        val imageDestination = match.groupValues[2].toMarkdownDestination()
        val linkDestination = match.groupValues[3].toMarkdownDestination()
        val normalizedImage = (base?.resolve(imageDestination.target, raw = true) ?: imageDestination.target)
            .normalizeRemoteBadgeImageUrl()
        val normalizedLink = base?.resolve(linkDestination.target, raw = false) ?: linkDestination.target
        "[![$label]($normalizedImage${imageDestination.titleSuffix})]($normalizedLink${linkDestination.titleSuffix})"
    }
}

private fun String.normalizeMarkdownImages(base: RepositoryMarkdownLinkBase?): String {
    return MarkdownImagePattern.replace(this) { match ->
        val label = match.groupValues[1]
        val destination = match.groupValues[2].toMarkdownDestination()
        val normalized = (base?.resolve(destination.target, raw = true) ?: destination.target)
            .normalizeRemoteBadgeImageUrl()
        "![$label]($normalized${destination.titleSuffix})"
    }
}

private fun String.normalizeMarkdownLinks(base: RepositoryMarkdownLinkBase?): String {
    if (base == null) return this
    return MarkdownLinkPattern.replace(this) { match ->
        val prefix = match.groupValues[1]
        val label = match.groupValues[2]
        val destination = match.groupValues[3].toMarkdownDestination()
        val normalized = base.resolve(destination.target, raw = prefix == "!")
        "$prefix[$label]($normalized${destination.titleSuffix})"
    }
}

private fun String.normalizeGitHubReferences(base: RepositoryMarkdownLinkBase?): String {
    if (base == null) return this
    return GitHubReferencePattern.replace(this) { match ->
        val prefix = match.groupValues[1]
        val reference = match.groupValues[2]
        val normalized = base.resolveReference(reference)
        "$prefix[$reference]($normalized)"
    }
}

private fun String.normalizeHtmlImages(base: RepositoryMarkdownLinkBase?): String {
    return HtmlImagePattern.replace(this) { match ->
        val tag = match.value
        val src = tag.findHtmlAttribute("src") ?: return@replace tag
        val label = tag.findHtmlAttribute("alt").orEmpty()
        val normalized = (base?.resolve(src, raw = true) ?: src)
            .normalizeRemoteBadgeImageUrl()
            .appendMarkdownImageWidthHint(tag.findHtmlImageWidth())
        "![$label]($normalized)"
    }
}

private fun String.findHtmlImageWidth(): Int? {
    return findHtmlAttribute("width")
        ?.substringBefore('.')
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
}

private fun String.appendMarkdownImageWidthHint(width: Int?): String {
    val resolvedWidth = width?.takeIf { it > 0 } ?: return this
    val separator = if (contains('#')) "&" else "#"
    return this + separator + MarkdownImageWidthHintPrefix + resolvedWidth
}

private fun String.findHtmlAttribute(name: String): String? {
    val pattern = Regex("(?i)\\b$name=(['\"])([^'\"]*)\\1")
    return pattern.find(this)?.groupValues?.getOrNull(2)
}

// Remote badges are rendered through Glide's SVG pipeline.

private fun String.normalizeRemoteBadgeImageUrl(): String {
    val lower = lowercase()
    if (!lower.isRemoteBadgeImageUrl()) return this
    val suffixStart = listOf(indexOf('?'), indexOf('#')).filter { it >= 0 }.minOrNull() ?: length
    val path = substring(0, suffixStart)
    val suffix = substring(suffixStart)
    if (lower.startsWith("https://img.shields.io/") || lower.startsWith("http://img.shields.io/")) {
        val normalizedPath = when {
            path.endsWith(".png", ignoreCase = true) -> path
            path.endsWith(".svg", ignoreCase = true) -> path.dropLast(4) + ".png"
            path.substringAfterLast('/').contains('.') -> path
            else -> "$path.png"
        }
        return normalizedPath + suffix
    }
    if (path.substringAfterLast('/').contains('.')) return this
    return path + ".svg" + suffix
}

private fun String.isRemoteBadgeImageUrl(): Boolean {
    val lower = lowercase()
    return lower.startsWith("https://img.shields.io/") ||
        lower.startsWith("http://img.shields.io/") ||
        lower.startsWith("https://badges.crowdin.net/") ||
        lower.isRemoteSvgImageUrl()
}

private fun String.isRemoteSvgImageUrl(): Boolean {
    val lower = lowercase()
    val suffixStart = listOf(lower.indexOf('?'), lower.indexOf('#')).filter { it >= 0 }.minOrNull() ?: lower.length
    return (lower.startsWith("https://") || lower.startsWith("http://")) &&
        lower.substring(0, suffixStart).endsWith(".svg")
}

private fun String.toMarkdownDestination(): MarkdownDestination {
    val trimmed = trim()
    if (trimmed.isBlank()) return MarkdownDestination(target = this, titleSuffix = "")
    val angleWrapped = trimmed.startsWith("<") && trimmed.indexOf('>') > 0
    if (angleWrapped) {
        val closeIndex = trimmed.indexOf('>')
        val target = trimmed.substring(1, closeIndex)
        val suffix = trimmed.substring(closeIndex + 1).takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
        return MarkdownDestination(target = target, titleSuffix = suffix)
    }
    val titleStart = MarkdownTitleStartPattern.find(trimmed)?.range?.first
    if (titleStart == null) return MarkdownDestination(target = trimmed, titleSuffix = "")
    val target = trimmed.substring(0, titleStart).trimEnd()
    val titleSuffix = trimmed.substring(titleStart)
    return MarkdownDestination(target = target, titleSuffix = titleSuffix)
}

private data class MarkdownDestination(
    val target: String,
    val titleSuffix: String
)

private fun String.normalizeHtmlLinks(base: RepositoryMarkdownLinkBase?): String {
    if (base == null) return this
    return HtmlLinkPattern.replace(this) { match ->
        val attr = match.groupValues[1]
        val quote = match.groupValues[2]
        val target = match.groupValues[3]
        val normalized = base.resolve(target, raw = attr.equals("src", ignoreCase = true))
        "$attr=$quote$normalized$quote"
    }
}

private data class RepositoryMarkdownLinkBase(
    val blobRootUrl: String,
    val blobDirectorySegments: List<String>,
    val blobFileUrl: String,
    val rawRootUrl: String,
    val rawDirectorySegments: List<String>,
    val repositoryUrl: String
) {
    fun resolveReference(reference: String): String {
        val issueNumber = reference.removePrefix("#").removePrefix("GH-").removePrefix("gh-")
        return "$repositoryUrl/issues/$issueNumber"
    }

    fun resolve(target: String, raw: Boolean): String {
        val trimmed = target.trim()
        if (trimmed.isBlank()) return target
        if (trimmed.startsWith("#")) return blobFileUrl + trimmed
        val lower = trimmed.lowercase()
        if (
            lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("mailto:") ||
            lower.startsWith("tel:") ||
            lower.startsWith("data:")
        ) return target
        val rootUrl = if (raw) rawRootUrl else blobRootUrl
        val baseSegments = if (raw) rawDirectorySegments else blobDirectorySegments
        return rootUrl + "/" + normalizeRelativePath(trimmed, baseSegments).replace(" ", "%20")
    }

    private fun normalizeRelativePath(target: String, baseSegments: List<String>): String {
        val fragmentIndex = target.indexOf('#').takeIf { it >= 0 } ?: target.length
        val queryIndex = target.indexOf('?').takeIf { it >= 0 } ?: target.length
        val suffixStart = minOf(fragmentIndex, queryIndex)
        val pathPart = target.substring(0, suffixStart)
        val suffix = target.substring(suffixStart)
        val segments = if (pathPart.startsWith("/")) mutableListOf() else baseSegments.toMutableList()
        pathPart.split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
                else -> segments.add(segment)
            }
        }
        return segments.joinToString(separator = "/") + suffix
    }

    companion object {
        fun from(htmlUrl: String?): RepositoryMarkdownLinkBase? {
            if (htmlUrl.isNullOrBlank()) return null
            val marker = "/blob/"
            val markerIndex = htmlUrl.indexOf(marker)
            if (markerIndex <= 0) return null
            val githubPrefix = htmlUrl.substring(0, markerIndex)
            val rest = htmlUrl.substring(markerIndex + marker.length)
            val slashIndex = rest.indexOf('/')
            if (slashIndex <= 0) return null
            val branch = rest.substring(0, slashIndex)
            val filePath = rest.substring(slashIndex + 1)
            val directoryPath = filePath.substringBeforeLast('/', "")
            val directorySegments = directoryPath.split('/').filter { it.isNotBlank() }
            val rawPrefix = githubPrefix
                .removePrefix("https://github.com/")
                .let { repoPath -> "https://raw.githubusercontent.com/$repoPath/$branch" }
            return RepositoryMarkdownLinkBase(
                blobRootUrl = "$githubPrefix/blob/$branch",
                blobDirectorySegments = directorySegments,
                blobFileUrl = htmlUrl,
                rawRootUrl = rawPrefix,
                rawDirectorySegments = directorySegments,
                repositoryUrl = githubPrefix
            )
        }
    }
}

private val LinkedMarkdownImagePattern = Regex("\\[!\\[([^\\]]*)]\\(([^)]+)\\)]\\(([^)]+)\\)")
private val WhitespaceOnlyLinesPattern = Regex("(?m)^[ \\t]+$")
private val ExcessiveBlankLinesPattern = Regex("\\n{3,}")
private val MarkdownImagePattern = Regex("!\\[([^\\]]*)]\\(([^)]+)\\)")
private val MarkdownLinkPattern = Regex("(!?)\\[([^\\]]+)]\\(([^)]+)\\)")
private val MarkdownTitleStartPattern = Regex("\\s+['\"]")
private val GitHubReferencePattern = Regex("(^|[\\s(])((?:#\\d+)|(?:GH-\\d+))", RegexOption.IGNORE_CASE)
private val HtmlImageWrapperPattern = Regex("</?(?:p|div|a|center|span|picture)\\b[^>]*>|<source\\b[^>]*>", RegexOption.IGNORE_CASE)
private val ZeroSizeHtmlImageContainerPattern = Regex("<div\\b[^>]*>\\s*<img(?=[^>]*\\bwidth=(['\"]?)0\\1)(?=[^>]*\\bheight=(['\"]?)0\\2)[^>]*?/?>\\s*</div>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val ZeroSizeHtmlImagePattern = Regex("<img(?=[^>]*\\bwidth=(['\"]?)0\\1)(?=[^>]*\\bheight=(['\"]?)0\\2)[^>]*?/?>", RegexOption.IGNORE_CASE)
private val HtmlCommentPattern = Regex("<!--[\\s\\S]*?-->")
private val HtmlImagePattern = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE)
private val HtmlLinkPattern = Regex("(?i)\\b(href|src)=(['\"])([^'\"]+)\\2")
private val HtmlAnchorPattern = Regex("<a\\b([^>]*)>(.*?)</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val HtmlImageAnchorPattern = Regex("<a\\b([^>]*)>\\s*(<img\\b[^>]*>)\\s*</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val HtmlBreakPattern = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
private val HtmlHorizontalRulePattern = Regex("<hr\\s*/?>", RegexOption.IGNORE_CASE)
private val HtmlSummaryPattern = Regex("<summary\\b[^>]*>(.*?)</summary>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val HtmlDetailsPattern = Regex("</?details\\b[^>]*>", RegexOption.IGNORE_CASE)
private val HtmlKeyboardPattern = Regex("<kbd\\b[^>]*>(.*?)</kbd>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val HtmlCodePattern = Regex("<code\\b[^>]*>(.*?)</code>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val HtmlMarkPattern = Regex("<mark\\b[^>]*>(.*?)</mark>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val HtmlStrongPattern = Regex("<(?:strong|b)\\b[^>]*>(.*?)</(?:strong|b)>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val HtmlEmphasisPattern = Regex("<(?:em|i)\\b[^>]*>(.*?)</(?:em|i)>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val HtmlDeletePattern = Regex("<(?:del|s|strike)\\b[^>]*>(.*?)</(?:del|s|strike)>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val HtmlUnderlinePattern = Regex("<u\\b[^>]*>(.*?)</u>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val HtmlSmallPattern = Regex("<small\\b[^>]*>(.*?)</small>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val HtmlSupPattern = Regex("<sup\\b[^>]*>(.*?)</sup>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val HtmlSubPattern = Regex("<sub\\b[^>]*>(.*?)</sub>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val GitHubAlertPattern = Regex("^(\\s*>\\s*)\\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)]\\s*$", RegexOption.IGNORE_CASE)
private val NonBreakingSpacePattern = Regex("&nbsp;", RegexOption.IGNORE_CASE)

private val GitHubTextPrimaryColor = 0xFF24292F.toInt()
private val SuperscriptCharacters = mapOf(
    '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
    '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
    '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
    'n' to 'ⁿ', 'i' to 'ⁱ'
)
private val SubscriptCharacters = mapOf(
    '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
    '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
    '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
    'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ',
    'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ',
    'p' to 'ₚ', 'r' to 'ᵣ', 's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ',
    'v' to 'ᵥ', 'x' to 'ₓ'
)
private val GitHubAccentColor = 0xFF0969DA.toInt()
private val GitHubBorderColor = 0xFFD0D7DE.toInt()
private val GitHubSubtleBackgroundColor = 0xFFF6F8FA.toInt()
private const val MarkdownBlockMarginDp = 14
private const val MarkdownBlockQuoteWidthDp = 3
private const val MarkdownBulletWidthDp = 6
private const val MarkdownThematicBreakHeightDp = 1
private const val MarkdownHeadingBreakHeightDp = 1
private const val RepositoryMarkdownLogTag = "RepositoryMarkdown"
private const val RepositoryMarkdownImageSignature = "repository-markdown-image-v9"
private const val MarkdownImageWidthHintPrefix = "sunset-width="
private const val MarkdownImagePlaceholderWidthDp = 320
private const val MarkdownImagePlaceholderHeightDp = 180
private const val MarkdownImageErrorWidthDp = 320
private const val MarkdownImageErrorHeightDp = 48
private const val MarkdownImageMaxDecodeWidthPx = 1600
private const val MarkdownImageMaxDecodeHeightPx = 1600
private const val MarkdownImageMaxDisplayHeightPx = 1400

private fun createRepositoryMarkdownThemePlugin(context: Context): AbstractMarkwonPlugin {
    val density = context.resources.displayMetrics.density
    fun dp(value: Int): Int = (value * density).toInt().coerceAtLeast(1)
    return object : AbstractMarkwonPlugin() {
        override fun configureTheme(builder: MarkwonTheme.Builder) {
            builder
                .linkColor(GitHubAccentColor)
                .isLinkUnderlined(false)
                .blockMargin(dp(MarkdownBlockMarginDp))
                .blockQuoteColor(GitHubBorderColor)
                .blockQuoteWidth(dp(MarkdownBlockQuoteWidthDp))
                .bulletWidth(dp(MarkdownBulletWidthDp))
                .codeTextColor(GitHubTextPrimaryColor)
                .codeBackgroundColor(GitHubSubtleBackgroundColor)
                .codeBlockTextColor(GitHubTextPrimaryColor)
                .codeBlockBackgroundColor(GitHubSubtleBackgroundColor)
                .thematicBreakColor(GitHubBorderColor)
                .thematicBreakHeight(dp(MarkdownThematicBreakHeightDp))
                .headingBreakColor(GitHubBorderColor)
                .headingBreakHeight(dp(MarkdownHeadingBreakHeightDp))
                .headingTextSizeMultipliers(floatArrayOf(1.7f, 1.45f, 1.25f, 1.1f, 1.0f, 0.95f))
                .listItemColor(GitHubTextPrimaryColor)
        }
    }
}
private object RepositoryMarkdownImageSizePlugin : AbstractMarkwonPlugin() {
    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
        builder.imageSizeResolver(RepositoryMarkdownImageSizeResolver())
    }
}
private class RepositoryMarkdownImageSizeResolver : ImageSizeResolverDef() {
    override fun resolveImageSize(drawable: AsyncDrawable): Rect {
        val destination = drawable.destination
        val requestedWidthDp = destination.findMarkdownImageWidthHint()
        if (requestedWidthDp != null) {
            val result = drawable.result
            val bounds = result.bounds
            val intrinsicWidth = result.intrinsicWidth
            val intrinsicHeight = result.intrinsicHeight
            val width = intrinsicWidth.takeIf { it > 0 } ?: bounds.width().takeIf { it > 0 } ?: requestedWidthDp
            val height = intrinsicHeight.takeIf { it > 0 } ?: bounds.height().takeIf { it > 0 } ?: requestedWidthDp
            val density = Resources.getSystem().displayMetrics.density
            val resolvedWidth = (requestedWidthDp * density).toInt().coerceAtLeast(1)
            val resolvedHeight = (resolvedWidth * height.toFloat() / width.toFloat()).toInt().coerceAtLeast(1)
            return Rect(0, 0, resolvedWidth, resolvedHeight)
        }
        val result = drawable.result
        val bounds = result.bounds
        val intrinsicWidth = result.intrinsicWidth
        val intrinsicHeight = result.intrinsicHeight
        val width = intrinsicWidth.takeIf { it > 0 } ?: bounds.width()
        val height = intrinsicHeight.takeIf { it > 0 } ?: bounds.height()
        if (width <= 0 || height <= 0) return super.resolveImageSize(drawable)
        val canvasWidth = drawable.lastKnownCanvasWidth.takeIf { it > 0 } ?: width
        val density = Resources.getSystem().displayMetrics.density
        val scaledWidth = if (destination.isRemoteBadgeImageUrl()) {
            (width * density).toInt().coerceAtLeast(1)
        } else {
            width
        }
        val resolvedWidth = scaledWidth.coerceAtMost(canvasWidth)
        val resolvedHeight = (resolvedWidth * height.toFloat() / width.toFloat()).toInt().coerceAtLeast(1)
            .coerceAtMost(if (destination.isRemoteBadgeImageUrl()) Int.MAX_VALUE else MarkdownImageMaxDisplayHeightPx)
        return Rect(0, 0, resolvedWidth, resolvedHeight)
    }
}


private object RepositoryMarkdownAsyncDrawablePlugin : AbstractMarkwonPlugin() {
    override fun beforeSetText(textView: TextView, markdown: Spanned) {
        AsyncDrawableScheduler.unschedule(textView)
    }

    override fun afterSetText(textView: TextView) {
        AsyncDrawableScheduler.schedule(textView)
    }
}


private fun createRepositoryGlideImagesPlugin(context: Context): GlideImagesPlugin {
    val appContext = context.applicationContext
    fun createBadgePlaceholder(): Drawable = ColorDrawable(Color.TRANSPARENT).apply {
        setBounds(0, 0, 1, 1)
    }
    fun createImagePlaceholder(): Drawable = ColorDrawable(GitHubSubtleBackgroundColor).apply {
        val density = appContext.resources.displayMetrics.density
        setBounds(0, 0, (MarkdownImagePlaceholderWidthDp * density).toInt(), (MarkdownImagePlaceholderHeightDp * density).toInt())
    }
    fun createImageErrorPlaceholder(): Drawable = ColorDrawable(GitHubBorderColor).apply {
        val density = appContext.resources.displayMetrics.density
        setBounds(0, 0, (MarkdownImageErrorWidthDp * density).toInt(), (MarkdownImageErrorHeightDp * density).toInt())
    }
    return GlideImagesPlugin.create(object : GlideImagesPlugin.GlideStore {
        override fun load(drawable: AsyncDrawable): RequestBuilder<Drawable> {
            val destination = drawable.destination
            if (destination.isRemoteBadgeImageUrl()) {
                Log.d(RepositoryMarkdownLogTag, "glide load destination=$destination")
            }
            val model = if (destination.needsGitHubAuthorizationHeader()) {
                val token = RepositoryMarkdownRenderer.imageAccessToken
                if (token.isBlank()) {
                    destination
                } else {
                    GlideUrl(
                        destination,
                        LazyHeaders.Builder()
                            .addHeader("Authorization", "Bearer $token")
                            .addHeader("User-Agent", "SunsetGitHub-Android")
                            .build()
                    )
                }
            } else {
                destination
            }
            val isBadge = destination.isRemoteBadgeImageUrl()
            return Glide.with(appContext)
                .load(model)
                .placeholder(if (isBadge) createBadgePlaceholder() else createImagePlaceholder())
                .error(if (isBadge) createBadgePlaceholder() else createImageErrorPlaceholder())
                .override(
                    if (isBadge) Target.SIZE_ORIGINAL else MarkdownImageMaxDecodeWidthPx,
                    if (isBadge) Target.SIZE_ORIGINAL else MarkdownImageMaxDecodeHeightPx
                )
                .dontTransform()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .signature(ObjectKey(RepositoryMarkdownImageSignature))
        }

        override fun cancel(target: Target<*>) {
            Glide.with(appContext).clear(target)
        }
    })
}

private fun String.needsGitHubAuthorizationHeader(): Boolean {
    val lower = lowercase()
    if (lower.startsWith("https://github.com/user-attachments/")) return false
    return lower.startsWith("https://raw.githubusercontent.com/") ||
        lower.startsWith("https://github.com/") ||
        lower.startsWith("https://user-images.githubusercontent.com/")
}

private fun String.findMarkdownImageWidthHint(): Int? {
    val markerIndex = indexOf(MarkdownImageWidthHintPrefix)
    if (markerIndex < 0) return null
    return substring(markerIndex + MarkdownImageWidthHintPrefix.length)
        .takeWhile { it.isDigit() }
        .toIntOrNull()
        ?.takeIf { it > 0 }
}

private object RepositoryMarkdownRenderer {
    var imageAccessToken: String = ""
    var badgeMetadata: RepositoryMarkdownBadgeMetadata? = null
    private var markwon: Markwon? = null

    fun get(textView: TextView): Markwon {
        val context = textView.context.applicationContext
        return markwon ?: Markwon.builder(context)
            .usePlugin(createRepositoryMarkdownThemePlugin(context))
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(createRepositoryGlideImagesPlugin(context))
            .usePlugin(RepositoryMarkdownAsyncDrawablePlugin)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(RepositoryMarkdownImageSizePlugin)
            .build()
            .also { markwon = it }
    }
}