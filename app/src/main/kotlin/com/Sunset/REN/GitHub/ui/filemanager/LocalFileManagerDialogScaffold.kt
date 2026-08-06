package com.Sunset.REN.GitHub.ui.filemanager

import android.app.Dialog
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.ui.common.showComposeDialog
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

/**
 * Shared MT-style plain dialog scaffold for the local file manager.
 *
 * Keep long paths, reports and facts in a selectable, scrollable content area so
 * every feature dialog follows the same overflow behaviour instead of relying on
 * AlertDialog#setMessage defaults.
 */
object LocalFileManagerDialogScaffold {
    fun showPlainScrollable(
        context: Context,
        title: String,
        message: String,
        @StringRes positiveText: Int = android.R.string.ok,
        positiveLabel: String? = null,
        @StringRes negativeText: Int? = null,
        @StringRes neutralText: Int? = null,
        onPositive: (() -> Unit)? = null,
        onNeutral: (() -> Unit)? = null
    ): Dialog {
        val dialog = showComposeDialog(context) { dismiss ->
            PlainScrollableDialogContent(
                title = title,
                message = message,
                positiveText = positiveLabel ?: context.getString(positiveText),
                negativeText = negativeText?.let(context::getString),
                neutralText = neutralText?.let(context::getString),
                onNegative = dismiss,
                onNeutral = {
                    dismiss()
                    onNeutral?.invoke()
                },
                onPositive = {
                    dismiss()
                    onPositive?.invoke()
                }
            )
        }
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }
}

@Composable
private fun PlainScrollableDialogContent(
    title: String,
    message: String,
    positiveText: String,
    negativeText: String?,
    neutralText: String?,
    onNegative: () -> Unit,
    onNeutral: () -> Unit,
    onPositive: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    val maxMessageHeight = (LocalConfiguration.current.screenHeightDp * 0.58f).dp.coerceAtLeast(220.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .heightIn(max = maxMessageHeight)
                    .verticalScroll(rememberScrollState()),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                negativeText?.let { label ->
                    TextButton(onClick = onNegative) { Text(label) }
                }
                neutralText?.let { label ->
                    TextButton(onClick = onNeutral) { Text(label) }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onPositive) { Text(positiveText) }
            }
        }
    }
}