package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import com.Sunset.REN.GitHub.domain.filemanager.FileSizeFormatter
import com.Sunset.REN.GitHub.domain.filemanager.FileTextEncodingPolicy
import com.Sunset.REN.GitHub.domain.filemanager.TextFormatConverter
import com.Sunset.REN.GitHub.ui.common.CompactBlackDialog
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.filemanager.LocalFilePreviewScreen
import com.Sunset.REN.GitHub.ui.editor.TextEditorAdapter
import com.Sunset.REN.GitHub.ui.editor.TextEditorConfig
import com.Sunset.REN.GitHub.ui.editor.TextSelection

class LocalFilePreviewFragment : Fragment() {
    private var previewBridge: LocalFilePreviewBridge? = null
    private lateinit var editorAdapter: TextEditorAdapter

    private lateinit var viewModel: LocalFilePreviewViewModel
    private lateinit var controllers: LocalFilePreviewControllerBundle
    private val searchController: LocalFileSearchController get() = controllers.searchController
    private val specializedPreviewRenderer: LocalFileSpecializedPreviewRenderer get() = controllers.specializedPreviewRenderer
    private val actionRenderer: LocalFilePreviewActionRenderer get() = controllers.actionRenderer
    private val editorModeController: LocalFileEditorModeController get() = controllers.editorModeController
    private val saveController: LocalFileSaveController get() = controllers.saveController
    private lateinit var previewBindingView: ComposeLocalFilePreviewActionView
    private val chromeHolder = LocalFilePreviewChromeHolder()
    private val session = LocalFilePreviewSession()
    private var sourceUri: Uri
        get() = session.sourceUri
        set(value) { session.sourceUri = value }
    private var displayName: String
        get() = session.displayName
        set(value) { session.displayName = value }
    private var displayPath: String
        get() = session.displayPath
        set(value) { session.displayPath = value }
    private var sizeBytes: Long
        get() = session.sizeBytes
        set(value) { session.sizeBytes = value }
    private var entryType: FileEntryType
        get() = session.entryType
        set(value) { session.entryType = value }
    private var loadedContent: String
        get() = session.loadedContent
        set(value) { session.loadedContent = value }
    private var loadedCharset: java.nio.charset.Charset
        get() = session.loadedCharset
        set(value) { session.loadedCharset = value }
    private var loadedHadBom: Boolean
        get() = session.loadedHadBom
        set(value) { session.loadedHadBom = value }
    private var loadedLineEnding: FileTextEncodingPolicy.LineEnding
        get() = session.loadedLineEnding
        set(value) { session.loadedLineEnding = value }
    private var loadedLastModifiedMillis: Long?
        get() = session.loadedLastModifiedMillis
        set(value) { session.loadedLastModifiedMillis = value }
    private var loadedTruncated: Boolean
        get() = session.loadedTruncated
        set(value) { session.loadedTruncated = value }
    private var isEditableMode: Boolean
        get() = session.isEditableMode
        set(value) { session.isEditableMode = value }
    private var isLoaded: Boolean
        get() = session.isLoaded
        set(value) { session.isLoaded = value }
    private var isSaving: Boolean
        get() = session.isSaving
        set(value) { session.isSaving = value }
    private var canWrite: Boolean
        get() = session.canWrite
        set(value) { session.canWrite = value }
    private var openMode: String
        get() = session.openMode
        set(value) { session.openMode = value }
    private var isMarkdownPreviewVisible: Boolean
        get() = session.isMarkdownPreviewVisible
        set(value) { session.isMarkdownPreviewVisible = value }
    private var loadedArchivePreview: com.Sunset.REN.GitHub.domain.filemanager.ArchivePreview?
        get() = session.loadedArchivePreview
        set(value) { session.loadedArchivePreview = value }
    private lateinit var backCallback: OnBackPressedCallback
    private val openArchiveExtractTarget = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { targetUri ->
        targetUri ?: return@registerForActivityResult
        runCatching {
            requireContext().contentResolver.takePersistableUriPermission(
                targetUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        viewModel.extractArchive(
            sourceUri = sourceUri,
            archiveDisplayName = displayName.ifBlank { displayPath },
            targetTreeUri = targetUri
        )
    }
    private val createTextExportDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { targetUri ->
        targetUri ?: return@registerForActivityResult
        exportCurrentTextToUri(targetUri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[LocalFilePreviewViewModel::class.java]
        readArguments()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    LocalFilePreviewScreen(
                        chromeState = chromeHolder.state,
                        chromeActions = if (::previewBindingView.isInitialized) {
                            previewBindingView.currentActions()
                        } else {
                            LocalFilePreviewChromeActions()
                        },
                        searchFocusRequestCount = chromeHolder.searchFocusRequestCount,
                        previewViewFactory = { createPreviewContentView(inflater) },
                        onDisposePreviewView = { disposePreviewContentView() }
                    )
                }
            }
        }
    }

