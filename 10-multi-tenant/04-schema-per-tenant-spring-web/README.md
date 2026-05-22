# Schema-per-Tenant Spring Web (04)

English | [한국어](./README.ko.md)

A Spring MVC example that keeps one shared database connection pool while isolating tenant data in separate database schemas. The example resolves a strict `X-Tenant-ID` header, maps it to a whitelisted schema, switches the Exposed transaction to that schema, and resets the connection to `PUBLIC` before it returns to the pool.

## Learning Goals

- Implement shared-database / separate-schema tenancy without a routing datasource.
- Keep schema switching inside one explicit `TenantTransaction` boundary.
- Reject unsafe tenant headers before application code runs.
- Verify tenant-local writes, connection reuse, schema reset, rollback, and connection eviction on reset failure.

## Architecture Diagram

![Schema-per-tenant Spring MVC architecture diagram](../../docs/images/readme-diagrams/10-multi-tenant-04-schema-per-tenant-spring-web-architecture-01.png)

## Request and Reset Flow

![Tenant request and schema reset sequence diagram](../../docs/images/readme-diagrams/10-multi-tenant-04-schema-per-tenant-spring-web-sequence-02.png)

## Tenant Model

| Header value | Schema        | Seed record |
|--------------|---------------|-------------|
| `acme`       | `TENANT_ACME` | `shared-widget = Acme Shared Widget` |
| `globex`     | `TENANT_GLOBEX` | `shared-widget = Globex Shared Widget` |

`TenantId` is a closed whitelist. External header values never become SQL identifiers directly. Mapped schema names are validated with `^[A-Z_][A-Z0-9_]{0,63}$` before schema creation.

## Key Implementation

### TenantFilter

`TenantFilter` is a servlet `OncePerRequestFilter`. It accepts exactly one `X-Tenant-ID` value, trims it, rejects blank, oversized, comma-separated, unknown, and path/SQL-like values by whitelist lookup, then clears `TenantContext` in `finally`.

### TenantTransaction

`TenantTransaction.execute { }` is the only schema switch boundary. It opens an Exposed JDBC transaction, records the current H2 session for tests, runs `SchemaUtils.setSchema(tenant.schema)`, executes the repository block, and resets to `PUBLIC` in `finally`.

If reset fails after successful work, the transaction rolls back first, throws `TenantSchemaResetFailedException`, and evicts the borrowed Hikari connection. If the business block already failed, the original exception remains primary and the reset failure is attached with `addSuppressed`. Rollback and eviction failures are also preserved as suppressed exceptions on the reset failure.

### InventoryRepository

The repository uses one `inventory_items` table definition. The active schema determines whether the query touches `TENANT_ACME.inventory_items` or `TENANT_GLOBEX.inventory_items`.

## Operations Notes

- This is a demo-only trust boundary: production systems should derive the tenant from authenticated identity, not from a raw client header.
- The reset failure policy favors isolation over availability. A reset failure rolls back successful work before evicting the connection to prevent schema leakage.
- The sample config sets Hikari `maximum-pool-size: 1` and `connection-init-sql: SET SCHEMA PUBLIC` so tests can prove safe reuse with the same H2 session.

## How to Test

```bash
./gradlew :04-schema-per-tenant-spring-web:test
```

## API Practice

```bash
./gradlew :04-schema-per-tenant-spring-web:bootRun

curl -H 'X-Tenant-ID: acme' http://localhost:8080/inventory/shared-widget
curl -H 'X-Tenant-ID: globex' http://localhost:8080/inventory/shared-widget

curl -X POST http://localhost:8080/inventory \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: acme' \
  -d '{"sku":"acme-local","name":"Acme Local Item","quantity":3}'
```

## Test Points

- Missing, duplicate, unknown, oversized, comma-separated, path-like, and SQL-like tenant headers return `400`.
- `acme` and `globex` return different rows for the same SKU.
- A row inserted for one tenant is not visible to the other tenant.
- Tenant context is cleared after downstream failures.
- Pool size `1` still reuses the same H2 session safely because every transaction resets to `PUBLIC`.
- Reset failures roll back successful work, evict the connection, return `503` over HTTP, and preserve original business failures as primary exceptions.

## Related Modules

- [`01-multitenant-spring-web`](../01-multitenant-spring-web/README.md): Spring MVC with ThreadLocal context and AOP schema switching.
- [`02-multitenant-spring-web-virtualthread`](../02-multitenant-spring-web-virtualthread/README.md): Virtual-thread variant with `ScopedValue`.
- [`03-multitenant-spring-webflux`](../03-multitenant-spring-webflux/README.md): WebFlux + coroutine context propagation.
