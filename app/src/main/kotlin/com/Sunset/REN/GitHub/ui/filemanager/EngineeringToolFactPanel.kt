package com.Sunset.REN.GitHub.ui.filemanager

import android.app.Dialog
import android.content.Context
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.ArscEntrySampleFacts
import com.Sunset.REN.GitHub.domain.filemanager.ArscPackageChildChunkFacts
import com.Sunset.REN.GitHub.domain.filemanager.ArscSource
import com.Sunset.REN.GitHub.domain.filemanager.EngineeringToolScanResult
import com.Sunset.REN.GitHub.domain.filemanager.EngineeringToolScanner
import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.FileSizeFormatter
import com.Sunset.REN.GitHub.domain.filemanager.FileToolAction
import com.Sunset.REN.GitHub.domain.filemanager.FileToolId
import java.text.DateFormat
import java.util.Date

/** Factual panels for MT-style engineering tools, backed by domain-layer scanning. */
object EngineeringToolFactPanel {
    fun show(
        context: Context,
        entry: FileManagerEntry,
        tool: FileToolAction,
        onCopyPath: () -> Unit
    ): Dialog {
        val scan = EngineeringToolScanner.scan(entry, tool.id)
        return LocalFileManagerDialogScaffold.showPlainScrollable(
            context = context,
            title = tool.title,
            message = when (tool.id) {
                FileToolId.ApkSignature -> apkSignatureFacts(context, scan)
                FileToolId.DexTools -> dexFacts(context, scan)
                FileToolId.ArscTools -> arscFacts(context, scan)
                else -> genericFacts(context, scan, tool)
            },
            positiveText = android.R.string.ok,
            neutralText = R.string.local_file_manager_copy_path,
            onNeutral = onCopyPath
        )
    }

    private fun apkSignatureFacts(context: Context, scan: EngineeringToolScanResult): String {
        val zip = scan.zipFacts
        return buildString {
            appendBaseFacts(context, scan.entry, "APK 签名事实扫描")
            append('\n')
            if (zip == null) {
                appendLine("扫描状态：当前来源不是可直接读取的本地 APK/ZIP 文件，暂只能展示入口与基础属性。")
            } else {
                appendLine("ZIP 条目数：${zip.entryCount}")
                appendLine("classes.dex：${if (zip.hasDex) "存在" else "未发现"}")
                appendLine("resources.arsc：${if (zip.hasArsc) "存在" else "未发现"}")
                appendLine("V1 清单：${if (zip.hasV1Manifest) "发现 META-INF/MANIFEST.MF" else "未发现"}")
                appendLine("V1 SF 文件：${if (zip.v1SignatureFiles.isEmpty()) "未发现" else zip.v1SignatureFiles.joinToString(limit = 4)}")
                appendLine("V1 证书块：${if (zip.v1CertificateFiles.isEmpty()) "未发现 .RSA/.DSA/.EC" else zip.v1CertificateFiles.joinToString(limit = 4)}")
                val block = zip.apkSigningBlock
                appendLine(
                    "APK Signing Block：" + when {
                        block == null -> "未扫描"
                        !block.present -> "未发现 APK Sig Block 42"
                        else -> "发现，block=${block.blockOffset ?: "未知"}，centralDir=${block.centralDirectoryOffset ?: "未知"}"
                    }
                )
                block?.takeIf { it.present }?.let {
                    appendLine("V2 ID：${if (it.hasV2Id) "疑似存在" else "未发现"}")
                    appendLine("V3 ID：${if (it.hasV3Id) "疑似存在" else "未发现"}")
                    appendLine("V3.1 ID：${if (it.hasV31Id) "疑似存在" else "未发现"}")
                    appendLine("V4 idsig：${if (it.hasV4Marker) "疑似存在" else "未发现"}")
                }
            }
            appendLine()
            appendLine("说明：该面板只报告可见事实，不对 APK 盖“安全/危险”结论。")
        }
    }

