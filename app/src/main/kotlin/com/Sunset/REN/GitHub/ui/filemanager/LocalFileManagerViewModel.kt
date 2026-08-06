package com.Sunset.REN.GitHub.ui.filemanager

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import java.util.ArrayDeque
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.filemanager.FavoriteDirectoryRecord
import com.Sunset.REN.GitHub.data.filemanager.FavoriteDirectoryStore
import com.Sunset.REN.GitHub.data.filemanager.FavoriteDirectoryType
import com.Sunset.REN.GitHub.data.filemanager.FileContentAccessRepository
import com.Sunset.REN.GitHub.data.filemanager.LocalFileAccessProvider
import com.Sunset.REN.GitHub.data.filemanager.LocalFileDirectoryListingCache
import com.Sunset.REN.GitHub.data.filemanager.LocalFileManagerSettingsStore
import com.Sunset.REN.GitHub.data.filemanager.ManualHiddenFileStore
import com.Sunset.REN.GitHub.data.filemanager.RecentDirectoryRecord
import com.Sunset.REN.GitHub.data.filemanager.RecentDirectoryStore
import com.Sunset.REN.GitHub.data.filemanager.RootAccessPreferenceStore
import com.Sunset.REN.GitHub.data.filemanager.SafDirectoryRecord
import com.Sunset.REN.GitHub.data.filemanager.SafDirectoryStore
import com.Sunset.REN.GitHub.data.filemanager.SafFileAccessProvider
import com.Sunset.REN.GitHub.data.filemanager.provider.ArchiveFileSystemProvider
import com.Sunset.REN.GitHub.data.filemanager.provider.LocalFileSystemProvider
import com.Sunset.REN.GitHub.data.filemanager.provider.RootFileSystemProvider
import com.Sunset.REN.GitHub.data.filemanager.provider.SafFileSystemProvider
import com.Sunset.REN.GitHub.domain.filemanager.ArchiveFormatResolver
import com.Sunset.REN.GitHub.domain.filemanager.BookmarkSettings
import com.Sunset.REN.GitHub.domain.filemanager.DirectoryListOptionsOverride
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryCapabilities
import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryTypeResolver
import com.Sunset.REN.GitHub.domain.filemanager.FileLocation
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerApkInstallSettings
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntrySorter
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerListOptions
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerMenuOrderSettings
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerOperationSettings
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerPaneId
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerSearchOptions
import com.Sunset.REN.GitHub.domain.filemanager.PaneStartupSettings
import com.Sunset.REN.GitHub.domain.filemanager.RecycleBinSettings
import com.Sunset.REN.GitHub.domain.filemanager.StartupPathMode
import com.Sunset.REN.GitHub.domain.filemanager.root.RootAccessManager
import com.Sunset.REN.GitHub.domain.filemanager.root.RootAccessSettings
import com.Sunset.REN.GitHub.domain.filemanager.root.RootAccessState
import com.Sunset.REN.GitHub.domain.filemanager.root.RootStartupPolicy
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileListResult
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileReadResult
import com.Sunset.REN.GitHub.domain.filemanager.provider.FileSystemProviderRegistry
import com.Sunset.REN.GitHub.domain.filemanager.path.FileManagerPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class LocalFileManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val localProvider = LocalFileAccessProvider(application)
    private val safProvider = SafFileAccessProvider(application)
    private val contentAccessRepository = FileContentAccessRepository(application)
    private val safDirectoryStore = SafDirectoryStore(application)
    private val favoriteDirectoryStore = FavoriteDirectoryStore(application)
    private val recentDirectoryStore = RecentDirectoryStore(application)
    private val settingsStore = LocalFileManagerSettingsStore(application)
    private val manualHiddenFileStore = ManualHiddenFileStore(application)
    private val fileTransferNamingPolicy = FileTransferNamingPolicy(application)
    private val recycleBinCoordinator = RecycleBinCoordinator(
        context = application,
        safProvider = safProvider,
        settingsStore = settingsStore,
        transferNamingPolicy = fileTransferNamingPolicy
    )
    private val archiveOperationCoordinator by lazy {
        ArchiveOperationCoordinator(
            context = getApplication<Application>(),
            navigator = fileManagerNavigator,
            namingPolicy = fileTransferNamingPolicy,
            listArchiveDirectory = { archiveFile, innerPath -> listArchiveDirectory(archiveFile, innerPath).getOrThrow() }
        )
    }
    private val fileTransferCoordinator by lazy {
        FileTransferCoordinator(
            context = getApplication<Application>(),
            safProvider = safProvider,
            navigator = fileManagerNavigator,
            namingPolicy = fileTransferNamingPolicy,
            copyArchiveEntryToLocalDirectory = archiveOperationCoordinator::copyArchiveEntryToLocalDirectory,
            copyArchiveEntryToSafDirectory = archiveOperationCoordinator::copyArchiveEntryToSafDirectory,
            deleteEntryPermanently = recycleBinCoordinator::deleteEntryPermanently
        )
    }
    private val textExportCoordinator by lazy {
        TextExportCoordinator(
            context = getApplication<Application>(),
            contentAccessRepository = contentAccessRepository,
            safProvider = safProvider,
            navigator = fileManagerNavigator,
            cacheArchiveEntryForContentAccess = archiveOperationCoordinator::cacheArchiveEntryForContentAccess
        )
    }
    private val archiveExtractionSourceResolver by lazy {
        ArchiveExtractionSourceResolver(
            context = getApplication<Application>(),
            cacheArchiveEntryForContentAccess = archiveOperationCoordinator::cacheArchiveEntryForContentAccess
        )
    }
    private val archiveExtractionNamingPolicy by lazy {
        ArchiveExtractionNamingPolicy(
            defaultArchiveName = getApplication<Application>().getString(R.string.local_file_manager_batch_zip_default_name)
        )
    }
    private val archiveExtractionExecutor by lazy {
        ArchiveExtractionExecutor(
            context = getApplication<Application>(),
            safProvider = safProvider,
            namingPolicy = archiveExtractionNamingPolicy
        )
    }
    private val rootAccessPreferenceStore = RootAccessPreferenceStore(application)
    private val rootAccessManager = RootAccessManager().also { manager ->
        manager.updateSettings(settingsStore.rootAccessSettings())
    }
    private val rootFileSystemProvider = RootFileSystemProvider(rootAccessManager)
    private val fileManagerNavigator = FileManagerNavigator()
    private val fileSystemProviderRegistry = FileSystemProviderRegistry(
        listOf(
            LocalFileSystemProvider(localProvider),
            SafFileSystemProvider(application, safProvider),
            ArchiveFileSystemProvider(),
            rootFileSystemProvider
        )
    )
    private val _state = MutableLiveData(LocalFileManagerUiState())
    val state: LiveData<LocalFileManagerUiState> = _state

    private var currentLocation: CurrentLocation = CurrentLocation.Local(
        directory = localProvider.resolveLocation(FileLocation.AppFiles),
        hint = application.getString(R.string.local_file_manager_location_app_files)
    )
    private val backStack = ArrayDeque<CurrentLocation>()
    private val forwardStack = ArrayDeque<CurrentLocation>()
    private var directoryLoadGeneration: Long = 0L

    fun recycleBinSettings(): RecycleBinSettings {
        return recycleBinCoordinator.settings()
    }

    fun setRecycleBinSettings(settings: RecycleBinSettings) {
        recycleBinCoordinator.setSettings(settings)
        _state.value = (_state.value ?: buildState(currentLocation, isLoading = false)).copy(isRecycleBinEnabled = settings.enabled)
    }

    fun rootAccessSettings(): RootAccessSettings {
        return settingsStore.rootAccessSettings()
    }

    fun setRootAccessSettings(settings: RootAccessSettings) {
        val normalized = settings.normalized()
        settingsStore.setRootAccessSettings(normalized)
        rootAccessManager.updateSettings(normalized)
        updateRootState(RootAccessState.Unknown)
        if (normalized.startupPolicy != RootStartupPolicy.Disabled) {
            detectRootAvailabilityIfNeeded(force = true)
        }
    }

    fun bookmarkSettings(): BookmarkSettings {
        return settingsStore.bookmarkSettings()
    }

    fun setBookmarkSettings(settings: BookmarkSettings) {
        settingsStore.setBookmarkSettings(settings)
    }

    fun listOptionsForPane(pane: FileManagerPaneId): FileManagerListOptions {
        return settingsStore.listOptionsForPane(pane)
    }

    fun setListOptionsForPane(pane: FileManagerPaneId, options: FileManagerListOptions) {
        settingsStore.setListOptionsForPane(pane, options)
    }

    fun operationSettings(): FileManagerOperationSettings = settingsStore.operationSettings()

    fun setOperationSettings(settings: FileManagerOperationSettings) {
        settingsStore.setOperationSettings(settings)
    }

    fun apkInstallSettings(): FileManagerApkInstallSettings = settingsStore.apkInstallSettings()

    fun setApkInstallSettings(settings: FileManagerApkInstallSettings) {
        settingsStore.setApkInstallSettings(settings)
    }

    fun menuOrderSettings(): FileManagerMenuOrderSettings = settingsStore.menuOrderSettings()

    fun setMenuOrderSettings(settings: FileManagerMenuOrderSettings) {
        settingsStore.setMenuOrderSettings(settings)
    }

    fun manualHiddenFileRules() = manualHiddenFileStore.rules()

    fun isManuallyHidden(path: String): Boolean = manualHiddenFileStore.isHidden(path)

    fun addManualHiddenPaths(paths: Collection<String>): Int = manualHiddenFileStore.addPaths(paths)

    fun removeManualHiddenPath(path: String): Boolean = manualHiddenFileStore.removePath(path)

    fun clearManualHiddenFiles() = manualHiddenFileStore.clear()

    fun directoryListOptionsOverrideForPane(
        pane: FileManagerPaneId,
        path: String
    ): DirectoryListOptionsOverride? {
        return settingsStore.directoryListOptionsOverrideForPane(pane, path)
    }

    fun directoryListOptionsOverrides(): List<DirectoryListOptionsOverride> {
        return settingsStore.directoryListOptionsOverrides()
    }

    fun setDirectoryListOptionsOverride(
        pane: FileManagerPaneId,
        path: String,
        options: FileManagerListOptions
    ) {
        settingsStore.setDirectoryListOptionsOverride(pane, path, options)
    }

    fun removeDirectoryListOptionsOverride(pane: FileManagerPaneId, path: String) {
        settingsStore.removeDirectoryListOptionsOverride(pane, path)
    }

    fun clearDirectoryListOptionsOverrides() {
        settingsStore.clearDirectoryListOptionsOverrides()
    }

    fun paneStartupSettings(pane: FileManagerPaneId): PaneStartupSettings {
        return settingsStore.paneStartupSettings(pane)
    }

    fun setPaneStartupSettings(pane: FileManagerPaneId, settings: PaneStartupSettings) {
        settingsStore.setPaneStartupSettings(pane, settings)
    }

    fun startupPathForPane(pane: FileManagerPaneId, defaultPath: String): String {
        val settings = paneStartupSettings(pane)
        return when (settings.mode) {
            StartupPathMode.Home -> settings.homePath ?: defaultPath
            StartupPathMode.StorageRoot -> defaultPath
            StartupPathMode.AppFiles -> localProvider.resolveLocation(FileLocation.AppFiles).absolutePath
        }
    }

    fun setStartupPathForPane(pane: FileManagerPaneId, path: String) {
        val settings = paneStartupSettings(pane)
        settingsStore.setPaneStartupSettings(pane, settings.copy(mode = StartupPathMode.Home, homePath = path))
    }

    fun loadInitialLocation() {
        if (_state.value?.entries?.isNotEmpty() == true || _state.value?.isLoading == true) return
        refreshAuthorizedDirectories()
        applyRootStartupPolicy()
        openLocation(FileLocation.AppFiles)
    }

    fun detectRootAvailabilityIfNeeded(force: Boolean = false) {
        if (!force && (_state.value?.rootAccessState ?: rootAccessManager.currentState()) !is RootAccessState.Unknown) return
        viewModelScope.launch {
            detectRootAvailabilityInternal(refreshCurrentDirectoryOnGranted = true)
        }
    }

    private fun applyRootStartupPolicy() {
        when (rootAccessSettings().startupPolicy) {
            RootStartupPolicy.Disabled -> Unit
            RootStartupPolicy.DetectOnly -> detectRootAvailabilityIfNeeded()
            RootStartupPolicy.RequestOnStartup -> requestRootAccess()
        }
    }

    fun openLocalDirectoryPreloadedAfterRootDetection(directory: File) {
        viewModelScope.launch {
            if (rootAccessSettings().startupPolicy != RootStartupPolicy.Disabled && (_state.value?.rootAccessState ?: rootAccessManager.currentState()) is RootAccessState.Unknown) {
                detectRootAvailabilityInternal(refreshCurrentDirectoryOnGranted = false)
            }
            openLocalDirectoryPreloaded(directory)
        }
    }

    private suspend fun detectRootAvailabilityInternal(refreshCurrentDirectoryOnGranted: Boolean): RootAccessState {
        val detectedState = rootAccessManager.detect()
        val state = if (detectedState is RootAccessState.AvailableButNotGranted && rootAccessPreferenceStore.hasGrantedRootBefore()) {
            rootAccessManager.verifyGranted()
        } else {
            detectedState
        }
        applyRootAccessResult(state)
        if (refreshCurrentDirectoryOnGranted && state is RootAccessState.Granted) {
            loadCurrentDirectory(preferCachedListing = false)
        }
        return state
    }

    fun requestRootAccess() {
        requestRootAccessThenOpen(path = null)
    }

    fun requestRootAccessThenOpen(path: String?) {
        updateRootState(RootAccessState.Requesting)
        viewModelScope.launch {
            val normalizedPath = path?.let(::normalizeRootPath)
            val state = rootAccessManager.requestAccess()
            applyRootAccessResult(state, lastPath = normalizedPath)
            if (state is RootAccessState.Granted && normalizedPath != null) {
                navigateTo(CurrentLocation.Root(normalizedPath, getApplication<Application>().getString(R.string.local_file_manager_root_location)))
            }
        }
    }

    fun shouldVerifyRootAccessForNaturalNavigation(): Boolean {
        return rootAccessPreferenceStore.hasGrantedRootBefore() && rootAccessManager.currentState() !is RootAccessState.Granted
    }

    fun openRootDirectory(path: String) {
        if (rootAccessManager.currentState() !is RootAccessState.Granted) {
            requestRootAccessThenOpen(path)
            return
        }
        openGrantedRootDirectory(path)
    }

    private fun openGrantedRootDirectory(path: String) {
        val normalizedPath = normalizeRootPath(path)
        rootAccessPreferenceStore.markRootGranted(normalizedPath)
        navigateTo(CurrentLocation.Root(normalizedPath, getApplication<Application>().getString(R.string.local_file_manager_root_location)))
    }

    private fun applyRootAccessResult(rootAccessState: RootAccessState, lastPath: String? = null) {
        if (rootAccessState is RootAccessState.Granted) {
            rootAccessPreferenceStore.markRootGranted(lastPath)
        } else if (rootAccessState is RootAccessState.NotAvailable) {
            rootAccessPreferenceStore.clearRootGranted()
        }
        updateRootState(rootAccessState)
    }

    private fun updateRootState(rootAccessState: RootAccessState) {
        _state.value = (_state.value ?: buildState(currentLocation, isLoading = false)).copy(rootAccessState = rootAccessState)
    }

    fun openLocation(location: FileLocation) {
        val target = when (location) {
            FileLocation.AppFiles,
            FileLocation.AppCache,
            FileLocation.Downloads,
            is FileLocation.LocalPath -> CurrentLocation.Local(
                directory = localProvider.resolveLocation(location),
                hint = buildLocationHint(location)
            )
            is FileLocation.SafTree -> CurrentLocation.Saf(
                document = safProvider.treeFromUri(location.uri) ?: safProvider.documentFromUri(location.uri),
                uri = location.uri,
                hint = location.label.ifBlank { location.uri.toString() }
            )
        }
        replaceLocation(target)
    }

    fun openLocalDirectory(directory: File) {
        openLocalDirectory(directory, getApplication<Application>().getString(R.string.local_file_manager_location_local_path))
    }

    fun openLocalDirectoryPreloaded(directory: File) {
        val target = CurrentLocation.Local(
            directory = directory,
            hint = getApplication<Application>().getString(R.string.local_file_manager_location_local_path)
        )
        replaceLocationPreloaded(target)
    }

    fun openDirectoryPath(path: String): Boolean {
        return when (val target = fileManagerNavigator.parseDirectoryPath(path)) {
            is FileManagerNavigator.DirectoryPathTarget.Root -> {
                openRootDirectory(target.path)
                true
            }
            is FileManagerNavigator.DirectoryPathTarget.Archive -> openArchiveDirectory(target.archiveFile, target.innerPath)
            is FileManagerNavigator.DirectoryPathTarget.Saf -> {
                openSafDirectory(target.uri, path)
                true
            }
            is FileManagerNavigator.DirectoryPathTarget.Local -> {
                if (!target.directory.isDirectory) return false
                openLocalDirectory(target.directory)
                true
            }
        }
    }

    fun openSafDirectory(uri: Uri, label: String) {
        val document = safProvider.treeFromUri(uri) ?: safProvider.documentFromUri(uri)
        navigateTo(CurrentLocation.Saf(document = document, uri = uri, hint = label.ifBlank { uri.toString() }))
    }

    fun openArchiveEntry(entry: FileManagerEntry): Boolean {
        val archiveLocation = when (val source = entry.source) {
            is FileEntrySource.LocalFile -> {
                val file = source.file
                if (!file.isFile || !isBrowsableArchiveName(file.name)) return false
                CurrentLocation.Archive(
                    archiveFile = file,
                    innerPath = "",
                    hint = file.name.ifBlank { getApplication<Application>().getString(R.string.local_file_manager_archive_location_hint) }
                )
            }
            is FileEntrySource.ArchiveEntry -> {
                if (!source.isDirectory) return false
                CurrentLocation.Archive(
                    archiveFile = source.archiveFile,
                    innerPath = source.innerPath,
                    hint = source.archiveFile.name.ifBlank { getApplication<Application>().getString(R.string.local_file_manager_archive_location_hint) }
                )
            }
            else -> return false
        }
        navigateTo(archiveLocation)
        return true
    }

    fun openArchiveDirectory(archiveFile: File, innerPath: String): Boolean {
        if (!archiveFile.isFile || !isBrowsableArchiveName(archiveFile.name)) return false
        navigateTo(
            CurrentLocation.Archive(
                archiveFile = archiveFile,
                innerPath = normalizeArchiveInnerPath(innerPath),
                hint = archiveFile.name.ifBlank { getApplication<Application>().getString(R.string.local_file_manager_archive_location_hint) }
            )
        )
        return true
    }

    fun refresh() {
        (currentLocation as? CurrentLocation.Local)?.directory?.absolutePath?.let(LocalFileDirectoryListingCache::invalidate)
        loadCurrentDirectory(refreshDirectoryShortcuts = true, preferCachedListing = false)
    }
    suspend fun listDirectoryPath(path: String): Result<List<FileManagerEntry>> {
        return withContext(Dispatchers.IO) {
            val fileManagerPath = fileManagerNavigator.parseFileManagerPath(path)
            val result = when (fileManagerPath) {
                is FileManagerPath.Archive -> listArchiveDirectory(
                    archiveFile = File(fileManagerPath.archivePath),
                    innerPath = fileManagerPath.innerPath
                )
                else -> listDirectoryThroughProvider(fileManagerPath)
            }
            result.map { entries -> withParentDirectoryEntry(path, entries) }
        }
    }

    fun parentPathForDirectoryPath(path: String): String? {
        return fileManagerNavigator.parentPathForDirectoryPath(
            path = path,
            safParentUriFor = safProvider::parentUriFor
        )
    }


    fun toggleCurrentFavorite(): Boolean {
        val record = favoriteRecordFor(currentLocation)
        val isFavorite = favoriteDirectoryStore.isFavorite(record)
        if (isFavorite) {
            favoriteDirectoryStore.removeFavorite(record)
        } else {
            favoriteDirectoryStore.addFavorite(record, addToTop = settingsStore.bookmarkSettings().addToTop)
        }
        loadCurrentDirectory()
        return !isFavorite
    }

    fun openFavorite(record: FavoriteDirectoryRecord) {
        val target = when (record.type) {
            FavoriteDirectoryType.Local -> CurrentLocation.Local(
                directory = File(record.value),
                hint = record.label.ifBlank { getApplication<Application>().getString(R.string.local_file_manager_favorite_directory) }
            )
            FavoriteDirectoryType.Saf -> {
                val uri = Uri.parse(record.value)
                CurrentLocation.Saf(
                    document = safProvider.treeFromUri(uri) ?: safProvider.documentFromUri(uri),
                    uri = uri,
                    hint = record.label.ifBlank { uri.toString() }
                )
            }
            FavoriteDirectoryType.Archive -> {
                val archivePath = parseArchiveDisplayPath(record.value)
                    ?: throw IllegalArgumentException(getApplication<Application>().getString(R.string.local_file_manager_archive_browse_unsupported))
                CurrentLocation.Archive(
                    archiveFile = archivePath.archiveFile,
                    innerPath = archivePath.innerPath,
                    hint = record.label.ifBlank { archivePath.archiveFile.name.ifBlank { getApplication<Application>().getString(R.string.local_file_manager_archive_location_hint) } }
                )
            }
        }
        replaceLocation(target)
    }

    fun openRecent(record: RecentDirectoryRecord) {
        openFavorite(record.directory)
    }

    fun clearRecentDirectories() {
        recentDirectoryStore.clear()
        loadCurrentDirectory()
    }

    fun removeFavorite(record: FavoriteDirectoryRecord) {
        favoriteDirectoryStore.removeFavorite(record)
        loadCurrentDirectory()
    }

    fun addFavorite(record: FavoriteDirectoryRecord) {
        favoriteDirectoryStore.addFavorite(record, addToTop = settingsStore.bookmarkSettings().addToTop)
        loadCurrentDirectory()
    }

    fun addFavoriteRecord(record: FavoriteDirectoryRecord) {
        favoriteDirectoryStore.addFavorite(record, addToTop = settingsStore.bookmarkSettings().addToTop)
        updateDirectoryShortcutState()
    }

    fun removeFavoriteRecord(record: FavoriteDirectoryRecord) {
        favoriteDirectoryStore.removeFavorite(record)
        updateDirectoryShortcutState()
    }

    fun toggleFavoriteRecord(record: FavoriteDirectoryRecord): Boolean {
        val isFavorite = favoriteDirectoryStore.isFavorite(record)
        if (isFavorite) {
            favoriteDirectoryStore.removeFavorite(record)
        } else {
            favoriteDirectoryStore.addFavorite(record, addToTop = settingsStore.bookmarkSettings().addToTop)
        }
        updateDirectoryShortcutState()
        return !isFavorite
    }

    private fun updateDirectoryShortcutState() {
        val state = _state.value ?: return
        val currentFavorite = favoriteRecordFor(currentLocation)
        _state.value = state.copy(
            isCurrentFavorite = favoriteDirectoryStore.isFavorite(currentFavorite),
            favoriteDirectories = favoriteDirectoryStore.getFavorites(),
            recentDirectories = recentDirectoryStore.getRecents(),
            authorizedDirectories = authorizedDirectories(),
            isRecycleBinEnabled = recycleBinCoordinator.isEnabled()
        )
    }

    fun renameEntry(entry: FileManagerEntry, newName: String): Result<Unit> {
        val result = renameEntryWithoutRefresh(entry, newName)
        result.onSuccess { refresh() }
        return result
    }

    fun renameEntryWithoutRefresh(entry: FileManagerEntry, newName: String): Result<Unit> {
        val normalizedName = newName.trim()
        if (normalizedName.isBlank()) {
            return Result.failure(IllegalArgumentException(getApplication<Application>().getString(R.string.local_file_manager_rename_name_empty)))
        }
        if (normalizedName.contains('/') || normalizedName.contains('\\')) {
            return Result.failure(IllegalArgumentException(getApplication<Application>().getString(R.string.local_file_manager_rename_name_invalid)))
        }
        if (normalizedName == entry.name) {
            return Result.success(Unit)
        }
        val result = runCatching {
            if (!entry.capabilities.canRename) {
                throw IOException(getApplication<Application>().getString(R.string.local_file_manager_rename_no_permission))
            }
            when (val source = entry.source) {
                is FileEntrySource.LocalFile -> {
                    val original = source.file
                    if (!original.exists()) throw IOException(getApplication<Application>().getString(R.string.local_file_manager_rename_missing))
                    val target = File(original.parentFile, normalizedName)
                    if (target.exists()) throw IOException(getApplication<Application>().getString(R.string.local_file_manager_rename_exists))
                    if (!original.renameTo(target)) throw IOException(getApplication<Application>().getString(R.string.local_file_manager_rename_failed))
                }
                is FileEntrySource.DocumentUri -> {
                    val document = safProvider.documentFromUri(source.uri)
                        ?: throw IOException(getApplication<Application>().getString(R.string.local_file_manager_rename_missing))
                    val parent = document.parentFile
                    if (parent?.findFile(normalizedName) != null) throw IOException(getApplication<Application>().getString(R.string.local_file_manager_rename_exists))
                    if (!document.renameTo(normalizedName)) throw IOException(getApplication<Application>().getString(R.string.local_file_manager_rename_failed))
                }
                is FileEntrySource.ParentDirectory,
                is FileEntrySource.ContentUri,
                is FileEntrySource.RootPath,
                is FileEntrySource.ArchiveEntry -> {
                    throw IOException(getApplication<Application>().getString(R.string.local_file_manager_rename_no_permission))
                }
            }
        }
        return result.map { Unit }
    }

    fun renameEntries(renames: List<Pair<FileManagerEntry, String>>): BatchRenameResult {
        val result = renameEntriesWithoutRefresh(renames)
        if (result.successCount > 0) refresh()
        return result
    }

    fun renameEntriesWithoutRefresh(renames: List<Pair<FileManagerEntry, String>>): BatchRenameResult {
        val failures = mutableListOf<BatchRenameFailure>()
        var successCount = 0
        renames.forEach { (entry, newName) ->
            renameEntryWithoutRefresh(entry, newName)
                .onSuccess { successCount++ }
                .onFailure { error ->
                    failures += BatchRenameFailure(
                        entry = entry,
                        message = error.message ?: getApplication<Application>().getString(R.string.local_file_manager_rename_failed)
                    )
                }
        }
        return BatchRenameResult(
            requestedCount = renames.size,
            successCount = successCount,
            failures = failures
        )
    }

    fun deleteEntry(entry: FileManagerEntry, moveToRecycleBin: Boolean = recycleBinCoordinator.defaultMoveToRecycleBin()): Result<Unit> {
        val result = recycleBinCoordinator.deleteEntryBlocking(entry, moveToRecycleBin)
        result.onSuccess { refresh() }
        return result
    }

    fun deleteEntryWithoutRefresh(entry: FileManagerEntry, moveToRecycleBin: Boolean = false): Result<Unit> {
        return recycleBinCoordinator.deleteEntryBlocking(entry, moveToRecycleBin)
    }

    suspend fun deleteEntries(
        entries: List<FileManagerEntry>,
        moveToRecycleBin: Boolean = recycleBinCoordinator.defaultMoveToRecycleBin(),
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchDeleteResult {
        val result = recycleBinCoordinator.deleteEntries(entries, moveToRecycleBin, onProgress)
        refresh()
        return result
    }

    fun toggleRecycleBinEnabled(): Boolean {
        val enabled = recycleBinCoordinator.toggleEnabled()
        _state.value = (_state.value ?: buildState(currentLocation, isLoading = false)).copy(isRecycleBinEnabled = enabled)
        return enabled
    }

    fun recycleBinDirectory(): File = recycleBinCoordinator.directory()

    fun openRecycleBin() {
        recycleBinCoordinator.cleanIfNeeded()
        openLocalDirectory(recycleBinDirectory())
    }

    fun cleanRecycleBinIfNeeded(): Int = recycleBinCoordinator.cleanIfNeeded()

    fun clearRecycleBin(): Result<Unit> {
        val result = recycleBinCoordinator.clear()
        result.onSuccess { refresh() }
        return result
    }

    fun isRecycleBinPath(path: String): Boolean = recycleBinCoordinator.isRecycleBinPath(path)

    fun restoreRecycleBinEntries(entries: List<FileManagerEntry>): BatchRestoreResult {
        val result = recycleBinCoordinator.restoreEntries(entries)
        refresh()
        return result
    }

    suspend fun exportEntriesAsTextInDirectoryPath(
        entries: List<FileManagerEntry>,
        targetDirectoryPath: String,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchTextExportResult {
        val result = textExportCoordinator.exportEntriesAsTextInDirectoryPath(entries, targetDirectoryPath, onProgress)
        refresh()
        return result
    }

    suspend fun copyEntriesInCurrentDirectory(
        entries: List<FileManagerEntry>,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchCopyResult {
        val result = fileTransferCoordinator.copyEntriesInCurrentDirectory(entries, onProgress)
        refresh()
        return result
    }

    suspend fun copyEntriesToLocalDirectory(
        entries: List<FileManagerEntry>,
        targetDirectoryPath: String,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchCopyResult {
        val result = fileTransferCoordinator.copyEntriesToDirectoryPath(entries, targetDirectoryPath, onProgress)
        refresh()
        return result
    }

    suspend fun copyEntriesToDirectoryPath(
        entries: List<FileManagerEntry>,
        targetDirectoryPath: String,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchCopyResult {
        val result = fileTransferCoordinator.copyEntriesToDirectoryPath(entries, targetDirectoryPath, onProgress, TransferConflictPolicy.Fail)
        refresh()
        return result
    }

    suspend fun copyEntriesToDirectoryPathKeepingBoth(
        entries: List<FileManagerEntry>,
        targetDirectoryPath: String,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchCopyResult {
        val result = fileTransferCoordinator.copyEntriesToDirectoryPath(entries, targetDirectoryPath, onProgress, TransferConflictPolicy.KeepBoth)
        refresh()
        return result
    }

    suspend fun copyEntriesToDirectoryPathReplacingExisting(
        entries: List<FileManagerEntry>,
        targetDirectoryPath: String,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchCopyResult {
        val result = fileTransferCoordinator.copyEntriesToDirectoryPath(entries, targetDirectoryPath, onProgress, TransferConflictPolicy.Replace)
        refresh()
        return result
    }

    suspend fun cacheArchiveEntryForPreview(entry: FileManagerEntry): Result<File> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val source = entry.source as? FileEntrySource.ArchiveEntry
                    ?: throw IOException(getApplication<Application>().getString(R.string.local_file_manager_file_not_readable))
                cacheArchiveEntryForContentAccess(source, entry.name)
            }
        }
    }

    suspend fun cacheRootEntryForPreview(entry: FileManagerEntry): Result<File> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val source = entry.source as? FileEntrySource.RootPath
                    ?: throw IOException(getApplication<Application>().getString(R.string.local_file_manager_file_not_readable))
                if (source.isDirectory) throw IOException(getApplication<Application>().getString(R.string.local_file_manager_file_not_readable))
                val readResult = rootFileSystemProvider.read(FileManagerPath.Root(source.absolutePath))
                val bytes = when (readResult) {
                    is FileReadResult.Success -> readResult.bytes
                    is FileReadResult.Failed -> throw IOException(readResult.message, readResult.throwable)
                }
                val cacheDirectory = File(getApplication<Application>().cacheDir, "root-preview-cache").also { it.mkdirs() }
                cleanupOldContentCacheFiles(cacheDirectory)
                val safeName = entry.name.ifBlank { source.absolutePath.substringAfterLast('/') }
                    .replace(Regex("[^A-Za-z0-9._-]"), "_")
                    .ifBlank { "root-entry" }
                val target = File(cacheDirectory, "${System.currentTimeMillis()}-$safeName")
                target.outputStream().buffered().use { output -> output.write(bytes) }
                target
            }
        }
    }

    private fun cleanupOldContentCacheFiles(cacheDirectory: File) {
        cacheDirectory.listFiles().orEmpty().forEach { cached ->
            if (System.currentTimeMillis() - cached.lastModified() > ArchiveContentCacheMaxAgeMillis) {
                cached.delete()
            }
        }
    }

    private suspend fun cacheArchiveEntryForContentAccess(source: FileEntrySource.ArchiveEntry, displayName: String): File {
        return archiveOperationCoordinator.cacheArchiveEntryForContentAccess(source, displayName)
    }


    private suspend fun resolveLocalTransferTarget(directory: File, desiredName: String, policy: TransferConflictPolicy): File {
        return fileTransferNamingPolicy.resolveLocalTransferTarget(
            directory = directory,
            desiredName = desiredName,
            policy = policy,
            deleteExisting = ::deleteExistingLocalTransferTarget
        )
    }

    private suspend fun resolveSafTransferName(directory: DocumentFile, desiredName: String, policy: TransferConflictPolicy): String {
        return fileTransferNamingPolicy.resolveSafTransferName(
            directory = directory,
            desiredName = desiredName,
            policy = policy,
            deleteExisting = ::deleteExistingSafTransferTarget
        )
    }

    private suspend fun deleteExistingLocalTransferTarget(target: File) {
        currentCoroutineContext().ensureActive()
        if (!target.exists()) return
        if (target.isDirectory) {
            target.deleteRecursivelyCancellable()
        } else if (!target.delete()) {
            throw IOException(getApplication<Application>().getString(R.string.local_file_manager_delete_failed))
        }
    }

    private suspend fun deleteExistingSafTransferTarget(target: DocumentFile) {
        currentCoroutineContext().ensureActive()
        if (target.exists() && !target.delete()) {
            throw IOException(getApplication<Application>().getString(R.string.local_file_manager_delete_failed))
        }
    }

    suspend fun moveEntriesToParentDirectory(
        entries: List<FileManagerEntry>,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchMoveResult {
        val result = fileTransferCoordinator.moveEntriesToParentDirectory(entries, onProgress)
        refresh()
        return result
    }

    suspend fun moveEntriesToLocalDirectory(
        entries: List<FileManagerEntry>,
        targetDirectoryPath: String,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchMoveResult {
        val result = fileTransferCoordinator.moveEntriesToDirectoryPath(entries, targetDirectoryPath, onProgress)
        refresh()
        return result
    }

    suspend fun moveEntriesToDirectoryPath(
        entries: List<FileManagerEntry>,
        targetDirectoryPath: String,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchMoveResult {
        val result = fileTransferCoordinator.moveEntriesToDirectoryPath(entries, targetDirectoryPath, onProgress, TransferConflictPolicy.Fail)
        refresh()
        return result
    }

    suspend fun moveEntriesToDirectoryPathKeepingBoth(
        entries: List<FileManagerEntry>,
        targetDirectoryPath: String,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchMoveResult {
        val result = fileTransferCoordinator.moveEntriesToDirectoryPath(entries, targetDirectoryPath, onProgress, TransferConflictPolicy.KeepBoth)
        refresh()
        return result
    }

    suspend fun moveEntriesToDirectoryPathReplacingExisting(
        entries: List<FileManagerEntry>,
        targetDirectoryPath: String,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchMoveResult {
        val result = fileTransferCoordinator.moveEntriesToDirectoryPath(entries, targetDirectoryPath, onProgress, TransferConflictPolicy.Replace)
        refresh()
        return result
    }

    // Local/SAF copy and move execution is handled by FileTransferCoordinator.

    suspend fun zipEntriesInCurrentDirectory(
        entries: List<FileManagerEntry>,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchZipResult {
        val result = withContext(Dispatchers.IO) {
            when (val location = currentLocation) {
                is CurrentLocation.Local -> zipEntriesToLocalArchive(location.directory, entries, onProgress)
                is CurrentLocation.Saf -> zipEntriesToSafArchive(location.document, entries, onProgress)
                is CurrentLocation.Archive -> BatchZipResult(entries.size, 0, "", entries.map { BatchZipFailure(it, getApplication<Application>().getString(R.string.local_file_manager_batch_zip_no_permission)) })
                is CurrentLocation.Root -> BatchZipResult(entries.size, 0, "", entries.map { BatchZipFailure(it, getApplication<Application>().getString(R.string.local_file_manager_batch_zip_no_permission)) })
            }
        }
        refresh()
        return result
    }

    suspend fun zipEntriesInDirectoryPath(
        directoryPath: String,
        entries: List<FileManagerEntry>,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchZipResult {
        val result = withContext(Dispatchers.IO) {
            val uri = directoryPath.toContentUriOrNull()
            if (uri != null) {
                zipEntriesToSafArchive(
                    safProvider.documentFromUri(uri) ?: safProvider.treeFromUri(uri),
                    entries,
                    onProgress
                )
            } else {
                zipEntriesToLocalArchive(File(directoryPath), entries, onProgress)
            }
        }
        refresh()
        return result
    }

    suspend fun zipEachEntryInDirectoryPath(
        directoryPath: String,
        entries: List<FileManagerEntry>,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchSeparateZipResult {
        val result = withContext(Dispatchers.IO) {
            val uri = directoryPath.toContentUriOrNull()
            if (uri != null) {
                zipEachEntryToSafArchives(
                    safProvider.documentFromUri(uri) ?: safProvider.treeFromUri(uri),
                    entries,
                    onProgress
                )
            } else {
                zipEachEntryToLocalArchives(File(directoryPath), entries, onProgress)
            }
        }
        refresh()
        return result
    }

    private suspend fun zipEachEntryToLocalArchives(
        directory: File,
        entries: List<FileManagerEntry>,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit
    ): BatchSeparateZipResult {
        if (!directory.canWrite()) {
            return failedSeparateZipResult(entries, getApplication<Application>().getString(R.string.local_file_manager_batch_zip_no_permission))
        }
        val failures = mutableListOf<BatchZipFailure>()
        val archiveNames = mutableListOf<String>()
        var completedCount = 0
        entries.forEach { entry ->
            currentCoroutineContext().ensureActive()
            var archive: File? = null
            runCatching {
                val target = nextAvailableLocalArchiveTargetForBaseName(directory, separateZipBaseName(entry))
                archive = target
                ZipOutputStream(target.outputStream().buffered()).use { zip ->
                    addEntryToZip(zip, entry)
                }
                archiveNames += target.name
            }.onFailure { error ->
                archive?.delete()
                failures += BatchZipFailure(
                    entry = entry,
                    message = error.message ?: getApplication<Application>().getString(R.string.local_file_manager_batch_zip_failed)
                )
            }
            completedCount++
            onProgress(completedCount, entries.size)
        }
        return BatchSeparateZipResult(
            requestedCount = entries.size,
            successCount = entries.size - failures.size,
            archiveNames = archiveNames,
            failures = failures
        )
    }

    private suspend fun zipEachEntryToSafArchives(
        directory: DocumentFile?,
        entries: List<FileManagerEntry>,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit
    ): BatchSeparateZipResult {
        val parent = directory ?: return failedSeparateZipResult(
            entries,
            getApplication<Application>().getString(R.string.local_file_manager_authorized_directory_unavailable)
        )
        if (!parent.canWrite()) {
            return failedSeparateZipResult(entries, getApplication<Application>().getString(R.string.local_file_manager_batch_zip_no_permission))
        }
        val resolver = getApplication<Application>().contentResolver
        val failures = mutableListOf<BatchZipFailure>()
        val archiveNames = mutableListOf<String>()
        var completedCount = 0
        entries.forEach { entry ->
            currentCoroutineContext().ensureActive()
            var archive: DocumentFile? = null
            runCatching {
                val archiveName = nextAvailableSafArchiveNameForBaseName(parent, separateZipBaseName(entry))
                val target = parent.createFile("application/zip", archiveName)
                    ?: throw IOException(getApplication<Application>().getString(R.string.local_file_manager_batch_zip_failed))
                archive = target
                val output = resolver.openOutputStream(target.uri, "w")
                    ?: throw IOException(getApplication<Application>().getString(R.string.local_file_manager_batch_zip_failed))
                output.use { rawOutput ->
                    ZipOutputStream(rawOutput.buffered()).use { zip ->
                        addEntryToZip(zip, entry)
                    }
                }
                archiveNames += archiveName
            }.onFailure { error ->
                archive?.delete()
                failures += BatchZipFailure(
                    entry = entry,
                    message = error.message ?: getApplication<Application>().getString(R.string.local_file_manager_batch_zip_failed)
                )
            }
            completedCount++
            onProgress(completedCount, entries.size)
        }
        return BatchSeparateZipResult(
            requestedCount = entries.size,
            successCount = entries.size - failures.size,
            archiveNames = archiveNames,
            failures = failures
        )
    }

    private suspend fun addEntryToZip(zip: ZipOutputStream, entry: FileManagerEntry) {
        when (val source = entry.source) {
            is FileEntrySource.LocalFile -> addLocalFileToZip(zip, source.file, entry.name)
            is FileEntrySource.DocumentUri -> addSafDocumentToZip(
                zip,
                safProvider.documentFromUri(source.uri)
                    ?: throw IOException(getApplication<Application>().getString(R.string.local_file_manager_batch_zip_missing)),
                entry.name
            )
            else -> throw IOException(getApplication<Application>().getString(R.string.local_file_manager_batch_zip_no_permission))
        }
    }

    private fun nextAvailableLocalArchiveTargetForBaseName(parent: File, baseName: String): File {
        var index = 1
        while (true) {
            val candidate = File(parent, separateArchiveNameFor(baseName, index))
            if (!candidate.exists()) return candidate
            index++
        }
    }

    private fun nextAvailableSafArchiveNameForBaseName(parent: DocumentFile, baseName: String): String {
        var index = 1
        while (true) {
            val name = separateArchiveNameFor(baseName, index)
            if (parent.findFile(name) == null) return name
            index++
        }
    }

    private fun separateArchiveNameFor(baseName: String, index: Int): String {
        return if (index == 1) "$baseName.zip" else "$baseName $index.zip"
    }

    private fun separateZipBaseName(entry: FileManagerEntry): String {
        val rawName = entry.name.trim().ifBlank {
            getApplication<Application>().getString(R.string.local_file_preview_unknown_name)
        }
        if (entry.type == FileEntryType.Directory) return rawName
        val dotIndex = rawName.lastIndexOf('.')
        return if (dotIndex > 0) rawName.substring(0, dotIndex) else rawName
    }

    private fun failedSeparateZipResult(entries: List<FileManagerEntry>, message: String): BatchSeparateZipResult {
        return BatchSeparateZipResult(
            requestedCount = entries.size,
            successCount = 0,
            archiveNames = emptyList(),
            failures = entries.map { entry -> BatchZipFailure(entry = entry, message = message) }
        )
    }

    private suspend fun zipEntriesToLocalArchive(
        directory: File,
        entries: List<FileManagerEntry>,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit
    ): BatchZipResult {
        if (!directory.canWrite()) {
            return failedZipResult(entries, getApplication<Application>().getString(R.string.local_file_manager_batch_zip_no_permission))
        }
        val archive = nextAvailableLocalArchiveTarget(directory)
        val failures = mutableListOf<BatchZipFailure>()
        var completedCount = 0
        ZipOutputStream(archive.outputStream().buffered()).use { zip ->
            entries.forEach { entry ->
                currentCoroutineContext().ensureActive()
                runCatching {
                    when (val source = entry.source) {
                        is FileEntrySource.LocalFile -> addLocalFileToZip(zip, source.file, entry.name)
                        is FileEntrySource.DocumentUri -> addSafDocumentToZip(
                            zip,
                            safProvider.documentFromUri(source.uri)
                                ?: throw IOException(getApplication<Application>().getString(R.string.local_file_manager_batch_zip_missing)),
                            entry.name
                        )
                        is FileEntrySource.ParentDirectory,
                        is FileEntrySource.ArchiveEntry,
                        is FileEntrySource.RootPath,
                        is FileEntrySource.ContentUri -> throw IOException(getApplication<Application>().getString(R.string.local_file_manager_batch_zip_no_permission))
                        else -> throw IOException(getApplication<Application>().getString(R.string.local_file_manager_batch_zip_no_permission))
                    }
                }.onFailure { error ->
                    failures += BatchZipFailure(
                        entry = entry,
                        message = error.message ?: getApplication<Application>().getString(R.string.local_file_manager_batch_zip_failed)
                    )
                }
                completedCount++
                onProgress(completedCount, entries.size)
            }
        }
        return BatchZipResult(
            requestedCount = entries.size,
            successCount = entries.size - failures.size,
            archiveName = archive.name,
            failures = failures
        )
    }

    private suspend fun zipEntriesToSafArchive(
        directory: DocumentFile?,
        entries: List<FileManagerEntry>,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit
    ): BatchZipResult {
        val parent = directory ?: return failedZipResult(
            entries,
            getApplication<Application>().getString(R.string.local_file_manager_authorized_directory_unavailable)
        )
        if (!parent.canWrite()) {
            return failedZipResult(entries, getApplication<Application>().getString(R.string.local_file_manager_batch_zip_no_permission))
        }
        val archiveName = nextAvailableSafArchiveName(parent)
        val archive = parent.createFile("application/zip", archiveName)
            ?: return failedZipResult(entries, getApplication<Application>().getString(R.string.local_file_manager_batch_zip_failed))
        val failures = mutableListOf<BatchZipFailure>()
        var completedCount = 0
        val resolver = getApplication<Application>().contentResolver
        resolver.openOutputStream(archive.uri, "w")?.use { output ->
            ZipOutputStream(output.buffered()).use { zip ->
                entries.forEach { entry ->
                    currentCoroutineContext().ensureActive()
                    runCatching {
                        when (val source = entry.source) {
                            is FileEntrySource.LocalFile -> addLocalFileToZip(zip, source.file, entry.name)
                            is FileEntrySource.DocumentUri -> addSafDocumentToZip(
                                zip,
                                safProvider.documentFromUri(source.uri)
                                    ?: throw IOException(getApplication<Application>().getString(R.string.local_file_manager_batch_zip_missing)),
                                entry.name
                            )
                            is FileEntrySource.ParentDirectory,
                            is FileEntrySource.ArchiveEntry,
                            is FileEntrySource.RootPath,
                            is FileEntrySource.ContentUri -> throw IOException(getApplication<Application>().getString(R.string.local_file_manager_batch_zip_no_permission))
                        }
                    }.onFailure { error ->
                        failures += BatchZipFailure(
                            entry = entry,
                            message = error.message ?: getApplication<Application>().getString(R.string.local_file_manager_batch_zip_failed)
                        )
                    }
                    completedCount++
                    onProgress(completedCount, entries.size)
                }
            }
        } ?: return failedZipResult(entries, getApplication<Application>().getString(R.string.local_file_manager_batch_zip_failed))
        return BatchZipResult(
            requestedCount = entries.size,
            successCount = entries.size - failures.size,
            archiveName = archiveName,
            failures = failures
        )
    }

    private suspend fun addLocalFileToZip(zip: ZipOutputStream, source: File, entryPath: String) {
        if (!source.exists()) throw IOException(getApplication<Application>().getString(R.string.local_file_manager_batch_zip_missing))
        val normalizedPath = normalizeZipEntryPath(entryPath)
        if (source.isDirectory) {
            addDirectoryEntryToZip(zip, normalizedPath)
            source.listFiles().orEmpty().forEach { child ->
                currentCoroutineContext().ensureActive()
                addLocalFileToZip(zip, child, "$normalizedPath/${child.name}")
            }
            return
        }
        zip.putNextEntry(ZipEntry(normalizedPath))
        source.inputStream().buffered().use { input -> input.copyToCancellable(zip) }
        zip.closeEntry()
    }

    private suspend fun addSafDocumentToZip(zip: ZipOutputStream, source: DocumentFile, entryPath: String) {
        if (!source.exists()) throw IOException(getApplication<Application>().getString(R.string.local_file_manager_batch_zip_missing))
        val normalizedPath = normalizeZipEntryPath(entryPath)
        if (source.isDirectory) {
            addDirectoryEntryToZip(zip, normalizedPath)
            source.listFiles().forEach { child ->
                currentCoroutineContext().ensureActive()
                val childName = child.name.orEmpty().ifBlank { getApplication<Application>().getString(R.string.local_file_preview_unknown_name) }
                addSafDocumentToZip(zip, child, "$normalizedPath/$childName")
            }
            return
        }
        zip.putNextEntry(ZipEntry(normalizedPath))
        getApplication<Application>().contentResolver.openInputStream(source.uri)?.use { input ->
            input.copyToCancellable(zip)
        } ?: throw IOException(getApplication<Application>().getString(R.string.local_file_manager_batch_zip_missing))
        zip.closeEntry()
    }

    private fun addDirectoryEntryToZip(zip: ZipOutputStream, entryPath: String) {
        val directoryPath = entryPath.trimEnd('/') + "/"
        zip.putNextEntry(ZipEntry(directoryPath))
        zip.closeEntry()
    }

    private fun normalizeZipEntryPath(path: String): String {
        return ArchiveExtractionPolicy.normalizeArchiveEntryPath(
            path,
            getApplication<Application>().getString(R.string.local_file_preview_unknown_name)
        )
    }

    private fun nextAvailableLocalArchiveTarget(parent: File): File {
        var index = 1
        while (true) {
            val name = archiveNameFor(index)
            val candidate = File(parent, name)
            if (!candidate.exists()) return candidate
            index++
        }
    }

    private fun nextAvailableSafArchiveName(parent: DocumentFile): String {
        var index = 1
        while (true) {
            val name = archiveNameFor(index)
            if (parent.findFile(name) == null) return name
            index++
        }
    }

    private fun archiveNameFor(index: Int): String {
        val baseName = getApplication<Application>().getString(R.string.local_file_manager_batch_zip_default_name)
        return if (index == 1) "$baseName.zip" else "$baseName $index.zip"
    }

    private fun failedZipResult(entries: List<FileManagerEntry>, message: String): BatchZipResult {
        return BatchZipResult(
            requestedCount = entries.size,
            successCount = 0,
            archiveName = "",
            failures = entries.map { entry -> BatchZipFailure(entry = entry, message = message) }
        )
    }

    suspend fun unzipEntryToCurrentDirectory(entry: FileManagerEntry): Result<String> {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                if (ArchiveFormatResolver.resolve(entry.name)?.supportsExtraction != true) {
                    val format = ArchiveFormatResolver.resolve(entry.name)
                    val message = if (format == null) {
                        getApplication<Application>().getString(R.string.local_file_manager_unzip_not_zip)
                    } else {
                        getApplication<Application>().getString(R.string.local_file_manager_archive_format_unsupported, format.displayName)
                    }
                    throw IOException(message)
                }
                when (val location = currentLocation) {
                    is CurrentLocation.Local -> unzipEntryToLocalDirectory(location.directory, entry)
                    is CurrentLocation.Saf -> unzipEntryToSafDirectory(location.document, entry)
                    is CurrentLocation.Archive -> throw IOException(getApplication<Application>().getString(R.string.local_file_manager_unzip_no_permission))
                    is CurrentLocation.Root -> throw IOException(getApplication<Application>().getString(R.string.local_file_manager_unzip_no_permission))
                }
            }
        }
        result.onSuccess { refresh() }
        return result
    }

    suspend fun unzipEntriesToDirectoryPath(
        entries: List<FileManagerEntry>,
        targetDirectoryPath: String,
        onProgress: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): BatchUnzipResult {
        val result = withContext(Dispatchers.IO) {
            val failures = mutableListOf<BatchUnzipFailure>()
            val targetNames = mutableListOf<String>()
            var completedCount = 0
            entries.forEach { entry ->
                currentCoroutineContext().ensureActive()
                runCatching { unzipEntryToDirectoryPathInternal(entry, targetDirectoryPath) }
                    .onSuccess { targetNames += it }
                    .onFailure { error ->
                        failures += BatchUnzipFailure(
                            entry = entry,
                            message = error.message ?: getApplication<Application>().getString(R.string.local_file_manager_unzip_failed)
                        )
                    }
                completedCount++
                onProgress(completedCount, entries.size)
            }
            BatchUnzipResult(
                requestedCount = entries.size,
                successCount = entries.size - failures.size,
                targetNames = targetNames,
                failures = failures
            )
        }
        refresh()
        return result
    }

    private suspend fun unzipEntryToDirectoryPathInternal(entry: FileManagerEntry, targetDirectoryPath: String): String {
        ensureArchiveEntryCanExtract(entry)
        val targetUri = targetDirectoryPath.toContentUriOrNull()
        val sourceFile = archiveSourceFileForExtraction(entry)
        return if (targetUri != null) {
            val directory = documentForPathUri(targetUri)
                ?: throw IOException(getApplication<Application>().getString(R.string.local_file_manager_authorized_directory_unavailable))
            unzipSourceFileToSafDirectory(directory, sourceFile, entry.name)
        } else {
            val directory = File(targetDirectoryPath)
            unzipSourceFileToLocalDirectory(directory, sourceFile, entry.name)
        }
    }

    private fun ensureArchiveEntryCanExtract(entry: FileManagerEntry) {
        if (ArchiveFormatResolver.resolve(entry.name)?.supportsExtraction != true) {
            val format = ArchiveFormatResolver.resolve(entry.name)
            val message = if (format == null) {
                getApplication<Application>().getString(R.string.local_file_manager_unzip_not_zip)
            } else {
                getApplication<Application>().getString(R.string.local_file_manager_archive_format_unsupported, format.displayName)
            }
            throw IOException(message)
        }
    }

    private suspend fun archiveSourceFileForExtraction(entry: FileManagerEntry): File {
        return archiveExtractionSourceResolver.archiveSourceFileForExtraction(entry)
    }

    private suspend fun unzipSourceFileToLocalDirectory(directory: File, sourceFile: File, archiveName: String): String {
        return archiveExtractionExecutor.unzipSourceFileToLocalDirectory(directory, sourceFile, archiveName)
    }

    private suspend fun unzipEntryToLocalDirectory(directory: File, entry: FileManagerEntry): String {
        return archiveExtractionExecutor.unzipLocalEntryToLocalDirectory(directory, entry)
    }

    private suspend fun unzipSourceFileToSafDirectory(parent: DocumentFile?, sourceFile: File, archiveName: String): String {
        return archiveExtractionExecutor.unzipSourceFileToSafDirectory(parent, sourceFile, archiveName)
    }

    private suspend fun unzipEntryToSafDirectory(directory: DocumentFile?, entry: FileManagerEntry): String {
        return archiveExtractionExecutor.unzipDocumentEntryToSafDirectory(directory, entry)
    }

    fun createDirectory(name: String): Result<Unit> {
        return createDirectoryInLocation(name, currentLocation)
            .onSuccess { refresh() }
    }

    fun createFile(name: String): Result<Unit> {
        return createFileInLocation(name, currentLocation)
            .onSuccess { refresh() }
    }

    fun createDirectoryInDirectoryPath(path: String, name: String): Result<Unit> {
        return createChildInDirectoryPath(
            path = path,
            name = name,
            emptyNameMessageRes = R.string.local_file_manager_create_directory_name_empty,
            invalidNameMessageRes = R.string.local_file_manager_create_directory_name_invalid,
            existsMessageRes = R.string.local_file_manager_create_directory_exists,
            noPermissionMessageRes = R.string.local_file_manager_create_directory_no_permission,
            failedMessageRes = R.string.local_file_manager_create_directory_failed,
            createLocal = { parent, childName ->
                val target = File(parent, childName)
                if (!target.mkdir()) throw IOException(getApplication<Application>().getString(R.string.local_file_manager_create_directory_failed))
            },
            createSaf = { uri, childName ->
                if (!safProvider.createDirectory(uri, childName)) {
                    throw IOException(getApplication<Application>().getString(R.string.local_file_manager_create_directory_failed))
                }
            }
        )
    }

    fun createFileInDirectoryPath(path: String, name: String): Result<Unit> {
        return createChildInDirectoryPath(
            path = path,
            name = name,
            emptyNameMessageRes = R.string.local_file_manager_create_file_name_empty,
            invalidNameMessageRes = R.string.local_file_manager_create_file_name_invalid,
            existsMessageRes = R.string.local_file_manager_create_file_exists,
            noPermissionMessageRes = R.string.local_file_manager_create_file_no_permission,
            failedMessageRes = R.string.local_file_manager_create_file_failed,
            createLocal = { parent, childName ->
                val target = File(parent, childName)
                if (!target.createNewFile()) throw IOException(getApplication<Application>().getString(R.string.local_file_manager_create_file_failed))
            },
            createSaf = { uri, childName ->
                if (!safProvider.createFile(uri, mimeTypeForName(childName), childName)) {
                    throw IOException(getApplication<Application>().getString(R.string.local_file_manager_create_file_failed))
                }
            }
        )
    }

    private fun createDirectoryInLocation(name: String, location: CurrentLocation): Result<Unit> {
        return createChildInLocation(
            name = name,
            location = location,
            emptyNameMessageRes = R.string.local_file_manager_create_directory_name_empty,
            invalidNameMessageRes = R.string.local_file_manager_create_directory_name_invalid,
            existsMessageRes = R.string.local_file_manager_create_directory_exists,
            noPermissionMessageRes = R.string.local_file_manager_create_directory_no_permission,
            createLocal = { parent, childName ->
                val target = File(parent, childName)
                if (!target.mkdir()) throw IOException(getApplication<Application>().getString(R.string.local_file_manager_create_directory_failed))
            },
            createSaf = { parent, childName ->
                parent.createDirectory(childName)
                    ?: throw IOException(getApplication<Application>().getString(R.string.local_file_manager_create_directory_failed))
            }
        )
    }

    private fun createFileInLocation(name: String, location: CurrentLocation): Result<Unit> {
        return createChildInLocation(
            name = name,
            location = location,
            emptyNameMessageRes = R.string.local_file_manager_create_file_name_empty,
            invalidNameMessageRes = R.string.local_file_manager_create_file_name_invalid,
            existsMessageRes = R.string.local_file_manager_create_file_exists,
            noPermissionMessageRes = R.string.local_file_manager_create_file_no_permission,
            createLocal = { parent, childName ->
                val target = File(parent, childName)
                if (!target.createNewFile()) throw IOException(getApplication<Application>().getString(R.string.local_file_manager_create_file_failed))
            },
            createSaf = { parent, childName ->
                parent.createFile(mimeTypeForName(childName), childName)
                    ?: throw IOException(getApplication<Application>().getString(R.string.local_file_manager_create_file_failed))
            }
        )
    }

    private fun createChildInLocation(
        name: String,
        location: CurrentLocation,
        emptyNameMessageRes: Int,
        invalidNameMessageRes: Int,
        existsMessageRes: Int,
        noPermissionMessageRes: Int,
        createLocal: (File, String) -> Unit,
        createSaf: (DocumentFile, String) -> Unit
    ): Result<Unit> {
        val normalizedName = validateChildName(name, emptyNameMessageRes, invalidNameMessageRes).getOrElse { return Result.failure(it) }
        return runCatching {
            when (location) {
                is CurrentLocation.Local -> {
                    val target = File(location.directory, normalizedName)
                    if (target.exists()) throw IOException(getApplication<Application>().getString(existsMessageRes))
                    createLocal(location.directory, normalizedName)
                }
                is CurrentLocation.Saf -> {
                    val document = location.document ?: throw IOException(getApplication<Application>().getString(R.string.local_file_manager_authorized_directory_unavailable))
                    if (!document.canWrite()) throw IOException(getApplication<Application>().getString(noPermissionMessageRes))
                    if (document.findFile(normalizedName) != null) throw IOException(getApplication<Application>().getString(existsMessageRes))
                    createSaf(document, normalizedName)
                }
                is CurrentLocation.Archive -> throw IOException(getApplication<Application>().getString(noPermissionMessageRes))
                is CurrentLocation.Root -> throw IOException(getApplication<Application>().getString(noPermissionMessageRes))
            }
        }.map { Unit }
    }

    private fun createChildInDirectoryPath(
        path: String,
        name: String,
        emptyNameMessageRes: Int,
        invalidNameMessageRes: Int,
        existsMessageRes: Int,
        noPermissionMessageRes: Int,
        failedMessageRes: Int,
        createLocal: (File, String) -> Unit,
        createSaf: (Uri, String) -> Unit
    ): Result<Unit> {
        val normalizedName = validateChildName(name, emptyNameMessageRes, invalidNameMessageRes).getOrElse { return Result.failure(it) }
        return runCatching {
            val uri = path.toContentUriOrNull()
            if (uri != null) {
                if (!safProvider.canWriteDirectory(uri)) throw IOException(getApplication<Application>().getString(noPermissionMessageRes))
                if (safProvider.findChild(uri, normalizedName)) throw IOException(getApplication<Application>().getString(existsMessageRes))
                createSaf(uri, normalizedName)
            } else {
                val directory = File(path)
                if (!directory.exists() || !directory.isDirectory) throw IOException(getApplication<Application>().getString(failedMessageRes))
                if (!directory.canWrite()) throw IOException(getApplication<Application>().getString(noPermissionMessageRes))
                val target = File(directory, normalizedName)
                if (target.exists()) throw IOException(getApplication<Application>().getString(existsMessageRes))
                createLocal(directory, normalizedName)
            }
        }.map { Unit }
    }

    private fun validateChildName(
        name: String,
        emptyNameMessageRes: Int,
        invalidNameMessageRes: Int
    ): Result<String> {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            return Result.failure(IllegalArgumentException(getApplication<Application>().getString(emptyNameMessageRes)))
        }
        if (normalizedName.contains('/') || normalizedName.contains('\\')) {
            return Result.failure(IllegalArgumentException(getApplication<Application>().getString(invalidNameMessageRes)))
        }
        return Result.success(normalizedName)
    }

    suspend fun collectCurrentDirectoryUploadableEntriesRecursively(): Result<List<FileManagerEntry>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                when (val location = currentLocation) {
                    is CurrentLocation.Local -> collectLocalUploadableEntriesRecursively(location.directory)
                    is CurrentLocation.Saf -> collectSafUploadableEntriesRecursively(location.document)
                    is CurrentLocation.Archive -> collectArchiveUploadableEntriesRecursively(location.archiveFile, location.innerPath)
                    is CurrentLocation.Root -> emptyList()
                }
            }
        }
    }

    suspend fun searchCurrentDirectoryRecursively(query: String): Result<List<FileManagerEntry>> {
        return searchCurrentDirectoryRecursively(FileManagerSearchOptions(query = query))
    }

    suspend fun searchCurrentDirectoryRecursively(options: FileManagerSearchOptions): Result<List<FileManagerEntry>> {
        if (options.normalizedQuery.isBlank()) {
            return Result.success(emptyList())
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                when (val location = currentLocation) {
                    is CurrentLocation.Local -> searchLocalDirectoryRecursively(location.directory, options)
                    is CurrentLocation.Saf -> searchSafDirectoryRecursively(location.document, options)
                    is CurrentLocation.Archive -> searchArchiveDirectoryRecursively(location.archiveFile, location.innerPath, options)
                    is CurrentLocation.Root -> searchLocalDirectoryRecursively(File(location.path), options)
                }
            }
        }
    }

    private suspend fun collectLocalUploadableEntriesRecursively(root: File): List<FileManagerEntry> {
        val results = mutableListOf<FileManagerEntry>()
        suspend fun visit(file: File) {
            currentCoroutineContext().ensureActive()
            if (results.size >= RecursiveSearchMaxResults) return
            if (!file.exists() || file.isHidden) return
            if (file != root) {
                val entry = localProvider.buildEntry(file)
                if (entry.capabilities.canUpload) {
                    results += entry
                    if (results.size >= RecursiveSearchMaxResults) return
                }
            }
            if (file.isDirectory) {
                for (child in file.listFiles().orEmpty()
                    .sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })) {
                    if (results.size >= RecursiveSearchMaxResults) return
                    visit(child)
                }
            }
        }
        visit(root)
        return results
    }

    private suspend fun collectSafUploadableEntriesRecursively(root: DocumentFile?): List<FileManagerEntry> {
        val document = root ?: throw IOException(getApplication<Application>().getString(R.string.local_file_manager_authorized_directory_unavailable))
        val results = mutableListOf<FileManagerEntry>()
        suspend fun visit(file: DocumentFile) {
            currentCoroutineContext().ensureActive()
            if (results.size >= RecursiveSearchMaxResults) return
            if (!file.exists()) return
            if (file.uri != document.uri) {
                val entry = safProvider.buildEntry(file)
                if (entry.capabilities.canUpload) {
                    results += entry
                    if (results.size >= RecursiveSearchMaxResults) return
                }
            }
            if (file.isDirectory) {
                for (child in file.listFiles()
                    .sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() })) {
                    if (results.size >= RecursiveSearchMaxResults) return
                    visit(child)
                }
            }
        }
        visit(document)
        return results
    }

    private suspend fun collectArchiveUploadableEntriesRecursively(archiveFile: File, innerPath: String): List<FileManagerEntry> {
        val results = mutableListOf<FileManagerEntry>()
        collectArchiveEntriesRecursively(archiveFile, innerPath) { entry ->
            currentCoroutineContext().ensureActive()
            if (results.size >= RecursiveSearchMaxResults) return@collectArchiveEntriesRecursively false
            if (entry.capabilities.canUpload) {
                results += entry
            }
            results.size < RecursiveSearchMaxResults
        }
        return results
    }

    private suspend fun searchLocalDirectoryRecursively(root: File, options: FileManagerSearchOptions): List<FileManagerEntry> {
        val results = mutableListOf<FileManagerEntry>()
        suspend fun visit(file: File) {
            currentCoroutineContext().ensureActive()
            if (results.size >= RecursiveSearchMaxResults) return
            if (!file.exists()) return
            if (!options.includeHiddenFiles && file.isHidden) return
            if (file != root) {
                val entry = localProvider.buildEntry(file)
                if (options.matches(entry)) {
                    results += entry
                    if (results.size >= RecursiveSearchMaxResults) return
                }
            }
            if (file.isDirectory) {
                for (child in file.listFiles().orEmpty()
                    .sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })) {
                    if (results.size >= RecursiveSearchMaxResults) return
                    visit(child)
                }
            }
        }
        visit(root)
        return results
    }

    private suspend fun searchSafDirectoryRecursively(root: DocumentFile?, options: FileManagerSearchOptions): List<FileManagerEntry> {
        val document = root ?: throw IOException(getApplication<Application>().getString(R.string.local_file_manager_authorized_directory_unavailable))
        val results = mutableListOf<FileManagerEntry>()
        suspend fun visit(file: DocumentFile) {
            currentCoroutineContext().ensureActive()
            if (results.size >= RecursiveSearchMaxResults) return
            if (!file.exists()) return
            if (file.uri != document.uri) {
                val entry = safProvider.buildEntry(file)
                if (options.matches(entry)) {
                    results += entry
                    if (results.size >= RecursiveSearchMaxResults) return
                }
            }
            if (file.isDirectory) {
                for (child in file.listFiles()
                    .sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() })) {
                    if (results.size >= RecursiveSearchMaxResults) return
                    visit(child)
                }
            }
        }
        visit(document)
        return results
    }

    private suspend fun searchArchiveDirectoryRecursively(archiveFile: File, innerPath: String, options: FileManagerSearchOptions): List<FileManagerEntry> {
        val results = mutableListOf<FileManagerEntry>()
        collectArchiveEntriesRecursively(archiveFile, innerPath) { entry ->
            currentCoroutineContext().ensureActive()
            if (results.size >= RecursiveSearchMaxResults) return@collectArchiveEntriesRecursively false
            if (options.matches(entry)) {
                results += entry
            }
            results.size < RecursiveSearchMaxResults
        }
        return FileManagerEntrySorter.sort(results)
    }

    private suspend fun collectArchiveEntriesRecursively(
        archiveFile: File,
        innerPath: String,
        visitor: suspend (FileManagerEntry) -> Boolean
    ) {
        val normalizedRoot = normalizeArchiveInnerPath(innerPath)
        ZipFile(archiveFile).use { zipFile ->
            val directories = linkedMapOf<String, ArchiveChild>()
            val files = mutableListOf<ArchiveChild>()
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                currentCoroutineContext().ensureActive()
                val zipEntry = entries.nextElement()
                val normalizedName = normalizeArchiveEntryName(zipEntry.name)
                if (normalizedName.isBlank()) continue
                if (relativeArchivePath(normalizedName, normalizedRoot) == null) continue
                if (normalizedName == normalizedRoot) continue
                val parentPaths = archiveAncestorInnerPaths(normalizedName, normalizedRoot)
                parentPaths.forEach { directoryPath ->
                    val directoryName = directoryPath.substringAfterLast('/')
                    directories[directoryPath] = ArchiveChild(
                        name = directoryName,
                        innerPath = directoryPath,
                        isDirectory = true,
                        sizeBytes = null,
                        modifiedAtMillis = directories[directoryPath]?.modifiedAtMillis
                    )
                }
                if (!zipEntry.isDirectory) {
                    files += ArchiveChild(
                        name = normalizedName.substringAfterLast('/'),
                        innerPath = normalizedName,
                        isDirectory = false,
                        sizeBytes = zipEntry.size.takeIf { it >= 0L },
                        modifiedAtMillis = zipEntry.time.takeIf { it > 0L }
                    )
                }
            }
            val allEntries = FileManagerEntrySorter.sort(
                directories.values.map { buildArchiveEntry(archiveFile, it) } +
                    files.map { buildArchiveEntry(archiveFile, it) }
            )
            for (entry in allEntries) {
                currentCoroutineContext().ensureActive()
                if (!visitor(entry)) break
            }
        }
    }

    private fun archiveAncestorInnerPaths(entryPath: String, rootInnerPath: String): List<String> {
        val normalizedEntryPath = normalizeArchiveInnerPath(entryPath)
        val normalizedRoot = normalizeArchiveInnerPath(rootInnerPath)
        val parentPath = normalizedEntryPath.substringBeforeLast('/', missingDelimiterValue = "")
        if (parentPath.isBlank()) return emptyList()
        val parentSegments = parentPath.split('/').filter { it.isNotBlank() }
        val rootSegments = normalizedRoot.split('/').filter { it.isNotBlank() }
        if (rootSegments.isNotEmpty() && parentSegments.take(rootSegments.size) != rootSegments) return emptyList()
        val results = mutableListOf<String>()
        for (index in rootSegments.size until parentSegments.size) {
            results += parentSegments.take(index + 1).joinToString("/")
        }
        return results
    }

    private fun mimeTypeForName(name: String): String {
        return FileMimeTypePolicy.mimeTypeForName(name)
    }

    fun openParent() {
        val current = currentLocation
        when (current) {
            is CurrentLocation.Local -> localProvider.parentOf(current.directory)?.let { parent ->
                navigateTo(CurrentLocation.Local(parent, getApplication<Application>().getString(R.string.local_file_manager_location_local_path)))
            }
            is CurrentLocation.Saf -> current.document?.parentFile?.takeIf { it.exists() && it.isDirectory }?.let { parent ->
                navigateTo(CurrentLocation.Saf(parent, parent.uri, current.hint))
            }
            is CurrentLocation.Archive -> {
                val parentInnerPath = archiveParentInnerPath(current.innerPath)
                if (parentInnerPath != null) {
                    navigateTo(current.copy(innerPath = parentInnerPath))
                } else {
                    navigateTo(CurrentLocation.Local(current.archiveFile.parentFile ?: current.archiveFile, getApplication<Application>().getString(R.string.local_file_manager_location_local_path)))
                }
            }
            is CurrentLocation.Root -> {
                File(current.path).parentFile?.absolutePath?.let { parent ->
                    navigateTo(current.copy(path = parent))
                }
            }
        }
    }

    fun navigateBack(): Boolean {
        if (backStack.isEmpty()) return false
        forwardStack.addLast(currentLocation)
        currentLocation = backStack.removeLast()
        loadCurrentDirectory()
        return true
    }

    /**
     * 清空主导航历史（启动路径是历史根，MT 管理器模型下启动后按返回应直接退出，
     * 而不是先退回到打开管理器之前的初始位置）。
     */
    fun clearNavigationHistory() {
        backStack.clear()
        forwardStack.clear()
        _state.value = _state.value?.copy(canGoBack = false, canGoForward = false)
    }

    fun navigateForward(): Boolean {
        if (forwardStack.isEmpty()) return false
        backStack.addLast(currentLocation)
        currentLocation = forwardStack.removeLast()
        loadCurrentDirectory()
        return true
    }

    fun addAuthorizedDirectory(uri: Uri, label: String) {
        safDirectoryStore.addDirectory(uri, label)
        refreshAuthorizedDirectories()
        val document = safProvider.treeFromUri(uri) ?: safProvider.documentFromUri(uri)
        replaceLocation(CurrentLocation.Saf(document = document, uri = uri, hint = label.ifBlank { uri.toString() }))
    }

    fun appFilesLocation(): FileLocation = FileLocation.AppFiles

    fun appCacheLocation(): FileLocation = FileLocation.AppCache

    fun downloadsLocation(): FileLocation = FileLocation.Downloads

    fun currentSafDocument(): DocumentFile? = (currentLocation as? CurrentLocation.Saf)?.document

    private fun openLocalDirectory(directory: File, hint: String) {
        navigateTo(CurrentLocation.Local(directory = directory, hint = hint))
    }

    private fun replaceLocation(location: CurrentLocation) {
        backStack.clear()
        forwardStack.clear()
        currentLocation = location
        loadCurrentDirectory()
    }

    private fun replaceLocationPreloaded(location: CurrentLocation) {
        backStack.clear()
        forwardStack.clear()
        currentLocation = location
        loadCurrentDirectoryPreloaded()
    }

    private fun navigateTo(location: CurrentLocation) {
        if (location == currentLocation) return
        backStack.addLast(currentLocation)
        forwardStack.clear()
        currentLocation = location
        loadCurrentDirectory()
    }

    private fun loadCurrentDirectory(
        refreshDirectoryShortcuts: Boolean = false,
        preferCachedListing: Boolean = true
    ) {
        val location = currentLocation
        val loadGeneration = ++directoryLoadGeneration
        if (refreshDirectoryShortcuts) refreshAuthorizedDirectories()
        val favoriteRecord = favoriteRecordFor(location)
        val cachedEntries = if (preferCachedListing) cachedEntriesFor(location) else null
        val previousState = _state.value
        val keepPreviousEntriesWhileReloading = previousState?.currentPath == location.displayPath && cachedEntries == null
        _state.value = previousState?.copy(
            isLoading = cachedEntries == null,
            errorMessage = null,
            currentPath = location.displayPath,
            locationHint = location.hint,
            entries = cachedEntries ?: if (keepPreviousEntriesWhileReloading) previousState.entries else emptyList(),
            canGoUp = canGoUp(location),
            canGoBack = canGoBack(),
            canGoForward = canGoForward(),
            isCurrentFavorite = favoriteDirectoryStore.isFavorite(favoriteRecord),
            favoriteDirectories = favoriteDirectoryStore.getFavorites(),
            recentDirectories = recentDirectoryStore.getRecents(),
            authorizedDirectories = authorizedDirectories(),
            rootAccessState = _state.value?.rootAccessState ?: rootAccessManager.currentState()
        ) ?: buildState(location, isLoading = cachedEntries == null, entries = cachedEntries.orEmpty())
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                listCurrentLocation(location)
            }
            if (loadGeneration != directoryLoadGeneration) return@launch
            _state.value = result.fold(
                onSuccess = { entries ->
                    recentDirectoryStore.markOpened(favoriteRecord)
                    cacheEntriesFor(location, entries)
                    buildState(
                        location = location,
                        isLoading = false,
                        entries = withParentDirectoryEntry(location.displayPath, entries)
                    )
                },
                onFailure = { throwable ->
                    if (cachedEntries != null) {
                        buildState(
                            location = location,
                            isLoading = false,
                            entries = cachedEntries,
                            errorMessage = throwable.message
                        )
                    } else {
                        buildState(
                            location = location,
                            isLoading = false,
                            entries = emptyList(),
                            errorMessage = throwable.message ?: getApplication<Application>().getString(R.string.local_file_manager_read_directory_failed)
                        )
                    }
                }
            )
        }
    }

    private fun loadCurrentDirectoryPreloaded() {
        loadCurrentDirectory(refreshDirectoryShortcuts = false, preferCachedListing = true)
    }

    private fun cachedEntriesFor(location: CurrentLocation): List<FileManagerEntry>? {
        val directory = (location as? CurrentLocation.Local)?.directory ?: return null
        return LocalFileDirectoryListingCache.get(directory.absolutePath)
            ?.let { entries -> withParentDirectoryEntry(location.displayPath, entries) }
    }

    private fun cacheEntriesFor(location: CurrentLocation, entries: List<FileManagerEntry>) {
        val directory = (location as? CurrentLocation.Local)?.directory ?: return
        LocalFileDirectoryListingCache.put(directory.absolutePath, entries)
    }

    private fun listCurrentLocation(location: CurrentLocation): Result<List<FileManagerEntry>> {
        return when (location) {
            is CurrentLocation.Archive -> listArchiveDirectory(location.archiveFile, location.innerPath)
            else -> runCatching {
                kotlinx.coroutines.runBlocking {
                    listDirectoryThroughProvider(location.toFileManagerPath()).getOrThrow()
                }
            }
        }
    }

    private fun CurrentLocation.toFileManagerPath(): FileManagerPath {
        return when (this) {
            is CurrentLocation.Local -> FileManagerPath.Local(directory.absolutePath)
            is CurrentLocation.Saf -> FileManagerPath.Saf(uri)
            is CurrentLocation.Archive -> FileManagerPath.Archive(archiveFile.absolutePath, innerPath)
            is CurrentLocation.Root -> FileManagerPath.Root(path)
        }
    }

    private suspend fun listDirectoryThroughProvider(path: FileManagerPath): Result<List<FileManagerEntry>> {
        val provider = fileSystemProviderRegistry.providerFor(path)
            ?: return Result.failure(IOException("No file-system provider registered for ${path.displayPath}"))
        return when (val result = provider.list(path)) {
            is FileListResult.Success -> Result.success(result.entries)
            is FileListResult.Failed -> Result.failure(result.throwable ?: IOException(result.message))
        }
    }

    private fun listRootDirectory(path: String): Result<List<FileManagerEntry>> {
        return when (val result = rootFileSystemProvider.listBlocking(FileManagerPath.Root(path))) {
            is FileListResult.Success -> Result.success(result.entries)
            is FileListResult.Failed -> Result.failure(result.throwable ?: IOException(result.message))
        }
    }

    private fun parseRootDisplayPath(path: String): String? {
        return fileManagerNavigator.parseRootDisplayPath(path)
    }

    private fun normalizeRootPath(path: String): String {
        return fileManagerNavigator.normalizeRootPath(path)
    }

    private fun parseArchiveDisplayPath(path: String): FileManagerNavigator.ArchiveDisplayPath? {
        return fileManagerNavigator.parseArchiveDisplayPath(path)
    }

    private fun listArchiveDirectory(archiveFile: File, innerPath: String): Result<List<FileManagerEntry>> {
        return runCatching {
            if (!archiveFile.exists() || !archiveFile.isFile) {
                throw IOException(getApplication<Application>().getString(R.string.local_file_manager_archive_missing))
            }
            if (!isBrowsableArchiveName(archiveFile.name)) {
                throw IOException(getApplication<Application>().getString(R.string.local_file_manager_archive_browse_unsupported))
            }
            val normalizedInnerPath = normalizeArchiveInnerPath(innerPath)
            val immediateChildren = linkedMapOf<String, ArchiveChild>()
            ZipFile(archiveFile).use { zipFile ->
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val zipEntry = entries.nextElement()
                    val normalizedName = normalizeArchiveEntryName(zipEntry.name)
                    if (normalizedName.isBlank()) continue
                    val relativePath = relativeArchivePath(normalizedName, normalizedInnerPath) ?: continue
                    val childName = relativePath.substringBefore('/')
                    if (childName.isBlank()) continue
                    val childInnerPath = if (normalizedInnerPath.isBlank()) childName else "$normalizedInnerPath/$childName"
                    val isDirectory = zipEntry.isDirectory || relativePath.contains('/')
                    val previous = immediateChildren[childName]
                    immediateChildren[childName] = ArchiveChild(
                        name = childName,
                        innerPath = childInnerPath,
                        isDirectory = previous?.isDirectory == true || isDirectory,
                        sizeBytes = if (isDirectory) previous?.sizeBytes else zipEntry.size.takeIf { it >= 0L } ?: previous?.sizeBytes,
                        modifiedAtMillis = zipEntry.time.takeIf { it > 0L } ?: previous?.modifiedAtMillis
                    )
                }
            }
            FileManagerEntrySorter.sort(immediateChildren.values.map { child ->
                buildArchiveEntry(archiveFile, child)
            })
        }
    }

    private fun buildArchiveEntry(archiveFile: File, child: ArchiveChild): FileManagerEntry {
        val type = if (child.isDirectory) {
            FileEntryType.Directory
        } else {
            FileEntryTypeResolver.resolveVerified(name = child.name, isDirectory = false)
        }
        return FileManagerEntry(
            id = "archive:${archiveFile.absolutePath}!/${child.innerPath}",
            name = child.name,
            displayPath = "${archiveFile.absolutePath}!/${child.innerPath}",
            type = type,
            source = FileEntrySource.ArchiveEntry(archiveFile, child.innerPath, child.isDirectory),
            sizeBytes = child.sizeBytes,
            modifiedAtMillis = child.modifiedAtMillis,
            capabilities = FileEntryCapabilities(
                canRead = true,
                canWrite = false,
                canRename = false,
                canDelete = false,
                canCreateChild = false,
                canUpload = false,
                canAccessContent = !child.isDirectory,
                canEditAsText = false
            )
        )
    }

    private fun isBrowsableArchiveName(name: String): Boolean {
        val normalized = name.lowercase()
        return normalized.endsWith(".zip") || normalized.endsWith(".apk") || normalized.endsWith(".jar") || normalized.endsWith(".aar")
    }

    private fun normalizeArchiveInnerPath(path: String): String {
        return fileManagerNavigator.normalizeArchiveInnerPath(path)
    }

    private fun normalizeArchiveEntryName(name: String): String = fileManagerNavigator.normalizeArchiveEntryName(name)

    private fun relativeArchivePath(entryName: String, innerPath: String): String? {
        if (innerPath.isBlank()) return entryName
        if (entryName == innerPath) return null
        val prefix = "$innerPath/"
        return entryName.removePrefix(prefix).takeIf { it != entryName && it.isNotBlank() }
    }

    private fun archiveParentInnerPath(innerPath: String): String? {
        return fileManagerNavigator.archiveParentInnerPath(innerPath)
    }

    private data class ArchiveChild(
        val name: String,
        val innerPath: String,
        val isDirectory: Boolean,
        val sizeBytes: Long?,
        val modifiedAtMillis: Long?
    )

    private fun refreshAuthorizedDirectories() {
        val persistedUris = getApplication<Application>().contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .map { it.uri }
            .toSet()
        safDirectoryStore.removeMissingPersistedPermissions(persistedUris)
    }

    private fun authorizedDirectories(): List<SafDirectoryRecord> = safDirectoryStore.getDirectories()

    private fun withParentDirectoryEntry(path: String, entries: List<FileManagerEntry>): List<FileManagerEntry> {
        if (!shouldShowParentDirectoryEntry(path)) return entries
        val parentPath = parentEntryTargetPath(path) ?: return entries
        if (entries.firstOrNull()?.type == FileEntryType.Parent) return entries
        val parentEntry = FileManagerEntry(
            id = "parent:$parentPath",
            name = "..",
            displayPath = parentPath,
            type = FileEntryType.Parent,
            source = FileEntrySource.ParentDirectory(parentPath),
            sizeBytes = null,
            modifiedAtMillis = null,
            capabilities = FileEntryCapabilities(
                canRead = true,
                canWrite = false,
                canRename = false,
                canDelete = false,
                canCreateChild = false,
                canUpload = false,
                canAccessContent = false,
                canEditAsText = false
            )
        )
        return listOf(parentEntry) + entries
    }

    private fun shouldShowParentDirectoryEntry(path: String): Boolean {
        return fileManagerNavigator.shouldShowParentDirectoryEntry(
            path = path,
            isRootGranted = rootAccessManager.currentState() is RootAccessState.Granted
        )
    }

    private fun parentEntryTargetPath(path: String): String? {
        return fileManagerNavigator.parentEntryTargetPath(
            path = path,
            isRootGranted = rootAccessManager.currentState() is RootAccessState.Granted,
            parentPathForDirectoryPath = ::parentPathForDirectoryPath
        )
    }

    private fun isStorageRootPath(path: String): Boolean {
        return fileManagerNavigator.isStorageRootPath(path)
    }

    private fun isStorageRootDescendant(path: String): Boolean {
        return fileManagerNavigator.isStorageRootDescendant(path)
    }

    private fun normalizeFileManagerPath(path: String): String {
        return fileManagerNavigator.normalizeFileManagerPath(path)
    }

    private fun buildState(
        location: CurrentLocation,
        isLoading: Boolean,
        entries: List<FileManagerEntry> = emptyList(),
        errorMessage: String? = null
    ): LocalFileManagerUiState {
        return LocalFileManagerUiState(
            isLoading = isLoading,
            currentPath = location.displayPath,
            locationHint = location.hint,
            entries = entries,
            errorMessage = errorMessage,
            canGoUp = canGoUp(location),
            canGoBack = canGoBack(),
            canGoForward = canGoForward(),
            isCurrentFavorite = favoriteDirectoryStore.isFavorite(favoriteRecordFor(location)),
            favoriteDirectories = favoriteDirectoryStore.getFavorites(),
            recentDirectories = recentDirectoryStore.getRecents(),
            authorizedDirectories = authorizedDirectories(),
            isRecycleBinEnabled = recycleBinCoordinator.isEnabled(),
            rootAccessState = _state.value?.rootAccessState ?: rootAccessManager.currentState()
        )
    }

    private fun favoriteRecordFor(location: CurrentLocation): FavoriteDirectoryRecord {
        return when (location) {
            is CurrentLocation.Local -> FavoriteDirectoryRecord(
                type = FavoriteDirectoryType.Local,
                value = location.directory.absolutePath,
                label = location.directory.name.ifBlank { location.hint }
            )
            is CurrentLocation.Saf -> FavoriteDirectoryRecord(
                type = FavoriteDirectoryType.Saf,
                value = location.uri.toString(),
                label = location.hint.ifBlank { location.uri.toString() }
            )
            is CurrentLocation.Archive -> FavoriteDirectoryRecord(
                type = FavoriteDirectoryType.Archive,
                value = location.displayPath,
                label = location.archiveFile.name.ifBlank { location.hint }
            )
            is CurrentLocation.Root -> FavoriteDirectoryRecord(
                type = FavoriteDirectoryType.Local,
                value = location.path,
                label = location.hint
            )
        }
    }

    private fun buildLocationHint(location: FileLocation): String {
        val application = getApplication<Application>()
        return when (location) {
            FileLocation.AppFiles -> application.getString(R.string.local_file_manager_location_app_files)
            FileLocation.AppCache -> application.getString(R.string.local_file_manager_location_cache)
            FileLocation.Downloads -> application.getString(R.string.local_file_manager_location_downloads)
            is FileLocation.LocalPath -> application.getString(R.string.local_file_manager_location_local_path)
            is FileLocation.SafTree -> location.label
        }
    }

    private fun canGoUp(location: CurrentLocation): Boolean {
        return when (location) {
            is CurrentLocation.Local -> localProvider.parentOf(location.directory) != null
            is CurrentLocation.Saf -> location.document?.parentFile?.takeIf { it.exists() && it.isDirectory } != null
            is CurrentLocation.Archive -> true
            is CurrentLocation.Root -> location.path != "/"
        }
    }

    private fun canGoBack(): Boolean = backStack.isNotEmpty()

    private fun canGoForward(): Boolean = forwardStack.isNotEmpty()

    private fun String.toContentUriOrNull(): Uri? {
        return fileManagerNavigator.toContentUriOrNull(this)
    }

    private fun documentForPathUri(uri: Uri): DocumentFile? {
        return safProvider.documentFromUri(uri) ?: safProvider.treeFromUri(uri)
    }

    companion object {
        private const val RecursiveSearchMaxResults = 500
        private const val ArchiveContentCacheMaxAgeMillis = 5 * 60 * 1000L
    }

    private sealed interface CurrentLocation {
        val hint: String
        val displayPath: String

        data class Local(
            val directory: File,
            override val hint: String
        ) : CurrentLocation {
            override val displayPath: String = directory.absolutePath
        }

        data class Saf(
            val document: DocumentFile?,
            val uri: Uri,
            override val hint: String
        ) : CurrentLocation {
            override val displayPath: String = document?.uri?.toString() ?: uri.toString()
        }

        data class Archive(
            val archiveFile: File,
            val innerPath: String,
            override val hint: String
        ) : CurrentLocation {
            override val displayPath: String = "${archiveFile.absolutePath}!/${innerPath.trim('/')}"
        }

        data class Root(
            val path: String,
            override val hint: String
        ) : CurrentLocation {
            override val displayPath: String = if (path == "/") "root:///" else "root://$path"
        }
    }
}

