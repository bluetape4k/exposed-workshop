# 07 Multitenant Ktor

English | [한국어](./README.ko.md)

This module shows schema-per-tenant access with Ktor and Exposed JDBC. A Ktor plugin validates `X-Tenant-ID`, request handlers bind the tenant to a coroutine `ThreadContextElement`, and repository transactions switch the active schema before touching tables.

## Architecture Diagram

![Multitenant Ktor architecture](../../docs/images/readme-diagrams/10-multi-tenant-07-multitenant-ktor-architecture-01.png)

## Core Flow

1. `TenantPlugin` reads `X-Tenant-ID` and rejects missing or unknown tenants.
2. Routes call `withTenantContext`, which binds the validated tenant for the coroutine.
3. `ExposedMovieRepository` reads `TenantContext.currentTenant()` and runs `SET SCHEMA`.
4. Each tenant sees only the rows created in its own schema.

## Run

```bash
./gradlew :07-multitenant-ktor:test
./gradlew :07-multitenant-ktor:run
```

## Requests

```bash
curl -H 'X-Tenant-ID: acme' http://localhost:8080/movies
curl -H 'X-Tenant-ID: globex' http://localhost:8080/movies
```

Use this example when a Ktor service needs explicit request-level tenant resolution without Spring MVC, WebFlux, or Spring transaction infrastructure.
