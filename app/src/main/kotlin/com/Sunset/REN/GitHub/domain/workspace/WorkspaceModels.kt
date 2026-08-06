package com.Sunset.REN.GitHub.domain.workspace

/**
 * 应用内工作区只描述 App 私有目录中的可同步项目。
 * 外部文件、文件夹或压缩包必须先导入到工作区，再参与上传/同步。
 */
data class WorkspaceProject(
    val id: String,
    val name: String,
    val rootPath: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val remoteBinding: WorkspaceRemoteBinding? = null
)

data class WorkspaceRemoteBinding(
    val owner: String,
    val repo: String,
    val branch: String,
    val remotePath: String = ""
) {
    val repositoryFullName: String
        get() = "$owner/$repo"

    /** GitHub tree API 使用不带首尾斜杠的仓库内路径；空字符串表示仓库根目录。 */
    val normalizedRemotePath: String
        get() = remotePath.toNormalizedRepositoryPath()
}

fun String.toNormalizedRepositoryPath(): String {
    return trim()
        .replace('\\', '/')
        .split('/')
        .filter { it.isNotBlank() && it != "." }
        .joinToString("/")
}

data class WorkspaceFile(
    val workspaceId: String,
    val relativePath: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val sha256: String,
    val status: WorkspaceFileStatus = WorkspaceFileStatus.Unchanged
)

enum class WorkspaceFileStatus {
    Unchanged,
    Added,
    Modified,
    Deleted,
    Renamed,
    Conflict,
    Ignored
}

data class WorkspaceSnapshot(
    val workspaceId: String,
    val files: List<WorkspaceFile>,
    val createdAtMillis: Long
)

data class WorkspaceScanOptions(
    val includeIgnored: Boolean = false,
    val detectSensitiveFiles: Boolean = true,
    val maxFileSizeBytes: Long? = null
)

data class WorkspaceScanResult(
    val workspaceId: String,
    val files: List<WorkspaceFile>,
    val ignoredPaths: List<String> = emptyList(),
    val sensitivePaths: List<SensitiveWorkspaceFile> = emptyList()
)

data class SensitiveWorkspaceFile(
    val relativePath: String,
    val reason: String,
    val severity: SensitiveFileSeverity
)

enum class SensitiveFileSeverity {
    Warning,
    Blocking
}

data class WorkspaceImportRequest(
    val workspaceId: String,
    val sources: List<WorkspaceImportSource>,
    val targetDirectory: String = "",
    val options: WorkspaceImportOptions = WorkspaceImportOptions()
)

sealed class WorkspaceImportSource {
    data class ContentUri(
        val uri: String,
        val displayName: String? = null,
        val mimeType: String? = null,
        val sizeBytes: Long? = null
    ) : WorkspaceImportSource()

    data class AppInternalPath(
        val absolutePath: String
    ) : WorkspaceImportSource()
}

data class WorkspaceImportOptions(
    val overwriteExisting: Boolean = false,
    val preserveDirectoryStructure: Boolean = true,
    val skipHiddenFiles: Boolean = true,
    val applyIgnoreRules: Boolean = true,
    val detectSensitiveFiles: Boolean = true
)

data class WorkspaceImportResult(
    val workspaceId: String,
    val importedFiles: List<WorkspaceImportedFile>,
    val skippedFiles: List<WorkspaceSkippedFile> = emptyList(),
    val sensitiveFiles: List<SensitiveWorkspaceFile> = emptyList()
)

data class WorkspaceImportedFile(
    val sourceDescription: String,
    val relativePath: String,
    val sizeBytes: Long
)

data class WorkspaceSkippedFile(
    val sourceDescription: String,
    val reason: String
)
