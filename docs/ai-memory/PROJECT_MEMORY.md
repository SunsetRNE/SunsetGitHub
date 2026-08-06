# Project Memory

SunsetGitHub is a Kotlin Android GitHub client. Future AI coding work should extend it into a local-first Android AI coding workspace without changing its core identity.

## Reference Sources

The project references two systems only for architectural ideas:

- `灵_AI记忆体`: structured long-term memory, conversation-memory binding, local state persistence.
- `AAswordman/Operit`: tool runtime, workflow, local automation, workspace-oriented tool calls.

No code or assets are copied from those systems.

## Long-Term Direction

```text
SunsetGitHub
= GitHub client
+ local workspace
+ project memory
+ JGit/Git operations
+ AI coding engine
+ internal tool runtime
+ switchable external AI API
```

## Key Constraints

- Local-first; no custom sync server in MVP.
- External AI API is configurable.
- Use OpenAI-compatible API first.
- Do not make the built-in file manager a full phone file manager.
- Reposition file UI toward code workspace browsing.
- Do not depend on Operit signatures/private APIs.
- Do not copy external LGPL code without explicit license review.
- SunsetGitHub app UI cleanup now follows `docs/COMPOSE_MIGRATION_PLAN.md`: new UI, dialogs, and selected migrated surfaces are Compose-first while the AI coding/workspace architecture remains local-first and boundary-driven.

## Core Architecture Direction

Future packages:

```text
domain/ai, data/ai, ui/ai
domain/workspace, data/workspace, ui/workspace
domain/git, data/git, ui/git
```

AI coding should be orchestrated by engine/repository classes, not by Fragment classes.