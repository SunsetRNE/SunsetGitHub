package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.html.GitHubHtmlParseResult
import com.Sunset.REN.GitHub.data.github.html.GitHubRepositoryInsightsGateway
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.RepositoryInsightsCacheStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryInsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)
    private val cacheStore = RepositoryInsightsCacheStore(application)

    private val _insightsState = MutableLiveData(RepositoryInsightsUiState())
    val insightsState: LiveData<RepositoryInsightsUiState> = _insightsState

    private var accessToken: String = ""
    private var hasPrepared = false
    fun prepare(owner: String, repo: String) {
        if (hasPrepared) return
        hasPrepared = true
        val initialState = RepositoryInsightsUiState(owner = owner, repo = repo)
        _insightsState.value = cacheStore.getCachedInsights(owner, repo)?.let { cached ->
            initialState.copy(
                summary = cached.summary,
                sourceUrl = cached.summary.sourceUrl,
                mode = RepositoryInsightsMode.Stale,
                cachedAtMillis = cached.refreshedAtMillis
            )
        } ?: initialState
        refresh()
    }


    fun refresh() {
        val state = _insightsState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _insightsState.value = state.copy(
                isLoading = true,
                errorMessage = null,
                summary = state.summary,
                sourceUrl = state.sourceUrl,
                mode = RepositoryInsightsMode.Loading,
                cachedAtMillis = state.cachedAtMillis
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _insightsState.value = cachedFallbackState(state, NotSignedInMessage)
                    ?: _insightsState.value?.copy(
                        isLoading = false,
                        errorMessage = NotSignedInMessage,
                        mode = RepositoryInsightsMode.Error
                    )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { GitHubRepositoryInsightsGateway(token).loadInsightsSummary(state.owner, state.repo) }
            }
            _insightsState.value = result.fold(
                onSuccess = { parseResult -> parseResult.toUiState(cacheOnSuccess = true) },
                onFailure = { error ->
                    cachedFallbackState(
                        state = state,
                        message = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    ) ?: _insightsState.value?.copy(
                        isLoading = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage,
                        mode = RepositoryInsightsMode.Error,
                        cachedAtMillis = null
                    )
                }
            )
        }
    }

    fun selectTab(tab: RepositoryInsightsTab) {
        _insightsState.value = _insightsState.value?.copy(selectedTab = tab)
    }

    fun selectChart(chart: RepositoryInsightsChart) {
        _insightsState.value = _insightsState.value?.copy(selectedChart = chart)
    }

    private fun GitHubHtmlParseResult<com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlSectionSummary>.toUiState(
        cacheOnSuccess: Boolean = false
    ): RepositoryInsightsUiState? {
        return when (this) {
            is GitHubHtmlParseResult.Success -> {
                if (cacheOnSuccess) {
                    val now = System.currentTimeMillis()
                    cacheStore.cacheInsights(value.owner, value.repo, value, now)
                }
                _insightsState.value?.copy(
                    isLoading = false,
                    summary = value,
                    sourceUrl = value.sourceUrl,
                    errorMessage = null,
                    mode = value.toMode(),
                    cachedAtMillis = null
                )
            }
            is GitHubHtmlParseResult.AccessDenied -> errorOrCachedState(message, sourceUrl)
            is GitHubHtmlParseResult.NotFound -> errorOrCachedState(message, sourceUrl)
            is GitHubHtmlParseResult.ParseError -> errorOrCachedState(message, sourceUrl)
        }
    }

    private fun errorOrCachedState(message: String, sourceUrl: String?): RepositoryInsightsUiState? {
        val state = _insightsState.value ?: return null
        return cachedFallbackState(state, message) ?: state.copy(
            isLoading = false,
            errorMessage = message,
            sourceUrl = sourceUrl,
            mode = RepositoryInsightsMode.Error,
            cachedAtMillis = null
        )
    }

    private fun cachedFallbackState(state: RepositoryInsightsUiState, message: String): RepositoryInsightsUiState? {
        val cached = cacheStore.getCachedInsights(state.owner, state.repo) ?: return null
        return state.copy(
            summary = cached.summary,
            isLoading = false,
            errorMessage = message,
            sourceUrl = cached.summary.sourceUrl,
            mode = RepositoryInsightsMode.Stale,
            cachedAtMillis = cached.refreshedAtMillis
        )
    }

    private fun com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlSectionSummary.toMode(): RepositoryInsightsMode {
        val hasLimitedParts = notices.any { notice ->
            notice.contains("探测失败") || notice.contains("403") || notice.contains("权限")
        }
        val activityScore = metrics.firstIntValue("最近 Releases") +
            metrics.firstIntValue("最近 PR") +
            metrics.firstIntValue("最近 Issues") +
            metrics.firstIntValue("最近 Actions")
        return when {
            hasLimitedParts -> RepositoryInsightsMode.Limited
            activityScore == 0 -> RepositoryInsightsMode.Empty
            else -> RepositoryInsightsMode.Ready
        }
    }

    private fun List<com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlMetric>.firstIntValue(label: String): Int {
        return firstOrNull { it.label == label }?.value?.filter { it.isDigit() }?.toIntOrNull() ?: 0
    }

    private suspend fun ensureAccessToken(): String? {
        if (accessToken.isNotBlank()) return accessToken
        val account = withContext(Dispatchers.IO) { currentAccountStore.getCurrentAccount() } ?: return null
        val token = withContext(Dispatchers.IO) { tokenStore.getAccessToken(account.id) }
            ?.takeIf { it.isNotBlank() } ?: return null
        accessToken = token
        return token
    }

    private companion object {
        const val NotSignedInMessage = "当前账号未登录或令牌已失效。"
        const val UnknownErrorMessage = "加载仓库洞察页面时发生未知错误。"
    }
}