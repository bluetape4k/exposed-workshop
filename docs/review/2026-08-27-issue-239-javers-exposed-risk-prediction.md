# 이슈 #239 구현 전 위험 예측

## 범위와 중단 조건

대상은 `13-ecosystem-integrations/12-javers-exposed-audit`의 Exposed JDBC
EntityHook 예제다. 구현은 H2 단일 JVM에서 진행하며, 아래 위험의 signal을
확인하면 해당 계약 테스트가 통과할 때까지 다음 단계로 넘어가지 않는다. 실제
원격 DB·컨테이너·서비스 endpoint나 R2DBC/coroutine API를 추가하는 경우에는 이
계획을 중단하고 범위를 다시 승인받아야 한다.

## 위험 목록

| 위험 | signal | 완화 | rollback/rerun |
|---|---|---|---|
| provider 버전이 중앙 BOM과 다르게 해석됨 | `dependencyInsight`가 `0.3.0` 이외를 선택하거나 alias를 찾지 못함 | versionless catalog alias만 사용하고 provider 좌표·BOM을 dependencyInsight로 확인 | build file/catalog 변경을 되돌리고 `projects`·dependencyInsight 재실행 |
| 업무 행과 감사 행의 원자성 붕괴 | 예외 transaction 뒤 `Customers`, `javers_commit`, `javers_snapshot` 중 하나라도 남음 | 같은 Exposed `transaction`에서 hook을 실행하고 rollback row-count 테스트를 먼저 통과 | 해당 fixture와 subscription을 격리한 뒤 targeted test 재실행; provider를 우회하는 workaround는 금지 |
| 전역 EntityHook 또는 문맥 누수 | 테스트 순서 변경 시 이전 actor/requestId가 사용되거나 close 후 새 audit이 생성됨 | 고유 H2/repository/JaVers fixture, nested/exception `ThreadLocal` 복원, `try/finally`/`.use`, idempotent `close()` | 실패 테스트만 먼저 재현하고 fixture 수명과 cleanup을 수정한 뒤 모듈 전체 test 재실행 |
| 민감 필드 저장 payload 노출 | `CdoSnapshotTable.state` 또는 `changedProperties`에 `secret` 문자열/key가 발견됨 | detached `AuditedCustomer` allow-list와 초기/secret-only update raw-column 검사 | mapper를 수정하고 sensitive test·module test를 처음부터 재실행 |
| provider head 캐시와 schema 재생성 충돌 | 테스트 간 commit sequence가 재사용되거나 새 Javers가 과거 head를 참조함 | 테스트마다 고유 H2 database와 새 repository/JaVers를 만들고 shared repository를 금지 | schema fixture를 재생성하고 모듈 테스트를 순차 재실행 |
| 문서/CI가 실제 모듈을 놓침 | README locale mismatch, asset pair 누락, selector가 새 task를 출력하지 않음 | source-equivalent README·12개 SVG/PNG pair·Examples task·Nightly H2 implicit coverage를 fresh head에서 검사 | 문서/index/selector만 수정하고 asset·writer·actionlint 검사를 재실행 |

## 명시적 N/A

- 처리량 개선이나 benchmark acceptance가 없는 교육용 cold path이므로 별도
  benchmark는 실행하지 않는다.
- API가 동기 JDBC `transaction {}`뿐이므로 coroutine cancellation, dispatcher,
  streaming/backpressure 검증은 해당하지 않는다.
- 승인된 one-developer 범위가 H2 deterministic test이므로 Testcontainers,
  PostgreSQL/MySQL 원격 smoke, 운영 rollout/health endpoint를 추가하지 않는다.

## 구현 전 판정

- P0: 0
- P1: 0 (각 signal에 계약 테스트와 rerun 지점이 배정됨)
- 상태: **PASS — TDD RED 단계로 진행 가능**
