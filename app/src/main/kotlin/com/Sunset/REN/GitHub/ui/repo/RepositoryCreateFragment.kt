package com.Sunset.REN.GitHub.ui.repo

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.common.CompactOptionPickerDialog
import com.Sunset.REN.GitHub.ui.common.showComposeDialog
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryConfirmDialog

class RepositoryCreateFragment : Fragment() {
    private lateinit var viewModel: RepositoryCreateViewModel
    private var composeRootView: View? = null
    private var hasOpenedCreatedRepository = false
    private var hasUserEditedForm = false
    private var isInitializingForm = true
    private var isSubmitting = false
    private var shouldOpenReadmeEditorAfterCreate = false
    private var selectedGitignoreTemplateIndex = 0
    private var selectedLicenseTemplateIndex = 0
    private lateinit var backCallback: OnBackPressedCallback

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(this)[RepositoryCreateViewModel::class.java]
        setupBackConfirmation()
        isInitializingForm = false
        return ComposeView(requireContext()).apply {
            this@RepositoryCreateFragment.composeRootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    var uiState by remember { mutableStateOf(viewModel.uiState.value ?: RepositoryCreateUiState.Idle) }
                    var gitignoreIndex by remember { mutableStateOf(selectedGitignoreTemplateIndex) }
                    var licenseIndex by remember { mutableStateOf(selectedLicenseTemplateIndex) }
                    DisposableEffect(viewLifecycleOwner) {
                        val observer = Observer<RepositoryCreateUiState> { state ->
                            uiState = state
                            renderState(state)
                        }
                        viewModel.uiState.observe(viewLifecycleOwner, observer)
                        onDispose { viewModel.uiState.removeObserver(observer) }
                    }
                    RepositoryCreateScreen(
                        uiState = uiState,
                        gitignoreLabel = GitignoreTemplates[gitignoreIndex].label,
                        licenseLabel = LicenseTemplates[licenseIndex].label,
                        onDirty = ::markFormEdited,
                        onPickGitignore = {
                            showTemplatePicker(getString(R.string.repository_create_gitignore_section), "选择一个初始化模板", GitignoreTemplates.map { it.label }, gitignoreIndex, "搜索模板，例如 Android / Kotlin") { index ->
                                selectedGitignoreTemplateIndex = index
                                gitignoreIndex = index
                                markFormEdited()
                            }
                        },
                        onPickLicense = {
                            showTemplatePicker(getString(R.string.repository_create_license_section), "选择仓库许可证", LicenseTemplates.map { it.label }, licenseIndex, "搜索许可证，例如 MIT / Apache") { index ->
                                selectedLicenseTemplateIndex = index
                                licenseIndex = index
                                markFormEdited()
                            }
                        },
                        initialFilesHint = { initializationHint(gitignoreIndex, licenseIndex) },
                        onSubmit = { form -> submitRepository(form, gitignoreIndex, licenseIndex) }
                    )
                }
            }
        }
    }

    private fun showTemplatePicker(title: String, subtitle: String, options: List<CharSequence>, selectedIndex: Int, searchHint: String, onSelected: (Int) -> Unit) {
        CompactOptionPickerDialog.show(
            context = requireContext(),
            title = title,
            options = options,
            selectedIndex = selectedIndex,
            subtitle = subtitle,
            iconText = "⌄",
            searchHint = searchHint,
            onOptionSelected = onSelected
        )
    }

    private fun markFormEdited() {
        if (!isInitializingForm) {
            hasUserEditedForm = true
            if (::backCallback.isInitialized) backCallback.isEnabled = shouldConfirmDiscard()
        }
    }

    private fun setupBackConfirmation() {
        backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() = showDiscardConfirmation()
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
    }

    fun requestNavigateUp(): Boolean = if (shouldConfirmDiscard()) {
        showDiscardConfirmation(); true
    } else false

    private fun shouldConfirmDiscard(): Boolean = hasUserEditedForm && !isSubmitting && !hasOpenedCreatedRepository

    private fun showDiscardConfirmation() {
        showComposeDialog(requireContext()) { dismiss ->
            RepositoryConfirmDialog(
                title = getString(R.string.repository_create_discard_title),
                message = getString(R.string.repository_create_discard_message),
                confirmText = getString(R.string.repository_create_discard_confirm),
                dismissText = getString(R.string.repository_create_discard_cancel),
                onDismiss = dismiss,
                onConfirm = {
                    dismiss(); hasUserEditedForm = false; backCallback.isEnabled = false; findNavController().navigateUp()
                }
            )
        }
    }

    private fun submitRepository(form: RepositoryCreateFormState, gitignoreIndex: Int, licenseIndex: Int) {
        hideKeyboard()
        shouldOpenReadmeEditorAfterCreate = form.createReadme
        viewModel.createRepository(
            name = form.name,
            description = form.description,
            homepage = form.homepage,
            isPrivate = form.isPrivate,
            autoInit = form.createReadme,
            gitignoreTemplate = GitignoreTemplates.getOrNull(gitignoreIndex)?.apiValue,
            licenseTemplate = LicenseTemplates.getOrNull(licenseIndex)?.apiValue,
            hasIssues = form.hasIssues,
            hasProjects = form.hasProjects,
            hasWiki = form.hasWiki
        )
    }

    private fun renderState(state: RepositoryCreateUiState) {
        isSubmitting = state is RepositoryCreateUiState.Submitting
        if (::backCallback.isInitialized) backCallback.isEnabled = shouldConfirmDiscard()
        when (state) {
            RepositoryCreateUiState.SignedOut -> Toast.makeText(requireContext(), R.string.repository_create_signed_out, Toast.LENGTH_SHORT).show()
            is RepositoryCreateUiState.ValidationError -> { Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show(); viewModel.consumeTransientState() }
            is RepositoryCreateUiState.Error -> { Toast.makeText(requireContext(), getString(R.string.repository_create_failed, state.message), Toast.LENGTH_SHORT).show(); viewModel.consumeTransientState() }
            is RepositoryCreateUiState.Success -> openCreatedRepository(state)
            else -> Unit
        }
    }

    private fun openCreatedRepository(state: RepositoryCreateUiState.Success) {
        if (hasOpenedCreatedRepository) return
        hasOpenedCreatedRepository = true
        hasUserEditedForm = false
        if (::backCallback.isInitialized) backCallback.isEnabled = false
        val repository = state.repository
        val destinationId = if (shouldOpenReadmeEditorAfterCreate) R.id.repository_file_edit_fragment else R.id.repository_detail_fragment
        val arguments = Bundle().apply {
            if (shouldOpenReadmeEditorAfterCreate) {
                putString(RepositoryFileEditFragment.ARG_OWNER, repository.ownerLogin)
                putString(RepositoryFileEditFragment.ARG_REPO, repository.name)
                putString(RepositoryFileEditFragment.ARG_PATH, DefaultReadmePath)
                putString(RepositoryFileEditFragment.ARG_NAME, DefaultReadmePath)
                putString(RepositoryFileEditFragment.ARG_INITIAL_COMMIT_MESSAGE, getString(R.string.repository_create_readme_commit_message))
            } else {
                putString(RepositoryDetailFragment.ARG_OWNER, repository.ownerLogin)
                putString(RepositoryDetailFragment.ARG_REPO, repository.name)
                putString(RepositoryDetailFragment.ARG_FULL_NAME, repository.fullName)
            }
        }
        findNavController().navigate(destinationId, arguments, NavOptions.Builder().setPopUpTo(R.id.repository_create_fragment, true).build())
    }

    private fun initializationHint(gitignoreIndex: Int, licenseIndex: Int): String {
        val selectedInitialFiles = buildList {
            GitignoreTemplates.getOrNull(gitignoreIndex)?.takeIf { it.apiValue != null }?.label?.let { add(it) }
            LicenseTemplates.getOrNull(licenseIndex)?.takeIf { it.apiValue != null }?.label?.let { add(it) }
        }
        return if (selectedInitialFiles.isEmpty()) getString(R.string.repository_create_readme_desc)
        else getString(R.string.repository_create_initial_files_hint, selectedInitialFiles.joinToString("、"))
    }

    private fun hideKeyboard() {
        val manager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        composeRootView?.windowToken?.let { manager?.hideSoftInputFromWindow(it, 0) }
    }

    override fun onDestroyView() { composeRootView = null; super.onDestroyView() }

    private data class TemplateOption(val label: String, val apiValue: String?)

    private companion object {
        const val DefaultReadmePath = "README.md"
        val GitignoreTemplates = listOf(
            TemplateOption("无", null), TemplateOption("Android", "Android"), TemplateOption("Kotlin", "Kotlin"), TemplateOption("Java", "Java"), TemplateOption("Gradle", "Gradle"), TemplateOption("Maven", "Maven"), TemplateOption("Node", "Node"), TemplateOption("Python", "Python"), TemplateOption("Go", "Go"), TemplateOption("Rust", "Rust"), TemplateOption("C", "C"), TemplateOption("C++", "C++"), TemplateOption("CMake", "CMake"), TemplateOption("Swift", "Swift"), TemplateOption("Dart", "Dart"), TemplateOption("Flutter/Dart", "Dart"), TemplateOption("Ruby", "Ruby"), TemplateOption("Rails", "Rails"), TemplateOption("PHP", "Composer"), TemplateOption("Laravel", "Laravel"), TemplateOption("Unity", "Unity"), TemplateOption("Unreal Engine", "UnrealEngine"), TemplateOption("Visual Studio", "VisualStudio"), TemplateOption("Objective-C", "Objective-C"), TemplateOption("R", "R"), TemplateOption("Scala", "Scala"), TemplateOption("Haskell", "Haskell"), TemplateOption("Erlang", "Erlang"), TemplateOption("Elixir", "Elixir"), TemplateOption("Clojure", "Clojure"), TemplateOption("Lua", "Lua"), TemplateOption("Perl", "Perl"), TemplateOption("Julia", "Julia"), TemplateOption("TeX", "TeX"), TemplateOption("Terraform", "Terraform"), TemplateOption("WordPress", "WordPress"), TemplateOption("Jekyll", "Jekyll"), TemplateOption("Godot", "Godot"), TemplateOption("Xcode", "Swift")
        )
        val LicenseTemplates = listOf(
            TemplateOption("无", null), TemplateOption("MIT License", "mit"), TemplateOption("Apache License 2.0", "apache-2.0"), TemplateOption("GNU GPL v3.0", "gpl-3.0"), TemplateOption("GNU GPL v2.0", "gpl-2.0"), TemplateOption("GNU AGPL v3.0", "agpl-3.0"), TemplateOption("GNU LGPL v2.1", "lgpl-2.1"), TemplateOption("Mozilla Public License 2.0", "mpl-2.0"), TemplateOption("BSD 2-Clause \"Simplified\"", "bsd-2-clause"), TemplateOption("BSD 3-Clause \"New/Revised\"", "bsd-3-clause"), TemplateOption("Boost Software License 1.0", "bsl-1.0"), TemplateOption("Eclipse Public License 2.0", "epl-2.0"), TemplateOption("Creative Commons Zero v1.0", "cc0-1.0"), TemplateOption("The Unlicense", "unlicense")
        )
    }
}

