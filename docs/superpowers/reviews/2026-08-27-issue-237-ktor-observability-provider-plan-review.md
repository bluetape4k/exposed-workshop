# Issue #237 Ktor observability provider 전환 계획 검토

## 검토 범위와 실행 방식

- 대상: `docs/superpowers/specs/2026-08-27-issue-237-ktor-observability-provider-design.md`와 승인된 구현 계획
  `docs/superpowers/plans/2026-08-27-issue-237-ktor-observability-provider-plan.md`
- 기준: Step 3-R 계획 검토 체크리스트와 성능·안정성·보안·운영·API·호출자 여섯 관점
- 실행: 단일 개발자 lane에서 동일한 순서로 여섯 관점을 독립 검토했다. 사용자가 명시한 1인 개발자 제약에 따라 native subagent 병렬 검토는 실행하지 않았다.
- 판정 규칙: P0/P1은 구현 전 차단, P2는 계획·문서·검증으로 완화, P3는 후속 개선으로 기록한다.

## 계획-명세 추적

| 명세/DoD | 계획 작업 | 검증 근거 |
|---|---|---|
| provider alias와 중앙 installer 사용 | Task 1, Task 3 | catalog alias, dependencyInsight, 중복 설치 `rg`, focused test |
| 유효/무효/누락 correlation ID와 응답 전파 | Task 2, Task 3, Task 5 | RED→GREEN route test, provider config, README 계약 |
| 구조화 400/500 오류와 cancellation rethrow | Task 2, Task 3 | provider test와 기존 route regression |
| optional metrics/tracing은 기본 비활성 | Task 2, Task 4, Task 5 | config default assertion, stability scan, README |
| JDBC 예제만 구현하고 R2DBC 제외 | File impact map, Task 5, Task 7 | diff 범위·issue link·PR body 재확인 |
| 한영 README와 architecture/sequence SVG·PNG | Task 5, Task 6 | parity, semantic/geometry/visual/asset-pair audits |
| 모듈 test/build/detekt와 workflow DoD | Task 4, Task 7 | 명령 출력, evidence receipt, completion-check |

## 여섯 관점 검토

### 1. 성능·안정성

- **P0:** 없음. provider installer는 애플리케이션 시작 시 한 번 호출하고, 요청마다 registry·tracing 객체를 생성하는 설계를 계획하지 않는다.
- **P1:** 없음. optional Micrometer/OpenTelemetry를 `null` 기본값으로 두고, CallId→CallLogging→ContentNegotiation/StatusPages 순서를 고정했다.
- **P2:** benchmark는 계획 범위에 없다. Task 4에서 요청당 할당·plugin 순서·기본 telemetry 비활성·cancellation 전파를 소스와 테스트로 확인하고 `미실행` 사유를 기록한다.
- **P3:** 실제 telemetry backend를 연결한 부하 수치는 별도 issue로 남길 수 있으나 이번 구현의 acceptance blocker가 아니다.

### 2. 보안·데이터 경계

- **P0:** 없음. R2DBC나 다른 repository를 수정하지 않고 모듈 범위를 JDBC/H2로 고정한다.
- **P1:** 없음. provider sanitization은 허용 문자 필터, trim, 최대 길이 120으로 bounded input을 보장하며, 구조화 오류에는 애플리케이션이 만든 메시지 계약만 노출한다.
- **P2:** provider 필터링으로 콜론·공백이 제거되는 behavioral change를 Task 2/5에서 명시적으로 검증·문서화한다. 인증/인가·비밀정보 로깅은 이 예제의 범위가 아니다.
- **P3:** 외부 header spoofing 정책을 더 제한해야 하면 provider 설정 변경으로 후속 issue에서 다룬다.

### 3. 운영·관측성

- **P0:** 없음. `/readyz`는 기존 StatusPages와 독립적으로 유지되며 변경 범위에 포함된다.
- **P1:** 없음. CallLogging은 provider가 소유하고 `X-Request-ID`를 response header로 전파한다. generic exception은 로그 후 500 응답, cancellation은 rethrow한다.
- **P2:** metrics/tracing route나 exporter는 기본 설치하지 않는다. README와 stability review에 명시하고 dependencyInsight로 transitives를 확인한다.
- **P3:** 운영 backend별 exporter 예시는 이 issue에서 추가하지 않으며 provider 문서/별도 issue의 책임으로 남긴다.