data class LocalFileManagerUiState(
    val isLoading: Boolean = false,
    val currentPath: String = "",
    val locationHint: String = "",
    val entries: List<FileManagerEntry> = emptyList(),
    val errorMessage: String? = null,
    val canGoUp: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isCurrentFavorite: Boolean = false,
    val favoriteDirectories: List<FavoriteDirectoryRecord> = emptyList(),
    val recentDirectories: List<RecentDirectoryRecord> = emptyList(),
    val authorizedDirectories: List<SafDirectoryRecord> = emptyList(),
    val isRecycleBinEnabled: Boolean = true,
    val rootAccessState: RootAccessState = RootAccessState.Unknown
)

data class BatchDeleteResult(
    val requestedCount: Int,
    val successCount: Int,
    val failures: List<BatchDeleteFailure>
)

data class BatchDeleteFailure(
    val entry: FileManagerEntry,
    val message: String
)

data class BatchCopyResult(
    val requestedCount: Int,
    val successCount: Int,
    val failures: List<BatchCopyFailure>
)

data class BatchCopyFailure(
    val entry: FileManagerEntry,
    val message: String
)

data class BatchTextExportResult(
    val requestedCount: Int,
    val successCount: Int,
    val failures: List<BatchTextExportFailure>
)

data class BatchTextExportFailure(
    val entry: FileManagerEntry,
    val message: String
)

