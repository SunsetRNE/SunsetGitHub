# Decisions

## D1: Local-first AI coding

The AI coding system should run locally on the phone as much as possible. External services are used for AI inference and optional Git/GitHub network operations, not for custom server-side memory synchronization.

## D2: Use external switchable AI APIs

The first AI provider layer should support OpenAI-compatible APIs with configurable base URL, API key, model, temperature, and max tokens.

## D3: Reference only, do not copy

`灵_AI记忆体` and `AAswordman/Operit` are references for architecture and concepts only. Do not copy code/assets/private APIs or rely on signatures without explicit approval and license review.

## D4: Reposition file manager

The built-in file manager should not grow into a full phone file manager. It should become a project workspace/code browser for local repositories, AI context, Git state, and patch review.

## D5: Tool runtime before autonomous coding

AI coding should use explicit tools with policy, logging, and changed-file tracking. Do not allow uncontrolled direct actions.

## D6: Patch-first modification

AI-generated code changes should prefer patch/diff generation, preview, and controlled application.

## D7: JGit as dedicated module

JGit/Git logic should live behind `domain/git` and `data/git` boundaries, not inside file manager UI.

## D8: Keep hybrid UI boundaries

AI coding/workspace architecture work should remain local-first and boundary-driven; it does not require a wholesale Compose rewrite. SunsetGitHub app UI technical-debt cleanup now follows `docs/COMPOSE_MIGRATION_PLAN.md`, using Compose-first dialogs and selected migrated surfaces while retaining the existing Fragment/NavHost shell.