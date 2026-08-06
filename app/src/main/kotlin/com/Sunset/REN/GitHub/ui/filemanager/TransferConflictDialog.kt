package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.common.showComposeDialog
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

/** Conflict report dialog for copy/move-to-target flows. */
object TransferConflictDialog {
    fun show(
        context: Context,
        title: String,
        message: String,
        onReplaceExisting: () -> Unit,
        onKeepBoth: () -> Unit,
        onSkipConflicts: () -> Unit
    ) {
        val actions = listOf(
            context.getString(R.string.local_file_manager_conflict_replace_existing) to onReplaceExisting,
            context.getString(R.string.local_file_manager_conflict_keep_both) to onKeepBoth,
            context.getString(R.string.local_file_manager_conflict_skip) to onSkipConflicts
        )
        showComposeDialog(context) { dismiss ->
            TransferConflictContent(
                title = title,
                message = message,
                actions = actions,
                cancelText = context.getString(android.R.string.cancel),
                onCancel = dismiss,
                onActionSelected = { action ->
                    dismiss()
                    action()
                }
            )
        }.setCanceledOnTouchOutside(true)
    }
}

@Composable
private fun TransferConflictContent(
    title: String,
    message: String,
    actions: List<Pair<String, () -> Unit>>,
    cancelText: String,
    onCancel: () -> Unit,
    onActionSelected: (() -> Unit) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    val maxMessageHeight = (LocalConfiguration.current.screenHeightDp * 0.36f).dp.coerceAtLeast(160.dp)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .heightIn(max = maxMessageHeight)
                    .verticalScroll(rememberScrollState()),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Column(modifier = Modifier.padding(top = 12.dp)) {
                actions.forEach { (label, action) ->
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onActionSelected(action) }
                            .padding(vertical = 10.dp),
                        color = colors.accent,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
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