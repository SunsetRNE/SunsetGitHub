package com.Sunset.REN.GitHub.domain.repo

/** 仓库 Actions workflow 条目。 */
data class RepositoryActionWorkflow(
    val id: Long,
    val name: String,
    val path: String,
    val state: String?,
    val htmlUrl: String?,
    val badgeUrl: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val isDispatchable: Boolean,
    val rawTriggers: List<String> = emptyList(),
    val dispatchInputs: List<RepositoryActionWorkflowInput> = emptyList(),
    val hasLoadedDispatchMetadata: Boolean = false
) {
    val displayState: String
        get() = state?.takeIf { it.isNotBlank() } ?: "unknown"
}

data class RepositoryActionWorkflowInput(
    val name: String,
    val description: String? = null,
    val required: Boolean = false,
    val defaultValue: String? = null,
    val type: String = "string",
    val options: List<String> = emptyList()
) {
    val isBoolean: Boolean
        get() = type.equals("boolean", ignoreCase = true)

    val isChoice: Boolean
        get() = type.equals("choice", ignoreCase = true) && options.isNotEmpty()
}
