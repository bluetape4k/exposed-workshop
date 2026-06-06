# Issue 118 Examples Weekly Gate

## Context

`exposed-workshop` already had an `Examples` workflow, but issue #118 required
the downstream example gate to run weekly instead of daily while preserving
manual dispatch and PR/push path filters.

## Decision

- Use one weekly scheduled run for selected downstream example modules.
- Keep selected examples in a single Gradle invocation so Testcontainers-backed
  paths do not compete across workflow jobs.
- Upload test reports only on failure to keep passing runs lightweight.

## Outcome

The workflow remains separate from CI and Nightly, retains `workflow_dispatch`
and path filters, documents the selected gate scope inline, and publishes failed
example reports as an artifact.

## Verification

- `actionlint .github/workflows/examples.yml`
- `git diff --check`

## Future Guidance

When adding new downstream examples, prefer extending the selected Examples
workflow scope instead of moving the modules into CI or Nightly.
