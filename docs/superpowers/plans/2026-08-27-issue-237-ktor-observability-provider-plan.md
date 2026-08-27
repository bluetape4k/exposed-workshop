# Ktor observability provider 전환 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use inline execution in this session. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `10-ktor-observability-readiness`가 로컬 중복 Ktor 관측성 플러그인 대신 `bluetape4k-ktor-observability`의 중앙 설치기를 사용하도록 전환하고, JDBC 예제의 상관관계 ID·구조화 오류·취소 전파 동작과 한영 문서/다이어그램을 검증한다.

**Architecture:** 라이브러리 provider가 CallId/CallLogging과 선택적 Micrometer/OpenTelemetry 설치를 소유한다. 워크숍 애플리케이션은 `KtorPlugins.kt`에서 명시적인 `CorrelationIdSettings`만 전달하고, ContentNegotiation·StatusPages 및 예제 라우트의 오류 계약은 계속 소유한다. R2DBC 구현은 `exposed-r2dbc-workshop`의 별도 이슈 범위로 유지한다.

**Tech Stack:** Kotlin 2.3, Ktor 3.5.2, bluetape4k BOM 1.4.0, Gradle version catalog, JUnit 5, LogCaptor, H2 fast test profile, SVG/PNG README diagrams.

---

## File impact map

| 영역 | 파일 | 변경 목적 |
|---|---|---|
| 의존성 | `gradle/libs.versions.toml` | 중앙 observability artifact alias 추가 |
| 빌드 | `12-production-integration/10-ktor-observability-readiness/build.gradle.kts` | provider 의존성과 로그 캡처 테스트 의존성 추가 |
| 플러그인 | `.../src/main/kotlin/.../config/KtorPlugins.kt` | 로컬 CallId/CallLogging 중복 제거, 중앙 installer 호출 |
| 회귀 테스트 | `.../src/test/kotlin/.../config/KtorPluginsProviderTest.kt` | 기본 설정, 오류, 취소, 로그 상관관계 검증 |
| 회귀 테스트 | `.../src/test/kotlin/.../routes/DiagnosticsRoutesTest.kt` | provider sanitization과 Base58 생성 ID 계약 검증 |
| 문서 | `.../README.md`, `.../README.ko.md` | provider 경계와 동작/실행 명령 source-equivalent 반영 |
| 다이어그램 | `.../docs/images/readme-diagrams/*` | 한영 architecture/sequence SVG·PNG와 semantic ledger 추가 |
| 검토 기록 | `docs/superpowers/reviews/*`, `docs/review/*` | 계획/성능/최종 여섯 관점 검토 근거 보존 |
| 교훈 | `docs/lessons/2026-08-27-issue-237-ktor-observability-provider.md` | 재사용 가능한 결정과 검증 교훈 기록 |

`00-shared`, JDBC repository/service/persistence 예제, R2DBC 모듈, `.github/scripts/select-changed-examples.sh`, 루트 README/chapter index는 변경하지 않는다. 모듈과 변경 감지 규칙은 이미 존재한다.

## Task 1: provider alias와 빌드 입력을 준비한다

- [x] `gradle/libs.versions.toml`의 bluetape4k alias 영역에 다음 versionless alias를 추가한다.

  ```toml
  bluetape4k-ktor-observability = { module = "io.github.bluetape4k:bluetape4k-ktor-observability" }
  ```

- [x] 모듈 `build.gradle.kts`에 다음 의존성을 추가한다.

  ```kotlin
  implementation(libs.bluetape4k.ktor.observability)
  testImplementation(libs.logcaptor)
  ```

- [x] 추가한 alias가 BOM `1.4.0`에서 해석되는지 다음 명령으로 확인한다.

  ```bash
  ./gradlew :10-ktor-observability-readiness:dependencyInsight \
    --dependency bluetape4k-ktor-observability \
    --configuration testRuntimeClasspath
  ```

- [x] provider artifact의 실제 public API를 소스와 published metadata에서 다시 확인하고, 직접 `ktor-server-call-id`/`ktor-server-call-logging` 의존성이 provider의 transitives로만 남는지 확인한다.

- [x] RED 테스트 전에 현재 baseline을 보존한다.

  ```bash
  USE_FAST_DB=true repo-test-summary -- ./gradlew :10-ktor-observability-readiness:test --no-build-cache --no-daemon --no-configuration-cache
  ```

- [x] 변경을 작은 Lore 커밋으로 기록한다. 커밋은 최종 구현·문서·검증 기록을 함께 묶어 하나의 reviewable change로 남긴다.

