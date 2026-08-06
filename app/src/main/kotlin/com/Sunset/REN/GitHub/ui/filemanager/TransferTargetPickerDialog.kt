package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.common.showComposeDialog
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

/** Shared picker for copy/move/unzip target locations. */
object TransferTargetPickerDialog {
    fun show(
        context: Context,
        title: String,
        options: List<TransferTargetOption>,
        manualOptionLabel: String = context.getString(R.string.local_file_manager_select_other_directory),
        onTargetSelected: (String) -> Unit,
        onManualTargetRequested: () -> Unit
    ) {
        showComposeDialog(context) { dismiss ->
            TransferTargetPickerContent(
                title = title,
                options = options,
                manualOptionLabel = manualOptionLabel,
                cancelText = context.getString(android.R.string.cancel),
                onCancel = dismiss,
                onTargetSelected = { path ->
                    dismiss()
                    onTargetSelected(path)
                },
                onManualTargetRequested = {
                    dismiss()
                    onManualTargetRequested()
                }
            )
        }.setCanceledOnTouchOutside(true)
    }
}

@Composable
private fun TransferTargetPickerContent(
    title: String,
    options: List<TransferTargetOption>,
    manualOptionLabel: String,
    cancelText: String,
    onCancel: () -> Unit,
    onTargetSelected: (String) -> Unit,
    onManualTargetRequested: () -> Unit
) {
    PickerSurface(title = title, cancelText = cancelText, onCancel = onCancel) {
        options.forEach { option ->
            PickerRow(
                title = option.label,
                subtitle = option.path,
                onClick = { onTargetSelected(option.path) }
            )
        }
        PickerRow(
            title = manualOptionLabel,
            subtitle = null,
            onClick = onManualTargetRequested
        )
    }
}

@Composable
private fun PickerSurface(
    title: String,
    cancelText: String,
    onCancel: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
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
            Column(modifier = Modifier.padding(top = 12.dp), content = content)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onCancel) { Text(cancelText) }
            }
        }
    }
}

@Composable
private fun PickerRow(title: String, subtitle: String?, onClick: () -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = title,
            color = colors.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        subtitle?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 2.dp),
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

data class TransferTargetOption(
    val label: String,
    val path: String
)