    private fun createPreviewContentView(inflater: LayoutInflater): View {
        val bridge = LocalFilePreviewBridge.create(
            context = requireContext(),
            inflater = inflater,
            editorConfig = createEditorConfig(),
            getChromeState = { chromeHolder.state },
            setChromeState = { chromeHolder.state = it },
            requestSearchFocus = { chromeHolder.requestSearchFocus() }
        )
        previewBridge = bridge
        previewBindingView = bridge.actionView
        editorAdapter = bridge.editorAdapter
        setupControllers()
        setupEditorListeners()
        setupActionListeners()
        setupBackConfirmation()
        viewModel.state.observe(viewLifecycleOwner, ::renderViewModelState)
        renderHeader()
        loadInitialPreview()
        return bridge.rootView
    }

    private fun readArguments() {
        sourceUri = Uri.parse(arguments?.getString(ARG_SOURCE_URI).orEmpty())
        displayName = arguments?.getString(ARG_DISPLAY_NAME).orEmpty()
        displayPath = arguments?.getString(ARG_DISPLAY_PATH).orEmpty()
        sizeBytes = arguments?.getLong(ARG_SIZE_BYTES, -1L) ?: -1L
        entryType = arguments?.getString(ARG_ENTRY_TYPE)
            ?.let { value -> runCatching { FileEntryType.valueOf(value) }.getOrNull() }
            ?: FileEntryType.Unknown
        canWrite = arguments?.getBoolean(ARG_CAN_WRITE, false) == true
        openMode = arguments?.getString(ARG_OPEN_MODE, MODE_PREVIEW) ?: MODE_PREVIEW

    }

    private fun createEditorConfig(): TextEditorConfig {
        return TextEditorConfig(
            preferredEngine = LocalFileEditorConfigPolicy.resolvePreferredEditorEngine(requireContext()),
            languageMode = LocalFileEditorConfigPolicy.resolveEditorLanguageMode(displayName.ifBlank { displayPath }),
            theme = "light",
            softWrap = LocalFileEditorConfigPolicy.shouldUseSoftWrap(displayName.ifBlank { displayPath })
        )
    }

    private fun setupControllers() {
        controllers = LocalFilePreviewControllerBundle.create(
            fragment = this,
            previewView = previewBindingView,
            editorAdapter = editorAdapter,
            editorRootViewProvider = { previewBridge?.editorRootView },
            contextProvider = { requireContext() },
            state = createControllerState(),
            callbacks = createControllerCallbacks()
        )
    }

    private fun createControllerState(): LocalFilePreviewControllerState {
        return LocalFilePreviewControllerState(
            session = session,
            isSpecializedPreview = { isSpecializedPreview() },
            canPreviewAsImage = { canPreviewAsImage() },
            canPreviewAsApk = { canPreviewAsApk() },
            canPreviewAsZipArchive = { canPreviewAsZipArchive() },
            canPreviewAsMarkdown = { canPreviewAsMarkdown() }
        )
    }

