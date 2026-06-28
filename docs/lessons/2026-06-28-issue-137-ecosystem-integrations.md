# Issue #137 Ecosystem Integrations Scaffold

## Context

Issue #137 is an epic for Exposed 1.11 database platform, Ktor, Spring
Modulith, and DDD examples. Chapter 12 already owns production-service
integration patterns, so extending it would blur the chapter purpose.

## Decision

Create `13-ecosystem-integrations` as a README-only chapter foundation. Child
issues #138-#145 will add runnable modules later. The root README links only
the chapter overview until those modules exist.

## Guards

- `settings.gradle.kts` scans the chapter directory, but no Gradle child project
  is expected until a child module has its own `build.gradle.kts`.
- The Examples workflow path filter includes the chapter, but this foundation
  PR does not add chapter 13 Gradle tasks. Child module PRs must add their own
  runnable tasks when they create modules.
- External-service examples must default to local, fake, Testcontainers,
  emulator, or documentation-only paths. Real-service execution must be explicit
  opt-in and skipped by default in CI.
- Root overview and module-composition visuals remain unchanged in this PR
  because chapter 13 has zero runnable leaf modules. Update them when the first
  runnable child module lands.

## Verification Notes

Future child PRs should prove Gradle project discovery, add real README links
only after files exist, and record whether their coverage belongs in weekly
Examples, full Nightly, or a manual opt-in lane.
