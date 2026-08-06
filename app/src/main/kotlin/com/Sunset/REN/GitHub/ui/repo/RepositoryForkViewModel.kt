package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.GitHubRepositoryApiGateway
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class RepositoryForkViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _forkState = MutableLiveData<RepositoryForkUiState>(RepositoryForkUiState.Loading)
    val forkState: LiveData<RepositoryForkUiState> = _forkState

    private var accessToken: String = ""
    private var currentAccountLogin: String = ""
    private var loadedOwner: String = ""
    private var loadedRepo: String = ""
    private var nameCheckJob: Job? = null

    fun prepare(owner: String, repo: String) {
        if (owner == loadedOwner && repo == loadedRepo && _forkState.value is RepositoryForkUiState.Content) return
        loadedOwner = owner
        loadedRepo = repo
        viewModelScope.launch {
            _forkState.value = RepositoryForkUiState.Loading
            try {
                val account = withContext(Dispatchers.IO) { currentAccountStore.getCurrentAccount() }
                if (account == null) {
                    _forkState.value = RepositoryForkUiState.SignedOut
                    return@launch
                }
                val token = withContext(Dispatchers.IO) { tokenStore.getAccessToken(account.id) }
                if (token.isNullOrBlank()) {
                    _forkState.value = RepositoryForkUiState.SignedOut
                    return@launch
                }
                accessToken = token
                currentAccountLogin = account.login
                val gateway = GitHubRepositoryApiGateway(token)
                val repository = withContext(Dispatchers.IO) { gateway.getRepository(owner, repo, includeLanguages = false) }
                val eligibilityError = repository.forkEligibilityError(account.login)
                _forkState.value = RepositoryForkUiState.Content(
                    sourceRepository = repository,
                    currentAccountLogin = account.login,
                    isCheckingExistingFork = eligibilityError == null,
                    eligibilityError = eligibilityError
                )
                if (eligibilityError == null) {
                    checkExistingFork(gateway, owner, repo, account.login)
                }
            } catch (exception: Exception) {
                _forkState.value = RepositoryForkUiState.Error(exception.message ?: "加载 Fork 信息失败")
            }
        }
    }

    fun updateDraft(targetOwner: String, targetName: String, description: String) {
        val state = _forkState.value as? RepositoryForkUiState.Content ?: return
        val normalizedOwner = targetOwner.trim()
        val normalizedName = targetName.trim()
        _forkState.value = state.copy(
            targetOwner = normalizedOwner,
            targetName = normalizedName,
            description = description.take(DescriptionMaxLength),
            isNameAvailable = null,
            nameCheckError = null
        )
        scheduleNameAvailabilityCheck(normalizedOwner, normalizedName)
    }

    fun createFork(targetOwner: String, targetName: String, description: String, defaultBranchOnly: Boolean) {
        updateDraft(targetOwner, targetName, description)
        val state = _forkState.value as? RepositoryForkUiState.Content ?: return
        if (!state.canCreateFork || accessToken.isBlank()) return
        viewModelScope.launch {
            _forkState.value = state.copy(isCreating = true, errorMessage = null)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val gateway = GitHubRepositoryApiGateway(accessToken)
                    val fork = gateway.createFork(
                        owner = loadedOwner,
                        repo = loadedRepo,
                        targetOwner = state.targetOwner.takeUnless { it.equals(currentAccountLogin, ignoreCase = true) },
                        targetName = state.targetName,
                        defaultBranchOnly = defaultBranchOnly
                    )
                    state.description.trim().takeIf { it.isNotBlank() && it != fork.description.orEmpty() }?.let { description ->
                        gateway.updateRepositoryDescription(
                            owner = fork.ownerLogin,
                            repo = fork.name,
                            description = description
                        )
                    } ?: fork
                }
            }
            val latest = _forkState.value as? RepositoryForkUiState.Content ?: return@launch
            _forkState.value = result.fold(
                onSuccess = { fork ->
                    latest.copy(
                        existingFork = fork,
                        createdFork = fork,
                        isCreating = false,
                        errorMessage = null
                    )
                },
                onFailure = { exception ->
                    latest.copy(
                        isCreating = false,
                        errorMessage = exception.message
                    )
                }
            )
        }
    }

    fun consumeCreatedFork(): GitHubRepository? {
        val state = _forkState.value as? RepositoryForkUiState.Content ?: return null
        val created = state.createdFork ?: return null
        _forkState.value = state.copy(createdFork = null)
        return created
    }

    private fun scheduleNameAvailabilityCheck(targetOwner: String, targetName: String) {
        nameCheckJob?.cancel()
        if (targetOwner.isBlank() || targetName.isBlank() || accessToken.isBlank()) return
        val state = _forkState.value as? RepositoryForkUiState.Content ?: return
        _forkState.value = state.copy(isCheckingName = true, nameCheckError = null)
        nameCheckJob = viewModelScope.launch {
            delay(NameCheckDebounceMillis)
            val result = withContext(Dispatchers.IO) {
                runCatching { GitHubRepositoryApiGateway(accessToken).isRepositoryNameAvailable(targetOwner, targetName) }
            }
            val latest = _forkState.value as? RepositoryForkUiState.Content ?: return@launch
            if (latest.targetOwner != targetOwner || latest.targetName != targetName) return@launch
            _forkState.value = latest.copy(
                isNameAvailable = result.getOrNull(),
                isCheckingName = false,
                nameCheckError = result.exceptionOrNull()?.message
            )
        }
    }

    private suspend fun checkExistingFork(
        gateway: GitHubRepositoryApiGateway,
        owner: String,
        repo: String,
        login: String
    ) {
        val result = withContext(Dispatchers.IO) {
            runCatching { gateway.findCurrentAccountFork(owner, repo, login) }
        }
        val latest = _forkState.value as? RepositoryForkUiState.Content ?: return
        _forkState.value = latest.copy(
            existingFork = result.getOrNull(),
            isCheckingExistingFork = false,
            errorMessage = result.exceptionOrNull()?.message
        )
    }

    private fun GitHubRepository.forkEligibilityError(currentAccountLogin: String): String? {
        if (ownerLogin.equals(currentAccountLogin, ignoreCase = true)) {
            return getApplication<Application>().getString(com.Sunset.REN.GitHub.R.string.repository_fork_ineligible_self)
        }
        if (fork && sourceFullName?.equals(fullName, ignoreCase = true) == false) {
            return getApplication<Application>().getString(com.Sunset.REN.GitHub.R.string.repository_fork_ineligible_fork)
        }
        return null
    }

    companion object {
        private const val NameCheckDebounceMillis = 450L
        private const val DescriptionMaxLength = 350
    }
}