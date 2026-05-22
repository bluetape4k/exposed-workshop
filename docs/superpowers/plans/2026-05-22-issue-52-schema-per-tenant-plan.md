# Issue 52 Schema-Per-Tenant Plan

## Steps

1. Add the Spring Boot module skeleton and dependencies.
2. Implement tenant resolution, `tenantTransaction { }`, Exposed repository,
   service, controller, and seed initializer. `tenantTransaction { }` must use
   `SchemaUtils.setSchema(Schema(...))`, must not concatenate raw tenant strings
   into SQL, must set the tenant schema at transaction start, and must reset to
   `PUBLIC` before returning. If reset fails, it must invoke a Hikari connection
   eviction hook before rethrowing the original reset failure. Add
   `SchemaResetter`, `ConnectionEvictor`, and connection-probe seams for
   reset-failure and same-connection reuse tests. The eviction target is the
   Hikari-borrowed proxy connection from
   `TransactionManager.current().connection.connection`, not an unwrapped driver
   connection.
3. Add focused Spring Boot tests for request errors, valid routing, and
   cross-tenant isolation. Configure the test datasource with a small Hikari
   pool (`maximumPoolSize=1`, `minimumIdle=1`) to force connection reuse across
   tenant requests.
4. Add English/Korean README files and a committed Architecture Diagram PNG/SVG.
5. Wire the module into the Examples workflow and update chapter/root docs if
   needed.
6. Run local verification and 6-Tier review before opening the PR.

## Acceptance Mapping

- Builds independently: targeted Gradle test/build task.
- Tenant isolation: same SKU can exist with different values per tenant, and a
  new tenant-local SKU is not visible in another schema.
- Error handling: missing/unknown tenant headers return HTTP 400.
- Context safety: `TenantContext` is always cleared even when downstream request
  handling fails.
- Reset safety: a tenant A request followed by tenant B on a reused connection
  cannot read tenant A data, and the current schema is reset to `PUBLIC` after
  a tenant transaction.
- Reset failure: a test-only schema reset failure invokes the eviction hook and
  the following tenant request remains isolated. The original reset exception
  remains primary if Exposed raises a secondary rollback/close failure.
- Reset failure after a successful business block throws
  `TenantSchemaResetFailedException`, rolls back the business write, logs the
  tenant/operation context at warn level, and evicts the connection.
- Reset failure after a business block failure preserves the business exception
  as primary and attaches the reset failure with `addSuppressed`.
- Same-connection proof: the pool=1 isolation test records the physical
  connection identity and verifies tenant A and tenant B operations used the
  reused connection before asserting B cannot read A-only data.
- Pool-state proof: reset-failure tests assert Hikari pool active connections
  return to zero and a fresh borrow is used for the next tenant request.
- Failure cleanup: a controller exception after tenant resolution still clears
  `TenantContext`; the next missing-tenant request is rejected instead of using a
  stale tenant.
- Header hardening: blank, uppercase, path-like, SQL-like, and oversized tenant
  values are rejected.
- Documentation: README pairs explain when schema-per-tenant is preferable and
  when database-per-tenant is a better next step.
