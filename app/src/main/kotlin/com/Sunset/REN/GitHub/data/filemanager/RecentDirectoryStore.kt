package com.Sunset.REN.GitHub.data.filemanager

import android.content.Context
import android.net.Uri

class RecentDirectoryStore(context: Context) {
    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun getRecents(): List<RecentDirectoryRecord> {
        return sharedPreferences.getStringSet(KeyRecents, emptySet()).orEmpty()
            .mapNotNull { encoded -> RecentDirectoryRecord.decode(encoded) }
            .distinctBy { it.directory.key }
            .sortedByDescending { it.openedAtMillis }
            .take(MaxRecents)
    }

    fun markOpened(directory: FavoriteDirectoryRecord, openedAtMillis: Long = System.currentTimeMillis()) {
        val normalized = RecentDirectoryRecord(directory.normalized(), openedAtMillis)
        val updated = (listOf(normalized) + getRecents().filterNot { it.directory.key == normalized.directory.key })
            .take(MaxRecents)
            .map { it.encode() }
            .toSet()
        sharedPreferences.edit().putStringSet(KeyRecents, updated).apply()
    }

    fun clear() {
        sharedPreferences.edit().remove(KeyRecents).apply()
    }

    private companion object {
        const val PreferencesName = "file_manager_recent_directories"
        const val KeyRecents = "recents"
        const val MaxRecents = 8
    }
}

data class RecentDirectoryRecord(
    val directory: FavoriteDirectoryRecord,
    val openedAtMillis: Long
) {
    fun encode(): String = listOf(
        directory.type.name,
        directory.value,
        directory.label,
        openedAtMillis.toString()
    ).joinToString("|") { Uri.encode(it) }

    companion object {
        fun decode(encoded: String): RecentDirectoryRecord? {
            val parts = encoded.split('|')
            if (parts.size != 4) return null
            val type = runCatching { FavoriteDirectoryType.valueOf(Uri.decode(parts[0])) }.getOrNull() ?: return null
            val value = Uri.decode(parts[1])
            val label = Uri.decode(parts[2])
            val openedAtMillis = Uri.decode(parts[3]).toLongOrNull() ?: return null
            if (value.isBlank()) return null
            return RecentDirectoryRecord(
                directory = FavoriteDirectoryRecord(type, value, label.ifBlank { value }),
                openedAtMillis = openedAtMillis
            )
        }
    }
}
