package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.data.github.html.RepositoryActionsCacheItem
import com.Sunset.REN.GitHub.data.github.html.RepositoryActionsSecretItem
import com.Sunset.REN.GitHub.data.github.html.RepositoryActionsSettingsSnapshot
import com.Sunset.REN.GitHub.data.github.html.RepositoryActionsVariableItem

data class RepositoryActionsSettingsUiState(
    val owner: String = "",
    val repo: String = "",
    val snapshot: RepositoryActionsSettingsSnapshot? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val pendingMessage: String? = null,
    val sourceUrl: String? = null,
    val secrets: List<RepositoryActionsSecretItem> = emptyList(),
    val variables: List<RepositoryActionsVariableItem> = emptyList(),
    val caches: List<RepositoryActionsCacheItem> = emptyList(),
    val isShowingStaleContent: Boolean = false
) {
    val isInitialLoad: Boolean get() = snapshot == null && isLoading
    val metrics: List<RepositoryActionsSettingsMetric> get() = snapshot?.toMetrics().orEmpty()
}

data class RepositoryActionsSettingsMetric(val label: String, val value: String)

fun RepositoryActionsSettingsSnapshot.toMetrics(): List<RepositoryActionsSettingsMetric> {
    return listOf(
        RepositoryActionsSettingsMetric("工作流状态", actionsPermissions?.enabled?.toEnabledText() ?: "未知"),
        RepositoryActionsSettingsMetric("允许运行范围", actionsPermissions?.allowedActions.toAllowedActionsText()),
        RepositoryActionsSettingsMetric("工作流默认权限", workflowPermissions?.defaultWorkflowPermissions.toWorkflowPermissionText()),
        RepositoryActionsSettingsMetric("允许批准拉取请求", workflowPermissions?.canApprovePullRequestReviews?.toEnabledText() ?: "未知"),
        RepositoryActionsSettingsMetric("密钥数量", secretsCount?.toString() ?: "不可读"),
        RepositoryActionsSettingsMetric("变量数量", variablesCount?.toString() ?: "不可读"),
        RepositoryActionsSettingsMetric("保留天数", retentionDays?.let { "${it} 天" } ?: "不可读"),
        RepositoryActionsSettingsMetric("Cache 用量", cacheUsage?.let { "${it.activeCachesCount} 个 · ${it.activeCachesSizeInBytes.toReadableBytes()}" } ?: "不可读"),
        RepositoryActionsSettingsMetric("当前权限", if (canAdmin) "可修改" else "只读")
    )
}

fun String?.toAllowedActionsText(): String = when (this) {
    "all" -> "允许所有操作和可复用工作流"
    "local_only" -> "仅允许本仓库内的操作和可复用工作流"
    "selected" -> "仅允许选定操作和可复用工作流"
    null, "unknown" -> "未知"
    else -> this
}

fun String?.toWorkflowPermissionText(): String = when (this) {
    "read" -> "只读仓库内容"
    "write" -> "读写仓库内容"
    null -> "未知"
    else -> this
}

fun Long.toReadableBytes(): String {
    if (this < 1024L) return "$this B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = this / 1024.0
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) { value /= 1024.0; unitIndex++ }
    return "%.1f %s".format(value, units[unitIndex])
}

private fun Boolean.toEnabledText(): String = if (this) "开启" else "关闭"