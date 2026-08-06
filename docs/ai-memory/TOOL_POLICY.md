# Tool Policy

## Default Allowed Read Tools

- list workspace files,
- read text files under size limits,
- search code/text,
- read Git status/diff,
- read project memory docs.

## Default Allowed Write Tools

Only after policy checks:

- write files inside workspace,
- apply validated patch,
- update memory documents,
- append changelog.

## Confirmation Required

- Git commit,
- command execution,
- build execution,
- deleting files,
- modifying many files,
- changing Gradle/dependencies,
- editing authentication/signing config.

## Denied Paths

```text
local.properties
local.properties.bak
*.jks
*.keystore
*.pem
*.key
.env
.gradle/
.kotlin/
build/
app/build/
.backup/
*.apk
*.aab
```

## Denied Operations

```text
git reset --hard
git clean -fd
force push
branch deletion
rm -rf workspace root
upload secrets
read or modify keystore credentials
```

## Context Sending Rules

Do not send secrets or generated build outputs to external AI APIs. Prefer sending small relevant source excerpts and memory summaries.