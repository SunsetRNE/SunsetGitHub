package com.Sunset.REN.GitHub.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.Sunset.REN.GitHub.data.local.RepositoryNavigationPreferencesRepository
import com.Sunset.REN.GitHub.data.local.ThemePreferenceStore
import com.Sunset.REN.GitHub.ui.debug.UiDebugConfig
import com.Sunset.REN.GitHub.ui.repo.RepositorySection

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val themePreferenceStore = ThemePreferenceStore(application)
    private val uiDebugConfig = UiDebugConfig(application)
    private val repositoryNavigationPreferencesRepository = RepositoryNavigationPreferencesRepository(application)

    private val _isFloatingNavigationEnabled = MutableLiveData(
        themePreferenceStore.isFloatingNavigationEnabled()
    )
    val isFloatingNavigationEnabled: LiveData<Boolean> = _isFloatingNavigationEnabled

    private val _isSoraEditorEnabled = MutableLiveData(
        themePreferenceStore.isSoraEditorEnabled()
    )
    val isSoraEditorEnabled: LiveData<Boolean> = _isSoraEditorEnabled

    private val _isUiDebugOverlayEnabled = MutableLiveData(
        uiDebugConfig.isOverlayEnabled()
    )
    val isUiDebugOverlayEnabled: LiveData<Boolean> = _isUiDebugOverlayEnabled

    private val _defaultSectionOrder = MutableLiveData(
        repositoryNavigationPreferencesRepository.getDefaultSectionOrder()
    )
    val defaultSectionOrder: LiveData<List<RepositorySection>> = _defaultSectionOrder

    fun setFloatingNavigationEnabled(isEnabled: Boolean) {
        themePreferenceStore.setFloatingNavigationEnabled(isEnabled)
        _isFloatingNavigationEnabled.value = isEnabled
    }

    fun setSoraEditorEnabled(isEnabled: Boolean) {
        themePreferenceStore.setSoraEditorEnabled(isEnabled)
        _isSoraEditorEnabled.value = isEnabled
    }

    fun setUiDebugOverlayEnabled(isEnabled: Boolean) {
        uiDebugConfig.setOverlayEnabled(isEnabled)
        _isUiDebugOverlayEnabled.value = uiDebugConfig.isOverlayEnabled()
    }

    fun setDefaultSectionOrder(sections: List<RepositorySection>) {
        repositoryNavigationPreferencesRepository.setDefaultSectionOrder(sections)
        _defaultSectionOrder.value = repositoryNavigationPreferencesRepository.getDefaultSectionOrder()
    }
}