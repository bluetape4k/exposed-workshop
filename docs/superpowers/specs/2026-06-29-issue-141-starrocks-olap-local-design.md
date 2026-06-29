# Issue 141 - StarRocks Local-First OLAP Design

## Context

Issue #141 asks for a workshop example that teaches the local-testability
boundary for the `bluetape4k-exposed-starrocks` helper. The example must avoid
making StarRocks a default CI dependency while still showing StarRocks-oriented
DDL and analytical projection shape.

## Decision

Create `13-ecosystem-integrations/04-starrocks-olap-local`.

- Use `StarRocksAnalyticsProfile` to validate the Connector/J boundary and
  render the expected `jdbc:starrocks://host:port/catalog.database` URL.
- Use `StarRocksTable` for the target rollup table so local DDL rendering keeps
  `ENGINE=OLAP` and `replication_num = 1` visible.
- Use H2 only as a transaction context and fixture store for local deterministic
  projection tests.
- Keep real StarRocks behavior out of the default test path and document it as
  explicit opt-in.

## Acceptance Mapping

- Default tests remain local and deterministic: H2-only tests, no Docker.
- README locale pair documents local and real StarRocks boundaries.
- Tests verify DDL/query shape and a real local aggregation scenario.
- Diagram asset ships as SVG plus rendered PNG under
  `docs/images/readme-diagrams/`.
- Examples workflow includes `:04-starrocks-olap-local:build`.

## Risks

- DDL rendering is structural only; it is not proof of StarRocks storage or
  distribution behavior.
- Real StarRocks validation must remain a separate opt-in lane to avoid hiding
  backend prerequisites inside fast CI.
