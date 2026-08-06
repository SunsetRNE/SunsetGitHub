package com.Sunset.REN.GitHub.domain.filemanager.path

import android.net.Uri
import java.io.File

/** Typed path model used by the file-manager core instead of passing every location as String. */
sealed interface FileManagerPath {
    val displayPath: String

    data class Local(val absolutePath: String) : FileManagerPath {
        constructor(file: File) : this(file.absolutePath)
        override val displayPath: String = absolutePath
    }

    data class Saf(val uri: Uri) : FileManagerPath {
        override val displayPath: String = uri.toString()
    }

    data class Archive(val archivePath: String, val innerPath: String = "") : FileManagerPath {
        override val displayPath: String = "$archivePath!/${innerPath.trim('/')}"
    }

    data class Root(val absolutePath: String) : FileManagerPath {
        override val displayPath: String = absolutePath
    }

    companion object {
        fun parse(displayPath: String): FileManagerPath {
            if (displayPath.startsWith("root://")) return Root(displayPath.removePrefix("root://").ifBlank { "/" })
            val marker = displayPath.indexOf("!/")
            if (marker > 0) {
                return Archive(
                    archivePath = displayPath.substring(0, marker),
                    innerPath = displayPath.substring(marker + 2).trim('/')
                )
            }
            val uri = runCatching { Uri.parse(displayPath) }.getOrNull()
            if (uri?.scheme.equals("content", ignoreCase = true)) return Saf(uri!!)
            return Local(displayPath)
        }
    }
}

enum class FileSystemProviderId {
    Local,
    Saf,
    Archive,
    Root,
    Shizuku
}
