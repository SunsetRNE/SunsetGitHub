package com.Sunset.REN.GitHub.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.auth.GitHubAccount
import com.Sunset.REN.GitHub.domain.auth.RememberedAccount
import com.Sunset.REN.GitHub.domain.auth.RememberedAccountLoginType
import com.Sunset.REN.GitHub.ui.auth.AuthUiState
import com.Sunset.REN.GitHub.ui.auth.LoginHomeFragment
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.bumptech.glide.Glide

class AccountFragment : Fragment() {

    private lateinit var viewModel: AccountViewModel
    private var authState by mutableStateOf<AuthUiState>(AuthUiState.Loading)
    private var rememberedAccounts by mutableStateOf<List<RememberedAccount>>(emptyList())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[AccountViewModel::class.java]
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    AccountScreen(
                        authState = authState,
                        rememberedAccounts = rememberedAccounts,
                        isCurrentAccount = viewModel::isCurrentAccount,
                        onAddAccount = ::openAddAccount,
                        onSignOut = viewModel::signOut,
                        onSwitchAccount = viewModel::switchAccount,
                        onRemoveAccount = viewModel::removeAccount
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            authState = state
            if (state != AuthUiState.Loading) {
                (activity as? AuthStateListener)?.onAuthStateChanged(state is AuthUiState.Authorized)
            }
        }
        viewModel.rememberedAccounts.observe(viewLifecycleOwner) { accounts -> rememberedAccounts = accounts }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) viewModel.refreshAccountState()
    }

    private fun openAddAccount() {
        findNavController().navigate(
            R.id.navigation_login,
            bundleOf(LoginHomeFragment.ARG_ADD_ACCOUNT_MODE to true)
        )
    }

    interface AuthStateListener {
        fun onAuthStateChanged(isAuthorized: Boolean)
    }
}

@Composable
private fun AccountScreen(
    authState: AuthUiState,
    rememberedAccounts: List<RememberedAccount>,
    isCurrentAccount: (GitHubAccount) -> Boolean,
    onAddAccount: () -> Unit,
    onSignOut: () -> Unit,
    onSwitchAccount: (GitHubAccount) -> Unit,
    onRemoveAccount: (GitHubAccount) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    var accountPendingRemoval by remember { mutableStateOf<GitHubAccount?>(null) }
    val currentLogin = (authState as? AuthUiState.Authorized)?.login
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.account_title_github),
            modifier = Modifier.fillMaxWidth(),
            color = colors.textPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = when (authState) {
                AuthUiState.Loading -> stringResource(R.string.home_account_state_refreshing)
                else -> stringResource(R.string.account_manage_credentials_description)
            },
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        AccountCurrentCard(currentLogin = currentLogin, isLoading = authState == AuthUiState.Loading, onSignOut = onSignOut)
        AccountGroupCard(
            title = stringResource(R.string.account_group_device_flow),
            accounts = rememberedAccounts.filter { it.loginType == RememberedAccountLoginType.DeviceFlow },
            loginType = RememberedAccountLoginType.DeviceFlow,
            isCurrentAccount = isCurrentAccount,
            onSwitchAccount = onSwitchAccount,
            onRemoveAccount = { accountPendingRemoval = it }
        )
        AccountGroupCard(
            title = stringResource(R.string.account_group_access_token),
            accounts = rememberedAccounts.filter { it.loginType == RememberedAccountLoginType.AccessToken },
            loginType = RememberedAccountLoginType.AccessToken,
            isCurrentAccount = isCurrentAccount,
            onSwitchAccount = onSwitchAccount,
            onRemoveAccount = { accountPendingRemoval = it }
        )
        Button(onClick = onAddAccount, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.account_add_new))
        }
    }

    accountPendingRemoval?.let { account ->
        AlertDialog(
            onDismissRequest = { accountPendingRemoval = null },
            title = { Text(stringResource(R.string.auth_remove_account)) },
            text = { Text(stringResource(R.string.auth_remove_account_message, account.login)) },
            confirmButton = {
                Button(onClick = {
                    onRemoveAccount(account)
                    accountPendingRemoval = null
                }) { Text(stringResource(R.string.auth_remove_account)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { accountPendingRemoval = null }) { Text(stringResource(android.R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun AccountCurrentCard(currentLogin: String?, isLoading: Boolean, onSignOut: () -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.auth_current_account_title), color = colors.textPrimary, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(url = null)
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(
                        text = when {
                            isLoading -> stringResource(R.string.home_account_state_refreshing)
                            currentLogin != null -> currentLogin
                            else -> stringResource(R.string.account_not_signed_in)
                        },
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (currentLogin != null) stringResource(R.string.account_current_active) else stringResource(R.string.account_signed_out_meta),
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            if (currentLogin != null) {
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.account_sign_out_current))
                }
            }
        }
    }
}

@Composable
private fun AccountGroupCard(
    title: String,
    accounts: List<RememberedAccount>,
    loginType: RememberedAccountLoginType,
    isCurrentAccount: (GitHubAccount) -> Boolean,
    onSwitchAccount: (GitHubAccount) -> Unit,
    onRemoveAccount: (GitHubAccount) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = colors.textPrimary, fontWeight = FontWeight.Bold)
            if (accounts.isEmpty()) {
                Text(stringResource(R.string.account_group_empty_count), color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            } else {
                Text(stringResource(R.string.account_group_saved_count, accounts.size), color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                accounts.forEach { remembered ->
                    AccountRow(
                        account = remembered.account,
                        loginType = loginType,
                        isCurrent = isCurrentAccount(remembered.account),
                        onSwitchAccount = onSwitchAccount,
                        onRemoveAccount = onRemoveAccount
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: GitHubAccount,
    loginType: RememberedAccountLoginType,
    isCurrent: Boolean,
    onSwitchAccount: (GitHubAccount) -> Unit,
    onRemoveAccount: (GitHubAccount) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(account.avatarUrl)
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(account.login, color = colors.textPrimary, fontWeight = FontWeight.Bold)
            Text(account.name?.takeIf { it.isNotBlank() } ?: "@${account.login}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            Text(
                text = when (loginType) {
                    RememberedAccountLoginType.DeviceFlow -> stringResource(R.string.account_row_login_type_device_flow)
                    RememberedAccountLoginType.AccessToken -> stringResource(R.string.account_row_login_type_access_token)
                },
                color = colors.textMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(if (isCurrent) R.string.account_row_current_status else R.string.account_row_saved_status),
                color = if (isCurrent) colors.accent else colors.textSecondary,
                style = MaterialTheme.typography.labelSmall
            )
            OutlinedButton(onClick = { onSwitchAccount(account) }, enabled = !isCurrent) {
                Text(stringResource(if (isCurrent) R.string.account_row_current_action else R.string.auth_switch_account))
            }
            Text(
                text = stringResource(R.string.auth_remove_account),
                color = colors.danger,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable { onRemoveAccount(account) }
            )
        }
    }
}

@Composable
private fun Avatar(url: String?) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        },
        update = { imageView ->
            Glide.with(imageView)
                .load(url)
                .placeholder(R.drawable.ic_people_24)
                .error(R.drawable.ic_people_24)
                .circleCrop()
                .into(imageView)
        },
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
    )
}
