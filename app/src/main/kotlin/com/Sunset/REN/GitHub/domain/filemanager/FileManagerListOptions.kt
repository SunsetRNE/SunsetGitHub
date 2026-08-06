package com.Sunset.REN.GitHub.domain.filemanager

/**
 * Stable list presentation choices for a file manager pane.
 *
 * Left and right panes may keep independent instances of this value so sorting
 * and filtering changes can apply only to the pane the user is operating on.
 */
data class FileManagerListOptions(
    val sortMode: FileManagerSortMode = FileManagerSortMode.Name,
    val reverse: Boolean = false,
    /** System hidden files/directories, i.e. entries whose name starts with '.'. */
    val showHiddenFiles: Boolean = false,
    /** App-level manual hidden rules selected from the file manager UI. */
    val showManualHiddenFiles: Boolean = false,
    /** MT-style compact year, e.g. 24-07-21 instead of 2024-07-21. */
    val useShortYear: Boolean = true,
    /** Include seconds in the metadata timestamp. */
    val showSeconds: Boolean = false,
    /** Show a concise capability/permission marker in metadata. */
    val showPermissions: Boolean = false
)

enum class FileManagerSortMode {
    Name,
    Time,
    Size,
    Type
}

data class FileManagerSearchOptions(
    val query: String,
    val includeSubdirectories: Boolean = true,
    val includeFiles: Boolean = true,
    val includeDirectories: Boolean = true,
    val caseSensitive: Boolean = false,
    val includeHiddenFiles: Boolean = false
) {
    val normalizedQuery: String = query.trim()

    fun hasValidTargetType(): Boolean = includeFiles || includeDirectories

    fun matches(entry: FileManagerEntry): Boolean {
        if (entry.type == FileEntryType.Parent) return false
        if (!includeHiddenFiles && entry.name.startsWith('.')) return false
        if (!matchesType(entry)) return false
        if (normalizedQuery.isBlank()) return true
        return entry.name.contains(normalizedQuery, ignoreCase = !caseSensitive) ||
            entry.displayPath.contains(normalizedQuery, ignoreCase = !caseSensitive)
    }

    fun matchesType(entry: FileManagerEntry): Boolean {
        return when (entry.type) {
            FileEntryType.Parent -> false
            FileEntryType.Directory -> includeDirectories
            else -> includeFiles
        }
    }
}
