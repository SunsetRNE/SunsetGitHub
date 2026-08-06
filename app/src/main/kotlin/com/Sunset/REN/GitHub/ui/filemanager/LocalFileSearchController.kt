package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import android.widget.Toast
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.TextSearchReplacePolicy
import com.Sunset.REN.GitHub.ui.editor.SearchReplaceCapableEditor
import com.Sunset.REN.GitHub.ui.editor.TextEditorAdapter
import com.Sunset.REN.GitHub.ui.editor.TextSelection
import com.Sunset.REN.GitHub.ui.common.showComposeMessageDialog

class LocalFileSearchController(
    private val searchView: LocalFileSearchView,
    private val editorAdapter: TextEditorAdapter,
    private val contextProvider: () -> Context,
    private val isLoaded: () -> Boolean,
    private val isSpecializedPreview: () -> Boolean,
    private val isEditableMode: () -> Boolean,
    private val canWrite: () -> Boolean,
    private val isSaving: () -> Boolean,
    private val onRenderActionState: (String) -> Unit,
    private val onRestoreSelection: (TextSelection, String) -> Unit
) {
    private var suppressSearchUiRefresh: Boolean = false

    fun configureStatusRefresh() {
        searchView.configureStatusRefresh { refreshStatusFromCurrentQuery() }
    }

    fun setEditorTextSilently(content: String) {
        suppressSearchUiRefresh = true
        try {
            editorAdapter.setText(content)
        } finally {
            suppressSearchUiRefresh = false
        }
        refreshStatusFromCurrentQuery()
    }

    fun refreshStatusFromCurrentQuery() {
        if (suppressSearchUiRefresh || !isLoaded() || isSpecializedPreview()) return
        val query = currentQuery()
        if (query.isBlank()) {
            setSearchStatus("")
            return
        }
        val isRegex = isSearchRegexEnabled()
        if (isRegex && !TextSearchReplacePolicy.isValidRegex(query)) {
            setSearchStatus(context().getString(R.string.local_file_preview_search_invalid_regex))
            return
        }
        setSearchStatus(buildSearchStatus(query, isSearchIgnoreCaseEnabled(), isRegex))
    }

    fun toggleSearchPanel() {
        val shouldShow = !searchView.isSearchPanelVisible
        searchView.isSearchPanelVisible = shouldShow
        if (shouldShow) {
            searchView.focusSearchQuery()
            refreshStatusFromCurrentQuery()
        }
        onRenderActionState(editorAdapter.getText())
    }

    fun findPreviousMatch() {
        findMatch(backward = true)
    }

    fun findNextMatch() {
        findMatch(backward = false)
    }

    fun replaceCurrentMatch() {
        val query = currentQuery()
        if (query.isBlank()) {
            toast(R.string.local_file_preview_search_query_required)
            return
        }
        if (!isEditableMode() || !canWrite()) {
            toast(R.string.local_file_preview_replace_read_only)
            return
        }
        val replacement = currentReplacement()
        val ignoreCase = isSearchIgnoreCaseEnabled()
        val isRegex = isSearchRegexEnabled()
        if (!validateSearchPattern(query, isRegex)) return
        val replaced = if (isRegex) {
            replaceCurrentMatchFallback(query, replacement, ignoreCase, isRegex = true)
        } else {
            searchReplaceEditor()?.replaceCurrent(replacement) ?: replaceCurrentMatchFallback(
                query = query,
                replacement = replacement,
                ignoreCase = ignoreCase,
                isRegex = false
            )
        }
        toast(if (replaced) R.string.local_file_preview_replace_done else R.string.local_file_preview_replace_no_match)
        onRenderActionState(editorAdapter.getText())
    }

    fun replaceAllMatches() {
        val query = currentQuery()
        if (query.isBlank()) {
            toast(R.string.local_file_preview_search_query_required)
            return
        }
        if (!isEditableMode() || !canWrite()) {
            toast(R.string.local_file_preview_replace_read_only)
            return
        }
        val replacement = currentReplacement()
        val isRegex = isSearchRegexEnabled()
        if (!validateSearchPattern(query, isRegex)) return
        val selection = editorAdapter.getSelection()
        val result = TextSearchReplacePolicy.replaceAll(
            content = editorAdapter.getText(),
            query = query,
            replacement = replacement,
            ignoreCase = isSearchIgnoreCaseEnabled(),
            isRegex = isRegex
        )
        if (result.invalidReplacement) {
            toast(R.string.local_file_preview_replace_invalid_regex_replacement)
            return
        }
        if (result.count == 0) {
            setSearchStatus(context().getString(R.string.local_file_preview_search_no_match))
            toast(R.string.local_file_preview_search_no_match)
            return
        }
        setEditorTextSilently(result.content)
        onRestoreSelection(selection, result.content)
        onRenderActionState(result.content)
        val message = context().getString(R.string.local_file_preview_replace_all_done, result.count)
        setSearchStatus(message)
        Toast.makeText(context(), message, Toast.LENGTH_SHORT).show()
    }

    fun showRegexHelp() {
        showComposeMessageDialog(
            context = context(),
            title = context().getString(R.string.local_file_preview_regex_help_title),
            message = context().getString(R.string.local_file_preview_regex_help_message)
        )
    }

    private fun findMatch(backward: Boolean) {
        val query = currentQuery()
        if (query.isBlank()) {
            toast(R.string.local_file_preview_search_query_required)
            return
        }
        val ignoreCase = isSearchIgnoreCaseEnabled()
        val isRegex = isSearchRegexEnabled()
        if (!validateSearchPattern(query, isRegex)) return
        val matched = if (backward) {
            findPreviousMatchFallback(query, ignoreCase, isRegex)
        } else if (isRegex) {
            findNextMatchFallback(query, ignoreCase, isRegex = true)
        } else {
            searchReplaceEditor()?.find(query, ignoreCase) ?: findNextMatchFallback(query, ignoreCase, isRegex = false)
        }
        val message = if (matched) {
            buildSearchStatus(query, ignoreCase, isRegex)
        } else {
            context().getString(R.string.local_file_preview_search_no_match)
        }
        setSearchStatus(message)
        Toast.makeText(context(), message, Toast.LENGTH_SHORT).show()
    }

    private fun validateSearchPattern(query: String, isRegex: Boolean): Boolean {
        if (!isRegex || TextSearchReplacePolicy.isValidRegex(query)) return true
        toast(R.string.local_file_preview_search_invalid_regex)
        return false
    }

    private fun buildSearchStatus(query: String, ignoreCase: Boolean, isRegex: Boolean): String {
        val matches = TextSearchReplacePolicy.findAll(editorAdapter.getText(), query, ignoreCase, isRegex)
        if (matches.isEmpty()) return context().getString(R.string.local_file_preview_search_no_match)
        val selection = editorAdapter.getSelection()
        val selectionStart = minOf(selection.start, selection.end)
        val selectionEnd = maxOf(selection.start, selection.end)
        val selectedIndex = matches.indexOfFirst { match ->
            selectionStart == match.start && selectionEnd == match.end
        }
        return if (selectedIndex >= 0) {
            context().getString(R.string.local_file_preview_search_match_position, selectedIndex + 1, matches.size)
        } else {
            context().getString(R.string.local_file_preview_search_match_count, matches.size)
        }
    }

    private fun findNextMatchFallback(query: String, ignoreCase: Boolean, isRegex: Boolean): Boolean {
        val content = editorAdapter.getText()
        val selectionEnd = editorAdapter.getSelection().end.coerceIn(0, content.length)
        val match = TextSearchReplacePolicy.findNext(content, query, selectionEnd, ignoreCase, isRegex) ?: return false
        editorAdapter.setSelection(match.start, match.end)
        editorAdapter.scrollToSelectionEnd()
        return true
    }

    private fun findPreviousMatchFallback(query: String, ignoreCase: Boolean, isRegex: Boolean): Boolean {
        val content = editorAdapter.getText()
        val matches = TextSearchReplacePolicy.findAll(content, query, ignoreCase, isRegex)
        if (matches.isEmpty()) return false
        val selectionStart = editorAdapter.getSelection().start.coerceIn(0, content.length)
        val match = matches.lastOrNull { it.start < selectionStart } ?: matches.last()
        editorAdapter.setSelection(match.start, match.end)
        editorAdapter.scrollToSelectionEnd()
        return true
    }

    private fun replaceCurrentMatchFallback(query: String, replacement: String, ignoreCase: Boolean, isRegex: Boolean): Boolean {
        val content = editorAdapter.getText()
        val selection = editorAdapter.getSelection()
        val start = selection.start.coerceIn(0, content.length)
        val end = selection.end.coerceIn(start, content.length)
        val selectedText = if (start == end) "" else content.substring(start, end)
        val currentSelectionMatches = if (isRegex) {
            start != end && TextSearchReplacePolicy.countMatches(selectedText, query, ignoreCase, isRegex = true) == 1
        } else {
            start != end && selectedText.equals(query, ignoreCase = ignoreCase)
        }
        if (!currentSelectionMatches) {
            if (!findNextMatchFallback(query, ignoreCase, isRegex)) return false
            return replaceCurrentMatchFallback(query, replacement, ignoreCase, isRegex)
        }
        val replacementResult = TextSearchReplacePolicy.replaceMatchedText(
            matchedText = selectedText,
            query = query,
            replacement = replacement,
            ignoreCase = ignoreCase,
            isRegex = isRegex
        )
        if (replacementResult.invalidReplacement) {
            toast(R.string.local_file_preview_replace_invalid_regex_replacement)
            return false
        }
        val updated = content.replaceRange(start, end, replacementResult.content)
        setEditorTextSilently(updated)
        editorAdapter.setSelection(start, start + replacementResult.content.length)
        editorAdapter.scrollToSelectionEnd()
        return true
    }

    private fun setSearchStatus(message: String) {
        searchView.searchStatus = message
    }

    private fun searchReplaceEditor(): SearchReplaceCapableEditor? {
        return editorAdapter as? SearchReplaceCapableEditor
    }

    private fun isSearchIgnoreCaseEnabled(): Boolean {
        return searchView.isIgnoreCaseEnabled
    }

    private fun isSearchRegexEnabled(): Boolean {
        return searchView.isRegexEnabled
    }

    private fun currentQuery(): String {
        return searchView.query
    }

    private fun currentReplacement(): String {
        return searchView.replacement
    }

    private fun toast(messageResId: Int) {
        Toast.makeText(context(), messageResId, Toast.LENGTH_SHORT).show()
    }

    private fun context(): Context = contextProvider()
}
