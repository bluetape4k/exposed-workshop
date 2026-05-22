# Chapter 12 - Production Integration

This chapter compares production-grade database-backed service patterns across
Spring Boot 4 and Ktor. Each topic keeps the example small enough to inspect
while preserving the boundary that matters in production: HTTP layer, service
use cases, Exposed persistence, tests, and operational documentation.

## Modules

| Topic | Spring Boot 4 | Ktor |
|---|---|---|
| Application architecture | [02-spring-application-architecture](02-spring-application-architecture/README.md) | [01-ktor-application-architecture](01-ktor-application-architecture/README.md) |
| Auth/session | Planned by issue #59 | Planned by issue #59 |
| Realtime outbox | Planned by issue #60 | Planned by issue #60 |
| HTTP client outbox/idempotency | [03-spring-http-outbox-idempotency](03-spring-http-outbox-idempotency/README.md) | [04-ktor-http-outbox-idempotency](04-ktor-http-outbox-idempotency/README.md) |
| Observability/readiness | Planned by issue #62 | Planned by issue #62 |

## Verification

```bash
./gradlew :01-ktor-application-architecture:test
./gradlew :02-spring-application-architecture:test
./gradlew :03-spring-http-outbox-idempotency:test
./gradlew :04-ktor-http-outbox-idempotency:test
```

Completed topics should keep the same shape: paired Spring/Ktor coverage,
focused tests, explicit README tradeoffs, and no real external service
dependency in examples.
