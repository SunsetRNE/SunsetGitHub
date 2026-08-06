package com.Sunset.REN.GitHub.ui.compose.screens.repo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionWorkflow
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionWorkflowInput

/** Compose replacement for the legacy Actions workflow drawer. */
@Composable
fun RepositoryActionsWorkflowDialog(
    title: String,
    message: String,
    allWorkflowsTitle: String,
    allWorkflowsMeta: String,
    emptyText: String,
    loadingText: String,
    workflows: List<RepositoryActionWorkflow>,
    selectedWorkflowId: Long?,
    isLoading: Boolean,
    dispatchingWorkflowId: Long?,
    dispatchableText: String,
    notDispatchableText: String,
    dispatchingText: String,
    runText: String,
    localizeWorkflowState: (String) -> String,
    localizeWorkflowTrigger: (String) -> String,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onSelectWorkflow: (RepositoryActionWorkflow) -> Unit,
    onDispatchWorkflow: (RepositoryActionWorkflow) -> Unit,
    onOpenWorkflow: (RepositoryActionWorkflow) -> Unit,
    dismissText: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                WorkflowRow(
                    title = allWorkflowsTitle,
                    meta = allWorkflowsMeta,
                    subtitle = "",
                    selected = selectedWorkflowId == null,
                    actionText = null,
                    onClick = onSelectAll,
                    onAction = null,
                    onOpen = null
                )
                if (workflows.isEmpty()) {
                    Text(
                        text = if (isLoading) loadingText else emptyText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    workflows.forEach { workflow ->
                        val meta = listOfNotNull(
                            workflow.displayState.takeIf { it.isNotBlank() }?.let(localizeWorkflowState),
                            workflow.path.takeIf { it.isNotBlank() },
                            workflow.rawTriggers.map(localizeWorkflowTrigger).joinToString("、").takeIf { it.isNotBlank() }
                        ).joinToString(" · ")
                        val subtitle = listOfNotNull(workflow.createdAt, workflow.updatedAt)
                            .distinct()
                            .joinToString(" · ")
                        val actionText = when {
                            !workflow.isDispatchable -> notDispatchableText
                            dispatchingWorkflowId == workflow.id -> dispatchingText
                            else -> runText
                        }
                        WorkflowRow(
                            title = workflow.name,
                            meta = meta,
                            subtitle = subtitle,
                            selected = selectedWorkflowId == workflow.id,
                            actionText = actionText,
                            actionEnabled = workflow.isDispatchable && dispatchingWorkflowId != workflow.id,
                            supportedText = dispatchableText,
                            onClick = { onSelectWorkflow(workflow) },
                            onAction = { onDispatchWorkflow(workflow) },
                            onOpen = { onOpenWorkflow(workflow) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = dismissText) }
        }
    )
}

@Composable
private fun WorkflowRow(
    title: String,
    meta: String,
    subtitle: String,
    selected: Boolean,
    actionText: String?,
    actionEnabled: Boolean = true,
    supportedText: String = "",
    onClick: () -> Unit,
    onAction: (() -> Unit)?,
    onOpen: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selected) "✓ $title" else title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            actionText?.let { text ->
                OutlinedButton(
                    enabled = actionEnabled,
                    onClick = { onAction?.invoke() }
                ) { Text(text = text) }
            }
        }
        if (meta.isNotBlank()) {
            Text(text = meta, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        if (subtitle.isNotBlank()) {
            Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        if (onOpen != null) {
            TextButton(onClick = onOpen) { Text(text = supportedText) }
        }
    }
}

data class RepositoryActionsWorkflowDispatchRequest(
    val workflow: RepositoryActionWorkflow,
    val ref: String,
    val inputs: Map<String, String>
)

/** Compose replacement for the legacy workflow dispatch dynamic bottom sheet form. */
@Composable
fun RepositoryActionsWorkflowDispatchDialog(
    workflow: RepositoryActionWorkflow,
    refOptions: List<String>,
    fallbackRef: String,
    title: String,
    inputsTitle: String,
    loadingText: String,
    notDispatchableText: String,
    noInputsText: String,
    refLabel: String,
    refHelper: String,
    cancelText: String,
    runText: String,
    notDispatchableButtonText: String,
    dispatchingWorkflowId: Long?,
    onDismiss: () -> Unit,
    onSubmit: (RepositoryActionsWorkflowDispatchRequest) -> Unit
) {
    var ref by remember(workflow.id, fallbackRef) { mutableStateOf(fallbackRef) }
    val inputValues = remember(workflow.id, workflow.dispatchInputs) {
        mutableStateMapOf<String, String>().apply {
            workflow.dispatchInputs.forEach { input -> put(input.name, input.defaultValue.orEmpty()) }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = workflow.name,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                RepositoryDropdownTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = refLabel,
                    helper = refHelper,
                    options = refOptions
                )
                Text(text = inputsTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                when {
                    !workflow.hasLoadedDispatchMetadata -> Text(text = loadingText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    !workflow.isDispatchable -> Text(text = notDispatchableText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    workflow.dispatchInputs.isEmpty() -> Text(text = noInputsText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> workflow.dispatchInputs.forEach { input ->
                        WorkflowInputField(
                            input = input,
                            value = inputValues[input.name].orEmpty(),
                            onValueChange = { inputValues[input.name] = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = workflow.hasLoadedDispatchMetadata && workflow.isDispatchable && dispatchingWorkflowId != workflow.id,
                onClick = {
                    onSubmit(
                        RepositoryActionsWorkflowDispatchRequest(
                            workflow = workflow,
                            ref = ref.ifBlank { fallbackRef },
                            inputs = workflow.dispatchInputs.mapNotNull { input ->
                                val value = inputValues[input.name].orEmpty().ifBlank { input.defaultValue.orEmpty() }
                                value.takeIf { it.isNotBlank() || input.required }?.let { input.name to it }
                            }.toMap()
                        )
                    )
                }
            ) {
                Text(text = if (workflow.isDispatchable) runText else notDispatchableButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = cancelText) }
        }
    )
}

@Composable
private fun WorkflowInputField(
    input: RepositoryActionWorkflowInput,
    value: String,
    onValueChange: (String) -> Unit
) {
    if (input.isBoolean) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = value.equals("true", ignoreCase = true),
                    onCheckedChange = { onValueChange(it.toString()) }
                )
                Text(text = input.displayLabel(), fontWeight = FontWeight.SemiBold)
            }
            input.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(text = description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    } else if (input.isChoice) {
        RepositoryDropdownTextField(
            value = value,
            onValueChange = onValueChange,
            label = input.displayLabel(),
            helper = input.description.orEmpty(),
            options = input.options
        )
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = input.displayLabel()) },
            supportingText = input.description?.takeIf { it.isNotBlank() }?.let { description ->
                { Text(text = description) }
            },
            singleLine = input.required
        )
    }
}

@Composable
private fun RepositoryDropdownTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    helper: String,
    options: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = label) },
            supportingText = helper.takeIf { it.isNotBlank() }?.let { text -> { Text(text = text) } },
            singleLine = true,
            trailingIcon = {
                if (options.isNotEmpty()) {
                    TextButton(onClick = { expanded = true }) { Text(text = "选择") }
                }
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun RepositoryActionWorkflowInput.displayLabel(): String {
    return buildString {
        append(name)
        if (required) append(" *")
    }
}
