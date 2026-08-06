package com.Sunset.REN.GitHub.data.filemanager

import android.content.Context
import android.net.Uri
import org.json.JSONArray

class FavoriteDirectoryStore(context: Context) {
    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun getFavorites(): List<FavoriteDirectoryRecord> {
        val ordered = sharedPreferences.getString(KeyFavoritesOrdered, null)
            ?.takeIf { it.isNotBlank() }
            ?.let(::decodeOrderedFavorites)
        if (ordered != null) return ordered

        return sharedPreferences.getStringSet(KeyFavorites, emptySet()).orEmpty()
            .mapNotNull { encoded -> FavoriteDirectoryRecord.decode(encoded) }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }
    }

    fun addFavorite(record: FavoriteDirectoryRecord, addToTop: Boolean = false) {
        val normalized = record.normalized()
        val existing = getFavorites().filterNot { it.key == normalized.key }
        val updated = if (addToTop) listOf(normalized) + existing else existing + normalized
        saveFavorites(updated)
    }

    fun removeFavorite(record: FavoriteDirectoryRecord) {
        val key = record.normalized().key
        val updated = getFavorites().filterNot { it.key == key }
        saveFavorites(updated)
    }

    fun isFavorite(record: FavoriteDirectoryRecord): Boolean {
        val key = record.normalized().key
        return getFavorites().any { it.key == key }
    }

    private fun saveFavorites(records: List<FavoriteDirectoryRecord>) {
        val encoded = records.map { it.normalized().encode() }
        sharedPreferences.edit()
            .putString(KeyFavoritesOrdered, JSONArray(encoded).toString())
            .putStringSet(KeyFavorites, encoded.toSet())
            .apply()
    }

    private fun decodeOrderedFavorites(raw: String): List<FavoriteDirectoryRecord> {
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val record = FavoriteDirectoryRecord.decode(array.optString(index)) ?: continue
                    add(record)
                }
            }.distinctBy { it.key }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PreferencesName = "file_manager_favorite_directories"
        const val KeyFavorites = "favorites"
        const val KeyFavoritesOrdered = "favorites_ordered"
    }
}

data class FavoriteDirectoryRecord(
    val type: FavoriteDirectoryType,
    val value: String,
    val label: String
) {
    val key: String = "${type.name}|$value"

    fun normalized(): FavoriteDirectoryRecord {
        return copy(label = label.ifBlank { value })
    }

    fun encode(): String = listOf(type.name, value, label)
        .joinToString("|") { Uri.encode(it) }

    companion object {
        fun decode(encoded: String): FavoriteDirectoryRecord? {
            val parts = encoded.split('|')
            if (parts.size != 3) return null
            val type = runCatching { FavoriteDirectoryType.valueOf(Uri.decode(parts[0])) }.getOrNull() ?: return null
            val value = Uri.decode(parts[1])
            val label = Uri.decode(parts[2])
            if (value.isBlank()) return null
            return FavoriteDirectoryRecord(type, value, label.ifBlank { value })
        }
    }
}

enum class FavoriteDirectoryType {
    Local,
    Saf,
    Archive
}
