package com.Sunset.REN.GitHub.ui.auth

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.auth.TokenPermissionStatus
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.auth.TokenPermissionReviewScreen
import com.Sunset.REN.GitHub.ui.compose.screens.auth.TokenPermissionRiskDialog
import com.Sunset.REN.GitHub.ui.compose.screens.auth.TokenRegenerateOptionsDialog

class TokenPermissionReviewFragment : Fragment() {

    private lateinit var viewModel: TokenPermissionReviewViewModel
    private var uiState by mutableStateOf(TokenPermissionReviewUiState())
    private var tokenInput by mutableStateOf("")
    private var dialogState by mutableStateOf<TokenPermissionReviewDialogState?>(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[TokenPermissionReviewViewModel::class.java]
        tokenInput = arguments?.getString(ARG_TOKEN).orEmpty()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    TokenPermissionReviewScreen(
                        state = uiState,
                        tokenInput = tokenInput,
                        onTokenInputChange = { tokenInput = it },
                        onRecheckClick = { viewModel.inspectToken(tokenInput) },
                        onConfirmClick = ::confirmLoginWithRiskCheck,
                        onCancelClick = { findNavController().popBackStack() },
                        onRegenerateClick = ::showRegenerateTokenOptions
                    )
                    TokenPermissionReviewDialogHost(
                        dialogState = dialogState,
                        onDismiss = { dialogState = null },
                        onConfirmRisk = {
                            dialogState = null
                            viewModel.confirmLogin()
                        },
                        onRegenerateOptionSelected = { index ->
                            dialogState = null
                            val url = if (index == 0) {
                                GITHUB_CLASSIC_TOKEN_SETTINGS_URL
                            } else {
                                GITHUB_FINE_GRAINED_TOKEN_SETTINGS_URL
                            }
                            openTokenSettingsPage(url)
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val token = arguments?.getString(ARG_TOKEN).orEmpty()
        viewModel.reviewState.observe(viewLifecycleOwner) { state -> renderState(state) }
        viewModel.prepare(token)
    }

    private fun renderState(state: TokenPermissionReviewUiState) {
        uiState = state
        if (tokenInput != state.token && !state.isLoading && state.token.isNotBlank()) {
            tokenInput = state.token
        }
        state.signedInLogin?.let { login ->
            Toast.makeText(requireContext(), getString(R.string.auth_token_login_success, login), Toast.LENGTH_SHORT).show()
            findNavController().navigate(
                R.id.navigation_home,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.navigation_login, true)
                    .build()
            )
        }
    }

    private fun confirmLoginWithRiskCheck() {
        val state = viewModel.reviewState.value ?: return
        val criticalMissing = state.checks.filter { check ->
            check.status == TokenPermissionStatus.Missing && check.isCritical
        }
        if (criticalMissing.isEmpty()) {
            viewModel.confirmLogin()
            return
        }
        dialogState = TokenPermissionReviewDialogState.Risk(
            message = getString(
                R.string.auth_token_review_risk_message,
                criticalMissing.joinToString(separator = "、") { it.title }
            )
        )
    }

    private fun showRegenerateTokenOptions() {
        dialogState = TokenPermissionReviewDialogState.RegenerateOptions
    }

    private fun openTokenSettingsPage(url: String) {
        val uri = Uri.parse(url)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (exception: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.auth_verification_no_browser, Toast.LENGTH_SHORT).show()
        } catch (exception: Exception) {
            Toast.makeText(requireContext(), R.string.auth_verification_open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ARG_TOKEN = "token"
        private const val GITHUB_CLASSIC_TOKEN_SETTINGS_URL = "https://github.com/settings/tokens"
        private const val GITHUB_FINE_GRAINED_TOKEN_SETTINGS_URL = "https://github.com/settings/personal-access-tokens/new"
    }
}

@androidx.compose.runtime.Composable
private fun TokenPermissionReviewDialogHost(
    dialogState: TokenPermissionReviewDialogState?,
    onDismiss: () -> Unit,
    onConfirmRisk: () -> Unit,
    onRegenerateOptionSelected: (Int) -> Unit
) {
    when (dialogState) {
        is TokenPermissionReviewDialogState.Risk -> TokenPermissionRiskDialog(
            message = dialogState.message,
            onDismiss = onDismiss,
            onConfirm = onConfirmRisk
        )
        TokenPermissionReviewDialogState.RegenerateOptions -> TokenRegenerateOptionsDialog(
            onDismiss = onDismiss,
            onOptionSelected = onRegenerateOptionSelected
        )
        null -> Unit
    }
}

private sealed class TokenPermissionReviewDialogState {
    data class Risk(val message: String) : TokenPermissionReviewDialogState()
    data object RegenerateOptions : TokenPermissionReviewDialogState()
}