    private fun createControllerCallbacks(): LocalFilePreviewControllerCallbacks {
        return LocalFilePreviewControllerCallbacks(
            modeEdit = MODE_EDIT,
            modePreview = MODE_PREVIEW,
            restoreSelection = { selection, content -> restoreSelection(selection, content) },
            setEditorTextSilently = { content -> setEditorTextSilently(content) },
            renderMarkdownContent = { content -> renderMarkdownContent(content) },
            renderPreviewModeVisibility = { renderPreviewModeVisibility() },
            renderActionState = { content -> renderActionState(content) },
            buildReadyStateText = { content -> buildReadyStateText(content) },
            shouldShowMarkdownPreviewByDefault = { shouldShowMarkdownPreviewByDefault() },
            hasUnsavedChanges = { hasUnsavedChanges() },
            backCallbackProvider = { if (::backCallback.isInitialized) backCallback else null },
            lastModifiedMillis = { uri -> viewModel.lastModifiedMillis(uri) },
            saveText = { uri, content, charset, preserveBom, lineEnding, expectedLastModifiedMillis ->
                viewModel.saveText(
                    sourceUri = uri,
                    content = content,
                    charset = charset,
                    preserveBom = preserveBom,
                    lineEnding = lineEnding,
                    expectedLastModifiedMillis = expectedLastModifiedMillis
                )
            }
        )
    }

    private fun setupEditorListeners() {
        editorAdapter.setOnTextChangedListener { content ->
            if (isLoaded && isEditableMode) {
                renderActionState(content)
                searchController.refreshStatusFromCurrentQuery()
            }
        }
        editorAdapter.setOnSelectionChangedListener {
            if (isLoaded && !isSaving) {
                if (isEditableMode) {
                    renderActionState(editorAdapter.getText())
                }
                searchController.refreshStatusFromCurrentQuery()
            }
        }
    }

    private fun setupActionListeners() {
        LocalFilePreviewActionBinder(
            actionView = previewBindingView,
            searchController = searchController,
            editorModeController = editorModeController,
            actions = LocalFilePreviewActionBinder.Actions(
                onToggleMarkdown = { toggleMarkdownPreview() },
                onConvert = { showTextConversionPicker() },
                onSaveAs = { launchTextExport() },
                onExtract = { openArchiveExtractTarget.launch(null) },
                onEdit = { enterEditMode() },
                onSave = { saveContent() },
                onArchiveClick = { showArchiveEntryPicker() }
            )
        ).bind()
    }

    private fun loadInitialPreview() {
        when {
            canPreviewAsImage() -> renderImagePreview()
            canPreviewAsApk() -> renderApkPreview()
            canExtractDocxText() -> loadDocxPreviewContent()
            canExtractPdfText() -> loadPdfPreviewContent()
            canPreviewAsZipArchive() -> renderArchivePreview()
            else -> loadPreviewContent()
        }
    }

    // Search field IME handling lives in the Compose preview chrome.

    private fun renderHeader() {
        previewBindingView.renderHeader(
            name = displayName.ifBlank { getString(R.string.local_file_preview_unknown_name) },
            path = displayPath.ifBlank { sourceUri.toString() },
            typeText = resolveTypeChipText(),
            accessText = resolveAccessPillText(),
            loadingText = getString(R.string.local_file_preview_loading)
        )
        renderActionState(editorAdapter.getText())
    }

    private fun setupBackConfirmation() {
        backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                showDiscardChangesDialog { findNavController().popBackStack() }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
    }

    fun requestNavigateUp(): Boolean {
        return if (hasUnsavedChanges()) {
            showDiscardChangesDialog { findNavController().navigateUp() }
            true
        } else {
            false
        }
    }

    private fun hasUnsavedChanges(): Boolean {
        return isEditableMode && isLoaded && !isSaving && editorAdapter.getText() != loadedContent
    }

    private fun showDiscardChangesDialog(onDiscard: () -> Unit) {
        CompactBlackDialog.show(
            context = requireContext(),
            title = getString(R.string.local_file_preview_discard_title),
            message = getString(R.string.local_file_preview_discard_message),
            negativeText = getString(android.R.string.cancel),
            positiveText = getString(R.string.local_file_preview_discard_positive),
            onPositiveClick = onDiscard
        )
    }

