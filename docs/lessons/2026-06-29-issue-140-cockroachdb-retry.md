# Issue #140 CockroachDB Retry Workshop

## Context

`bluetape4k-exposed` 1.11.0 added CockroachDB PostgreSQL-wire helpers and a
bounded serializable retry helper. `exposed-workshop` needed a runnable example
for issue #140 under chapter 13.

## Decision

Use public helper APIs and a deterministic SQLSTATE `40001` retry injection in
tests. Do not create a race-based concurrent conflict test for the workshop
default path.

## Outcome

The new `03-cockroachdb-retry` module demonstrates:

- `CockroachDatabase.connect` with `CockroachServer.Launcher.cockroach`.
- schema bootstrap for inventory and ledger tables.
- `withCockroachTransaction` around a whole inventory reservation.
- retryable conflict replay and non-retryable SQL failure boundary.

## Verification

- Test: PASS, 5 tests.
- Build: PASS.
- Diagram validation and visual inspection: PASS.
- Workflow lint and diff check: PASS.

## Future Guidance

For CockroachDB retry examples, keep the default workshop path deterministic.
Use live concurrent conflicts only in a separate opt-in lane if the example's
goal is contention behavior rather than helper API usage.

For sequence diagrams, keep pill labels above their own message lines instead
of letting the line run through the label. If endpoints attach to activation
bars, attach to the left or right edge with a horizontal terminal segment so
the endpoint audit does not treat activation-bar top/bottom hits as card-corner
attachments.
