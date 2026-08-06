package com.Sunset.REN.GitHub.ui.common

import android.app.Dialog
import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import java.util.Locale

/** Rounded modal sheet used to select a local file provider. */
object FileSourcePickerDialog {
    fun show(
        context: Context,
        title: String,
        sources: List<FileSourceUiModel>,
        builtInActionLabel: String? = null,
        builtInActionDescription: String? = null,
        onBuiltInSelected: (() -> Unit)? = null,
        onSourceSelected: (FileSourceUiModel) -> Unit
    ) {
        lateinit var dialog: Dialog
        dialog = showComposeDialog(context) { dismiss ->
            FileSourcePickerContent(
                title = title,
                sources = sources,
                builtInActionLabel = builtInActionLabel,
                builtInActionDescription = builtInActionDescription.orEmpty(),
                onBuiltInSelected = onBuiltInSelected?.let { action ->
                    {
                        dismiss()
                        action()
                    }
                },
                onSourceSelected = { source ->
                    dismiss()
                    onSourceSelected(source)
                }
            )
        }
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}

@Composable
private fun FileSourcePickerContent(
    title: String,
    sources: List<FileSourceUiModel>,
    builtInActionLabel: String?,
    builtInActionDescription: String,
    onBuiltInSelected: (() -> Unit)?,
    onSourceSelected: (FileSourceUiModel) -> Unit
) {
    val context = LocalContext.current
    val colors = SunsetGitHubThemeTokens.colors
    val groupedSources = sources.groupBy { it.groupLabel }
    val groupOrder = listOf(
        context.getString(R.string.file_source_picker_group_system),
        context.getString(R.string.file_source_picker_group_third_party),
        context.getString(R.string.file_source_picker_group_other)
    )

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = context.getString(R.string.file_source_picker_description),
                    modifier = Modifier.padding(top = 3.dp, bottom = 6.dp),
                    color = colors.textMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (!builtInActionLabel.isNullOrBlank() && onBuiltInSelected != null) {
                item { FileSourceGroupHeader(context.getString(R.string.file_source_picker_group_built_in)) }
                item {
                    FileSourceRow(
                        iconRes = R.drawable.ic_file_24,
                        title = builtInActionLabel,
                        subtitle = builtInActionDescription,
                        packageName = null,
                        recommended = true,
                        onClick = onBuiltInSelected
                    )
                }
            }

            groupOrder.forEach { groupLabel ->
                val groupSources = groupedSources[groupLabel].orEmpty()
                if (groupSources.isNotEmpty()) {
                    item { FileSourceGroupHeader(groupLabel.uppercase(Locale.getDefault())) }
                    items(groupSources, key = { source -> source.packageName + source.label }) { source ->
                        FileSourceRow(
                            iconRes = source.iconRes,
                            title = source.label,
                            subtitle = if (source.isRecommended) {
                                context.getString(R.string.file_source_picker_recommended)
                            } else {
                                source.packageName
                            },
                            packageName = source.packageName,
                            recommended = source.isRecommended,
                            trailingLabel = if (source.isRecommended) {
                                context.getString(R.string.file_source_picker_recommended)
                            } else {
                                null
                            },
                            onClick = { onSourceSelected(source) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileSourceGroupHeader(text: String) {
    val colors = SunsetGitHubThemeTokens.colors
    Text(
        text = text,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
        color = colors.textSecondary,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun FileSourceRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    packageName: String?,
    recommended: Boolean,
    trailingLabel: String? = null,
    onClick: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    val rowBackground = if (recommended) colors.subtleBackground else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(rowBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileSourceIcon(
            packageName = packageName,
            iconRes = iconRes,
            recommended = recommended,
            modifier = Modifier.size(28.dp)
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (recommended) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 1.dp),
                color = if (recommended) colors.accent else colors.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailingLabel?.let { label ->
            Text(
                text = label,
                modifier = Modifier.padding(start = 10.dp),
                color = colors.accent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FileSourceIcon(
    packageName: String?,
    iconRes: Int,
    recommended: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = SunsetGitHubThemeTokens.colors
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            ImageView(viewContext).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE }
        },
        update = { imageView ->
            val appIcon = packageName?.let { name ->
                runCatching { context.packageManager.getApplicationIcon(name) }.getOrNull()
            }
            if (appIcon != null) {
                imageView.setImageDrawable(appIcon)
                imageView.imageTintList = null
            } else {
                imageView.setImageResource(iconRes)
                imageView.imageTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.valueOf(
                        if (recommended) colors.accent.red else colors.textSecondary.red,
                        if (recommended) colors.accent.green else colors.textSecondary.green,
                        if (recommended) colors.accent.blue else colors.textSecondary.blue,
                        if (recommended) colors.accent.alpha else colors.textSecondary.alpha
                    ).toArgb()
                )
            }
            imageView.alpha = if (recommended) 1f else 0.86f
        }
    )
}
