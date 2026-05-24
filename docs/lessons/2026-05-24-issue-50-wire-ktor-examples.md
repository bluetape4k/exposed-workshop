# Issue 50 Wire Ktor Chapter Examples

## Context

Issue #50 asked to wire the new Ktor chapter examples into documentation and verification guidance.

## Decision

Record the chapter 11 Ktor module links, test tasks, and CI/nightly coverage decision in both English and Korean README files. Treat the Ktor modules as H2-only examples that do not need container-backed nightly coverage.

## Outcome

Updated `11-high-performance/README.md` and `README.ko.md` with Ktor cache, coroutine cache, and routing datasource modules plus verification commands.

## Verification

Passed: `git diff --check`.

## Future Guidance

Docs PRs that depend on feature PRs may reference not-yet-merged module directories, but they should clearly list the prerequisite module PRs in the PR body.
