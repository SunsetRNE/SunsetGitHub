package com.Sunset.REN.GitHub.domain.filemanager

/**
 * Forward-compatible storage provider contract for the MT-style file manager.
 *
 * The current app only implements local / SAF / archive navigation in the view model,
 * but the UI and capability matrix need stable slots for future Root, Shell/Shizuku
 * and network storage providers. Keeping this descriptor in the domain layer prevents
 * future providers from being hard-coded into the fragment.
 */
data class StorageProviderDescriptor(
    val id: String,
    val title: String,
    val kind: StorageProviderKind,
    val implemented: Boolean,
    val description: String
)

enum class StorageProviderKind {
    Local,
    Saf,
    Archive,
    Shell,
    Root,
    Network,
    Cloud
}

object StorageProviderCatalog {
    fun defaultProviders(): List<StorageProviderDescriptor> = listOf(
        StorageProviderDescriptor(
            id = "local",
            title = "本地存储",
            kind = StorageProviderKind.Local,
            implemented = true,
            description = "普通本地文件系统路径。"
        ),
        StorageProviderDescriptor(
            id = "saf",
            title = "授权目录",
            kind = StorageProviderKind.Saf,
            implemented = true,
            description = "Android SAF 持久授权目录。"
        ),
        StorageProviderDescriptor(
            id = "archive",
            title = "压缩包",
            kind = StorageProviderKind.Archive,
            implemented = true,
            description = "ZIP/APK 等压缩包内目录浏览。"
        ),
        StorageProviderDescriptor(
            id = "shell_shizuku",
            title = "Shell / Shizuku",
            kind = StorageProviderKind.Shell,
            implemented = false,
            description = "预留 Android/data 等受限目录访问通道。"
        ),
        StorageProviderDescriptor(
            id = "root",
            title = "Root",
            kind = StorageProviderKind.Root,
            implemented = false,
            description = "预留 Root 文件系统访问与 Root 安装通道。"
        ),
        StorageProviderDescriptor(
            id = "network",
            title = "网络存储",
            kind = StorageProviderKind.Network,
            implemented = false,
            description = "预留 FTP/SFTP/WebDAV 等远端目录。"
        ),
        StorageProviderDescriptor(
            id = "oss_s3",
            title = "OSS / S3",
            kind = StorageProviderKind.Cloud,
            implemented = false,
            description = "预留阿里云 OSS、AWS S3、Cloudflare R2。"
        )
    )
}