# 10 Multi-Tenant (Production)

English | [한국어](./README.ko.md)

This chapter follows the multi-tenant implementations in the source tree: Spring MVC schema routing, Tomcat Virtual Thread tenant propagation, WebFlux/Reactor context bridging, explicit schema-per-tenant and database-per-tenant transaction helpers, Spring Security tenant authorization, a Ktor variant, and tenant onboarding. The examples focus on where tenant identity is accepted, how it is propagated, and where database isolation is enforced.

## Chapter Goals

- Understand the full flow of tenant identification, propagation, and isolation.
- Compare implementation differences across Spring MVC, Virtual Thread, and WebFlux environments.
- Establish validation points to prevent leakage and isolation failures in production.

## Prerequisites

- Contents of `09-spring`
- Basic concepts of transactions and DataSource routing

---

## Multi-Tenancy Strategy Overview

This chapter starts with the **Shared Database / Separate Schema** strategy. Data is isolated by separating per-tenant schemas (`korean`, `english`) within a single DB instance.

![Per-tenant schema layout diagram](../docs/images/readme-diagrams/10-multi-tenant-schema-layout-04.png)

It then compares that baseline with **Database per Tenant**, where each whitelisted tenant owns a dedicated Hikari pool and Exposed `Database`.

### Per-Tenant Schema Isolation Architecture

![Per-Tenant Schema Isolation Architecture diagram](../docs/images/readme-diagrams/10-multi-tenant-architecture-01.png)

---

## Included Modules

| Module                                    | Description                                        | Context Propagation   |
|-------------------------------------------|----------------------------------------------------|-----------------------|
| `01-multitenant-spring-web`               | Multi-tenant with Spring MVC                       | `ThreadLocal`         |
| `02-multitenant-spring-web-virtualthread` | Multi-tenant with Java 25 Virtual Threads          | `ScopedValue`         |
| `03-multitenant-spring-webflux`           | Multi-tenant with WebFlux + Coroutines             | Reactor `Context`     |
| `04-schema-per-tenant-spring-web`         | Schema-per-tenant with one shared Hikari pool      | `ThreadLocal`         |
| `05-database-per-tenant-spring-web`       | Database-per-tenant with dedicated Hikari pools    | `ThreadLocal`         |
| `06-spring-security-tenant-authorization-spring-web` | Tenant authorization with Spring Security before database routing | `ThreadLocal` |
| `07-multitenant-ktor`                     | Multi-tenant with Ktor request plugins             | Coroutine `ThreadContextElement` |
| `08-tenant-onboarding-spring-web`         | Tenant catalog persistence and schema provisioning | Service transaction   |

