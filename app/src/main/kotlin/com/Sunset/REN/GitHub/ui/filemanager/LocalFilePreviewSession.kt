package com.Sunset.REN.GitHub.ui.filemanager

import android.net.Uri
import com.Sunset.REN.GitHub.domain.filemanager.ArchivePreview
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import com.Sunset.REN.GitHub.domain.filemanager.FileTextEncodingPolicy
import java.nio.charset.Charset

class LocalFilePreviewSession {
    var sourceUri: Uri = Uri.EMPTY
    var displayName: String = ""
    var displayPath: String = ""
    var sizeBytes: Long = -1L
    var entryType: FileEntryType = FileEntryType.Unknown
    var loadedContent: String = ""
    var loadedCharset: Charset = Charsets.UTF_8
    var loadedHadBom: Boolean = false
    var loadedLineEnding: FileTextEncodingPolicy.LineEnding = FileTextEncodingPolicy.LineEnding.Lf
    var loadedLastModifiedMillis: Long? = null
    var loadedTruncated: Boolean = false
    var isEditableMode: Boolean = false
    var isLoaded: Boolean = false
    var isSaving: Boolean = false
    var canWrite: Boolean = false
    var openMode: String = DEFAULT_OPEN_MODE
    var isMarkdownPreviewVisible: Boolean = false
    var loadedArchivePreview: ArchivePreview? = null

    private companion object {
        const val DEFAULT_OPEN_MODE = "preview"
    }
}
