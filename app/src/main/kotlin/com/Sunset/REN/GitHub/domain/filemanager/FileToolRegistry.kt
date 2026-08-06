package com.Sunset.REN.GitHub.domain.filemanager

/**
 * MT-style lightweight tool registry for local file manager entries.
 *
 * This registry intentionally describes available tools only. The fragment decides
 * how much of a tool is implemented today, so new engineering tools can be added
 * without turning LocalFileManagerFragment into an extension if/else hub.
 */
object FileToolRegistry {
    fun toolsFor(entry: FileManagerEntry): List<FileToolAction> {
        if (entry.type == FileEntryType.Parent || entry.type == FileEntryType.Directory) return emptyList()
        val name = entry.name.lowercase()
        val tools = linkedMapOf<FileToolId, FileToolAction>()

        fun add(id: FileToolId, title: String, implemented: Boolean = false, singleWindow: Boolean = false) {
            tools[id] = FileToolAction(id, title, implemented, singleWindow)
        }

        when (entry.type) {
            FileEntryType.Apk -> {
                add(FileToolId.ApkInfo, "Apk信息", implemented = true, singleWindow = true)
                add(FileToolId.ArchiveBrowse, "浏览压缩包", implemented = true)
                add(FileToolId.ArchiveExtract, "解压到…", implemented = ArchiveFormatResolver.resolve(entry.name)?.supportsExtraction == true)
                add(FileToolId.ApkSignature, "APK签名", implemented = true)
            }
            FileEntryType.Archive -> {
                add(FileToolId.ArchiveBrowse, "浏览压缩包", implemented = ArchiveFormatResolver.resolve(entry.name)?.supportsExtraction == true)
                add(FileToolId.ArchiveExtract, "解压到…", implemented = ArchiveFormatResolver.resolve(entry.name)?.supportsExtraction == true)
            }
            FileEntryType.Text,
            FileEntryType.Markdown,
            FileEntryType.Code -> add(FileToolId.TextEdit, "编辑文本", implemented = true, singleWindow = true)
            FileEntryType.Image -> add(FileToolId.ImagePreview, "查看图片", implemented = true)
            else -> Unit
        }

        if (name.endsWith(".dex")) add(FileToolId.DexTools, "Dex功能", implemented = true)
        if (name.endsWith(".arsc")) add(FileToolId.ArscTools, "Arsc功能", implemented = true)
        if (name.endsWith(".xml")) add(FileToolId.XmlTools, "Xml功能", implemented = entry.capabilities.canAccessContent)

        add(FileToolId.HexViewer, "十六进制", implemented = entry.capabilities.canAccessContent)
        add(FileToolId.CopyPath, "复制路径", implemented = true)
        add(FileToolId.Properties, "属性", implemented = true)

        return tools.values.toList()
    }
}

data class FileToolAction(
    val id: FileToolId,
    val title: String,
    val implemented: Boolean,
    val singleWindow: Boolean = false
)

enum class FileToolId {
    ApkInfo,
    ApksTools,
    ApkSignature,
    DexTools,
    ArscTools,
    XmlTools,
    HexViewer,
    TextEdit,
    ImagePreview,
    ArchiveBrowse,
    ArchiveExtract,
    ArchiveCompare,
    JarTools,
    ClassTools,
    NinePatchEdit,
    SvgTools,
    LinuxScript,
    CopyPath,
    Properties
}
