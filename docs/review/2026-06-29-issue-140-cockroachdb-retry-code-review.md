# Issue #140 CockroachDB retry code review

Date: 2026-06-29
Issue: https://github.com/bluetape4k/exposed-workshop/issues/140

## 검토 범위

- 새 모듈 `13-ecosystem-integrations/03-cockroachdb-retry`
- CockroachDB retry workshop code와 test
- README.md / README.ko.md locale pair
- Diagram source와 rendered PNG
- Chapter/root README link
- Examples workflow registration

## 발견 사항

P0: none.

P1: none.

## 근거

- Test는 schema bootstrap, successful reservation, retryable serialization failure,
  non-retryable SQL failure, retry predicate를 다룬다.
- 구현은 public `bluetape4k-exposed-cockroachdb` API만 사용한다:
  `CockroachDatabase`, `CockroachTransactionRetryOptions`,
  `withCockroachTransaction`, and retry predicate.
- Retry conflict는 timing-sensitive concurrent write race가 아니라 deterministic SQLSTATE
  `40001` injection이다.
- Diagram은 editable SVG와 generated PNG를 사용하며 raw Mermaid가 없다.

## 검증

- `./gradlew :03-cockroachdb-retry:test --no-daemon --no-configuration-cache`: PASS, 5 tests.
- `./gradlew :03-cockroachdb-retry:build --no-daemon --no-configuration-cache`: PASS.
- `./gradlew projects --quiet --no-daemon --no-configuration-cache | grep ':03-cockroachdb-retry'`: PASS.
- `actionlint .github/workflows/examples.yml`: PASS.
- `xmllint --noout docs/images/readme-diagrams/13-cockroachdb-retry-sequence-01.svg`: PASS.
- `cairosvg docs/images/readme-diagrams/13-cockroachdb-retry-sequence-01.svg -o docs/images/readme-diagrams/13-cockroachdb-retry-sequence-01.png -s 2`: PASS.
- Diagram rendered PNG를 시각적으로 검사했다: PASS.
- `git diff --check`: PASS.

## 잔여 위험

Retryable failure path는 deterministic하며 public retry signature를 문서화하지만, live
concurrent write conflict는 아니다. 이는 workshop 안정성을 위한 의도적 선택이며, 나중 issue가
dedicated contention example을 필요로 할 때만 넓혀야 한다.
