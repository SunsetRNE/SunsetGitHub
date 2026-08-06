package com.Sunset.REN.GitHub.ui.compose.screens.repo

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.data.github.html.RepositoryBranchProtectionUpdateRequest
import com.Sunset.REN.GitHub.data.github.html.RepositoryBranchRestrictionUpdate
import com.Sunset.REN.GitHub.data.github.html.RepositoryCollaboratorPermission
import com.Sunset.REN.GitHub.data.github.html.RepositoryRequiredPullRequestReviewsUpdate
import com.Sunset.REN.GitHub.data.github.html.RepositoryRequiredStatusChecksUpdate
import com.Sunset.REN.GitHub.data.github.html.RepositoryBranchProtectionSnapshot

@Composable
fun RepositoryOptionListDialog(
    title: String,
    message: String,
    options: List<String>,
    onDismiss: () -> Unit,
    onOptionSelected: (Int) -> Unit,
    dismissText: String = "取消"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                options.forEachIndexed { index, option ->
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOptionSelected(index) }
                    ) {
                        Text(text = option)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        }
    )
}

@Composable
fun RepositoryConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    dismissText: String = "取消"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        }
    )
}

@Composable
fun RepositoryDualActionDialog(
    title: String,
    message: String,
    primaryText: String,
    secondaryText: String,
    onDismiss: () -> Unit,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onPrimary) {
                Text(text = primaryText)
            }
        },
        dismissButton = {
            TextButton(onClick = onSecondary) {
                Text(text = secondaryText)
            }
        }
    )
}

@Composable
fun RepositoryForkContributeDialog(
    title: String,
    message: String,
    defaultTitle: String,
    defaultBody: String,
    titleLabel: String,
    bodyLabel: String,
    helperText: String,
    titleRequiredError: String,
    primaryText: String,
    secondaryText: String,
    dismissText: String,
    onDismiss: () -> Unit,
    onCreatePullRequest: (String, String) -> Unit,
    onOpenGitHub: () -> Unit
) {
    var pullRequestTitle by remember { mutableStateOf(defaultTitle) }
    var pullRequestBody by remember { mutableStateOf(defaultBody) }
    var titleError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = pullRequestTitle,
                    onValueChange = {
                        pullRequestTitle = it
                        if (titleError != null && it.isNotBlank()) titleError = null
                    },
                    label = { Text(text = titleLabel) },
                    singleLine = true,
                    isError = titleError != null,
                    supportingText = { titleError?.let { Text(text = it) } }
                )
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    value = pullRequestBody,
                    onValueChange = { pullRequestBody = it },
                    label = { Text(text = bodyLabel) },
                    minLines = 4,
                    maxLines = 8
                )
                Text(
                    text = helperText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalizedTitle = pullRequestTitle.trim()
                    if (normalizedTitle.isBlank()) {
                        titleError = titleRequiredError
                    } else {
                        onCreatePullRequest(normalizedTitle, pullRequestBody)
                    }
                }
            ) {
                Text(text = primaryText)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onOpenGitHub) {
                    Text(text = secondaryText)
                }
                TextButton(onClick = onDismiss) {
                    Text(text = dismissText)
                }
            }
        }
    )
}

@Composable
fun RepositoryTripleActionDialog(
    title: String,
    message: String,
    primaryText: String,
    secondaryText: String,
    neutralText: String,
    onDismiss: () -> Unit,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onNeutral: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onPrimary) {
                Text(text = primaryText)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onNeutral) {
                    Text(text = neutralText)
                }
                TextButton(onClick = onSecondary) {
                    Text(text = secondaryText)
                }
            }
        }
    )
}

