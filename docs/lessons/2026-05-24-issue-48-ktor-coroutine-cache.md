# Issue 48 Ktor Coroutine Cache

## Context

Issue #48 asked for a Ktor coroutine cache example near the existing Spring coroutine cache module.

## Decision

Use suspend route handlers, `newSuspendedTransaction(Dispatchers.IO, ...)` for JDBC work, a coroutine-friendly per-key `Mutex`, and explicit cancellation rethrowing in `StatusPages`.

## Outcome

Added `11-high-performance/06-cache-strategies-coroutines-ktor` with read-through, write-through, invalidation, concurrent request tests, English/Korean README files, and a rendered architecture diagram.

## Verification

Passed: `repo-test-summary -- ./gradlew :06-cache-strategies-coroutines-ktor:test` with four passing tests, including concurrent read-through load coalescing.

## Future Guidance

Coroutine cache examples should prove concurrent request behavior, not only sequential cache hits. Keep `runBlocking` out of production request paths.
