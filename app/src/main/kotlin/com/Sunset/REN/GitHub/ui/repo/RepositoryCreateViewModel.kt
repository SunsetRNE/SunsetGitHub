package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.GitHubRepositoryApiGateway
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.RepositoryCacheRepository
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.RepositoryCreateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryCreateViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)
    private val repositoryCacheRepository = RepositoryCacheRepository(application)

    private val _uiState = MutableLiveData<RepositoryCreateUiState>(RepositoryCreateUiState.Idle)
    val uiState: LiveData<RepositoryCreateUiState> = _uiState

    fun createRepository(
        name: String,
        description: String,
        homepage: String,
        isPrivate: Boolean,
        autoInit: Boolean,
        gitignoreTemplate: String?,
        licenseTemplate: String?,
        hasIssues: Boolean,
        hasProjects: Boolean,
        hasWiki: Boolean
    ) {
        if (_uiState.value is RepositoryCreateUiState.Submitting) return
        val normalizedName = name.trim()
        val normalizedHomepage = homepage.trim()
        val validationError = validateRepositoryName(normalizedName)
            ?: validateHomepage(normalizedHomepage)
        if (validationError != null) {
            _uiState.value = RepositoryCreateUiState.ValidationError(validationError)
            return
        }

        viewModelScope.launch {
            _uiState.value = RepositoryCreateUiState.Submitting
            try {
                val account = withContext(Dispatchers.IO) { currentAccountStore.getCurrentAccount() }
                    ?: run {
                        _uiState.value = RepositoryCreateUiState.SignedOut
                        return@launch
                    }
                val token = withContext(Dispatchers.IO) { tokenStore.getAccessToken(account.id) }
                    ?.takeIf { it.isNotBlank() }
                    ?: run {
                        _uiState.value = RepositoryCreateUiState.SignedOut
                        return@launch
                    }
                val repository = withContext(Dispatchers.IO) {
                    GitHubRepositoryApiGateway(token).createCurrentUserRepository(
                        RepositoryCreateRequest(
                            name = normalizedName,
                            description = description.trim().takeIf { it.isNotBlank() },
                            homepage = normalizedHomepage.takeIf { it.isNotBlank() },
                            isPrivate = isPrivate,
                            autoInit = autoInit,
                            gitignoreTemplate = gitignoreTemplate?.takeIf { it.isNotBlank() },
                            licenseTemplate = licenseTemplate?.takeIf { it.isNotBlank() },
                            hasIssues = hasIssues,
                            hasProjects = hasProjects,
                            hasWiki = hasWiki
                        )
                    ).also { createdRepository ->
                        repositoryCacheRepository.addOrUpdateRepository(account.id, createdRepository)
                    }
                }
                _uiState.value = RepositoryCreateUiState.Success(repository)
            } catch (exception: Exception) {
                _uiState.value = RepositoryCreateUiState.Error(
                    exception.message ?: "创建仓库失败，请稍后重试。"
                )
            }
        }
    }

    fun consumeTransientState() {
        if (_uiState.value !is RepositoryCreateUiState.Submitting && _uiState.value !is RepositoryCreateUiState.Success) {
            _uiState.value = RepositoryCreateUiState.Idle
        }
    }

    private fun validateRepositoryName(name: String): String? {
        return when {
            name.isBlank() -> "请填写仓库名称。"
            name.length > RepositoryNameMaxLength -> "仓库名称不能超过 $RepositoryNameMaxLength 个字符。"
            !RepositoryNamePattern.matches(name) -> "仓库名称只能包含字母、数字、点、短横线和下划线。"
            name == "." || name == ".." -> "仓库名称不能只使用点号。"
            else -> null
        }
    }

    private fun validateHomepage(homepage: String): String? {
        if (homepage.isBlank()) return null
        return when {
            homepage.length > HomepageMaxLength -> "主页地址不能超过 $HomepageMaxLength 个字符。"
            !HomepagePattern.matches(homepage) -> "主页地址需以 http:// 或 https:// 开头。"
            else -> null
        }
    }


    private companion object {
        const val RepositoryNameMaxLength = 100
        const val HomepageMaxLength = 512
        val RepositoryNamePattern = Regex("^[A-Za-z0-9._-]+$")
        val HomepagePattern = Regex("^https?://\\S+$")
    }
}

sealed interface RepositoryCreateUiState {
    data object Idle : RepositoryCreateUiState
    data object Submitting : RepositoryCreateUiState
    data object SignedOut : RepositoryCreateUiState
    data class ValidationError(val message: String) : RepositoryCreateUiState
    data class Error(val message: String) : RepositoryCreateUiState
    data class Success(val repository: GitHubRepository) : RepositoryCreateUiState
}