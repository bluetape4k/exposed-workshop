# 06 Cache Strategies Coroutines Ktor

English | [한국어](./README.ko.md)

This module demonstrates coroutine-safe Ktor cache access with suspending route handlers, Exposed JDBC transactions isolated on `Dispatchers.IO`, Bluetape4k's `AbstractSuspendedJdbcCaffeineRepository`, library-owned per-key load coalescing, write-through updates, and explicit invalidation. `CoroutineCachedProductService` no longer owns a per-SKU `Mutex`; the repository's read-through `get` coalesces concurrent misses while the service keeps route counters and failure-latch observability.

## Architecture Diagram

![Ktor coroutine cache architecture](../../docs/images/readme-diagrams/11-high-performance-06-cache-strategies-coroutines-ktor-architecture-01.png)

## Coroutine Behavior

- Route handlers stay suspend-first and do not use `runBlocking`.
- Database access uses `newSuspendedTransaction(Dispatchers.IO, ...)`.
- Concurrent read-through requests for the same SKU share one repository loader; the acceptance test proves `databaseReads == 1`. The service's hit/miss counters are request-side observations and are not used as an exact loader count.
- `GET /healthz/exposed` and `GET /ready` use Bluetape4k Ktor health/readiness routes; legacy `GET /health` keeps the workshop response.
- `CancellationException` is rethrown by `StatusPages` instead of being converted to a generic error response.

## Verification

```bash
./gradlew :06-cache-strategies-coroutines-ktor:test
```

The tests verify cached second reads, coalesced concurrent loads, write-through cache refresh, invalidation (204/404), library health/readiness routes, and cancellation-friendly error handling. The JSON test client is shared through `:exposed-shared-tests`. Use this example when a Ktor service needs cache behavior that remains safe under concurrent suspending request handlers.