    private fun resolveTypeChipText(): String {
        return when (entryType) {
            FileEntryType.Directory -> getString(R.string.local_file_preview_type_directory)
            FileEntryType.Markdown -> getString(R.string.local_file_preview_type_markdown)
            FileEntryType.Code -> getString(R.string.local_file_preview_type_code)
            FileEntryType.Text -> getString(R.string.local_file_preview_type_text)
            FileEntryType.Image -> getString(R.string.local_file_preview_type_image)
            FileEntryType.Archive -> getString(R.string.local_file_preview_type_archive)
            FileEntryType.Apk -> getString(R.string.local_file_preview_type_apk)
            FileEntryType.Binary -> getString(R.string.local_file_preview_type_binary)
            FileEntryType.Parent, FileEntryType.Unknown -> getString(R.string.local_file_preview_type_unknown)
        }
    }

    private fun resolveAccessPillText(): String {
        return if (canWrite) {
            getString(R.string.local_file_preview_access_writable)
        } else {
            getString(R.string.local_file_preview_access_read_only)
        }
    }

    private fun renderImagePreview() {
        isLoaded = true
        isEditableMode = false
        isMarkdownPreviewVisible = false
        setEditorTextSilently("")
        specializedPreviewRenderer.renderImagePreview(
            sourceUri = sourceUri,
            sizeBytes = sizeBytes,
            onMissingSource = { renderError(getString(R.string.local_file_preview_missing_source)) },
            onRenderActionState = { content -> renderActionState(content) }
        )
    }

    private fun renderApkPreview() {
        if (sourceUri == Uri.EMPTY) {
            renderError(getString(R.string.local_file_preview_missing_source))
            return
        }
        prepareSpecializedPreviewLoading()
        previewBindingView.stateText = getString(R.string.local_file_preview_apk_loading)
        viewModel.loadApk(
            sourceUri = sourceUri,
            displayName = displayName.ifBlank { displayPath },
            declaredSizeBytes = sizeBytes.takeIf { it >= 0L }
        )
    }

    private fun prepareSpecializedPreviewLoading() {
        isLoaded = true
        isEditableMode = false
        loadedTruncated = false
        isMarkdownPreviewVisible = false
        specializedPreviewRenderer.prepareSpecializedPreviewLoading()
    }

    private fun renderArchivePreview() {
        if (sourceUri == Uri.EMPTY) {
            renderError(getString(R.string.local_file_preview_missing_source))
            return
        }
        prepareSpecializedPreviewLoading()
        previewBindingView.stateText = getString(R.string.local_file_preview_archive_loading)
        viewModel.loadArchive(
            sourceUri = sourceUri,
            displayName = displayName.ifBlank { displayPath },
            declaredSizeBytes = sizeBytes.takeIf { it >= 0L }
        )
    }

    private fun loadPreviewContent() {
        if (sourceUri == Uri.EMPTY) {
            renderError(getString(R.string.local_file_preview_missing_source))
            return
        }
        viewModel.loadText(
            sourceUri = sourceUri,
            displayName = displayName.ifBlank { displayPath },
            entryType = entryType,
            declaredSizeBytes = sizeBytes.takeIf { it >= 0L }
        )
    }

    private fun loadDocxPreviewContent() {
        if (sourceUri == Uri.EMPTY) {
            renderError(getString(R.string.local_file_preview_missing_source))
            return
        }
        canWrite = false
        viewModel.loadDocxText(
            sourceUri = sourceUri,
            displayName = displayName.ifBlank { displayPath },
            declaredSizeBytes = sizeBytes.takeIf { it >= 0L }
        )
    }

