package com.Sunset.REN.GitHub.data.filemanager

import android.content.Context
import android.net.Uri

class RecycleBinRecordStore(context: Context) {
    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun getRecords(): List<RecycleBinRecord> {
        return sharedPreferences.getStringSet(KeyRecords, emptySet()).orEmpty()
            .mapNotNull(RecycleBinRecord::decode)
            .distinctBy { it.recyclePath }
            .sortedByDescending { it.deletedAtMillis }
    }

    fun record(record: RecycleBinRecord) {
        val normalized = record.normalized()
        val updated = (getRecords().filterNot { it.recyclePath == normalized.recyclePath } + normalized)
            .map { it.encode() }
            .toSet()
        sharedPreferences.edit().putStringSet(KeyRecords, updated).apply()
    }

    fun recordForRecyclePath(path: String): RecycleBinRecord? {
        return getRecords().firstOrNull { it.recyclePath == path }
    }

    fun removeForRecyclePath(path: String) {
        val updated = getRecords()
            .filterNot { it.recyclePath == path }
            .map { it.encode() }
            .toSet()
        sharedPreferences.edit().putStringSet(KeyRecords, updated).apply()
    }

    fun clear() {
        sharedPreferences.edit().remove(KeyRecords).apply()
    }

    private companion object {
        const val PreferencesName = "file_manager_recycle_bin_records"
        const val KeyRecords = "records"
    }
}

data class RecycleBinRecord(
    val recyclePath: String,
    val originalPath: String,
    val originalName: String,
    val deletedAtMillis: Long
) {
    fun normalized(): RecycleBinRecord {
        return copy(
            originalName = originalName.ifBlank { originalPath.substringAfterLast('/').ifBlank { recyclePath.substringAfterLast('/') } }
        )
    }

    fun encode(): String = listOf(
        recyclePath,
        originalPath,
        originalName,
        deletedAtMillis.toString()
    ).joinToString("|") { Uri.encode(it) }

    companion object {
        fun decode(encoded: String): RecycleBinRecord? {
            val parts = encoded.split('|')
            if (parts.size != 4) return null
            val recyclePath = Uri.decode(parts[0])
            val originalPath = Uri.decode(parts[1])
            val originalName = Uri.decode(parts[2])
            val deletedAtMillis = Uri.decode(parts[3]).toLongOrNull() ?: return null
            if (recyclePath.isBlank() || originalPath.isBlank()) return null
            return RecycleBinRecord(
                recyclePath = recyclePath,
                originalPath = originalPath,
                originalName = originalName,
                deletedAtMillis = deletedAtMillis
            )
        }
    }
}