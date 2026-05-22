# Issue 63 Chapter 12 Wiring

## Context

Issue #63 asked to make chapter 12 production integration examples discoverable
from root documentation and explicit about verification coverage.

## Decision

Keep chapter 12 module registration automatic through
`includeModules("12-production-integration", false, false)`. Wire discoverability
through the root English/Korean README module map and keep detailed paired
Spring/Ktor verification commands in the chapter README.
Add chapter 12 modules 05 through 10 to `.github/workflows/examples.yml` so the
daily Examples workflow and chapter-change PRs cover the completed examples.

## Outcome

The root README production integration section now lists all completed chapter
12 examples from 01 through 10. The chapter README embeds a committed PNG
chapter architecture diagram, records local verification, Examples workflow
coverage, CI DB matrix coverage, and the reason no separate nightly override is
needed for the current self-contained examples.

## Verification

Run the root README link scan and the chapter 12 README diagram scan after the
documentation edit. Confirm Gradle project discovery with `./gradlew projects`
or a settings scan before opening the PR.

## Future Agents

When adding a new chapter 12 example, update both the chapter README pair and
the root README pair. Keep Architecture Diagram PNG links committed, and add
workflow-specific entries only when the new example requires external
infrastructure or non-default CI coverage. Regenerate
`docs/images/readme-diagrams/12-production-integration-architecture-01.svg` and
its PNG when the chapter-level module map changes.
