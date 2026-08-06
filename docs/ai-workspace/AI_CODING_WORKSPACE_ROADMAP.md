# AI Coding Workspace Roadmap

## 1. Purpose

This document is the long-term planning memory for SunsetGitHub's local AI coding workspace.

The project will reference two external systems only at the architectural level:

- `灵_AI记忆体`: reference for structured long-term AI memory and conversation-memory binding.
- `AAswordman/Operit`: reference for local tool runtime, workspace automation, workflow, and tool-call architecture.

No code, assets, signatures, private APIs, or license-sensitive implementation details from either project should be copied into SunsetGitHub unless a separate license review and explicit implementation decision is made.

## 2. Product Direction

SunsetGitHub remains a Kotlin Android GitHub client, but its future local coding direction is:

```text
GitHub client
+ local workspace
+ local project memory
+ local Git/JGit operations
+ AI coding engine
+ Operit-like internal tool runtime
+ switchable external AI API providers
```

The app should not become a general-purpose phone file manager or a full Operit clone.

## 3. Core Decision

The local file manager line should be repositioned.

Old direction:

```text
General built-in file manager
```

New direction:

```text
Project workspace browser / code workbench
```

The built-in file UI should serve:

- local repositories,
- code project browsing,
- file editing,
- AI context selection,
- Git status and diff review,
- patch preview,
- task continuation.

It should not prioritize:

- full-disk file management,
- root file management,
- media management,
- archive/APK deep management,
- complex dual-pane copy/move workflows,
- replacing MT Manager or NP Manager.

## 4. Local-First Strategy

The AI coding system is local-first.

Local:

- workspace metadata,
- cloned/imported repositories,
- project memory,
- task board,
- AI session history,
- patch history,
- Git status/diff/commit state,
- execution logs.

Remote/external:

- AI inference API calls,
- GitHub API operations,
- optional Git remote pull/push,
- optional dependency downloads during build.

Do not introduce a custom synchronization server in the first architecture.

## 5. External AI API Strategy

The first implementation should support an OpenAI-compatible provider interface.

Required settings:

- provider name,
- base URL,
- API key,
- model,
- max tokens,
- temperature,
- enable memory,
- enable tool calling,
- request timeout.

Likely compatible providers:

- OpenAI,
- DeepSeek,
- Qwen compatible mode,
- Kimi compatible endpoints,
- SiliconFlow,
- OpenRouter,
- local Ollama/LiteLLM gateway if exposed through an OpenAI-compatible API.

Provider-specific clients can be added later only behind a common `AiModelClient` interface.

## 6. Memory Model Direction

Reference from `灵_AI记忆体`: memory should be structured long-term state, not only chat transcript text.

SunsetGitHub should model project memory as layered state:

```text
AiProjectMemory
├── ProjectIntent
├── ArchitectureDecisions
├── Constraints
├── CurrentState
├── TaskContinuity
├── CodebaseMap
├── KnownIssues
├── PendingTasks
├── VerificationState
├── SafetyPolicy
└── AgentWorkingRules
```

The project should also maintain human-readable memory documents:

```text
docs/ai-memory/
├── PROJECT_MEMORY.md
├── CURRENT_STATE.md
├── TASK_BOARD.md
├── DECISIONS.md
├── TOOL_POLICY.md
├── CHANGELOG.md
└── VERIFICATION_LOG.md
```

The app may later store the same concepts in Room or JSON for UI and automation, but Markdown memory files are the first durable project-level memory.

## 7. Tool Runtime Direction

Reference from Operit: AI should operate through explicit, permissioned tools rather than uncontrolled direct actions.

Initial internal tools should be small and auditable:

- `read_file`,
- `write_file`,
- `list_files`,
- `grep_code`,
- `apply_patch`,
- `git_status`,
- `git_diff`,
- `git_commit`,
- `update_memory`,
- `append_changelog`,
- `run_verification`.

Tools must report:

- success/failure,
- output,
- error message,
- changed files,
- execution duration,
- safety warnings.

Dangerous operations require explicit policy and confirmation.

## 8. Workspace Strategy

AI and Git operations should operate on app-controlled local workspaces whenever possible.

Preferred storage shape:

```text
app files/
└── workspaces/
    ├── repositories/
    ├── imports/
    ├── ai-sessions/
    └── temp/
```

External file pickers remain useful for importing files or projects, but external `content://` URIs should not be the primary surface for JGit or AI code editing.

## 9. Git/JGit Strategy

JGit should be introduced as a dedicated module, not as part of the file manager.

