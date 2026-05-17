# Issue 58 - Ktor Application Architecture Design

## Context

Issue #58 starts chapter 12 production integration work with the smallest Ktor
example in `exposed-workshop`. The module should become the baseline shape for
later chapter 12 examples without pulling in auth, outbox, WebSocket, external
client, or observability scope.

## Goal

Add `12-production-integration/01-ktor-application-architecture`, a minimal
Ktor + Exposed JDBC example that demonstrates explicit application assembly,
feature routes, service/repository boundaries, JSON serialization, error
mapping, and route tests.

## Non-Goals

- No Spring Boot dependency in this module.
- No Testcontainers; use in-memory H2 for the first baseline example.
- No authentication, sessions, WebSocket, external HTTP client, or metrics.
- No shared abstraction for future chapter 12 modules until duplication is real.

## Design

- Register the chapter in `settings.gradle.kts` with the existing
  `includeModules("12-production-integration", false, false)` helper.
- Add Ktor aliases to `gradle/libs.versions.toml` using the current official
  Ktor 3.4.3 coordinates already used in `bluetape4k-projects`.
- Use `kotlin("plugin.serialization")` for DTOs and Ktor kotlinx JSON support.
- Use a small `Customer` domain:
  - `Customers` Exposed table.
  - `CustomerRepository` interface.
  - `ExposedCustomerRepository` implementation exposing `suspend` functions
    and wrapping every blocking JDBC `transaction {}` call in
    `withContext(Dispatchers.IO)`.
  - `CustomerService` for validation and route-friendly errors.
- Use Ktor `ContentNegotiation`, `StatusPages`, `CallLogging`, `CallId`, and
  feature route functions.
- Configure JSON with `ignoreUnknownKeys = true` and enforce a small request
  body limit for the demo API.
- Map `IllegalArgumentException` to HTTP 400, missing records to HTTP 404, and
  unexpected exceptions to sanitized HTTP 500 JSON responses.
- Use `testApplication` for route tests with a unique H2 JDBC URL per test.

## Acceptance Criteria

- `:01-ktor-application-architecture:compileKotlin` passes.
- `:01-ktor-application-architecture:test` passes.
- Tests cover create, get, list, not found, malformed JSON, at least two
  validation failure paths, and a parallel insert smoke path.
- `README.md` and `README.ko.md` exist for the module.
- KDoc for public/internal module entrypoints explains the contract in English.
- `git diff --check` passes.

## Risks

- Blocking Exposed JDBC is acceptable only behind the repository-owned
  `Dispatchers.IO` boundary. README must state that higher-throughput
  non-blocking persistence belongs in the R2DBC workshop.
- Version catalog changes affect the whole repo; keep aliases minimal and reuse
  the Ktor 3.4.3 version from existing bluetape4k examples.