@Composable
fun RepositoryTextInputDialog(
    title: String,
    label: String,
    helperText: String,
    initialValue: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    dismissText: String = "取消",
    normalizeValue: (String) -> String = { it.trim() },
    requiredErrorText: String = "不能为空"
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = value,
                    onValueChange = {
                        value = it
                        error = null
                    },
                    singleLine = true,
                    label = { Text(text = label) },
                    supportingText = { Text(text = error ?: helperText) },
                    isError = error != null
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalized = normalizeValue(value)
                    if (normalized.isBlank()) {
                        error = requiredErrorText
                    } else {
                        onConfirm(normalized)
                    }
                }
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        }
    )
}

@Composable
fun RepositoryTwoTextInputDialog(
    title: String,
    message: String,
    firstLabel: String,
    secondLabel: String,
    firstInitialValue: String = "",
    secondInitialValue: String = "",
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    dismissText: String = "取消",
    firstRequiredErrorText: String = "不能为空",
    secondRequiredErrorText: String = "不能为空"
) {
    var firstValue by remember(firstInitialValue) { mutableStateOf(firstInitialValue) }
    var secondValue by remember(secondInitialValue) { mutableStateOf(secondInitialValue) }
    var firstError by remember { mutableStateOf<String?>(null) }
    var secondError by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = firstValue,
                    onValueChange = { value ->
                        firstValue = value
                        firstError = null
                    },
                    singleLine = true,
                    label = { Text(text = firstLabel) },
                    supportingText = firstError?.let { error -> { Text(error) } },
                    isError = firstError != null
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = secondValue,
                    onValueChange = { value ->
                        secondValue = value
                        secondError = null
                    },
                    singleLine = true,
                    label = { Text(text = secondLabel) },
                    supportingText = secondError?.let { error -> { Text(error) } },
                    isError = secondError != null
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val first = firstValue.trim()
                    val second = secondValue.trim()
                    firstError = if (first.isBlank()) firstRequiredErrorText else null
                    secondError = if (second.isBlank()) secondRequiredErrorText else null
                    if (firstError == null && secondError == null) {
                        onConfirm(first, second)
                    }
                }
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        }
    )
}

data class RepositoryEditorToolDialogItem(
    val label: String,
    val icon: String,
    val isSection: Boolean = false
)

@Composable
fun RepositoryEditorToolsDialog(
    title: String,
    message: String,
    items: List<RepositoryEditorToolDialogItem>,
    dismissText: String,
    onDismiss: () -> Unit,
    onItemSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                items.forEachIndexed { index, item ->
                    if (item.isSection) {
                        Text(
                            modifier = Modifier.padding(top = 8.dp),
                            text = item.label.replace("──", "").trim(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    } else {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onItemSelected(index) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.icon,
                                    modifier = Modifier.padding(end = 12.dp),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = item.label)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        }
    )
}

@Composable
fun RepositoryDiffPreviewDialog(
    title: String,
    fileName: String,
    path: String,
    summary: String,
    stats: List<String>,
    diffText: String,
    copyText: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = fileName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(text = path, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(text = summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(text = stats.joinToString("   "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(10.dp)
                        .horizontalScroll(rememberScrollState())
                        .verticalScroll(rememberScrollState()),
                    text = diffText,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onCopy) {
                Text(text = copyText)
            }
        }
    )
}

@Composable
fun RepositorySubmitConfirmDialog(
    title: String,
    message: String,
    commitMessageLabel: String,
    initialCommitMessage: String,
    confirmText: String,
    viewDiffText: String,
    dismissText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onViewDiff: () -> Unit
) {
    var commitMessage by remember(initialCommitMessage) { mutableStateOf(initialCommitMessage) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    minLines = 1,
                    maxLines = 3,
                    label = { Text(text = commitMessageLabel) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(commitMessage) }) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onViewDiff) {
                    Text(text = viewDiffText)
                }
                TextButton(onClick = onDismiss) {
                    Text(text = dismissText)
                }
            }
        }
    )
}

