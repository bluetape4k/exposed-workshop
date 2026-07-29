# Issue #140 CockroachDB serializable retry workshop 설계

Date: 2026-06-29
Issue: https://github.com/bluetape4k/exposed-workshop/issues/140
Parent epic: https://github.com/bluetape4k/exposed-workshop/issues/137

## 목표

CockroachDB connection setup과 serializable transaction retry를 위한
`bluetape4k-exposed-cockroachdb` helper surface 사용법을 가르치는 13장 workshop module을
추가한다.

## 현재 근거

- `bluetape4k-exposed` exposes `CockroachDatabase.connect`,
  `CockroachTransactionRetryOptions`, `Throwable.isCockroachRetryableTransactionError()`,
  and `withCockroachTransaction`.
- Library는 CockroachDB를 custom Exposed dialect가 아니라 PostgreSQL-wire helper module로
  의도적으로 유지한다.
- CockroachDB serializable retry handling은 전체 transaction을 다시 시작해야 한다. Exposed
  generic `maxAttempts`는 CockroachDB-specific classification 없이 `SQLException`을 retry하므로
  너무 넓다.
- `exposed-workshop` 13장은 이미 issue #140을 위해
  `13-ecosystem-integrations/03-cockroachdb-retry`를 예약했다.

## 범위

- `13-ecosystem-integrations/03-cockroachdb-retry`를 만든다.
- 하나의 retryable transaction 안에서 inventory row와 ledger row를 모두 쓰는 작은 inventory
  reservation transaction을 보여 준다.
- Test에서는 `CockroachServer.Launcher.cockroach`와 함께 `CockroachDatabase.connect`를
  사용한다.
- Timing-sensitive concurrent write race 대신 deterministic SQLSTATE `40001` retry injection을
  사용한다.
- README locale pair와 하나의 SVG+PNG sequence diagram을 다음 위치에 추가한다.
  `docs/images/readme-diagrams/`.

## 비목표

- Custom CockroachDB dialect support.
- R2DBC retry support.
- Savepoint-based advanced retry protocol.
- Real multi-node CockroachDB cluster testing.
- Public bluetape4k-exposed helper API 밖의 generic retry wrapper.

## 수용 기준

- Test는 schema bootstrap, successful transaction, retryable serialization conflict,
  non-retryable SQL failure를 다룬다.
- README.md와 README.ko.md는 CockroachDB의 serializable default와 whole-transaction retry가
  correctness requirement인 이유를 설명한다.
- Diagram은 bluetape4k diagram style을 따르고 SVG/PNG로 검증된다.
- Examples workflow는 `:03-cockroachdb-retry:build`를 포함한다.
- PR body의 마지막 `##` section은 `## DoD Status`다.
