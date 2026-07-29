# Issue 58 - Ktor application architecture 설계

## 배경

Issue #58은 `exposed-workshop`에서 가장 작은 Ktor 예제로 12장 production integration 작업을
시작한다. 이 모듈은 auth, outbox, WebSocket, external client, observability scope를 끌어오지
않고 이후 12장 예제의 baseline shape가 되어야 한다.

## 목표

명시적 application assembly, feature route, service/repository boundary, JSON
serialization, error mapping, route test를 보여 주는 최소 Ktor + Exposed JDBC 예제
`12-production-integration/01-ktor-application-architecture`를 추가한다.

## 비목표

- 이 모듈에는 Spring Boot dependency를 넣지 않는다.
- Testcontainers 없음. 첫 baseline example에는 in-memory H2를 사용한다.
- Authentication, session, WebSocket, external HTTP client, metrics 없음.
- Duplication이 실제로 나타나기 전까지 future chapter 12 module용 shared abstraction 없음.

## 설계

- 기존 `includeModules("12-production-integration", false, false)` helper로 chapter를
  `settings.gradle.kts`에 등록한다.
- `bluetape4k-projects`에서 이미 사용하는 현재 official Ktor 3.4.3 coordinate로
  `gradle/libs.versions.toml`에 Ktor alias를 추가한다.
- DTO와 Ktor kotlinx JSON support에는 `kotlin("plugin.serialization")`을 사용한다.
- 작은 `Customer` domain을 사용한다.
  - `Customers` Exposed table.
  - `CustomerRepository` interface.
  - `suspend` function을 노출하고 모든 blocking JDBC `transaction {}` 호출을
    `withContext(Dispatchers.IO)`로 감싸는 `ExposedCustomerRepository` 구현.
  - Validation과 route-friendly error를 위한 `CustomerService`.
- Ktor `ContentNegotiation`, `StatusPages`, `CallLogging`, `CallId`, feature route function을
  사용한다.
- JSON은 `ignoreUnknownKeys = true`로 설정하고 demo API에는 작은 request body limit을
  적용한다.
- `IllegalArgumentException`은 HTTP 400, missing record는 HTTP 404, unexpected exception은
  sanitized HTTP 500 JSON response로 mapping한다.
- Route test에는 test마다 unique H2 JDBC URL을 사용하는 `testApplication`을 사용한다.

## 수용 기준

- `:01-ktor-application-architecture:compileKotlin` 통과.
- `:01-ktor-application-architecture:test` 통과.
- Test는 create, get, list, not found, malformed JSON, 최소 두 validation failure path,
  parallel insert smoke path를 다룬다.
- 모듈에 `README.md`와 `README.ko.md`가 존재한다.
- Public/internal module entrypoint의 KDoc은 contract를 English로 설명한다.
- `git diff --check` 통과.

## 위험

- Blocking Exposed JDBC는 repository-owned `Dispatchers.IO` boundary 뒤에서만 허용된다.
  README는 higher-throughput non-blocking persistence가 R2DBC workshop에 속한다고 명시해야
  한다.
- Version catalog 변경은 repo 전체에 영향을 준다. Alias를 최소화하고 기존 bluetape4k 예제의
  Ktor 3.4.3 version을 재사용한다.
