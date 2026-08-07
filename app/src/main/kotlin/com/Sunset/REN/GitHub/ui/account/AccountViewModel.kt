package com.Sunset.REN.GitHub.ui.account

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.auth.AuthSessionRepository
import com.Sunset.REN.GitHub.domain.auth.GitHubAccount
import com.Sunset.REN.GitHub.domain.auth.RememberedAccount
import com.Sunset.REN.GitHub.ui.auth.AuthUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val authSessionRepository = AuthSessionRepository(application)

    private val _authState = MutableLiveData<AuthUiState>(AuthUiState.Loading)
    val authState: LiveData<AuthUiState> = _authState

    private val _rememberedAccounts = MutableLiveData<List<RememberedAccount>>(emptyList())
    val rememberedAccounts: LiveData<List<RememberedAccount>> = _rememberedAccounts
    private val _signOutHint = MutableLiveData<String?>(null)
    val signOutHint: LiveData<String?> = _signOutHint
    private var currentAccount: GitHubAccount? = null

    init {
        refreshAccountState()
    }

    fun refreshAccountState() {
        viewModelScope.launch {
            val current = withContext(Dispatchers.IO) { authSessionRepository.getCurrentAccount() }
            val remembered = withContext(Dispatchers.IO) { authSessionRepository.listRememberedAccounts() }
            currentAccount = current
            _authState.value = if (current != null) {
                AuthUiState.Authorized(current.login)
            } else {
                AuthUiState.SignedOut
            }
            _rememberedAccounts.value = remembered
        }
    }

    fun switchAccount(account: GitHubAccount) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { authSessionRepository.switchAccount(account) }
            refreshAccountState()
        }
    }

    fun removeAccount(account: GitHubAccount) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { authSessionRepository.removeAccount(account) }
            refreshAccountState()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { authSessionRepository.signOutCurrent() }
            refreshAccountState()
            _signOutHint.value = "已退出登录（本地凭据仍加密保留，切换账号可免登录返回）。如需彻底撤销 GitHub 授权，请到 GitHub → Settings → Applications 撤销本应用，再在账号页移除该账号。"
        }
    }

    fun consumeSignOutHint() {
        _signOutHint.value = null
    }

    fun isCurrentAccount(account: GitHubAccount): Boolean = account.id == currentAccount?.id
}