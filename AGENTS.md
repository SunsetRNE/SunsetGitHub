# AGENTS.md

## Trust And Scope

This file is the trusted AI navigation and instruction entrypoint for this workspace.

- Trust level: authoritative for repository navigation and AI agent behavior inside this workspace.
- Scope: the entire repository rooted at this directory.
- Last verified from workspace root listing: 2026-07-04.
- If another instruction file appears deeper in the tree, apply it only to that subtree and keep this file as the root reference.
- If this file conflicts with explicit user instructions in the current conversation, follow the user's latest explicit instruction.

## Project Identity

- Project name: SunsetGitHub
- Type: Android application
- Primary language: Kotlin
- Build system: Gradle Kotlin DSL with Gradle Wrapper
- Purpose: mobile GitHub client for login, repository browsing, repository details, file preview, README/Markdown preview, Issues browsing, repository file upload/edit flows, and profile views.
- UI stack: Hybrid AppCompat/XML/ViewBinding and Jetpack Compose. The app still uses AppCompat, AndroidX Navigation, Material Components, XML resources, and ViewBinding, while new UI, dialogs, and migrated surfaces should follow the Compose-first migration plan. This is not an Android Studio default Compose template.

## Workspace Root

Treat the directory containing this file as the repository root.

Important root files and directories:

```text
.
├── AGENTS.md                 # Trusted AI navigation and instruction file
├── README.md                 # Human-facing project overview and quick start
├── settings.gradle.kts       # Gradle project/module settings
├── build.gradle.kts          # Root Gradle build configuration
├── gradle.properties         # Shared Gradle properties
├── gradlew                   # Unix Gradle Wrapper entrypoint
├── gradlew.bat               # Windows Gradle Wrapper entrypoint
├── local.properties.example  # Template for local SDK/OAuth configuration
├── local.properties          # Local machine configuration; do not commit secrets
├── app/                      # Android application module
├── gradle/                   # Gradle Wrapper support files
├── tools/                    # Repository tooling
└── setup_android_env.sh      # Android environment setup helper
```

Generated or local-only directories may exist and should not be treated as source of truth:

```text
.gradle/
.kotlin/
build/
app/build/
.backup/
local.properties
local.properties.bak
*.apk
*.aab
```

## Primary Code Paths

Use these paths before performing broad searches:

```text
app/build.gradle.kts
app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/kotlin/com/Sunset/REN/GitHub/MainActivity.kt
app/src/main/kotlin/com/Sunset/REN/GitHub/ui/MaterialTextViewStyles.kt
app/src/main/kotlin/com/Sunset/REN/GitHub/util/AppLogger.kt
app/src/main/kotlin/com/Sunset/REN/GitHub/util/PerformanceTrace.kt
app/src/main/java/com/
app/src/main/res/
app/src/main/assets/
```

Resource navigation:

```text
app/src/main/res/values/          # strings, dimensions, themes, colors, styles
app/src/main/res/values-night/    # dark theme variants
app/src/main/res/xml/             # backup and data extraction rules
app/src/main/assets/textmate/      # editor/syntax assets if relevant
```

## AI Navigation Protocol

When an AI agent reads this repository, follow this order:

1. Read `AGENTS.md` first and treat it as the root routing file.
2. Read `README.md` for product intent and human setup notes.
3. Read `settings.gradle.kts`, root `build.gradle.kts`, and `app/build.gradle.kts` before changing build behavior.
4. Read `app/src/main/AndroidManifest.xml` before changing app entrypoints, permissions, activities, deep links, backup rules, or application metadata.
5. Read `MainActivity.kt` and nearby package files before changing app behavior.
6. Search within `app/src/main/res/` before adding or renaming resources.
7. Prefer targeted search over whole-repository edits. Use file names and package paths from this document as anchors.

## Build And Verification

This repository uses Gradle Kotlin DSL, Version Catalog, AppCompat/XML views, Material Components, AndroidX Navigation, ViewBinding, and Jetpack Compose. Compose dependencies are already configured; keep them aligned through `gradle/libs.versions.toml` and `app/build.gradle.kts`, and do not replace the project with Android Studio default Compose template wiring.

