package com.Sunset.REN.GitHub.data.github.html

/** GitHub 网页解析的标准结果。HTML 结构变化时不抛给 UI，转成 ParseError 状态。 */
sealed class GitHubHtmlParseResult<out T> {
    data class Success<T>(val value: T) : GitHubHtmlParseResult<T>()
    data class ParseError(
        val message: String,
        val sourceUrl: String? = null,
        val statusCode: Int? = null,
        val htmlPreview: String? = null
    ) : GitHubHtmlParseResult<Nothing>()
    data class AccessDenied(
        val message: String,
        val sourceUrl: String? = null,
        val statusCode: Int? = null,
        val htmlPreview: String? = null
    ) : GitHubHtmlParseResult<Nothing>()
    data class NotFound(
        val message: String,
        val sourceUrl: String? = null,
        val statusCode: Int? = null,
        val htmlPreview: String? = null
    ) : GitHubHtmlParseResult<Nothing>()
}

data class GitHubHtmlPage(
    val url: String,
    val statusCode: Int,
    val html: String,
    val errorMessage: String? = null
) {
    val isSuccessful: Boolean get() = statusCode in 200..299
    val hasNetworkError: Boolean get() = errorMessage != null
    val htmlPreview: String get() = html.take(240).replace(Regex("\\s+"), " ").trim()
}

data class RepositoryHtmlSectionSummary(
    val owner: String,
    val repo: String,
    val sectionKey: String,
    val title: String,
    val status: RepositoryHtmlSectionStatus,
    val description: String,
    val metrics: List<RepositoryHtmlMetric> = emptyList(),
    val notices: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val sourceUrl: String
)

data class RepositoryHtmlMetric(
    val label: String,
    val value: String
)

enum class RepositoryHtmlSectionStatus {
    Available,
    Empty,
    Disabled,
    AccessDenied,
    ParsePartial,
    ParseFailed
}