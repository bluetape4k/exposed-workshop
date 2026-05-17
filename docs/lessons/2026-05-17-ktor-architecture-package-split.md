# Ktor Architecture Package Split

## Context

The first Ktor architecture example was intentionally compact, but keeping
routes, services, repositories, persistence, and models in one file made the
production-integration lesson harder to scan.

## Decision

Keep customer models in one model package and split the remaining implementation
by layer: application wiring, Ktor config, routes, service, repository, and
persistence. Mirror that structure in tests instead of relying on one broad
HTTP integration test. Use `Base58.randomString(8)` for short H2 database
suffixes instead of UUID strings.

## Outcome

The module now shows the architecture boundary through package layout. Tests are
split across application wiring, routes, service behavior, and repository
persistence/concurrency.

## Verification

- `./gradlew --offline :01-ktor-application-architecture:compileKotlin :01-ktor-application-architecture:compileTestKotlin :01-ktor-application-architecture:test` - 13 passing
- `./gradlew --offline detekt` - `NO-SOURCE`
- `git diff --check`
- IntelliJ optimize imports and batch diagnostics - zero problems, not fresh editor highlights
- Claude Code refactor review - PASS, P0 = 0, P1 = 0

## Future Guidance

For architecture-focused Ktor examples, avoid single-file implementations once
the example includes more than routing. Keep model DTOs together when the domain
is small, but split route, service, repository, and persistence code so readers
can see the intended production shape immediately.
