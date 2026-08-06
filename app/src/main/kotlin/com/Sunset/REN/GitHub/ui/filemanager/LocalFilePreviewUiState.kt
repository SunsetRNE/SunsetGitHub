package com.Sunset.REN.GitHub.ui.filemanager

import com.Sunset.REN.GitHub.domain.filemanager.ApkPreview
import com.Sunset.REN.GitHub.domain.filemanager.ArchiveEntryTextPreview
import com.Sunset.REN.GitHub.domain.filemanager.ArchiveExtractSummary
import com.Sunset.REN.GitHub.domain.filemanager.ArchivePreview
import com.Sunset.REN.GitHub.domain.filemanager.FileContentReadResult
import com.Sunset.REN.GitHub.domain.filemanager.FileContentWriteResult

sealed interface LocalFilePreviewUiState {
    data object Idle : LocalFilePreviewUiState
    data class Loading(val kind: LoadKind) : LocalFilePreviewUiState
    data class TextLoaded(val result: FileContentReadResult.Text, val lastModifiedMillis: Long?) : LocalFilePreviewUiState
    data class ArchiveLoaded(val preview: ArchivePreview) : LocalFilePreviewUiState
    data class ArchiveEntryTextLoaded(val preview: ArchiveEntryTextPreview) : LocalFilePreviewUiState
    data class ApkLoaded(val preview: ApkPreview) : LocalFilePreviewUiState
    data object ArchiveExtracting : LocalFilePreviewUiState
    data class ArchiveExtractSucceeded(val summary: ArchiveExtractSummary) : LocalFilePreviewUiState
    data class ArchiveExtractFailed(val message: String) : LocalFilePreviewUiState
    data class LoadFailed(val message: String) : LocalFilePreviewUiState
    data class TextTooLarge(val sizeBytes: Long, val limitBytes: Long) : LocalFilePreviewUiState
    data class BinaryBlocked(val reason: String) : LocalFilePreviewUiState
    data class Saving(val content: String) : LocalFilePreviewUiState
    data class SaveSucceeded(val result: FileContentWriteResult) : LocalFilePreviewUiState
    data class SaveFailed(val message: String) : LocalFilePreviewUiState
}

enum class LoadKind {
    Text,
    Archive,
    Apk
}
