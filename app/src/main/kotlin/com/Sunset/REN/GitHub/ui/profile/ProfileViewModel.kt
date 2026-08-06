package com.Sunset.REN.GitHub.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.GitHubRepositoryApiGateway
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.RepositoryListCacheStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import com.Sunset.REN.GitHub.data.local.UserProfileCacheStore
import com.Sunset.REN.GitHub.domain.auth.GitHubAccount
import com.Sunset.REN.GitHub.domain.profile.GitHubContributionCalendar
import com.Sunset.REN.GitHub.domain.profile.GitHubUserProfile
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.RepositoryLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)
    private val profileCacheStore = UserProfileCacheStore(application)
    private val repositoryListCacheStore = RepositoryListCacheStore(application)
    private var isRefreshingProfile = false

    private val _profileState = MutableLiveData<ProfileUiState>(ProfileUiState.Loading)
    val profileState: LiveData<ProfileUiState> = _profileState

    private var targetLogin: String? = null
    private var selectedContributionYear: Int? = null
    private var hasStarted = false

    /**
     * 启动加载。login 为 null 时展示当前登录用户，非空时展示指定用户（如仓库贡献者）。
     */
    fun start(login: String?) {
        val normalizedLogin = login?.takeIf { it.isNotBlank() }
        if (hasStarted && normalizedLogin == targetLogin) return
        hasStarted = true
        targetLogin = normalizedLogin
        refreshProfile(forceRefresh = false)
    }

    fun retryProfile() {
        refreshProfile(forceRefresh = true)
    }

    fun setContributionYear(year: Int?) {
        if (selectedContributionYear == year) return
        selectedContributionYear = year
        refreshProfile(forceRefresh = true)
    }

    fun availableContributionYears(): List<Int> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return (currentYear downTo currentYear - ContributionYearHistoryCount + 1).toList()
    }

    fun refreshProfile(forceRefresh: Boolean = false) {
        if (isRefreshingProfile) return
        viewModelScope.launch {
            isRefreshingProfile = true
            var shownCachedState = false
            try {
                val session = loadSessionOrNull() ?: run {
                    _profileState.value = ProfileUiState.SignedOut
                    return@launch
                }
                val login = targetLogin
                if (login != null) {
                    refreshOtherUserProfile(session, login, forceRefresh)
                } else {
                    shownCachedState = refreshCurrentUserProfile(session, forceRefresh)
                }
            } catch (exception: Exception) {
                val message = exception.message ?: "加载个人页面失败"
                val currentState = _profileState.value as? ProfileUiState.Content
                _profileState.value = if (shownCachedState && currentState != null) {
                    currentState.copy(
                        isRefreshingFromCache = false,
                        refreshError = message
                    )
                } else {
                    ProfileUiState.Error(message)
                }
            } finally {
                isRefreshingProfile = false
            }
        }
    }

    private suspend fun refreshCurrentUserProfile(session: GitHubSession, forceRefresh: Boolean): Boolean {
        var shownCachedState = false
        val cachedRepositories = withContext(Dispatchers.IO) {
            repositoryListCacheStore.getCachedRepositories(session.account.id)?.repositories.orEmpty()
        }
        if (!forceRefresh) {
            val cachedProfile = withContext(Dispatchers.IO) {
                profileCacheStore.getCachedProfile(session.account.id)
            }
            if (cachedProfile != null) {
                val profileNeedsRefresh = shouldRefresh(cachedProfile.refreshedAtMillis)
                    _profileState.value = buildContent(
                        profile = cachedProfile.profile,
                        repositories = cachedRepositories,
                        refreshedAtMillis = cachedProfile.refreshedAtMillis,
                        isRefreshingFromCache = profileNeedsRefresh
                    )
                    shownCachedState = true
                    if (!profileNeedsRefresh) {
                        refreshCachedCurrentUserContributions(
                            session = session,
                            profile = cachedProfile.profile,
                            repositories = cachedRepositories,
                            refreshedAtMillis = cachedProfile.refreshedAtMillis
                        )
                        return shownCachedState
                    }
                } else {
                    _profileState.value = ProfileUiState.Loading
                }
        } else {
            _profileState.value = ProfileUiState.Loading
        }

        val gateway = GitHubRepositoryApiGateway(session.token)
        val refreshedProfile = withContext(Dispatchers.IO) {
            gateway.getCurrentUserProfile()
        }
        val contributionResult = loadContributionCalendar(gateway, refreshedProfile.login, selectedContributionYear)
        val refreshedAtMillis = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            profileCacheStore.cacheProfile(session.account.id, refreshedProfile, refreshedAtMillis)
            currentAccountStore.saveCurrentAccount(
                session.account.copy(
                    login = refreshedProfile.login,
                    avatarUrl = refreshedProfile.avatarUrl,
                    name = refreshedProfile.name
                )
            )
        }
        _profileState.value = buildContent(
            profile = refreshedProfile,
            repositories = cachedRepositories,
            contributionCalendar = contributionResult.calendar,
            contributionError = contributionResult.error,
            refreshedAtMillis = refreshedAtMillis,
            isRefreshingFromCache = false
        )
        return shownCachedState
    }

    private suspend fun refreshCachedCurrentUserContributions(
        session: GitHubSession,
        profile: GitHubUserProfile,
        repositories: List<GitHubRepository>,
        refreshedAtMillis: Long
    ) {
        val gateway = GitHubRepositoryApiGateway(session.token)
        val contributionResult = loadContributionCalendar(gateway, profile.login, selectedContributionYear)
        _profileState.value = buildContent(
            profile = profile,
            repositories = repositories,
            contributionCalendar = contributionResult.calendar,
            contributionError = contributionResult.error,
            refreshedAtMillis = refreshedAtMillis,
            isRefreshingFromCache = false
        )
    }

    private suspend fun refreshOtherUserProfile(session: GitHubSession, login: String, forceRefresh: Boolean) {
        _profileState.value = ProfileUiState.Loading
        val gateway = GitHubRepositoryApiGateway(session.token)
        val refreshedProfile = withContext(Dispatchers.IO) {
            gateway.getUserProfile(login)
        }
        val repositories = withContext(Dispatchers.IO) {
            runCatching {
                gateway.listUserRepositories(login, page = 1, perPage = OtherUserRepositoryPageSize)
            }.getOrDefault(emptyList())
        }
        val contributionResult = loadContributionCalendar(gateway, refreshedProfile.login, selectedContributionYear)
        _profileState.value = buildContent(
            profile = refreshedProfile,
            repositories = repositories,
            contributionCalendar = contributionResult.calendar,
            contributionError = contributionResult.error,
            refreshedAtMillis = System.currentTimeMillis(),
            isRefreshingFromCache = false
        )
    }

    private fun buildContent(
        profile: GitHubUserProfile,
        repositories: List<GitHubRepository>,
        contributionCalendar: GitHubContributionCalendar? = null,
        contributionError: String? = null,
        refreshedAtMillis: Long?,
        isRefreshingFromCache: Boolean
    ): ProfileUiState.Content {
        val visibleRepositories = repositories.filterNot { it.isPrivate }
        val profileRepositories = visibleRepositories
            .sortedWith(
                compareByDescending<GitHubRepository> { repository -> repository.updatedAt.orEmpty() }
                    .thenByDescending { it.pushedAt.orEmpty() }
                    .thenBy { it.name.lowercase() }
            )
            .take(ProfileRepositoryListLimit)
        val primaryLanguage = resolvePrimaryLanguage(visibleRepositories)
        val languageSummaries = buildLanguageSummaries(visibleRepositories)
        return ProfileUiState.Content(
            profile = profile,
            profileRepositories = profileRepositories,
            primaryLanguage = primaryLanguage,
            languageSummaries = languageSummaries,
            contributionCalendar = contributionCalendar,
            contributionError = contributionError,
            sourceRepositoryCount = visibleRepositories.count { !it.fork },
            forkRepositoryCount = visibleRepositories.count { it.fork },
            archivedRepositoryCount = visibleRepositories.count { it.archived },
            totalStars = visibleRepositories.sumOf { it.stargazersCount },
            totalForks = visibleRepositories.sumOf { it.forksCount },
            totalWatchers = visibleRepositories.sumOf { it.watchersCount },
            totalOpenIssues = visibleRepositories.sumOf { it.openIssuesCount },
            refreshedAtMillis = refreshedAtMillis,
            isRefreshingFromCache = isRefreshingFromCache
        )
    }

    private suspend fun loadContributionCalendar(
        gateway: GitHubRepositoryApiGateway,
        login: String,
        year: Int?
    ): ContributionCalendarResult {
        return withContext(Dispatchers.IO) {
            runCatching { gateway.getContributionCalendar(login, year) }
                .fold(
                    onSuccess = { calendar -> ContributionCalendarResult(calendar = calendar, error = null) },
                    onFailure = { error -> ContributionCalendarResult(calendar = null, error = error.message ?: "加载贡献墙失败") }
                )
        }
    }

    private fun buildLanguageSummaries(repositories: List<GitHubRepository>): List<ProfileLanguageSummary> {
        val languages = repositories.flatMap { repository ->
            if (repository.languages.isNotEmpty()) {
                repository.languages
            } else {
                repository.language
                    ?.takeIf(String::isNotBlank)
                    ?.let { language -> listOf(RepositoryLanguage(language, 0L, 0)) }
                    .orEmpty()
            }
        }
        val totalBytes = languages.sumOf { it.bytes }.takeIf { it > 0L }
        val totalOccurrences = languages.size.takeIf { it > 0 }
        return languages
            .groupBy { it.name }
            .map { (name, values) ->
                val bytes = values.sumOf { it.bytes }
                val repositoryCount = values.size
                val percentage = totalBytes?.let { ((bytes * 100) / it).toInt() }
                    ?: values.maxOfOrNull { it.percentage }?.takeIf { it > 0 }
                    ?: totalOccurrences?.let { ((repositoryCount * 100) / it) }
                    ?: 0
                ProfileLanguageSummary(
                    name = name,
                    repositoryCount = repositoryCount,
                    bytes = bytes,
                    percentage = percentage
                )
            }
            .sortedWith(
                compareByDescending<ProfileLanguageSummary> { it.bytes }
                    .thenByDescending { it.repositoryCount }
                    .thenByDescending { it.percentage }
                    .thenBy { it.name.lowercase() }
            )
            .take(LanguageSummaryLimit)
    }

    private fun resolvePrimaryLanguage(repositories: List<GitHubRepository>): String? {
        val bytesByLanguage = repositories
            .flatMap { repository -> repository.languages }
            .groupBy { language -> language.name }
            .mapValues { entry -> entry.value.sumOf { language -> language.bytes } }
            .filterValues { bytes -> bytes > 0L }
        if (bytesByLanguage.isNotEmpty()) {
            return bytesByLanguage.maxByOrNull { it.value }?.key
        }
        return repositories
            .mapNotNull { repository -> repository.language?.takeIf(String::isNotBlank) }
            .groupingBy { language -> language }
            .eachCount()
            .maxByOrNull { entry -> entry.value }
            ?.key
    }

    private suspend fun loadSessionOrNull(): GitHubSession? {
        val account = withContext(Dispatchers.IO) {
            currentAccountStore.getCurrentAccount()
        } ?: return null
        val token = withContext(Dispatchers.IO) {
            tokenStore.getAccessToken(account.id)
        }?.takeIf { it.isNotBlank() } ?: return null
        return GitHubSession(account, token)
    }

    private fun shouldRefresh(refreshedAtMillis: Long): Boolean {
        if (refreshedAtMillis <= 0L) return true
        return System.currentTimeMillis() - refreshedAtMillis > ProfileCacheTtlMillis
    }

    private data class GitHubSession(
        val account: GitHubAccount,
        val token: String
    )

    private data class ContributionCalendarResult(
        val calendar: GitHubContributionCalendar?,
        val error: String?
    )

    private companion object {
        const val ProfileRepositoryListLimit = 12
        const val LanguageSummaryLimit = 4
        const val ProfileCacheTtlMillis = 24L * 60L * 60L * 1_000L
        const val OtherUserRepositoryPageSize = 30
        const val ContributionYearHistoryCount = 5
    }
}