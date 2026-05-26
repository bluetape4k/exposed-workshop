## Context

Adopted the JetBrains Exposed Gradle plugin across Exposed workshop modules that define tables in main sources.

## Decision

The workshop uses a repo-local plugin alias tied to the existing Exposed version alias, not the managed `bt4k` catalog.

## Outcome

Spring, multi-tenant, performance, Ktor, and production-integration examples now expose `generateMigrations` with explicit migration settings.

## Verification

Ran `git diff --check`, `./gradlew -q help`, and `:spring-mvc-exposed:tasks --all`.

## Future Guard

Keep shared test fixtures out of the migration plugin rollout unless a concrete migration output is needed for those fixtures.
