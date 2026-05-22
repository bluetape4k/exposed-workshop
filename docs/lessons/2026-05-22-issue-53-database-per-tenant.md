# Issue 53 Database-per-Tenant Example

## Context

Issue #53 required a chapter 10 Spring MVC example that routes each tenant to a
dedicated datasource/database instead of switching schemas on one shared pool.

## Decision

Use a closed `TenantId` whitelist, a `TenantDatabaseRegistry` that owns one
Hikari pool and Exposed `Database` per tenant, and explicit
`TenantTransaction.execute {}` calls so there is no default datasource fallback.

## Outcome

Added `10-multi-tenant/05-database-per-tenant-spring-web` with English/Korean
READMEs, architecture and sequence PNG diagrams, focused tests for isolation,
rollback, config rejection, lifecycle close, and servlet-thread context cleanup.
The selected examples workflow now builds the new module.

## Verification

- `./gradlew :05-database-per-tenant-spring-web:build --stacktrace --continue`
  passed with 14 tests.
- `actionlint .github/workflows/examples.yml` passed.
- `./gradlew projects --quiet` lists `:05-database-per-tenant-spring-web`.
- README scan confirmed Architecture Diagram PNG links, no Mermaid, and existing
  PNG files.
- Claude Step 6-R rerun:
  `.omx/artifacts/claude-issue-53-code-review-rerun-stdin-6min-20260523002013.md`
  reported `P0=0, P1=0, P2=0`.

## Future Agents

ThreadLocal cleanup tests must observe the servlet/filter thread, not the JUnit
client thread. For tenant datasource examples, close partially-created pools on
registry initialization failure and wire Spring bean shutdown explicitly.