    private fun loadPdfPreviewContent() {
        if (sourceUri == Uri.EMPTY) {
            renderError(getString(R.string.local_file_preview_missing_source))
            return
        }
        canWrite = false
        viewModel.loadPdfText(
            sourceUri = sourceUri,
            displayName = displayName.ifBlank { displayPath },
            declaredSizeBytes = sizeBytes.takeIf { it >= 0L }
        )
    }

    private fun renderViewModelState(state: LocalFilePreviewUiState) {
        when (state) {
            LocalFilePreviewUiState.Idle -> Unit
            is LocalFilePreviewUiState.Loading -> {
                previewBindingView.stateText = when (state.kind) {
                    LoadKind.Text -> getString(R.string.local_file_preview_loading)
                    LoadKind.Archive -> getString(R.string.local_file_preview_archive_loading)
                    LoadKind.Apk -> getString(R.string.local_file_preview_apk_loading)
                }
            }
            is LocalFilePreviewUiState.TextLoaded -> renderLoadedText(state)
            is LocalFilePreviewUiState.ArchiveLoaded -> renderLoadedArchive(state.preview)
            is LocalFilePreviewUiState.ArchiveEntryTextLoaded -> renderLoadedArchiveEntryText(state.preview)
            is LocalFilePreviewUiState.ApkLoaded -> renderLoadedApk(state.preview)
            LocalFilePreviewUiState.ArchiveExtracting -> renderArchiveExtracting()
            is LocalFilePreviewUiState.ArchiveExtractSucceeded -> renderArchiveExtractSucceeded(state.summary)
            is LocalFilePreviewUiState.ArchiveExtractFailed -> renderArchiveExtractFailed(state.message)
            is LocalFilePreviewUiState.TextTooLarge -> renderError(getString(R.string.local_file_preview_too_large, FileSizeFormatter.format(state.sizeBytes)))
            is LocalFilePreviewUiState.BinaryBlocked -> renderError(state.reason)
            is LocalFilePreviewUiState.LoadFailed -> renderError(state.message.ifBlank { getString(R.string.local_file_preview_read_failed) })
            is LocalFilePreviewUiState.Saving -> {
                isSaving = true
                renderActionState(state.content)
            }
            is LocalFilePreviewUiState.SaveSucceeded -> handleSaveSucceeded(state.result)
            is LocalFilePreviewUiState.SaveFailed -> handleSaveFailed(state.message)
        }
    }

    private fun renderLoadedText(state: LocalFilePreviewUiState.TextLoaded) {
        val result = state.result
        loadedContent = result.content
        loadedCharset = result.charset
        loadedHadBom = result.hadBom
        loadedLineEnding = result.lineEnding
        sizeBytes = result.sizeBytes
        loadedLastModifiedMillis = state.lastModifiedMillis
        loadedTruncated = result.truncated
        isLoaded = true
        if (result.truncated) {
            canWrite = false
            isEditableMode = false
        }
        setEditorTextSilently(result.content)
        editorAdapter.setReadOnly(result.truncated)
        editorAdapter.scrollToTop()
        renderMarkdownContent(result.content)
        isMarkdownPreviewVisible = shouldShowMarkdownPreviewByDefault()
        renderPreviewModeVisibility()
        previewBindingView.stateText = buildReadyStateText(result.content)
        if (openMode == MODE_EDIT && !result.truncated) enterEditMode() else renderActionState(result.content)
    }

    private fun renderLoadedArchive(preview: com.Sunset.REN.GitHub.domain.filemanager.ArchivePreview) {
        sizeBytes = preview.sizeBytes ?: sizeBytes
        loadedArchivePreview = preview
        isLoaded = true
        specializedPreviewRenderer.renderLoadedArchive(preview, sizeBytes)
        renderActionState("")
    }

