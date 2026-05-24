# 05 Cache Strategies Ktor

English | [한국어](./README.ko.md)

This module demonstrates cache-aside, read-through, write-through, and explicit invalidation flows with Ktor routes, Exposed JDBC persistence, and an in-memory cache.

## Architecture Diagram

![Ktor cache strategy architecture](../../docs/images/readme-diagrams/11-high-performance-05-cache-strategies-ktor-architecture-01.png)

## Routes

| Route | Strategy | Behavior |
|---|---|---|
| `GET /users/{id}/cache-aside` | Cache-aside | Check cache, then load from database and populate cache on miss. |
| `GET /users/{id}/read-through` | Read-through | Service owns database fallback and returns the cached value on later reads. |
| `PUT /users/{id}/write-through` | Write-through | Update database and cache in the same application operation. |
| `DELETE /users/{id}/cache` | Invalidation | Remove the cache entry so the next read falls back to database. |
| `GET /cache/stats` | Observability | Return database read, hit, miss, and cache size counters. |

## Verification

```bash
./gradlew :05-cache-strategies-ktor:test
```

Use this example when a Ktor service needs explicit, testable cache behavior without Spring Cache abstractions.
