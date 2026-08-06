package com.Sunset.REN.GitHub.data.filemanager

import android.content.Context
import com.Sunset.REN.GitHub.domain.filemanager.ApkInstallStrategy
import com.Sunset.REN.GitHub.domain.filemanager.BookmarkSettings
import com.Sunset.REN.GitHub.domain.filemanager.DirectoryListOptionsOverride
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerApkInstallSettings
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerListOptions
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerMenuOrder
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerMenuOrderSettings
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerOpenWithOrder
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerOperationSettings
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerPaneId
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerSortMode
import com.Sunset.REN.GitHub.domain.filemanager.RecycleBinSettings
import com.Sunset.REN.GitHub.domain.filemanager.PaneStartupSettings
import com.Sunset.REN.GitHub.domain.filemanager.StartupPathMode
import com.Sunset.REN.GitHub.domain.filemanager.root.RootAccessBackend
import com.Sunset.REN.GitHub.domain.filemanager.root.RootAccessSettings
import com.Sunset.REN.GitHub.domain.filemanager.root.RootStartupPolicy
import org.json.JSONArray
import org.json.JSONObject

class LocalFileManagerSettingsStore(context: Context) {
    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun recycleBinSettings(): RecycleBinSettings {
        return RecycleBinSettings(
            enabled = sharedPreferences.getBoolean(KeyRecycleBinEnabled, true),
            defaultMoveToRecycleBin = sharedPreferences.getBoolean(KeyRecycleBinDefaultMove, true),
            autoCleanDays = sharedPreferences.getInt(KeyRecycleBinAutoCleanDays, 0).coerceAtLeast(0),
            showDeletionWarning = sharedPreferences.getBoolean(KeyRecycleBinShowDeletionWarning, true)
        )
    }

    fun setRecycleBinSettings(settings: RecycleBinSettings) {
        sharedPreferences.edit()
            .putBoolean(KeyRecycleBinEnabled, settings.enabled)
            .putBoolean(KeyRecycleBinDefaultMove, settings.defaultMoveToRecycleBin)
            .putInt(KeyRecycleBinAutoCleanDays, settings.autoCleanDays.coerceAtLeast(0))
            .putBoolean(KeyRecycleBinShowDeletionWarning, settings.showDeletionWarning)
            .apply()
    }
    fun rootAccessSettings(): RootAccessSettings {
        val backend = sharedPreferences.getString(KeyRootBackend, null)
            ?.let { value -> RootAccessBackend.entries.firstOrNull { it.name == value } }
            ?: RootAccessBackend.Su
        val startupPolicy = sharedPreferences.getString(KeyRootStartupPolicy, null)
            ?.let { value -> RootStartupPolicy.entries.firstOrNull { it.name == value } }
            ?: RootStartupPolicy.DetectOnly
        return RootAccessSettings(
            backend = backend,
            startupPolicy = startupPolicy,
            suCommand = sharedPreferences.getString(KeyRootSuCommand, null).orEmpty().ifBlank { RootAccessSettings.DefaultSuCommand }
        ).normalized()
    }

    fun setRootAccessSettings(settings: RootAccessSettings) {
        val normalized = settings.normalized()
        sharedPreferences.edit()
            .putString(KeyRootBackend, normalized.backend.name)
            .putString(KeyRootStartupPolicy, normalized.startupPolicy.name)
            .putString(KeyRootSuCommand, normalized.suCommand)
            .apply()
    }

    fun isRecycleBinEnabled(): Boolean = recycleBinSettings().enabled

    fun setRecycleBinEnabled(enabled: Boolean) {
        setRecycleBinSettings(recycleBinSettings().copy(enabled = enabled))
    }

    fun toggleRecycleBinEnabled(): Boolean {
        val enabled = !isRecycleBinEnabled()
        setRecycleBinEnabled(enabled)
        return enabled
    }

