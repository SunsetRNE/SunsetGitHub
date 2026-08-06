package com.Sunset.REN.GitHub.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.auth.AuthSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LoginHomeUiState(
    val message: String = "选择一种方式继续使用 SunsetGitHub。",
    val shouldEnterHome: Boolean = false
)

class LoginHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val authSessionRepository = AuthSessionRepository(application)

    private val _state = MutableLiveData(LoginHomeUiState())
    val state: LiveData<LoginHomeUiState> = _state

    fun refresh(autoEnterCurrent: Boolean) {
        viewModelScope.launch {
            val hasCurrentSession = withContext(Dispatchers.IO) { authSessionRepository.hasCurrentSession() }
            _state.value = if (autoEnterCurrent && hasCurrentSession) {
                LoginHomeUiState(message = "正在进入首页……", shouldEnterHome = true)
            } else {
                LoginHomeUiState()
            }
        }
    }
}