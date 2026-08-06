package com.Sunset.REN.GitHub.ui.debug

import android.content.Context
import android.content.SharedPreferences
import com.Sunset.REN.GitHub.BuildConfig

class UiDebugConfig(context: Context) {

    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun isOverlayEnabled(): Boolean {
        return BuildConfig.DEBUG && sharedPreferences.getBoolean(KeyOverlayEnabled, DefaultOverlayEnabled)
    }

    fun setOverlayEnabled(isEnabled: Boolean) {
        if (!BuildConfig.DEBUG) return
        sharedPreferences.edit()
            .putBoolean(KeyOverlayEnabled, isEnabled)
            .apply()
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val PreferencesName = "ui_debug_preferences"
        const val KeyOverlayEnabled = "ui_debug_overlay_enabled"
        private const val DefaultOverlayEnabled = false
    }
}
