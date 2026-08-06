package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.FileSizeFormatter
import com.Sunset.REN.GitHub.ui.common.showComposeDialog
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

/** Directory summary panel for the focused manager pane. */
object DirectoryPropertiesPanel {
    fun show(
        context: Context,
        path: String,
        entries: List<FileManagerEntry>
    ) {
        val realEntries = entries.filterNot { it.type == FileEntryType.Parent }
        val message = context.getString(
            R.string.local_file_manager_properties_directory_message,
            path,
            realEntries.count { it.type == FileEntryType.Directory },
            realEntries.count { it.type != FileEntryType.Directory },
            FileSizeFormatter.format(realEntries.mapNotNull { it.sizeBytes }.sum())
        )
        showComposeDialog(context) { dismiss ->
            DirectoryPropertiesContent(
                title = context.getString(R.string.local_file_manager_properties_directory),
                message = message,
                positiveText = context.getString(android.R.string.ok),
                onDismiss = dismiss
            )
        }.setCanceledOnTouchOutside(true)
    }
}

@Composable
private fun DirectoryPropertiesContent(
    title: String,
    message: String,
    positiveText: String,
    onDismiss: () -> Unit
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
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(positiveText)
                }
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    }
}
