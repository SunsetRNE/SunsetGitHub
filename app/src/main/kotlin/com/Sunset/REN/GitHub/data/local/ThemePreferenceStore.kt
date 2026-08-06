package com.Sunset.REN.GitHub.data.local

import android.content.Context
import android.content.SharedPreferences

class ThemePreferenceStore(context: Context) {

    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun isFloatingNavigationEnabled(): Boolean {
        return sharedPreferences.getBoolean(KeyFloatingNavigationEnabled, false)
    }

    fun setFloatingNavigationEnabled(isEnabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KeyFloatingNavigationEnabled, isEnabled)
            .apply()
    }

    fun isSoraEditorEnabled(): Boolean {
        return sharedPreferences.getBoolean(KeySoraEditorEnabled, true)
    }

    fun setSoraEditorEnabled(isEnabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KeySoraEditorEnabled, isEnabled)
            .apply()
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val PreferencesName = "theme_preferences"
        const val KeyFloatingNavigationEnabled = "floating_navigation_enabled"
        const val KeySoraEditorEnabled = "sora_editor_enabled"
    }
}