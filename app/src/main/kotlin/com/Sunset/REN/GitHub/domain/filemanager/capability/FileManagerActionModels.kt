package com.Sunset.REN.GitHub.domain.filemanager.capability

enum class FileManagerActionId {
    Open,
    Preview,
    Rename,
    Delete,
    Copy,
    Move,
    Compress,
    Extract,
    ConvertToText,
    CreateFile,
    CreateDirectory,
    Search,
    Sort,
    ToggleHidden,
    DualPane,
    Refresh,
    JumpPath,
    AuthorizeSaf,
    EnableRoot,
    OpenRootPath,
    EditPermission,
    EditOwner,
    Properties
}

data class FileManagerActionUiModel(
    val id: FileManagerActionId,
    val title: String,
    val iconRes: Int? = null,
    val visible: Boolean,
    val enabled: Boolean,
    val disabledReason: String? = null
)

data class FileManagerActionSet(
    val primary: List<FileManagerActionUiModel>,
    val overflow: List<FileManagerActionUiModel>,
    val selection: List<FileManagerActionUiModel>
) {
    fun all(): List<FileManagerActionUiModel> = primary + overflow + selection
}
