# Chapter 12 - Production Integration

This chapter compares production-grade database-backed service patterns across
Spring Boot 4 and Ktor. Each topic keeps the example small enough to inspect
while preserving the boundary that matters in production: HTTP layer, service
use cases, Exposed persistence, tests, and operational documentation.

Every completed example README in this chapter must include a committed PNG
Architecture Diagram under `docs/images/readme-diagrams/`. Mermaid may be used
as an intermediate source, but the final README should embed PNG diagrams so the
rendered documentation is stable across GitHub, IDEs, and offline readers.

## Modules

| Topic | Spring Boot 4 | Ktor |
|---|---|---|
| Application architecture | [02-spring-application-architecture](02-spring-application-architecture/README.md) | [01-ktor-application-architecture](01-ktor-application-architecture/README.md) |
| Auth/session | [05-spring-auth-session](05-spring-auth-session/README.md) | [06-ktor-auth-session](06-ktor-auth-session/README.md) |
| Realtime outbox | [07-spring-outbox-realtime](07-spring-outbox-realtime/README.md) | [08-ktor-outbox-realtime](08-ktor-outbox-realtime/README.md) |
| HTTP client outbox/idempotency | [03-spring-http-outbox-idempotency](03-spring-http-outbox-idempotency/README.md) | [04-ktor-http-outbox-idempotency](04-ktor-http-outbox-idempotency/README.md) |
| Observability/readiness | [09-spring-observability-readiness](09-spring-observability-readiness/README.md) | [10-ktor-observability-readiness](10-ktor-observability-readiness/README.md) |

## Verification

```bash
./gradlew :01-ktor-application-architecture:test
./gradlew :02-spring-application-architecture:test
./gradlew :03-spring-http-outbox-idempotency:test
./gradlew :04-ktor-http-outbox-idempotency:test
./gradlew :05-spring-auth-session:test
./gradlew :06-ktor-auth-session:test
./gradlew :07-spring-outbox-realtime:test
./gradlew :08-ktor-outbox-realtime:test
./gradlew :09-spring-observability-readiness:test
./gradlew :10-ktor-observability-readiness:test
```

Completed topics should keep the same shape: paired Spring/Ktor coverage,
focused tests, PNG Architecture Diagram assets, explicit README tradeoffs, and
no real external service dependency in examples.
