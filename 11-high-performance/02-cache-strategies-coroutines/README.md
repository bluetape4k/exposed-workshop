# Cache Strategies - Coroutines (02-cache-strategies-coroutines)

English | [한국어](./README.ko.md)

The coroutines/non-blocking version of `01-cache-strategies`. Practises `suspend`-based cache access patterns in a WebFlux + Netty + Coroutines environment.

## Learning Goals

- Learn `suspend`-based cache/DB access patterns.
- Implement an event-loop-friendly cache processing model.
- Verify stability under high-concurrency conditions.

## Prerequisites

- [`../08-coroutines/README.md`](../08-coroutines/README.md)
- [`../01-cache-strategies/README.md`](../01-cache-strategies/README.md)

---

## Overview

By extending `AbstractSuspendedJdbcRedissonRepository`, you can apply cache strategies as `suspend` functions. Internally, `newSuspendedTransaction` is used to handle Exposed DB access within a coroutine context. Because the Netty event loop is never blocked, thread pool exhaustion does not occur even in high-concurrency environments.

---

## Cache Strategy Architecture

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
flowchart LR
    Client([WebFlux Client])

    subgraph "Read-Through suspend"
        RT_Cache{Near Cache\nL1}
        RT_Redis[(Redis L2)]
        RT_DB[(Database)]
        Client -- suspend get --> RT_Cache
        RT_Cache -- miss --> RT_Redis
        RT_Redis -- miss --> RT_DB
        RT_DB -- load --> RT_Redis
        RT_Redis -- fill --> RT_Cache
        RT_Cache -- hit --> Client
    end

    subgraph "Write-Through suspend"
        WT_Cache{Near Cache\nL1}
        WT_Redis[(Redis L2)]
        WT_DB[(Database)]
        Client -- suspend save --> WT_Cache
        WT_Cache -- sync --> WT_Redis
        WT_Redis -- sync --> WT_DB
        WT_DB -->|done| WT_Cache
    end

    subgraph "Write-Behind suspend"
        WB_Cache{Near Cache\nL1}
        WB_Redis[(Redis L2)]
        WB_Queue[[Coroutine\nAsync Queue]]
        WB_DB[(Database)]
        Client -- suspend save --> WB_Cache
        WB_Cache -- immediate --> WB_Redis
        WB_Redis -- enqueue --> WB_Queue
        WB_Queue -- batch flush --> WB_DB
    end

    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef pink fill:#FCE4EC,stroke:#F48FB1,color:#AD1457
    classDef yellow fill:#FFFDE7,stroke:#FFF176,color:#F57F17

    class Client blue
    class RT_Cache,WT_Cache,WB_Cache pink
    class RT_Redis,WT_Redis,WB_Redis orange
    class RT_DB,WT_DB,WB_DB orange
    class WB_Queue yellow
```

---

## Class Structure

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
classDiagram
    class AbstractSuspendedJdbcRedissonRepository {
        <<abstract>>
        +cacheName: String
        +config: RedisCacheConfig
        +entityTable: T
        +suspend findById(id): E?
        +suspend findAll(): List~E~
        +suspend saveAll(entities): List~E~
        +suspend deleteById(id)
        +suspend invalidate(ids)
        #doInsertEntity(stmt, entity)
        #doUpdateEntity(stmt, entity)
        #toEntity(row): E
    }

    class UserCacheRepository {
        +cacheName = "exposed:coroutines:users"
        +config = READ_WRITE_THROUGH_WITH_NEAR_CACHE
        +entityTable: UserTable
        #doInsertEntity(stmt, entity)
        #doUpdateEntity(stmt, entity)
    }

    class UserCredentialsCacheRepository {
        +cacheName = "exposed:coroutines:user-credentials"
        +config = READ_ONLY_WITH_NEAR_CACHE
        +entityTable: UserCredentialsTable
    }

    class UserEventCacheRepository {
        +cacheName = "exposed:coroutines:user-events"
        +config = WRITE_BEHIND_WITH_NEAR_CACHE
        +entityTable: UserEventTable
        #doInsertEntity(stmt, entity)
        #doUpdateEntity(stmt, entity)
    }

    class RedisCacheConfig {
<<enum-likeobject>>
READ_WRITE_THROUGH_WITH_NEAR_CACHE
READ_ONLY_WITH_NEAR_CACHE
WRITE_BEHIND_WITH_NEAR_CACHE
}

AbstractSuspendedJdbcRedissonRepository <|-- UserCacheRepository
AbstractSuspendedJdbcRedissonRepository <|-- UserCredentialsCacheRepository
AbstractSuspendedJdbcRedissonRepository <|-- UserEventCacheRepository
AbstractSuspendedJdbcRedissonRepository --> RedisCacheConfig

    style AbstractSuspendedJdbcRedissonRepository fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style UserCacheRepository fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style UserCredentialsCacheRepository fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style UserEventCacheRepository fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style RedisCacheConfig fill:#FFFDE7,stroke:#FFF176,color:#F57F17
```

