# 10 Multi-Tenant (Production)

English | [한국어](./README.ko.md)

A chapter for implementing production-grade multi-tenant architecture with Exposed + Spring, covering schema-based tenant isolation, dynamic routing, and context propagation flows. Compares how the same multi-tenancy requirements are implemented across three environments: Spring MVC, Virtual Thread, and WebFlux.

## Chapter Goals

- Understand the full flow of tenant identification, propagation, and isolation.
- Compare implementation differences across Spring MVC, Virtual Thread, and WebFlux environments.
- Establish validation points to prevent leakage and isolation failures in production.

## Prerequisites

- Contents of `09-spring`
- Basic concepts of transactions and DataSource routing

---

## Multi-Tenancy Strategy Overview

This chapter uses the **Shared Database / Separate Schema** strategy as its foundation. Data is isolated by separating per-tenant schemas (`korean`, `english`) within a single DB instance.

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

A `TenantAwareDataSource` (extending `AbstractRoutingDataSource`) is provided so you can also switch to a **Database per Tenant** approach.

### Per-Tenant Schema Isolation Architecture

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
flowchart TD
    Request[HTTP Request] --> TenantResolver[Tenant Resolver]
    TenantResolver --> |tenant_id extracted| TenantContext[TenantContext\nThreadLocal / ScopedValue / ReactorContext]
    TenantContext --> RoutingDS[RoutingDataSource]
    RoutingDS --> |tenant_a| SchemaA[(Schema: korean\nMovies, Actors)]
    RoutingDS --> |tenant_b| SchemaB[(Schema: english\nMovies, Actors)]

    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef purple fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100

    class Request blue
    class TenantResolver green
    class TenantContext purple
    class RoutingDS purple
    class SchemaA,SchemaB orange
```

---

## Included Modules

| Module                                    | Description                                        | Context Propagation   |
|-------------------------------------------|----------------------------------------------------|----------------------|
| `01-multitenant-spring-web`               | Multi-tenant with Spring MVC                       | `ThreadLocal`         |
| `02-multitenant-spring-web-virtualthread` | Multi-tenant with Java 21 Virtual Threads          | `ScopedValue`         |
| `03-multitenant-spring-webflux`           | Multi-tenant with WebFlux + Coroutines             | Reactor `Context`     |

---

## Module Implementation Comparison

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
classDiagram
    class TenantFilter_MVC {
        jakarta.servlet.Filter
        ThreadLocal 바인딩
    }
    class TenantFilter_VT {
        jakarta.servlet.Filter
        ScopedValue 바인딩
    }
    class TenantFilter_WebFlux {
        WebFilter (Reactor)
        contextWrite()
    }

    class TenantContext_MVC {
        ThreadLocal~Tenant~
        withTenant() + finally clear()
    }
    class TenantContext_VT {
        ScopedValue~Tenant~
        ScopedValue.where().run()
    }
    class TenantId_WebFlux {
        CoroutineContext.Element
        ReactorContext 브릿지
    }

    class SchemaAspect_MVC {
        TenantSchemaAspect
        AOP @Before @Transactional
        setSchema()
    }
    class SchemaAspect_VT {
        TransactionSchemaAspect
        AOP @Before @Transactional
        createSchema() + setSchema()
    }
    class SuspendedTx_WebFlux {
        newSuspendedTransactionWithTenant()
        Dispatchers.IO + TenantId
        setSchema()
    }

    TenantFilter_MVC --> TenantContext_MVC
    TenantFilter_VT --> TenantContext_VT
    TenantFilter_WebFlux --> TenantId_WebFlux

    TenantContext_MVC --> SchemaAspect_MVC
    TenantContext_VT --> SchemaAspect_VT
    TenantId_WebFlux --> SuspendedTx_WebFlux

    style TenantFilter_MVC fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style TenantFilter_VT fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style TenantFilter_WebFlux fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style TenantContext_MVC fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style TenantContext_VT fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style TenantId_WebFlux fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style SchemaAspect_MVC fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style SchemaAspect_VT fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style SuspendedTx_WebFlux fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
```

### Key Differences by Environment

| Item             |  01 Spring MVC   |     02 Virtual Threads     |             03 WebFlux              |
|----------------|:----------------:|:--------------------------:|:-----------------------------------:|
| Server          |      Tomcat      |        Tomcat + VT         |                Netty                |
| Thread Model    |  OS thread pool  | Virtual Thread per request |             Event loop              |
| Context         |  `ThreadLocal`   |       `ScopedValue`        |          Reactor `Context`          |
| Schema Switch   |  AOP `@Before`   |       AOP `@Before`        |    Inside `newSuspendedTransaction` |
| Transaction Decl| `@Transactional` |      `@Transactional`      | `newSuspendedTransactionWithTenant` |
| Blocking Allowed|       Yes        |            Yes             |     No (event loop must not block)  |

---

## Common Request Flow

All modules follow the flow below. Only the context propagation mechanism differs by environment.

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"', 'actorBkg': '#E3F2FD', 'actorBorder': '#90CAF9', 'actorTextColor': '#1565C0', 'actorLineColor': '#90CAF9', 'activationBkgColor': '#E8F5E9', 'activationBorderColor': '#A5D6A7', 'labelBoxBkgColor': '#FFF3E0', 'labelBoxBorderColor': '#FFCC80', 'labelTextColor': '#E65100', 'loopTextColor': '#6A1B9A', 'noteBkgColor': '#F3E5F5', 'noteBorderColor': '#CE93D8', 'noteTextColor': '#6A1B9A', 'signalColor': '#1565C0', 'signalTextColor': '#1565C0'}}}%%
sequenceDiagram
    participant Client
    participant Filter
    participant Context
    participant Controller
    participant Repository
    participant DB

    Client->>Filter: GET /actors (X-TENANT-ID: {tenant})
    Filter->>Context: Bind tenant (ThreadLocal / ScopedValue / ReactorContext)
    Filter->>Controller: Forward request
    Controller->>DB: Switch schema (setSchema({tenant}))
    Controller->>Repository: Fetch data
    Repository->>DB: SELECT * FROM {tenant}.actor
    DB-->>Client: Tenant-isolated response
    Note over Filter,Context: Clean up context after request completes
```

---

## Recommended Learning Order

1. [`01-multitenant-spring-web`](01-multitenant-spring-web/README.md) — Understand basic structure with ThreadLocal + AOP
2. [`02-multitenant-spring-web-virtualthread`](02-multitenant-spring-web-virtualthread/README.md) — Switch to ScopedValue, compare Virtual Thread configuration
3. [`03-multitenant-spring-webflux`](03-multitenant-spring-webflux/README.md) — Understand Reactor Context + coroutine bridge pattern

---

## How to Run

```bash
# Individual module tests
./gradlew :10-multi-tenant:01-multitenant-spring-web:test
./gradlew :10-multi-tenant:02-multitenant-spring-web-virtualthread:test
./gradlew :10-multi-tenant:03-multitenant-spring-webflux:test

# Full chapter build
./gradlew :10-multi-tenant:build
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

---

## Next Chapter

- [11-high-performance](../11-high-performance/README.md): Extend to high-performance cache/routing strategies.
