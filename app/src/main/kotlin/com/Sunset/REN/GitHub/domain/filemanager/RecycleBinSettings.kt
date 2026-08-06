package com.Sunset.REN.GitHub.domain.filemanager

data class RecycleBinSettings(
    val enabled: Boolean = true,
    val defaultMoveToRecycleBin: Boolean = true,
    val autoCleanDays: Int = 0,
    val showDeletionWarning: Boolean = true
)