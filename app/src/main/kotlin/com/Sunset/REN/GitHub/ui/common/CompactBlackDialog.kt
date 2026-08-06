package com.Sunset.REN.GitHub.ui.common

import android.app.Dialog
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton

/**
 * Compact, black-accented confirmation dialog used for high-signal actions.
 *
 * Keep this component UI-only: callers own navigation, deletion, submission and
 * other side effects through the supplied callbacks.
 */
object CompactBlackDialog {

    fun show(
        context: Context,
        @StringRes titleRes: Int,
        @StringRes messageRes: Int,
        iconText: CharSequence = "!",
        @StringRes negativeTextRes: Int,
        @StringRes positiveTextRes: Int,
        cancelable: Boolean = true,
        onNegativeClick: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null,
        onPositiveClick: () -> Unit
    ): Dialog {
        return show(
            context = context,
            title = context.getString(titleRes),
            message = context.getString(messageRes),
            iconText = iconText,
            negativeText = context.getString(negativeTextRes),
            positiveText = context.getString(positiveTextRes),
            cancelable = cancelable,
            onNegativeClick = onNegativeClick,
            onCancel = onCancel,
            onPositiveClick = onPositiveClick
        )
    }

    fun show(
        context: Context,
        title: CharSequence,
        message: CharSequence,
        iconText: CharSequence = "!",
        negativeText: CharSequence,
        positiveText: CharSequence,
        cancelable: Boolean = true,
        onNegativeClick: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null,
        onPositiveClick: () -> Unit
    ): Dialog {
        lateinit var dialog: Dialog
        dialog = showComposeDialog(context) { dismiss ->
            CompactBlackDialogContent(
                title = title.toString(),
                message = message.toString(),
                iconText = iconText.toString(),
                negativeText = negativeText.toString(),
                positiveText = positiveText.toString(),
                onNegativeClick = {
                    onNegativeClick?.invoke()
                    dismiss()
                },
                onPositiveClick = {
                    dismiss()
                    onPositiveClick()
                }
            )
        }
        dialog.setCancelable(cancelable)
        dialog.setCanceledOnTouchOutside(cancelable)
        if (onCancel != null) {
            dialog.setOnCancelListener { onCancel() }
        }
        return dialog
    }
}

@Composable
private fun CompactBlackDialogContent(
    title: String,
    message: String,
    iconText: String,
    negativeText: String,
    positiveText: String,
    onNegativeClick: () -> Unit,
    onPositiveClick: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Surface(
        color = colors.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = colors.textPrimary
            ) {
                Text(
                    text = iconText,
                    color = colors.surface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Text(
                text = title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onNegativeClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(negativeText)
                }
                SunsetPrimaryButton(
                    text = positiveText,
                    onClick = onPositiveClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
