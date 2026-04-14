# Exposed + Spring Web + Virtual Threads + Multi-Tenant (02)

English | [한국어](./README.ko.md)

An example that extends the multi-tenancy structure from module `01` to a Java 21 Virtual Threads environment. Focuses on a configuration that increases concurrent throughput while retaining blocking I/O style. Uses `ScopedValue` instead of `ThreadLocal` for Virtual Thread-friendly context propagation.

## Learning Goals

- Understand the Virtual Thread-based request processing model.
- Compare context propagation differences between `ThreadLocal` and `ScopedValue`.
- Learn how `TransactionSchemaAspect` handles schema creation and switching simultaneously.
- Verify isolation/stability under increased concurrency.

## Prerequisites

- [`../01-multitenant-spring-web/README.md`](../01-multitenant-spring-web/README.md)
- Java 21 Virtual Threads basics

---

## Domain Model

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
erDiagram
    MovieTable {
        BIGSERIAL id PK
        VARCHAR(255) name
        VARCHAR(255) producerName
        DATE releaseDate
    }
    ActorTable {
        BIGSERIAL id PK
        VARCHAR(255) firstName
        VARCHAR(255) lastName
        DATE birthday
    }
    ActorInMovieTable {
        BIGINT movieId FK
        BIGINT actorId FK
    }

    MovieTable ||--o{ ActorInMovieTable : "has"
    ActorTable ||--o{ ActorInMovieTable : "appears in"
```

---

## Key Differences from Module 01

| Item         | 01 (Spring MVC)                   | 02 (Virtual Threads)                                 |
|------------|-----------------------------------|------------------------------------------------------|
| Thread Model | OS thread pool (Tomcat default)  | Virtual Thread per request                           |
| Context Storage | `ThreadLocal`                 | `ScopedValue`                                        |
| Schema Aspect | `TenantSchemaAspect` (setSchema only) | `TransactionSchemaAspect` (createSchema + setSchema) |
| Tomcat Config | Default                        | `TomcatVirtualThreadConfig`                          |

---

## Architecture

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
classDiagram
    class TomcatVirtualThreadConfig {
        +protocolHandlerVirtualThreadExecutorCustomizer()
    }

    class TenantFilter {
        +TENANT_HEADER: String
        +doFilter(request, response, chain)
        -extractTenant(request) Tenant
    }

    class TenantContext {
        +CURRENT_TENANT: ScopedValue~Tenant~
        +getCurrentTenant() Tenant
        +getCurrentTenantSchema() Schema
        +withTenant(tenant, block)
    }

    class Tenants {
        +DEFAULT_TENANT: Tenant
        +getById(tenantId) Tenant
    }

    class Tenant {
        <<enumeration>>
        KOREAN
        ENGLISH
        +id: String
    }

    class TenantAwareDataSource {
        +determineCurrentLookupKey() Any
    }

    class TransactionSchemaAspect {
        +setSchemaForTransaction()
    }

    class TenantInitializer {
        +onApplicationEvent(event)
    }

    class DataInitializer {
        +initialize()
    }

    class ActorController {
        +getAllActors() List~ActorRecord~
        +findById(id) ActorRecord
    }

    TomcatVirtualThreadConfig --> Executors : newVirtualThreadPerTaskExecutor()
    TenantFilter --> TenantContext : withTenant()
    TenantContext --> Tenants : lookup tenant
    Tenants --> Tenant : enum member
    TenantContext --> ScopedValue : where().run()
    TenantAwareDataSource --> TenantContext : getCurrentTenant()
    TransactionSchemaAspect --> TenantContext : getCurrentTenantSchema()
    TransactionSchemaAspect --> SchemaUtils : createSchema() + setSchema()
    TenantInitializer --> DataInitializer : initialize()
    ActorController --> TransactionSchemaAspect : @Transactional(AOP)

    style TomcatVirtualThreadConfig fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style TenantFilter fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style TenantContext fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style Tenants fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style Tenant fill:#FFFDE7,stroke:#FFF176,color:#F57F17
    style TenantAwareDataSource fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style TransactionSchemaAspect fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style TenantInitializer fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style DataInitializer fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style ActorController fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
```

### ScopedValue-Based Context Propagation

Virtual Threads can be created in the millions concurrently, making `ThreadLocal`'s memory overhead problematic. Java 21's `ScopedValue` operates as an immutable binding, making it well-suited for Virtual Thread environments.

```
ThreadLocal  → Mutable, requires manual clear()
ScopedValue  → Immutable binding, automatically destroyed when scope exits
```

### Platform Thread vs Virtual Thread Comparison

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
flowchart LR
    subgraph PT["Platform Thread (Module 01)"]
        PT1[Thread-1] --> DB1[(Schema: korean)]
        PT2[Thread-2] --> DB2[(Schema: english)]
    end
    subgraph VT["Virtual Thread (Module 02)"]
        VT1[VThread-1] --> DB3[(Schema: korean)]
        VT2[VThread-2] --> DB3
        VT3[VThread-3] --> DB4[(Schema: english)]
        VT4[VThread-n...] --> DB4
    end

    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100

    class PT1,PT2 blue
    class VT1,VT2,VT3,VT4 green
    class DB1,DB2,DB3,DB4 orange
```

---

## Request Flow

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"', 'actorBkg': '#E3F2FD', 'actorBorder': '#90CAF9', 'actorTextColor': '#1565C0', 'actorLineColor': '#90CAF9', 'activationBkgColor': '#E8F5E9', 'activationBorderColor': '#A5D6A7', 'labelBoxBkgColor': '#FFF3E0', 'labelBoxBorderColor': '#FFCC80', 'labelTextColor': '#E65100', 'loopTextColor': '#6A1B9A', 'noteBkgColor': '#F3E5F5', 'noteBorderColor': '#CE93D8', 'noteTextColor': '#6A1B9A', 'signalColor': '#1565C0', 'signalTextColor': '#1565C0'}}}%%
sequenceDiagram
    participant Client
    participant Tomcat
    participant TenantFilter
    participant TenantContext
    participant TransactionSchemaAspect
    participant ActorController
    participant ActorExposedRepository
    participant Database

    Client->>Tomcat: GET /actors (X-TENANT-ID: english)
    Note over Tomcat: VirtualThread assigned (newVirtualThreadPerTaskExecutor)

    Tomcat->>TenantFilter: doFilter()
    TenantFilter->>TenantContext: withTenant(ENGLISH) { ... }
    Note over TenantContext: ScopedValue.where(CURRENT_TENANT, ENGLISH).run { }

    TenantFilter->>ActorController: chain.doFilter()
    ActorController->>TransactionSchemaAspect: @Transactional entry (AOP Before)
    TransactionSchemaAspect->>TenantContext: getCurrentTenantSchema()
    TransactionSchemaAspect->>Database: SchemaUtils.createSchema("english")
    TransactionSchemaAspect->>Database: SchemaUtils.setSchema("english")

    ActorController->>ActorExposedRepository: findAll()
    ActorExposedRepository->>Database: SELECT * FROM english.actor
    Database-->>ActorExposedRepository: ResultSet
    ActorExposedRepository-->>ActorController: List~ActorRecord~
    ActorController-->>Client: 200 OK [{ "firstName": "Johnny", ... }]

    Note over TenantContext: ScopedValue scope ends automatically (no explicit clear needed)
```

---

## Key Implementation

### TomcatVirtualThreadConfig

Replaces Tomcat's `ProtocolHandler` executor with `Executors.newVirtualThreadPerTaskExecutor()` when `spring.threads.virtual.enabled=true` (the default). Minimal configuration to activate Virtual Threads without changing existing code.

```kotlin
@Bean
fun protocolHandlerVirtualThreadExecutorCustomizer(): TomcatProtocolHandlerCustomizer<*> {
    return TomcatProtocolHandlerCustomizer<ProtocolHandler> { protocolHandler ->
        protocolHandler.executor = Executors.newVirtualThreadPerTaskExecutor()
    }
}
```

### TenantContext (ScopedValue Version)

A version of the `ThreadLocal` approach from module `01` replaced with `ScopedValue`. Values are only valid inside the `ScopedValue.where().run { }` block and automatically disappear when the block ends.

```kotlin
object TenantContext {
    val CURRENT_TENANT: ScopedValue<Tenant> = ScopedValue.newInstance()

    inline fun withTenant(tenant: Tenants.Tenant = getCurrentTenant(), crossinline block: () -> Unit) {
        ScopedValue.where(CURRENT_TENANT, tenant).run {
            block()
        }
    }
}
```

### TransactionSchemaAspect

Performs the same role as `TenantSchemaAspect` from module `01`, but additionally calls `SchemaUtils.createSchema()` to automatically create schemas if they don't exist. Prevents schema initialization races when concurrent requests flood in under Virtual Thread environments.

```kotlin
@Before("@within(...Transactional) || @annotation(...Transactional)")
fun setSchemaForTransaction() {
    transaction {
        val schema = TenantContext.getCurrentTenantSchema()
        SchemaUtils.createSchema(schema)  // Additional compared to module 01
        SchemaUtils.setSchema(schema)
        commit()
    }
}
```

### TenantFilter

Uses the same servlet filter interface as module `01`, but internally `TenantContext.withTenant()` operates on a `ScopedValue` basis.

---

## Key Components Summary

| File                                    | Role                                          |
|---------------------------------------|-----------------------------------------------|
| `config/TomcatVirtualThreadConfig.kt` | Replace Tomcat executor with Virtual Thread   |
| `tenant/TenantFilter.kt`              | Extract tenant from header, bind ScopedValue  |
| `tenant/TenantContext.kt`             | ScopedValue-based tenant store                |
| `tenant/Tenants.kt`                   | Tenant enum + schema mapping                  |
| `tenant/SchemaSupport.kt`             | Helper for creating `Schema` objects          |
| `tenant/TransactionSchemaAspect.kt`   | Schema creation/switching before transaction via AOP |
| `tenant/TenantAwareDataSource.kt`     | Tenant-based DataSource routing               |
| `tenant/TenantInitializer.kt`         | Schema/data initialization on app startup     |
| `tenant/DataInitializer.kt`           | Schema creation + sample data insertion       |
| `config/ExposedMultitenantConfig.kt`  | DataSource/Database bean configuration        |
| `controller/ActorController.kt`       | Actor query REST API                          |

---

## How to Test

```bash
# Run module tests
./gradlew :10-multi-tenant:02-multitenant-spring-web-virtualthread:test

# Start application
./gradlew :10-multi-tenant:02-multitenant-spring-web-virtualthread:bootRun
```

### API Practice

```bash
# Korean tenant actor list
curl -H 'X-TENANT-ID: korean' http://localhost:8080/actors

# English tenant actor list
curl -H 'X-TENANT-ID: english' http://localhost:8080/actors

# Query specific actor
curl -H 'X-TENANT-ID: english' http://localhost:8080/actors/1
```

---

## Practice Checklist

- Verify response data differs between `X-TENANT-ID: korean` and `X-TENANT-ID: english`
- Verify that tenant data does not cross over even as concurrent request count increases
- Confirm default value returned when `getCurrentTenant()` is called outside `ScopedValue` scope
- Measure latency changes when thread pool/connection pool settings are modified

## Operations Checkpoints

- Increasing Virtual Threads alone does not resolve DB bottlenecks — tune HikariCP `maximumPoolSize` together
- `ScopedValue` is immutable so tenant cannot be changed after binding — finalize flow design upfront
- Ensure no long-running blocking tasks are placed in the request path
- Fix integration tests for tenant leak detection in CI

---

## Next Module

- [`../03-multitenant-spring-webflux/README.md`](../03-multitenant-spring-webflux/README.md): Non-blocking multi-tenant with WebFlux + Coroutines
