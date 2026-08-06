package com.Sunset.REN.GitHub.domain.filemanager.capability

data class FileManagerCapabilitySet(
    val canOpen: Boolean = false,
    val canPreview: Boolean = false,
    val canRename: Boolean = false,
    val canDelete: Boolean = false,
    val canCopy: Boolean = false,
    val canMove: Boolean = false,
    val canCompress: Boolean = false,
    val canExtract: Boolean = false,
    val canCreateChild: Boolean = false,
    val canEditPermission: Boolean = false,
    val canEditOwner: Boolean = false
)
