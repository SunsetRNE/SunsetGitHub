package com.Sunset.REN.GitHub.domain.workspace

import java.util.Locale

/**
 * Shared policy for workspace import, scan, upload, and sync safety checks.
 */
object WorkspaceSensitiveFilePolicy {
    private val blockingFileNames = setOf(
        ".env",
        ".env.local",
        ".env.production",
        ".npmrc",
        ".pypirc",
        "id_rsa",
        "id_dsa",
        "id_ecdsa",
        "id_ed25519",
        "known_hosts",
        "local.properties",
        "local.properties.bak",
        "google-services.json",
        "google-services.plist",
        "keystore.properties",
        "gradle.properties.local"
    )

    private val blockingExtensions = setOf(
        "jks",
        "keystore",
        "p12",
        "pfx",
        "pem",
        "key",
        "mobileprovision"
    )

    private val warningDirectorySegments = setOf(
        ".git",
        ".gradle",
        ".kotlin",
        ".idea",
        ".backup",
        "build",
        "captures",
        "node_modules",
        "__pycache__"
    )

    private val warningExtensions = setOf(
        "apk",
        "aab",
        "apks",
        "ipa",
        "log",
        "tmp",
        "bak"
    )

    fun inspect(relativePath: String): SensitiveWorkspaceFile? {
        val normalizedPath = relativePath.toNormalizedRepositoryPath()
        if (normalizedPath.isBlank()) return null

        val segments = normalizedPath.split('/').filter { it.isNotBlank() }
        val fileName = segments.lastOrNull().orEmpty()
        val lowerFileName = fileName.lowercase(Locale.ROOT)
        val extension = lowerFileName.substringAfterLast('.', missingDelimiterValue = "")
        val lowerSegments = segments.map { it.lowercase(Locale.ROOT) }

        blockingReasonFor(lowerFileName, extension)?.let { reason ->
            return SensitiveWorkspaceFile(
                relativePath = normalizedPath,
                reason = reason,
                severity = SensitiveFileSeverity.Blocking
            )
        }

        warningReasonFor(lowerSegments, lowerFileName, extension)?.let { reason ->
            return SensitiveWorkspaceFile(
                relativePath = normalizedPath,
                reason = reason,
                severity = SensitiveFileSeverity.Warning
            )
        }

        return null
    }

    fun inspectAll(relativePaths: Iterable<String>): List<SensitiveWorkspaceFile> {
        return relativePaths.mapNotNull(::inspect)
    }

    private fun blockingReasonFor(fileName: String, extension: String): String? {
        return when {
            fileName in blockingFileNames -> "包含本地配置、令牌或账号相关文件。"
            fileName.endsWith(".env") || fileName.contains(".env.") -> "包含环境变量配置文件。"
            fileName.contains("secret") || fileName.contains("token") || fileName.contains("credential") -> "文件名疑似包含密钥、令牌或凭据。"
            extension in blockingExtensions -> "包含证书、私钥或签名文件。"
            else -> null
        }
    }

    private fun warningReasonFor(segments: List<String>, fileName: String, extension: String): String? {
        return when {
            segments.any { it in warningDirectorySegments } -> "包含本地缓存、构建目录或版本控制内部文件。"
            extension in warningExtensions -> "包含构建产物、日志或临时备份文件。"
            fileName.endsWith("~") -> "包含编辑器临时备份文件。"
            else -> null
        }
    }
}
