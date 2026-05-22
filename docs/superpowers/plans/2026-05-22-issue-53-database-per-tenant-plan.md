# Issue 53 Database-Per-Tenant Plan

## Steps

1. Confirm routing and docs requirements.
   - Use issue #53 and epic #51 as scope.
   - Compare issue #52 schema-per-tenant module for controller, domain,
     README, CI, and test patterns.
   - Keep fallback behavior explicit: missing tenant is 400, unknown tenant is
     404, and no default datasource is used.

2. Create the new module.
   - Add `10-multi-tenant/05-database-per-tenant-spring-web`.
   - Add Gradle dependencies matching the issue #52 Spring MVC/H2 example.
   - Add application YAML with `app.tenants.acme` and
     `app.tenants.globex` H2 JDBC URLs.

3. Implement tenant routing.
   - Add `TenantId`, `TenantContext`, and `TenantFilter`.
   - Register `TenantFilter` at highest precedence and clear `TenantContext`
     in `finally`.
   - Add tenant datasource properties and `TenantDatabaseRegistry`.
   - Pin safe Hikari defaults and require H2 URLs with `DB_CLOSE_DELAY=-1`.
   - Add `TenantTransaction` as the only repository transaction boundary.
   - Ensure registry validates known tenants and closes all owned datasources.
   - Close owned datasources from a Spring lifecycle hook.

4. Implement inventory API.
   - Add table/model/repository/service/controller/error-handler classes.
   - Seed distinct inventory rows per tenant.
   - Bootstrap DDL once per tenant database before seed writes.
   - Keep seed writes idempotent for repeated application context starts.
   - Keep repository code datasource-agnostic.

5. Add focused tests.
   - Valid tenants read isolated seed data.
   - Writes for one tenant are not visible to another tenant.
   - Parallel alternating tenant requests cannot leak `TenantContext`.
   - Missing tenant returns 400.
   - Unknown tenant returns 404.
   - Error responses use stable `{code, message}` JSON.
   - Registry rejects incomplete or unknown tenant configuration.
   - Registry close closes all owned Hikari datasources.
   - Failing writes roll back in the selected tenant database only.
   - DDL bootstrap creates inventory tables in every configured database.

6. Add documentation and diagrams.
   - Add English/Korean module READMEs.
   - Document that header-based tenant routing is a workshop simplification;
     production systems must bind tenant identity to authentication.
   - Add architecture PNG/SVG and sequence PNG/SVG.
   - Update `10-multi-tenant/README.md` and `README.ko.md`.
   - Update root `README.md` and `README.ko.md`.

7. Wire selected examples CI.
   - Add module path to `.github/workflows/examples.yml` triggers.
   - Add `:05-database-per-tenant-spring-web:build` to selected example build.

8. Verify and review.
   - Run targeted module tests/build.
   - Run `actionlint`.
   - Run README diagram scan and visual PNG inspection.
   - Run Step 6-R 6-Tier review plus Claude Code CLI review.

9. Publish.
   - Commit with Lore protocol.
   - Push branch and create PR against `develop`, assigned to `debop`.
   - Add `examples` and `documentation` labels when available.
   - Watch CI and address failures before handoff.

## Acceptance Mapping

- Tenant-specific datasource/database routing:
  Steps 2-4 implement `TenantDatabaseRegistry` and `TenantTransaction`.
- Focused tenant isolation and error tests:
  Step 5 covers isolation, missing/unknown tenant, and no fallback.
- README.md and README.ko.md strategy guidance:
  Step 6 documents when database-per-tenant is appropriate.
- CI/nightly coverage decision:
  The spec records H2 selected-example CI coverage; Step 7 enforces it.

## Review Notes

Step 2-R/3-R advisor review is required before implementation proceeds.
The gate passes only with latest normalized `P0=0` and `P1=0`.

Initial advisor artifact:
`.omx/artifacts/claude-issue-53-spec-plan-advisor-stdin-6min-20260522235409.md`.
It failed with P0=2/P1=6. The spec and plan now include the accepted fixes:
ThreadLocal cleanup, H2 lifecycle, lifecycle close hook, filter ordering,
parallel isolation, rollback, DDL bootstrap, and README auth-warning coverage.
