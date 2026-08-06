package com.Sunset.REN.GitHub.ui.compose.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.Sunset.REN.GitHub.R

@Composable
fun TokenPermissionRiskDialog(
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.auth_token_review_risk_title)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.auth_token_review_confirm_anyway))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
fun TokenRegenerateOptionsDialog(
    onDismiss: () -> Unit,
    onOptionSelected: (Int) -> Unit
) {
    val options = listOf(
        stringResource(R.string.auth_token_review_regenerate_classic),
        stringResource(R.string.auth_token_review_regenerate_fine_grained)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.auth_token_review_regenerate)) },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOptionSelected(index) }
                    ) {
                        Text(option)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
