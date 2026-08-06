package com.Sunset.REN.GitHub.ui.compose.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.auth.device.DeviceFlowUiState
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton

@Composable
fun DeviceFlowIntroScreen(
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthPageSurface(modifier = modifier, verticalPadding = 24.dp) {
        AuthTitle(
            title = stringResource(R.string.device_flow_intro_title),
            subtitle = stringResource(R.string.device_flow_intro_subtitle)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(top = 18.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SunsetGitHubThemeTokens.colors.border),
            colors = CardDefaults.cardColors(containerColor = SunsetGitHubThemeTokens.colors.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AuthLogo(size = 52)
                Text(
                    modifier = Modifier.padding(top = 10.dp),
                    text = stringResource(R.string.device_flow_intro_browser_label),
                    color = SunsetGitHubThemeTokens.colors.textSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
        AuthStepCard(
            number = "1",
            title = stringResource(R.string.auth_step_generate_code_title),
            description = stringResource(R.string.auth_step_generate_code_desc)
        )
        AuthStepCard(
            number = "2",
            title = stringResource(R.string.auth_step_open_browser_title),
            description = stringResource(R.string.auth_step_open_browser_desc)
        )
        AuthStepCard(
            number = "3",
            title = stringResource(R.string.auth_step_finish_authorization_title),
            description = stringResource(R.string.auth_step_finish_authorization_desc)
        )
        Spacer(modifier = Modifier.height(56.dp))
        SunsetPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.device_flow_intro_next),
            onClick = onNextClick
        )
    }
}

@Composable
fun DeviceFlowCodeScreen(
    state: DeviceFlowUiState,
    onCopyOrRetryClick: () -> Unit,
    onOpenBrowserClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val codeText = when (state) {
        DeviceFlowUiState.RequestingCode -> stringResource(R.string.device_flow_code_preparing)
        is DeviceFlowUiState.CodeReady -> state.userCode
        is DeviceFlowUiState.Error -> stringResource(R.string.device_flow_code_generation_failed)
        is DeviceFlowUiState.SignedIn -> state.account.login
        DeviceFlowUiState.Cancelled -> stringResource(R.string.device_flow_code_cancelled)
    }
    val statusText = when (state) {
        DeviceFlowUiState.RequestingCode -> stringResource(R.string.device_flow_requesting_status)
        is DeviceFlowUiState.CodeReady -> stringResource(R.string.device_flow_waiting_detail)
        is DeviceFlowUiState.Error -> stringResource(R.string.device_flow_error_status)
        is DeviceFlowUiState.SignedIn -> stringResource(R.string.device_flow_signed_in_status)
        DeviceFlowUiState.Cancelled -> stringResource(R.string.device_flow_status_cancelled)
    }
    val detailText = when (state) {
        DeviceFlowUiState.RequestingCode -> stringResource(R.string.device_flow_detail_requesting)
        is DeviceFlowUiState.CodeReady -> state.message
        is DeviceFlowUiState.Error -> stringResource(
            R.string.device_flow_detail_error,
            state.message.ifBlank { stringResource(R.string.device_flow_fallback_network_error) }
        )
        is DeviceFlowUiState.SignedIn -> stringResource(R.string.device_flow_detail_signed_in, state.account.login)
        DeviceFlowUiState.Cancelled -> stringResource(R.string.device_flow_detail_cancelled)
    }
    val canCopyOrRetry = state is DeviceFlowUiState.CodeReady || state is DeviceFlowUiState.Error
    val canOpenBrowser = state is DeviceFlowUiState.CodeReady
    val copyOrRetryText = if (state is DeviceFlowUiState.Error) {
        stringResource(R.string.device_flow_retry_generate)
    } else {
        stringResource(R.string.device_flow_copy_code)
    }

    AuthPageSurface(modifier = modifier, verticalPadding = 24.dp) {
        AuthTitle(
            title = stringResource(R.string.device_flow_code_title),
            subtitle = statusText
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, colors.border),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.device_flow_code_label),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    text = codeText,
                    color = colors.textPrimary,
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SunsetSecondaryButton(
                modifier = Modifier.weight(1f),
                text = copyOrRetryText,
                enabled = canCopyOrRetry,
                onClick = onCopyOrRetryClick
            )
            SunsetPrimaryButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.device_flow_open_browser),
                enabled = canOpenBrowser,
                onClick = onOpenBrowserClick
            )
        }
        InfoCard(
            title = stringResource(R.string.device_flow_waiting_title),
            description = detailText
        )
        Spacer(modifier = Modifier.height(56.dp))
        SunsetSecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.auth_cancel),
            onClick = onCancelClick
        )
    }
}