## Task 2: provider-specific RED 테스트를 먼저 추가한다

- [x] `DiagnosticsRoutesTest.kt`에 다음 두 테스트를 추가한다.
  - `X-Request-ID: trace:with spaces`가 응답과 validation JSON의 `requestId`에 `tracewithspaces`로 남는지 검증한다. 기존 로컬 sanitizer는 콜론을 허용하거나 공백 입력을 거부하므로 이 테스트는 중앙 provider 전환 전 RED여야 한다.
  - 헤더가 없을 때 응답 `X-Request-ID`와 validation JSON `requestId`가 모두 Base58 문자 `[A-Za-z0-9]+` 16자 생성값인지 검증한다. 기존 UUID 생성값은 36자이므로 RED여야 한다.

- [x] 새 `KtorPluginsProviderTest.kt`를 다음 계약으로 작성한다. LogCaptor가 전역 logger level을 바꾸므로 클래스에 `@Execution(ExecutionMode.SAME_THREAD)`를 적용하고 `@AfterAll`에서 clear/close한다.

  ```kotlin
  package exposed.examples.ktor.observability.config

  import io.bluetape4k.ktor.observability.Bluetape4kKtorObservabilityConfig
  import nl.altindag.log.LogCaptor
  import io.ktor.client.request.get
  import io.ktor.client.request.header
  import io.ktor.client.statement.bodyAsText
  import io.ktor.http.HttpStatusCode
  import io.ktor.server.application.call
  import io.ktor.server.response.respondText
  import io.ktor.server.routing.get
  import io.ktor.server.routing.routing
  import io.ktor.server.testing.testApplication
  import kotlin.coroutines.cancellation.CancellationException
  import kotlin.test.assertEquals
  import kotlin.test.assertTrue
  import org.junit.jupiter.api.Test
  import org.junit.jupiter.api.AfterAll
  import org.junit.jupiter.api.BeforeAll
  import org.junit.jupiter.api.TestInstance
  import org.junit.jupiter.api.parallel.Execution
  import org.junit.jupiter.api.parallel.ExecutionMode

  @Execution(ExecutionMode.SAME_THREAD)
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class KtorPluginsProviderTest {
      private lateinit var logCaptor: LogCaptor

      @BeforeAll
      fun setUpLogger() {
          logCaptor = LogCaptor.forRoot()
          logCaptor.setLogLevelToInfo()
      }

      @AfterAll
      fun tearDownLogger() {
          logCaptor.clearLogs()
          logCaptor.close()
      }

      @Test
      fun `provider 기본 설정은 correlation 과 call logging 을 켜고 telemetry 를 끈다`() {
          val config = Bluetape4kKtorObservabilityConfig()

          assertTrue(config.installCorrelationId)
          assertTrue(config.installCallLogging)
          assertEquals(false, config.installMicrometerMetrics)
          assertEquals(null, config.meterRegistry)
          assertEquals(null, config.tracing)
      }

      @Test
      fun `provider 설치 후 generic exception 은 구조화 오류와 correlation id 를 유지한다`() = testApplication {
          application {
              installKtorPlugins()
              routing {
                  get("/boom") { error("boom") }
              }
          }

          val response = client.get("/boom") {
              header("X-Request-ID", "internal-trace")
          }

          assertEquals(HttpStatusCode.InternalServerError, response.status)
          assertTrue(response.bodyAsText().contains("\"requestId\":\"internal-trace\""))
          assertEquals("internal-trace", response.headers[REQUEST_ID_HEADER])
      }

      @Test
      fun `provider 설치는 CancellationException 을 소비하지 않는다`() {
          testApplication {
              application {
                  installKtorPlugins()
                  routing {
                      get("/cancel") { throw CancellationException("cancelled") }
                  }
              }

              val response = client.get("/cancel")
              val body = response.bodyAsText()

              assertEquals(HttpStatusCode.InternalServerError, response.status)
              assertTrue(body.contains("CancellationException"))
              assertTrue(!body.contains("\"code\":\"INTERNAL_ERROR\""))
          }
      }

      @Test
      fun `provider call logging 은 sanitized correlation id 를 기록한다`() = testApplication {
          application {
              installKtorPlugins()
              routing { get("/ok") { call.respondText("ok") } }
          }

          client.get("/ok") { header("X-Request-ID", "trace:with spaces") }

          assertTrue(logCaptor.getLogs().any { it.contains("correlationId=tracewithspaces") })
      }
  }
  ```

  실제 모듈 package에서 `installKtorPlugins()`를 호출하고 응답 header를 `REQUEST_ID_HEADER`로 읽는다. 사용하지 않는 mock/로깅 import는 남기지 않는다. 테스트가 provider semantics를 정확히 겨냥하는지 확인한 뒤 다음 RED 명령을 실행한다.

  ```bash
  USE_FAST_DB=true ./gradlew :10-ktor-observability-readiness:test --tests '*DiagnosticsRoutesTest' --tests '*KtorPluginsProviderTest' --no-build-cache --no-daemon --no-configuration-cache
  ```