---

## Request Processing Flow — Write-Behind Async Event Loading (Coroutines)

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
sequenceDiagram
    box rgb(227, 242, 253) Client
        participant C as WebFlux Client
    end
    box rgb(232, 245, 233) Application
        participant Ctrl as UserEventController
        participant R as UserEventCacheRepository
    end
    box rgb(252, 228, 236) Cache
        participant NC as Near Cache (L1)
        participant RD as Redis (L2)
        participant Q as Coroutine Async Queue
    end
    box rgb(255, 243, 224) Database
        participant DB as Database
    end
    C ->> Ctrl: POST /user-events/bulk (suspend)
    Ctrl ->> R: saveAll(events) [suspend]
    R ->> NC: put events (immediately)
    R ->> RD: put events (immediately)
    RD -->> Ctrl: Save complete
    Ctrl -->> C: 200 OK
    Note over RD, Q: Coroutine async batch flush
    RD ->> Q: enqueue dirty entries
    Q ->> DB: newSuspendedTransaction { batchInsert }
    DB -->> Q: commit
    Q -->> RD: flush complete
    C ->> Ctrl: GET /user-events/{id} (suspend)
    Ctrl ->> R: findById(id) [suspend]
    R ->> NC: get(id)
    NC -->> R: hit
    R -->> Ctrl: UserEventRecord
    Ctrl -->> C: 200 OK + body
```

---

## Request Processing Flow — Read-Through + Write-Through (Coroutines User)

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
sequenceDiagram
    box rgb(227, 242, 253) Client
        participant C as WebFlux Client
    end
    box rgb(232, 245, 233) Application
        participant Ctrl as UserController
        participant R as UserCacheRepository
    end
    box rgb(252, 228, 236) Cache
        participant NC as Near Cache (L1)
        participant RD as Redis (L2)
    end
    box rgb(255, 243, 224) Database
        participant DB as Database
    end
    C ->> Ctrl: GET /users/{id}
    Ctrl ->> R: findById(id) [suspend]
    R ->> NC: get(id)
    NC -->> R: miss
    R ->> RD: get(id)
    RD -->> R: miss
    R ->> DB: newSuspendedTransaction { SELECT }
    DB -->> R: UserRecord
    R ->> RD: put(id, record)
    R ->> NC: put(id, record)
    R -->> Ctrl: UserRecord
    Ctrl -->> C: 200 OK (first query)
    C ->> Ctrl: GET /users/{id}
    Ctrl ->> R: findById(id) [suspend]
    R ->> NC: get(id)
    NC -->> Ctrl: hit → UserRecord
    Ctrl -->> C: 200 OK (cache hit)
    C ->> Ctrl: POST /users (save)
    Ctrl ->> R: saveAll([user]) [suspend]
    R ->> DB: newSuspendedTransaction { UPDATE }
    R ->> RD: put(id, updated)
    R ->> NC: put(id, updated)
    R -->> Ctrl: Save complete
    Ctrl -->> C: 200 OK
```

---

## Key Configuration

### application.yml

```yaml
server:
    port: 8080
    compression:
        enabled: true
    shutdown: graceful

spring:
    datasource:
        url: jdbc:h2:mem:cache-strategy;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
        driver-class-name: org.h2.Driver
        hikari:
            maximum-pool-size: 80
            minimum-idle: 4
            idle-timeout: 30000
            connection-timeout: 30000
    exposed:
        generate-ddl: true
        show-sql: false
```

### NettyConfig Key Settings

