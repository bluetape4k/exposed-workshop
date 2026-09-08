# Tenant JDBC Support

English | [한국어](./README.ko.md)

`tenant-jdbc-support` is the Spring-independent support module for the
database-per-tenant Spring MVC examples. It keeps Spring
`@ConfigurationProperties` binding in each consumer while centralizing the
pure JDBC settings and provider wiring that must behave identically.

## Responsibilities

- `TenantJdbcSettings` validates JDBC/H2 lifecycle options and creates a
  configured `HikariDataSource`.
- `normalizeTenantJdbcSettings` maps consumer tenant keys to domain keys,
  rejects normalized duplicates, and rejects missing expected tenants.
- `toTenantJdbcResourceRegistry` validates settings and connects the normalized
  map to the published `TenantJdbcResourceRegistry` factory and disposer.

The helper is not a Spring configuration module. Consumers own the
`@ConfigurationProperties` wrapper and tenant parser, while the provider owns
the returned `DataSource` and Exposed `Database` after factory creation.

## Dependency

The provider API is pinned to the exact mutable snapshot selected by the
workshop catalog:

```kotlin
api(libs.exposed.tenant.jdbc.snapshot)
```

The version directory remains `2.1.0-SNAPSHOT`; the catalog resolves the
published build `2.1.0-20260907.153611-1` through Sonatype metadata.

## Example

```kotlin
@ConfigurationProperties(prefix = "app")
data class TenantDataSourceProperties(
    val tenants: Map<String, TenantJdbcSettings> = emptyMap(),
)

val registry = properties.tenants.toTenantJdbcResourceRegistry(
    parseTenant = TenantId::fromHeader,
    expectedTenants = TenantId.entries,
    tenantName = TenantId::headerValue,
)
```

The adapter fails before registry creation when a key normalizes to an existing
tenant, for example `acme` and ` ACME `. It also fails when any expected tenant
is absent. `close()` remains the provider registry's idempotent lifecycle
boundary; consumers must drain in-flight requests before Spring destroys the
bean.

## Test

```bash
./gradlew :tenant-jdbc-support:test --rerun-tasks --no-build-cache --no-daemon
```

The tests cover settings validation, Hikari defaults, normalized duplicate
keys, missing tenants, and provider-owned datasource disposal.
