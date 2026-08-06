package com.Sunset.REN.GitHub.ui.filemanager

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.filemanager.FileContentAccessRepository
import com.Sunset.REN.GitHub.domain.filemanager.FileContentReadResult
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import com.Sunset.REN.GitHub.domain.filemanager.FileTextEncodingPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.Charset

class LocalFilePreviewViewModel(application: Application) : AndroidViewModel(application) {
    private val contentAccessRepository = FileContentAccessRepository(application)
    private val _state = MutableLiveData<LocalFilePreviewUiState>(LocalFilePreviewUiState.Idle)
    val state: LiveData<LocalFilePreviewUiState> = _state

    fun loadText(
        sourceUri: Uri,
        displayName: String,
        entryType: FileEntryType,
        declaredSizeBytes: Long?
    ) {
        _state.value = LocalFilePreviewUiState.Loading(LoadKind.Text)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                contentAccessRepository.readText(
                    sourceUri = sourceUri,
                    displayName = displayName,
                    entryType = entryType,
                    declaredSizeBytes = declaredSizeBytes
                )
            }
            _state.value = textReadState(result, sourceUri)
        }
    }

    fun loadDocxText(sourceUri: Uri, displayName: String, declaredSizeBytes: Long?) {
        _state.value = LocalFilePreviewUiState.Loading(LoadKind.Text)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                contentAccessRepository.readDocxText(sourceUri, displayName, declaredSizeBytes)
            }
            _state.value = textReadState(result, sourceUri)
        }
    }

    fun loadPdfText(sourceUri: Uri, displayName: String, declaredSizeBytes: Long?) {
        _state.value = LocalFilePreviewUiState.Loading(LoadKind.Text)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                contentAccessRepository.readPdfText(sourceUri, displayName, declaredSizeBytes)
            }
            _state.value = textReadState(result, sourceUri)
        }
    }

    private suspend fun textReadState(result: FileContentReadResult, sourceUri: Uri): LocalFilePreviewUiState {
        return when (result) {
            is FileContentReadResult.Text -> LocalFilePreviewUiState.TextLoaded(
                result = result,
                lastModifiedMillis = withContext(Dispatchers.IO) { contentAccessRepository.lastModifiedMillis(sourceUri) }
            )
            is FileContentReadResult.TooLarge -> LocalFilePreviewUiState.TextTooLarge(result.sizeBytes, result.limitBytes)
            is FileContentReadResult.BinaryBlocked -> LocalFilePreviewUiState.BinaryBlocked(result.reason)
            is FileContentReadResult.Failed -> LocalFilePreviewUiState.LoadFailed(result.message)
        }
    }

    fun loadArchive(sourceUri: Uri, displayName: String, declaredSizeBytes: Long?) {
        _state.value = LocalFilePreviewUiState.Loading(LoadKind.Archive)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                contentAccessRepository.readZipPreview(sourceUri, displayName, declaredSizeBytes)
            }
            _state.value = result.fold(
                onSuccess = { LocalFilePreviewUiState.ArchiveLoaded(it) },
                onFailure = { LocalFilePreviewUiState.LoadFailed(it.message.orEmpty()) }
            )
        }
    }

    fun loadApk(sourceUri: Uri, displayName: String, declaredSizeBytes: Long?) {
        _state.value = LocalFilePreviewUiState.Loading(LoadKind.Apk)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                contentAccessRepository.readApkPreview(sourceUri, displayName, declaredSizeBytes)
            }
            _state.value = result.fold(
                onSuccess = { LocalFilePreviewUiState.ApkLoaded(it) },
                onFailure = { LocalFilePreviewUiState.LoadFailed(it.message.orEmpty()) }
            )
        }
    }

    fun loadArchiveEntryText(sourceUri: Uri, archiveDisplayName: String, entryName: String) {
        _state.value = LocalFilePreviewUiState.Loading(LoadKind.Text)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                contentAccessRepository.readArchiveEntryText(sourceUri, archiveDisplayName, entryName)
            }
            _state.value = result.fold(
                onSuccess = { LocalFilePreviewUiState.ArchiveEntryTextLoaded(it) },
                onFailure = { LocalFilePreviewUiState.LoadFailed(it.message.orEmpty()) }
            )
        }
    }

    fun extractArchive(sourceUri: Uri, archiveDisplayName: String, targetTreeUri: Uri) {
        _state.value = LocalFilePreviewUiState.ArchiveExtracting
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                contentAccessRepository.extractArchive(sourceUri, archiveDisplayName, targetTreeUri)
            }
            _state.value = result.fold(
                onSuccess = { LocalFilePreviewUiState.ArchiveExtractSucceeded(it) },
                onFailure = { LocalFilePreviewUiState.ArchiveExtractFailed(it.message.orEmpty()) }
            )
        }
    }

    fun lastModifiedMillis(sourceUri: Uri): Long? = contentAccessRepository.lastModifiedMillis(sourceUri)

    fun saveText(
        sourceUri: Uri,
        content: String,
        charset: Charset,
        preserveBom: Boolean,
        lineEnding: FileTextEncodingPolicy.LineEnding,
        expectedLastModifiedMillis: Long?
    ) {
        _state.value = LocalFilePreviewUiState.Saving(content)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                contentAccessRepository.writeText(
                    sourceUri = sourceUri,
                    content = content,
                    charset = charset,
                    preserveBom = preserveBom,
                    lineEnding = lineEnding,
                    expectedLastModifiedMillis = expectedLastModifiedMillis
                )
            }
            _state.value = result.fold(
                onSuccess = { LocalFilePreviewUiState.SaveSucceeded(it) },
                onFailure = { LocalFilePreviewUiState.SaveFailed(it.message.orEmpty()) }
            )
        }
    }
}