    fun bookmarkSettings(): BookmarkSettings {
        return BookmarkSettings(
            showInSidebar = sharedPreferences.getBoolean(KeyBookmarkShowInSidebar, false),
            addToTop = sharedPreferences.getBoolean(KeyBookmarkAddToTop, true),
            swipePositionAware = sharedPreferences.getBoolean(KeyBookmarkSwipePositionAware, false)
        )
    }

    fun setBookmarkSettings(settings: BookmarkSettings) {
        sharedPreferences.edit()
            .putBoolean(KeyBookmarkShowInSidebar, settings.showInSidebar)
            .putBoolean(KeyBookmarkAddToTop, settings.addToTop)
            .putBoolean(KeyBookmarkSwipePositionAware, settings.swipePositionAware)
            .apply()
    }

    fun listOptionsForPane(pane: FileManagerPaneId): FileManagerListOptions {
        val prefix = listOptionsKeyPrefix(pane)
        val sortMode = sharedPreferences.getString("${prefix}sort_mode", null)
            ?.toSortModeOrNull()
            ?: FileManagerSortMode.Name
        return FileManagerListOptions(
            sortMode = sortMode,
            reverse = sharedPreferences.getBoolean("${prefix}reverse", false),
            showHiddenFiles = sharedPreferences.getBoolean("${prefix}show_hidden_files", false),
            showManualHiddenFiles = sharedPreferences.getBoolean("${prefix}show_manual_hidden_files", false),
            useShortYear = sharedPreferences.getBoolean("${prefix}use_short_year", true),
            showSeconds = sharedPreferences.getBoolean("${prefix}show_seconds", false),
            showPermissions = sharedPreferences.getBoolean("${prefix}show_permissions", false)
        )
    }

    fun setListOptionsForPane(pane: FileManagerPaneId, options: FileManagerListOptions) {
        val prefix = listOptionsKeyPrefix(pane)
        sharedPreferences.edit()
            .putString("${prefix}sort_mode", options.sortMode.name)
            .putBoolean("${prefix}reverse", options.reverse)
            .putBoolean("${prefix}show_hidden_files", options.showHiddenFiles)
            .putBoolean("${prefix}show_manual_hidden_files", options.showManualHiddenFiles)
            .putBoolean("${prefix}use_short_year", options.useShortYear)
            .putBoolean("${prefix}show_seconds", options.showSeconds)
            .putBoolean("${prefix}show_permissions", options.showPermissions)
            .apply()
    }

    fun directoryListOptionsOverrideForPane(
        pane: FileManagerPaneId,
        path: String
    ): DirectoryListOptionsOverride? {
        val normalizedPath = normalizePathKey(path)
        return directoryListOptionsOverrides().firstOrNull { override ->
            override.pane == pane && normalizePathKey(override.path) == normalizedPath
        }
    }

