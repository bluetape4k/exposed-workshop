# 이슈 #236 JDBC 전용 구현 계획 리뷰

## 검토 범위

- 대상: `docs/superpowers/plans/2026-08-27-issue-236-checkpointable-jdbc-batch-plan.md`
- 기준: JDBC-only 설계, provider public API, repository `AGENTS.md`, `bluetape-workflow` Type A gates, `bluetape-kotlin-patterns`
- 방식: 단일 개발자 실행을 보존하면서 여섯 렌즈를 독립적으로 순차 검토하고 통합했다.

## 필수 항목 검토

| 우선순위 | 영역 | 판정 및 근거 | 계획 반영 |
|---|---|---|---|
| P2 | 요구사항/DoD 매핑 | module, API, H2 success, restart, skip/retry/timeout/cancel, docs parity, CI selection, receipt가 Tasks 1–8에 매핑된다 | 수용 |
| P2 | 순서/의존성 | catalog/module discovery → RED test → source contract → GREEN harness → failure tests → docs/assets → README/CI registration → full verification 순서가 implementable하다 | 수용 |
| P2 | 테스트 범위 | success/failure/edge/coroutine/lifecycle/backend 기본 경로를 H2 deterministic 테스트로 고정하고 PostgreSQL은 opt-in 경계로 남긴다 | 수용, H2 한정 명시 |
| P2 | Kotlin/Exposed 패턴 | `org.jetbrains.exposed.v1.*`, `Dispatchers.VT` provider 경로, receiver-shadowing 검사, deprecated import 검사를 Task 8에 명시했다 | 반영 완료 |
| P2 | 안정성/리소스 | provider의 reader/writer `finally` close, cancellation `STOPPED`, timeout margin, unique H2 names와 rerun 규칙을 Task 5/Step 3-P에 둔다 | 반영 완료 |
| P2 | 보안/운영 | credential/network 없는 H2 기본값, no new dependency, PostgreSQL opt-in, rollback/rerun 위험과 신호를 Step 3-P에 둔다 | 수용 |
| P2 | 문서/공개 API | EN/KO README, Korean KDoc, PNG embed + SVG source, issue #205 exclusion/link를 Task 6에 둔다 | 수용 |
| P2 | module/workflow | auto-discovery, catalog alias, test resources, Kover report, fixed Examples task와 dynamic path mapping을 Tasks 1/7/8에 둔다 | 반영 완료 |
| P2 | 범위/중복 | R2DBC module/alias/adapter를 추가하지 않고 target repository issue #205로 연결한다 | 수용 |

## 정리 및 수정

- path-selection 예시의 잘못된 pipe를 `./.github/scripts/select-changed-examples.sh HEAD~1..HEAD`로 교정했다.
- timeout 테스트는 source `1..3`, `chunkSize = 3` 한 chunk로 고정해 `skipCount == 3` 기대값과 일치시켰다.
- repository 실제 catalog의 Kotlin `2.4.0`을 Tech Stack에 반영했다(AGENTS의 오래된 서술과 구분).
- 계획에 provider API/BOM drift, checkpoint boundary, cancellation, timeout variance, docs/CI drift의 signal·mitigation·rollback/rerun 표를 추가했다.

## SPW writer gate

- [x] SPW-01 — 계획 독자, JDBC-only 목적, 근거와 제외 범위를 명시했다.
- [x] SPW-02 — 파일별 작업, 정확한 명령, acceptance, rollback/rerun을 포함했다.
- [x] SPW-03 — Korean work-document 규칙과 source/API/command 보존을 확인했다.
- [x] SPW-04 — provider/local repo/Gradle/CI 구조와 계획 순서를 대조했다.
- [x] SPW-05 — 계획 Markdown 구조와 Korean terminology audit를 재실행했다.

## 통합 판정

- P0: 0
- P1: 0
- P2: 모두 수용 또는 계획에 반영 완료
- 최종 상태: `PASS` — 구현 및 verification gates로 진행 가능
