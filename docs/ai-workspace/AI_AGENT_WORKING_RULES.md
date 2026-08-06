# AI Agent Working Rules

## Purpose

This file defines how AI agents should continue AI-coding-workspace work in this repository.

Read this file before implementing tasks related to:

- AI coding,
- local workspace,
- JGit/Git,
- project memory,
- tool runtime,
- local file manager repositioning.

## Required Reading Order

For this topic, read:

1. `AGENTS.md`
2. `README.md`
3. `docs/AI_CODING_WORKSPACE_ROADMAP.md`
4. `docs/AI_TOOL_RUNTIME_DESIGN.md`
5. `docs/AI_TASK_BOARD.md`
6. `docs/ai-memory/CURRENT_STATE.md`
7. `docs/ai-memory/TASK_BOARD.md`

## Reference Boundaries

`灵_AI记忆体` and `AAswordman/Operit` are architecture references only.

Allowed:

- reference structural ideas,
- reference memory layering concepts,
- reference tool runtime and workflow concepts,
- reference local-first execution principles.

Not allowed without explicit approval:

- copying source code,
- copying assets,
- copying private APIs,
- depending on app signatures,
- integrating full Operit runtime,
- changing SunsetGitHub license posture by accident.

## Scope Discipline

Do not expand tasks beyond the selected issue.

Do not turn the built-in file manager into a general phone file manager. Reposition it toward workspace/code browsing.

Follow `docs/COMPOSE_MIGRATION_PLAN.md` for SunsetGitHub UI technical-debt cleanup. Do not turn AI coding/workspace architecture work into a wholesale UI rewrite.

Do not introduce a custom sync server.

Do not implement a plugin market before internal tool runtime MVP.

## Safety Rules

Never expose or commit:

- `local.properties`,
- keystores,
- OAuth secrets,
- API keys,
- tokens,
- personal account data,
- APK/AAB outputs,
- build directories.

Do not perform destructive Git operations:

- `git reset --hard`,
- `git clean -fd`,
- force push,
- branch deletion,
- deleting workspace root.

AI-generated code changes should prefer patch/diff review paths.

## Implementation Rules

Use package boundaries from the roadmap:

```text
domain/ai, data/ai, ui/ai
domain/workspace, data/workspace, ui/workspace
domain/git, data/git, ui/git
```

Do not put AI coding orchestration inside Fragment classes.

Keep provider-specific AI API logic behind a common client interface.

Keep Git logic behind a dedicated Git/JGit service boundary.

Keep command execution behind a `CommandExecutor` abstraction if introduced.

## Documentation Update Rules

Every completed AI-coding-workspace task should update at least one of:

- `docs/AI_TASK_BOARD.md`,
- `docs/ai-memory/CURRENT_STATE.md`,
- `docs/ai-memory/CHANGELOG.md`,
- `docs/ai-memory/VERIFICATION_LOG.md`,
- `docs/ai-memory/BLOCKERS.md` if created later.

Update should include:

- what changed,
- files touched,
- verification run or reason not run,
- next recommended task.

## Verification Rules

Use the narrowest useful verification.

For source/model-only changes, inspect compile impact and run tests if available.

For Android build-impacting changes, prefer:

```bash
./gradlew assembleDebug
```

If the environment cannot build, record the reason in `VERIFICATION_LOG.md`.

## Stop Conditions

Stop and ask for user confirmation when:

- a task requires copying external LGPL code,
- a task needs signing/keystore changes,
- a task needs broad command execution,
- a task would alter authentication/token storage,
- a task requires deleting files outside the requested scope,
- a task needs network credentials.

## Handoff Format

When stopping, summarize:

```text
Completed:
- ...

Changed files:
- ...

Verification:
- ...

Risks / blockers:
- ...

Next task:
- ...
```