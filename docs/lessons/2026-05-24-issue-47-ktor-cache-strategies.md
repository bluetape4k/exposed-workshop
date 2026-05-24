# Issue 47 Ktor Cache Strategies

## Context

Issue #47 asked for a Ktor counterpart to the chapter 11 Spring cache strategy example.

## Decision

Keep the example framework-neutral: Ktor routes, an application service with explicit cache strategy methods, Exposed JDBC persistence, and a simple in-memory cache that exposes hit/miss counters in tests.

## Outcome

Added `11-high-performance/05-cache-strategies-ktor` with cache-aside, read-through, write-through, invalidation, English/Korean README files, and a rendered architecture diagram.

## Verification

Passed: `repo-test-summary -- ./gradlew :05-cache-strategies-ktor:test` with three passing tests.

## Future Guidance

Prefer observable route responses and counters for workshop cache examples so users can see whether a scenario hit cache, missed cache, or fell back to the database.
