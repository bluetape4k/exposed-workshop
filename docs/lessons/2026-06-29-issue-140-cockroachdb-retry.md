# Issue #140 CockroachDB retry workshop

## 배경

`bluetape4k-exposed` 1.11.0은 CockroachDB PostgreSQL-wire helper와 bounded serializable
retry helper를 추가했다. `exposed-workshop`에는 13장 아래 issue #140용 runnable example이
필요했다.

## 결정

Public helper API와 test의 deterministic SQLSTATE `40001` retry injection을 사용한다.
Workshop default path에는 race-based concurrent conflict test를 만들지 않는다.

## 결과

새 `03-cockroachdb-retry` module은 다음을 보여 준다.

- `CockroachDatabase.connect` with `CockroachServer.Launcher.cockroach`.
- schema bootstrap for inventory and ledger tables.
- `withCockroachTransaction` around a whole inventory reservation.
- retryable conflict replay와 non-retryable SQL failure boundary.

## 검증

- Test: PASS, 5 tests.
- Build: PASS.
- Diagram validation and visual inspection: PASS.
- Workflow lint and diff check: PASS.

## 향후 지침

CockroachDB retry 예제에서는 default workshop path를 deterministic하게 유지한다. 예제의 목표가
helper API 사용이 아니라 contention behavior라면 live concurrent conflict는 별도의 opt-in
lane에서만 사용한다.

Sequence diagram에서는 line이 label을 관통하지 않도록 pill label을 자신의 message line 위에
둔다. Endpoint가 activation bar에 붙는다면 horizontal terminal segment로 왼쪽 또는 오른쪽
edge에 붙인다. 그래야 endpoint audit이 activation-bar top/bottom hit를 card-corner attachment로
취급하지 않는다.
