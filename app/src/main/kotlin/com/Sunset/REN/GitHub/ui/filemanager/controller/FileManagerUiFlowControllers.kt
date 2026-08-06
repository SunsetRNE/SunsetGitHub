package com.Sunset.REN.GitHub.ui.filemanager.controller

import com.Sunset.REN.GitHub.data.filemanager.FavoriteDirectoryRecord
import com.Sunset.REN.GitHub.data.filemanager.RecentDirectoryRecord
import com.Sunset.REN.GitHub.data.filemanager.SafDirectoryRecord
import com.Sunset.REN.GitHub.domain.filemanager.ArchiveSidePaneController
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerDualPaneNavigationState
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerDualPaneState
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerPaneId
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerSearchOptions
import com.Sunset.REN.GitHub.domain.filemanager.OperationSafety
import com.Sunset.REN.GitHub.ui.filemanager.DrawerTab
import com.Sunset.REN.GitHub.ui.filemanager.ManualTransferOperation
import com.Sunset.REN.GitHub.ui.filemanager.TransferTargetOption

object FileManagerDrawerLogic {
    fun expandedHeight(screenHeightPixels: Int, density: Float): Int {
        val screenBased = (screenHeightPixels * 0.28f).toInt()
        val min = (160 * density).toInt()
        val max = (280 * density).toInt()
        return screenBased.coerceIn(min, max)
    }

    fun handleText(height: Int, expandedHeight: Int): String = if (height > expandedHeight / 2) "⌄" else "⌃"

    fun tabIndicatorX(tab: DrawerTab, density: Float): Float = if (tab == DrawerTab.Recent) 0f else 74f * density
}

object FileManagerSelectionActionController {
    fun hasSelection(leftSelectedIds: Set<String>, rightSelectedIds: Set<String>): Boolean =
        leftSelectedIds.isNotEmpty() || rightSelectedIds.isNotEmpty()

    fun selectedEntries(
        leftSelectedIds: Set<String>,
        rightSelectedIds: Set<String>,
        leftEntries: List<FileManagerEntry>,
        rightEntries: List<FileManagerEntry>
    ): List<FileManagerEntry> {
        return if (rightSelectedIds.isNotEmpty()) {
            rightEntries.filter { it.id in rightSelectedIds }
        } else {
            leftEntries.filter { it.id in leftSelectedIds }
        }
    }

    fun conflictedEntriesForTarget(entries: List<FileManagerEntry>, targetPath: String, targetEntries: List<FileManagerEntry>): List<FileManagerEntry> {
        if (!OperationSafety.isWritableDirectoryTarget(targetPath)) return emptyList()
        return ArchiveSidePaneController.conflictedEntriesForTarget(entries = entries, targetEntries = targetEntries)
    }

    fun targetEntriesForPath(
        targetPath: String,
        leftPath: String,
        leftEntries: List<FileManagerEntry>,
        rightPath: String,
        rightEntries: List<FileManagerEntry>
    ): List<FileManagerEntry> {
        return ArchiveSidePaneController.targetEntriesForPath(
            targetPath = targetPath,
            leftPath = leftPath,
            leftEntries = leftEntries,
            rightPath = rightPath,
            rightEntries = rightEntries
        )
    }
}

object FileManagerTransferTargetController {
    fun buildTransferTargetOptions(
        activePath: String,
        includeParent: Boolean,
        parentPathResolver: (String) -> String?,
        dualPaneTargetResolver: () -> String?,
        authorizedDirectories: List<SafDirectoryRecord>,
        dualPaneEnabled: Boolean
    ): List<TransferTargetOption> {
        return buildTransferTargetOptionsFromTargets(
            activePath = activePath,
            includeParent = includeParent,
            parentPathResolver = parentPathResolver,
            dualPaneTargetResolver = dualPaneTargetResolver,
            authorizedTargets = authorizedDirectories.map { record -> record.label to record.uri.toString() },
            dualPaneEnabled = dualPaneEnabled
        )
    }

    fun buildTransferTargetOptionsFromTargets(
        activePath: String,
        includeParent: Boolean,
        parentPathResolver: (String) -> String?,
        dualPaneTargetResolver: () -> String?,
        authorizedTargets: List<Pair<String, String>>,
        dualPaneEnabled: Boolean
    ): List<TransferTargetOption> {
        val options = mutableListOf<TransferTargetOption>()
        if (OperationSafety.isWritableDirectoryTarget(activePath)) {
            options += TransferTargetOption("当前目录：$activePath", activePath)
        }
        if (includeParent) {
            parentPathResolver(activePath)
                ?.takeIf(OperationSafety::isWritableDirectoryTarget)
                ?.let { parentPath -> options += TransferTargetOption("上级目录：$parentPath", parentPath) }
        }
        if (dualPaneEnabled) {
            dualPaneTargetResolver()?.let { targetPath ->
                options += TransferTargetOption("目标窗格：$targetPath", targetPath)
            }
        }
        authorizedTargets.forEach { (label, targetPath) ->
            if (OperationSafety.isWritableDirectoryTarget(targetPath)) {
                options += TransferTargetOption("授权目录：${label.ifBlank { targetPath }}", targetPath)
            }
        }
        return options.distinctBy { it.path }
    }
}
object FileManagerTransferActionController {
    fun canUseManualTarget(targetPath: String): Boolean = OperationSafety.isWritableDirectoryTarget(targetPath)
    fun shouldBlockRootWrite(targetPath: String): Boolean = targetPath.startsWith("root://")
    fun describeOperation(operation: ManualTransferOperation): String = if (operation == ManualTransferOperation.Copy) "复制到…" else "移动到…"
}

object FileManagerPathEditController {
    fun normalizeInlinePath(rawPath: String, storageRootPath: String): String {
        val trimmed = rawPath.trim()
        val withoutTrailingSlash = if (trimmed.length > 1) trimmed.removeSuffix("/") else trimmed
        val normalizedStorageRoot = storageRootPath.removeSuffix("/")
        return when {
            withoutTrailingSlash == "/sdcard" -> normalizedStorageRoot
            withoutTrailingSlash.startsWith("/sdcard/") -> normalizedStorageRoot + withoutTrailingSlash.removePrefix("/sdcard")
            else -> withoutTrailingSlash
        }
    }
}

object FileManagerSearchFlowController {
    fun currentListSearchSource(
        dualPaneState: FileManagerDualPaneState,
        leftEntries: List<FileManagerEntry>,
        rightEntries: List<FileManagerEntry>
    ): List<FileManagerEntry> {
        return if (dualPaneState.isDualPane && dualPaneState.focusedPane == FileManagerPaneId.Right) {
            rightEntries
        } else {
            leftEntries
        }
    }

    fun filterCurrentList(entries: List<FileManagerEntry>, options: FileManagerSearchOptions): List<FileManagerEntry> =
        entries.filter(options::matches)

    fun modeDescription(options: FileManagerSearchOptions): String = buildString {
        append(if (options.includeSubdirectories) "递归" else "当前目录")
        append("  ·  ")
        append(when {
            options.includeFiles && options.includeDirectories -> "文件+文件夹"
            options.includeFiles -> "仅文件"
            else -> "仅文件夹"
        })
        if (options.caseSensitive) append("  ·  区分大小写")
    }
}

