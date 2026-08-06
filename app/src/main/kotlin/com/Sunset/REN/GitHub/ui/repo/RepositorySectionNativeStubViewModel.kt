package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.html.GitHubHtmlGateway
import com.Sunset.REN.GitHub.data.github.html.GitHubHtmlParseResult
import com.Sunset.REN.GitHub.data.github.html.GitHubRepositoryAgentsGateway
import com.Sunset.REN.GitHub.data.github.html.GitHubRepositoryInsightsGateway
import com.Sunset.REN.GitHub.data.github.html.GitHubRepositoryProjectsGateway
import com.Sunset.REN.GitHub.data.github.html.GitHubRepositorySecurityQualityGateway
import com.Sunset.REN.GitHub.data.github.html.GitHubRepositorySettingsGateway
import com.Sunset.REN.GitHub.data.github.html.GitHubRepositoryWikiGateway
import com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlFacade
import com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlSectionStatus
import com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlSectionSummary
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositorySectionNativeStubViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _sectionState = MutableLiveData(RepositorySectionNativeStubUiState())
    val sectionState: LiveData<RepositorySectionNativeStubUiState> = _sectionState

    private var accessToken: String = ""
    private var hasPrepared = false

    fun prepare(owner: String, repo: String, sectionKey: String) {
        if (hasPrepared) return
        hasPrepared = true
        _sectionState.value = RepositorySectionNativeStubUiState(
            owner = owner,
            repo = repo,
            sectionKey = sectionKey
        )
        loadSection()
    }

    fun loadSection() {
        val state = _sectionState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank() || state.sectionKey.isBlank()) {
            _sectionState.value = state.copy(
                isLoading = false,
                errorMessage = "仓库或分区信息缺失，无法加载 HTML 摘要。"
            )
            return
        }
        viewModelScope.launch {
            _sectionState.value = state.copy(
                isLoading = true,
                errorMessage = null,
                sectionStatusCode = state.sectionStatusCode.takeIf { state.summary != null },
                htmlPreview = state.htmlPreview.takeIf { state.summary != null },
                isShowingStaleContent = state.summary != null
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _sectionState.value = _sectionState.value?.copy(
                    isLoading = false,
                    isShowingStaleContent = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val htmlResult = RepositoryHtmlFacade(GitHubHtmlGateway(token)).loadRepositorySection(
                        owner = state.owner,
                        repo = state.repo,
                        sectionKey = state.sectionKey
                    )
                    htmlResult.withRestFallback(
                        owner = state.owner,
                        repo = state.repo,
                        sectionKey = state.sectionKey,
                        token = token
                    )
                }
            }
            _sectionState.value = result.fold(
                onSuccess = { parseResult -> parseResult.toUiState() },
                onFailure = { error ->
                    Log.w(LogTag, "HTML section load failed: ${state.owner}/${state.repo} ${state.sectionKey}", error)
                    _sectionState.value?.copy(
                        isLoading = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage,
                        isShowingStaleContent = false,
                        sectionStatusCode = null,
                        htmlPreview = null
                    )
                }
            )
        }
    }

    private suspend fun GitHubHtmlParseResult<RepositoryHtmlSectionSummary>.withRestFallback(
        owner: String,
        repo: String,
        sectionKey: String,
        token: String
    ): GitHubHtmlParseResult<RepositoryHtmlSectionSummary> {
        val htmlSummary = (this as? GitHubHtmlParseResult.Success)?.value
        if (htmlSummary?.status == RepositoryHtmlSectionStatus.Available && sectionKey != AgentsSectionKey) return this
        val fallback = when (sectionKey) {
            SettingsSectionKey -> GitHubRepositorySettingsGateway(token).loadSettingsSummary(owner, repo)
            InsightsSectionKey -> GitHubRepositoryInsightsGateway(token).loadInsightsSummary(owner, repo)
            WikiSectionKey -> GitHubRepositoryWikiGateway(token).loadWikiSummary(owner, repo)
            ProjectsSectionKey -> GitHubRepositoryProjectsGateway(token).loadProjectsSummary(owner, repo)
            SecurityQualitySectionKey -> GitHubRepositorySecurityQualityGateway(token).loadSecurityQualitySummary(owner, repo)
            AgentsSectionKey -> GitHubRepositoryAgentsGateway(token).loadAgentsSummary(owner, repo)
            else -> return this
        }
        Log.w(
            LogTag,
            "${sectionKey} HTML summary unavailable, using REST fallback: $owner/$repo htmlStatus=${htmlSummary?.status ?: this::class.simpleName}"
        )
        return fallback
    }

    private fun GitHubHtmlParseResult<RepositoryHtmlSectionSummary>.toUiState(): RepositorySectionNativeStubUiState? {
        val current = _sectionState.value ?: return null
        return when (this) {
            is GitHubHtmlParseResult.Success -> {
                Log.d(LogTag, value.toDebugLogLine())
                current.copy(
                    isLoading = false,
                    summary = value,
                    isShowingStaleContent = false,
                    errorMessage = null,
                    sourceUrl = value.sourceUrl,
                    sectionStatusCode = null,
                    htmlPreview = null
                )
            }
            is GitHubHtmlParseResult.AccessDenied -> {
                Log.w(LogTag, toDebugLogLine("access denied"))
                current.copy(
                    isLoading = false,
                    summary = null,
                    isShowingStaleContent = false,
                    errorMessage = message,
                    sourceUrl = sourceUrl,
                    sectionStatusCode = statusCode,
                    htmlPreview = htmlPreview
                )
            }
            is GitHubHtmlParseResult.NotFound -> {
                Log.w(LogTag, toDebugLogLine("not found"))
                current.copy(
                    isLoading = false,
                    summary = null,
                    isShowingStaleContent = false,
                    errorMessage = message,
                    sourceUrl = sourceUrl,
                    sectionStatusCode = statusCode,
                    htmlPreview = htmlPreview
                )
            }
            is GitHubHtmlParseResult.ParseError -> {
                Log.w(LogTag, toDebugLogLine("parse error"))
                current.copy(
                    isLoading = false,
                    summary = null,
                    isShowingStaleContent = false,
                    errorMessage = message,
                    sourceUrl = sourceUrl,
                    sectionStatusCode = statusCode,
                    htmlPreview = htmlPreview
                )
            }
        }
    }

    private suspend fun ensureAccessToken(): String? {
        if (accessToken.isNotBlank()) return accessToken
        val account = withContext(Dispatchers.IO) { currentAccountStore.getCurrentAccount() } ?: return null
        val token = withContext(Dispatchers.IO) { tokenStore.getAccessToken(account.id) }
            ?.takeIf { it.isNotBlank() } ?: return null
        accessToken = token
        return token
    }

    private fun RepositoryHtmlSectionSummary.toDebugLogLine(): String {
        val metricText = metrics.joinToString { metric -> "${metric.label}=${metric.value}" }
        return "HTML section parsed: $owner/$repo $sectionKey status=$status metrics=[$metricText] notices=${notices.size} actions=${actions.size} url=$sourceUrl"
    }

    private fun GitHubHtmlParseResult<RepositoryHtmlSectionSummary>.toDebugLogLine(fallbackTag: String): String {
        return when (this) {
            is GitHubHtmlParseResult.AccessDenied -> "HTML section $fallbackTag: $message statusCode=$statusCode url=$sourceUrl preview=$htmlPreview"
            is GitHubHtmlParseResult.NotFound -> "HTML section $fallbackTag: $message statusCode=$statusCode url=$sourceUrl preview=$htmlPreview"
            is GitHubHtmlParseResult.ParseError -> "HTML section $fallbackTag: $message statusCode=$statusCode url=$sourceUrl preview=$htmlPreview"
            is GitHubHtmlParseResult.Success -> "HTML section parsed"
        }
    }

    private companion object {
        const val NotSignedInMessage = "当前账号未登录或令牌已失效。"
        const val UnknownErrorMessage = "加载 GitHub HTML 分区摘要时发生未知错误。"
        const val LogTag = "RepositoryHtmlParse"
        const val SettingsSectionKey = "settings"
        const val InsightsSectionKey = "insights"
        const val WikiSectionKey = "wiki"
        const val ProjectsSectionKey = "projects"
        const val SecurityQualitySectionKey = "security_quality"
        const val AgentsSectionKey = "agents"
    }
}

data class RepositorySectionNativeStubUiState(
    val owner: String = "",
    val repo: String = "",
    val sectionKey: String = "",
    val isLoading: Boolean = false,
    val summary: RepositoryHtmlSectionSummary? = null,
    val errorMessage: String? = null,
    val sourceUrl: String? = null,
    val sectionStatusCode: Int? = null,
    val htmlPreview: String? = null,
    val isShowingStaleContent: Boolean = false
)