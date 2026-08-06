# Repository Settings Advanced Backlog

This document records the repository settings work that remains after the first real settings page implementation.

## Current Baseline

Implemented in the repository settings pages:

- Read repository settings from `GET /repos/{owner}/{repo}`.
- Update supported repository settings with `PATCH /repos/{owner}/{repo}`.
- Edit repository name, description, homepage, default branch, and visibility.
- Toggle Issues, Projects, Wiki, Discussions, forking, archived state, merge strategies, delete branch on merge, and auto merge.
- Use admin permission from the repository permissions payload to switch between editable and read-only UI.
- Dedicated native pages exist for branch protection, collaborators/access, Actions permissions/storage/cache, danger zone, Webhooks, and Deploy Keys.
- Danger zone supports archive/unarchive, transfer, and delete with confirmation.
- Webhooks supports list, create, ping, and delete.
- Deploy Keys supports list, add read-only key, and delete.

## Remaining Advanced Settings

### Rulesets

Status: not implemented.

Target capabilities:

- List repository rulesets.
- View rule conditions and enforcement mode.
- Create or edit a ruleset.
- Enable, disable, or delete a ruleset.

Likely APIs:

- `GET /repos/{owner}/{repo}/rulesets`
- `GET /repos/{owner}/{repo}/rulesets/{ruleset_id}`
- `POST /repos/{owner}/{repo}/rulesets`
- `PUT /repos/{owner}/{repo}/rulesets/{ruleset_id}`
- `DELETE /repos/{owner}/{repo}/rulesets/{ruleset_id}`

Notes:

- Rulesets overlap with branch protection but are a separate GitHub feature.
- Treat this as a separate feature area with its own models.
- Start read-only before allowing writes.

### GitHub Pages

Status: not implemented.

Target capabilities:

- Read Pages configuration.
- Enable or update Pages source settings.
- Disable Pages.
- Show latest Pages build/deployment state if available.

Likely APIs:

- `GET /repos/{owner}/{repo}/pages`
- `POST /repos/{owner}/{repo}/pages`
- `PUT /repos/{owner}/{repo}/pages`
- `DELETE /repos/{owner}/{repo}/pages`
- `GET /repos/{owner}/{repo}/pages/builds/latest`

Notes:

- Some Pages operations differ by repository and account type.
- Keep Pages as a dedicated page because configuration and state are more complex than simple toggles.

### Environments

Status: not implemented.

Target capabilities:

- List environments.
- View protection rules and deployment branch policies.
- Create or update an environment.
- Delete an environment.

Likely APIs:

- `GET /repos/{owner}/{repo}/environments`
- `GET /repos/{owner}/{repo}/environments/{environment_name}`
- `PUT /repos/{owner}/{repo}/environments/{environment_name}`
- `DELETE /repos/{owner}/{repo}/environments/{environment_name}`

Notes:

- Environment protection rules are nested and should use typed models.
- Secrets and variables can also exist at environment scope and should not be mixed into the repository-level secrets UI unless explicitly scoped.

### Actions Permissions

Status: not implemented.

Target capabilities:

- Read Actions permissions.
- Update enabled/disabled policy.
- Configure allowed actions policy if supported.
- Configure workflow permissions.

Likely APIs:

- `GET /repos/{owner}/{repo}/actions/permissions`
- `PUT /repos/{owner}/{repo}/actions/permissions`
- `GET /repos/{owner}/{repo}/actions/permissions/workflow`
- `PUT /repos/{owner}/{repo}/actions/permissions/workflow`

Notes:

- This should share UI language with the existing Actions feature area.
- Some org-level policies can override repository settings; display override states clearly.

### Discussions

Status: not implemented.

Target capabilities:

- Detect whether Discussions are enabled.
- Toggle Discussions if supported by available API.
- Link to GitHub web when API support is unavailable.

Likely APIs:

- GitHub REST support for repository Discussions settings is limited compared with other repo settings.
- GraphQL may be required for full discussion category and discussion data features.

Notes:

- Do not implement HTML form submission for this unless there is a deliberate web automation strategy.
- Prefer read-only detection or open-in-GitHub fallback until a stable API path is confirmed.

## Recommended Implementation Order

1. Rulesets read-only, then write support.
2. GitHub Pages read-only, then source/custom-domain operations.
3. Environments read-only, then protection/deployment branch operations.
4. Discussions support after confirming API feasibility.

## Engineering Notes

- Keep the current repository settings overview focused on simple repository-level fields backed by `PATCH /repos/{owner}/{repo}`.
- Add advanced settings as separate pages from the Settings page rather than expanding the overview indefinitely.
- Each advanced settings area should have its own gateway, models, UI state, and confirmation rules.
- Do not rely on GitHub Settings HTML parsing for writes.
- HTML parsing can remain a read-only fallback only when official APIs do not expose a field.
- Destructive actions need confirmation dialogs.
- Secret-like values must not be logged, stored, or redisplayed after submission.
