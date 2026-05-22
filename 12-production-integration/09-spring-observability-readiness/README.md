# Spring Observability Readiness

English | [한국어](./README.ko.md)

This module shows a Spring Boot 4 production diagnostics slice for an
Exposed-backed HTTP service. It focuses on readiness, request correlation,
structured errors, and slow-operation diagnostics without adding external
observability infrastructure.

## Architecture

![09 spring observability readiness Architecture diagram](../../docs/images/readme-diagrams/12-production-integration-09-spring-observability-readiness-architecture-01.png)

## Learning Goals

- Configure Spring Boot 4 Actuator readiness with a database-backed custom
  health contributor.
- Sanitize, echo, or generate `X-Request-ID` and include it in diagnostic
  responses.
- Return structured validation and fallback errors.
- Persist operation diagnostics so slow-operation behavior can be tested.

## Run

```bash
./gradlew :09-spring-observability-readiness:bootRun
```

Useful endpoints:

- `GET /actuator/health/readiness`
- `GET /diagnostics/operations/import?delayMs=15`
- `GET /diagnostics/operations`

## Test

```bash
./gradlew :09-spring-observability-readiness:test
```

The tests cover readiness success/failure, request-id propagation, structured
validation errors, and slow-operation persistence.

## Production Notes

- `management.endpoint.health.show-details: always` is used only so workshop
  tests can inspect readiness component details. Production services should use
  `when-authorized` or secure Actuator endpoints before exposing details.
- Incoming `X-Request-ID` values are capped at 120 characters and limited to
  letters, digits, `.`, `_`, `:`, and `-` before being echoed or persisted.

## Spring Boot 4 vs Ktor Tradeoffs

Spring Boot gives production readiness through Actuator health groups and the
application availability model. That is concise and standard for platform
teams, but it brings framework-managed endpoint shape and lifecycle behavior.
The paired Ktor module implements `/readyz` explicitly, which is smaller and
more transparent but leaves readiness semantics and response contracts to the
application.
