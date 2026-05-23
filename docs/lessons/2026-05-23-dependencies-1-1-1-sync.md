# Dependencies 1.1.1 Sync

## Context

`bluetape4k-dependencies` 1.1.0 was superseded by 1.1.1 after the artifact
availability audit found generated aliases for non-published mock web
application modules. This workshop consumes the shared catalog and should stay
aligned with the release train.

## Decision

Consume `bluetape4k-dependencies = "1.1.1"` through the standard shared-version
sync path. Do not add workshop-local overrides for artifacts that the central
catalog has already removed.

## Outcome

PR #96 aligned this repository to the 1.1.1 catalog and merged after CI passed.

## Verification

- GitHub PR #96 status checks passed before merge.
- Workspace-level `scripts/sync-shared-versions.py --workspace .. --check --summary`
  passed after the downstream PRs were merged.

## Future Guidance

When the shared catalog patch fixes publication availability, wait until Maven
Central `repo1` resolves the new version before rerunning downstream CI. Keep
workshop dependency drift fixes in the catalog whenever they affect multiple
bluetape4k repositories.