private data class RepositoryCreateFormState(val name: String, val description: String, val homepage: String, val isPrivate: Boolean, val createReadme: Boolean, val hasIssues: Boolean, val hasProjects: Boolean, val hasWiki: Boolean)

@Composable
private fun RepositoryCreateScreen(uiState: RepositoryCreateUiState, gitignoreLabel: String, licenseLabel: String, onDirty: () -> Unit, onPickGitignore: () -> Unit, onPickLicense: () -> Unit, initialFilesHint: () -> String, onSubmit: (RepositoryCreateFormState) -> Unit) {
    val context = LocalContext.current
    val colors = SunsetGitHubThemeTokens.colors
    val isSubmitting = uiState is RepositoryCreateUiState.Submitting
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var homepage by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var createReadme by remember { mutableStateOf(false) }
    var hasIssues by remember { mutableStateOf(true) }
    var hasProjects by remember { mutableStateOf(true) }
    var hasWiki by remember { mutableStateOf(true) }
    fun dirty(block: () -> Unit) { block(); onDirty() }

    Column(Modifier.fillMaxSize().background(colors.canvas).verticalScroll(rememberScrollState()).padding(14.dp).padding(bottom = 24.dp)) {
        RepositoryCreateHero(repositoryCreateStatusText(context, uiState))
        Spacer(Modifier.height(12.dp))
        FormCard {
            SectionTitle(context.getString(R.string.repository_create_basic_info_section))
            LabeledField(context.getString(R.string.repository_create_name_section))
            OutlinedTextField(name, { dirty { name = it.take(100) } }, Modifier.fillMaxWidth(), enabled = !isSubmitting, singleLine = true, placeholder = { Text(context.getString(R.string.repository_create_name_hint)) })
            LabeledField(context.getString(R.string.repository_create_description_section))
            OutlinedTextField(description, { dirty { description = it.take(350) } }, Modifier.fillMaxWidth(), enabled = !isSubmitting, minLines = 2, maxLines = 3, placeholder = { Text(context.getString(R.string.repository_create_description_hint)) })
            LabeledField(context.getString(R.string.repository_create_homepage_section))
            OutlinedTextField(homepage, { dirty { homepage = it.take(512) } }, Modifier.fillMaxWidth(), enabled = !isSubmitting, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), placeholder = { Text(context.getString(R.string.repository_create_homepage_hint)) })
            MetaText(context.getString(R.string.repository_create_homepage_desc))
            DividerSpacer()
            SectionTitle(context.getString(R.string.repository_create_visibility_section))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VisibilityOption(Modifier.weight(1f), context.getString(R.string.repository_create_visibility_public), context.getString(R.string.repository_create_visibility_public_desc), !isPrivate, !isSubmitting) { dirty { isPrivate = false } }
                VisibilityOption(Modifier.weight(1f), context.getString(R.string.repository_create_visibility_private), context.getString(R.string.repository_create_visibility_private_desc), isPrivate, !isSubmitting) { dirty { isPrivate = true } }
            }
            DividerSpacer()
            SectionTitle(context.getString(R.string.repository_create_initialize_section))
            CheckRow(context.getString(R.string.repository_create_readme), createReadme, !isSubmitting) { dirty { createReadme = it } }
            MetaText(initialFilesHint())
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TemplatePicker(Modifier.weight(1f), context.getString(R.string.repository_create_gitignore_section), "$gitignoreLabel  ⌄", !isSubmitting, onPickGitignore)
                TemplatePicker(Modifier.weight(1f), context.getString(R.string.repository_create_license_section), "$licenseLabel  ⌄", !isSubmitting, onPickLicense)
            }
            DividerSpacer()
            SectionTitle(context.getString(R.string.repository_create_features_section))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                CompactCheck(Modifier.weight(1f), context.getString(R.string.repository_create_feature_issues), hasIssues, !isSubmitting) { dirty { hasIssues = it } }
                CompactCheck(Modifier.weight(1f), context.getString(R.string.repository_create_feature_projects), hasProjects, !isSubmitting) { dirty { hasProjects = it } }
                CompactCheck(Modifier.weight(1f), context.getString(R.string.repository_create_feature_wiki), hasWiki, !isSubmitting) { dirty { hasWiki = it } }
            }
            MetaText(context.getString(R.string.repository_create_feature_desc))
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onSubmit(RepositoryCreateFormState(name, description, homepage, isPrivate, createReadme, hasIssues, hasProjects, hasWiki)) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !isSubmitting,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24292F))
        ) {
            if (isSubmitting) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(8.dp)) }
            Text(context.getString(if (isSubmitting) R.string.repository_create_submitting else R.string.repository_create_submit), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun RepositoryCreateHero(statusText: String) { val c = LocalContext.current; Card(colors = CardDefaults.cardColors(Color.Transparent), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.background(Brush.linearGradient(listOf(Color(0xFF24292F), Color(0xFF57606A)))).padding(16.dp)) { Badge(c.getString(R.string.repository_create_current_owner_badge), true); Text(c.getString(R.string.repository_create_page_title), Modifier.padding(top = 10.dp), Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(c.getString(R.string.repository_create_page_subtitle), Modifier.padding(top = 6.dp), Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodyMedium); Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Badge(c.getString(R.string.repository_create_required_name_badge), false); Badge(statusText, true) } } } }
