package com.Sunset.REN.GitHub.domain.filemanager

/**
 * App-level manual hidden file rule.
 *
 * MT-style "manual hidden" is independent from Unix/Android dot-file hiding:
 * the app stores exact display paths selected by the user and filters them at
 * presentation time without renaming or modifying the underlying files.
 */
data class ManualHiddenFileRule(
    val path: String,
    val type: ManualHiddenFileRuleType = ManualHiddenFileRuleType.ExactPath
)

enum class ManualHiddenFileRuleType {
    ExactPath
}
