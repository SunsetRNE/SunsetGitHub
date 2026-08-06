package com.Sunset.REN.GitHub.ui.compose.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.auth.TokenPermissionStatus
import com.Sunset.REN.GitHub.ui.auth.TokenPermissionCheckUiModel
import com.Sunset.REN.GitHub.ui.auth.TokenPermissionReviewUiState
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton

@Composable
fun TokenLoginChoiceScreen(
    onHaveTokenClick: () -> Unit,
    onNeedTokenGuideClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthPageSurface(modifier = modifier) {
        AuthTitle(
            title = stringResource(R.string.login_home_token_title),
            subtitle = stringResource(R.string.token_login_choice_subtitle)
        )
        Spacer(modifier = Modifier.height(28.dp))
        AuthActionCard(
            title = stringResource(R.string.token_login_have_title),
            description = stringResource(R.string.token_login_have_desc),
            primaryActionText = stringResource(R.string.token_login_have_title),
            onPrimaryActionClick = onHaveTokenClick
        )
        Spacer(modifier = Modifier.height(16.dp))
        AuthActionCard(
            title = stringResource(R.string.token_login_need_title),
            description = stringResource(R.string.token_login_need_desc),
            primaryActionText = stringResource(R.string.token_login_guide_action),
            onPrimaryActionClick = onNeedTokenGuideClick,
            primary = false
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            text = stringResource(R.string.token_login_security_note),
            color = SunsetGitHubThemeTokens.colors.textMuted,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TokenGuideScreen(
    onOpenBrowserClick: () -> Unit,
    onTokenAcquiredClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthPageSurface(modifier = modifier, verticalPadding = 24.dp) {
        AuthTitle(
            title = stringResource(R.string.token_guide_title),
            subtitle = stringResource(R.string.token_guide_subtitle)
        )
        AuthStepCard(
            number = "1",
            title = stringResource(R.string.token_guide_step_open_title),
            description = stringResource(R.string.token_guide_step_open_description)
        )
        AuthStepCard(
            number = "2",
            title = stringResource(R.string.token_guide_step_permissions_title),
            description = stringResource(R.string.token_guide_step_permissions_description)
        )
        AuthStepCard(
            number = "3",
            title = stringResource(R.string.token_guide_step_copy_title),
            description = stringResource(R.string.token_guide_step_copy_description)
        )
        InfoCard(
            title = stringResource(R.string.token_guide_permissions_title),
            description = stringResource(R.string.token_guide_permissions_desc)
        )
        InfoCard(
            title = stringResource(R.string.token_guide_security_title),
            description = stringResource(R.string.token_guide_security_desc)
        )
        SunsetPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            text = stringResource(R.string.token_guide_open_browser),
            onClick = onOpenBrowserClick
        )
        SunsetSecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            text = stringResource(R.string.token_guide_acquired),
            onClick = onTokenAcquiredClick
        )
    }
}

@Composable
fun TokenPermissionReviewScreen(
    state: TokenPermissionReviewUiState,
    tokenInput: String,
    onTokenInputChange: (String) -> Unit,
    onRecheckClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onCancelClick: () -> Unit,
    onRegenerateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val statusText = when {
        state.isLoading -> stringResource(R.string.auth_token_review_loading)
        state.isSaving -> stringResource(R.string.auth_token_review_saving)
        state.account != null -> stringResource(R.string.auth_token_review_ready)
        else -> stringResource(R.string.auth_token_review_waiting)
    }
    val accountText = state.account?.let { account ->
        account.name?.takeIf { it.isNotBlank() }
            ?.let { name -> "${account.login} · $name" }
            ?: account.login
    } ?: stringResource(R.string.auth_token_review_account_unknown)
    val scopesText = if (state.scopes.isEmpty()) {
        stringResource(R.string.auth_token_review_scopes_empty)
    } else {
        stringResource(R.string.auth_token_review_scopes, state.scopes.joinToString())
    }

    AuthPageSurface(modifier = modifier, verticalPadding = 24.dp) {
        AuthTitle(
            title = stringResource(R.string.auth_token_review_page_title),
            subtitle = stringResource(R.string.auth_token_review_page_subtitle)
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            value = tokenInput,
            onValueChange = onTokenInputChange,
            enabled = !state.isLoading && !state.isSaving,
            minLines = 4,
            label = { Text(stringResource(R.string.auth_token_input_hint)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedLabelColor = colors.accent,
                unfocusedLabelColor = colors.textSecondary,
                cursorColor = colors.accent
            )
        )
        SunsetPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            text = stringResource(R.string.auth_token_review_recheck_action),
            enabled = tokenInput.isNotBlank() && !state.isLoading && !state.isSaving,
            onClick = onRecheckClick
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            text = statusText,
            color = colors.textMuted,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
        InfoCard(
            title = stringResource(R.string.auth_token_review_account_title),
            description = "$accountText\n$scopesText"
        )
        TokenPermissionChecksList(checks = state.checks)
        if (!state.errorMessage.isNullOrBlank()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                text = state.errorMessage.orEmpty(),
                color = colors.danger,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        SunsetPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            text = stringResource(R.string.auth_token_review_confirm_home),
            enabled = state.account != null && !state.isLoading && !state.isSaving,
            onClick = onConfirmClick
        )
        SunsetSecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            text = stringResource(R.string.auth_token_review_cancel),
            enabled = !state.isSaving,
            onClick = onCancelClick
        )
        SunsetSecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            text = stringResource(R.string.auth_token_review_open_token_page),
            enabled = !state.isSaving,
            onClick = onRegenerateClick
        )
    }
}

@Composable
private fun TokenPermissionChecksList(checks: List<TokenPermissionCheckUiModel>) {
    if (checks.isEmpty()) {
        InfoCard(
            title = stringResource(R.string.auth_token_review_check_title),
            description = stringResource(R.string.auth_token_review_checks_empty)
        )
        return
    }
    checks.forEach { check ->
        TokenPermissionCheckCard(check = check)
    }
}

@Composable
private fun TokenPermissionCheckCard(check: TokenPermissionCheckUiModel) {
    val colors = SunsetGitHubThemeTokens.colors
    val statusColor = when (check.status) {
        TokenPermissionStatus.Granted -> colors.success
        TokenPermissionStatus.Missing -> colors.danger
        TokenPermissionStatus.Unknown -> colors.textMuted
    }
    val backgroundColor = when (check.status) {
        TokenPermissionStatus.Granted -> colors.successSoft
        TokenPermissionStatus.Missing -> colors.dangerSoft
        TokenPermissionStatus.Unknown -> colors.subtleBackground
    }
    val mark = when (check.status) {
        TokenPermissionStatus.Granted -> "✓"
        TokenPermissionStatus.Missing -> "!"
        TokenPermissionStatus.Unknown -> "?"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.border),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = "$mark ${check.title}",
                color = statusColor,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = check.description,
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = check.detail,
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}