## Task 3: 중앙 installer로 구현을 전환하고 GREEN으로 만든다

- [x] `KtorPlugins.kt`에서 기존 `CallId` 설치, `sanitizeRequestId`, UUID 생성, `callIdMdc`/`callId` 기반 CallLogging 설치와 관련 import를 제거한다.

- [x] ContentNegotiation/StatusPages보다 먼저 다음 installer를 한 번 호출한다.

  ```kotlin
  installBluetape4kKtorObservability(
      Bluetape4kKtorObservabilityConfig(
          correlationId = CorrelationIdSettings(
              requestHeaderName = REQUEST_ID_HEADER,
              responseHeaderName = REQUEST_ID_HEADER,
              mdcKey = "callId",
              maxLength = MAX_REQUEST_ID_LENGTH,
              propagateResponseHeader = true,
          ),
      ),
  )
  ```

- [x] `REQUEST_ID_HEADER = "X-Request-ID"`와 `MAX_REQUEST_ID_LENGTH = 120`을 유지한다. provider의 허용문자/trim/filter/최대 길이 및 16자 Base58 생성 동작을 문서와 테스트에 명시한다.

- [x] StatusPages의 기존 계약은 유지한다. `IllegalArgumentException`, `BadRequestException`은 400 구조화 응답, generic `Exception`은 500 구조화 응답, `CancellationException`은 기록 후 반드시 rethrow한다. 응답의 `requestId`는 `callId`에서 읽는다.

- [x] GREEN focused test와 중복 설치 정적 검색을 실행한다. version catalog의 전역 Ktor alias는 다른 예제가 사용할 수 있으므로 삭제하지 않고, 이 모듈의 직접 의존성과 로컬 중복 구현만 검색한다.

  ```bash
  USE_FAST_DB=true ./gradlew :10-ktor-observability-readiness:test --tests '*DiagnosticsRoutesTest' --tests '*KtorPluginsProviderTest' --no-build-cache --no-daemon --no-configuration-cache
  rg -n 'sanitizeRequestId|UUID\.randomUUID|install\s*\(CallId|callIdMdc|ktor-server-call-id|ktor-server-call-logging' \
    12-production-integration/10-ktor-observability-readiness gradle/libs.versions.toml
  ```

- [x] 구현 변경을 Lore 형식의 최종 커밋으로 기록하고, 커밋 전 `git diff --check`를 통과시킨다.

## Task 4: 모듈 회귀·정적 검사와 성능/안정성 검토를 수행한다

- [x] 다음 명령을 순서대로 실행하고 출력/실패 원인을 기록한다.

  ```bash
  USE_FAST_DB=true repo-test-summary -- ./gradlew :10-ktor-observability-readiness:test --no-build-cache --no-daemon --no-configuration-cache
  ./gradlew :10-ktor-observability-readiness:build --no-build-cache --no-daemon --no-configuration-cache
  ./gradlew :10-ktor-observability-readiness:detekt --no-build-cache --no-daemon --no-configuration-cache
  ./gradlew detekt --no-build-cache --no-daemon --no-configuration-cache
  git diff --check
  ```

- [x] `performance-stability-scan.md` 프레임으로 Korean review artifact를 작성한다. provider installer가 요청당 새 객체/registry를 만들지 않는지, optional telemetry가 기본 비활성인지, CallId/CallLogging 순서가 안정적인지, cancellation이 소비되지 않는지, log capture가 테스트 범위에 국한되는지를 소스/테스트 근거로 판정한다. benchmark가 없는 경우 `미실행`과 이유를 명시한다.

- [x] `org.jetbrains.exposed.v1.*` import, JDBC-only 범위, Ktor module test resource, repository helper 영향이 유지되는지 `rg`와 diff로 확인한다.

## Task 5: 한영 README를 source-equivalent로 갱신한다