@Composable
fun RepositoryEditorDisplaySettingsDialog(
    title: String,
    textSizeValueText: (Int) -> String,
    softWrapText: String,
    symbolBarText: String,
    initialTextSizeSp: Float,
    minTextSizeSp: Float,
    maxTextSizeSp: Float,
    initialSoftWrap: Boolean,
    initialSymbolBar: Boolean,
    confirmText: String,
    onDismiss: () -> Unit,
    onTextSizeChange: (Float) -> Unit,
    onSoftWrapChange: (Boolean) -> Unit,
    onSymbolBarChange: (Boolean) -> Unit
) {
    var textSize by remember(initialTextSizeSp) { mutableStateOf(initialTextSizeSp) }
    var softWrap by remember(initialSoftWrap) { mutableStateOf(initialSoftWrap) }
    var symbolBar by remember(initialSymbolBar) { mutableStateOf(initialSymbolBar) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = textSizeValueText(textSize.toInt()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = textSize,
                    onValueChange = { value ->
                        val coerced = value.coerceIn(minTextSizeSp, maxTextSizeSp)
                        textSize = coerced
                        onTextSizeChange(coerced)
                    },
                    valueRange = minTextSizeSp..maxTextSizeSp,
                    steps = (maxTextSizeSp - minTextSizeSp).toInt().minus(1).coerceAtLeast(0)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = softWrapText)
                    Switch(
                        checked = softWrap,
                        onCheckedChange = { checked ->
                            softWrap = checked
                            onSoftWrapChange(checked)
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = symbolBarText)
                    Switch(
                        checked = symbolBar,
                        onCheckedChange = { checked ->
                            symbolBar = checked
                            onSymbolBarChange(checked)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = confirmText)
            }
        }
    )
}

@Composable
fun RepositoryDeployKeyAddDialog(
    onDismiss: () -> Unit,
    onConfirmReadOnly: (title: String, key: String) -> Unit,
    onConfirmWriteAccess: (title: String, key: String) -> Unit,
    dismissText: String = "取消"
) {
    var title by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var writeAccess by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var keyError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "新增部署密钥") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "默认添加只读部署密钥。写权限密钥可直接向仓库推送代码，仅在完全信任私钥保存位置时启用。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = null
                    },
                    singleLine = true,
                    label = { Text(text = "标题") },
                    supportingText = { Text(text = titleError ?: "用于识别这把密钥的用途") },
                    isError = titleError != null
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = key,
                    onValueChange = {
                        key = it
                        keyError = null
                    },
                    minLines = 3,
                    label = { Text(text = "SSH 公钥") },
                    supportingText = { Text(text = keyError ?: "粘贴 OpenSSH 公钥，私钥不要上传到 GitHub") },
                    isError = keyError != null
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = writeAccess,
                        onCheckedChange = { writeAccess = it }
                    )
                    Text(
                        text = "允许写权限（高风险）",
                        color = if (writeAccess) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalizedTitle = title.trim()
                    val normalizedKey = key.trim()
                    titleError = if (normalizedTitle.isBlank()) "请输入标题" else null
                    keyError = when {
                        normalizedKey.isBlank() -> "请输入 SSH 公钥"
                        !normalizedKey.startsWith("ssh-") && !normalizedKey.startsWith("ecdsa-") -> "请粘贴 OpenSSH 格式公钥，例如 ssh-ed25519 或 ssh-rsa 开头"
                        else -> null
                    }
                    if (titleError == null && keyError == null) {
                        if (writeAccess) {
                            onConfirmWriteAccess(normalizedTitle, normalizedKey)
                        } else {
                            onConfirmReadOnly(normalizedTitle, normalizedKey)
                        }
                    }
                }
            ) {
                Text(text = "继续")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        }
    )
}

