package com.Sunset.REN.GitHub.ui.auth.device

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.core.network.GitHubOAuthConfig
import com.Sunset.REN.GitHub.data.auth.GitHubDeviceFlowRepository
import com.Sunset.REN.GitHub.data.auth.AuthSessionRepository
import com.Sunset.REN.GitHub.domain.auth.DeviceFlowRepository
import com.Sunset.REN.GitHub.domain.auth.DeviceTokenPollResult
import com.Sunset.REN.GitHub.domain.auth.RememberedAccountLoginType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class DeviceFlowViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DeviceFlowRepository? = GitHubOAuthConfig.ClientId.takeIf { it.isNotBlank() }?.let(::GitHubDeviceFlowRepository)
    private val authSessionRepository = AuthSessionRepository(application)
    private var job: Job? = null

    private val _state = MutableLiveData<DeviceFlowUiState>()
    val state: LiveData<DeviceFlowUiState> = _state

    fun start() {
        val repo = repository
        if (repo == null) {
            _state.value = DeviceFlowUiState.Error(getString(R.string.auth_error_missing_client_id_device_flow))
            return
        }
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            try {
                _state.value = DeviceFlowUiState.RequestingCode
                val grant = withContext(Dispatchers.IO) { repo.requestDeviceCode() }
                val expiresAt = System.currentTimeMillis() + grant.expiresInSeconds * 1_000L
                var nextDelaySeconds = grant.intervalSeconds
                _state.value = DeviceFlowUiState.CodeReady(
                    userCode = grant.userCode,
                    verificationUri = grant.verificationUri,
                    verificationUriComplete = grant.verificationUriComplete,
                    expiresAtMillis = expiresAt,
                    message = "浏览器打开后，请在 GitHub 页面完成登录和授权。"
                )
                while (System.currentTimeMillis() < expiresAt) {
                    val remainingMillis = expiresAt - System.currentTimeMillis()
                    delay((nextDelaySeconds * 1_000L).coerceAtMost(remainingMillis))
                    when (val result = withContext(Dispatchers.IO) { repo.pollAccessToken(grant.deviceCode) }) {
                        DeviceTokenPollResult.AuthorizationPending -> Unit
                        DeviceTokenPollResult.SlowDown -> nextDelaySeconds += 5L
                        DeviceTokenPollResult.ExpiredToken -> {
                            _state.value = DeviceFlowUiState.Error(getString(R.string.auth_expired_state)); return@launch
                        }
                        DeviceTokenPollResult.AccessDenied -> {
                            _state.value = DeviceFlowUiState.Error(getString(R.string.auth_denied_state)); return@launch
                        }
                        is DeviceTokenPollResult.Success -> {
                            val account = withContext(Dispatchers.IO) { repo.fetchCurrentAccount(result.accessToken) }
                            withContext(Dispatchers.IO) {
                                authSessionRepository.saveSignedInAccount(
                                    account,
                                    result.accessToken,
                                    RememberedAccountLoginType.DeviceFlow
                                )
                            }
                            _state.value = DeviceFlowUiState.SignedIn(account)
                            return@launch
                        }
                        is DeviceTokenPollResult.NetworkError -> {
                            _state.value = DeviceFlowUiState.Error(result.message ?: getString(R.string.auth_error_network_request_failed)); return@launch
                        }
                        is DeviceTokenPollResult.UnknownError -> {
                            _state.value = DeviceFlowUiState.Error(result.description ?: result.code ?: getString(R.string.auth_error_unknown_auth)); return@launch
                        }
                    }
                }
                _state.value = DeviceFlowUiState.Error(getString(R.string.auth_expired_state))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IOException) {
                _state.value = DeviceFlowUiState.Error(toNetworkErrorMessage(exception))
            } catch (exception: Exception) {
                _state.value = DeviceFlowUiState.Error(exception.message ?: getString(R.string.auth_error_flow_failed))
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = DeviceFlowUiState.Cancelled
    }

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)

    private fun toNetworkErrorMessage(error: IOException): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("Connection reset", ignoreCase = true) -> "网络连接被重置，请检查网络、VPN 或代理后重试。"
            message.contains("Failed to connect", ignoreCase = true) -> "无法连接 GitHub，请检查网络、VPN 或代理后重试。"
            message.contains("timed out", ignoreCase = true) -> "连接 GitHub 超时，请稍后重试。"
            message.isNotBlank() -> "网络请求失败：$message"
            else -> getString(R.string.auth_error_network_request_failed)
        }
    }

    override fun onCleared() {
        job?.cancel()
        super.onCleared()
    }
}