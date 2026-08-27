# 이슈 #238 Exposed measured 구현 계획 리뷰

## 검토 범위와 근거

- 대상: `docs/superpowers/plans/2026-08-27-issue-238-exposed-measured-plan.md`
- 승인된 설계: `docs/superpowers/specs/2026-08-27-issue-238-exposed-measured-design.md`
- 설계 리뷰: `docs/superpowers/reviews/2026-08-27-issue-238-exposed-measured-spec-review.md`
- 이슈: [#238](https://github.com/bluetape4k/exposed-workshop/issues/238)의 live
  DoD, milestone `1.4.0`, labels, assignee `debop`
- local 근거: `06-advanced/05-exposed-money`, `settings.gradle.kts`,
  `gradle/libs.versions.toml`, `.github/workflows/ci.yml`,
  `.github/workflows/nightly.yml`, `.github/scripts/select-changed-examples.sh`
- provider 근거: `MeasuredColumnTypes.kt`, `Units.kt`, `Temperature.kt`의
  공개 DSL/`DOUBLE`/기준 단위/지원되지 않는 DB 타입 계약
- 검토 방식: single-developer lane에서 여섯 관점을 한 세션에서 순차 적용했다.
  계획·리뷰 write scope는 leader가 소유하며 독립 subagent는 사용하지 않았다.

## 요구사항 추적성 통합

| Issue 요구사항/DoD | 계획 task | 구체적인 증거 |
|---|---|---|
| measured alias와 신규 `06-advanced` 모듈 | 1, 5 | `gradle/libs.versions.toml`, `13-exposed-measured`, `projects`, dependency insight |
| 길이·질량·온도 3계열 base-unit round-trip | 2, 3, 7 | `MeasuredData.kt`, `Ex01MeasuredColumns.kt`, H2/PG/MySQL/MariaDB 결과 |
| DSL 및 DAO/Entity 조회 | 2, 3, 4 | 같은 `ProductTable`을 통한 row와 entity read-back, README 코드 |
| null·소수 정밀도·단위 변환·부적합 입력 | 2, 3, 4, 7 | `nullable()`, `shouldBeNear`, provider source contract 문서, compile-time 문서 예 |
| base unit 저장과 migration 주의 | 3, 4, 6 | meter/kg/K source declaration, README와 ERD |
| EN/KO README와 SVG/PNG ERD/architecture | 4, 6 | source-equivalent 섹션, four SVG/four PNG, semantic/asset audits |
| 기존 schema/custom column/R2DBC 제외 | 1, 3, 4, 8 | 신규 격리 테이블, changed-path audit, README 범위 안내 |
| module test와 static check | 1, 3, 5, 7 | module test/build/detekt, root detekt, CI/nightly registration |

## 여섯 렌즈 판정

| 우선순위 | 영역 | 근거 | 필요한 plan edit |
|---|---|---|---|
| P2 | Performance | 한 상품 row의 JDBC 왕복과 provider의 단일 `DOUBLE` 변환만 추가한다. batch/benchmark/반복 polling은 없다. | production benchmark를 도입하지 않고 `shouldBeNear` 및 요청당 새 변환/버퍼를 만들지 않는 소스 검사를 유지한다. |
| P2 | Stability | `withTables`와 기존 shared lock을 사용하며, cross-dialect 검사는 H2 fast → real DB 순서로 직렬화한다. `valueFromDB`의 unsupported type은 provider 경계에서 실패한다. | `maxParallelUsages = 1`, test resources, Testcontainers 순차 실행, 실패 시 단계 7 재실행을 명시했다. |
| P2 | Security | 측정값은 정적 테스트 fixture이며 SQL 문자열 조합, credential, 외부 endpoint, 역직렬화 입력이 없다. | 새 dependency/운영 endpoint를 추가하지 않고 단위 문자열을 DB에 저장하지 않는 결정을 유지한다. |
| P2 | Operator/Ops | `DOUBLE` 기준 단위 변경은 데이터 변환 migration을 요구하고 정확도 정책은 범위 밖이다. CI/nightly에서 신규 module task를 명시한다. | README/ERD migration 주의와 nightly shard 등록, actionlint 검증을 task 4–7에 고정했다. |
| P2 | Developer/API | `org.jetbrains.exposed.v1.*`, `IntIdTable`/`IntEntity`, provider DSL, BOM alias가 현재 repository 패턴과 일치한다. | receiver shadowing/import alias, compile-time generic boundary, direct version override 금지를 task 2–3에 명시했다. |
| P2 | User/caller | 표시 단위 입력과 m/kg/K 저장을 같은 예제에서 비교하고 nullable/unsupported 동작을 설명한다. | EN/KO README 동일 순서, migration/`DOUBLE` 한계/R2DBC 별도 범위를 task 4에 고정했다. |

## Step 3-R 필수 점검

1. 모든 spec 수용 기준과 issue DoD가 위 traceability 표의 구체 task/파일/명령에
   매핑된다.
2. 의존성 등록 → RED fixture/test → GREEN provider DSL/DAO → 문서 → CI/diagram →
   최종 검증 순서이며, 후속 산출물을 앞선 task에서 참조하지 않는다.
3. 성공, 실패, edge(nullable/large/decimal), lifecycle/cleanup, backend capability
   (H2 및 JDBC dialect) 테스트가 포함된다. coroutine/cancellation/concurrency는
   이 JDBC 동기 예제에 해당하지 않으며, shared test 직렬성만 검증한다.
4. 검증 명령은 module-scoped Gradle, root detekt, actionlint, writer/diagram
   audits, `git diff --check`로 구체화되어 있다.
5. 새 모듈의 settings auto-discovery, BOM alias, CI/nightly task, test resources,
   README locale, Korean KDoc, rollback/rerun 지점이 모두 계획에 있다.
6. `.github/workflows/examples.yml`를 확장하지 않는 이유를 현재 fixed weekly
   scope와 full CI/nightly coverage로 설명했으며, 이는 issue 범위를 넓히지 않는다.

## 통합 판정

- P0: **0**
- P1: **0**
- P2: 위 사항은 이미 계획에 반영되어 별도 차단 없이 수용한다.
- 모순 없음: provider는 `DOUBLE`을 사용하므로 issue의 `precision/scale`은
  근사 정밀도/유한 범위로 검증하고 DECIMAL 정책은 추가하지 않는다. provider
  invalid DB type 경계는 source contract 문서로 남기고 정상 JDBC round-trip에
  인위적인 driver 출력 주입 테스트를 추가하지 않는다.
- single-developer 판단: 별도 reviewer/subagent lane은 `N/A (single-developer
  lane)`이며, leader가 모든 렌즈와 integration을 수행한다. 이는 CI, final review,
  lesson 또는 live PR 검증을 생략하는 근거가 아니다.
- 열린 결정: 없음. 사용자가 승인한 선택지 A와 명세를 그대로 실행한다.

## SPW writer gate

- [x] **SPW-01** — 독자·목표·범위·issue/provider 근거를 계획에 명시했다.
- [x] **SPW-02** — ordered task, file impact, tests, failure/risk, docs/diagram,
  CI/nightly, rollback/rerun, traceability를 포함했다.
- [x] **SPW-03** — 한국어 work document 문체와 code/API/identifier/command/URL/
  version 보존을 확인했다.
- [x] **SPW-04** — local module/shared test/catalog와 provider source를 대조하고
  새 abstraction/dependency를 추가하지 않는 이유를 기록했다.
- [x] **SPW-05** — `git diff --check`, terminology audit, 여섯 렌즈와 main
  integration을 수행했으며 P0/P1=0이다.

## 최종 상태

`PASS` — implementation plan은 승인된 spec과 issue #238 DoD를 모두 추적하며,
실행 순서·검증 명령·single-developer 경계·rollback/rerun 조건이 명확하다.
사용자의 plan approval 후 A-05 risk prediction과 TDD 구현으로 진행할 수 있다.