    fun directoryListOptionsOverrides(): List<DirectoryListOptionsOverride> {
        val raw = sharedPreferences.getString(KeyDirectoryListOptionsOverrides, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val pane = item.optString("pane").toPaneIdOrNull() ?: continue
                    val path = item.optString("path").takeIf { it.isNotBlank() } ?: continue
                    val sortMode = item.optString("sortMode").toSortModeOrNull() ?: FileManagerSortMode.Name
                    add(
                        DirectoryListOptionsOverride(
                            pane = pane,
                            path = path,
                            options = FileManagerListOptions(
                                sortMode = sortMode,
                                reverse = item.optBoolean("reverse", false),
                                showHiddenFiles = item.optBoolean("showHiddenFiles", false),
                                showManualHiddenFiles = item.optBoolean("showManualHiddenFiles", false),
                                useShortYear = item.optBoolean("useShortYear", true),
                                showSeconds = item.optBoolean("showSeconds", false),
                                showPermissions = item.optBoolean("showPermissions", false)
                            )
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun setDirectoryListOptionsOverride(
        pane: FileManagerPaneId,
        path: String,
        options: FileManagerListOptions
    ) {
        val normalizedPath = normalizePathKey(path)
        if (normalizedPath.isBlank()) return
        val updated = directoryListOptionsOverrides()
            .filterNot { override ->
                override.pane == pane && normalizePathKey(override.path) == normalizedPath
            }
            .plus(DirectoryListOptionsOverride(pane = pane, path = normalizedPath, options = options))
        saveDirectoryListOptionsOverrides(updated)
    }

    fun removeDirectoryListOptionsOverride(pane: FileManagerPaneId, path: String) {
        val normalizedPath = normalizePathKey(path)
        val updated = directoryListOptionsOverrides().filterNot { override ->
            override.pane == pane && normalizePathKey(override.path) == normalizedPath
        }
        saveDirectoryListOptionsOverrides(updated)
    }

    fun clearDirectoryListOptionsOverrides() {
        sharedPreferences.edit()
            .remove(KeyDirectoryListOptionsOverrides)
            .apply()
    }

    fun paneStartupSettings(pane: FileManagerPaneId): PaneStartupSettings {
        val mode = sharedPreferences.getString(startupModeKey(pane), null)
            ?.let { value -> StartupPathMode.entries.firstOrNull { it.name == value } }
            ?: StartupPathMode.Home
        val homePath = sharedPreferences.getString(homePathKey(pane), null)
            ?.takeIf { it.isNotBlank() }
        return PaneStartupSettings(mode = mode, homePath = homePath)
    }

    fun setPaneStartupSettings(pane: FileManagerPaneId, settings: PaneStartupSettings) {
        sharedPreferences.edit()
            .putString(startupModeKey(pane), settings.mode.name)
            .apply {
                val homePath = settings.homePath
                if (homePath.isNullOrBlank()) remove(homePathKey(pane)) else putString(homePathKey(pane), homePath)
            }
            .apply()
    }

    fun homePathForPane(pane: FileManagerPaneId, defaultPath: String): String {
        return paneStartupSettings(pane).homePath ?: defaultPath
    }

    fun setHomePathForPane(pane: FileManagerPaneId, path: String) {
        if (path.isBlank()) return
        val settings = paneStartupSettings(pane)
        setPaneStartupSettings(pane, settings.copy(homePath = path))
    }

    fun operationSettings(): FileManagerOperationSettings {
        return FileManagerOperationSettings(
            backupBeforeTextSave = sharedPreferences.getBoolean(KeyBackupBeforeTextSave, true),
            preserveModifiedTimeOnCopy = sharedPreferences.getBoolean(KeyPreserveModifiedTimeOnCopy, true),
            preserveModifiedTimeOnExtract = sharedPreferences.getBoolean(KeyPreserveModifiedTimeOnExtract, true)
        )
    }

    fun setOperationSettings(settings: FileManagerOperationSettings) {
        sharedPreferences.edit()
            .putBoolean(KeyBackupBeforeTextSave, settings.backupBeforeTextSave)
            .putBoolean(KeyPreserveModifiedTimeOnCopy, settings.preserveModifiedTimeOnCopy)
            .putBoolean(KeyPreserveModifiedTimeOnExtract, settings.preserveModifiedTimeOnExtract)
            .apply()
    }

    fun apkInstallSettings(): FileManagerApkInstallSettings {
        val strategy = sharedPreferences.getString(KeyApkInstallStrategy, null)
            ?.let { value -> ApkInstallStrategy.entries.firstOrNull { it.name == value } }
            ?: ApkInstallStrategy.SystemPackageInstaller
        return FileManagerApkInstallSettings(strategy = strategy)
    }

    fun setApkInstallSettings(settings: FileManagerApkInstallSettings) {
        sharedPreferences.edit()
            .putString(KeyApkInstallStrategy, settings.strategy.name)
            .apply()
    }

    fun menuOrderSettings(): FileManagerMenuOrderSettings {
        val fileMenuOrder = sharedPreferences.getString(KeyFileMenuOrder, null)
            ?.let { value -> FileManagerMenuOrder.entries.firstOrNull { it.name == value } }
            ?: FileManagerMenuOrder.MtClassic
        val openWithOrder = sharedPreferences.getString(KeyOpenWithOrder, null)
            ?.let { value -> FileManagerOpenWithOrder.entries.firstOrNull { it.name == value } }
            ?: FileManagerOpenWithOrder.SystemDefault
        return FileManagerMenuOrderSettings(fileMenuOrder = fileMenuOrder, openWithOrder = openWithOrder)
    }

    fun setMenuOrderSettings(settings: FileManagerMenuOrderSettings) {
        sharedPreferences.edit()
            .putString(KeyFileMenuOrder, settings.fileMenuOrder.name)
            .putString(KeyOpenWithOrder, settings.openWithOrder.name)
            .apply()
    }

    private fun saveDirectoryListOptionsOverrides(overrides: List<DirectoryListOptionsOverride>) {
        val array = JSONArray()
        overrides.forEach { override ->
            array.put(
                JSONObject()
                    .put("pane", override.pane.name)
                    .put("path", normalizePathKey(override.path))
                    .put("sortMode", override.options.sortMode.name)
                    .put("reverse", override.options.reverse)
                    .put("showHiddenFiles", override.options.showHiddenFiles)
                    .put("showManualHiddenFiles", override.options.showManualHiddenFiles)
                    .put("useShortYear", override.options.useShortYear)
                    .put("showSeconds", override.options.showSeconds)
                    .put("showPermissions", override.options.showPermissions)
            )
        }
        sharedPreferences.edit()
            .putString(KeyDirectoryListOptionsOverrides, array.toString())
            .apply()
    }

    private fun String.toPaneIdOrNull(): FileManagerPaneId? {
        return FileManagerPaneId.entries.firstOrNull { it.name == this }
    }

    private fun String.toSortModeOrNull(): FileManagerSortMode? {
        return FileManagerSortMode.entries.firstOrNull { it.name == this }
    }

    private fun normalizePathKey(path: String): String {
        return path.trim()
            .replace('\\', '/')
            .trimEnd('/')
            .ifBlank { path.trim() }
    }

    private fun homePathKey(pane: FileManagerPaneId): String = "${KeyHomePathPrefix}${pane.name.lowercase()}"

    private fun startupModeKey(pane: FileManagerPaneId): String = "${KeyStartupModePrefix}${pane.name.lowercase()}"

    private fun listOptionsKeyPrefix(pane: FileManagerPaneId): String {
        return "${KeyPaneListOptionsPrefix}${pane.name.lowercase()}_"
    }

    private companion object {
        const val PreferencesName = "file_manager_settings"
        const val KeyRecycleBinEnabled = "recycle_bin_enabled"
        const val KeyRecycleBinDefaultMove = "recycle_bin_default_move"
        const val KeyRecycleBinAutoCleanDays = "recycle_bin_auto_clean_days"
        const val KeyRecycleBinShowDeletionWarning = "recycle_bin_show_deletion_warning"
        const val KeyRootBackend = "root_backend"
        const val KeyRootStartupPolicy = "root_startup_policy"
        const val KeyRootSuCommand = "root_su_command"
        const val KeyHomePathPrefix = "home_path_"
        const val KeyStartupModePrefix = "pane_startup_mode_"
        const val KeyPaneListOptionsPrefix = "pane_list_options_"
        const val KeyDirectoryListOptionsOverrides = "directory_list_options_overrides"
        const val KeyBookmarkShowInSidebar = "bookmark_show_in_sidebar"
        const val KeyBookmarkAddToTop = "bookmark_add_to_top"
        const val KeyBookmarkSwipePositionAware = "bookmark_swipe_position_aware"
        const val KeyBackupBeforeTextSave = "backup_before_text_save"
        const val KeyPreserveModifiedTimeOnCopy = "preserve_modified_time_on_copy"
        const val KeyPreserveModifiedTimeOnExtract = "preserve_modified_time_on_extract"
        const val KeyApkInstallStrategy = "apk_install_strategy"
        const val KeyFileMenuOrder = "file_menu_order"
        const val KeyOpenWithOrder = "open_with_order"
    }
}