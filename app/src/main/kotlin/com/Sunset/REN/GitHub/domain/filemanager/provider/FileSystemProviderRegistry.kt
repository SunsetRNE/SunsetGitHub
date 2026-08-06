package com.Sunset.REN.GitHub.domain.filemanager.provider

import com.Sunset.REN.GitHub.domain.filemanager.path.FileManagerPath
import com.Sunset.REN.GitHub.domain.filemanager.path.FileSystemProviderId

class FileSystemProviderRegistry(
    providers: List<FileSystemProvider>
) {
    private val byId = providers.associateBy { it.id }

    fun providerFor(path: FileManagerPath): FileSystemProvider? {
        val id = when (path) {
            is FileManagerPath.Local -> FileSystemProviderId.Local
            is FileManagerPath.Saf -> FileSystemProviderId.Saf
            is FileManagerPath.Archive -> FileSystemProviderId.Archive
            is FileManagerPath.Root -> FileSystemProviderId.Root
        }
        return byId[id]
    }

    fun provider(id: FileSystemProviderId): FileSystemProvider? = byId[id]
}
