# Issue 57 Spring Architecture Pair

## Context

Issue #57 is a chapter 12 epic. WIP limits require completing one child lane at a
time; the existing state already had the Ktor side of issue #58.

## Decision

Add the Spring Boot 4 application architecture pair as
`12-production-integration/02-spring-application-architecture` instead of
starting another topic. Keep the shape parallel to the Ktor module: HTTP layer,
service validation, Exposed repository, H2/Hikari persistence, and focused tests.

## Outcome

Chapter 12 now has a paired Spring/Ktor architecture topic plus chapter-level
README files that mark later production-integration topics as planned work.

## Verification

- `./gradlew projects --no-daemon` registered
  `:02-spring-application-architecture`.
- `./gradlew :02-spring-application-architecture:compileKotlin --no-daemon`
  passed.
- `./gradlew :02-spring-application-architecture:test --no-daemon` passed with
  8 tests.
- `./gradlew :01-ktor-application-architecture:test --no-daemon` passed with
  13 tests.
- `./gradlew :01-ktor-application-architecture:test
  :02-spring-application-architecture:test --no-daemon` passed after Claude
  advisor fixes.
- `git diff --check` passed.
- `:02-spring-application-architecture:detekt` is not registered.
- Claude CLI P0/P1 advisor review returned no P0 and two P1 findings in
  `ErrorAdvice`: avoid catching `Throwable` and log unexpected 5xx causes. Both
  were fixed by handling `Exception` and logging the exception before returning
  the sanitized response.

## Future Guard

For #59-#62, add Spring and Ktor coverage as pairs and update the chapter README
table in the same change.
