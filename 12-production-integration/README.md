# Chapter 12 - Production Integration

English | [한국어](README.ko.md)

This chapter compares production-style service patterns across Spring Boot 4
and Ktor using the examples in this source tree. Each pair keeps the code small
enough to inspect while preserving the boundaries that matter in production:
HTTP entrypoints, service use cases, Exposed persistence, tests, security,
outbox delivery, and operational diagnostics.

## Chapter Goals

- Compare the same service patterns in Spring Boot 4 and Ktor instead of
  treating either framework as the default.
- Keep Exposed persistence behind repository boundaries that are easy to test.
- Show where authentication, idempotency, realtime delivery, readiness, and
  request correlation belong in a small service.
- Keep external systems out of the workshop path by using H2, fake gateways,
  in-process hubs, and focused tests.

## Architecture Diagram

![Chapter 12 production integration architecture](../docs/images/readme-diagrams/12-production-integration-architecture-01.png)

Foundation examples 01-04 cover application architecture and HTTP
outbox/idempotency. Examples 05-10 layer authentication/session metadata,
realtime outbox delivery, and observability/readiness behavior on top.

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

Each example follows the same shape: paired Spring/Ktor coverage where useful,
focused tests, generated PNG/SVG architecture assets, explicit tradeoffs, and
no real external service dependency in the default workshop path.

## Verification Scope

- `settings.gradle.kts` includes chapter modules through
  `includeModules("12-production-integration", false, false)`, so new example
  directories are discovered without per-module settings edits.
- `.github/workflows/examples.yml` builds the selected chapter 12 modules from
  01 through 10 on chapter changes and on its daily schedule.
- `.github/workflows/ci.yml` runs the repository test matrix for H2, PostgreSQL,
  MySQL 8, and MariaDB when non-documentation changes require full CI.
- `.github/workflows/nightly.yml` already runs the full H2 test task and
  selected DB shards. These self-contained examples do not need a separate
  nightly override unless a future example needs external infrastructure.
