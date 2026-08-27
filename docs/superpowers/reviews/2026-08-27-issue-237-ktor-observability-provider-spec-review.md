# 이슈 #237 Ktor observability provider 설계 리뷰

## 검토 범위와 근거

- 대상: `docs/superpowers/specs/2026-08-27-issue-237-ktor-observability-provider-design.md`
- 이슈: [#237](https://github.com/bluetape4k/exposed-workshop/issues/237)의 live
  title, body, assignee, milestone, labels
- local 근거: `12-production-integration/10-ktor-observability-readiness`의
  `KtorPlugins.kt`, route/application test, `build.gradle.kts`,
  `gradle/libs.versions.toml`
- provider 근거: `bluetape4k-projects` tag `1.12.1`의
  `Bluetape4kKtorObservability`, `CorrelationIdSettings`, `KtorCallIdSupport`,
  `CallLoggingSettings`, `KtorCallLoggingSupport`와 공식 문서
- 검토 방식: 사용자 요청의 single-developer 제약에 따라 여섯 렌즈를 한 세션에서
  순차적으로 적용했다. 독립 write subagent는 사용하지 않았으며, 각 렌즈의
  evidence와 필요한 수정 여부를 이 문서에 분리해 기록했다.

## 렌즈별 판정

| 우선순위 | 렌즈 | 근거 | 필요한 수정 | 재검토 |
|---|---|---|---|---|
| P2 | Performance | provider CallLogging은 모든 일반 요청을 포맷하고 `/readyz` 등 기본 health 경로만 제외한다. workshop은 H2 기반 학습 모듈이며 production throughput 목표나 external telemetry는 이슈 범위에 없다. | benchmark나 운영 backend를 추가하지 않고, plan과 README에 local deterministic 학습 경계와 telemetry 기본 비활성을 유지한다. | 수용 — 범위 밖 성능 위험이며 문서 경계가 명시됨 |
| P2 | Stability | `installBluetape4kKtorObservability`가 CallId와 CallLogging을 한 번 설치하고 provider artifact가 Ktor plugin API를 `api`로 노출한다. 로컬 `StatusPages`가 `call.callId`를 계속 읽으므로 installer 순서와 dependency resolution을 compile/test로 확인해야 한다. | provider installer를 단일 호출로 고정하고, direct call-id/call-logging dependency 제거 후 module compile/test와 dependency insight를 실행한다. | 수용 — 구현 plan의 dependency/compile gate에 반영 |
| P2 | Stability | 현재 `StatusPages`는 `CancellationException`을 일반 `Exception` 처리에서 재전파한다. provider 전환이 이 분기를 바꾸면 취소가 500 JSON으로 삼켜질 수 있다. | cancellation route test를 유지하고 일반 오류·validation 오류와 별도 assertion으로 재전파를 검증한다. | 수용 — spec 테스트 설계와 수용 기준에 포함 |
| P2 | Security | provider는 raw header를 trim·문자 필터·최대 길이 제한 후 MDC/response에 사용한다. 앱 override 120은 provider 허용 상한 256 안의 bounded 값이며, 새 credential·외부 endpoint·운영 네트워크를 추가하지 않는다. | control character, 특수문자, 120자 초과 입력이 raw로 반향되지 않는지 route read-back으로 확인하고 secrets를 문서/fixture에 넣지 않는다. | 수용 — 구현 테스트와 README 경계에 반영 |
| P2 | Operator/Ops | registry/tracing을 config에 전달하지 않으면 provider 선택 기능은 설치되지 않는다. CallLogging 기본 제외 경로가 `/readyz`를 포함하므로 readiness scrape noise는 줄지만, 실제 metrics/tracing backend가 있는 것처럼 설명하면 안 된다. | README 양쪽에 기본 비활성·opt-in 확장 지점과 production 운영 대상 아님을 명시한다. Prometheus route는 이번 변경에서 설치하지 않는다. | 수용 — 문서 task와 범위 경계에 반영 |
| P2 | Developer/API | 기존 local sanitizer는 invalid 전체를 거부했지만 provider는 허용 문자만 필터링하고 blank일 때 생성한다. 또한 응답 propagation은 provider default가 true여도 앱의 기존 계약이므로 implicit default에만 의존하면 의도가 흐려진다. | spec에 provider sanitization으로의 의도적 계약 이동과 `propagateResponseHeader = true`를 명시했고, raw 비반향·정제 값·response header·MDC log capture를 테스트한다. | 통과 — 설계 자체검토에서 수정 완료 |
| P2 | User/Caller | caller가 `:` 또는 공백을 보낸 경우 이전 UUID fallback 대신 정제된 ID를 받을 수 있다. 이는 provider를 권위로 채택한 의도적 변화이며 route/error JSON의 `requestId`와 기존 endpoint/status 계약은 유지된다. | README와 테스트에서 허용 문자(`A-Za-z0-9._-`), 최대 길이 120, provider 생성 ID, 기존 route/error 계약을 같은 순서로 설명한다. | 수용 — 문서 parity 및 route regression gate에 반영 |

## 통합 판정

- P0: **0**
- P1: **0**
- P2: 위 위험은 모두 provider 계약, deterministic test, README 범위 고정으로
  수용 가능하다. 새로운 운영 dependency나 R2DBC 범위 확장은 없다.
- 모순 점검: issue가 말하는 central alias는 `bluetape4k-dependencies:1.4.0` BOM의
  version authority이고, local `gradle/libs.versions.toml`에는 consumer alias를
  추가해야 한다. spec은 두 책임을 구분한다. `1.12.1` provider source와 local
  `KtorPlugins.kt`의 `callId`/cancellation 계약도 일치한다.
- 범위 점검: provider installer 전환, JDBC persistence 보존, JSON/StatusPages/
  readiness 유지, diagrams/README 갱신만 포함한다. R2DBC·Redis·JaVers·backend
  운영 설정은 제외한다.
- single-developer 점검: parallel subagent review는 `N/A (single-developer
  lane)`로 기록하며, 여섯 렌즈와 최종 integration은 leader가 순차 수행한다.
- 미해결 사용자 결정: 없음. 선택지 A와 명시된 provider sanitization 변화는 설계
  승인으로 결정되었다.

## SPW writer gate

- [x] **SPW-01** — spec 대상 독자·목표·JDBC-only 경계와 issue/provider/catalog
  근거를 확인했다.
- [x] **SPW-02** — 책임 경계, 선택지, 실패 모드, 호환성·rollback, 테스트,
  문서·다이어그램, 수용 기준의 누락을 여섯 렌즈로 점검했다.
- [x] **SPW-03** — 한국어 work document 문체와 code/API/identifier/command/URL/
  version 보존을 확인했다.
- [x] **SPW-04** — local implementation/test/catalog와 provider `1.12.1` source/
  공식 문서의 installer·sanitization·logging 기본값을 대조했다.
- [x] **SPW-05** — `git diff --check`와 terminology audit를 실행했고, 표·링크·
  heading 구조 및 한국어 용어를 확인했다.

## 최종 상태

`PASS` — P0/P1 차단 항목이 없고, 보강 사항은 설계 명세에 반영했다. 작성된
설계 문서는 사용자 review gate를 통과한 뒤 Step 3 implementation plan으로
진행할 수 있다.
