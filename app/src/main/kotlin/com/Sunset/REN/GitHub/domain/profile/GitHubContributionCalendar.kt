package com.Sunset.REN.GitHub.domain.profile

/**
 * GitHub profile contribution calendar returned by GraphQL `contributionsCollection`.
 * The shape mirrors the web contribution wall: weeks are columns and days are rows.
 */
data class GitHubContributionCalendar(
    val totalContributions: Int,
    val weeks: List<GitHubContributionWeek>,
    val months: List<GitHubContributionMonth>,
    val overview: GitHubContributionOverview = GitHubContributionOverview(),
    val year: Int? = null
)

data class GitHubContributionWeek(
    val firstDay: String,
    val days: List<GitHubContributionDay>
)

data class GitHubContributionDay(
    val date: String,
    val weekday: Int,
    val contributionCount: Int,
    val color: String?
)

data class GitHubContributionOverview(
    val commitCount: Int = 0,
    val issueCount: Int = 0,
    val pullRequestCount: Int = 0,
    val pullRequestReviewCount: Int = 0,
    val restrictedContributionCount: Int = 0,
    val repositoryNames: List<String> = emptyList()
) {
    val totalCategorizedContributions: Int
        get() = commitCount + issueCount + pullRequestCount + pullRequestReviewCount
}

data class GitHubContributionMonth(
    val name: String,
    val year: Int,
    val firstDay: String,
    val totalWeeks: Int
)
