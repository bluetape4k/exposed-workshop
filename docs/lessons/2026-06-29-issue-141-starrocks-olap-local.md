# Issue 141 - StarRocks Local-First OLAP Lesson

## Context

StarRocks examples can be tempting to treat as Testcontainers-backed integration
tests, but issue #141 asked for a local-first OLAP boundary.

## Decision

Keep the default workshop test lane local and deterministic. Use H2 for SQL
rendering and fixture aggregation, while showing StarRocks-specific DDL shape
through `StarRocksTable`.

## Outcome

The example proves typed connection settings, OLAP DDL rendering, query shape,
and aggregation without Docker or network access. README files document the
separate real StarRocks validation boundary.

## Future Guard

Do not add StarRocks containers to the default workshop CI path unless a later
issue explicitly asks for a heavyweight opt-in or nightly lane.