### 4. API·개발자 경험

- **P0:** 없음. 변경은 version catalog alias와 기존 `installKtorPlugins()` 내부의 단일 installer 호출로 제한된다.
- **P1:** 없음. `CorrelationIdSettings`의 header 이름, MDC key, max length, propagation을 코드에서 명시하고 기존 `call.callId` 기반 ErrorResponse 계약을 보존한다.
- **P2:** generated ID가 UUID에서 16자 Base58로 바뀌므로 focused test와 한영 README에 형식과 예시를 함께 기록한다. provider artifact가 BOM에서 해석되지 않으면 임의 버전을 추가하지 않고 build convention을 재확인한다.
- **P3:** 애플리케이션별 custom log format은 provider `CallLoggingSettings` 확장 지점의 후속 예제로 남긴다.

### 5. 호출자·통합 호환성

- **P0:** 없음. 모듈 public route와 JDBC persistence API는 변경하지 않는다.
- **P1:** 없음. 유효 header echo, invalid header sanitization, 누락 header 생성값, 400/500 JSON, cancellation을 기존 testApplication 경계에서 검증한다.
- **P2:** `LogCaptor`가 logger level을 전역 변경하므로 새 테스트는 `SAME_THREAD`로 직렬화하고 `AfterAll`에서 clear/close한다. 전체 모듈 회귀를 Task 4에서 재실행한다.
- **P3:** 다른 Ktor 예제 모듈의 provider migration은 별도 issue 범위로 유지한다.

### 6. 문서·시각 자산

- **P0:** 없음. README에는 raw Mermaid를 두지 않고 편집 가능한 SVG와 CairoSVG 생성 PNG를 함께 둔다.
- **P1:** 없음. 영어/한국어 README와 SVG는 같은 구조·의미를 유지하고, architecture/sequence semantic ledger가 branch·long identifier·visible text를 추적한다.
- **P2:** 계획은 두 catalog-style sequence reference PNG를 먼저 열고, full-size PNG를 `view_image(detail="original")`로 확인한다. audit 실패 시 SVG를 먼저 고친 뒤 PNG를 재생성한다.
- **P3:** 텍스트 없는 공통 diagram은 향후 공유할 수 있지만 이번에는 reader-facing 문구를 한영 source-equivalent로 유지한다.

## 체크리스트 판정

| 검토 항목 | 결과 |
|---|---|
| 각 acceptance criterion에 concrete task와 명령이 매핑되었는가 | PASS |
| RED→GREEN→회귀 테스트 순서가 지켜지는가 | PASS |
| 성공·실패·경계·취소·로그 lifecycle 테스트가 있는가 | PASS |
| dependency/BOM·Kotlin/Ktor import와 모듈 경계가 명시되었는가 | PASS |
| performance/stability와 rollback이 계획되었는가 | PASS |
| README/diagram/KDoc/workflow/PR metadata가 누락되지 않았는가 | PASS |
| R2DBC 및 다른 repository 변경을 차단하는가 | PASS |
| P0/P1 blocker | 0 / 0 |

## 최종 판정

**PASS — 구현 준비 완료.** 계획은 승인된 명세의 모든 DoD를 concrete task와 fresh verification 명령으로 연결한다. P0/P1 차단 사항은 없으며, P2 위험은 테스트·문서·정적/시각 audit·workflow receipt로 완화한다. 다음 단계는 사용자 승인 후 TDD RED 구현이다.

검증:

```bash
git diff --check
node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs --series clinic-appointment \
  docs/superpowers/specs/2026-08-27-issue-237-ktor-observability-provider-design.md \
  docs/superpowers/reviews/2026-08-27-issue-237-ktor-observability-provider-spec-review.md \
  docs/superpowers/plans/2026-08-27-issue-237-ktor-observability-provider-plan.md \
  docs/superpowers/reviews/2026-08-27-issue-237-ktor-observability-provider-plan-review.md
```
