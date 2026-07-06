# Issue #140 CockroachDB Retry Code Review

Date: 2026-06-29
Issue: https://github.com/bluetape4k/exposed-workshop/issues/140

## Scope Reviewed

- New module `13-ecosystem-integrations/03-cockroachdb-retry`
- CockroachDB retry workshop code and tests
- README.md / README.ko.md locale pair
- Diagram source and rendered PNG
- Chapter/root README links
- Examples workflow registration

## Findings

P0: none.

P1: none.

## Evidence

- Tests cover schema bootstrap, successful reservation, retryable
  serialization failure, non-retryable SQL failure, and retry predicate.
- Implementation uses public `bluetape4k-exposed-cockroachdb` APIs only:
  `CockroachDatabase`, `CockroachTransactionRetryOptions`,
  `withCockroachTransaction`, and retry predicate.
- Retry conflict is deterministic SQLSTATE `40001` injection instead of a
  timing-sensitive concurrent write race.
- Diagram uses editable SVG plus generated PNG and no raw Mermaid.

## Verification

- `./gradlew :03-cockroachdb-retry:test --no-daemon --no-configuration-cache`: PASS, 5 tests.
- `./gradlew :03-cockroachdb-retry:build --no-daemon --no-configuration-cache`: PASS.
- `./gradlew projects --quiet --no-daemon --no-configuration-cache | grep ':03-cockroachdb-retry'`: PASS.
- `actionlint .github/workflows/examples.yml`: PASS.
- `xmllint --noout docs/images/readme-diagrams/13-cockroachdb-retry-sequence-01.svg`: PASS.
- `cairosvg docs/images/readme-diagrams/13-cockroachdb-retry-sequence-01.svg -o docs/images/readme-diagrams/13-cockroachdb-retry-sequence-01.png -s 2`: PASS.
- Diagram rendered PNG inspected visually: PASS.
- `git diff --check`: PASS.

## Residual Risk

The retryable failure path is deterministic and documents the public retry
signature, but it is not a live concurrent write conflict. That is intentional
for workshop stability and should only be widened if a later issue needs a
dedicated contention example.
