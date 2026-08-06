package com.Sunset.REN.GitHub.domain.repo

object RepositoryUploadTargetPath {
    fun sanitize(path: String): String {
        return path.replace('\\', '/').replace(Regex("/{2,}"), "/")
    }

    fun resolve(rawTargetPath: String, displayName: String): String {
        val normalized = sanitize(rawTargetPath).trim()
        val fileName = displayName.substringAfterLast('/').ifBlank { displayName }.trim()
        if (normalized.isBlank() || normalized == "/") return fileName
        if (normalized.endsWith('/')) return "${normalized.trim('/')}/$fileName"
        return normalized.trim('/')
    }

    fun defaultDirectoryForDisplayName(displayName: String): String {
        val normalized = sanitize(displayName).trim('/')
        if ('/' !in normalized) return "/"
        val directory = normalized.substringBeforeLast('/').trim('/')
        return if (directory.isBlank()) "/" else "/$directory/"
    }

    fun normalizeDirectory(path: String): String {
        val normalized = sanitize(path).trim('/')
        return if (normalized.isBlank()) "/" else "/$normalized/"
    }

    fun normalizeTargetPathAsDirectory(path: String): String {
        val sanitized = sanitize(path).trim()
        if (sanitized.isBlank() || sanitized == "/") return "/"
        if (sanitized.endsWith('/')) return normalizeDirectory(sanitized)
        val parent = sanitized.trim('/').substringBeforeLast('/', missingDelimiterValue = "")
        return normalizeDirectory(parent)
    }

    fun expandDirectoryOptions(directoryPath: String): List<String> {
        val directorySegments = directoryPath.trim('/').split('/').filter { it.isNotBlank() }
        val options = mutableListOf("/")
        directorySegments.indices.forEach { index ->
            options += "/${directorySegments.take(index + 1).joinToString("/")}/"
        }
        return options
    }
}
