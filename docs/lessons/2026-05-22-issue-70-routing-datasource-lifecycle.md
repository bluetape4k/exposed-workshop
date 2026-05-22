# Issue 70 Routing Datasource Lifecycle

## Context

Issue #70 reported that the chapter 11 routing datasource example created
tenant Hikari pools but did not give Spring a deterministic shutdown hook.

## Decision

Make `DataSourceRegistry` `AutoCloseable` and let `InMemoryDataSourceRegistry`
own shutdown for currently registered closeable data sources. Deduplicate by
identity so a shared datasource registered under multiple keys is closed once.
Document that replacing an existing key does not close the old datasource.

## Outcome

The TODO in `RoutingDataSourceConfig` was removed. The routing datasource example
now closes registry-owned Hikari pools during Spring shutdown, and `Examples.yml`
builds the routing module together with selected chapter 12 examples. The
workflow disables Gradle configuration cache for this mixed example build because
the routing module's GraalVM AOT tasks triggered a local configuration-cache
serialization failure.

## Verification

- `actionlint .github/workflows/examples.yml`
- `./gradlew :03-routing-datasource:test --no-daemon` — 29 passing
- `./gradlew :03-routing-datasource:build :01-ktor-application-architecture:build :02-spring-application-architecture:build :03-spring-http-outbox-idempotency:build :04-ktor-http-outbox-idempotency:build --no-daemon --no-configuration-cache --continue`
- Claude advisor artifact: `.omx/artifacts/ask-claude-code-review-issue-70-routing-datasource-final2-20260522103201.md` — P0=0, P1=0

## Future Guard

When adding example-owned pools, make the owner `AutoCloseable` and add tests
for idempotent close, shared-instance close-once behavior, and suppressed
exception propagation.
