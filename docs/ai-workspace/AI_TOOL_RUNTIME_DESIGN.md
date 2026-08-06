# AI Tool Runtime Design

## 1. Scope

This document defines the planned internal AI tool runtime for SunsetGitHub.

It references Operit's tool/workflow architecture only as an architectural inspiration. The goal is not to copy Operit, embed Operit, depend on Operit signatures, or import Operit's implementation.

## 2. Why A Tool Runtime

AI coding must be constrained by explicit tools rather than arbitrary hidden actions.

The tool runtime gives the project:

- auditable AI actions,
- safety policy enforcement,
- changed-file tracking,
- resumable execution logs,
- future workflow support,
- compatibility path for external model tool-call formats.

## 3. Core Abstractions

Planned conceptual API:

```kotlin
interface AiTool {
    val name: String
    val description: String
    val parametersSchema: String
    val riskLevel: ToolRiskLevel

    suspend fun execute(input: ToolInput, context: ToolExecutionContext): ToolExecutionResult
}
```

```kotlin
data class ToolExecutionResult(
    val success: Boolean,
    val output: String,
    val error: String? = null,
    val changedFiles: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val durationMillis: Long = 0L
)
```

```kotlin
enum class ToolRiskLevel {
    ReadOnly,
    WorkspaceWrite,
    GitWrite,
    CommandExecution,
    Dangerous
}
```

## 4. Tool Registry

A central registry should expose available tools to the AI coding engine.

Responsibilities:

- register internal tools,
- expose tool metadata to prompt builder or native tool-call API,
- deny unknown tools,
- check safety policy before execution,
- record results.

Conceptual shape:

```kotlin
class AiToolRegistry(
    private val tools: Map<String, AiTool>,
    private val policy: ToolPolicy
) {
    fun listTools(): List<AiTool>
    fun getTool(name: String): AiTool?
    suspend fun execute(name: String, input: ToolInput, context: ToolExecutionContext): ToolExecutionResult
}
```

## 5. Initial Tool Set

### Read-only tools

- `list_files`: list workspace directory entries.
- `read_file`: read a text file under policy limits.
- `grep_code`: search project text/code.
- `git_status`: read current Git status.
- `git_diff`: read current diff.
- `read_memory`: read project memory documents.

### Workspace write tools

- `write_file`: write a file under allowed workspace paths.
- `apply_patch`: apply a patch after validation.
- `update_memory`: update memory documents.
- `append_changelog`: append implementation notes.

### Git write tools

- `git_add`: stage selected files.
- `git_commit`: create commit after user or policy approval.

### Verification tools

- `run_verification`: run configured narrow verification task.
- `parse_build_log`: summarize build errors.

### Later command tools

- `execute_command`: run controlled commands behind a `CommandExecutor` abstraction.
- `gradle_assemble_debug`: specialized wrapper around Gradle build.

Command tools must not be part of the first unguarded MVP.

## 6. Tool Policy

Tool execution must be controlled by policy.

Policy inputs:

- current workspace root,
- allowed paths,
- denied paths,
- allowed risk level,
- current task permissions,
- user confirmation requirements,
- file size limits,
- secret file patterns.

Default denied paths/patterns:

```text
local.properties
*.jks
*.keystore
*.pem
*.key
.env
.gradle/
.kotlin/
build/
app/build/
*.apk
*.aab
```

Default denied commands:

```text
git reset --hard
git clean -fd
rm -rf /
rm -rf .
rm -rf <workspace-root>
```

## 7. Tool Call Formats

The runtime should support two model interaction modes.

### 7.1 Native OpenAI-compatible tool call

When provider supports structured tool calls, expose tool schemas directly.

### 7.2 Text fallback protocol

When provider does not support native tools, prompt the model to emit a strict block:

```text
TOOL_CALL
name: read_file
arguments:
{
  "path": "app/src/main/..."
}
END_TOOL_CALL
```

The parser must reject malformed or unknown tool calls.

## 8. Execution Logging

Every tool call should be logged.

Log fields:

- session id,
- task id,
- tool name,
- input summary,
- output summary,
- changed files,
- warnings,
- startedAt,
- finishedAt,
- success/failure.

This supports unattended execution and future handoff.

## 9. Relationship To Project Memory

The memory layer decides what should be done. The tool runtime decides how actions are performed safely.

Required memory documents:

```text
docs/ai-memory/TOOL_POLICY.md
docs/ai-memory/CURRENT_STATE.md
docs/ai-memory/TASK_BOARD.md
docs/ai-memory/CHANGELOG.md
docs/ai-memory/VERIFICATION_LOG.md
```

## 10. Relationship To Workspace

Tools should operate on a declared workspace root. All relative paths are resolved inside that root. Path traversal outside the root must be rejected unless explicitly handled as an import/export operation.

## 11. Relationship To JGit

Git tools should call a dedicated Git service/JGit wrapper, not shell out by default.

Initial Git service capabilities:

- status,
- diff,
- add,
- commit.

Later:

- branch,
- checkout,
- fetch,
- pull,
- push,
- conflict detection.

## 12. Command Execution Backend

Inspired by Operit's terminal/Ubuntu backend, but not required for MVP.

Future abstraction:

```kotlin
interface CommandExecutor {
    suspend fun execute(
        command: String,
        workingDirectory: File,
        timeoutMillis: Long
    ): CommandResult
}
```

Potential implementations:

- Android process backend for very limited commands,
- proot/terminal backend if available,
- no-op backend when command execution is disabled.

## 13. Workflow Direction

A future workflow should be task-oriented:

```text
read memory
choose task
collect context
produce plan
execute allowed tools
apply patch
verify
update memory
stop with handoff
```

Failures should update blockers rather than looping indefinitely.

## 14. Non-Goals

- Do not build a plugin market in the first tool runtime.
- Do not load arbitrary untrusted scripts in MVP.
- Do not execute shell commands without policy.
- Do not use Operit private APIs or signature permissions.
- Do not copy Operit source code without separate license review.

## 15. Implementation Order

1. Define domain models for tools and results.
2. Implement static registry with read-only tools.
3. Add policy checks and logging.
4. Add patch/memory write tools.
5. Add Git read tools.
6. Add AI provider tool-call integration.
7. Add Git write tools.
8. Add command backend only after safety review.