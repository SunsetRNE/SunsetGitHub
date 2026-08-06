package com.Sunset.REN.GitHub.data.filemanager

import android.content.Context
import com.Sunset.REN.GitHub.domain.filemanager.ManualHiddenFileRule
import com.Sunset.REN.GitHub.domain.filemanager.ManualHiddenFileRuleType
import org.json.JSONArray
import org.json.JSONObject

class ManualHiddenFileStore(context: Context) {
    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun rules(): List<ManualHiddenFileRule> {
        val raw = sharedPreferences.getString(KeyRules, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val path = normalizePath(item.optString("path")).takeIf { it.isNotBlank() } ?: continue
                    val type = item.optString("type")
                        .takeIf { it.isNotBlank() }
                        ?.let { rawType -> ManualHiddenFileRuleType.entries.firstOrNull { it.name == rawType } }
                        ?: ManualHiddenFileRuleType.ExactPath
                    add(ManualHiddenFileRule(path = path, type = type))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addPaths(paths: Collection<String>): Int {
        val normalized = paths.map(::normalizePath).filter { it.isNotBlank() }.distinct()
        if (normalized.isEmpty()) return 0
        val existing = rules()
        val existingPaths = existing.map { normalizePath(it.path) }.toSet()
        val additions = normalized
            .filterNot { it in existingPaths }
            .map { ManualHiddenFileRule(path = it) }
        if (additions.isEmpty()) return 0
        saveRules(existing + additions)
        return additions.size
    }

    fun removePath(path: String): Boolean {
        val normalized = normalizePath(path)
        val old = rules()
        val updated = old.filterNot { normalizePath(it.path) == normalized }
        if (updated.size == old.size) return false
        saveRules(updated)
        return true
    }

    fun clear() {
        sharedPreferences.edit().remove(KeyRules).apply()
    }

    fun isHidden(path: String): Boolean {
        val normalized = normalizePath(path)
        return rules().any { rule ->
            when (rule.type) {
                ManualHiddenFileRuleType.ExactPath -> normalizePath(rule.path) == normalized
            }
        }
    }

    private fun saveRules(rules: List<ManualHiddenFileRule>) {
        val array = JSONArray()
        rules.distinctBy { normalizePath(it.path) }.forEach { rule ->
            array.put(
                JSONObject()
                    .put("path", normalizePath(rule.path))
                    .put("type", rule.type.name)
            )
        }
        sharedPreferences.edit().putString(KeyRules, array.toString()).apply()
    }

    private fun normalizePath(path: String): String {
        return path.trim().replace('\\', '/').trimEnd('/').ifBlank { path.trim() }
    }

    private companion object {
        const val PreferencesName = "file_manager_manual_hidden"
        const val KeyRules = "rules"
    }
}