Modules `02` and `06` are the two existing reference consumers for the shared
`TenantContext` work tracked in
[#255](https://github.com/bluetape4k/exposed-workshop/issues/255). The base
`io.github.bluetape4k:bluetape4k-dependencies:2.0.0-SNAPSHOT` metadata and POM
are public. Both examples now consume `io.github.bluetape4k:bluetape4k-tenant`
through the versionless catalog alias and keep only a thin application mapping
from their local tenant types to the common carrier. The exact tenant artifact
is not yet resolvable from the public snapshot repository, so local verification
uses the upstream PR artifact; no new module is added.

---

## Module Implementation Comparison

![Module Implementation Comparison diagram](../docs/images/readme-diagrams/10-multi-tenant-class-02.png)

### Key Differences by Environment

| Item             |  01 Spring MVC   |     02 Virtual Threads     |             03 WebFlux              |             04 Schema-per-Tenant             |          05 Database-per-Tenant           |          06 Spring Security Tenant Auth          |
|------------------|:----------------:|:--------------------------:|:-----------------------------------:|:--------------------------------------------:|:-----------------------------------------:|:------------------------------------------------:|
| Server           |      Tomcat      |        Tomcat + VT         |                Netty                |                    Tomcat                    |                  Tomcat                   |                      Tomcat                      |
| Thread Model     |  OS thread pool  | Virtual Thread per request |             Event loop              |                OS thread pool                |              OS thread pool               |                  OS thread pool                  |
| Context          |  `ThreadLocal`   |       `ScopedValue`        |          Reactor `Context`          |                `ThreadLocal`                 |               `ThreadLocal`               |                   `ThreadLocal`                  |
| Schema Switch    |  AOP `@Before`   |       AOP `@Before`        |    Inside `newSuspendedTransaction` |        Inside `TenantTransaction`            |                    None                   |                       None                       |
| Transaction Decl | `@Transactional` |      `@Transactional`      | `newSuspendedTransactionWithTenant` | Explicit `tenantTransaction.execute { }`     | Explicit `tenantTransaction.execute { }`  |       Explicit `tenantTransaction.execute { }`   |
| Isolation Guard  |      Schema      |           Schema           |                Schema               | Header whitelist + reset/evict on failure    | Header whitelist + no default datasource  | Authenticated tenant match + no fallback database |
| Blocking Allowed |       Yes        |            Yes             |     No (event loop must not block)  |                     Yes                      |                    Yes                    |                       Yes                        |

### Ktor and Onboarding Additions

| Module | Runtime boundary | Isolation / transaction focus |
|---|---|---|
| `07-multitenant-ktor` | Ktor request plugin + coroutine `ThreadContextElement` | Validate `X-Tenant-ID`, bind the tenant, then switch the schema inside the JDBC repository transaction. |
| `08-tenant-onboarding-spring-web` | Spring service transaction | Persist the tenant catalog after schema provisioning and drop partial resources on failure. |

---

## Common Request Flow

All modules follow the flow below. Only the context propagation mechanism differs by environment.

![Common Request Flow diagram](../docs/images/readme-diagrams/10-multi-tenant-sequence-03.png)

---

## Recommended Learning Order

1. [`01-multitenant-spring-web`](01-multitenant-spring-web/README.md) — Understand basic structure with ThreadLocal + AOP
2. [`02-multitenant-spring-web-virtualthread`](02-multitenant-spring-web-virtualthread/README.md) — Switch to ScopedValue, compare Virtual Thread configuration
3. [`03-multitenant-spring-webflux`](03-multitenant-spring-webflux/README.md) — Understand Reactor Context + coroutine bridge pattern
4. [`04-schema-per-tenant-spring-web`](04-schema-per-tenant-spring-web/README.md) — Practice explicit schema switching, reset, and connection eviction with one shared pool
5. [`05-database-per-tenant-spring-web`](05-database-per-tenant-spring-web/README.md) — Route each tenant to a dedicated datasource with no fallback database
6. [`06-spring-security-tenant-authorization-spring-web`](06-spring-security-tenant-authorization-spring-web/README.md) — Bind tenant routing to authenticated identity before database selection
7. [`07-multitenant-ktor`](07-multitenant-ktor/README.md) — Carry validated tenant context through a Ktor request plugin
8. [`08-tenant-onboarding-spring-web`](08-tenant-onboarding-spring-web/README.md) — Persist tenant metadata and provision tenant schemas with cleanup

---

## How to Run

```bash
# Individual module tests
./gradlew :01-multitenant-spring-web:test
./gradlew :02-multitenant-spring-web-virtualthread:test
./gradlew :03-multitenant-spring-webflux:test
./gradlew :04-schema-per-tenant-spring-web:test
./gradlew :05-database-per-tenant-spring-web:test
./gradlew :06-spring-security-tenant-authorization-spring-web:test
./gradlew :07-multitenant-ktor:test
./gradlew :08-tenant-onboarding-spring-web:test

# Full chapter build
./gradlew :01-multitenant-spring-web:build :02-multitenant-spring-web-virtualthread:build :03-multitenant-spring-webflux:build :04-schema-per-tenant-spring-web:build :05-database-per-tenant-spring-web:build :06-spring-security-tenant-authorization-spring-web:build :07-multitenant-ktor:build :08-tenant-onboarding-spring-web:build --no-parallel
```

---

## Test Points

- Verify failure behavior when `X-TENANT-ID` is missing or invalid.
- Confirm that tenant B data is not exposed in tenant A requests.
- Verify no context leakage under concurrent request load.
- Confirm tenant onboarding rejects duplicates and removes partially provisioned schemas after failure.
- H2-only onboarding tests stay in normal CI; container-heavy tenant modules should remain in nightly coverage.

## Performance & Stability Checkpoints

- Review schema switch cost and connection reuse policy.
- Prevent context propagation gaps when using ThreadLocal/Reactor Context.
- Ensure tenant information is not omitted from production logs for traceability.

---

## Complex Scenarios

### Schema-Based Tenant Isolation + ThreadLocal Context Propagation (Spring MVC)

`TenantFilter` extracts the tenant from the `X-TENANT-ID` header and stores it in `TenantContext` (ThreadLocal). Then `TenantSchemaAspect` switches to the corresponding schema via `SchemaUtils.setSchema()` before `@Transactional` entry.

- Related module: [`01-multitenant-spring-web`](01-multitenant-spring-web/)

### Tenant Context Propagation in Virtual Thread Environments

Virtual Threads use `ScopedValue` instead of `ThreadLocal` for context propagation. `02-multitenant-spring-web-virtualthread` replaces the executor with `TomcatVirtualThreadConfig` and binds the tenant through the shared `ScopedValueTenantContext` inside `TenantContexts`.

- Related module: [`02-multitenant-spring-web-virtualthread`](02-multitenant-spring-web-virtualthread/)

### Reactor Context Propagation in WebFlux + Coroutines

In WebFlux, tenant information is propagated to the coroutine context via Reactor `Context`. `TenantId` implements `CoroutineContext.Element` to switch the schema inside `newSuspendedTransactionWithTenant`.

- Related module: [`03-multitenant-spring-webflux`](03-multitenant-spring-webflux/)

### Explicit Schema Reset with One Shared Pool

`04-schema-per-tenant-spring-web` keeps a single Hikari pool and switches schemas only inside `TenantTransaction`. It validates `X-Tenant-ID` through a closed whitelist, resets every connection to `PUBLIC`, and evicts the connection when reset fails to prevent tenant schema leakage.

- Related module: [`04-schema-per-tenant-spring-web`](04-schema-per-tenant-spring-web/)

### Dedicated Database Routing per Tenant

`05-database-per-tenant-spring-web` creates one Hikari pool and Exposed `Database` per whitelisted tenant. `TenantTransaction` selects the database from the current `TenantContext`, so there is no default datasource fallback when a tenant is missing or unknown.

- Related module: [`05-database-per-tenant-spring-web`](05-database-per-tenant-spring-web/)

### Spring Security Tenant Authorization

`06-spring-security-tenant-authorization-spring-web` authenticates the caller
with a demo JWT, API key, or demo session header, then authorizes the requested
`X-Tenant-ID` before binding the shared `ThreadLocalTenantContext` through
`TenantContexts`. It keeps the database-per-tenant routing boundary but removes
raw header-only tenant trust from request paths.

- Related module: [`06-spring-security-tenant-authorization-spring-web`](06-spring-security-tenant-authorization-spring-web/)

### Ktor Tenant Context

`07-multitenant-ktor` validates `X-Tenant-ID` in a Ktor plugin, binds the value
through a coroutine `ThreadContextElement`, and switches the Exposed schema in
the repository transaction.

- Related module: [`07-multitenant-ktor`](07-multitenant-ktor/)

### Tenant Onboarding and Provisioning

`08-tenant-onboarding-spring-web` writes the tenant catalog only after schema
and marker-table provisioning succeeds, and removes the schema when provisioning
fails before the catalog write.

- Related module: [`08-tenant-onboarding-spring-web`](08-tenant-onboarding-spring-web/)

---

## Next Chapter

- [11-high-performance](../11-high-performance/README.md): Extend to high-performance cache/routing strategies.