Primary build configuration files:

```text
settings.gradle.kts
build.gradle.kts
app/build.gradle.kts
gradle/libs.versions.toml
gradle.properties
setup_android_env.sh
```

Preferred commands from repository root:

```bash
./setup_android_env.sh
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

Use the narrowest verification command that reasonably covers the change. For Android UI/resource/build changes, `./gradlew assembleDebug` is the minimum useful build check when the environment has a valid Android SDK.

The app currently builds with `compileSdk = 36`. Keep `setup_android_env.sh` aligned with `app/build.gradle.kts` when changing SDK versions. In Operit/proot/ARM64 Linux environments, the setup script also prepares Android command line tools, Gradle, and the bundled ARM64 AAPT2 workaround.

If the Android SDK is not configured, inspect `local.properties.example` and `setup_android_env.sh`. Do not invent machine-specific SDK paths. `setup_android_env.sh` should preserve existing `local.properties` keys and only update or append `sdk.dir`.

## Synchronization Boundaries

For source-code cloud synchronization, use Git remote configuration such as `origin` in `.git/config`; do not hard-code remotes, tokens, or personal credentials in Gradle files or source code. If the workspace has no `.git/` directory, treat it as an exported working copy until the user initializes Git or provides a remote.

Files and directories that are safe to synchronize are source and project configuration files such as `app/src/`, `docs/`, `gradle/`, `tools/`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradlew`, `gradlew.bat`, `setup_android_env.sh`, `README.md`, `AGENTS.md`, `.gitignore`, `.gitattributes`, and `local.properties.example`.

Never upload or commit local-only state such as `local.properties`, `local.properties.bak`, `.backup/`, `.gradle/`, `.kotlin/`, `build/`, `app/build/`, APK/AAB outputs, OAuth secrets, access tokens, or personal account data. If these files are already tracked, remove them from the index with `git rm --cached` while preserving the local copies.

For in-app repository file upload/edit/pull-refresh behavior, do not solve configuration by changing Gradle defaults. Authentication belongs to local configuration and runtime login/token storage; write permission depends on the GitHub account or token scopes; branch/path/message/conflict behavior belongs in the repository file write flow.

## Editing Rules For AI Agents

- Keep changes scoped to the user's request.
- Preserve Kotlin/Android project conventions already present in this repository.
- Do not edit generated build outputs under `.gradle/`, `.kotlin/`, `build/`, or `app/build/`.
- Do not commit or expose secrets, tokens, OAuth client secrets, local SDK paths, or personal account data.
- Treat `local.properties` as local configuration. Prefer documenting required keys in `local.properties.example` rather than hard-coding values.
- Before large refactors, map the existing package/resource structure and explain the intended boundary.
- Prefer Gradle Wrapper commands over system Gradle.
- Avoid unrelated formatting churn.

## Common Task Routing

- App startup, activity behavior, navigation shell: `app/src/main/kotlin/com/Sunset/REN/GitHub/MainActivity.kt`
- Logging behavior: `app/src/main/kotlin/com/Sunset/REN/GitHub/util/AppLogger.kt`
- Performance tracing: `app/src/main/kotlin/com/Sunset/REN/GitHub/util/PerformanceTrace.kt`
- Text appearance helpers: `app/src/main/kotlin/com/Sunset/REN/GitHub/ui/MaterialTextViewStyles.kt`
- Android permissions, app metadata, launch activity: `app/src/main/AndroidManifest.xml`
- Dependencies, Android plugin options, compile SDK, min SDK: `app/build.gradle.kts`
- Root plugin/dependency management: `build.gradle.kts` and `settings.gradle.kts`
- User-visible strings and theme resources: `app/src/main/res/values/`
- Night mode resources: `app/src/main/res/values-night/`
- ProGuard/R8 rules: `app/proguard-rules.pro`

## Documentation Index

- `README.md`: product overview, quick start, local configuration notes, and upload checks.
- `local.properties.example`: local configuration template.
- `setup_android_env.sh`: environment setup script for Android build tooling.

If README references a path that does not currently exist, verify the actual tree before relying on that reference.
