package com.Sunset.REN.GitHub.data.local

import android.content.Context
import com.Sunset.REN.GitHub.ui.repo.RepositorySection

class RepositoryNavigationPreferenceStore(context: Context) {

    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun getDefaultShortcutSections(): List<RepositorySection> {
        val rawValue = sharedPreferences.getString(KeyShortcutSections, null).orEmpty()
        return normalizeShortcutSections(parseSections(rawValue))
    }

    fun setDefaultShortcutSections(sections: List<RepositorySection>) {
        setSections(KeyShortcutSections, normalizeShortcutSections(sections))
    }

    fun getRepositoryShortcutSections(repositoryFullName: String): List<RepositorySection> {
        val key = buildRepositoryShortcutSectionsKey(repositoryFullName) ?: return emptyList()
        val rawValue = sharedPreferences.getString(key, null).orEmpty()
        return normalizeShortcutSections(parseSections(rawValue))
    }

    fun setRepositoryShortcutSections(repositoryFullName: String, sections: List<RepositorySection>) {
        val key = buildRepositoryShortcutSectionsKey(repositoryFullName) ?: return
        setSections(key, normalizeShortcutSections(sections))
    }
    fun getDefaultSectionOrder(): List<RepositorySection> {
        val rawValue = sharedPreferences.getString(KeySectionOrder, null).orEmpty()
        if (rawValue.isBlank()) return RepositorySection.ShortcutCandidateSections
        return normalizeSectionOrder(parseSections(rawValue))
    }


    fun setDefaultSectionOrder(sections: List<RepositorySection>) {
        setSections(KeySectionOrder, normalizeSectionOrder(sections))
    }

    fun getRepositorySectionOrder(repositoryFullName: String): List<RepositorySection> {
        val key = buildRepositorySectionOrderKey(repositoryFullName) ?: return emptyList()
        val rawValue = sharedPreferences.getString(key, null).orEmpty()
        if (rawValue.isBlank()) return emptyList()
        return normalizeSectionOrder(parseSections(rawValue))
    }

    fun setRepositorySectionOrder(repositoryFullName: String, sections: List<RepositorySection>) {
        val key = buildRepositorySectionOrderKey(repositoryFullName) ?: return
        setSections(key, normalizeSectionOrder(sections))
    }

    /**
     * 弹性槽位：保留用户钉选的顺序，仅做合法性过滤、去重与上限截断（最多 [MaxShortcutSectionCount] 个）。
     * 不再强制补齐到固定数量，允许中间快捷区为 1~3 格弹性变化。
     */
    fun normalizeShortcutSections(sections: List<RepositorySection>): List<RepositorySection> {
        return sections
            .filter { section -> section in RepositorySection.ShortcutCandidateSections }
            .distinct()
            .take(MaxShortcutSectionCount)
    }

    fun normalizeSectionOrder(sections: List<RepositorySection>): List<RepositorySection> {
        val orderedSections = sections
            .filter { section -> section in RepositorySection.ShortcutCandidateSections }
            .distinct()
        return orderedSections + RepositorySection.ShortcutCandidateSections.filter { section -> section !in orderedSections }
    }

    private fun parseSections(rawValue: String): List<RepositorySection> {
        return rawValue.split(SectionSeparator)
            .mapNotNull { storageKey -> RepositorySection.fromStorageKey(storageKey) }
    }

    private fun setSections(key: String, sections: List<RepositorySection>) {
        sharedPreferences.edit()
            .putString(key, sections.joinToString(SectionSeparator) { it.storageKey })
            .apply()
    }

    private fun buildRepositoryShortcutSectionsKey(repositoryFullName: String): String? {
        return repositoryFullName.normalizedRepositoryKey()?.let { normalizedFullName ->
            "$KeyRepositoryShortcutSectionsPrefix$normalizedFullName"
        }
    }

    private fun buildRepositorySectionOrderKey(repositoryFullName: String): String? {
        return repositoryFullName.normalizedRepositoryKey()?.let { normalizedFullName ->
            "$KeyRepositorySectionOrderPrefix$normalizedFullName"
        }
    }

    private fun String.normalizedRepositoryKey(): String? {
        return trim().lowercase().takeIf { normalizedFullName -> normalizedFullName.contains('/') }
    }

    companion object {
        private const val PreferencesName = "repository_navigation_preferences"
        private const val KeyShortcutSections = "shortcut_sections"
        private const val KeySectionOrder = "section_order"
        private const val KeyRepositoryShortcutSectionsPrefix = "repository_shortcut_sections:"
        private const val KeyRepositorySectionOrderPrefix = "repository_section_order:"
        private const val SectionSeparator = ","

        /** 中间快捷区最多容纳的分区数量（底部栏 5 格减去常驻的「代码」与「更多」）。 */
        const val MaxShortcutSectionCount = 3

        /** 中间快捷区至少保留的分区数量，避免底部栏只剩「代码 / 更多」两格。 */
        const val MinShortcutSectionCount = 1
    }
}
