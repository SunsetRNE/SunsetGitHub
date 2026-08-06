package com.Sunset.REN.GitHub.data.github.html

/** Experimental Copilot / Agents session parsed from GitHub's repository Agents page. */
data class RepositoryAgentSession(
    val id: String,
    val title: String,
    val status: RepositoryAgentSessionStatus = RepositoryAgentSessionStatus.Unknown,
    val type: RepositoryAgentSessionType? = null,
    val agent: RepositoryAgentSessionAgent? = null,
    val summary: String = "",
    val htmlUrl: String? = null,
    val author: String? = null,
    val branch: String? = null,
    val updatedAt: String? = null,
    val target: String? = null,
    val targetUrl: String? = null
)

data class RepositoryAgentsPageSummary(
    val summary: RepositoryHtmlSectionSummary,
    val sessions: List<RepositoryAgentSession> = emptyList(),
    val isExperimentalHtmlParse: Boolean = false
)

enum class RepositoryAgentSessionStatus(
    val displayLabel: String,
    val queryToken: String,
    val isCompleted: Boolean
) {
    InProgress("In progress", "status:in-progress", false),
    Queued("Queued", "status:queued", false),
    Idle("Idle", "status:idle", false),
    NeedsAttention("Needs attention", "status:needs-attention", false),
    Failed("Failed", "status:failed", true),
    Completed("Completed", "status:completed", true),
    Cancelled("Cancelled", "status:cancelled", true),
    TimedOut("Timed out", "status:timed-out", true),
    Unknown("Unknown", "status:unknown", false)
}

enum class RepositoryAgentSessionType(
    val displayLabel: String,
    val queryToken: String
) {
    CloudAgents("Cloud agents", "type:cloud"),
    Cli("CLI", "type:cli"),
    VsCode("VS Code", "type:vscode")
}

enum class RepositoryAgentSessionAgent(
    val displayLabel: String,
    val queryToken: String
) {
    CloudAgents("Cloud agents", "agent:cloud"),
    CopilotCli("Copilot CLI", "agent:copilot-cli"),
    JetBrains("Copilot in JetBrains", "agent:jetbrains"),
    VsCode("Copilot in VS Code", "agent:vscode")
}
