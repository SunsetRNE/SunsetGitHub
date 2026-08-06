package com.Sunset.REN.GitHub.ui.repo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

class RepositoryForkFragment : Fragment() {

    private lateinit var viewModel: RepositoryForkViewModel
    private var owner: String = ""
    private var repo: String = ""
    private var uiState by mutableStateOf<RepositoryForkUiState>(RepositoryForkUiState.Loading)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[RepositoryForkViewModel::class.java]
        owner = requireArguments().getString(ARG_OWNER).orEmpty()
        repo = requireArguments().getString(ARG_REPO).orEmpty()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryForkScreen(
                        state = uiState,
                        onDraftChanged = viewModel::updateDraft,
                        onSubmit = viewModel::createFork,
                        onOpenExisting = ::openRepository,
                        onConsumeCreated = ::consumeCreatedFork
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.forkState.observe(viewLifecycleOwner) { state -> uiState = state }
        if (owner.isBlank() || repo.isBlank()) {
            uiState = RepositoryForkUiState.Error(getString(R.string.repository_fork_source_missing))
        } else {
            viewModel.prepare(owner, repo)
        }
    }

    private fun consumeCreatedFork(repository: GitHubRepository) {
        viewModel.consumeCreatedFork()
        Toast.makeText(requireContext(), getString(R.string.repository_detail_fork_success), Toast.LENGTH_SHORT).show()
        openRepository(repository)
    }

    private fun openRepository(repository: GitHubRepository) {
        findNavController().navigate(
            R.id.repository_detail_fragment,
            Bundle().apply {
                putString(RepositoryDetailFragment.ARG_OWNER, repository.ownerLogin)
                putString(RepositoryDetailFragment.ARG_REPO, repository.name)
                putString(RepositoryDetailFragment.ARG_FULL_NAME, repository.fullName)
            }
        )
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
    }
}

@Composable
private fun RepositoryForkScreen(
    state: RepositoryForkUiState,
    onDraftChanged: (String, String, String) -> Unit,
    onSubmit: (String, String, String, Boolean) -> Unit,
    onOpenExisting: (GitHubRepository) -> Unit,
    onConsumeCreated: (GitHubRepository) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.repository_fork_page_title), color = colors.textPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.repository_fork_page_subtitle), color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
        Text(stringResource(R.string.repository_fork_required_hint), color = colors.textMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 16.dp))
        Spacer(modifier = Modifier.height(12.dp))
        when (state) {
            RepositoryForkUiState.Loading -> ForkStateCard(stringResource(R.string.repository_detail_loading), isError = false)
            RepositoryForkUiState.SignedOut -> ForkStateCard(stringResource(R.string.profile_signed_out), isError = true)
            is RepositoryForkUiState.Error -> ForkStateCard(state.message, isError = true)
            is RepositoryForkUiState.Content -> RepositoryForkContent(
                state = state,
                onDraftChanged = onDraftChanged,
                onSubmit = onSubmit,
                onOpenExisting = onOpenExisting,
                onConsumeCreated = onConsumeCreated
            )
        }
    }
}

