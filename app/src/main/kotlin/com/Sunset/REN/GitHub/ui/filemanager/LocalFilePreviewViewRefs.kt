package com.Sunset.REN.GitHub.ui.filemanager

import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.IdRes
import com.Sunset.REN.GitHub.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox

class LocalFilePreviewViewRefs(root: View) {
    val root: View = root
    val name: TextView = root.requireView(R.id.text_local_file_preview_name)
    val state: TextView = root.requireView(R.id.text_local_file_preview_state)
    val path: TextView = root.requireView(R.id.text_local_file_preview_path)
    val typeChip: TextView = root.requireView(R.id.text_local_file_preview_type_chip)
    val accessPill: TextView = root.requireView(R.id.text_local_file_preview_access_pill)
    val editorContainer: FrameLayout = root.requireView(R.id.container_local_file_preview_editor)
    val markdownScroll: ScrollView = root.requireView(R.id.scroll_local_file_preview_markdown)
    val markdownText: TextView = root.requireView(R.id.text_local_file_preview_markdown)
    val imageContainer: FrameLayout = root.requireView(R.id.container_local_file_preview_image)
    val image: ImageView = root.requireView(R.id.image_local_file_preview)
    val archiveScroll: ScrollView = root.requireView(R.id.scroll_local_file_preview_archive)
    val archiveText: TextView = root.requireView(R.id.text_local_file_preview_archive)
    val searchContainer: View = root.requireView(R.id.container_local_file_preview_search)
    val searchQuery: EditText = root.requireView(R.id.edit_local_file_preview_search_query)
    val findPrevious: MaterialButton = root.requireView(R.id.button_local_file_preview_find_previous)
    val findNext: MaterialButton = root.requireView(R.id.button_local_file_preview_find_next)
    val regexHelp: MaterialButton = root.requireView(R.id.button_local_file_preview_regex_help)
    val replaceText: EditText = root.requireView(R.id.edit_local_file_preview_replace_text)
    val replaceCurrent: MaterialButton = root.requireView(R.id.button_local_file_preview_replace_current)
    val replaceAll: MaterialButton = root.requireView(R.id.button_local_file_preview_replace_all)
    val ignoreCase: MaterialCheckBox = root.requireView(R.id.check_local_file_preview_ignore_case)
    val regex: MaterialCheckBox = root.requireView(R.id.check_local_file_preview_regex)
    val searchStatus: TextView = root.requireView(R.id.text_local_file_preview_search_status)
    val markdownToggle: MaterialButton = root.requireView(R.id.button_local_file_preview_markdown_toggle)
    val searchToggle: MaterialButton = root.requireView(R.id.button_local_file_preview_search_toggle)
    val convert: MaterialButton = root.requireView(R.id.button_local_file_preview_convert)
    val saveAs: MaterialButton = root.requireView(R.id.button_local_file_preview_save_as)
    val extract: MaterialButton = root.requireView(R.id.button_local_file_preview_extract)
    val edit: MaterialButton = root.requireView(R.id.button_local_file_preview_edit)
    val undo: MaterialButton = root.requireView(R.id.button_local_file_preview_undo)
    val redo: MaterialButton = root.requireView(R.id.button_local_file_preview_redo)
    val cancel: MaterialButton = root.requireView(R.id.button_local_file_preview_cancel)
    val save: MaterialButton = root.requireView(R.id.button_local_file_preview_save)
}

private fun <T : View> View.requireView(@IdRes id: Int): T {
    return findViewById<T>(id) ?: error("Missing required local file preview view: $id")
}
