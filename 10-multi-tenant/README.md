# 10 Multi-Tenant (Production)

English | [한국어](./README.ko.md)

A chapter for implementing production-grade multi-tenant architecture with Exposed + Spring, covering schema-based tenant isolation, database-per-tenant routing, dynamic routing, and context propagation flows. Compares how the same multi-tenancy requirements are implemented across Spring MVC, Virtual Thread, WebFlux, explicit schema-per-tenant transaction boundaries, and dedicated tenant databases.

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

```
Single DB Instance
├── Schema: korean
│   ├── actor
│   ├── movie
│   └── actor_in_movie
└── Schema: english
    ├── actor
    ├── movie
    └── actor_in_movie
```

It then compares that baseline with **Database per Tenant**, where each whitelisted tenant owns a dedicated Hikari pool and Exposed `Database`.

### Per-Tenant Schema Isolation Architecture

![Per-Tenant Schema Isolation Architecture diagram](../docs/images/readme-diagrams/10-multi-tenant-architecture-01.png)

---

## Included Modules

| Module                                    | Description                                        | Context Propagation   |
|-------------------------------------------|----------------------------------------------------|-----------------------|
| `01-multitenant-spring-web`               | Multi-tenant with Spring MVC                       | `ThreadLocal`         |
| `02-multitenant-spring-web-virtualthread` | Multi-tenant with Java 21 Virtual Threads          | `ScopedValue`         |
| `03-multitenant-spring-webflux`           | Multi-tenant with WebFlux + Coroutines             | Reactor `Context`     |
| `04-schema-per-tenant-spring-web`         | Schema-per-tenant with one shared Hikari pool      | `ThreadLocal`         |
| `05-database-per-tenant-spring-web`       | Database-per-tenant with dedicated Hikari pools    | `ThreadLocal`         |
| `06-spring-security-tenant-authorization-spring-web` | Tenant authorization with Spring Security before database routing | `ThreadLocal` |

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

# Full chapter build
./gradlew :01-multitenant-spring-web:build :02-multitenant-spring-web-virtualthread:build :03-multitenant-spring-webflux:build :04-schema-per-tenant-spring-web:build :05-database-per-tenant-spring-web:build :06-spring-security-tenant-authorization-spring-web:build
```

---

## Test Points

- Verify failure behavior when `X-TENANT-ID` is missing or invalid.
- Confirm that tenant B data is not exposed in tenant A requests.
- Verify no context leakage under concurrent request load.

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

Virtual Threads use `ScopedValue` instead of `ThreadLocal` for context propagation. `02-multitenant-spring-web-virtualthread` replaces the executor with `TomcatVirtualThreadConfig` and binds the tenant using a `ScopedValue.where().run { }` block.

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
`X-Tenant-ID` before setting `TenantContext`. It keeps the database-per-tenant
routing boundary but removes raw header-only tenant trust from request paths.

- Related module: [`06-spring-security-tenant-authorization-spring-web`](06-spring-security-tenant-authorization-spring-web/)

---

## Next Chapter

- [11-high-performance](../11-high-performance/README.md): Extend to high-performance cache/routing strategies.
