# Issue 53 Database-Per-Tenant Design

## Context

Issue #53 extends the Chapter 10 Spring Boot multi-tenant strategy set after
the schema-per-tenant example from issue #52. The goal is to teach tenant
routing at the datasource/database boundary: each accepted tenant maps to its
own datasource and Exposed `Database`, and every repository transaction runs
against the selected tenant database.

This is a Type A Full Design change because it adds a new example module,
public README material, CI wiring, and a routing boundary that future examples
can compare against.

## Scope

- Add `10-multi-tenant/05-database-per-tenant-spring-web`.
- Use Spring MVC and Exposed JDBC with two H2 in-memory tenant databases:
  `acme` and `globex`.
- Resolve tenant from `X-Tenant-ID` through a strict whitelist.
- Route Exposed transactions through a `TenantDatabaseRegistry`.
- Seed each tenant database independently.
- Test tenant isolation, missing tenant, unknown tenant, no-default fallback,
  and datasource lifecycle assumptions.
- Add English/Korean READMEs with committed Architecture Diagram PNG and an
  additional sequence diagram PNG.
- Wire the module into Chapter 10 docs, root README files, and selected
  examples CI.

Out of scope:

- Runtime tenant provisioning. Issue #55 owns onboarding/provisioning.
- Spring Security-bound tenant authorization. Issue #54 owns auth coupling.
- Container-backed database matrix coverage for this module. The example uses
  H2 to keep PR CI fast; the CI decision is documented in README and lessons.

## Design

### Tenant Resolution

`TenantFilter` reads `X-Tenant-ID`, normalizes it with trim/lowercase, and
maps it to `TenantId`. Accepted values are `acme` and `globex`.

Missing tenant is a caller error and returns HTTP 400. Unknown tenant is not
silently mapped to a default database; it returns HTTP 404. This intentionally
rejects fallback routing because a default datasource would hide isolation
misconfiguration and can leak tenant data.

`TenantContext` remains request scoped through `ThreadLocal`, matching the
Spring MVC chapter pattern and the issue #52 module.

`TenantFilter` must always clear `TenantContext` in `finally`, including
controller/repository exception paths. The filter is registered with highest
precedence so all downstream MVC handlers and error handlers see the resolved
tenant and no later request can inherit stale `ThreadLocal` state.

This example intentionally does not perform tenant authorization. The README
must warn that `X-Tenant-ID` is trusted only for workshop routing; production
systems must bind the tenant to authenticated claims or server-side session
state. Issue #54 owns that security-bound example.

### Database Registry

`TenantDataSourceProperties` defines per-tenant Hikari settings under
`app.tenants`. `DatabaseConfiguration` builds one `HikariDataSource` and one
Exposed `Database` per configured `TenantId`. The H2 URLs must include
`DB_CLOSE_DELAY=-1` so each tenant database survives connection churn for the
life of the application context. The Hikari defaults are pinned in code:
maximum pool size 4, minimum idle 1, connection timeout 5 seconds, and pool
name `tenant-{id}` unless YAML overrides a non-safety field.

`TenantDatabaseRegistry` exposes:

- `databaseFor(tenantId: TenantId): Database`
- `dataSourceFor(tenantId: TenantId): DataSource`
- `configuredTenants(): Set<TenantId>`
- `close()` for lifecycle cleanup

The registry validates at bean initialization that every `TenantId` has a
datasource and that no unknown tenant key appears in configuration. Unknown
or incomplete tenant configuration fails startup, not the first request.
The registry is the only place that owns datasource lifecycle and closes all
owned `HikariDataSource` instances through a `@PreDestroy`/`DisposableBean`
hook.

### Transaction Boundary

`TenantTransaction.execute { ... }` obtains the current `TenantId` from
`TenantContext`, resolves its Exposed `Database`, and runs `transaction(db)`.

Repositories must not pick a datasource directly. They depend on
`TenantTransaction` so the tenant/database routing boundary is explicit and
auditable.

Rollback behavior is owned by the Exposed transaction. Tests must prove that a
failing tenant write rolls back only in that tenant database and does not alter
another tenant database.

### Domain

The new module reuses the inventory domain shape from issue #52:

- `InventoryItems` table
- `InventoryItemRecord`
- `CreateInventoryItemRequest`
- `InventoryRepository`
- `InventoryService`
- `InventoryController`

The table name is the same in every tenant database, but physical storage is
separate because each tenant has a different JDBC URL and Exposed `Database`.

### Seeding

`InventorySeeder` iterates configured tenants, creates `InventoryItems`, and
seeds tenant-specific rows through the same repository/transaction path used by
requests. DDL bootstrap must run once per tenant database before seed writes.
The seed operation is idempotent for repeated context starts by checking
whether tenant rows already exist before inserting default rows. Tests assert
`acme` and `globex` see different seed data.

### Error Contract

- Missing `X-Tenant-ID`: 400 with `MISSING_TENANT`.
- Unknown `X-Tenant-ID`: 404 with `UNKNOWN_TENANT`.
- Registry missing datasource for a known tenant: startup failure.
- No default tenant/database fallback is allowed.

Error responses use a stable JSON shape:

```json
{"code":"MISSING_TENANT","message":"X-Tenant-ID header is required"}
```

### README Diagrams

Both README files link the same committed PNG files under
`docs/images/readme-diagrams/`:

- `10-multi-tenant-05-database-per-tenant-spring-web-architecture-01.png`
- `10-multi-tenant-05-database-per-tenant-spring-web-sequence-02.png`

SVG sources are committed next to the PNGs. Diagram text is English.

## Verification

Local verification:

- `./gradlew :05-database-per-tenant-spring-web:test --stacktrace --continue`
- `./gradlew :05-database-per-tenant-spring-web:build --stacktrace --continue`
- `./gradlew projects --quiet | rg '05-database-per-tenant-spring-web'`
- `actionlint .github/workflows/examples.yml`
- README diagram scan for Architecture Diagram PNG links and no Mermaid blocks.
- Visual inspection of generated PNGs for readable contrast.

Required test coverage:

- Parallel alternating requests through the MVC thread pool cannot leak
  `TenantContext`.
- A failing tenant write rolls back in the selected tenant only.
- DDL bootstrap creates `InventoryItems` in every configured tenant database.
- Unknown tenant config keys and missing known tenant config fail fast.
- Registry close closes all owned Hikari datasources.

Review gates:

- Step 2-R/3-R advisor review on this spec and plan using Claude Code CLI via
  stdin with timeout >= 5 minutes.
- Step 6-R code review after implementation using the 6-Tier frame plus Claude
  Code CLI via stdin with timeout >= 5 minutes.

## CI Decision

This module should run in selected examples CI because it uses H2-only tenant
databases and does not start Testcontainers. Full repository DB matrix coverage
will still run through the existing CI workflow when non-doc files change, but
the module itself is designed to remain fast and deterministic.

## Advisor Gate

- Initial artifact:
  `.omx/artifacts/claude-issue-53-spec-plan-advisor-stdin-6min-20260522235409.md`
- Result: FAIL, P0=2, P1=6.
- Accepted edits: ThreadLocal cleanup/filter ordering, H2 URL lifecycle,
  README auth-trust warning, registry lifecycle hook, concurrent isolation
  tests, rollback tests, DDL bootstrap, stable error JSON, and fail-fast
  configuration validation.
