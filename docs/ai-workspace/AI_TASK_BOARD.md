# AI Task Board

This task board tracks implementation of the local-first AI coding workspace direction.

## Current Status

Planning memory has been created. No implementation code has been added for AI coding, JGit, or tool runtime yet.

## Now

### T0.1 Mark file manager plan as secondary for AI coding work

Goal:
- Update `docs/FILE_MANAGER_IMPLEMENTATION_PLAN.md` to clarify that AI coding/workspace/JGit development should follow `docs/AI_CODING_WORKSPACE_ROADMAP.md`.

Allowed files:
- `docs/FILE_MANAGER_IMPLEMENTATION_PLAN.md`
- `docs/ai-memory/CHANGELOG.md`

Acceptance:
- Existing file manager plan remains available as historical/reference material.
- New direction is clearly linked.

### T0.2 Create AI memory baseline documents

Goal:
- Fill initial contents under `docs/ai-memory/`.

Files:
- `PROJECT_MEMORY.md`
- `CURRENT_STATE.md`
- `TASK_BOARD.md`
- `DECISIONS.md`
- `TOOL_POLICY.md`
- `CHANGELOG.md`
- `VERIFICATION_LOG.md`

Acceptance:
- Documents are readable by future AI agents.
- They summarize the current decisions from roadmap and tool runtime design.

## Next

### T1.1 Define AI provider domain models

Goal:
- Add pure Kotlin models for AI provider configuration and chat requests/responses.

Suggested package:
- `domain/ai/`

Candidate files:
- `AiProviderModels.kt`
- `AiChatModels.kt`

Acceptance:
- No Android UI dependency.
- No network implementation yet.
- Models support OpenAI-compatible request concepts.

### T1.2 Define AI settings storage boundary

Goal:
- Design settings store for provider/base URL/API key/model/maxTokens/temperature.

Suggested package:
- `data/ai/`

Acceptance:
- API key is not stored in project files.
- Initial implementation can use SharedPreferences if consistent with existing project style.

### T2.1 Define workspace domain models

Goal:
- Add local workspace metadata models.

Suggested package:
- `domain/workspace/`

Acceptance:
- Models represent workspace id, name, type, root path, source, timestamps.
- No file manager UI changes.

### T3.1 Define tool runtime domain models

Goal:
- Add pure Kotlin tool models and result types.

Suggested package:
- `domain/ai/` or `domain/aitool/` after naming decision.

Acceptance:
- Includes tool name, schema, risk level, execution result.
- Does not execute commands.

## Later

### T4.1 Implement AI context collector MVP

Goal:
- Read project memory docs, task board, selected files, and Git status placeholder.

### T4.2 Implement patch parser/apply preview MVP

Goal:
- Parse unified diff from AI response and present/apply safely.

### T5.1 Add JGit dependency and status MVP

Goal:
- Add JGit after build review and implement status for local workspace.

### T6.1 Add verification runner abstraction

Goal:
- Define a safe verification runner that can call Gradle only when environment allows.

## Blocked / Requires Confirmation

- Copying any Operit implementation code.
- Depending on Operit app signature/private APIs.
- Changing signing configuration.
- Introducing command execution without policy.
- Adding a custom sync server.

## Done

### D0.1 Create AI coding workspace roadmap

Files:
- `docs/AI_CODING_WORKSPACE_ROADMAP.md`

### D0.2 Create AI tool runtime design

Files:
- `docs/AI_TOOL_RUNTIME_DESIGN.md`

### D0.3 Create AI agent working rules

Files:
- `docs/AI_AGENT_WORKING_RULES.md`
