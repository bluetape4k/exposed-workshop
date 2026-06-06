# Issue 118 Examples Workflow Review

## Scope

- `.github/workflows/examples.yml`
- `docs/lessons/2026-06-06-issue-118-examples-weekly.md`

## Review Result

- P0: 0
- P1: 0
- P2: 0

## Findings

No blocking findings.

## Evidence

- The schedule is weekly: `0 21 * * 0`.
- `workflow_dispatch`, push path filters, and pull request path filters remain
  present.
- Selected downstream example modules stay in one Gradle invocation, preserving
  sequential Testcontainers behavior.
- Failed test reports are uploaded through `actions/upload-artifact@v5` with
  `if: failure()`.
- `actionlint .github/workflows/examples.yml` passed.
- `git diff --check` passed.

## Residual Risk

GitHub Actions dispatch was not manually run before PR creation; the PR checks
are the required live validation surface for this workflow-only change.
