# Ktor Observability Readiness

English | [한국어](README.ko.md)

This module shows a Ktor 3 production diagnostics slice for an Exposed-backed
HTTP service. It pairs with the Spring Boot 4 module and keeps readiness,
request correlation, structured errors, and slow-operation diagnostics explicit
in application code.

## Architecture

![10 ktor observability readiness Architecture diagram](../../docs/images/readme-diagrams/12-production-integration-10-ktor-observability-readiness-architecture-01.png)

## Learning Goals

- Implement a database-backed `/readyz` endpoint without Actuator.
- Use Ktor `CallId`, `CallLogging`, `ContentNegotiation`, and `StatusPages` for
  operational behavior.
- Sanitize incoming `X-Request-ID` values before echoing them through `CallId`.
- Persist slow-operation diagnostics behind an Exposed JDBC repository.
- Test readiness success/failure and structured error responses with
  `testApplication`.

## Run

```bash
./gradlew :10-ktor-observability-readiness:run
```

Useful endpoints:

- `GET /readyz`
- `GET /diagnostics/operations/import?delayMs=15`
- `GET /diagnostics/operations`

## Test

```bash
./gradlew :10-ktor-observability-readiness:test
```

The tests cover readiness success/failure, request-id propagation, structured
validation errors, repository persistence, and slow-operation classification.

## Production Notes

- Incoming `X-Request-ID` values are capped at 120 characters and limited to
  letters, digits, `.`, `_`, `:`, and `-` before being echoed or persisted.
- The in-memory H2 persistence setup keeps the example self-contained. Real
  deployments should use the same readiness contract with the production
  database and pool sizing.

## Spring Boot 4 vs Ktor Tradeoffs

Ktor keeps the production contract visible: readiness response shape, degraded
state, and error mapping are ordinary route/plugin code. That makes the example
easy to adapt, but there is no built-in Actuator health-group convention. The
paired Spring Boot module uses the platform convention instead, which reduces
custom code and aligns with Kubernetes probe defaults.
