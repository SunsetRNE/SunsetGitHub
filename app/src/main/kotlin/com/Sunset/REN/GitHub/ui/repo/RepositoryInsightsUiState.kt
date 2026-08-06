package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlSectionSummary

data class RepositoryInsightsUiState(
    val owner: String = "",
    val repo: String = "",
    val summary: RepositoryHtmlSectionSummary? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val sourceUrl: String? = null,
    val selectedTab: RepositoryInsightsTab = RepositoryInsightsTab.Overview,
    val selectedChart: RepositoryInsightsChart = RepositoryInsightsChart.Combined,
    val mode: RepositoryInsightsMode = RepositoryInsightsMode.Loading,
    val cachedAtMillis: Long? = null
) {
    val isInitialLoad: Boolean get() = isLoading && summary == null && errorMessage == null
}

enum class RepositoryInsightsMode {
    Loading,
    Ready,
    Empty,
    Limited,
    Stale,
    Error
}

enum class RepositoryInsightsTab {
    Overview,
    Pulse,
    Quality,
    Traffic
}

enum class RepositoryInsightsChart {
    Combined,
    Actions,
    Issues,
    Releases
}

data class RepositoryInsightsChartData(
    val chip: String,
    val subtitle: String,
    val hint: String,
    val total: String,
    val heights: List<Int>,
    val kinds: List<RepositoryInsightsChartBarKind>,
    val labels: List<String> = emptyList()
)

enum class RepositoryInsightsChartBarKind {
    Blue,
    Green,
    Amber
}