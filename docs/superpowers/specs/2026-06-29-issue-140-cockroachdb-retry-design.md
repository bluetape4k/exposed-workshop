# Issue #140 CockroachDB Serializable Retry Workshop Design

Date: 2026-06-29
Issue: https://github.com/bluetape4k/exposed-workshop/issues/140
Parent epic: https://github.com/bluetape4k/exposed-workshop/issues/137

## Goal

Add a chapter 13 workshop module that teaches how to use the
`bluetape4k-exposed-cockroachdb` helper surface for CockroachDB connection
setup and serializable transaction retry.

## Current Evidence

- `bluetape4k-exposed` exposes `CockroachDatabase.connect`,
  `CockroachTransactionRetryOptions`, `Throwable.isCockroachRetryableTransactionError()`,
  and `withCockroachTransaction`.
- The library deliberately keeps CockroachDB as a PostgreSQL-wire helper module,
  not a custom Exposed dialect.
- CockroachDB serializable retry handling must restart the whole transaction;
  Exposed generic `maxAttempts` is too broad because it retries `SQLException`
  without CockroachDB-specific classification.
- `exposed-workshop` chapter 13 already reserves
  `13-ecosystem-integrations/03-cockroachdb-retry` for issue #140.

## Scope

- Create `13-ecosystem-integrations/03-cockroachdb-retry`.
- Demonstrate a small inventory reservation transaction that writes both an
  inventory row and a ledger row in one retryable transaction.
- Use `CockroachDatabase.connect` with `CockroachServer.Launcher.cockroach` in
  tests.
- Use deterministic SQLSTATE `40001` retry injection instead of timing-sensitive
  concurrent write races.
- Add README locale pair and one SVG+PNG sequence diagram under
  `docs/images/readme-diagrams/`.

## Non-Goals

- Custom CockroachDB dialect support.
- R2DBC retry support.
- Savepoint-based advanced retry protocol.
- Real multi-node CockroachDB cluster testing.
- Generic retry wrappers outside the public bluetape4k-exposed helper API.

## Acceptance Criteria

- Tests cover schema bootstrap, successful transaction, retryable serialization
  conflict, and non-retryable SQL failure.
- README.md and README.ko.md explain CockroachDB's serializable default and why
  whole-transaction retry is a correctness requirement.
- Diagram follows the bluetape4k diagram style and is validated as SVG and PNG.
- The Examples workflow includes `:03-cockroachdb-retry:build`.
- PR body final `##` section is `## DoD Status`.