    private fun renderLoadedArchiveEntryText(preview: com.Sunset.REN.GitHub.domain.filemanager.ArchiveEntryTextPreview) {
        loadedContent = preview.content
        loadedCharset = preview.charset
        loadedHadBom = false
        loadedLineEnding = FileTextEncodingPolicy.LineEnding.Lf
        loadedLastModifiedMillis = null
        loadedTruncated = false
        isLoaded = true
        isEditableMode = false
        isMarkdownPreviewVisible = false
        canWrite = false
        setEditorTextSilently(preview.content)
        editorAdapter.setReadOnly(true)
        editorAdapter.scrollToTop()
        previewBindingView.hideSpecializedPreviewSurfaces()
        renderPreviewModeVisibility()
        previewBindingView.stateText = getString(
            R.string.local_file_preview_archive_entry_ready,
            preview.entryName,
            FileSizeFormatter.format(preview.sizeBytes)
        )
        renderActionState(preview.content)
    }

    private fun renderLoadedApk(preview: com.Sunset.REN.GitHub.domain.filemanager.ApkPreview) {
        sizeBytes = preview.sizeBytes ?: sizeBytes
        isLoaded = true
        specializedPreviewRenderer.renderLoadedApk(preview, sizeBytes)
        renderActionState("")
    }

    private fun renderArchiveExtracting() {
        isSaving = true
        previewBindingView.stateText = getString(R.string.local_file_preview_archive_extracting)
        renderActionState("")
    }

    private fun renderArchiveExtractSucceeded(summary: com.Sunset.REN.GitHub.domain.filemanager.ArchiveExtractSummary) {
        isSaving = false
        val message = getString(
            R.string.local_file_preview_archive_extract_success,
            summary.targetName,
            summary.fileCount,
            summary.directoryCount
        )
        previewBindingView.stateText = message
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        renderActionState("")
    }

    private fun renderArchiveExtractFailed(message: String) {
        isSaving = false
        val fallback = getString(R.string.local_file_manager_unzip_failed)
        previewBindingView.stateText = message.ifBlank { fallback }
        Toast.makeText(requireContext(), message.ifBlank { fallback }, Toast.LENGTH_LONG).show()
        renderActionState("")
    }

    private fun enterEditMode() {
        editorModeController.enterEditMode()
    }

    private fun saveContent() {
        saveController.saveContent()
    }

    private fun showTextConversionPicker() {
        if (!isLoaded || isSpecializedPreview() || !canWrite || isSaving) return
        val actions = listOf(
            TextFormatConverter.Action.LineEndingLf to getString(R.string.local_file_preview_convert_lf),
            TextFormatConverter.Action.LineEndingCrLf to getString(R.string.local_file_preview_convert_crlf),
            TextFormatConverter.Action.LineEndingCr to getString(R.string.local_file_preview_convert_cr),
            TextFormatConverter.Action.TrimTrailingWhitespace to getString(R.string.local_file_preview_convert_trim_trailing),
            TextFormatConverter.Action.EnsureFinalNewline to getString(R.string.local_file_preview_convert_ensure_final_newline),
            TextFormatConverter.Action.RemoveFinalNewline to getString(R.string.local_file_preview_convert_remove_final_newline),
            TextFormatConverter.Action.TabsToSpaces to getString(R.string.local_file_preview_convert_tabs_to_spaces),
            TextFormatConverter.Action.SpacesToTabs to getString(R.string.local_file_preview_convert_spaces_to_tabs),
            TextFormatConverter.Action.JsonPretty to getString(R.string.local_file_preview_convert_json_pretty),
            TextFormatConverter.Action.JsonCompact to getString(R.string.local_file_preview_convert_json_compact),
            TextFormatConverter.Action.UrlEncode to getString(R.string.local_file_preview_convert_url_encode),
            TextFormatConverter.Action.UrlDecode to getString(R.string.local_file_preview_convert_url_decode),
            TextFormatConverter.Action.Base64Encode to getString(R.string.local_file_preview_convert_base64_encode),
            TextFormatConverter.Action.Base64Decode to getString(R.string.local_file_preview_convert_base64_decode),
            TextFormatConverter.Action.Uppercase to getString(R.string.local_file_preview_convert_uppercase),
            TextFormatConverter.Action.Lowercase to getString(R.string.local_file_preview_convert_lowercase)
        )
        SelectionActionSheetDialog.show(
            context = requireContext(),
            title = getString(R.string.local_file_preview_convert_title),
            actions = actions.map { (action, label) ->
                SelectionActionItem(label = label) { applyTextConversion(action) }
            }
        )
    }