@Composable
private fun RepositoryForkContent(
    state: RepositoryForkUiState.Content,
    onDraftChanged: (String, String, String) -> Unit,
    onSubmit: (String, String, String, Boolean) -> Unit,
    onOpenExisting: (GitHubRepository) -> Unit,
    onConsumeCreated: (GitHubRepository) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    var seeded by rememberSaveable(state.sourceRepository.fullName) { mutableStateOf(false) }
    var targetOwner by rememberSaveable(state.sourceRepository.fullName) { mutableStateOf("") }
    var targetName by rememberSaveable(state.sourceRepository.fullName) { mutableStateOf("") }
    var description by rememberSaveable(state.sourceRepository.fullName) { mutableStateOf("") }
    var defaultBranchOnly by rememberSaveable(state.sourceRepository.fullName) { mutableStateOf(false) }

    LaunchedEffect(state.sourceRepository.fullName, state.currentAccountLogin) {
        if (!seeded) {
            targetOwner = state.currentAccountLogin
            targetName = state.sourceRepository.name
            description = state.sourceRepository.description.orEmpty()
            seeded = true
            onDraftChanged(targetOwner, targetName, description)
        }
    }

    LaunchedEffect(state.createdFork) {
        state.createdFork?.let(onConsumeCreated)
    }

    val defaultBranch = state.sourceRepository.defaultBranch.ifBlank { "main" }
    val message = when {
        state.eligibilityError != null -> state.eligibilityError
        state.existingFork != null -> stringResource(R.string.repository_fork_existing_message, state.existingFork.fullName)
        state.isCheckingExistingFork -> "正在检查当前账号是否已有该仓库的 Fork……"
        state.isCreating -> stringResource(R.string.repository_fork_submit_loading)
        state.errorMessage != null -> state.errorMessage
        else -> "可以创建 Fork。"
    }

    ForkStateCard(message = message, isError = state.eligibilityError != null || state.errorMessage != null)
    Spacer(modifier = Modifier.height(12.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.repository_fork_source_section), color = colors.textMuted, style = MaterialTheme.typography.labelMedium)
            Text(state.sourceRepository.fullName, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("默认分支 $defaultBranch", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)

            OutlinedTextField(
                value = targetOwner,
                onValueChange = { value ->
                    targetOwner = value
                    onDraftChanged(targetOwner, targetName, description)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.repository_fork_target_owner_section)) },
                placeholder = { Text(stringResource(R.string.repository_fork_target_owner_hint)) },
                enabled = !state.isCreating
            )
            OutlinedTextField(
                value = targetName,
                onValueChange = { value ->
                    targetName = value
                    onDraftChanged(targetOwner, targetName, description)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.repository_fork_target_name_section)) },
                placeholder = { Text(stringResource(R.string.repository_fork_target_name_hint)) },
                supportingText = {
                    Text(nameAvailabilityMessage(state, targetName).orEmpty())
                },
                enabled = !state.isCreating
            )
            Text(stringResource(R.string.repository_fork_name_helper), color = colors.textMuted, style = MaterialTheme.typography.labelMedium)
            OutlinedTextField(
                value = description,
                onValueChange = { value ->
                    description = value.take(350)
                    onDraftChanged(targetOwner, targetName, description)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.repository_fork_description_section)) },
                placeholder = { Text(stringResource(R.string.repository_fork_description_hint)) },
                supportingText = { Text(stringResource(R.string.repository_fork_description_count, description.length)) },
                minLines = 2,
                enabled = !state.isCreating
            )
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = defaultBranchOnly, onCheckedChange = { defaultBranchOnly = it }, enabled = !state.isCreating)
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(stringResource(R.string.repository_fork_default_branch_only, defaultBranch), color = colors.textPrimary)
                    Text(stringResource(R.string.repository_fork_default_branch_only_desc, state.sourceRepository.fullName), color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(
                stringResource(
                    if (targetOwner.equals(state.currentAccountLogin, ignoreCase = true)) R.string.repository_fork_account_notice_personal else R.string.repository_fork_account_notice_org,
                    targetOwner.ifBlank { state.currentAccountLogin }
                ),
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelMedium
            )
            Button(
                onClick = { onSubmit(targetOwner.trim(), targetName.trim(), description, defaultBranchOnly) },
                enabled = state.canCreateFork && !state.isCreating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(if (state.isCreating) R.string.repository_fork_submit_loading else R.string.repository_fork_submit))
            }
            state.existingFork?.let { existingFork ->
                OutlinedButton(onClick = { onOpenExisting(existingFork) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.repository_fork_open_existing))
                }
            }
        }
    }
}

@Composable
private fun ForkStateCard(message: String, isError: Boolean) {
    val colors = SunsetGitHubThemeTokens.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Text(
            text = message,
            color = if (isError) colors.danger else colors.textSecondary,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun nameAvailabilityMessage(state: RepositoryForkUiState.Content, name: String): String? {
    return when {
        name.isBlank() -> null
        state.isCheckingName -> "正在检查 $name 是否可用……"
        state.isNameAvailable == true -> stringResource(R.string.repository_fork_name_available, name)
        state.isNameAvailable == false -> stringResource(R.string.repository_fork_name_unavailable, name)
        state.nameCheckError != null -> state.nameCheckError
        else -> null
    }
}
