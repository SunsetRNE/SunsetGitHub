package com.Sunset.REN.GitHub.ui.compose.screens.filemanager

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.filemanager.LocalFilePreviewActionState
import com.Sunset.REN.GitHub.ui.filemanager.LocalFilePreviewChromeActions
import com.Sunset.REN.GitHub.ui.filemanager.LocalFilePreviewChromeState
import com.Sunset.REN.GitHub.ui.repo.setRepositoryMarkdown
import com.bumptech.glide.Glide

@Composable
fun LocalFilePreviewScreen(
    chromeState: LocalFilePreviewChromeState,
    chromeActions: LocalFilePreviewChromeActions,
    searchFocusRequestCount: Int,
    previewViewFactory: () -> View,
    onDisposePreviewView: (View) -> Unit,
    modifier: Modifier = Modifier
) {
    val previewView = remember(previewViewFactory)
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(searchFocusRequestCount) {
        if (searchFocusRequestCount > 0) {
            searchFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SunsetGitHubThemeTokens.colors.canvas)
    ) {
        PreviewHeader(chromeState)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(SunsetGitHubThemeTokens.colors.surface)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { previewView }
            )
            when {
                chromeState.showImagePreview -> ImagePreview(chromeState)
                chromeState.showArchivePreview -> ArchivePreviewText(chromeState.archivePreviewText)
                chromeState.showMarkdownPreview -> MarkdownPreviewText(chromeState.markdownPreviewText)
            }
        }
        if (chromeState.isSearchPanelVisible) {
            PreviewSearchPanel(
                state = chromeState,
                actions = chromeActions,
                searchFocusRequester = searchFocusRequester
            )
        }
        PreviewActionBar(state = chromeState, actions = chromeActions)
    }

    DisposableEffect(previewView) {
        onDispose {
            (previewView.parent as? ViewGroup)?.removeView(previewView)
            onDisposePreviewView(previewView)
        }
    }
}

@Composable
private fun PreviewHeader(state: LocalFilePreviewChromeState) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 34.dp)
            .background(colors.surface)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.name.ifBlank { state.path },
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (state.path.isNotBlank()) {
                Text(
                    text = state.path,
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = state.stateText,
            color = colors.textSecondary,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ImagePreview(state: LocalFilePreviewChromeState) {
    val colors = SunsetGitHubThemeTokens.colors
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(16.dp),
        factory = { context ->
            ImageView(context).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
        },
        update = { imageView ->
            if (state.imagePreviewUri != android.net.Uri.EMPTY) {
                Glide.with(imageView)
                    .load(state.imagePreviewUri)
                    .fitCenter()
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imageView)
            } else {
                imageView.setImageDrawable(null)
            }
        }
    )
}

@Composable
private fun ArchivePreviewText(text: String) {
    val colors = SunsetGitHubThemeTokens.colors
    Text(
        text = text,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        color = colors.textPrimary,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace
    )
}

@Composable
private fun MarkdownPreviewText(markdown: String) {
    val colors = SunsetGitHubThemeTokens.colors
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        factory = { context ->
            TextView(context).apply {
                setTextColor(colors.textPrimary.toArgb())
                textSize = 14f
                setLineSpacing(4f, 1f)
            }
        },
        update = { textView ->
            textView.setRepositoryMarkdown(
                markdown = markdown,
                baseHtmlUrl = null,
                imageAccessToken = ""
            )
        }
    )
}

@Composable
private fun PreviewSearchPanel(
    state: LocalFilePreviewChromeState,
    actions: LocalFilePreviewChromeActions,
    searchFocusRequester: FocusRequester
) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.query,
                onValueChange = actions.onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(searchFocusRequester),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { actions.onSearchQueryDone() }),
                placeholder = { Text("Search", style = MaterialTheme.typography.bodySmall) }
            )
            TextButton(enabled = state.canSearchText, onClick = actions.onFindPrevious) { Text("Prev") }
            TextButton(enabled = state.canSearchText, onClick = actions.onFindNext) { Text("Next") }
            TextButton(onClick = actions.onRegexHelp) { Text("?") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.replacement,
                onValueChange = actions.onReplacementChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { actions.onReplaceTextDone() }),
                placeholder = { Text("Replace", style = MaterialTheme.typography.bodySmall) }
            )
            TextButton(enabled = state.canEditSearchText, onClick = actions.onReplaceCurrent) { Text("Replace") }
            TextButton(enabled = state.canEditSearchText, onClick = actions.onReplaceAll) { Text("All") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.isIgnoreCaseEnabled, onCheckedChange = actions.onIgnoreCaseChange)
            Text("Ignore case", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
            Checkbox(checked = state.isRegexEnabled, onCheckedChange = actions.onRegexChange)
            Text("Regex", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
            Text(
                text = state.searchStatus,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PreviewActionBar(state: LocalFilePreviewChromeState, actions: LocalFilePreviewChromeActions) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(state.markdownToggle, actions.onToggleMarkdown)
        ActionButton(state.searchToggle, actions.onToggleSearch)
        ActionButton(state.convert, actions.onConvert)
        ActionButton(state.saveAs, actions.onSaveAs)
        ActionButton(state.extract, actions.onExtract)
        ActionButton(state.edit, actions.onEdit)
        ActionButton(state.undo, actions.onUndo)
        ActionButton(state.redo, actions.onRedo)
        ActionButton(state.cancel, actions.onCancel)
        ActionButton(state.save, actions.onSave)
    }
}

@Composable
private fun ActionButton(state: LocalFilePreviewActionState, onClick: () -> Unit) {
    if (!state.visible) return
    TextButton(
        enabled = state.enabled,
        modifier = Modifier.sizeIn(minWidth = 56.dp),
        onClick = onClick
    ) {
        Text(text = state.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