@Composable
fun RepositoryWebhookCreateDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, secret: String, events: String) -> Unit,
    dismissText: String = "取消"
) {
    var url by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var events by remember { mutableStateOf("push") }
    var urlError by remember { mutableStateOf<String?>(null) }
    var eventsError by remember { mutableStateOf<String?>(null) }
    val eventPattern = remember { Regex("^[A-Za-z0-9_.*-]+$") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "新增 Webhook") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Webhook Secret 只会随创建请求发送，不会保存到本地，也不会在列表中回显。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = url,
                    onValueChange = {
                        url = it
                        urlError = null
                    },
                    singleLine = true,
                    label = { Text(text = "Payload URL") },
                    supportingText = { Text(text = urlError ?: "必须是外部服务可访问的 HTTPS 地址") },
                    isError = urlError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = secret,
                    onValueChange = { secret = it },
                    singleLine = true,
                    label = { Text(text = "Secret（可选）") },
                    supportingText = { Text(text = "建议配置 Secret 以验证请求来源") }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = events,
                    onValueChange = {
                        events = it
                        eventsError = null
                    },
                    minLines = 2,
                    label = { Text(text = "事件") },
                    supportingText = { Text(text = eventsError ?: "多个事件用逗号或换行分隔，留空时默认 push") },
                    isError = eventsError != null
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalizedUrl = url.trim()
                    urlError = when {
                        normalizedUrl.isBlank() -> "请输入 Payload URL"
                        !normalizedUrl.startsWith("https://", ignoreCase = true) -> "建议使用 HTTPS 地址，避免 Webhook 内容被明文传输"
                        else -> null
                    }
                    val invalidEvent = events
                        .split(',', '\n')
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .any { !eventPattern.matches(it) }
                    eventsError = if (invalidEvent) "事件名称只能包含字母、数字、下划线、点、星号或短横线" else null
                    if (urlError == null && eventsError == null) {
                        onConfirm(normalizedUrl, secret, events)
                    }
                }
            ) {
                Text(text = "创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        }
    )
}

@Composable
fun RepositoryPermissionPickerDialog(
    title: String,
    message: String? = null,
    permissions: List<RepositoryCollaboratorPermission>,
    initialPermission: RepositoryCollaboratorPermission,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (RepositoryCollaboratorPermission) -> Unit,
    dismissText: String = "取消",
    permissionDescription: (RepositoryCollaboratorPermission) -> String
) {
    var selectedPermission by remember(initialPermission) { mutableStateOf(initialPermission) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                permissions.forEach { permission ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = permission == selectedPermission,
                                onClick = { selectedPermission = permission }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = permission == selectedPermission,
                            onClick = { selectedPermission = permission }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = permission.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = permissionDescription(permission),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedPermission) }) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        }
    )
}

