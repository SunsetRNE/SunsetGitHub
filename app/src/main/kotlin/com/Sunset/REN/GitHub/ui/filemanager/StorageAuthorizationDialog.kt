package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.common.showComposeDialog
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

/** Dialogs for SAF/storage authorization entry points. */
object StorageAuthorizationDialog {
    fun showAuthorizeDirectory(
        context: Context,
        onAuthorize: () -> Unit
    ) {
        showComposeDialog(context) { dismiss ->
            StorageAuthorizationContent(
                title = context.getString(R.string.local_file_manager_authorize_directory),
                message = context.getString(R.string.local_file_manager_authorize_directory_message),
                negativeText = context.getString(android.R.string.cancel),
                neutralText = null,
                positiveText = context.getString(R.string.local_file_manager_authorize_directory),
                onNegative = dismiss,
                onNeutral = null,
                onPositive = {
                    dismiss()
                    onAuthorize()
                }
            )
        }.setCanceledOnTouchOutside(true)
    }

    fun showAuthorizedDirectoryAction(
        context: Context,
        label: String,
        uriText: String,
        onCopyPath: () -> Unit,
        onOpen: () -> Unit
    ) {
        showComposeDialog(context) { dismiss ->
            StorageAuthorizationContent(
                title = label.ifBlank { context.getString(R.string.local_file_manager_authorized_directory) },
                message = uriText,
                negativeText = context.getString(android.R.string.cancel),
                neutralText = context.getString(R.string.local_file_manager_copy_path),
                positiveText = context.getString(R.string.local_file_manager_authorized_directory),
                onNegative = dismiss,
                onNeutral = {
                    dismiss()
                    onCopyPath()
                },
                onPositive = {
                    dismiss()
                    onOpen()
                }
            )
        }.setCanceledOnTouchOutside(true)
    }
}

@Composable
private fun StorageAuthorizationContent(
    title: String,
    message: String,
    negativeText: String,
    neutralText: String?,
    positiveText: String,
    onNegative: () -> Unit,
    onNeutral: (() -> Unit)?,
    onPositive: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                modifier = Modifier.padding(top = 12.dp),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onNegative) { Text(negativeText) }
                onNeutral?.let { action ->
                    TextButton(onClick = action) { Text(neutralText.orEmpty()) }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onPositive) { Text(positiveText) }
            }
        }
    }
}