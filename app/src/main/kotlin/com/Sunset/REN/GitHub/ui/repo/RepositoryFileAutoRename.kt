package com.Sunset.REN.GitHub.ui.repo

object RepositoryFileAutoRename {
    fun buildNextCopyPath(path: String): String {
        val directory = path.substringBeforeLast('/', missingDelimiterValue = "")
        val name = path.substringAfterLast('/')
        val dotIndex = name.lastIndexOf('.').takeIf { it > 0 }
        val baseName = if (dotIndex == null) name else name.substring(0, dotIndex)
        val extension = if (dotIndex == null) "" else name.substring(dotIndex)
        val copySuffixRegex = Regex(" copy(?: (\\d+))?$")
        val copyMatch = copySuffixRegex.find(baseName)
        val nextBaseName = if (copyMatch == null) {
            "$baseName copy"
        } else {
            val nextCopyNumber = copyMatch.groupValues.getOrNull(1)?.toIntOrNull()?.plus(1) ?: 2
            baseName.replaceRange(copyMatch.range, " copy $nextCopyNumber")
        }
        val renamedName = "$nextBaseName$extension"
        return if (directory.isBlank()) renamedName else "$directory/$renamedName"
    }
}
