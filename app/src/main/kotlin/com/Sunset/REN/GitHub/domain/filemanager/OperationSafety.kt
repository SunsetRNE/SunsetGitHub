package com.Sunset.REN.GitHub.domain.filemanager

/**
 * Centralises local-file-manager safety copy and lightweight write guards.
 *
 * The manager intentionally avoids scary, global labels such as "dangerous app".
 * Safety here means: be explicit about destination, conflict policy and which
 * privileged / destructive capability is still only an entry point.
 */
object OperationSafety {
    fun isRootPath(path: String): Boolean = path.startsWith("root://")
    fun isWritableDirectoryTarget(path: String): Boolean = path.isNotBlank() && !path.contains("!/") && !isRootPath(path)

    fun archiveExtractionPlan(entryName: String, formatName: String): String {
        return "将在当前目录解压“$entryName”。\n\n格式：$formatName\n输出目录会自动避开同名冲突。"
    }

    fun multiArchiveExtractionPlan(targetPath: String, archiveCount: Int): String {
        return "目标目录：$targetPath\n压缩包：$archiveCount 个\n\n每个压缩包会自动创建独立输出目录，并避开同名冲突。"
    }

    fun zipPlan(directoryPath: String, entryCount: Int): String {
        return "将在当前目录创建 ZIP 压缩包。\n\n位置：$directoryPath\n选中项：$entryCount 项"
    }

    fun textExportPlan(directoryPath: String, entryCount: Int): String {
        return "将在当前目录导出 TXT 文本副本。\n\n位置：$directoryPath\n选中项：$entryCount 项\n\n支持普通文本、部分文档文本提取，以及可读取的压缩包内文件。原文件不会被修改。"
    }

    fun textExportDirectorySkipPlan(directoryCount: Int): String {
        return "选中项中包含 $directoryCount 个文件夹，当前暂不支持将文件夹转换为 TXT。\n\n继续后会跳过/记录这些失败项，并转换其余可读取文件。"
    }

    fun transferPlan(targetPath: String, entryCount: Int): String {
        return "目标目录：$targetPath\n选中项：$entryCount 项"
    }

    fun transferConflictReport(
        targetPath: String,
        conflictedEntries: List<FileManagerEntry>
    ): String {
        val conflictNames = conflictedEntries.joinToString("\n") { entry ->
            "已存在项：$targetPath/${entry.name}\n待写入项：${entry.displayPath}"
        }
        return """
            目标目录已存在 ${conflictedEntries.size} 个同名条目。

            $conflictNames

            可选择跳过冲突，或继续执行并在失败详情中查看报告。
        """.trimIndent()
    }

    fun apkInstallAndSignatureFacts(
        entry: FileManagerEntry,
        typeName: String,
        size: String,
        modified: String,
        archiveSupport: String
    ): String {
        return """
            名称：${entry.name}
            路径：${entry.displayPath}
            类型：$typeName
            大小：$size
            修改时间：$modified

            安装策略：Shizuku → Dhizuku → Root → 系统安装
            安装前验证：签名与版本号校验入口已预留
            APK 防自动删除：入口已预留
            签名状态：当前未执行签名校验
            压缩包能力：$archiveSupport

            说明：这里仅展示事实和工具入口，不对 APK 盖“危险软件”印章。
        """.trimIndent()
    }
}
