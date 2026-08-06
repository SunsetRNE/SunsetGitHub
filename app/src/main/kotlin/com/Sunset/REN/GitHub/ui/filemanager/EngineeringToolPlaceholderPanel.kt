package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.FileToolAction
import com.Sunset.REN.GitHub.domain.filemanager.FileToolId

/** Dedicated placeholder panels for MT-style engineering tools. */
object EngineeringToolPlaceholderPanel {
    fun show(
        context: Context,
        entry: FileManagerEntry,
        tool: FileToolAction
    ) {
        LocalFileManagerDialogScaffold.showPlainScrollable(
            context = context,
            title = tool.title,
            message = messageFor(entry, tool),
            positiveText = android.R.string.ok
        )
    }

    private fun messageFor(entry: FileManagerEntry, tool: FileToolAction): String {
        val capability = when (tool.id) {
            FileToolId.ApkSignature -> "签名证书查看、签名校验、V1/V2/V3/V4 签名状态、安装前版本校验。"
            FileToolId.ApksTools -> "APKS/XAPK/APKM 拆包、split APK 清单浏览、合并安装策略与导出。"
            FileToolId.DexTools -> "Dex 方法/类浏览、字符串检索、smali 导出、基础反编译入口。"
            FileToolId.ArscTools -> "resources.arsc 包表浏览、资源 ID 查询、字符串/类型表查看。"
            FileToolId.XmlTools -> "二进制 XML 解码、AXML 属性查看、格式化预览与导出。"
            FileToolId.JarTools -> "JAR 条目浏览、class 清单、反编译入口、Manifest 查看。"
            FileToolId.ClassTools -> "Class 基本信息、常量池浏览、方法/字段概要。"
            FileToolId.NinePatchEdit -> "点九 PNG 边界查看、拉伸区域预览、Patch 信息导出。"
            FileToolId.SvgTools -> "SVG 预览、尺寸/路径概要、导出与文本编辑入口。"
            FileToolId.LinuxScript -> "脚本查看、可执行权限检查、Shell/Root/Shizuku 运行槽位。"
            FileToolId.ArchiveCompare -> "压缩包目录差异、条目大小/时间对比、变更报告导出。"
            else -> "该工具已注册到打开方式矩阵，后续会接入对应工程模块。"
        }
        return """
            ${tool.title} 已作为独立工程工具入口接入。

            文件：${entry.name}
            路径：${entry.displayPath}

            规划能力：
            $capability

            当前状态：入口已注册，面板已独立；底层解析/写入能力将在后续 ApkTooling / DexTooling / ArscXmlEngineering / ArchiveTooling 模块继续补齐。
        """.trimIndent()
    }
}