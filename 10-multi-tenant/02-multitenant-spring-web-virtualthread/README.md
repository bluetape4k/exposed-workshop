# Exposed + Spring Web + Virtual Threads + Multi-Tenant (02)

English | [한국어](./README.ko.md)

An example that extends the multi-tenancy structure from module `01` to a Java 25 Virtual Threads environment. Focuses on a configuration that increases concurrent throughput while retaining blocking I/O style. Uses the shared `bluetape4k-tenant` `ScopedValueTenantContext` instead of a module-local `ThreadLocal` implementation for Virtual Thread-friendly context propagation.

## Learning Goals

- Understand the Virtual Thread-based request processing model.
- Compare context propagation differences between `ThreadLocal` and `ScopedValue`.
- Learn how `TransactionSchemaAspect` handles schema creation and switching simultaneously.
- Verify isolation/stability under increased concurrency.

## Prerequisites

- [`../01-multitenant-spring-web/README.md`](../01-multitenant-spring-web/README.md)
- Java 25 Virtual Threads basics

## Dependency

The module uses the shared tenant carrier from the `2.0.0-SNAPSHOT` dependency
line. The catalog alias is versionless; `bluetape4k-dependencies` and its BOM
remain the version authority.

```kotlin
implementation(libs.bluetape4k.tenant)
```

---

## Domain Model

![Domain Model diagram](../../docs/images/readme-diagrams/10-multi-tenant-02-multitenant-spring-web-virtualthread-erd-01.png)

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

![02 multitenant spring web virtualthread Class Structure 2 diagram](../../docs/images/readme-diagrams/10-multi-tenant-02-multitenant-spring-web-virtualthread-class-02.png)

### ScopedValue-Based Context Propagation

Virtual Threads can be created in the millions concurrently, making `ThreadLocal`'s memory overhead problematic. Java 25's `ScopedValue` operates as an immutable binding, making it well-suited for Virtual Thread environments.

```
ThreadLocal  → Mutable, requires manual clear()
ScopedValue  → Immutable binding, automatically destroyed when scope exits
```

### Platform Thread vs Virtual Thread Comparison

![Platform Thread vs Virtual Thread Comparison diagram](../../docs/images/readme-diagrams/10-multi-tenant-02-multitenant-spring-web-virtualthread-architecture-03.png)

---

## Request Flow

![Request Flow diagram](../../docs/images/readme-diagrams/10-multi-tenant-02-multitenant-spring-web-virtualthread-sequence-04.png)

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

### TenantContexts (shared ScopedValue carrier)

`TenantContexts` is a thin application boundary around the shared
`ScopedValueTenantContext` from `bluetape4k-tenant`. The application keeps header
parsing and `Tenants` lookup; the common carrier owns lexical binding and has no
default tenant. Values are only valid inside the carrier scope and automatically
disappear when the block ends.

```kotlin
object TenantContexts {
    private val delegate = ScopedValueTenantContext()

    fun currentOrNull(): Tenant? = delegate.currentOrNull()?.let { Tenants.getById(it.value) }
    fun current(): Tenant = Tenants.getById(delegate.requireCurrent().value)
    fun <T> withTenant(tenant: Tenant, block: () -> T): T =
        delegate.withTenant(BluetapeTenantId(tenant.id), block)
}
```

### TransactionSchemaAspect

Performs the same role as `TenantSchemaAspect` from module `01`, but additionally calls `SchemaUtils.createSchema()` to automatically create schemas if they don't exist. Prevents schema initialization races when concurrent requests flood in under Virtual Thread environments.

```kotlin
@Before("@within(...Transactional) || @annotation(...Transactional)")
fun setSchemaForTransaction() {
    transaction {
        val schema = getSchemaDefinition(TenantContexts.current())
        SchemaUtils.createSchema(schema)  // Additional compared to module 01
        SchemaUtils.setSchema(schema)
        commit()
    }
}
```

### TenantFilter

Uses the same servlet filter interface as module `01`, but internally
`TenantContexts.withTenant()` delegates to the shared `ScopedValueTenantContext`.
The filter still owns the explicit application policy for missing or unknown
headers; the common carrier itself never substitutes a tenant.

---

## Key Components Summary

| File                                    | Role                                          |
|---------------------------------------|-----------------------------------------------|
| `config/TomcatVirtualThreadConfig.kt` | Replace Tomcat executor with Virtual Thread   |
| `tenant/TenantFilter.kt`              | Extract tenant from header, bind the shared ScopedValue carrier |
| `tenant/TenantContexts.kt`            | Map application tenants to `ScopedValueTenantContext`         |
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
./gradlew :02-multitenant-spring-web-virtualthread:test

# Start application
./gradlew :02-multitenant-spring-web-virtualthread:bootRun
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
- Confirm `currentOrNull()` returns `null` and `current()` throws `MissingTenantContextException` outside the `ScopedValue` scope
- Measure latency changes when thread pool/connection pool settings are modified

## Operations Checkpoints

- Increasing Virtual Threads alone does not resolve DB bottlenecks — tune HikariCP `maximumPoolSize` together
- `ScopedValue` bindings are immutable within a scope; nested scopes may temporarily rebind a tenant and restore the outer binding when they end — finalize flow design upfront
- Ensure no long-running blocking tasks are placed in the request path
- Fix integration tests for tenant leak detection in CI

---

## Next Module

- [`../03-multitenant-spring-webflux/README.md`](../03-multitenant-spring-webflux/README.md): Non-blocking multi-tenant with WebFlux + Coroutines
