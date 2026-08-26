# 이슈 #236 JDBC 전용 설계 리뷰

## 검토 범위와 근거

- 대상: `docs/superpowers/specs/2026-08-27-issue-236-checkpointable-jdbc-batch-design.md`
- 근거: 이슈 #236 및 R2DBC target issue #205, 중앙 catalog `1.4.0`, provider `bluetape4k-exposed-batch:1.12.1`, provider JDBC source/test
- 방식: 단일 개발자 제약에 따라 Performance, Stability, Security, Operator/Ops, Developer/API, User/Caller 여섯 렌즈를 같은 세션에서 독립적으로 순차 검토

## 렌즈별 판정

| 우선순위 | 렌즈 | 근거 | 필요한 수정 | 재검토 |
|---|---|---|---|---|
| P2 | Performance | keyset reader와 chunk writer 경계는 provider의 `Dispatchers.VT` 경로를 재사용하지만 workshop에는 대규모 benchmark가 없다 | production benchmark를 이 이슈의 범위에 추가하지 않고 README에 H2 학습 경계와 PostgreSQL opt-in을 명시 | Performance 재확인: 수용 |
| P2 | Stability | provider의 `write → onChunkCommitted → saveCheckpoint`, retry, timeout, `STOPPED` 계약이 설계와 테스트 목록에 모두 있다 | cancellation/restart 테스트에서 metadata checkpoint와 terminal status를 직접 read-back | Stability 재확인: 계획 Task 5에 반영 |
| P2 | Security | 기본 실행은 H2이며 credentials/network가 없고 checkpoint serializer가 `CheckpointJson.jackson3()`로 고정된다 | 외부 입력/인증을 새로 도입하지 않으며 at-least-once와 exactly-once 제외를 문서화 | Security 재확인: 수용 |
| P2 | Operator/Ops | 배포 가능한 서비스가 아니라 workshop module이므로 health/readiness/runbook은 범위 밖이다 | opt-in PostgreSQL, CI 기본 H2, 실패 시 재실행 경계를 README와 위험 표에 유지 | Operator/Ops 재확인: 수용 |
| P1 | Developer/API | 설계의 public default symbol이 구현 계획의 실제 이름과 달랐다 | `defaultProcessor`/`exposedJdbcTargetWriter`를 `defaultJdbcProcessor`/`jdbcTargetWriter`로 통일 | Developer/API 재확인: 통과 |
| P2 | User/Caller | 주입 가능한 processor/writer와 restart·skip·retry·timeout·cancel 예제가 학습자 계약을 설명한다 | R2DBC를 sibling API로 추상화하지 않고 target issue 링크를 유지 | User/Caller 재확인: 수용 |

## 통합 판정

- P0: 0
- P1: 0 (설계 API 이름을 계획과 일치시켜 수정 완료)
- P2: benchmark/운영 기능은 workshop 범위 밖으로 명시했고, 안정성 read-back과 문서 경계는 구현 계획에 반영했다.
- 모순: JDBC 전용 범위와 R2DBC target issue #205가 모든 설계·수용 기준에서 일치한다.
- 미해결 사용자 결정: 없음. 사용자가 JDBC만 이 저장소에서 구현하고 R2DBC를 별도 저장소 이슈로 등록하도록 범위를 확정했다.

## SPW writer gate

- [x] SPW-01 — 대상 독자와 JDBC-only 목적, issue/provider/catalog 근거를 명시했다.
- [x] SPW-02 — 선택지, 경계, API, 실패 모드, 호환성, acceptance를 포함했다.
- [x] SPW-03 — 한국어 기술 문체와 API/identifier/command/URL/version 보존을 확인했다.
- [x] SPW-04 — provider source/test, local catalog, chapter 13 workflow와 대조했다.
- [x] SPW-05 — Markdown 구조, 링크, 용어 감사를 재실행했다.

## 최종 상태

`PASS` — P0/P1 차단 항목 없음. 계획 리뷰와 구현 게이트로 진행한다.
