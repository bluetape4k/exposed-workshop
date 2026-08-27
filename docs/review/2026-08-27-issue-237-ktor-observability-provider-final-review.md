# 이슈 #237 최종 여섯 관점 검토

검토 대상은 `feat/issue-237-ktor-observability-provider`의 현재 working tree다.
이 작업은 한 명의 개발자가 단일 실행 lane에서 수행했으며, 별도 subagent를
사용하지 않았다. 따라서 아래 검토는 독립된 여섯 검토 관점을 같은 변경에
순차적으로 적용하고, 각 관점의 근거를 모듈 테스트·정적 검사·문서 감사와
대조한 기록이다.

## 판정 요약

| 관점 | P0 | P1 | P2 | P3 | 판정 |
|---|---:|---:|---:|---:|---|
| 성능 | 0 | 0 | 1 | 0 | 통과 |
| 안정성 | 0 | 0 | 0 | 1 | 통과 |
| 보안 | 0 | 0 | 0 | 1 | 통과 |
| 운영성 | 0 | 0 | 1 | 0 | 통과 |
| 개발자/API/Kotlin | 0 | 0 | 1 | 0 | 통과 |
| 사용자/호출자 | 0 | 0 | 0 | 1 | 통과 |
| **합계** | **0** | **0** | **3** | **3** | **PASS** |

P0/P1 차단 이슈는 없다. P2는 테스트 전용 root logger 캡처와 운영 benchmark
부재처럼 범위와 이유가 문서화된 관찰이며, P3는 기존 baseline 또는 workshop
범위의 비차단 후속 항목이다.

## 관점별 검토

### 1. 성능

- provider installer는 `KtorPlugins.kt`의 application startup에서 한 번만
  호출된다. 요청마다 provider, meter registry, tracer를 생성하지 않는다.
- 기본 설정에서 Micrometer와 tracing은 비활성이고, request ID 정제는 중앙
  provider의 한 번의 필터링 경로를 사용한다.
- `DiagnosticsService`의 지연은 `kotlinx.coroutines.delay`이며 새 blocking
  호출은 추가하지 않았다.
- P2: 이 예제는 운영 throughput/latency 목표가 없는 JDBC workshop slice라서
  benchmark/stress/race harness는 실행하지 않았다. 성능 목표가 생기면 별도
  benchmark issue로 승격한다.

근거: `performance-stability.md`, focused 9개 및 module 12개 테스트 통과,
provider dependency resolution 성공.

### 2. 안정성

- `CancellationException`은 generic `Exception` 처리보다 먼저 rethrow한다.
- 기존 `ApplicationStopped` Hikari datasource 정리를 유지했다.
- LogCaptor 전역 상태는 `SAME_THREAD`, `@AfterAll`의 clear/close로 테스트
  범위에 한정했다.
- P3: cancellation test는 Ktor test engine이 반환하는 500 HTML에
  `CancellationException`이 남고 구조화 `INTERNAL_ERROR`가 생성되지 않는다는
  계약을 확인한다. 실제 서버 transport cancellation은 통합 환경에서 추가로
  확인할 수 있다.

근거: `KtorPluginsProviderTest`, module build/Kover 성공, 기존 lifecycle 코드
변경 없음.

### 3. 보안

- request ID는 `[A-Za-z0-9._-]` 외 문자를 제거하고 최대 120자로 제한한다.
- 응답과 JSON error에 같은 정제된 correlation ID를 사용하며, 입력을 로그에
  그대로 반영하지 않는다.
- 오류 응답은 내부 exception stacktrace 대신 고정된 `code`, `message`,
  `requestId` 구조를 사용한다.
- P3: provider의 optional telemetry runtime 자체는 이 모듈의 scope가 아니며,
  기본 비활성 계약과 설정 객체 null 검증으로 외부 exporter를 자동 활성화하지
  않음을 고정했다.

근거: `DiagnosticsRoutesTest`, `KtorPluginsProviderTest`, EN/KO README의
provider/error contract 섹션.

### 4. 운영성

- provider 의존성은 version catalog의 versionless alias로 BOM 관리에 편입했다.
- `dependencyInsight`에서 `bluetape4k-ktor-observability:1.12.1`이 BOM 규칙으로
  해석되고, 모듈의 직접 CallId/CallLogging 의존성은 제거되었다.
- README에 테스트, build, detekt 명령과 JDBC-only/R2DBC 별도 저장소 경계를
  기록했다.
- P2: repository-wide detekt는 기존 baseline finding을 출력하지만 exit code 0이며,
  변경 모듈의 기존 `DiagnosticsPersistence.kt:21` `MagicNumber` 외 새 finding은
  확인되지 않았다. 이 unrelated warning은 범위 밖으로 유지했다.

근거: module/root detekt 성공, dependency insight 성공, README parity 및
`git diff --check` 통과.

### 5. 개발자/API/Kotlin

- `installBluetape4kKtorObservability`와 `CorrelationIdSettings`를 사용해 공통
  관측성 책임을 provider로 이동하고, 애플리케이션은 ContentNegotiation·StatusPages와
  JDBC route 계약만 소유한다.
- Kotlin import와 coroutine cancellation 처리는 Kotlin/Ktor 관용 패턴을
  따르며 로컬 UUID sanitizer 중복 구현을 삭제했다.
- 테스트는 기본값, invalid/missing request ID, call log, generic error,
  cancellation을 각각 표현한다.
- P2: public library API를 추가하지 않았으므로 새 KDoc 표면은 없다. 모듈의
  reader-facing 설명은 Korean KDoc/README 정책에 맞춰 유지했다.

근거: `KtorPlugins.kt`, provider test, `org.jetbrains.exposed.v1.*` import와
JDBC 경계 정적 검색.

### 6. 사용자/호출자

- `X-Request-ID: trace:with spaces`는 `tracewithspaces`가 되고, 헤더가 없으면
  16자 Base58 ID가 생성되어 응답 header와 JSON `requestId`에 함께 전달된다.
- validation/bad request는 400, generic exception은 500 구조화 오류이며,
  cancellation은 오류 응답으로 소비하지 않는다.
- EN/KO README는 같은 순서와 의미로 provider 경계, 실행 명령, 오류/취소 계약,
  다이어그램 링크를 제공한다.
- P3: R2DBC 예제는 이 저장소에 추가하지 않고 `exposed-r2dbc-workshop` 링크로
  분리했다. 호출자에게 JDBC 범위를 오인시키지 않는 것이 이번 issue의 계약이다.

근거: 12개 module test, README section/diagram reference parity, EN/KO
architecture·sequence semantic/style/geometry/visual audit 통과.

## 최종 결론

모든 필수 요구사항에 대해 P0=0, P1=0이며 계획의 구현·문서·검증 항목을
충족한다. 알려진 gap은 기존 detekt baseline, 운영 benchmark 미실행, test
engine 수준 cancellation 확인뿐이고 이번 issue를 막지 않는다. PR은 정확한
head와 CI 상태를 live-read-back한 뒤 사용자 승인 게이트에서 대기한다.