    private fun dexFacts(context: Context, scan: EngineeringToolScanResult): String {
        val dex = scan.dexHeader
        return buildString {
            appendBaseFacts(context, scan.entry, "Dex 事实扫描")
            append('\n')
            if (dex == null) {
                appendLine("扫描状态：当前来源不可直接读取字节，暂只能展示基础属性。")
            } else {
                appendLine("Magic：${dex.magicHex}")
                appendLine("Dex 版本：${dex.dexVersion ?: "未识别为标准 dex"}")
                appendLine("Header 文件大小：${dex.fileSize?.let(FileSizeFormatter::format) ?: "未知"}")
                appendLine("Header 大小：${dex.headerSize ?: "未知"}")
                appendLine("StringIds：${dex.stringIds ?: "未知"} @ ${dex.stringIdsOffset ?: "未知"}")
                appendLine("TypeIds：${dex.typeIds ?: "未知"} @ ${dex.typeIdsOffset ?: "未知"}")
                appendLine("ProtoIds：${dex.protoIds ?: "未知"} @ ${dex.protoIdsOffset ?: "未知"}")
                appendLine("FieldIds：${dex.fieldIds ?: "未知"} @ ${dex.fieldIdsOffset ?: "未知"}")
                appendLine("MethodIds：${dex.methodIds ?: "未知"} @ ${dex.methodIdsOffset ?: "未知"}")
                appendLine("ClassDefs：${dex.classDefs ?: "未知"} @ ${dex.classDefsOffset ?: "未知"}")
                if (dex.sampleTypes.isNotEmpty()) {
                    appendLine("类型描述符预览：")
                    dex.sampleTypes.forEachIndexed { index, value ->
                        appendLine("  ${index + 1}. $value")
                    }
                }
                if (dex.sampleClasses.isNotEmpty()) {
                    appendLine("类定义预览：")
                    dex.sampleClasses.forEachIndexed { index, value ->
                        appendLine("  ${index + 1}. $value")
                    }
                }
                if (dex.sampleFields.isNotEmpty()) {
                    appendLine("字段名预览：")
                    dex.sampleFields.forEachIndexed { index, value ->
                        appendLine("  ${index + 1}. $value")
                    }
                }
                if (dex.sampleMethods.isNotEmpty()) {
                    appendLine("方法名预览：")
                    dex.sampleMethods.forEachIndexed { index, value ->
                        appendLine("  ${index + 1}. $value")
                    }
                }
                if (dex.sampleStrings.isNotEmpty()) {
                    appendLine("字符串预览：")
                    dex.sampleStrings.forEachIndexed { index, value ->
                        appendLine("  ${index + 1}. $value")
                    }
                }
            }
            appendLine()
            appendLine("后续槽位：类/方法浏览、字符串检索、smali 导出、反编译入口。")
        }
    }

    private fun arscFacts(context: Context, scan: EngineeringToolScanResult): String {
        val arsc = scan.arscHeader
        return buildString {
            appendBaseFacts(context, scan.entry, "ARSC 事实扫描")
            append('\n')
            when (arsc?.source) {
                ArscSource.ApkZipEntry -> {
                    appendLine("APK 内 resources.arsc：存在")
                    appendLine("压缩前大小：${arsc.zipEntrySize?.let(FileSizeFormatter::format) ?: "未知"}")
                    appendLine("压缩后大小：${arsc.zipEntryCompressedSize?.let(FileSizeFormatter::format) ?: "未知"}")
                }
                ArscSource.DirectBytes -> {
                    appendLine("Header Type：${arsc.headerType?.let { "0x" + it.toString(16).padStart(4, '0') } ?: "未知"}")
                    appendLine("Header Size：${arsc.headerSize ?: "未知"}")
                    appendLine("Chunk Size：${arsc.chunkSize?.let(FileSizeFormatter::format) ?: "未知"}")
                    appendLine("Package Count：${arsc.packageCount ?: "未知"}")
                    appendLine("ResTable 判断：${if (arsc.headerType == 0x0002) "符合 RES_TABLE_TYPE" else "未确认"}")
                    arsc.globalStringPool?.let { pool ->
                        appendLine("全局字符串池：count=${pool.stringCount} styles=${pool.styleCount} utf8=${if (pool.utf8) "是" else "否"} offset=${pool.offset}")
                        appendLine("字符串区：${pool.stringsStart} 样式区：${pool.stylesStart} chunk=${pool.chunkSize}")
                        appendStringSamples("全局字符串预览", pool.sampleStrings)
                    }
                    arsc.firstPackage?.let { pkg ->
                        appendLine("首个包：id=0x${pkg.id.toString(16)} name=${pkg.name.ifBlank { "未知" }}")
                        appendLine("包偏移：${pkg.offset} typeStrings=${pkg.typeStringsOffset} keyStrings=${pkg.keyStringsOffset}")
                        pkg.typeStringPool?.let { pool ->
                            appendLine("类型字符串池：count=${pool.stringCount} utf8=${if (pool.utf8) "是" else "否"} offset=${pool.offset}")
                            appendStringSamples("类型名预览", pool.sampleStrings)
                        }
                        pkg.keyStringPool?.let { pool ->
                            appendLine("Key 字符串池：count=${pool.stringCount} utf8=${if (pool.utf8) "是" else "否"} offset=${pool.offset}")
                            appendStringSamples("Key 名预览", pool.sampleStrings)
                        }
                        if (pkg.childChunks.isNotEmpty()) {
                            appendLine("资源类型块预览：")
                            pkg.childChunks.forEachIndexed { index, chunk ->
                                appendLine(
                                    "  ${index + 1}. ${chunk.kindLabel()} typeId=${chunk.typeId ?: "未知"} " +
                                        "name=${chunk.typeName ?: "未知"} entries=${chunk.entryCount ?: "未知"} " +
                                        "nonEmpty=${chunk.nonEmptyEntryCount ?: "未知"} config=${chunk.configSummary ?: chunk.configSize ?: "-"} offset=${chunk.offset}"
                                )
                                appendEntrySamples(chunk.sampleEntries)
                            }
                        }
                    }
                }
                null -> appendLine("扫描状态：当前来源不可直接读取 ARSC 字节，暂只能展示基础属性。")
            }
            appendLine()
            appendLine("后续槽位：包表浏览、资源 ID 查询、字符串池/类型表查看、AXML/ARSC 联动。")
        }
    }