- [x] 모듈 `README.md`와 `README.ko.md`에 같은 순서와 의미로 다음 섹션을 반영한다.
  - provider가 CallId/CallLogging을 소유하고 애플리케이션이 correlation settings와 StatusPages를 소유한다는 경계
  - `X-Request-ID` trim/filter/max 120, 공백/콜론 입력의 sanitized 결과, 헤더 누락 시 16자 Base58 생성, 응답 header 전파
  - 기본값에서 Micrometer/OpenTelemetry를 설치하지 않으며 별도 설정일 때만 활성화된다는 점
  - generic error의 JSON 계약과 cancellation rethrow
  - 이 예제는 JDBC/H2 fast profile이며 R2DBC 구현 링크/범위는 `exposed-r2dbc-workshop`임
  - 모듈 테스트/build/detekt 실행 명령
  - architecture/sequence PNG 링크 2개 이상

- [x] 명령, API 이름, URL, 파일 경로는 원문을 보존하고 설명 문장만 각 언어로 작성한다. `git diff --check`와 Korean terminology audit 및 EN/KO 섹션 parity 검사를 통과시킨다.

## Task 6: 한영 architecture/sequence SVG·PNG와 semantic ledger를 만든다

- [x] 작업 시작 전에 다음 레퍼런스 PNG를 실제로 열어 스타일/밀도/라벨 배치를 확인한다.
  - `/Users/debop/work/bluetape4k/bluetape4k-workshop/docs/images/readme-diagrams/observability-observability-basic-readme-trace-sequence-01.png`
  - `/Users/debop/work/bluetape4k/exposed-workshop/docs/images/readme-diagrams/13-trino-session-options-sequence-01.png`

- [x] 기존 architecture SVG/PNG를 영어 source-equivalent로 갱신하고 한국어 SVG/PNG를 추가한다. 10개 이하 노드, 9개 이하 edge, `graph TB`, provider 경계를 하나의 연결된 흐름으로 표현한다. 예시 노드는 `Client`, `Ktor Application`, `Provider Installer`, `CallId`, `CallLogging`, `StatusPages`, `ErrorResponse`, `Optional Telemetry`, `JDBC Example`로 제한한다.

- [x] 영어/한국어 sequence SVG/PNG를 추가한다. participants는 `Client`, `Provider installer`, `Correlation`, `CallLogging`, `StatusPages`, `JDBC diagnostics`로 제한하고 visible numbered rows는 1–9를 사용한다. 성공/오류/취소 alt fragment를 포함하되 cancel path에서 rethrow를 명시한다.

- [x] 각 SVG 옆에 `.semantic.json` ledger를 둔다. branch/decision/long identifier와 visible text를 기록하고, ledger의 `revision`은 작성 시점 `git rev-parse HEAD`의 실제 출력으로 채운다. 각 PNG는 SVG를 CairoSVG scale 2로 재생성하며 수동으로 만든 PNG를 사용하지 않는다.

- [ ] 다이어그램 skill의 모든 필수 audit를 각 SVG/PNG에 실행한다.

  ```bash
  for svg in docs/images/readme-diagrams/12-production-integration-10-ktor-observability-readiness-*.svg; do
      png="${svg%.svg}.png"
      ledger="${svg%.svg}.semantic.json"
      xmllint --noout "$svg"
      cairosvg "$svg" -o "$png" -s 2
      python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-semantic-audit.py --repo-root . --json "$ledger"
      python /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-connector-audit.py "$svg"
      python /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-arrowhead-audit.py "$svg"
      python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-geometry-audit.py --fail-diagonal "$svg"
      python /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-endpoint-audit.py "$svg"
      python /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-mixed-corner-audit.py "$svg"
      python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-svg-text-normalize.py "$svg"
      python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-visual-audit.py --require-opaque "$png"
  done
  python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-asset-pair-audit.py \
    --asset-dir docs/images/readme-diagrams \
    --readme 12-production-integration/10-ktor-observability-readiness/README.md
  python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-asset-pair-audit.py \
    --asset-dir docs/images/readme-diagrams \
    --readme 12-production-integration/10-ktor-observability-readiness/README.ko.md
  # --require-all-referenced is intentionally omitted: the repository already
  # contains historical diagrams that are not exposed by the module README.
  rg -n 'architecture-01\.png|sequence-02\.png' \
    12-production-integration/10-ktor-observability-readiness/README.md \
    12-production-integration/10-ktor-observability-readiness/README.ko.md
  python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-sequence-style-audit.py docs/images/readme-diagrams/12-production-integration-10-ktor-observability-readiness-sequence-02.svg
  python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-sequence-style-audit.py docs/images/readme-diagrams/12-production-integration-10-ktor-observability-readiness-sequence-02.ko.svg
  ```

