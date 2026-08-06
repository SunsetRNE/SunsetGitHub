package com.Sunset.REN.GitHub.data.local

import android.content.Context
import com.Sunset.REN.GitHub.ui.repo.RepositorySection

class RepositoryNavigationPreferencesRepository(context: Context) {

    private val preferenceStore = RepositoryNavigationPreferenceStore(context)

    fun getDefaultShortcutSections(): List<RepositorySection> {
        return preferenceStore.getDefaultShortcutSections().let { sections ->
            if (sections.isEmpty()) RepositorySection.DefaultShortcutSections else sections
        }
    }

    fun setDefaultShortcutSections(sections: List<RepositorySection>) {
        preferenceStore.setDefaultShortcutSections(sections)
    }

    fun getRepositoryShortcutSections(repositoryFullName: String): List<RepositorySection> {
        return preferenceStore.getRepositoryShortcutSections(repositoryFullName).let { sections ->
            if (sections.isEmpty()) getDefaultShortcutSections() else sections
        }
    }

    fun setRepositoryShortcutSections(repositoryFullName: String, sections: List<RepositorySection>) {
        preferenceStore.setRepositoryShortcutSections(repositoryFullName, sections)
    }

    fun getDefaultSectionOrder(): List<RepositorySection> {
        return preferenceStore.getDefaultSectionOrder().let { sections ->
            if (sections.isEmpty()) RepositorySection.ShortcutCandidateSections else sections
        }
    }

    fun setDefaultSectionOrder(sections: List<RepositorySection>) {
        preferenceStore.setDefaultSectionOrder(sections)
    }

    fun getRepositorySectionOrder(repositoryFullName: String): List<RepositorySection> {
        return preferenceStore.getRepositorySectionOrder(repositoryFullName).let { sections ->
            if (sections.isEmpty()) getDefaultSectionOrder() else sections
        }
    }

    fun setRepositorySectionOrder(repositoryFullName: String, sections: List<RepositorySection>) {
        preferenceStore.setRepositorySectionOrder(repositoryFullName, sections)
    }

    /**
     * 把某个分区钉进中间快捷区（追加到末尾）。已存在则不变；超过上限时忽略。
     * 返回更新后的快捷区列表。
     */
    fun pinShortcutSection(repositoryFullName: String, section: RepositorySection): List<RepositorySection> {
        val current = getRepositoryShortcutSections(repositoryFullName)
        if (section in current) return current
        if (current.size >= RepositoryNavigationPreferenceStore.MaxShortcutSectionCount) return current
        val updated = preferenceStore.normalizeShortcutSections(current + section)
        setRepositoryShortcutSections(repositoryFullName, updated)
        return updated
    }

    /**
     * 把某个分区从中间快捷区取消钉选。会保留至少 [RepositoryNavigationPreferenceStore.MinShortcutSectionCount] 个，
     * 已是下限时忽略。返回更新后的快捷区列表。
     */
    fun unpinShortcutSection(repositoryFullName: String, section: RepositorySection): List<RepositorySection> {
        val current = getRepositoryShortcutSections(repositoryFullName)
        if (section !in current) return current
        if (current.size <= RepositoryNavigationPreferenceStore.MinShortcutSectionCount) return current
        val updated = preferenceStore.normalizeShortcutSections(current - section)
        setRepositoryShortcutSections(repositoryFullName, updated)
        return updated
    }

    fun canPinShortcutSection(repositoryFullName: String): Boolean {
        return getRepositoryShortcutSections(repositoryFullName).size < RepositoryNavigationPreferenceStore.MaxShortcutSectionCount
    }

    fun canUnpinShortcutSection(repositoryFullName: String): Boolean {
        return getRepositoryShortcutSections(repositoryFullName).size > RepositoryNavigationPreferenceStore.MinShortcutSectionCount
    }

    fun moveRepositorySection(repositoryFullName: String, section: RepositorySection, delta: Int): List<RepositorySection> {
        val current = getRepositorySectionOrder(repositoryFullName).toMutableList()
        val index = current.indexOf(section)
        if (index < 0) return current
        val targetIndex = (index + delta).coerceIn(0, current.lastIndex)
        if (targetIndex == index) return current
        current.removeAt(index)
        current.add(targetIndex, section)
        val updated = preferenceStore.normalizeSectionOrder(current)
        setRepositorySectionOrder(repositoryFullName, updated)
        return updated
    }
}
