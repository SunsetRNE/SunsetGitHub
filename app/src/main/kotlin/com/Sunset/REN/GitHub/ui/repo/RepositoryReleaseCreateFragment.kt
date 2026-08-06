package com.Sunset.REN.GitHub.ui.repo

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.common.FileSourcePickerDialog
import com.Sunset.REN.GitHub.ui.common.LocalFilePicker
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryReleaseCreateScreen

/** 创建发布版本页。 */
class RepositoryReleaseCreateFragment : Fragment() {

    private lateinit var viewModel: RepositoryReleaseCreateViewModel
    private var createState by mutableStateOf(RepositoryReleaseCreateUiState())
    private var tagName by mutableStateOf("")
    private var releaseName by mutableStateOf("")
    private var body by mutableStateOf("")
    private var prerelease by mutableStateOf(false)
    private var draft by mutableStateOf(false)
    private var makeLatest by mutableStateOf(true)
    private var tagError by mutableStateOf<String?>(null)

    private val assetPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleSelectedAsset(result.data?.data)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[RepositoryReleaseCreateViewModel::class.java]
        createState = viewModel.createState.value ?: RepositoryReleaseCreateUiState()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryReleaseCreateScreen(
                        state = createState,
                        tagName = tagName,
                        releaseName = releaseName,
                        body = body,
                        prerelease = prerelease,
                        draft = draft,
                        makeLatest = makeLatest,
                        tagError = tagError,
                        onTagNameChange = {
                            tagName = it
                            if (tagError != null && it.isNotBlank()) tagError = null
                        },
                        onReleaseNameChange = { releaseName = it },
                        onBodyChange = { body = it },
                        onPrereleaseChange = { checked ->
                            prerelease = checked
                            if (checked) {
                                draft = false
                                makeLatest = false
                            }
                        },
                        onDraftChange = { checked ->
                            draft = checked
                            if (checked) {
                                prerelease = false
                                makeLatest = false
                            }
                        },
                        onMakeLatestChange = { checked ->
                            makeLatest = checked && !draft && !prerelease
                        },
                        onSelectBranch = viewModel::selectBranch,
                        onAddAsset = ::openAssetPicker,
                        onRemoveAsset = viewModel::removeAsset,
                        onSubmit = ::submit
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.createState.observe(viewLifecycleOwner) { state -> renderState(state) }
        viewModel.prepare(
            owner = requireArguments().getString(ARG_OWNER).orEmpty(),
            repo = requireArguments().getString(ARG_REPO).orEmpty()
        )
    }

    private fun submit() {
        val normalizedTag = tagName.trim()
        if (normalizedTag.isBlank()) {
            tagError = getString(R.string.repository_release_create_tag_empty)
            return
        }
        tagError = null
        viewModel.submit(
            tagName = normalizedTag,
            targetCommitish = createState.selectedBranchName.trim(),
            name = releaseName.trim(),
            body = body.trim(),
            prerelease = prerelease,
            draft = draft,
            makeLatest = makeLatest && !draft && !prerelease
        )
    }

    private fun renderState(state: RepositoryReleaseCreateUiState) {
        createState = state
        state.createdTagName?.let { tag ->
            Toast.makeText(
                requireContext(),
                getString(R.string.repository_release_create_success, tag),
                Toast.LENGTH_SHORT
            ).show()
            findNavController().previousBackStackEntry?.savedStateHandle?.set(RESULT_RELEASE_CREATED, tag)
            findNavController().navigateUp()
        }
    }

    private fun openAssetPicker() {
        val sources = LocalFilePicker.getFileSources(requireContext())
        if (sources.isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.repository_file_upload_no_picker),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        FileSourcePickerDialog.show(
            context = requireContext(),
            title = getString(R.string.repository_release_create_asset_picker_title),
            sources = sources,
            onSourceSelected = { source -> assetPicker.launch(source.intent) }
        )
    }

    private fun handleSelectedAsset(uri: Uri?) {
        if (uri == null) return
        val cachedFile = runCatching {
            LocalFilePicker.cachePickedFile(
                context = requireContext(),
                uri = uri,
                directoryName = "release_assets"
            )
        }.getOrElse {
            Toast.makeText(
                requireContext(),
                getString(R.string.repository_release_create_asset_picker_failed),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        viewModel.addAsset(
            uri = cachedFile.uri,
            fileName = cachedFile.displayName,
            mimeType = cachedFile.mimeType,
            sizeBytes = cachedFile.sizeBytes
        )
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
        const val RESULT_RELEASE_CREATED = "repository_release_created_tag"
    }
}