    private fun genericFacts(context: Context, scan: EngineeringToolScanResult, tool: FileToolAction): String = buildString {
        appendBaseFacts(context, scan.entry, tool.title)
        append('\n')
        appendLine("当前状态：入口已注册，后续接入对应工程模块。")
    }

    private fun StringBuilder.appendEntrySamples(samples: List<ArscEntrySampleFacts>) {
        if (samples.isEmpty()) return
        appendLine("     Entry 预览：")
        samples.take(8).forEach { sample ->
            appendLine(
                if (sample.complex) {
                    buildString {
                        append("       #${sample.entryIndex} key=${sample.keyName ?: "未知"} ")
                        append("flags=0x${sample.flags.toString(16)} complex parent=${sample.complexParent?.let { "0x" + it.toString(16) } ?: "未知"} ")
                        append("maps=${sample.complexMapCount ?: "未知"}")
                        if (sample.complexMaps.isNotEmpty()) {
                            append(" mapPreview=")
                            append(sample.complexMaps.joinToString(limit = 3) { map ->
                                "0x${map.nameRef.toString(16)}:${map.valueDataTypeName ?: "?"}=${map.valueDisplay ?: map.valueData?.let { "0x" + it.toString(16) } ?: "未知"}"
                            })
                        }
                    }
                } else {
                    "       #${sample.entryIndex} key=${sample.keyName ?: "未知"} " +
                        "flags=0x${sample.flags.toString(16)} type=${sample.valueDataTypeName ?: sample.valueDataType?.let { "0x" + it.toString(16) } ?: "未知"} " +
                        "data=${sample.valueDisplay ?: sample.valueData?.let { "0x" + it.toString(16) } ?: "未知"}"
                }
            )
        }
    }

    private fun ArscPackageChildChunkFacts.kindLabel(): String = when (chunkType) {
        0x0201 -> "Type"
        0x0202 -> "TypeSpec"
        else -> "Chunk 0x${chunkType.toString(16)}"
    }

    private fun StringBuilder.appendStringSamples(title: String, samples: List<String>) {
        if (samples.isEmpty()) return
        appendLine("$title：")
        samples.forEachIndexed { index, value ->
            appendLine("  ${index + 1}. $value")
        }
    }

    private fun StringBuilder.appendBaseFacts(context: Context, entry: FileManagerEntry, title: String) {
        val size = entry.sizeBytes?.let(FileSizeFormatter::format)
            ?: context.getString(R.string.local_file_manager_properties_unknown)
        val modified = entry.modifiedAtMillis?.let { millis ->
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
        } ?: context.getString(R.string.local_file_manager_properties_unknown)
        appendLine(title)
        appendLine("文件：${entry.name}")
        appendLine("路径：${entry.displayPath}")
        appendLine("大小：$size")
        appendLine("修改时间：$modified")
        appendLine("来源：${sourceLabel(entry.source)}")
    }

    private fun sourceLabel(source: FileEntrySource): String = when (source) {
        is FileEntrySource.LocalFile -> "本地文件"
        is FileEntrySource.DocumentUri -> "SAF 文档"
        is FileEntrySource.ContentUri -> "Content URI"
        is FileEntrySource.ArchiveEntry -> "压缩包条目"
        is FileEntrySource.ParentDirectory -> "父目录"
        is FileEntrySource.RootPath -> "Root 路径"
    }
}