package com.Sunset.REN.GitHub.domain.repo

/** 仓库安全与质量分区的只读聚合摘要。 */
data class RepositorySecuritySummary(
    val probes: List<RepositorySecurityProbe>,
    val alerts: List<RepositorySecurityAlert> = emptyList(),
    val notices: List<String> = emptyList()
) {
    val availableCount: Int
        get() = probes.count {
            it.status == RepositorySecurityProbeStatus.Available || it.status == RepositorySecurityProbeStatus.Empty
        }

    val unavailableCount: Int
        get() = probes.count {
            it.status == RepositorySecurityProbeStatus.Disabled ||
                it.status == RepositorySecurityProbeStatus.Inaccessible ||
                it.status == RepositorySecurityProbeStatus.Error
        }
}

data class RepositorySecurityProbe(
    val key: String,
    val title: String,
    val description: String,
    val status: RepositorySecurityProbeStatus,
    val value: String? = null,
    val detail: String? = null
)

data class RepositorySecurityAlert(
    val number: Int? = null,
    val source: String,
    val title: String,
    val state: String,
    val severity: String? = null,
    val createdAt: String? = null,
    val htmlUrl: String? = null,
    val details: List<String> = emptyList(),
    val detailGroups: List<RepositorySecurityAlertDetailGroup> = emptyList()
)

data class RepositorySecurityAlertDetailGroup(
    val title: String,
    val items: List<String>
)

enum class RepositorySecurityProbeStatus {
    Available,
    Empty,
    Disabled,
    Inaccessible,
    Error
}