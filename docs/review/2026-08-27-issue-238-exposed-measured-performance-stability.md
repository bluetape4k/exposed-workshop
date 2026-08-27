# 이슈 #238 measured 예제 성능·안정성 위험 예측

## 검토 범위

- 대상: 승인된 `bluetape4k-exposed-measured` JDBC 예제의 catalog, 신규
  `ProductTable`/`ProductEntity`, `AbstractExposedTest` 기반 dialect 테스트,
  README/다이어그램 등록
- source: `docs/superpowers/specs/2026-08-27-issue-238-exposed-measured-design.md`,
  `docs/superpowers/plans/2026-08-27-issue-238-exposed-measured-plan.md`,
  provider `MeasuredColumnTypes.kt`/`Units.kt`/`Temperature.kt`, 기존
  `06-advanced/05-exposed-money`와 shared test
- 범위: 한 명의 개발자가 유지하는 학습용 JDBC 모듈. production benchmark,
  R2DBC, 운영 cache/telemetry는 포함하지 않는다.

## 위험과 조기 신호

| 영역 | 위험 | 조기 신호 | 완화 및 재실행 지점 |
|---|---|---|---|
| 의존성 | `exposed-measured` alias가 BOM `1.4.0`에서 해석되지 않거나 provider 전이 의존성이 빠짐 | `dependencyInsight` 또는 test compile 실패 | 중앙 coordinate와 local alias를 대조하고 직접 버전을 고정하지 않는다. 단계 1의 catalog/build만 수정한 뒤 dependency insight를 재실행한다. |
| DB 안정성 | dialect가 `DOUBLE`/nullable 값을 다르게 반환함 | H2는 통과하지만 PostgreSQL/MySQL/MariaDB 왕복 실패 | `ENABLE_DIALECTS_METHOD`와 `withTables`를 유지하고 H2 fast → 실제 JDBC dialect 순서로 직렬 재실행한다. |
| 수치 정확도 | `DOUBLE`을 exact equality로 검증해 Celsius offset·큰 값에서 flaky test 발생 | 허용 오차 밖 assertion 또는 DB별 마지막 자리 차이 | `shouldBeNear`와 representative finite values를 사용한다. DECIMAL/반올림 정책은 추가하지 않는다. 실패 시 assertion만 보정하고 provider 계약은 유지한다. |
| API 경계 | DAO delegated property와 DSL 컬럼의 generic 타입/기준 단위가 어긋남 | row와 entity가 서로 다른 단위·null을 반환하거나 receiver shadowing compile error | 같은 `ProductTable`을 양쪽이 사용하게 하고 `columnType`/base unit 정적 확인을 추가한다. `org.jetbrains.exposed.v1.*` import와 명시적 receiver를 재점검한다. |
| lifecycle | 테스트 간 전역 DB/Entity cache가 남거나 병렬 컨테이너가 충돌함 | 테이블 잔존, lock timeout, flaky dialect 결과 | `withTables` 및 `maxParallelUsages = 1`, module `junit-platform.properties`를 사용한다. Testcontainers는 병렬 실행하지 않고 실패한 dialect부터 다시 실행한다. |
| 문서/스키마 | 코드의 meter/kg/K와 README/ERD 설명이 drift하거나 표시 단위를 DB에 저장하는 것으로 오해됨 | source-equivalent parity/semantic audit 실패 | 기준 단위와 migration 주의를 모든 locale/asset에 반복하고 ledger·PNG를 같은 source에서 재생성한다. docs slice만 롤백/재생성한다. |
| CI 등록 | 신규 module이 full CI/Nightly에서 실행되지 않음 | `projects`에는 있으나 nightly task/grep에 없음 | Nightly의 PG/MySQL/MariaDB shard-1에 `:13-exposed-measured:test`를 등록하고 actionlint/registration grep을 실행한다. |

## 성능 판정

- provider는 측정값 하나를 `DOUBLE` 하나로 변환하고, 예제는 상품 row를 한 번
  insert/select하는 학습 흐름이다. 새 반복 변환, reflection, serialization,
  unbounded buffer, polling, retry, cache, network call을 도입할 계획이 없다.
- DSL/DAO read-back은 같은 transaction과 같은 테이블을 사용하므로 불필요한
  추가 DB round-trip을 만들지 않는다. benchmark는 production throughput을
  주장하지 않으며 `N/A (production benchmark out of scope)`이다.
- 실제 dialect/Testcontainers 검사는 shared infrastructure를 재사용하고
  직렬화한다. 컨테이너 수명과 connection pool을 예제 코드가 소유하지 않는다.

## 안정성 판정

- 동기 JDBC 예제이므로 coroutine cancellation, event-loop blocking, virtual
  thread monitor pinning은 해당 없음(`N/A — no coroutine/event-loop code`).
- `withTables`가 각 dialect 테스트의 schema lifecycle을 소유하고, 신규 테스트
  resource는 parallel execution을 끈다. 실패 시 skip을 성공으로 해석하지 않는다.
- provider의 unsupported DB value는 `error(...)`로 즉시 드러나며, workshop은
  그 계약을 한 개의 경계 테스트로 고정하고 provider 내부 구현을 복제하지 않는다.
- 기준 단위 변경은 기존 `DOUBLE` 데이터를 변환해야 하는 migration 위험이다.
  이번 모듈은 격리 학습 schema라 migration을 구현하지 않고 README/ERD에 명시한다.

## 결론

- P0: **0**
- P1: **0**
- P2: 위 위험은 단계 1–7의 dependency insight, dialect test, near assertion,
  module resource, docs/diagram audit, CI registration으로 검증 가능하다.
- A-05 상태: `PASS` — 구현 전에 위험·신호·완화·rollback/rerun 지점을 계획과
  함께 고정했다.

## SPW writer gate

- [x] **SPW-01** — 대상 독자와 JDBC-only 범위, production benchmark/R2DBC 제외를
  명시했다.
- [x] **SPW-02** — 의존성·DB·수치·API·lifecycle·docs·CI 위험과 대응을 기록했다.
- [x] **SPW-03** — 한국어 work document 문체와 code/API/identifier/command를
  보존했다.
- [x] **SPW-04** — provider/local/shared-test source 근거를 대조했다.
- [x] **SPW-05** — 성능/안정성 scan은 구현 전 prediction으로 완료했고 P0/P1=0이다.
