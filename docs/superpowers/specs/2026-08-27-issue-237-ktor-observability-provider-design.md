# Ktor observability provider 전환 설계

## 목표

이슈 [#237](https://github.com/bluetape4k/exposed-workshop/issues/237)의 목표는
`12-production-integration/10-ktor-observability-readiness` 예제가 직접 구현한
Ktor `CallId`·`CallLogging` 설정을 `bluetape4k-ktor-observability` provider의
공식 installer로 전환하는 것이다. 현재 예제가 학습시키는 JSON 응답, `StatusPages`,
readiness, 진단 route, Exposed JDBC persistence 경계는 그대로 보존하고, 관측성
공통 설정만 provider로 위임한다.

이번 변경은 한 명의 개발자가 유지하는 workshop의 최소 변경으로 제한한다. 기본
검증은 외부 서비스·네트워크·자격 증명 없이 실행하며, R2DBC 구현은 이 저장소의
범위가 아니다. R2DBC 후속 작업은 `exposed-r2dbc-workshop` 저장소의 별도 이슈에서
다룬다.

## 현재 근거와 source ledger

| 근거 | 확인한 계약 |
|---|---|
| `12-production-integration/10-ktor-observability-readiness/src/main/kotlin/exposed/examples/ktor/observability/config/KtorPlugins.kt` | 현재 앱이 `CallId`, `CallLogging`, `ContentNegotiation`, `StatusPages`를 직접 설치하며 `X-Request-ID`, `callId` MDC, 오류 응답, cancellation 재전파를 소유한다. |
| 같은 모듈의 `DiagnosticsRoutesTest.kt`, `KtorObservabilityReadinessApplicationTest.kt` | 유효한 요청 ID의 응답 전파, 유효하지 않은 입력의 비반향, 구조화된 오류, `/readyz` 계약을 검증한다. |
| `gradle/libs.versions.toml` | `bluetape4k-dependencies = "1.4.0"` BOM을 사용하지만 이 repository의 local catalog alias는 아직 없다. alias 버전은 BOM이 제공한다. |
| provider tag `1.12.1`의 `Bluetape4kKtorObservability.kt` | `installBluetape4kKtorObservability(config)`가 CallId와 CallLogging을 기본 설치하고, registry/tracing을 명시했을 때만 선택 기능을 설치한다. |
| provider tag `1.12.1`의 `CorrelationIdSettings.kt`, `KtorCallIdSupport.kt` | 기본 요청/응답 헤더는 `X-Request-ID`, 기본 MDC key는 `correlation-id`, 생성 길이는 16, 최대 길이는 64이며 허용 문자는 영숫자와 `.`, `_`, `-`이다. 입력은 trim·필터·길이 제한 후 blank이면 생성 경로로 간다. |
| provider tag `1.12.1`의 `CallLoggingSettings.kt`, `KtorCallLoggingSupport.kt` | CallLogging은 correlation ID를 MDC에 넣고 기본 `/healthz`, `/readyz`, `/metrics`를 제외하며 query string을 기본 로그에 포함하지 않는다. |
| provider 공식 문서 | <https://github.com/bluetape4k/bluetape4k-projects/blob/1.12.1/ktor/observability/README.md> |

provider 소스의 동작은 이슈 본문의 요약보다 우선한다. 따라서 현재 로컬
`sanitizeRequestId`가 허용하던 `:`를 유지하지 않고 provider의 문자 필터를
권위 있는 계약으로 채택한다. 현재 테스트의 핵심 요구인 “raw 값이 응답에 그대로
반향되지 않음”은 유지되며, 생성 ID의 형식과 길이는 provider 기본값을 따른다.

## 책임 경계

### provider가 소유하는 책임

- `installBluetape4kKtorObservability`를 통한 `CallId`와 `CallLogging` 설치
- `X-Request-ID` 입력의 trim·문자 필터·최대 길이 제한
- 입력이 비어 있거나 유효 문자가 없을 때의 Base58 correlation ID 생성
- 정제되거나 생성된 ID의 응답 헤더 전파
- `callId` MDC 키를 통한 로그 상관관계
- Micrometer와 OpenTelemetry tracing의 선택적 설치 지점(이번 예제에서는 비활성)

### workshop이 계속 소유하는 책임

- `ContentNegotiation`의 `ApplicationJson` 설정
- `StatusPages`의 `IllegalArgumentException`, `BadRequestException`, 일반 오류 응답
- `CancellationException`의 재전파
- `/`, `/readyz`, `/diagnostics` route와 요청/응답 모델
- Exposed JDBC repository, H2 기본 실행, persistence 테스트

두 책임을 같은 Ktor plugin으로 중복 설치하지 않는다. `KtorPlugins.kt`는 설치
경계로 남기고 provider installer를 먼저 호출한 뒤 JSON과 오류 전용 plugin만
설치한다. route와 repository에는 관측성 provider 의존 API를 새로 노출하지 않는다.

## 선택지와 결정

### 선택지 A — provider installer로 CallId·CallLogging만 전환 (채택)

`installKtorPlugins()`에서 다음 설정으로 provider를 한 번 호출한다.

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

`generatedLength`는 provider의 기본값 16을 사용한다. 예제 코드가 생성 ID의
형식을 다시 정의하지 않음으로써 provider 기본 계약의 변경을 자동으로 따라가며,
예제가 보장하는 애플리케이션별 제한은 최대 길이 120과 헤더 이름·MDC key로
명시한다. 이후 `ContentNegotiation`과 `StatusPages`는 현재 설정을 유지한다.

이 선택은 provider API를 실제로 보여 주면서도 새 adapter, registry, tracing,
운영 인프라를 추가하지 않는 최소 변경이다.

### 선택지 B — provider와 로컬 CallId·CallLogging 병행 설치 (기각)

동일 plugin을 두 번 설치하면 Ktor plugin 충돌 또는 설치 순서 의존성이 생기고,
두 sanitization·logging 정책이 drift한다. provider 전환의 목적과 반대이므로
채택하지 않는다.

### 선택지 C — Micrometer·OpenTelemetry·Prometheus까지 활성화 (기각)

registry, tracing exporter, metrics route와 외부 운영 설정이 필요하다. 이 이슈의
목표는 readiness 예제의 공통 correlation/logging provider 전환이며, 외부 telemetry
운영을 검증하는 이슈가 아니다. 기본 비활성 계약을 문서화하고 별도 후속 이슈로
남긴다.

## 의존성·구성 변경

중앙 catalog에 BOM 기반 alias를 하나 추가한다.

```toml
bluetape4k-ktor-observability = { module = "io.github.bluetape4k:bluetape4k-ktor-observability" }
```

대상 모듈의 `build.gradle.kts`에는 `implementation(libs.bluetape4k.ktor.observability)`를
추가하고, 소스에서 직접 참조하지 않게 되는 Ktor `call-id`와 `call-logging` 직접
의존성은 제거한다. `ktor-server-core`, CIO, content negotiation, status pages,
serialization, Exposed JDBC/Hikari/H2 의존성은 유지한다. provider가 전이 의존성으로
제공하는 plugin API에 별도 버전 override나 새 dependency를 추가하지 않는다.

## 동작 계약과 실패 모드

1. `X-Request-ID: trace_01-abc`처럼 허용 문자를 포함한 입력은 trim 후 provider가
   정제한 값이 `call.callId`, `callId` MDC, 응답 헤더와 오류 응답의 `requestId`에
   사용된다.
2. 공백, 허용되지 않은 문자, 또는 120자를 초과한 입력은 원문 그대로 응답하지
   않는다. provider가 허용 문자를 필터링하고 120자로 제한한 결과가 blank이면
   새로운 Base58 ID를 생성한다.
3. 요청 헤더가 없으면 provider가 16자 Base58 ID를 생성하고, 응답 헤더와
   `ErrorResponse.requestId`에는 생성된 값이 들어간다.
4. `IllegalArgumentException`은 현재와 같이 `400 VALIDATION_FAILED`,
   `BadRequestException`은 `400 BAD_REQUEST`로 응답하며 correlation ID를 담는다.
5. 그 밖의 `Exception`은 로그에 correlation ID를 남기고 `500 INTERNAL_ERROR`로
   응답한다. 단, `CancellationException`은 잡아서 응답하지 않고 다시 던진다.
6. `/readyz`는 call logging 제외 경로로 동작하지만 correlation ID 응답 전파와
   readiness payload는 유지한다.
7. `meterRegistry`와 `tracing`을 `Bluetape4kKtorObservabilityConfig`에 전달하지
   않으므로 Micrometer와 OpenTelemetry는 기본 실행에서 설치되지 않는다. 이
   비활성 상태와 opt-in 확장 지점을 README 양쪽 언어로 명시한다.

실패 모드와 대응은 다음과 같다.

| 실패 모드 | 관찰 가능한 결과 | 대응/검증 |
|---|---|---|
| provider alias가 BOM `1.4.0`에서 해석되지 않음 | Gradle dependency resolution 실패 | catalog alias와 모듈 compile/test를 같은 변경에서 검증하고 provider coordinate를 직접 version 고정하지 않는다. |
| provider installer와 로컬 CallId/CallLogging이 중복 설치됨 | Ktor plugin 충돌 또는 중복 로그/MDC | 로컬 설치·sanitizer·UUID generator를 제거하고 installer 호출 횟수를 단일 경계로 유지한다. |
| 허용되지 않은 request ID가 raw 값으로 응답됨 | 응답/오류의 `requestId`가 입력을 그대로 노출 | provider sanitization 테스트와 route read-back으로 raw 값 비반향을 검증한다. |
| `StatusPages`가 cancellation을 일반 500으로 변환함 | 취소 요청에 오류 JSON이 생성됨 | `CancellationException` 재전파 테스트를 유지하고 일반 `Exception` 처리보다 먼저 확인한다. |
| `X-Request-ID`와 `ErrorResponse.requestId`가 달라짐 | 추적 가능한 요청이 응답 오류에서 분리됨 | 정상·validation·bad request·internal error에서 동일 correlation ID를 읽는다. |
| 선택 telemetry가 기본 활성화됨 | 외부 registry/exporter 없이 앱 기동 실패 또는 추가 의존성 요구 | config에 registry/tracing을 주입하지 않고 README에 opt-in만 설명한다. |

## 호환성·롤백

- 유지되는 외부 계약: `X-Request-ID` 헤더 이름, `callId` MDC key, JSON 오류
  코드와 HTTP status, `/readyz` 응답 구조, Exposed JDBC persistence, 기존 route URL.
- 의도적으로 바뀌는 계약: provider의 허용 문자 집합(`A-Za-z0-9._-`)과 생성 ID
  형식/길이(기본 16)가 source of truth가 된다. `:`를 포함한 값은 더 이상 원문으로
  허용하지 않는다.
- 롤백은 feature branch에서 provider alias와 installer 호출을 제거하고 기존
  `KtorPlugins.kt` 수동 plugin 설치를 복원하는 한 커밋 단위로 가능해야 한다.
  ContentNegotiation, StatusPages, route, repository 변경을 provider rollback과
  섞지 않는다.
- R2DBC, Redis, JaVers 및 운영 backend 설정은 수정하지 않는다.

## 테스트 설계

기존 6개 테스트를 보존하고 다음 동작을 명시적으로 확인한다.

- 유효한 `X-Request-ID`가 응답과 diagnostics 결과에 전파된다.
- 공백·특수문자·과도한 길이 입력이 raw 값으로 응답되지 않고 provider 규칙으로
  정제되거나 새 ID를 받는다.
- validation error와 generic internal error가 같은 correlation ID를 담는다.
- provider CallLogging 설정이 `callId` MDC를 사용하고, captured log event에 정제된
  correlation ID가 포함된다.
- `/readyz` 응답과 correlation header가 함께 유지된다.
- `CancellationException`이 `StatusPages`에서 다시 던져진다.
- 기본 config가 Micrometer/tracing을 활성화하지 않으며 별도 registry/exporter가
  요구되지 않는다.

RED 단계에서는 provider API를 아직 연결하지 않은 상태에서 새 회귀 테스트가
실패함을 기록하고, GREEN 단계에서는 최소 installer/catalog 변경 후 대상 모듈
테스트를 통과시킨다. 기본 명령은 다음과 같다.

```bash
USE_FAST_DB=true repo-test-summary -- ./gradlew :10-ktor-observability-readiness:test --no-build-cache
./gradlew :10-ktor-observability-readiness:detekt
./gradlew detekt
```

Docker/Testcontainers나 외부 Ktor 서버는 이 예제의 기본 검증에 포함하지 않는다.

## 문서·다이어그램

다음 파일을 source-equivalent로 유지한다.

- `12-production-integration/10-ktor-observability-readiness/README.md`
- `12-production-integration/10-ktor-observability-readiness/README.ko.md`

두 README는 provider installer가 correlation ID·call logging을 담당하고,
workshop이 JSON·StatusPages·readiness·Exposed JDBC를 담당한다는 같은 구조와
순서로 설명한다. provider의 기본 sanitization 문자, 최대 길이 override, 기본
telemetry 비활성, 테스트 명령을 양쪽에 기록한다. R2DBC는 이 모듈의 구현 대상이
아니라는 링크 수준의 범위 안내만 둔다.

기존 architecture 다이어그램을 provider 경계에 맞게 갱신하고 sequence 다이어그램을
추가한다. 독자에게 보이는 prose가 있으므로 영어와 한국어 SVG/PNG를 각각 둔다.

- `docs/images/readme-diagrams/12-production-integration-10-ktor-observability-readiness-architecture-01.svg`
- `docs/images/readme-diagrams/12-production-integration-10-ktor-observability-readiness-architecture-01.png`
- `docs/images/readme-diagrams/12-production-integration-10-ktor-observability-readiness-architecture-01.ko.svg`
- `docs/images/readme-diagrams/12-production-integration-10-ktor-observability-readiness-architecture-01.ko.png`
- `docs/images/readme-diagrams/12-production-integration-10-ktor-observability-readiness-sequence-01.svg`
- `docs/images/readme-diagrams/12-production-integration-10-ktor-observability-readiness-sequence-01.png`
- `docs/images/readme-diagrams/12-production-integration-10-ktor-observability-readiness-sequence-01.ko.svg`
- `docs/images/readme-diagrams/12-production-integration-10-ktor-observability-readiness-sequence-01.ko.png`

architecture는 provider installer, local HTTP/error boundary, Exposed JDBC repository,
H2와 deterministic test의 책임 경계를 보여 준다. sequence는 request header가
provider에서 정제·생성되고 route/service/JDBC를 거쳐 응답·오류·cancellation으로
돌아오는 흐름을 보여 준다. Mermaid/Graphviz를 사용하지 않고 기존 SVG 스타일을
따르며, CairoSVG scale 2 렌더링과 XML·semantic·connector·arrowhead·geometry·visual·
asset-pair 감사를 통과시킨다.

## 수용 기준

- [ ] `gradle/libs.versions.toml`에서 `bluetape4k-ktor-observability` alias가
  `bluetape4k-dependencies:1.4.0`으로 해석된다.
- [ ] `KtorPlugins.kt`가 provider installer를 한 번만 호출하며 로컬 CallId,
  CallLogging, sanitizer, UUID generator를 중복 설치하지 않는다.
- [ ] `X-Request-ID`, `callId` MDC, response propagation, provider sanitization,
  오류 correlation, cancellation 재전파가 테스트로 고정된다.
- [ ] ContentNegotiation, StatusPages, route, readiness, Exposed JDBC 동작이
  기존 의미를 유지한다.
- [ ] Micrometer/OpenTelemetry는 기본 비활성이고 README에 opt-in 경계가 있다.
- [ ] English/Korean README와 architecture/sequence SVG·PNG가 source-equivalent다.
- [ ] module test, detekt/static check, changed-example workflow selection이
  통과한다.
- [ ] Korean lesson, issue/PR metadata와 `## DoD Status`가 workflow 계약에 맞고,
  PR은 exact head·CI·review/thread read-back 후 merge-ready 상태로만 보고된다.

## SPW writer gate

- [x] **SPW-01** — 독자, 목표, JDBC-only/R2DBC 제외 범위와 issue/provider/catalog
  근거를 명시했다.
- [x] **SPW-02** — 책임 경계, 선택지와 기각 사유, API 설정, 실패 모드, 호환성,
  rollback, 테스트, 문서·다이어그램, 수용 기준을 포함했다.
- [x] **SPW-03** — work document와 KDoc 기준에 맞춰 한국어 prose를 사용하고
  code/API/identifier/command/URL/version은 원문을 보존했다.
- [x] **SPW-04** — local plugin/test/catalog와 provider `1.12.1` source 및 공식
  문서를 대조했다.
- [x] **SPW-05** — Markdown 구조와 한국어 용어 감사 후 Step 2-R 여섯 렌즈 검토를
  수행한다.

## 설계 상태

`APPROVED` — 사용자 승인(2026-08-27)을 받은 선택지 A를 기준으로 작성했다.
Step 2-R 통합 검토에서 P0/P1 차단 항목이 없음을 확인한 뒤 implementation plan과
TDD 실행으로 진행한다.
