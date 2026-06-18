# 11 High Performance (Production)

English | [한국어](./README.ko.md)

This chapter follows the high-performance examples in the source tree: Redisson-backed cache repositories for Spring MVC and WebFlux, a registry-driven read/write `DataSource` router, JMH benchmarks, and compact Ktor variants that keep the same cache and routing decisions explicit. The examples focus on where latency is removed, where consistency is intentionally traded, and how each optimization is verified by tests.

## Chapter Goals

- Compare read-through, write-through, read-only, and write-behind cache behavior against concrete repository implementations.
- Apply cache access patterns in Spring MVC + Virtual Threads, WebFlux + Coroutines, and Ktor request handlers.
- Design read/write `DataSource` routing that is explicit enough to test and safe enough to operate.
- Use smoke benchmarks for quick trend checks before running precise JMH measurements.

## Prerequisites

- Contents of `10-multi-tenant`
- Cache theory and transaction consistency concepts

---

## Included Modules

| Module                                                                         | Description                                               |
|------------------------------------------------------------------------------|----------------------------------------------------------|
| [`01-cache-strategies`](01-cache-strategies/README.md)                       | Cache strategies with Spring MVC + Virtual Threads        |
| [`02-cache-strategies-coroutines`](02-cache-strategies-coroutines/README.md) | Cache strategies with WebFlux + Coroutines                |
| [`03-routing-datasource`](03-routing-datasource/README.md)                   | DataSource routing design guide                           |
| [`04-benchmark`](04-benchmark/README.md)                                     | Performance measurement with `kotlinx-benchmark`          |
| [`05-cache-strategies-ktor`](05-cache-strategies-ktor/README.md)             | Ktor cache-aside/read-through/write-through examples      |
| [`06-cache-strategies-coroutines-ktor`](06-cache-strategies-coroutines-ktor/README.md) | Coroutine-safe Ktor cache examples               |
| [`07-routing-datasource-ktor`](07-routing-datasource-ktor/README.md)         | Ktor read/write datasource routing examples               |

---

## Module Relationships

![Module Relationships diagram](../docs/images/readme-diagrams/11-high-performance-architecture-01.png)

---

## Overall Architecture

![Overall Architecture diagram](../docs/images/readme-diagrams/11-high-performance-architecture-02.png)

---

## Cache Strategy Comparison

| Strategy                     | Repository                       | Write Time             | Read Time        | Suitable Data              |
|------------------------------|----------------------------------|------------------------|------------------|---------------------------|
| Read-Through + Write-Through | `UserCacheRepository`            | Cache + DB simultaneously | DB fallback on miss | Entities with updates    |
| Read-Only                    | `UserCredentialsCacheRepository` | None (read-only)       | DB fallback on miss | Immutable data like auth info |
| Write-Behind                 | `UserEventCacheRepository`       | Cache immediately + DB async | Cache first  | Loss-tolerant data like events/logs |

---

## Technology Stack Comparison

| Module                           | Runtime    | Thread Model                         | HTTP Server |
|----------------------------------|------------|--------------------------------------|-------------|
| `01-cache-strategies`            | Spring MVC | Virtual Threads                      | Tomcat      |
| `02-cache-strategies-coroutines` | WebFlux    | Coroutines + Netty event loop         | Netty       |
| `03-routing-datasource`          | Spring MVC | Thread-based                         | Tomcat      |
| `04-benchmark`                   | JMH        | JMH threads                          | N/A         |
| `05-cache-strategies-ktor`       | Ktor       | CIO event loop + blocking repository | CIO         |
| `06-cache-strategies-coroutines-ktor` | Ktor  | Suspend routes + `Dispatchers.IO` DB | CIO         |
| `07-routing-datasource-ktor`     | Ktor       | Request plugin + coroutine context   | CIO         |

---

## Recommended Learning Order

1. [`01-cache-strategies`](01-cache-strategies/README.md) — Basic cache strategy concepts
2. [`02-cache-strategies-coroutines`](02-cache-strategies-coroutines/README.md) — Coroutine async cache
3. [`03-routing-datasource`](03-routing-datasource/README.md) — Dynamic DataSource routing
4. [`04-benchmark`](04-benchmark/README.md) — Performance measurement and comparison
5. [`05-cache-strategies-ktor`](05-cache-strategies-ktor/README.md) — Ktor cache strategy routes
6. [`06-cache-strategies-coroutines-ktor`](06-cache-strategies-coroutines-ktor/README.md) — Coroutine-safe Ktor cache
7. [`07-routing-datasource-ktor`](07-routing-datasource-ktor/README.md) — Ktor read/write datasource routing

---

## How to Run

```bash
# Individual module tests
./gradlew :01-cache-strategies:test
./gradlew :02-cache-strategies-coroutines:test
./gradlew :03-routing-datasource:test
./gradlew :04-benchmark:test
./gradlew :05-cache-strategies-ktor:test
./gradlew :06-cache-strategies-coroutines-ktor:test
./gradlew :07-routing-datasource-ktor:test

# Benchmark (smoke: fast trend check, main: precise measurement)
./gradlew :04-benchmark:smokeBenchmark
./gradlew :04-benchmark:benchmarkMarkdown -PbenchmarkProfile=smoke
```

---

## Test Points

- Verify cache hit rate/latency/DB load reduction effects.
- Check consistency guarantee scenarios during Write-Behind delayed propagation.
- Confirm fallback path (cache failure → DB) operates correctly on failure.
- For Ktor modules, confirm route responses expose cache source or selected datasource role.
- Ktor examples are H2-only, so they exercise cache/routing behavior without container-backed Redis or replica databases.

---

## Performance & Stability Checkpoints

- Align cache invalidation policy with data freshness SLA.
- Tune backpressure/batch size during event floods.
- Prevent false positives/misses in routing key resolution logic.
- Use smoke profile for quick trend checks, main profile for precise measurement.

---

## Complex Scenarios

### Write-Behind Async Propagation Verification

`UserEventCacheRepository` pre-stores events in Redis and then batch-saves to DB asynchronously. A scenario verifying the final persisted count with Awaitility after bulk loading.

- MVC version: [`01-cache-strategies/src/test/kotlin/.../UserEventCacheRepositoryTest.kt`](01-cache-strategies/src/test/kotlin/exposed/examples/cache/domain/repository/UserEventCacheRepositoryTest.kt)
- Coroutines version: [`02-cache-strategies-coroutines/src/test/kotlin/.../UserEventCacheRepositoryTest.kt`](02-cache-strategies-coroutines/src/test/kotlin/exposed/examples/cache/coroutines/domain/repository/UserEventCacheRepositoryTest.kt)

### Multi-Tenant Dynamic DataSource Routing

`DynamicRoutingDataSource` selects the appropriate DataSource by combining `TenantContext` with transaction read-only status. Verifies per-tenant read/write separation scenarios through integration tests.

- Related file: [`03-routing-datasource/src/main/kotlin/.../DynamicRoutingDataSource.kt`](03-routing-datasource/src/main/kotlin/exposed/examples/routing/datasource/DynamicRoutingDataSource.kt)
- Verification tests: [`DynamicRoutingDataSourceTest.kt`](03-routing-datasource/src/test/kotlin/exposed/examples/routing/datasource/DynamicRoutingDataSourceTest.kt), [`RoutingMarkerControllerTest.kt`](03-routing-datasource/src/test/kotlin/exposed/examples/routing/web/RoutingMarkerControllerTest.kt)

---

## Notes

- Redisson-based cache strategies require a Redis server. Testcontainers automatically starts a Redis container.
- The RoutingDataSource example can be used with Read Replica or multi-tenant structures.