@Composable private fun FormCard(content: @Composable () -> Unit) { Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), color = SunsetGitHubThemeTokens.colors.surface, border = BorderStroke(1.dp, SunsetGitHubThemeTokens.colors.border)) { Column(Modifier.padding(14.dp)) { content() } } }
@Composable private fun SectionTitle(text: String) = Text(text, Modifier.padding(top = 4.dp, bottom = 8.dp), SunsetGitHubThemeTokens.colors.textPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
@Composable private fun LabeledField(text: String) = Text(text, Modifier.padding(top = 8.dp, bottom = 5.dp), SunsetGitHubThemeTokens.colors.textPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
@Composable private fun MetaText(text: String) = Text(text, Modifier.padding(top = 5.dp), SunsetGitHubThemeTokens.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
@Composable private fun DividerSpacer() = Spacer(Modifier.fillMaxWidth().padding(top = 13.dp, bottom = 13.dp).height(1.dp).background(SunsetGitHubThemeTokens.colors.border))
@Composable private fun Badge(text: String, dark: Boolean) { Surface(shape = RoundedCornerShape(999.dp), color = if (dark) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.86f)) { Text(text, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), if (dark) Color.White else Color(0xFF24292F), fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
@Composable private fun VisibilityOption(modifier: Modifier, title: String, description: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) { Surface(modifier.height(112.dp).clickable(enabled, onClick = onClick), RoundedCornerShape(14.dp), color = if (selected) Color(0xFFEFF6FF) else SunsetGitHubThemeTokens.colors.surface, border = BorderStroke(1.dp, if (selected) Color(0xFF0969DA) else SunsetGitHubThemeTokens.colors.border)) { Column(Modifier.padding(10.dp)) { RadioButton(selected, onClick, enabled = enabled); Text(title, fontWeight = FontWeight.Bold, color = SunsetGitHubThemeTokens.colors.textPrimary); Text(description, Modifier.padding(top = 3.dp), SunsetGitHubThemeTokens.colors.textSecondary, style = MaterialTheme.typography.bodySmall) } } }
@Composable private fun CheckRow(text: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) { Surface(Modifier.fillMaxWidth().height(44.dp).clickable(enabled) { onCheckedChange(!checked) }, RoundedCornerShape(12.dp), color = Color(0xFFF6F8FA), border = BorderStroke(1.dp, SunsetGitHubThemeTokens.colors.border)) { Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked, onCheckedChange, enabled = enabled); Text(text, color = SunsetGitHubThemeTokens.colors.textPrimary, fontWeight = FontWeight.Bold) } } }
@Composable private fun CompactCheck(modifier: Modifier, text: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) { Surface(modifier.height(42.dp).clickable(enabled) { onCheckedChange(!checked) }, RoundedCornerShape(12.dp), color = Color(0xFFF6F8FA), border = BorderStroke(1.dp, SunsetGitHubThemeTokens.colors.border)) { Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked, onCheckedChange, Modifier.size(32.dp), enabled = enabled); Text(text, color = SunsetGitHubThemeTokens.colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold) } } }
@Composable private fun TemplatePicker(modifier: Modifier, title: String, value: String, enabled: Boolean, onClick: () -> Unit) { Column(modifier) { LabeledField(title); Surface(Modifier.fillMaxWidth().height(42.dp).clickable(enabled, onClick = onClick), RoundedCornerShape(12.dp), color = SunsetGitHubThemeTokens.colors.surface, border = BorderStroke(1.dp, SunsetGitHubThemeTokens.colors.border)) { Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(value, color = SunsetGitHubThemeTokens.colors.textPrimary, fontWeight = FontWeight.Bold) } } } }
private fun repositoryCreateStatusText(context: Context, state: RepositoryCreateUiState): String = when (state) { RepositoryCreateUiState.Idle -> context.getString(R.string.repository_create_ready); RepositoryCreateUiState.Submitting -> context.getString(R.string.repository_create_submitting); RepositoryCreateUiState.SignedOut -> context.getString(R.string.repository_create_signed_out); is RepositoryCreateUiState.ValidationError -> state.message; is RepositoryCreateUiState.Error -> context.getString(R.string.repository_create_failed, state.message); is RepositoryCreateUiState.Success -> context.getString(R.string.repository_create_success, state.repository.fullName) }