    private fun applyTextConversion(action: TextFormatConverter.Action) {
        val current = editorAdapter.getText()
        TextFormatConverter.convert(current, action).fold(
            onSuccess = { converted ->
                if (!isEditableMode) enterEditMode()
                setEditorTextSilently(converted)
                renderMarkdownContent(converted)
                renderActionState(converted)
                Toast.makeText(requireContext(), R.string.local_file_preview_convert_done, Toast.LENGTH_SHORT).show()
            },
            onFailure = {
                Toast.makeText(requireContext(), R.string.local_file_preview_convert_failed, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun launchTextExport() {
        if (!isLoaded || isSpecializedPreview() || isSaving) return
        createTextExportDocument.launch(defaultTextExportName())
    }

    private fun exportCurrentTextToUri(targetUri: Uri) {
        val content = editorAdapter.getText()
        runCatching {
            requireContext().contentResolver.openOutputStream(targetUri, "wt")?.use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
            } ?: error(getString(R.string.local_file_preview_save_as_failed))
        }.fold(
            onSuccess = {
                Toast.makeText(requireContext(), R.string.local_file_preview_save_as_done, Toast.LENGTH_SHORT).show()
                previewBindingView.stateText = getString(R.string.local_file_preview_save_as_done)
            },
            onFailure = {
                Toast.makeText(requireContext(), R.string.local_file_preview_save_as_failed, Toast.LENGTH_SHORT).show()
            }
        )
        renderActionState(editorAdapter.getText())
    }

    private fun defaultTextExportName(): String {
        val rawName = displayName.ifBlank { displayPath.substringAfterLast('/') }.ifBlank { getString(R.string.local_file_preview_unknown_name) }
        val baseName = rawName.substringBeforeLast('.', rawName).ifBlank { rawName }
        return if (baseName.endsWith(".txt", ignoreCase = true)) baseName else "$baseName.txt"
    }

    private fun showArchiveEntryPicker() {
        val preview = loadedArchivePreview ?: return
        val previewableEntries = preview.entries.filter { it.canPreviewText && !it.isDirectory }
        if (previewableEntries.isEmpty()) {
            Toast.makeText(requireContext(), R.string.local_file_preview_archive_no_previewable_entries, Toast.LENGTH_SHORT).show()
            return
        }
        SelectionActionSheetDialog.show(
            context = requireContext(),
            title = getString(R.string.local_file_preview_archive_open_entry_title),
            actions = previewableEntries.map { entry ->
                val sizeText = entry.sizeBytes?.let(FileSizeFormatter::format).orEmpty()
                val capability = entry.compileCapability?.let { " · ${it.language} · ${it.toolHint}" }.orEmpty()
                val label = listOf(entry.name, sizeText, capability).filter { it.isNotBlank() }.joinToString("  ")
                SelectionActionItem(label = label) {
                    viewModel.loadArchiveEntryText(
                        sourceUri = sourceUri,
                        archiveDisplayName = displayName.ifBlank { displayPath },
                        entryName = entry.name
                    )
                }
            }
        )
    }

    private fun handleSaveSucceeded(writeResult: com.Sunset.REN.GitHub.domain.filemanager.FileContentWriteResult) {
        saveController.handleSaveSucceeded(writeResult)
    }

    private fun handleSaveFailed(message: String) {
        saveController.handleSaveFailed(message)
    }

    private fun restoreSelection(selection: TextSelection, content: String) {
        editorModeController.restoreSelection(selection, content)
    }

    private fun renderActionState(content: String) {
        actionRenderer.render(content)
    }

    private fun buildReadyStateText(content: String): String {
        val displaySize = sizeBytes.takeIf { it >= 0L } ?: content.length.toLong()
        if (loadedTruncated) {
            return getString(
                R.string.local_file_preview_truncated_read_only,
                FileSizeFormatter.format(displaySize),
                FileSizeFormatter.format(content.toByteArray(loadedCharset).size.toLong())
            )
        }
        return getString(R.string.repository_file_cursor_status, 1, 1)
    }

    private fun renderError(message: String) {
        isLoaded = false
        isMarkdownPreviewVisible = false
        previewBindingView.stateText = message
        editorAdapter.setText("")
        editorAdapter.setReadOnly(true)
        previewBindingView.hideSpecializedPreviewSurfaces()
        renderPreviewModeVisibility()
        renderActionState("")
    }

    private fun setEditorTextSilently(content: String) {
        searchController.setEditorTextSilently(content)
    }

    private fun toggleMarkdownPreview() {
        if (!canPreviewAsMarkdown() || isEditableMode) return
        isMarkdownPreviewVisible = !isMarkdownPreviewVisible
        if (isMarkdownPreviewVisible) {
            renderMarkdownContent(editorAdapter.getText())
        }
        renderPreviewModeVisibility()
        renderActionState(editorAdapter.getText())
    }

    private fun renderMarkdownContent(content: String) {
        specializedPreviewRenderer.renderMarkdownContent(content)
    }

    private fun renderPreviewModeVisibility() {
        specializedPreviewRenderer.renderPreviewModeVisibility()
    }

    private fun shouldShowMarkdownPreviewByDefault(): Boolean {
        return LocalFilePreviewTypePolicy.shouldShowMarkdownPreviewByDefault(openMode, entryType, displayName, displayPath)
    }

    private fun canPreviewAsMarkdown(): Boolean {
        return LocalFilePreviewTypePolicy.canPreviewAsMarkdown(entryType, displayName, displayPath)
    }

    private fun isSpecializedPreview(): Boolean {
        return LocalFilePreviewTypePolicy.isSpecializedPreview(entryType, displayName, displayPath)
    }

    private fun canPreviewAsImage(): Boolean {
        return LocalFilePreviewTypePolicy.canPreviewAsImage(entryType, displayName, displayPath)
    }

    private fun canPreviewAsZipArchive(): Boolean {
        return LocalFilePreviewTypePolicy.canPreviewAsZipArchive(entryType, displayName, displayPath)
    }

    private fun canExtractDocxText(): Boolean {
        return LocalFilePreviewTypePolicy.canExtractDocxText(displayName, displayPath)
    }

    private fun canExtractPdfText(): Boolean {
        return LocalFilePreviewTypePolicy.canExtractPdfText(displayName, displayPath)
    }

    private fun canPreviewAsApk(): Boolean {
        return LocalFilePreviewTypePolicy.canPreviewAsApk(entryType, displayName, displayPath)
    }

    private fun FileTextEncodingPolicy.LineEnding.displayLabel(): String {
        return when (this) {
            FileTextEncodingPolicy.LineEnding.Lf -> "LF"
            FileTextEncodingPolicy.LineEnding.CrLf -> "CRLF"
            FileTextEncodingPolicy.LineEnding.Cr -> "CR"
            FileTextEncodingPolicy.LineEnding.Mixed -> "Mixed EOL"
            FileTextEncodingPolicy.LineEnding.None -> "No EOL"
        }
    }

    private fun disposePreviewContentView() {
        previewBridge?.dispose()
        previewBridge = null
    }

    override fun onDestroyView() {
        disposePreviewContentView()
        super.onDestroyView()
    }

    companion object {
        const val ARG_SOURCE_URI = "source_uri"
        const val ARG_DISPLAY_NAME = "display_name"
        const val ARG_DISPLAY_PATH = "display_path"
        const val ARG_SIZE_BYTES = "size_bytes"
        const val ARG_ENTRY_TYPE = "entry_type"
        const val ARG_CAN_WRITE = "can_write"
        const val ARG_OPEN_MODE = "open_mode"
        const val MODE_PREVIEW = "preview"
        const val MODE_EDIT = "edit"
    }
}