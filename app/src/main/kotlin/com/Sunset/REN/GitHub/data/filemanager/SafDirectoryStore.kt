package com.Sunset.REN.GitHub.data.filemanager

import android.content.Context
import android.net.Uri

class SafDirectoryStore(context: Context) {
    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun getDirectories(): List<SafDirectoryRecord> {
        return sharedPreferences.getStringSet(KeyDirectories, emptySet()).orEmpty()
            .mapNotNull { encoded -> SafDirectoryRecord.decode(encoded) }
            .sortedBy { it.label.lowercase() }
    }

    fun addDirectory(uri: Uri, label: String) {
        val current = getDirectories()
        val updated = (current.filterNot { it.uri == uri } + SafDirectoryRecord(uri, label.ifBlank { uri.toString() }))
            .map { it.encode() }
            .toSet()
        sharedPreferences.edit().putStringSet(KeyDirectories, updated).apply()
    }

    fun removeMissingPersistedPermissions(persistedUris: Set<Uri>) {
        val updated = getDirectories()
            .filter { it.uri in persistedUris }
            .map { it.encode() }
            .toSet()
        sharedPreferences.edit().putStringSet(KeyDirectories, updated).apply()
    }

    private companion object {
        const val PreferencesName = "file_manager_saf_directories"
        const val KeyDirectories = "directories"
    }
}

data class SafDirectoryRecord(
    val uri: Uri,
    val label: String
) {
    fun encode(): String = "${Uri.encode(uri.toString())}|${Uri.encode(label)}"

    companion object {
        fun decode(encoded: String): SafDirectoryRecord? {
            val separatorIndex = encoded.indexOf('|')
            if (separatorIndex <= 0) return null
            val uriText = Uri.decode(encoded.substring(0, separatorIndex))
            val label = Uri.decode(encoded.substring(separatorIndex + 1))
            return SafDirectoryRecord(Uri.parse(uriText), label)
        }
    }
}