# 이슈 #237 성능·안정성 검토

검토 대상은 `feat/issue-237-ktor-observability-provider`의 현재 working tree이며,
provider 전환으로 변경된 Ktor 설정·테스트와 기존 JDBC 진단 경로를 함께 확인했다.
이 예제는 운영 벤치마크가 아니라 작은 JDBC workshop slice이므로 재현 가능한
throughput/latency benchmark는 실행하지 않았다.

## 성능·안정성 판정

| 우선순위 | 파일:라인 | 관점 | 결과 | 필요한 조치/근거 |
|---|---|---|---|---|
| P0/P1 없음 | `config/KtorPlugins.kt:25-36` | startup 설치 | `installBluetape4kKtorObservability`를 애플리케이션 시작 시 한 번 호출하며 요청마다 provider/registry를 만들지 않는다. | 현재 구조 유지. focused provider test 통과. |
| P0/P1 없음 | `service/DiagnosticsService.kt:20-25` | event loop·blocking | 지연은 `kotlinx.coroutines.delay`를 사용하고 JDBC 기록은 기존 repository 경계로 위임한다. | 새 blocking 호출 없음. module test 통과. |
| P0/P1 없음 | `config/KtorPlugins.kt:61-64` | cancellation | broad `Exception` 처리보다 먼저 `CancellationException`을 rethrow한다. | cancellation response test에서 `INTERNAL_ERROR` 소비가 없음을 확인. |
| P0/P1 없음 | `KtorObservabilityReadinessApplication.kt:40-42` | 자원 정리 | `ApplicationStopped`에서 Hikari datasource를 닫는 기존 lifecycle을 유지한다. | 변경 없음; 전체 module 회귀로 보호. |
| P0/P1 없음 | `config/KtorPluginsProviderTest.kt:24-40` | 테스트 안정성 | 전역 LogCaptor를 `SAME_THREAD`로 제한하고 `@AfterAll`에서 clear/close한다. | 병렬 간섭 범위를 테스트 클래스에 한정. |
| P2 | `config/KtorPluginsProviderTest.kt:32` | 로그 캡처 범위 | Ktor application logger가 root appender를 통해 기록되어 `LogCaptor.forRoot()`가 필요하다. | 테스트 전용이며 운영 코드에는 영향 없음. 향후 provider logger hook이 노출되면 logger 범위를 좁힐 수 있다. |

## 스캔 결과

- `GlobalScope`, `runBlocking`, `Thread.sleep`, `synchronized`, `@Synchronized`,
  `runCatching` 신규 사용 없음.
- provider 기본값은 Micrometer metrics와 tracing을 만들지 않으며, 모듈 설정도
  이를 opt-in으로 남긴다.
- request ID 정제·생성은 중앙 provider로 이동했고, 응답 header 전파와
  `StatusPages`의 구조화 오류/취소 계약을 focused test로 검증했다.
- Testcontainers나 외부 DB를 새로 시작하지 않는다. H2 fast profile과 기존
  `DiagnosticsPersistence` lifecycle을 사용한다.

## 미실행 및 영향

- 별도 benchmark/stress/race harness: 미실행. 이 변경은 provider 설치 경계와
  ID/error semantics를 교체하는 예제 수준이며, 재현 가능한 성능 목표가 이슈에
  정의되어 있지 않다.
- 외부 Micrometer/OpenTelemetry runtime: 미실행. 기본 비활성 계약을 유지하고
  provider API 기본값 테스트로 범위를 고정했다.

판정: `PASS` (P0 = 0, P1 = 0). P2는 테스트 전용 root log capture의 이유를
문서화한 비차단 관찰이다.