- [x] `view_image(detail="original")`로 architecture와 sequence PNG를 각각 확인해 글자 겹침, 잘림, 투명 배경, 화살표 endpoint, 색 대비 문제를 판정한다. 모든 audit/시각 검토 결과를 plan/lesson에 연결한다.

## Task 7: 최종 검증, workflow receipt, lesson, PR handoff를 완료한다

- [x] 최종 verifier 체크를 수행한다.

  ```bash
  USE_FAST_DB=true repo-test-summary -- ./gradlew :10-ktor-observability-readiness:test --no-build-cache --no-daemon --no-configuration-cache
  ./gradlew :10-ktor-observability-readiness:build --no-build-cache --no-daemon --no-configuration-cache
  ./gradlew :10-ktor-observability-readiness:detekt --no-build-cache --no-daemon --no-configuration-cache
  ./gradlew detekt --no-build-cache --no-daemon --no-configuration-cache
  git diff --check
  node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs --series clinic-appointment \
    docs/superpowers/specs/2026-08-27-issue-237-ktor-observability-provider-design.md \
    docs/superpowers/reviews/2026-08-27-issue-237-ktor-observability-provider-spec-review.md \
    docs/superpowers/plans/2026-08-27-issue-237-ktor-observability-provider-plan.md \
    docs/review/2026-08-27-issue-237-ktor-observability-provider-final-review.md \
    docs/lessons/2026-08-27-issue-237-ktor-observability-provider.md
  ```

- [x] issue #237의 title/body/milestone/labels/assignee/parent·related links를 `gh issue view --json`으로 다시 읽고, 변경 diff가 issue DoD를 모두 매핑하는지 확인한다. R2DBC 코드나 다른 repository 변경이 없는지 확인한다.

- [x] Korean lesson과 `docs/review/2026-08-27-issue-237-ktor-observability-provider-final-review.md`를 작성한다. 여섯 관점별 P0–P3를 증거와 함께 기록하고 P0/P1은 0이어야 한다. 단일 개발자 실행이므로 병렬 subagent를 사용하지 않았다는 검토 범위를 명시한다.

- [x] workflow helper로 evidence/check-result와 component-evidence를 등록하고, 모든 필수 check가 통과한 뒤 lane-complete와 completion-check를 실행한다. 최종 receipt checksum과 검증 명령을 lesson/PR에 연결했다.

- [x] final docs/review/lesson을 Lore 커밋으로 기록하고, `git diff --check` 후 semantic branch를 push한다. 커밋 `9e38d62f1084f0bed453ee43ce6224dc99c96ce1`을 push했다.

- [x] PR 생성 권한 범위는 `feat/issue-237-ktor-observability-provider` → `develop`로 고정한다. PR #249 본문은 한국어로 작성하고 `Summary`, `Testing`, `Docs`, `DoD Status`와 workflow receipt를 포함했다.

- [x] PR 생성 후 exact head SHA, base, body, status checks, review threads, mergeability를 live-read-back했다. CI가 진행 중이므로 fresh explicit `승인` 전 merge/auto-merge를 수행하지 않고 `PENDING`으로 유지한다.

## Risk prediction and plan self-review

- provider artifact가 BOM에 없거나 alias accessor가 다르면 Task 1에서 중단하고 `buildSrc`/catalog convention을 재확인한다. 새 dependency를 임의 버전으로 추가하지 않는다.
- 기존 invalid request-id 테스트가 provider의 filter semantics와 충돌하면 provider 소스의 허용문자 계약을 기준으로 테스트 기대값을 조정하고, 의도적인 behavioral change를 README/PR에 명시한다.
- LogCaptor가 병렬 테스트와 간섭하면 `SAME_THREAD` 범위를 유지하고 모듈 테스트 전체를 다시 실행한다. 로그 문자열을 애플리케이션 구현에 맞춰 억지로 고정하지 않는다.
- 다이어그램 audit가 실패하면 SVG source를 먼저 고치고 PNG를 재생성한다. raw Mermaid, 수동 PNG, source-equivalent가 아닌 번역본은 허용하지 않는다.
- 계획 self-review 결과: 모든 spec DoD가 Task 1–7에 매핑되고, RED→GREEN→회귀→문서/다이어그램→final verifier 순서이며, R2DBC 제외 경계·cancellation·optional telemetry·workflow/PR gate가 명시되었다. 구현 전 새 계획 승인 없이는 코드를 수정하지 않는다.
