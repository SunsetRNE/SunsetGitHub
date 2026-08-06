package com.Sunset.REN.GitHub.ui.common

import android.app.Dialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

data class ComposeDialogAction(
    val text: String,
    val onClick: () -> Unit
)

class ComposeProgressDialog internal constructor(
    private val dialog: Dialog,
    private val titleState: MutableState<String>,
    private val messageState: MutableState<String>,
    private val completedState: MutableState<Int>,
    private val totalState: MutableState<Int>,
    private val determinate: Boolean
) {
    fun update(title: String, message: String, completedCount: Int, totalCount: Int) {
        titleState.value = title
        messageState.value = message
        if (determinate) {
            totalState.value = totalCount.coerceAtLeast(1)
            completedState.value = completedCount.coerceIn(0, totalState.value)
        }
    }

    fun dismiss() {
        if (dialog.isShowing) dialog.dismiss()
    }
}

fun showComposeMessageDialog(
    context: Context,
    title: String,
    message: String,
    positiveText: String = context.getString(android.R.string.ok),
    negativeText: String? = null,
    neutralAction: ComposeDialogAction? = null,
    onPositive: (() -> Unit)? = null,
    onNegative: (() -> Unit)? = null
): Dialog {
    val dialog = showComposeDialog(context) { dismiss ->
        LegacyDialogSurface {
            LegacyDialogTitle(title)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = SunsetGitHubThemeTokens.colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            LegacyDialogActions(
                negativeText = negativeText,
                neutralText = neutralAction?.text,
                positiveText = positiveText,
                onNegative = {
                    dismiss()
                    onNegative?.invoke()
                },
                onNeutral = {
                    dismiss()
                    neutralAction?.onClick?.invoke()
                },
                onPositive = {
                    dismiss()
                    onPositive?.invoke()
                }
            )
        }
    }
    dialog.setCanceledOnTouchOutside(true)
    return dialog
}

fun <T> showComposeSingleChoiceDialog(
    context: Context,
    title: String,
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
): Dialog {
    val dialog = showComposeDialog(context) { dismiss ->
        LegacyDialogSurface {
            LegacyDialogTitle(title)
            Column(modifier = Modifier.padding(top = 10.dp)) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                dismiss()
                                onSelected(item)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = item == selected,
                            onClick = {
                                dismiss()
                                onSelected(item)
                            }
                        )
                        Text(
                            text = label(item),
                            modifier = Modifier.padding(start = 8.dp),
                            color = SunsetGitHubThemeTokens.colors.textPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            LegacyDialogActions(
                negativeText = context.getString(android.R.string.cancel),
                positiveText = null,
                onNegative = dismiss
            )
        }
    }
    dialog.setCanceledOnTouchOutside(true)
    return dialog
}

fun showComposeProgressDialog(
    context: Context,
    title: String,
    message: String,
    totalCount: Int? = null,
    onCancel: (() -> Unit)? = null
): ComposeProgressDialog {
    val titleState = mutableStateOf(title)
    val messageState = mutableStateOf(message)
    val completedState = mutableStateOf(0)
    val totalState = mutableStateOf(totalCount?.coerceAtLeast(1) ?: 1)
    val determinate = totalCount != null
    val dialog = showComposeDialog(context) { dismiss ->
        ProgressDialogContent(
            title = titleState.value,
            message = messageState.value,
            determinate = determinate,
            completedCount = completedState.value,
            totalCount = totalState.value,
            cancelText = context.getString(android.R.string.cancel),
            onCancel = onCancel?.let {
                {
                    dismiss()
                    it()
                }
            }
        )
    }
    dialog.setCancelable(onCancel != null)
    dialog.setCanceledOnTouchOutside(false)
    dialog.setOnCancelListener { onCancel?.invoke() }
    return ComposeProgressDialog(dialog, titleState, messageState, completedState, totalState, determinate)
}

@Composable
fun LegacyDialogSurface(content: @Composable ColumnScope.() -> Unit) {
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
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            content = content
        )
    }
}

@Composable
fun LegacyDialogTitle(title: String) {
    Text(
        text = title,
        color = SunsetGitHubThemeTokens.colors.textPrimary,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun LegacyDialogActions(
    negativeText: String? = null,
    neutralText: String? = null,
    positiveText: String? = null,
    onNegative: () -> Unit = {},
    onNeutral: () -> Unit = {},
    onPositive: () -> Unit = {}
) {
    if (negativeText == null && neutralText == null && positiveText == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        neutralText?.let {
            TextButton(onClick = onNeutral) { Text(it) }
        }
        Spacer(modifier = Modifier.weight(1f))
        negativeText?.let {
            TextButton(onClick = onNegative) { Text(it) }
        }
        positiveText?.let {
            TextButton(onClick = onPositive) { Text(it) }
        }
    }
}

@Composable
private fun ProgressDialogContent(
    title: String,
    message: String,
    determinate: Boolean,
    completedCount: Int,
    totalCount: Int,
    cancelText: String,
    onCancel: (() -> Unit)?
) {
    LegacyDialogSurface {
        LegacyDialogTitle(title)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            color = SunsetGitHubThemeTokens.colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(14.dp))
        if (determinate) {
            LinearProgressIndicator(
                progress = { completedCount.toFloat() / totalCount.coerceAtLeast(1).toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        if (onCancel != null) {
            LegacyDialogActions(
                negativeText = cancelText,
                positiveText = null,
                onNegative = onCancel
            )
        }
    }
}
