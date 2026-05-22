# Issue 52 Schema-Per-Tenant Design

## Context

Issue #52 adds a chapter 10 Spring Boot example for tenant-specific schema
routing on shared database infrastructure. The example should focus on strict
tenant resolution, schema switching, tenant-local persistence, and cross-tenant
isolation.

## Scope

- Add `10-multi-tenant/04-schema-per-tenant-spring-web`.
- Resolve tenants from `X-Tenant-ID` with explicit missing and unknown tenant
  handling.
- Use one shared Hikari datasource and Exposed JDBC transactions.
- Create one schema per tenant and run table operations after selecting the
  tenant schema.
- Test missing tenant, unknown tenant, valid tenant routing, and isolation.
- Add README pairs and committed Architecture Diagram PNG/SVG.

## Design

The module uses a strict servlet filter to set a request-local `TenantContext`.
Unlike the earlier chapter 10 baseline modules, missing tenant headers fail with
HTTP 400 instead of falling back to a default tenant. Repository operations use
one `tenantTransaction { }` wrapper before touching the shared `InventoryItems`
table. Repositories must not issue ad hoc schema switching calls.

Every Exposed transaction MUST explicitly `SET SCHEMA` at the start; no code may
implicitly rely on a pooled connection's prior schema state. The helper also
resets the schema to `PUBLIC` before the transaction returns so Hikari connection
reuse cannot expose a previous tenant's schema to the next borrower.
Schema switching must use Exposed `Schema` objects and `SchemaUtils.setSchema`;
raw SQL such as `exec("SET SCHEMA $name")` is forbidden even for whitelisted
tenant values.
Mapped schema identifiers must match `^[A-Z_][A-Z0-9_]{0,63}$` before a
`Schema` object is created.

If resetting to `PUBLIC` fails, the underlying JDBC connection must not be
silently returned to the pool as healthy. The reset-failure path must evict the
current Hikari connection through `HikariDataSource.evictConnection(connection)`
or an equivalent pool-level eviction hook before rethrowing the failure.
Tests must prove a later tenant request cannot inherit the previous tenant
schema after reset failure handling.
The helper obtains the physical JDBC connection from the current Exposed
transaction via `TransactionManager.current().connection.connection as
java.sql.Connection`; this is the Hikari-borrowed proxy connection handed to
Exposed, not an unwrapped physical driver connection. `tenantTransaction` must
use this shape:
`transaction { try { SchemaUtils.setSchema(tenantSchema); block() } finally { resetter.resetToPublic(proxyConnection) } }`.
The reset therefore runs while the connection is still bound to the active
Exposed transaction, before Exposed commits, rolls back, or releases it.
Reset behavior is implemented behind a `SchemaResetter` seam so tests can inject
a failing reset and assert that the connection eviction hook is invoked.
When reset fails, the implementation must mark the current transaction for
rollback or call rollback on the active transaction before evicting the
Hikari-borrowed proxy connection. If Exposed later raises a secondary
commit/rollback/connection-closed error, the original reset failure must remain
the primary exception and the secondary failure must be attached with
`addSuppressed`.
If `block()` throws and `resetToPublic()` also throws, the original `block()`
failure remains primary, the reset failure is added as suppressed, and the
connection is evicted before rethrowing the original failure. If `block()`
succeeds but reset fails, `tenantTransaction` rolls back the business work to
preserve isolation, evicts the connection, logs a warning with tenant and
operation context, and throws `TenantSchemaResetFailedException`.

Tenant headers are mapped only through the `TenantId` whitelist. The raw request
header is never concatenated into SQL; missing, blank, or unknown tenant values
are rejected before schema selection.
Tenant header values longer than 64 characters are rejected before lookup.
The servlet filter must wrap downstream handling in `try/finally` and always
call `TenantContext.clear()`. This example is servlet-synchronous only; it does
not support async servlet dispatch, WebFlux, `@Async`, or coroutine context
propagation.

Tenants:

- `acme` -> `TENANT_ACME`
- `globex` -> `TENANT_GLOBEX`

The sample domain is intentionally small: inventory items keyed by SKU. Seed
data differs per schema so tests can prove that each tenant reads its own data.
An `ApplicationRunner` creates both tenant schemas idempotently before inserting
seed data, so repeated test/application starts do not race DDL. The runner must
complete during Spring application startup; integration tests only run after the
`ApplicationContext` is ready.

Security model: this example trusts `X-Tenant-ID` only to demonstrate routing.
Production systems must derive the tenant from an authenticated session, token,
or server-side account mapping instead of trusting a caller-controlled header.
The reset target `PUBLIC` is H2-specific. PostgreSQL uses `public` plus
`search_path` semantics, so production PostgreSQL implementations must adapt the
reset strategy instead of copying the H2 command blindly.
README files must call out the rollback-on-reset-failure trade-off: if a
successful business write cannot safely reset the connection schema afterward,
the example discards the write to preserve tenant isolation.

## Verification

- `./gradlew projects` must discover `:04-schema-per-tenant-spring-web`.
- `./gradlew :04-schema-per-tenant-spring-web:test`.
- Isolation tests must force Hikari connection reuse with
  `maximumPoolSize=1` and `minimumIdle=1`, then prove that tenant A work followed
  by tenant B work cannot see tenant A data.
- Isolation assertions must include tenant A inserting a unique SKU and tenant B
  receiving 404 or an empty result for that exact SKU on the reused connection.
- Reset-failure tests must simulate a failing `PUBLIC` reset, verify the
  eviction hook is invoked, and verify the next tenant request remains isolated.
- Reuse tests must assert the same physical Hikari connection is reused, for
  example by recording `System.identityHashCode` of
  `TransactionManager.current().connection.connection`.
- Eviction tests must treat that connection as the Hikari proxy and verify pool
  eviction with Hikari pool stats or a changed proxy identity on the next borrow,
  while preserving the original reset failure as the primary exception.
- Reset-failure tests must assert `addSuppressed` behavior for both
  `block()`-failure plus reset-failure and reset-failure plus Exposed cleanup
  secondary failure cases when those paths are exercised.
- README scan must show committed PNG Architecture Diagram links and no Mermaid
  blocks.
- `.github/workflows/examples.yml` must build the new module.
- Nightly DB shard decision must be recorded; the module is H2-focused and does
  not require a new Testcontainers shard.
