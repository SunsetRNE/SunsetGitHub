package com.Sunset.REN.GitHub.domain.filemanager

/**
 * Low-risk operation preferences that mirror MT-style expert toggles.
 * Potentially destructive privileged actions remain disabled elsewhere; these
 * settings only describe local file behavior and install preference ordering.
 */
data class FileManagerOperationSettings(
    val backupBeforeTextSave: Boolean = true,
    val preserveModifiedTimeOnCopy: Boolean = true,
    val preserveModifiedTimeOnExtract: Boolean = true
)

data class FileManagerApkInstallSettings(
    val strategy: ApkInstallStrategy = ApkInstallStrategy.SystemPackageInstaller
)

enum class ApkInstallStrategy {
    SystemPackageInstaller,
    Shizuku,
    Dhizuku,
    Root
}

data class FileManagerMenuOrderSettings(
    val fileMenuOrder: FileManagerMenuOrder = FileManagerMenuOrder.MtClassic,
    val openWithOrder: FileManagerOpenWithOrder = FileManagerOpenWithOrder.SystemDefault
)

enum class FileManagerMenuOrder {
    MtClassic,
    SafeFirst,
    FrequentFirst
}

enum class FileManagerOpenWithOrder {
    SystemDefault,
    TextFirst,
    PreviewFirst
}
