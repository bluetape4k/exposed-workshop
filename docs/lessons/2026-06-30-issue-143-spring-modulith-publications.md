# Issue 143 Spring Modulith Publications

## Context

Issue #143 added `13-ecosystem-integrations/06-spring-modulith-publications`,
a local Spring Modulith publication-store example backed by
`bluetape4k-exposed-spring-modulith`.

## Decision

- Use in-memory H2 and an explicit `springTransactionManager` bean so the
  Exposed Modulith auto-configuration can create the publication repository.
- Provide an explicit Jackson 3 `EventSerializer`; without it, the repository
  condition chain is harder for workshop readers to diagnose.
- Keep the example focused on three operational paths: completed publication,
  failed publication resubmission, and unloadable event rows.

## Diagram Guard

Connector scripts can miss same-line overlap when two routes share a corridor.
After automated audits pass, inspect the full-size PNG and check for same-color
or cross-color route sharing by eye. In this example the retry route was moved
to the lower recovery corridor so it no longer shares the migration-guard path.

## Verification

- `./gradlew :06-spring-modulith-publications:test --no-daemon --no-configuration-cache`
- `./gradlew :06-spring-modulith-publications:build --no-daemon --no-configuration-cache`
- `./gradlew projects --no-daemon --no-configuration-cache`
- `actionlint .github/workflows/examples.yml`
- `git diff --check`
- `xmllint`, CairoSVG render, endpoint, geometry, mixed-corner, connector audits,
  and full-size PNG inspection for the README diagram.
