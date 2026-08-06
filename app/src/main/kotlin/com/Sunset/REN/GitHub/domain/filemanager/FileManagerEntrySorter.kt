package com.Sunset.REN.GitHub.domain.filemanager

object FileManagerEntrySorter {
    fun sort(entries: Iterable<FileManagerEntry>): List<FileManagerEntry> {
        return sort(entries, FileManagerSortMode.Name)
    }

    fun sort(
        entries: Iterable<FileManagerEntry>,
        mode: FileManagerSortMode,
        reverse: Boolean = false
    ): List<FileManagerEntry> {
        val sorted = entries.sortedWith(comparatorFor(mode))
        if (!reverse) return sorted

        val parents = sorted.filter { it.type == FileEntryType.Parent }
        val regularEntries = sorted.filterNot { it.type == FileEntryType.Parent }.asReversed()
        return parents + regularEntries
    }

    fun filterAndSort(entries: Iterable<FileManagerEntry>, options: FileManagerListOptions): List<FileManagerEntry> {
        return sort(
            entries.filter { entry ->
                options.showHiddenFiles || entry.type == FileEntryType.Parent || !entry.name.startsWith('.')
            },
            options.sortMode,
            options.reverse
        )
    }

    val EntryComparator: Comparator<FileManagerEntry> = comparatorFor(FileManagerSortMode.Name)

    private fun comparatorFor(mode: FileManagerSortMode): Comparator<FileManagerEntry> {
        val base = compareBy<FileManagerEntry> { it.type != FileEntryType.Parent }
            .thenBy { it.type != FileEntryType.Directory }
        return when (mode) {
            FileManagerSortMode.Name -> base.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            FileManagerSortMode.Time -> base.thenByDescending { it.modifiedAtMillis ?: Long.MIN_VALUE }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            FileManagerSortMode.Size -> base.thenByDescending { it.sizeBytes ?: -1L }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            FileManagerSortMode.Type -> base.thenBy { it.type.name }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        }
    }
}