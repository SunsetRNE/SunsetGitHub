package com.Sunset.REN.GitHub.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.auth.AuthSessionRepository
import com.Sunset.REN.GitHub.data.auth.TokenAuthRepository
import com.Sunset.REN.GitHub.domain.auth.RememberedAccountLoginType
import com.Sunset.REN.GitHub.domain.auth.TokenPermissionCapability
import com.Sunset.REN.GitHub.domain.auth.TokenPermissionCheck
import com.Sunset.REN.GitHub.domain.auth.TokenPermissionDetail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TokenPermissionReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val authSessionRepository = AuthSessionRepository(application)
    private val tokenAuthRepository = TokenAuthRepository()

    private val _reviewState = MutableLiveData(TokenPermissionReviewUiState())
    val reviewState: LiveData<TokenPermissionReviewUiState> = _reviewState

    /** 输入框实时回调：仅更新状态，不触发网络检查（避免每敲一个字符发请求）。 */
    fun updateToken(token: String) {
        val current = _reviewState.value
        if (current?.token == token) return
        _reviewState.value = (current ?: TokenPermissionReviewUiState()).copy(token = token)
    }

    fun prepare(token: String) {
        val accessToken = token.trim()
        if (accessToken.isBlank()) {
            _reviewState.value = TokenPermissionReviewUiState(
                token = accessToken,
                errorMessage = getString(R.string.auth_error_token_empty)
            )
            return
        }
        val current = _reviewState.value
        if (current?.token == accessToken && (current.account != null || current.isLoading)) return
        inspectToken(accessToken)
    }

    fun inspectToken(token: String = _reviewState.value?.token.orEmpty()) {
        val accessToken = token.trim()
        if (accessToken.isBlank()) {
            _reviewState.value = TokenPermissionReviewUiState(
                token = accessToken,
                errorMessage = getString(R.string.auth_error_token_empty)
            )
            return
        }
        viewModelScope.launch {
            _reviewState.value = TokenPermissionReviewUiState(token = accessToken, isLoading = true)
            try {
                val inspection = withContext(Dispatchers.IO) {
                    tokenAuthRepository.inspectToken(accessToken)
                }
                _reviewState.value = TokenPermissionReviewUiState(
                    token = accessToken,
                    account = inspection.account,
                    scopes = inspection.scopes,
                    checks = inspection.checks.map { it.toUiModel() },
                    isLoading = false
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _reviewState.value = TokenPermissionReviewUiState(
                    token = accessToken,
                    isLoading = false,
                    errorMessage = exception.message ?: getString(R.string.auth_error_token_invalid)
                )
            }
        }
    }

    fun confirmLogin() {
        val state = _reviewState.value ?: return
        val account = state.account ?: return
        val token = state.token.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch {
            _reviewState.value = state.copy(isSaving = true, errorMessage = null)
            try {
                withContext(Dispatchers.IO) {
                    authSessionRepository.saveSignedInAccount(
                        account,
                        token,
                        RememberedAccountLoginType.AccessToken
                    )
                }
                _reviewState.value = state.copy(isSaving = false, signedInLogin = account.login)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _reviewState.value = state.copy(
                    isSaving = false,
                    errorMessage = exception.message ?: getString(R.string.auth_error_flow_failed)
                )
            }
        }
    }

    private fun TokenPermissionCheck.toUiModel(): TokenPermissionCheckUiModel {
        return TokenPermissionCheckUiModel(
            title = getString(capability.titleResId),
            description = getString(capability.descriptionResId),
            status = status,
            detail = getString(detail.detailResId),
            isCritical = isCritical
        )
    }

    private val TokenPermissionCapability.titleResId: Int
        get() = when (this) {
            TokenPermissionCapability.Repository -> R.string.auth_token_scope_repo_title
            TokenPermissionCapability.Workflow -> R.string.auth_token_scope_workflow_title
            TokenPermissionCapability.Issues -> R.string.auth_token_scope_issues_title
            TokenPermissionCapability.Notifications -> R.string.auth_token_scope_notifications_title
            TokenPermissionCapability.UserProfile -> R.string.auth_token_scope_user_title
        }

    private val TokenPermissionCapability.descriptionResId: Int
        get() = when (this) {
            TokenPermissionCapability.Repository -> R.string.auth_token_scope_repo_desc
            TokenPermissionCapability.Workflow -> R.string.auth_token_scope_workflow_desc
            TokenPermissionCapability.Issues -> R.string.auth_token_scope_issues_desc
            TokenPermissionCapability.Notifications -> R.string.auth_token_scope_notifications_desc
            TokenPermissionCapability.UserProfile -> R.string.auth_token_scope_user_desc
        }

    private val TokenPermissionDetail.detailResId: Int
        get() = when (this) {
            TokenPermissionDetail.Granted -> R.string.auth_token_scope_granted
            TokenPermissionDetail.Unknown -> R.string.auth_token_scope_unknown
            TokenPermissionDetail.RepositoryMissing -> R.string.auth_token_scope_repo_missing
            TokenPermissionDetail.WorkflowMissing -> R.string.auth_token_scope_workflow_missing
            TokenPermissionDetail.IssuesMissing -> R.string.auth_token_scope_issues_missing
            TokenPermissionDetail.NotificationsMissing -> R.string.auth_token_scope_notifications_missing
            TokenPermissionDetail.UserProfileMissing -> R.string.auth_token_scope_user_missing
        }

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)
}