Target boundaries:

```text
domain/git/
data/git/
ui/git/
```

MVP operations:

- open repository,
- clone repository,
- status,
- diff,
- add,
- commit,
- branch metadata.

Later operations:

- checkout,
- fetch,
- pull,
- push,
- merge conflict detection,
- conflict resolution UI.

A command backend inspired by Operit terminal capabilities can be added later behind a `CommandExecutor` abstraction, but should not be required for the first JGit MVP.

## 10. AI Coding Engine Direction

The AI coding engine should orchestrate memory, workspace, Git, model API, and tools.

High-level loop:

```text
load project memory
read task board
inspect workspace and Git status
collect relevant context
build prompt
call AI provider
parse tool calls or patch
execute tools under policy
preview/apply patch
run verification if configured
update memory documents
record changelog and handoff
```

AI changes should prefer patch/diff-based application. Silent direct file modifications should be avoided.

## 11. Safety Policies

The AI coding system must not send or modify sensitive files by default.

Default exclude examples:

- `local.properties`,
- `*.jks`,
- `*.keystore`,
- `*.pem`,
- `*.key`,
- `.env`,
- token/secret/password files,
- `.gradle/`,
- `.kotlin/`,
- `build/`,
- `app/build/`,
- APK/AAB outputs.

Default prohibited operations:

- `git reset --hard`,
- `git clean -fd`,
- deleting workspace root,
- uploading local secrets,
- modifying signing files,
- rewriting unrelated project files.

## 12. Architecture Packages

Future package targets:

```text
app/src/main/kotlin/com/Sunset/REN/GitHub/domain/ai/
app/src/main/kotlin/com/Sunset/REN/GitHub/data/ai/
app/src/main/kotlin/com/Sunset/REN/GitHub/ui/ai/

app/src/main/kotlin/com/Sunset/REN/GitHub/domain/workspace/
app/src/main/kotlin/com/Sunset/REN/GitHub/data/workspace/
app/src/main/kotlin/com/Sunset/REN/GitHub/ui/workspace/

app/src/main/kotlin/com/Sunset/REN/GitHub/domain/git/
app/src/main/kotlin/com/Sunset/REN/GitHub/data/git/
app/src/main/kotlin/com/Sunset/REN/GitHub/ui/git/
```

Avoid placing AI coding logic directly inside Fragment classes or the existing generic file manager.

## 13. Phased Plan

### Phase 0: Project memory documents

- Add AI roadmap.
- Add task board.
- Add agent working rules.
- Add local memory documents.
- Mark the old file manager implementation plan as historical/secondary for AI coding work.

### Phase 1: AI settings and provider abstraction

- Add local settings storage for OpenAI-compatible API configuration.
- Define common `AiModelClient` models.
- Implement non-streaming request first.
- Add streaming later.

### Phase 2: Workspace foundation

- Define workspace models.
- Define workspace storage paths.
- Support local workspace discovery/import metadata.
- Keep external file picker as import helper.

### Phase 3: Tool runtime MVP

- Define `AiTool` and `AiToolRegistry`.
- Implement read/list/search/patch/memory tools.
- Add safety policy checks.
- Log tool execution results.

### Phase 4: AI coding engine MVP

- Load memory and task board.
- Build context.
- Call AI provider.
- Parse patch or tool-call text.
- Preview/apply patch.
- Update memory and changelog.

### Phase 5: Git/JGit MVP

- Add JGit dependency after build review.
- Implement status/diff/commit.
- Show Git status in workspace UI.

### Phase 6: Verification and automation

- Add configured verification commands.
- Add Gradle build runner when environment allows.
- Add task-level success/failure continuation.

### Phase 7: Advanced tool and workflow compatibility

- Consider Operit-like package/skill compatibility only after internal tool runtime stabilizes.
- Consider command/proot backend only behind abstractions.

## 14. Explicit Non-Goals For First Version

- Do not build a full general file manager.
- Do not copy Operit source code into the project.
- Do not depend on Operit app signature or private APIs.
- Do not add a custom synchronization server.
- Do not implement plugin market first.
- Do not allow AI to apply broad destructive operations.
- Keep AI coding/workspace architecture local-first and boundary-driven; app UI technical-debt cleanup follows the separate Compose-first migration plan.

## 15. Decision Summary

SunsetGitHub will reference `灵_AI记忆体` for structured memory concepts and Operit for tool-runtime/workflow concepts. The implementation should remain local-first, API-provider-switchable, Git-aware, and focused on code workspaces rather than general device file management.