| Item                    | Value                              | Description                         |
|-----------------------|------------------------------------|-------------------------------------|
| `SO_BACKLOG`          | 8,000                              | Pending connection queue size        |
| `SO_KEEPALIVE`        | true                               | TCP keepalive enabled                |
| `ReadTimeoutHandler`  | 10s                                | Read timeout                         |
| `WriteTimeoutHandler` | 10s                                | Write timeout                        |
| `maxConnections`      | 8,000                              | ConnectionProvider max connections   |
| `maxIdleTime`         | 30s                                | Idle connection retention time       |
| `loopResources`       | `availableProcessors * 8` (min 64) | Event loop thread count              |

---

## Key Components

| File/Area                                                 | Description                            |
|-------------------------------------------------------|----------------------------------------|
| `domain/repository/UserCacheRepository.kt`            | Suspended Read-Through + Write-Through |
| `domain/repository/UserCredentialsCacheRepository.kt` | Suspended Read-Only Cache              |
| `domain/repository/UserEventCacheRepository.kt`       | Suspended Write-Behind                 |
| `config/NettyConfig.kt`                               | Netty event loop and connection pool tuning |
| `config/RedissonConfig.kt`                            | Redisson client configuration          |
| `controller/*Controller.kt`                           | `suspend` endpoints                    |

---

## How to Test

```bash
# Unit/integration tests (Testcontainers auto-starts Redis)
./gradlew :11-high-performance:02-cache-strategies-coroutines:test

# Run application
./gradlew :11-high-performance:02-cache-strategies-coroutines:bootRun
```

### API Endpoints (WebFlux / suspend)

```bash
# User (Suspended Read-Through / Write-Through)
GET  /users/{id}
POST /users

# UserCredentials (Suspended Read-Only Cache)
GET  /user-credentials/{username}
DELETE /user-credentials

# UserEvent (Suspended Write-Behind)
GET  /user-events/{id}
POST /user-events/bulk
```

---

## Practice Checklist

- Verify cache hit/miss behavior on the `suspend` path
- Use `flatMapMerge` to run 100 parallel queries and measure shorter second-query time with `measureTimeMillis`
- Observe async propagation delay during bulk event loading with `untilSuspending`
- Prevent regression with WebTestClient-based integration tests

---

## Operations Checkpoints

- Never block the event loop (no `runBlocking`)
- Confirm that async propagation delay is acceptable for the domain before adoption
- Verify data consistency on coroutine cancellation/timeout
- Isolate `ReactorResourceFactory` from global resources to prevent test interference

---

## Complex Scenarios

### Coroutine Read-Through + Write-Through Flow (User)

`UserCacheRepository` (suspend version) queries the DB on cache miss inside `newSuspendedTransaction` and loads it into Redis.

- Related file: [`domain/repository/UserCacheRepository.kt`](src/main/kotlin/exposed/examples/cache/coroutines/domain/repository/UserCacheRepository.kt)
- Verification test: [`UserCacheRepositoryTest.kt`](src/test/kotlin/exposed/examples/cache/coroutines/domain/repository/UserCacheRepositoryTest.kt)

### Coroutine Write-Behind Bulk Event Async Propagation (UserEvent)

`UserEventCacheRepository` (suspend version) pre-stores events in Redis and then batch-saves to DB via coroutines. After a 500-item bulk insert, Awaitility + `untilSuspending` waits for async DB propagation to complete.

- Related file: [`domain/repository/UserEventCacheRepository.kt`](src/main/kotlin/exposed/examples/cache/coroutines/domain/repository/UserEventCacheRepository.kt)
- Verification test: [`UserEventCacheRepositoryTest.kt`](src/test/kotlin/exposed/examples/cache/coroutines/domain/repository/UserEventCacheRepositoryTest.kt)

### Coroutine Cache Invalidation (UserCredentials)

`UserCredentialsCacheRepository` (suspend version) provides a Read-Only cache with explicit ID-based invalidation in the coroutine environment.

- Related file: [`domain/repository/UserCredentialsCacheRepository.kt`](src/main/kotlin/exposed/examples/cache/coroutines/domain/repository/UserCredentialsCacheRepository.kt)
- Verification test: [`UserCredentialsCacheRepositoryTest.kt`](src/test/kotlin/exposed/examples/cache/coroutines/domain/repository/UserCredentialsCacheRepositoryTest.kt)

---

## Next Module

- [`../03-routing-datasource/README.md`](../03-routing-datasource/README.md)
