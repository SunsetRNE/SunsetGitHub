package com.Sunset.REN.GitHub.domain.filemanager.root

object RootPathPolicy {
    private val rootOnlyPrefixes = listOf("/data", "/system", "/vendor", "/product", "/apex", "/proc", "/sys")
    private val highRiskWritePrefixes = listOf("/", "/system", "/vendor", "/product", "/data", "/apex")

    fun isRootOnlyPath(path: String): Boolean = rootOnlyPrefixes.any { path == it || path.startsWith("$it/") }
    fun isHighRiskWritePath(path: String): Boolean = highRiskWritePrefixes.any { path == it || path.startsWith("$it/") }

    fun riskLabel(path: String): String = when {
        path == "/" -> "极高风险"
        isHighRiskWritePath(path) -> "高风险"
        isRootOnlyPath(path) -> "受保护路径"
        else -> "普通 Root 路径"
    }

    fun advancedActionUnavailableReport(actionTitle: String, path: String): String {
        return """
            操作：$actionTitle
            路径：$path
            风险等级：${riskLabel(path)}

            该 Root 高级操作已纳入能力模型，但当前版本暂不直接执行。

            开放前必须补齐：
            1. 二次确认与路径展示；
            2. 参数转义与命令注入防护；
            3. 系统目录递归保护；
            4. 失败详情与任务日志；
            5. 可取消的 Operation 任务事件。
        """.trimIndent()
    }

    fun writeOperationBlockedReport(actionTitle: String, path: String): String {
        val guard = when {
            path == "/" -> "禁止对根目录执行递归写入或删除。"
            isHighRiskWritePath(path) -> "系统/数据分区路径默认只读浏览，写入必须由专用 Root 任务逐项开放。"
            isRootOnlyPath(path) -> "受保护路径需要额外校验目标、命令参数和失败回滚。"
            else -> "Root 路径写入仍需统一任务日志、二次确认和参数转义。"
        }
        return """
            操作：$actionTitle
            路径：$path
            风险等级：${riskLabel(path)}

            当前版本已拦截本次 Root 写入/删除请求，未执行任何命令。
            防护策略：$guard

            后续开放条件：
            1. 逐操作二次确认，展示完整源路径与目标路径；
            2. 命令参数必须经过 shell 单参数转义；
            3. /、/system、/vendor、/product、/data、/apex 默认禁止递归破坏性操作；
            4. 所有 Root 写操作必须进入 Operation 事件流并记录失败详情；
            5. 支持取消、进度反馈与结果报告复制。
        """.trimIndent()
    }
}
