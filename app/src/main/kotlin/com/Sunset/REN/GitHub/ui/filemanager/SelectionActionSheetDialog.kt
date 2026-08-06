package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.ui.common.showComposeDialog
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

/** Simple list-based action sheet for selected file-manager entries. */
object SelectionActionSheetDialog {
    fun show(
        context: Context,
        title: String,
        actions: List<SelectionActionItem>
    ) {
        if (actions.isEmpty()) return
        showComposeDialog(context) { dismiss ->
            SelectionActionSheetContent(
                title = title,
                actions = actions,
                cancelText = context.getString(android.R.string.cancel),
                onCancel = dismiss,
                onActionSelected = { action ->
                    dismiss()
                    action.onClick()
                }
            )
        }.setCanceledOnTouchOutside(true)
    }
}

@Composable
private fun SelectionActionSheetContent(
    title: String,
    actions: List<SelectionActionItem>,
    cancelText: String,
    onCancel: () -> Unit,
    onActionSelected: (SelectionActionItem) -> Unit
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
            Column(modifier = Modifier.padding(top = 12.dp)) {
                actions.forEach { action ->
                    val textColor = if (action.enabled) colors.textPrimary else colors.textSecondary
                    val actionModifier = Modifier
                        .fillMaxWidth()
                        .then(if (action.enabled) Modifier.clickable { onActionSelected(action) } else Modifier)
                        .padding(vertical = 9.dp)
                    Column(modifier = actionModifier) {
                        Text(
                            text = action.label,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val disabledReason = action.disabledReason
                        if (!action.enabled && !disabledReason.isNullOrBlank()) {
                            Text(
                                text = disabledReason,
                                color = colors.textSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
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

data class SelectionActionItem(
    val label: String,
    val enabled: Boolean = true,
    val disabledReason: String? = null,
    val onClick: () -> Unit
) {
    constructor(label: String, onClick: () -> Unit) : this(label, true, null, onClick)
}