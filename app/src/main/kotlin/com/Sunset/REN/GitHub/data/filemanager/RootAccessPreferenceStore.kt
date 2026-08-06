package com.Sunset.REN.GitHub.data.filemanager

import android.content.Context

class RootAccessPreferenceStore(context: Context) {
    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun hasGrantedRootBefore(): Boolean = sharedPreferences.getBoolean(KeyGrantedBefore, false)

    fun markRootGranted(lastPath: String? = null) {
        sharedPreferences.edit()
            .putBoolean(KeyGrantedBefore, true)
            .apply {
                lastPath?.takeIf { it.isNotBlank() }?.let { putString(KeyLastRootPath, it) }
            }
            .apply()
    }

    fun clearRootGranted() {
        sharedPreferences.edit()
            .putBoolean(KeyGrantedBefore, false)
            .apply()
    }

    fun lastRootPath(): String = sharedPreferences.getString(KeyLastRootPath, DefaultRootPath).orEmpty()
        .ifBlank { DefaultRootPath }

    private companion object {
        const val PreferencesName = "file_manager_root_access"
        const val KeyGrantedBefore = "granted_before"
        const val KeyLastRootPath = "last_root_path"
        const val DefaultRootPath = "/"
    }
}
