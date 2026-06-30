# Issue 188 DuckDB Embedded Analytics Lesson

## Context

The blog backlog for ecosystem integrations needed more source-backed examples
before article writing. DuckDB adds a local analytics lane that does not require
a remote service, credentials, Docker, or cloud setup.

## Decision

Use a file-backed DuckDB database and keep one root `DuckDBConnection` open for
the workshop session. Exposed receives duplicated transaction connections so
separate transactions observe the same local database. The README explicitly
states that `queryFlow` is a coroutine consumption boundary after transaction
materialization, not raw JDBC streaming outside the transaction.

## Outcome

The module verifies schema creation, batch insert, aggregate projection,
rendered SQL shape, validation failures, and Flow consumption locally. The
README pair embeds architecture, example-flow, and sequence diagrams with SVG
sources and PNG renders.

## Future Guidance

For DuckDB follow-up examples, add Parquet/CSV scans, Arrow integration, or
extension loading as separate modules. Keep this module focused on the Exposed
+ embedded analytics boundary and avoid introducing real-service assumptions.
