package com.Sunset.REN.GitHub.ui.common

import android.app.Dialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

/**
 * Reusable compact option picker dialog for replacing platform Spinner popups.
 * It keeps list rendering consistent with the black-accent dialog family.
 */
object CompactOptionPickerDialog {

    fun show(
        context: Context,
        title: CharSequence,
        options: List<CharSequence>,
        selectedIndex: Int,
        subtitle: CharSequence = "选择一个选项",
        iconText: CharSequence = "⌄",
        searchHint: CharSequence = "搜索选项",
        cancelText: CharSequence = "取消",
        searchable: Boolean = true,
        onOptionSelected: (Int) -> Unit
    ): Dialog {
        lateinit var dialog: Dialog
        dialog = showComposeDialog(context) { dismiss ->
            CompactOptionPickerContent(
                title = title.toString(),
                options = options.map { it.toString() },
                selectedIndex = selectedIndex,
                subtitle = subtitle.toString(),
                iconText = iconText.toString(),
                searchHint = searchHint.toString(),
                cancelText = cancelText.toString(),
                searchable = searchable,
                onCancel = dismiss,
                onOptionSelected = { index ->
                    dismiss()
                    onOptionSelected(index)
                }
            )
        }
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }
}

@Composable
private fun CompactOptionPickerContent(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    subtitle: String,
    iconText: String,
    searchHint: String,
    cancelText: String,
    searchable: Boolean,
    onCancel: () -> Unit,
    onOptionSelected: (Int) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    var query by remember { mutableStateOf("") }
    val filteredOptions = options.mapIndexed { index, label -> IndexedOption(index, label) }
        .filter { option -> query.isBlank() || option.label.contains(query.trim(), ignoreCase = true) }

    Surface(
        color = colors.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = colors.textPrimary
                ) {
                    Text(
                        text = iconText,
                        color = colors.surface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        text = title,
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = colors.textMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (searchable) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(searchHint) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    singleLine = true
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredOptions, key = { it.index }) { option ->
                    CompactOptionRow(
                        label = option.label,
                        selected = option.index == selectedIndex,
                        onClick = { onOptionSelected(option.index) }
                    )
                }
            }

            TextButton(
                onClick = onCancel,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 10.dp)
            ) {
                Text(cancelText)
            }
        }
    }
}

@Composable
private fun CompactOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    val background = if (selected) colors.textPrimary else colors.subtleBackground
    val content = if (selected) colors.surface else colors.textPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (selected) "✓" else "",
            color = content,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 10.dp)
        )
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Text(
                text = "当前",
                color = content.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private data class IndexedOption(val index: Int, val label: String)