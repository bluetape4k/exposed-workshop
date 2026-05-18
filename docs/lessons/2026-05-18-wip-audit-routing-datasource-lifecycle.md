# WIP Audit Routing Datasource Lifecycle

## Context

The 2026-05-18 qmd-backed exposed-workshop audit compared prior README/WIP
refresh lessons, live GitHub issue state, and current source markers.

## Decision or Finding

The chapter 11 routing datasource example registers tenant-owned
`HikariDataSource` instances but does not give the registry or a Spring lifecycle
hook ownership of closing them. The existing config contains a TODO for
`@PreDestroy` cleanup, and the unit test manually closes pools in `finally`,
which confirms explicit cleanup is required.

## Outcome

Registered GitHub issue #70 and refreshed `WIP.md` from 0 assigned issues to
20 live assigned issues.

## Verification

- `qmd query ... --no-rerank -c bluetape4k-docs` surfaced the prior
  exposed-workshop WIP refresh lesson.
- `gh issue list --assignee debop` confirmed 20 live assigned open issues after
  registering #70.
- `gh issue list --search "Hikari routing datasource close PreDestroy"` found
  no duplicate.
- `./gradlew :03-routing-datasource:test --tests "exposed.examples.routing.config.RoutingDataSourceConfigTest"`
  completed with `BUILD SUCCESSFUL` and `2 passing`.

## Future Guidance

For datasource-routing examples, define resource shutdown ownership in the
sample itself. Do not rely on tests to close pools manually when production
Spring context shutdown still lacks deterministic cleanup.
