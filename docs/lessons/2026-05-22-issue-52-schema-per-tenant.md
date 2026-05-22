# Issue 52 Schema-per-Tenant Example

## Context

Chapter 10 needed a schema-per-tenant Spring Boot example that uses one shared
database pool and proves tenant isolation without relying on datasource routing.
README policy also requires committed Architecture Diagram PNG images for every
example.

## Decision

Keep tenant input as a closed whitelist (`acme`, `globex`) and map it to
validated schema identifiers (`TENANT_ACME`, `TENANT_GLOBEX`). Place schema
switching only inside `TenantTransaction`, reset every transaction to `PUBLIC`,
roll back before evicting a Hikari connection if reset fails, and preserve
rollback/eviction secondary failures with `addSuppressed`. Treat reset failure
after successful work as rollback-worthy to avoid schema leakage.

## Outcome

Added `10-multi-tenant/04-schema-per-tenant-spring-web` with servlet header
validation, Exposed JDBC repository access, schema setup/seed data, failure
handling, README pairs, committed SVG+PNG diagrams, and examples workflow
coverage. Chapter and root README indexes now include the new module.

## Verification

- `./gradlew :04-schema-per-tenant-spring-web:test --stacktrace --continue`
  passed with 15 tests.
- Architecture and sequence PNGs were rendered from SVG and visually opened to
  confirm labels are dark and readable on light boxes.
- Spec/plan Claude advisor gate passed with `P0=0`, `P1=0` in
  `.omx/artifacts/claude-issue-52-spec-plan-advisor-stdin-6min-20260522215325.md`.

## Future Notes

Do not use Hikari proxy object identity as proof of physical connection reuse;
checkout proxies can differ even when the pool has one physical connection.
For H2 examples, record `SESSION_ID()` when tests need same-session evidence.
