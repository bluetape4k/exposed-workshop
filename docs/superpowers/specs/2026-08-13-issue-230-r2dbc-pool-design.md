# Issue #230 R2DBC 커넥션 풀 헬퍼 통합 설계

## 문제와 목표

`13-ecosystem-integrations/05-ktor-exposed-integration`은 H2 R2DBC URL을
`ConnectionFactoryOptions.parse`로 해석한 뒤 `ConnectionFactories.get`와
`ConnectionPoolConfiguration.builder`를 직접 조합한다. 같은 저장소의
`bluetape4k-r2dbc:1.12.1`에는 이 경계를 `connectionFactoryOptionsOf(url)`와
`connectionPoolOf(options) { ... }`로 단일화하는 API가 이미 있다.

목표는 해당 예제의 직접 builder 호출만 helper로 교체하고, 다음 기존 계약을
그대로 유지하는 것이다.

- `r2dbc:h2:mem:///$databaseName-r2dbc;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
  URL과 `R2dbcDatabaseConfig.connectionFactoryOptions` 값.
- `maxSize = 2`, `initialSize = 1`, `minIdle = 0` 풀 설정. provider 기본
  `minIdle = 8`은 `maxSize = 2`와 함께 사용할 수 없으므로 작은 예제 풀에는
  `minIdle = 0`을 명시한다.
- `KtorExposedIntegrationResources`가 Hikari datasource, R2DBC pool,
  dispatcher의 소유자이며 `ApplicationStopped`에서 한 번 정리하는 lifecycle.
- CRUD, readiness, SQL 오류 비노출 응답과 README 양쪽 locale의 설명.

## 조사 근거

- 로컬 provider tag `1.12.1`의
  `data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/pool/ConnectionPoolSupport.kt`:
  `connectionPoolOf(ConnectionFactoryOptions, R2dbcPoolConfig.() -> Unit)`을
  제공한다.
- 같은 provider의 `R2dbcConnectionConfig.kt`:
  `connectionFactoryOptionsOf(String)`은 URL을 `ConnectionFactoryOptions.parse`
  로 변환한다.
- 현재 예제:
  `13-ecosystem-integrations/05-ktor-exposed-integration/src/main/kotlin/exposed/examples/ktor/exposedintegration/KtorExposedIntegrationApplication.kt`.
- 기준선 명령 `./gradlew :05-ktor-exposed-integration:test`는 현재
  `BUILD SUCCESSFUL`이다.

## 대안과 선택

1. **문자열 URL helper 직접 호출** — `r2dbcConnectionPool(url) { ... }`로 가장
   짧지만 Exposed의 `R2dbcDatabaseConfig`에 전달할 동일한 options 객체를
   재사용하기 어렵다.
2. **options를 먼저 만들고 pool helper 호출** — `connectionFactoryOptionsOf(url)`로
   options를 한 번 만들고 `connectionPoolOf(options) { ... }`에 전달한다.
   Exposed와 pool이 같은 parsed options를 사용하고, URL·풀 설정을 테스트로
   고정할 수 있으므로 이 방식을 선택한다.
3. **기존 R2DBC builder 유지 + alias만 추가** — 신규 dependency API를 실제
   예제에 반영하지 못하므로 제외한다.

## 구현 경계

### Gradle

`gradle/libs.versions.toml`의 `bluetape4k` library 목록에
`bluetape4k-r2dbc = { module = "io.github.bluetape4k:bluetape4k-r2dbc" }`를
versionless alias로 추가한다. 버전은 이미 import한
`bluetape4k-dependencies:1.4.0` BOM이 `1.12.1`로 결정한다.

예제 module에는 `implementation(libs.bluetape4k.r2dbc)`를 추가한다.

### Kotlin

직접 import하는 `io.r2dbc.pool.ConnectionPoolConfiguration`와
`ConnectionFactories`를 제거하고 다음 helper를 import한다.

```kotlin
val r2dbcOptions = connectionFactoryOptionsOf(r2dbcUrl)
val r2dbcPool = connectionPoolOf(r2dbcOptions) {
    maxSize = 2
    initialSize = 1
    minIdle = 0
}
```

`R2dbcDatabase.connect`에는 같은 `r2dbcOptions`를 계속 전달한다. lifecycle
코드는 caller-owned pool을 `disposeLater().block(Duration.ofSeconds(5))`로
정리하는 현재 동작을 유지한다. `close()`는 suspend 함수가 아니므로
`runCatching`으로 독립된 cleanup 실패를 격리할 수 있지만,
`CancellationException`을 포착하는 coroutine 경로는 새로 만들지 않는다.

### 테스트와 문서

- 기존 CRUD/readiness/error 테스트를 그대로 실행해 helper 교체가 HTTP 계약을
  바꾸지 않는지 확인한다.
- pool 생성 경계에 대해 URL options와 `maxSize`/`initialSize`를 고정하는
  작은 테스트를 추가한다. 테스트는 실제 H2 in-memory pool을 생성하고
  `use`로 닫으며, mock으로 builder 호출을 검증하지 않는다.
- `README.md`와 `README.ko.md`에 caller-owned R2DBC pool이
  `bluetape4k-r2dbc` helper로 만들어진다는 코드 예시와 의존성 설명을
  source-equivalent하게 반영한다.
- 기존 architecture diagram은 리소스 소유 경계를 이미 표현하고 있으며
  topology가 바뀌지 않으므로 diagram asset 변경은 하지 않는다.

## 실패·호환성 계약

- malformed R2DBC URL이나 H2 driver 누락은 helper가 기존 parse/factory와 같은
  생성 시점 예외로 반환한다.
- `initialSize > maxSize` 같은 pool 제약은 provider `R2dbcPoolConfig.validate`
  에서 거부한다. 예제 값은 유효한 `1 <= 2`다.
- pool dispose 실패가 datasource/dispatcher cleanup을 막지 않아야 한다.
- 이 issue에서는 `02-alternatives-to-jpa/r2dbc-example`, 신규 parity 모듈,
  provider issue #80 또는 `exposed-r2dbc-workshop` #113/#116을 수정하지 않는다.

## 수용 기준과 DoD

- [x] 직접 `ConnectionPoolConfiguration.builder`/`ConnectionFactories.get`가
  예제 production source에서 제거된다.
- [x] `bluetape4k-r2dbc` versionless catalog alias와 module dependency가
  추가된다.
- [x] URL, options 재사용, `maxSize = 2`, `initialSize = 1`, `minIdle = 0`이
  테스트로 증명된다.
- [x] CRUD, readiness, sanitized SQL error 테스트가 새로 통과한다.
- [x] caller-owned cleanup와 one-time `ApplicationStopped` 경계가 README 양쪽
  locale에 일치하게 설명된다.
- [x] `git diff --check`, Detekt/compile 및 module test가 통과한다.

## Writer evidence

- `SPW-01`: 예제 source, provider tag, issue 범위와 현재 테스트 결과를 고정했다.
- `SPW-02`: 문제, 대안, 경계, 실패 계약, acceptance/DoD를 포함했다.
- `SPW-03`: 한국어 기술 문장과 API/명령/URL 보존을 자연스러움 checklist로
  검토한다.
- `SPW-04`: provider source와 local caller를 line-level로 대조한다.
- `SPW-05`: commit 전 Markdown read-back과 workflow receipt evidence를 남긴다.