@Composable
fun RepositoryBranchProtectionEditorDialog(
    branch: String,
    current: RepositoryBranchProtectionSnapshot?,
    title: String,
    message: String,
    saveText: String,
    cancelText: String,
    labels: RepositoryBranchProtectionEditorLabels,
    onDismiss: () -> Unit,
    onSave: (RepositoryBranchProtectionUpdateRequest) -> Unit
) {
    var reviews by remember(current) { mutableStateOf((current?.requiredPullRequestReviews?.requiredApprovingReviewCount ?: 1).toString()) }
    var checks by remember(current) { mutableStateOf(current?.requiredStatusChecks?.contexts.orEmpty().joinToString(",")) }
    var users by remember(current) { mutableStateOf(current?.restrictions?.users.orEmpty().joinToString(",")) }
    var teams by remember(current) { mutableStateOf(current?.restrictions?.teams.orEmpty().joinToString(",")) }
    var apps by remember(current) { mutableStateOf(current?.restrictions?.apps.orEmpty().joinToString(",")) }
    var enforce by remember(current) { mutableStateOf(current?.enforceAdmins ?: false) }
    var stale by remember(current) { mutableStateOf(current?.requiredPullRequestReviews?.dismissStaleReviews ?: true) }
    var codeOwner by remember(current) { mutableStateOf(current?.requiredPullRequestReviews?.requireCodeOwnerReviews ?: false) }
    var lastPush by remember(current) { mutableStateOf(current?.requiredPullRequestReviews?.requireLastPushApproval ?: false) }
    var linear by remember(current) { mutableStateOf(current?.requiredLinearHistory ?: false) }
    var conversation by remember(current) { mutableStateOf(current?.requiredConversationResolution ?: false) }
    var signed by remember(current) { mutableStateOf(current?.requiredSignatures ?: false) }
    var forcePush by remember(current) { mutableStateOf(current?.allowForcePushes ?: false) }
    var deletion by remember(current) { mutableStateOf(current?.allowDeletions ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = reviews,
                    onValueChange = { reviews = it },
                    singleLine = true,
                    label = { Text(labels.reviews) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = checks,
                    onValueChange = { checks = it },
                    label = { Text(labels.checks) }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = users,
                    onValueChange = { users = it },
                    label = { Text(labels.users) }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = teams,
                    onValueChange = { teams = it },
                    label = { Text(labels.teams) }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = apps,
                    onValueChange = { apps = it },
                    label = { Text(labels.apps) }
                )
                BranchProtectionCheckbox(labels.enforceAdmins, enforce) { enforce = it }
                BranchProtectionCheckbox(labels.dismissStaleReviews, stale) { stale = it }
                BranchProtectionCheckbox(labels.requireCodeOwnerReviews, codeOwner) { codeOwner = it }
                BranchProtectionCheckbox(labels.requireLastPushApproval, lastPush) { lastPush = it }
                BranchProtectionCheckbox(labels.requireLinearHistory, linear) { linear = it }
                BranchProtectionCheckbox(labels.requireConversationResolution, conversation) { conversation = it }
                BranchProtectionCheckbox(labels.requireSignedCommits, signed) { signed = it }
                BranchProtectionCheckbox(labels.allowForcePushes, forcePush) { forcePush = it }
                BranchProtectionCheckbox(labels.allowDeletions, deletion) { deletion = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val count = (reviews.toIntOrNull() ?: 1).coerceIn(0, 6)
                    val contexts = splitCsv(checks)
                    val restrictedUsers = splitCsv(users)
                    val restrictedTeams = splitCsv(teams)
                    val restrictedApps = splitCsv(apps)
                    val restrictions = if (restrictedUsers.isNotEmpty() || restrictedTeams.isNotEmpty() || restrictedApps.isNotEmpty()) {
                        RepositoryBranchRestrictionUpdate(restrictedUsers, restrictedTeams, restrictedApps)
                    } else {
                        null
                    }
                    onSave(
                        RepositoryBranchProtectionUpdateRequest(
                            requiredStatusChecks = contexts.takeIf { it.isNotEmpty() }?.let {
                                RepositoryRequiredStatusChecksUpdate(strict = true, contexts = it)
                            },
                            requiredPullRequestReviews = count.takeIf { it > 0 }?.let {
                                RepositoryRequiredPullRequestReviewsUpdate(
                                    dismissStaleReviews = stale,
                                    requireCodeOwnerReviews = codeOwner,
                                    requiredApprovingReviewCount = it,
                                    requireLastPushApproval = lastPush
                                )
                            },
                            enforceAdmins = enforce,
                            restrictions = restrictions,
                            requiredLinearHistory = linear,
                            allowForcePushes = forcePush,
                            allowDeletions = deletion,
                            requiredConversationResolution = conversation,
                            requiredSignatures = signed
                        )
                    )
                }
            ) {
                Text(text = saveText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = cancelText)
            }
        }
    )
}

@Composable
private fun BranchProtectionCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = checked, onClick = { onCheckedChange(!checked) })
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun splitCsv(value: String): List<String> = value.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct()

data class RepositoryBranchProtectionEditorLabels(
    val reviews: String,
    val checks: String,
    val users: String,
    val teams: String,
    val apps: String,
    val enforceAdmins: String,
    val dismissStaleReviews: String,
    val requireCodeOwnerReviews: String,
    val requireLastPushApproval: String,
    val requireLinearHistory: String,
    val requireConversationResolution: String,
    val requireSignedCommits: String,
    val allowForcePushes: String,
    val allowDeletions: String
)
