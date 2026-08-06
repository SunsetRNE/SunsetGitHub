package com.Sunset.REN.GitHub.data.workspace

import com.Sunset.REN.GitHub.domain.workspace.SensitiveFileSeverity
import com.Sunset.REN.GitHub.domain.workspace.SensitiveWorkspaceFile
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceFile
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceFileStatus
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceScanOptions
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceScanResult
import com.Sunset.REN.GitHub.domain.workspace.toNormalizedRepositoryPath
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale

/**
 * 扫描 App 私有目录中的工作区文件。
 *
 * 该类不申请也不直接访问外部存储；外部内容应先复制/导入到工作区根目录。
 */
class AppInternalWorkspaceFileScanner(
    private val ignoreMatcher: WorkspaceIgnoreMatcher = WorkspaceIgnoreMatcher.default(),
    private val sensitiveFileDetector: SensitiveFileDetector = SensitiveFileDetector.default()
) {
    fun scan(
        workspaceId: String,
        rootDirectory: File,
        options: WorkspaceScanOptions = WorkspaceScanOptions()
    ): WorkspaceScanResult {
        require(rootDirectory.exists()) { "工作区目录不存在：${rootDirectory.absolutePath}" }
        require(rootDirectory.isDirectory) { "工作区路径不是目录：${rootDirectory.absolutePath}" }

        val rootCanonical = rootDirectory.canonicalFile
        val files = mutableListOf<WorkspaceFile>()
        val ignoredPaths = mutableListOf<String>()
        val sensitivePaths = mutableListOf<SensitiveWorkspaceFile>()

        rootCanonical.walkTopDown()
            .onEnter { directory ->
                if (directory == rootCanonical) return@onEnter true
                val relativePath = directory.toWorkspaceRelativePath(rootCanonical)
                val ignored = ignoreMatcher.shouldIgnore(relativePath, isDirectory = true)
                if (ignored) {
                    ignoredPaths += ensureDirectorySuffix(relativePath)
                }
                !ignored || options.includeIgnored
            }
            .filter { it.isFile }
            .forEach { file ->
                val relativePath = file.toWorkspaceRelativePath(rootCanonical)
                val ignored = ignoreMatcher.shouldIgnore(relativePath, isDirectory = false)
                if (ignored) {
                    ignoredPaths += relativePath
                    if (!options.includeIgnored) return@forEach
                }

                val sizeBytes = file.length()
                val overSizeLimit = options.maxFileSizeBytes?.let { sizeBytes > it } == true
                val status = if (ignored || overSizeLimit) WorkspaceFileStatus.Ignored else WorkspaceFileStatus.Unchanged
                if (overSizeLimit) {
                    ignoredPaths += relativePath
                }

                if (options.detectSensitiveFiles) {
                    sensitiveFileDetector.detect(relativePath)?.let { sensitivePaths += it }
                }

                if (!overSizeLimit || options.includeIgnored) {
                    files += WorkspaceFile(
                        workspaceId = workspaceId,
                        relativePath = relativePath,
                        sizeBytes = sizeBytes,
                        lastModifiedMillis = file.lastModified(),
                        sha256 = file.sha256Hex(),
                        status = status
                    )
                }
            }

        return WorkspaceScanResult(
            workspaceId = workspaceId,
            files = files.sortedBy { it.relativePath },
            ignoredPaths = ignoredPaths.distinct().sorted(),
            sensitivePaths = sensitivePaths.distinctBy { it.relativePath }.sortedBy { it.relativePath }
        )
    }

    private fun File.toWorkspaceRelativePath(rootDirectory: File): String {
        val relative = rootDirectory.toPath().relativize(canonicalFile.toPath()).toString()
        return relative.toNormalizedRepositoryPath()
    }

    private fun ensureDirectorySuffix(path: String): String {
        return if (path.endsWith('/')) path else "$path/"
    }
}

class WorkspaceIgnoreMatcher(
    private val rules: List<WorkspaceIgnoreRule>
) {
    fun shouldIgnore(relativePath: String, isDirectory: Boolean): Boolean {
        val normalizedPath = relativePath.toNormalizedRepositoryPath()
        if (normalizedPath.isBlank()) return false
        val normalizedWithDirectorySuffix = if (isDirectory) "$normalizedPath/" else normalizedPath
        return rules.any { it.matches(normalizedPath, normalizedWithDirectorySuffix, isDirectory) }
    }

    companion object {
        fun default(): WorkspaceIgnoreMatcher {
            return WorkspaceIgnoreMatcher(DefaultIgnorePatterns.map(::WorkspaceIgnoreRule))
        }
    }
}