data class BatchMoveResult(
    val requestedCount: Int,
    val successCount: Int,
    val failures: List<BatchMoveFailure>
)

data class BatchMoveFailure(
    val entry: FileManagerEntry,
    val message: String
)

data class BatchRestoreResult(
    val requestedCount: Int,
    val successCount: Int,
    val failures: List<BatchRestoreFailure>
)

data class BatchRestoreFailure(
    val entry: FileManagerEntry,
    val message: String
)

data class BatchRenameResult(
    val requestedCount: Int,
    val successCount: Int,
    val failures: List<BatchRenameFailure>
)

data class BatchRenameFailure(
    val entry: FileManagerEntry,
    val message: String
)

data class BatchZipResult(
    val requestedCount: Int,
    val successCount: Int,
    val archiveName: String,
    val failures: List<BatchZipFailure>
)

data class BatchSeparateZipResult(
    val requestedCount: Int,
    val successCount: Int,
    val archiveNames: List<String>,
    val failures: List<BatchZipFailure>
)

data class BatchUnzipResult(
    val requestedCount: Int,
    val successCount: Int,
    val targetNames: List<String>,
    val failures: List<BatchUnzipFailure>
)

data class BatchUnzipFailure(
    val entry: FileManagerEntry,
    val message: String
)

data class BatchZipFailure(
    val entry: FileManagerEntry,
    val message: String
)

// File transfer IO helpers live in FileRecursiveIo.kt.
// Path relationship helpers live in FilePathRelationshipPolicy.kt.