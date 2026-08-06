package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.ui.common.showComposeDialog
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

/** Shared input dialog for create/rename file-manager entries. */
object EntryNameInputDialog {
    fun show(
        context: Context,
        title: String,
        hint: String,
        @StringRes positiveText: Int,
        initialText: String = "",
        selectNameBeforeExtension: Boolean = false,
        onConfirm: (name: String, handle: EntryNameInputDialogHandle) -> Unit
    ) {
        showComposeDialog(context) { dismiss ->
            EntryNameInputContent(
                title = title,
                hint = hint,
                positiveText = context.getString(positiveText),
                cancelText = context.getString(android.R.string.cancel),
                initialText = initialText,
                selectNameBeforeExtension = selectNameBeforeExtension,
                onCancel = dismiss,
                onConfirm = onConfirm,
                dismiss = dismiss
            )
        }.setCanceledOnTouchOutside(true)
    }
}

interface EntryNameInputDialogHandle {
    fun dismiss()
    fun showError(message: String)
}

@Composable
private fun EntryNameInputContent(
    title: String,
    hint: String,
    positiveText: String,
    cancelText: String,
    initialText: String,
    selectNameBeforeExtension: Boolean,
    onCancel: () -> Unit,
    onConfirm: (name: String, handle: EntryNameInputDialogHandle) -> Unit,
    dismiss: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val initialSelectionEnd = remember(initialText, selectNameBeforeExtension) {
        if (initialText.isBlank()) {
            0
        } else if (selectNameBeforeExtension) {
            initialText.lastIndexOf('.').takeIf { it > 0 } ?: initialText.length
        } else {
            initialText.length
        }
    }
    var input by remember(initialText, initialSelectionEnd) {
        mutableStateOf(TextFieldValue(initialText, selection = TextRange(0, initialSelectionEnd)))
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val handle = remember {
        object : EntryNameInputDialogHandle {
            override fun dismiss() = dismiss()
            override fun showError(message: String) {
                errorMessage = message
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

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
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    errorMessage = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                label = { Text(hint) },
                singleLine = true,
                isError = errorMessage != null,
                supportingText = { errorMessage?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text
                )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onCancel) {
                    Text(cancelText)
                }
                TextButton(onClick = { onConfirm(input.text, handle) }) {
                    Text(positiveText)
                }
            }
        }
    }
}