class WorkspaceIgnoreRule(pattern: String) {
    private val normalizedPattern = pattern.trim().replace('\\', '/')
    private val directoryOnly = normalizedPattern.endsWith('/')
    private val patternBody = normalizedPattern.trim('/').lowercase(Locale.ROOT)

    fun matches(relativePath: String, relativeDirectoryPath: String, isDirectory: Boolean): Boolean {
        if (patternBody.isBlank()) return false
        if (directoryOnly && !isDirectory && !relativePath.contains('/')) return false

        val target = if (isDirectory) relativeDirectoryPath.trim('/').lowercase(Locale.ROOT) else relativePath.lowercase(Locale.ROOT)
        val fileName = target.substringAfterLast('/')

        return when {
            patternBody.startsWith("*.") -> fileName.endsWith(patternBody.removePrefix("*"))
            '/' in patternBody -> target == patternBody || target.startsWith("$patternBody/")
            directoryOnly -> target == patternBody || target.contains("/$patternBody") || target.startsWith("$patternBody/")
            else -> fileName == patternBody || target == patternBody || target.startsWith("$patternBody/") || target.contains("/$patternBody/")
        }
    }
}

class SensitiveFileDetector(
    private val rules: List<SensitiveFileRule>
) {
    fun detect(relativePath: String): SensitiveWorkspaceFile? {
        val normalizedPath = relativePath.toNormalizedRepositoryPath()
        val lowerPath = normalizedPath.lowercase(Locale.ROOT)
        val fileName = lowerPath.substringAfterLast('/')
        return rules.firstOrNull { it.matches(lowerPath, fileName) }?.toSensitiveFile(normalizedPath)
    }

    companion object {
        fun default(): SensitiveFileDetector {
            return SensitiveFileDetector(DefaultSensitiveFileRules)
        }
    }
}

data class SensitiveFileRule(
    val pattern: String,
    val reason: String,
    val severity: SensitiveFileSeverity = SensitiveFileSeverity.Blocking
) {
    private val normalizedPattern = pattern.trim().replace('\\', '/').lowercase(Locale.ROOT)

    fun matches(lowerPath: String, lowerFileName: String): Boolean {
        return when {
            normalizedPattern.startsWith("*.") -> lowerFileName.endsWith(normalizedPattern.removePrefix("*"))
            '/' in normalizedPattern -> lowerPath == normalizedPattern || lowerPath.endsWith("/$normalizedPattern")
            else -> lowerFileName == normalizedPattern
        }
    }

    fun toSensitiveFile(relativePath: String): SensitiveWorkspaceFile {
        return SensitiveWorkspaceFile(
            relativePath = relativePath,
            reason = reason,
            severity = severity
        )
    }
}

private fun File.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    FileInputStream(this).use { input ->
        val buffer = ByteArray(HashBufferSize)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private const val HashBufferSize = 64 * 1024

private val DefaultIgnorePatterns = listOf(
    ".git/",
    ".gradle/",
    ".idea/",
    ".kotlin/",
    ".backup/",
    ".DS_Store",
    "build/",
    "app/build/",
    "node_modules/",
    "dist/",
    "target/",
    "*.apk",
    "*.aab",
    "*.class",
    "*.dex",
    "*.tmp",
    "*.log",
    "local.properties",
    "local.properties.bak"
)

private val DefaultSensitiveFileRules = listOf(
    SensitiveFileRule(".env", "环境变量文件可能包含 token、密钥或私有配置。"),
    SensitiveFileRule(".env.local", "本地环境变量文件可能包含 token、密钥或私有配置。"),
    SensitiveFileRule("local.properties", "Android local.properties 通常包含本机 SDK 路径或 OAuth 本地配置。"),
    SensitiveFileRule("local.properties.bak", "local.properties 备份文件可能包含本机路径或敏感配置。"),
    SensitiveFileRule("secrets.properties", "secrets.properties 通常包含密钥或私有配置。"),
    SensitiveFileRule("github.properties", "GitHub 配置文件可能包含 OAuth 或 token 信息。", SensitiveFileSeverity.Warning),
    SensitiveFileRule("google-services.json", "google-services.json 可能包含项目私有配置，上传前请确认目标仓库可见性。", SensitiveFileSeverity.Warning),
    SensitiveFileRule("*.jks", "Java keystore 文件不应上传到公开仓库。"),
    SensitiveFileRule("*.keystore", "签名 keystore 文件不应上传到公开仓库。"),
    SensitiveFileRule("id_rsa", "SSH 私钥不应上传。"),
    SensitiveFileRule("id_ed25519", "SSH 私钥不应上传。"),
    SensitiveFileRule("id_ecdsa", "SSH 私钥不应上传。")
)