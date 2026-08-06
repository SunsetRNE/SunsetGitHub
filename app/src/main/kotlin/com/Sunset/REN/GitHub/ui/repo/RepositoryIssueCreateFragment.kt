package com.Sunset.REN.GitHub.ui.repo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.RepositoryLabel
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

class RepositoryIssueCreateFragment : Fragment() {

    private lateinit var viewModel: RepositoryIssueCreateViewModel
    private var repositoryOwner: String = ""
    private var repositoryName: String = ""
    private var uiState by mutableStateOf(RepositoryIssueCreateUiState())
    private var draftTitle by mutableStateOf("")
    private var draftBody by mutableStateOf("")
    private var titleError by mutableStateOf<String?>(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[RepositoryIssueCreateViewModel::class.java]
        repositoryOwner = requireArguments().getString(ARG_OWNER).orEmpty()
        repositoryName = requireArguments().getString(ARG_REPO).orEmpty()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryIssueCreateScreen(
                        contextText = buildContextText(),
                        state = uiState,
                        titleError = titleError,
                        onDraftChanged = { title, body ->
                            draftTitle = title
                            draftBody = body
                            if (title.isNotBlank()) titleError = null
                        },
                        onLabelsChanged = viewModel::switchLabels,
                        onCreatedIssue = ::openCreatedIssue
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.createState.observe(viewLifecycleOwner) { state ->
            uiState = state
            activity?.invalidateOptionsMenu()
        }
        viewModel.prepare(repositoryOwner, repositoryName)
    }

    private fun submit() {
        val title = draftTitle.trim()
        val body = draftBody.trim()
        titleError = if (title.isBlank()) getString(R.string.repository_issues_create_title_empty) else null
        if (title.isBlank()) return
        viewModel.submit(title, body)
    }

    private fun openCreatedIssue(number: Int) {
        findNavController().previousBackStackEntry?.savedStateHandle?.set(RESULT_ISSUE_CREATED, number)
        findNavController().navigateUp()
    }

    private fun buildContextText(): String {
        return if (repositoryOwner.isBlank() || repositoryName.isBlank()) {
            getString(R.string.repository_issues_create_title)
        } else {
            "$repositoryOwner/$repositoryName"
        }
    }

    fun submitFromToolbar() {
        if (viewModel.createState.value?.isSubmitting == true) return
        submit()
    }

    fun isSubmittingIssue(): Boolean {
        return viewModel.createState.value?.isSubmitting == true
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
        const val RESULT_ISSUE_CREATED = "repository_issue_created_number"
    }
}

@Composable
private fun RepositoryIssueCreateScreen(
    contextText: String,
    state: RepositoryIssueCreateUiState,
    titleError: String?,
    onDraftChanged: (String, String) -> Unit,
    onLabelsChanged: (List<String>) -> Unit,
    onCreatedIssue: (Int) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(title, body) { onDraftChanged(title, body) }
    LaunchedEffect(state.createdIssueNumber) {
        state.createdIssueNumber?.let(onCreatedIssue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        RepositoryIssueContextStrip(contextText = contextText)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.repository_issues_create_section_title), color = colors.textPrimary, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.repository_issues_create_title_count, title.length, 256), color = colors.textMuted, style = MaterialTheme.typography.labelMedium)
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(256) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.repository_issues_create_title_hint)) },
                    isError = titleError != null,
                    supportingText = { titleError?.let { Text(it) } },
                    enabled = !state.isSubmitting,
                    singleLine = true
                )
                IssueLabelsSection(state = state, onLabelsChanged = onLabelsChanged)
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.repository_issues_create_body_hint)) },
                    minLines = 9,
                    enabled = !state.isSubmitting,
                    supportingText = { Text(stringResource(R.string.repository_issue_create_chip_markdown)) }
                )
            }
        }
        if (!state.errorMessage.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.repository_issues_create_failed, state.errorMessage),
                color = colors.danger,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun RepositoryIssueContextStrip(contextText: String) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("#", color = colors.textMuted, fontWeight = FontWeight.Bold)
        Text(contextText, color = colors.textPrimary, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.repository_issue_create_kind_issue), color = colors.textMuted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun IssueLabelsSection(state: RepositoryIssueCreateUiState, onLabelsChanged: (List<String>) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.repository_issues_create_labels_section), color = colors.textPrimary, fontWeight = FontWeight.Bold)
            Text(
                text = when {
                    state.isLoadingLabels -> stringResource(R.string.repository_issues_create_labels_loading)
                    state.availableLabels.isEmpty() -> stringResource(R.string.repository_issue_detail_labels_empty)
                    state.selectedLabels.isEmpty() -> stringResource(R.string.repository_issues_create_labels_empty)
                    else -> "已选择 ${state.selectedLabels.size} 个标签"
                },
                color = colors.textMuted,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            state.availableLabels.forEach { label ->
                IssueLabelChip(
                    label = label,
                    selected = state.selectedLabels.contains(label.name),
                    enabled = !state.isSubmitting,
                    onClick = {
                        val next = if (state.selectedLabels.contains(label.name)) {
                            state.selectedLabels - label.name
                        } else {
                            state.selectedLabels + label.name
                        }
                        onLabelsChanged(next)
                    }
                )
            }
        }
        if (!state.labelErrorMessage.isNullOrBlank()) {
            Text(state.labelErrorMessage, color = colors.attention, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun IssueLabelChip(label: RepositoryLabel, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(IssueLabelDisplayNames.displayName(label.name)) }
    )
}
