package com.Sunset.REN.GitHub.ui.filemanager.controller

import android.view.View
import com.Sunset.REN.GitHub.domain.filemanager.capability.FileManagerActionUiModel

/** Thin controller seams introduced by the modular file-manager plan. */
class FileManagerTopBarController(private val root: View) {
    fun render(path: String, subtitle: String) { root.contentDescription = "$path $subtitle" }
}

class FileManagerBottomBarController(private val root: View) {
    fun render(actions: List<FileManagerActionUiModel>) { root.isEnabled = actions.any { it.visible && it.enabled } }
}

class FileManagerSelectionBarController(private val root: View) {
    fun render(selectedCount: Int) { root.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE }
}

class FileManagerDrawerController(private val root: View) {
    fun setExpanded(expanded: Boolean) { root.visibility = if (expanded) View.VISIBLE else View.GONE }
}

class FileManagerPaneController(private val root: View) {
    fun setFocused(focused: Boolean) { root.isSelected = focused }
}

class FileManagerDualPaneController(private val left: View, private val right: View) {
    fun renderDualPane(enabled: Boolean) { right.visibility = if (enabled) View.VISIBLE else View.GONE; left.isSelected = enabled }
}

class FileManagerDialogCoordinator
