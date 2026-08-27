# 이슈 #239 JaVers + Exposed 구현 계획 리뷰

## 검토 범위와 근거

- 설계: `docs/superpowers/specs/2026-08-27-issue-239-javers-exposed-design.md`
- 계획: `docs/superpowers/plans/2026-08-27-issue-239-javers-exposed-plan.md`
- 이슈: [#239](https://github.com/bluetape4k/exposed-workshop/issues/239)의 live
  DoD, milestone `1.4.0`, labels, assignee `debop`
- local 근거: `settings.gradle.kts`, `gradle/libs.versions.toml`,
  `.github/scripts/select-changed-examples.sh`,
  `.github/workflows/examples.yml`, `.github/workflows/nightly.yml`,
  `13-ecosystem-integrations/10-druid-query-only`,
  `13-ecosystem-integrations/11-checkpointable-batch`
- provider 근거: `ExposedCdoSnapshotRepository`,
  `ExposedJaversEntityHookMapping`, `ExposedJaversEntityHookSubscription`,
  `JaversExposedTables`, H2 hook/repository tests
- 검토 방식: Performance, Stability, Security, Operator/Ops,
  Developer/API, User/caller 여섯 관점을 독립 lane으로 나누어 검토한 뒤
  leader가 요구사항 추적성과 증적을 통합했다. 단일 개발자 범위에 맞춰
  benchmark·원격 DB·컨테이너는 N/A로 판정하되, 해당 근거를 계획과 최종
  verification에 남긴다.

## 초기 검토 finding과 수정

| 우선순위 | 관점 | 근거 | 수정 |
|---|---|---|---|
| P1 | Stability | provider repository의 head 캐시와 `@BeforeEach` schema 재생성을 공유 fixture로 사용하면 테스트 간 상태가 남을 수 있음 | 계획 2단계에서 테스트마다 고유 H2 database·repository·JaVers를 만들고 재사용하지 않도록 고정 |
| P1 | Security | provider는 전체 encoded `state`와 `changedProperties`를 저장하므로 accessor 조회만으로 민감 값 제외를 증명할 수 없음 | 계획 2단계에서 초기 생성과 `secret` 단독 변경 후 두 저장 컬럼의 모든 행을 직접 검사 |
| P1 | Stability/Security | 문맥 부재 callback 예외가 업무 행과 감사 행의 원자적 실패로 검증되지 않음 | 계획 2단계에서 문맥 없는 DAO 변경 시 세 종류 행이 모두 0건인지 검증 |
| P1 | Stability | `ThreadLocal` 중첩/예외 종료와 전역 hook cleanup을 명시하지 않으면 thread reuse 오염 가능 | 계획 2단계와 3단계에 중첩 복원·최상위 제거·예외 cleanup, `try/finally`/`.use`, idempotent `close()` 테스트 추가 |
| P2 | Performance/API | history 조회가 이력 전체를 decode하므로 운영 규모에서 무제한 조회 위험 | 계획·KDoc·README에 교육용 무제한 조회이며 production pagination/retention 대체가 아님을 명시 |
| P2 | Operator/Ops | 별도 Nightly module row가 없어도 H2 전체 `test`가 신규 project를 포함한다는 증적이 필요 | 계획 6·7단계에 Nightly H2 full `test`, root CI 전체 test, `dependencyInsight`, Kover XML, `actionlint`, selector 출력 검증 추가 |
| P2 | Operator/Ops | `ensureSchema()`의 DDL 편의 기능을 운영 migration과 혼동할 위험 | 설계·계획·README에 외부 migration 소유 및 `createSchemaOnEnsure=false` 경계 추가 |
| P3 | Scope | catalog/plugin Kotlin 버전과 language/API 버전을 단일 숫자로 표현하면 부정확 | 계획 Tech Stack을 language/API 2.3, catalog/plugin 2.4.0으로 정정 |

모든 P1 finding은 설계의 EntityHook/JDBC-H2 선택을 바꾸지 않고 fixture,
payload 검증, fail-closed 계약을 구체화하는 방식으로 수정했다. 따라서 사용자에게
새로운 아키텍처 승인을 요구할 정도의 범위 변경은 없다.

## 요구사항 추적성

| 이슈 요구사항/DoD | 계획 task | 검증 증적 |
|---|---|---|
| JaVers BOM/module alias와 신규 integration module | 1, 6 | catalog alias, `projects`, `dependencyInsight`, module build |
| Exposed/JaVers schema 결정론적 초기화 | 2, 3, 4 | 고유 H2 fixture, `ensureSchema()` 반복 호출, schema row 확인 |
| create/update metadata·diff/history | 2, 3, 4 | actor/requestId/changeType, `findSnapshots`, `findChanges`, diff 단언 |
| 성공 commit 이후 관찰·rollback 원자성 | 2, 4, 7 | commit 밖 read-back, 업무·`javers_commit`·`javers_snapshot` row count |
| 중복 commit 정책 | 2, 3 | 값 불변 재저장 후 새 감사 기준 데이터와 성공 이력 부재 |
| 민감 필드 제외 | 2, 3, 5, 7 | DTO allow-list, property/`state`/`changedProperties` 직접 조회 |
| subscription/context lifecycle | 2, 3, 5 | 중첩 context 복원, 최상위 제거, `close()` idempotence와 scoped cleanup |
| EN/KO README와 SVG/PNG sequence/ERD | 5, 7 | source-equivalent locale, 12개 SVG/PNG pair, asset/audit 결과 |
| JDBC-only/R2DBC 분리·one-developer 범위 | 1, 5, 6, 7 | H2-only 근거, README 경계, Nightly/Examples 선택 기록 |
| module test/static check | 1, 4, 6, 7 | test, detekt, build, Kover, `git diff --check`, `actionlint` |

## 여섯 관점 통합 판정

| 우선순위 | 영역 | 판정 및 근거 |
|---|---|---|
| P2 | Performance | 감사 기준 데이터는 예제의 JDBC cold path이며 처리량 목표·benchmark module이 없다. 무제한 history 위험은 문서 경계로 고정하고 production pagination은 범위 밖으로 둔다. |
| P2 | Stability | 고유 fixture, 전역 hook의 `try/finally` cleanup, nested/exception context, rollback row-count, idempotent `close()`를 계획에 반영했다. coroutine cancellation/streaming은 동기 JDBC scope라 N/A다. |
| P2 | Security | detached DTO allow-list와 raw encoded payload 직접 검증, 문맥 부재 fail-closed, 인증/인가 미구현과 actor 공급 책임 문서화를 포함한다. 네트워크/credential/외부 endpoint는 없어 N/A다. |
| P2 | Operator/Ops | pure workshop module이므로 health/readiness/runbook과 remote rollout은 N/A다. `ensureSchema()` migration 경계, Nightly implicit H2 coverage, Kover·dependency·workflow 증적은 포함한다. |
| P2 | Developer/API | BOM versionless alias, Exposed v1 import, DAO EntityHook provider signature, Korean KDoc, ordered module→test→implementation→docs→CI tasks가 현재 repository pattern과 맞는다. |
| P2 | User/caller | README는 actor를 인증된 caller identity에서 공급해야 함, subscription 종료, 민감 값 제외, rollback이 원장 복원이 아님, 무제한 조회의 교육용 한계를 사용 예와 함께 설명한다. |

## 필수 점검

1. 모든 설계 성공 기준과 이슈 DoD가 위 추적성 표의 구체 task·파일·명령에
   매핑된다.
2. alias/module discovery → contract test → 최소 구현 → 문서/asset → CI/index
   → fresh verification 순서이며 후속 산출물을 선행 task에서 요구하지 않는다.
3. 성공, 실패, edge(불변 재커밋·민감 단독 변경·문맥 부재), lifecycle, rollback,
   backend capability(H2 JDBC)를 포함한다. coroutine/streaming/remote DB는
   해당 없는 범위로 명시적인 N/A 근거를 가진다.
4. `dependencyInsight`, `koverXmlReport`, `actionlint`, changed-example selector,
   asset audit와 `git diff --check`가 계획의 실행 가능한 검증 명령이다.
5. 새 모듈의 dynamic settings include, BOM alias, Examples task, H2 Nightly
   implicit coverage, test resources, KDoc, README locale, diagram pair를 모두
   계획에 포함했다.

## SPW writer gate

- [x] **SPW-01** — 독자·목표·범위·issue/provider 근거를 설계와 계획에 명시했다.
- [x] **SPW-02** — ordered task, file impact, tests, failure/risk, docs/diagram,
  CI/Nightly, rollback/rerun, traceability를 포함했다.
- [x] **SPW-03** — 한국어 work document 문체와 code/API/identifier/command/URL/
  version 보존을 확인했다.
- [x] **SPW-04** — local module/catalog/CI와 provider source를 대조하고 새
  abstraction·version override·원격 의존성을 추가하지 않는 이유를 기록했다.
- [x] **SPW-05** — `git diff --check`와
  `audit-korean-terms.mjs`를 설계·계획에 실행했고, 여섯 관점과 main
  integration에서 P0/P1을 수렴했다.

## 최종 상태

- P0: **0**
- P1: **0** (위 수정으로 해소)
- P2: 교육용 무제한 조회, provider global hook 동시 close, benchmark/remote
  service N/A 등의 근거를 문서·검증 task에 고정
- 열린 사용자 결정: 없음. 기존 승인된 EntityHook/JDBC-H2 선택을 그대로 실행

**PASS** — 계획은 승인된 설계와 #239 DoD를 모두 추적하며, fixture/보안/원자성
위험을 구체적인 테스트와 검증 명령으로 잠갔다. 다음 게이트는 risk prediction 후
TDD RED 구현이다.
