package com.Sunset.REN.GitHub.domain.filemanager

object FileEntryCapabilityResolver {
    fun resolve(
        type: FileEntryType,
        isFile: Boolean,
        isDirectory: Boolean,
        canRead: Boolean,
        canWrite: Boolean
    ): FileEntryCapabilities {
        return FileEntryCapabilities(
            canRead = canRead,
            canWrite = canWrite,
            canRename = canWrite,
            canDelete = canWrite,
            canCreateChild = isDirectory && canWrite,
            canUpload = isFile && canRead,
            canAccessContent = FileContentAccessPolicy.canAccessContent(
                type = type,
                isFile = isFile,
                canRead = canRead
            ),
            canEditAsText = FileContentAccessPolicy.canEditAsText(
                type = type,
                isFile = isFile,
                canRead = canRead
            )
        )
    }
}