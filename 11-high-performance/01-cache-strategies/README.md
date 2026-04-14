# Cache Strategies (01-cache-strategies)

English | [한국어](./README.ko.md)

A module for hands-on practice with cache strategies using Redisson + Exposed in a Spring MVC + Virtual Threads environment. Compares consistency/performance trade-offs for Read Through, Write Through, and Write Behind strategies.

## Learning Goals

- Distinguish behavior and trade-offs by cache strategy.
- Understand consistency models based on Redis + DB synchronization timing.
- Verify invalidation/recovery scenarios needed in production.

## Prerequisites

- [`../09-spring/README.md`](../09-spring/README.md)

---

## Overview

By extending `AbstractJdbcRedissonRepository`, you can select among three cache strategies with a single configuration value. Redisson's Near Cache acts as L1 (local memory), Redis as L2 (distributed cache), and Exposed as the DB access layer. Tomcat is replaced with a Virtual Thread-based Executor to reduce blocking I/O cost.

---

## Domain ERD

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
erDiagram
    users {
        BIGSERIAL id PK
        VARCHAR username UK
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR address
        VARCHAR zipcode
        DATE birth_date
        BLOB avatar
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    user_credentials {
        VARCHAR id PK "TimebasedUUID Base62"
        VARCHAR username UK
        VARCHAR email
        TIMESTAMP last_login_at
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    user_action {
        BIGSERIAL id PK
        VARCHAR username
        VARCHAR event_source
        VARCHAR event_type
        VARCHAR event_details
        TIMESTAMP event_time
    }
```

---

## Cache Strategy Architecture

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
flowchart LR
    Client([Client])

    subgraph "Read-Through"
        RT_Cache{Near Cache\nL1}
        RT_Redis[(Redis L2)]
        RT_DB[(Database)]
        RT_Cache -- hit --> Client
        RT_Cache -- miss --> RT_Redis
        RT_Redis -- hit --> RT_Cache
        RT_Redis -- miss --> RT_DB
        RT_DB -- load --> RT_Redis
        RT_Redis -- fill --> RT_Cache
    end

    subgraph "Write-Through"
        WT_Cache{Near Cache\nL1}
        WT_Redis[(Redis L2)]
        WT_DB[(Database)]
        Client -- save --> WT_Cache
        WT_Cache -- sync --> WT_Redis
        WT_Redis -- sync --> WT_DB
    end

    subgraph "Write-Behind"
        WB_Cache{Near Cache\nL1}
        WB_Redis[(Redis L2)]
        WB_DB[(Database)]
        WB_Queue[[Async Queue]]
        Client -- save --> WB_Cache
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
    class AbstractJdbcRedissonRepository {
        <<abstract>>
        +cacheName: String
        +config: RedisCacheConfig
        +entityTable: T
        +findById(id): E?
        +findAll(): List~E~
        +saveAll(entities): List~E~
        +deleteById(id)
        +invalidate(ids)
        #doInsertEntity(stmt, entity)
        #doUpdateEntity(stmt, entity)
        #toEntity(row): E
    }

    class UserCacheRepository {
        +cacheName = "exposed:users"
        +config = READ_WRITE_THROUGH_WITH_NEAR_CACHE
        +entityTable: UserTable
        #doInsertEntity(stmt, entity)
        #doUpdateEntity(stmt, entity)
    }

    class UserCredentialsCacheRepository {
        +cacheName = "exposed:user-credentials"
        +config = READ_ONLY_WITH_NEAR_CACHE
        +entityTable: UserCredentialsTable
    }

    class UserEventCacheRepository {
        +cacheName = "exposed:user-events"
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

AbstractJdbcRedissonRepository <|-- UserCacheRepository
AbstractJdbcRedissonRepository <|-- UserCredentialsCacheRepository
AbstractJdbcRedissonRepository <|-- UserEventCacheRepository
AbstractJdbcRedissonRepository --> RedisCacheConfig

    style AbstractJdbcRedissonRepository fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style UserCacheRepository fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style UserCredentialsCacheRepository fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style UserEventCacheRepository fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style RedisCacheConfig fill:#FFFDE7,stroke:#FFF176,color:#F57F17
```

---

## Request Processing Flow — Write-Behind Async Event Loading

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
sequenceDiagram
    participant C as Client
    participant R as UserEventCacheRepository
    participant NC as Near Cache (L1)
    participant RD as Redis (L2)
    participant Q as Async Write Queue
    participant DB as Database
    C ->> R: saveAll(events)
    R ->> NC: put events (immediately)
    R ->> RD: put events (immediately)
    RD -->> C: Save complete response
    Note over RD, Q: Async batch flush
    RD ->> Q: enqueue dirty entries
    Q ->> DB: batchInsert (Exposed)
    DB -->> Q: commit
    Q -->> RD: flush complete
    C ->> R: findById(id)
    R ->> NC: get(id)
    NC -->> R: hit → return
    R -->> C: UserEventRecord
```

---

## Request Processing Flow — Read-Through + Write-Through (User)

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
sequenceDiagram
    participant C as Client
    participant R as UserCacheRepository
    participant NC as Near Cache (L1)
    participant RD as Redis (L2)
    participant DB as Database
    C ->> R: findById(id)
    R ->> NC: get(id)
    NC -->> R: miss
    R ->> RD: get(id)
    RD -->> R: miss
    R ->> DB: SELECT (Exposed)
    DB -->> R: UserRecord
    R ->> RD: put(id, record)
    R ->> NC: put(id, record)
    R -->> C: UserRecord (first query)
    C ->> R: findById(id)
    R ->> NC: get(id)
    NC -->> R: hit
    R -->> C: UserRecord (cache hit)
    C ->> R: save(updated)
    R ->> DB: UPDATE (Exposed, synchronous)
    R ->> RD: put(id, updated)
    R ->> NC: put(id, updated)
    R -->> C: Save complete
```

---

## Key Configuration

### application.yml

```yaml
server:
    port: 8080
    compression:
        enabled: true
    tomcat:
        threads:
            max: 8000          # High value allowed since Virtual Thread-based
            min-spare: 20
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

### RedissonConfig Key Settings

| Item                        | Value                 | Description                |
|-----------------------------|-----------------------|---------------------------|
| `connectionPoolSize`        | 256                   | Max Redis connection pool size |
| `connectionMinimumIdleSize` | 32                    | Min connections always maintained |
| `timeout`                   | 5000ms                | Command response wait time |
| `retryAttempts`             | 3                     | Retry count on failure     |
| `codec`                     | LZ4ForyComposite      | LZ4 compression + Fury serialization |
| `executor`                  | VirtualThreadExecutor | Redisson internal thread pool |

---

## Key Components

| File/Area                                                 | Description                           |
|-------------------------------------------------------|---------------------------------------|
| `domain/repository/UserCacheRepository.kt`            | Read-Through + Write-Through          |
| `domain/repository/UserCredentialsCacheRepository.kt` | Read-Only Cache                       |
| `domain/repository/UserEventCacheRepository.kt`       | Write-Behind                          |
| `config/RedissonConfig.kt`                            | Redis/Redisson connection config      |
| `config/TomcatVirtualThreadConfig.kt`                 | Replace Tomcat Virtual Thread Executor|

---

## How to Test

```bash
# Unit/integration tests (Testcontainers auto-starts Redis)
./gradlew :11-high-performance:01-cache-strategies:test

# Run application
./gradlew :11-high-performance:01-cache-strategies:bootRun
```

### API Endpoints

```bash
# User (Read-Through / Write-Through)
GET  /users/{id}
POST /users

# UserCredentials (Read-Only Cache)
GET  /user-credentials/{username}
DELETE /user-credentials  # Cache invalidation

# UserEvent (Write-Behind)
GET  /user-events/{id}
POST /user-events/bulk
```

---

## Practice Checklist

- Measure response time difference between cache hit and miss
- Verify DB is immediately updated after Write-Through save
- Verify final DB count with Awaitility after Write-Behind bulk load
- Confirm DB fallback path works on Redis failure

---

## Operations Checkpoints

- Align cache invalidation policy (TTL/manual) with data freshness SLA
- Apply Write-Behind only to loss-tolerant data
- Always verify DB fallback path on Redis failure
- Secure sufficient `connectionMinimumIdleSize` to prevent cold start latency

---

## Complex Scenarios

### Read-Through + Write-Through Flow (User)

`UserCacheRepository` queries the DB on cache miss and loads it into the cache (Read-Through), and simultaneously reflects entity updates in both the DB and cache (Write-Through).

- Related file: [`domain/repository/UserCacheRepository.kt`](src/main/kotlin/exposed/examples/cache/domain/repository/UserCacheRepository.kt)
- Verification test: [`UserCacheRepositoryTest.kt`](src/test/kotlin/exposed/examples/cache/domain/repository/UserCacheRepositoryTest.kt)

### Write-Behind Bulk Event Async Propagation (UserEvent)

`UserEventCacheRepository` pre-stores events in Redis and then batch-saves to DB asynchronously. Verifies the final DB count with Awaitility after bulk loading.

- Related file: [`domain/repository/UserEventCacheRepository.kt`](src/main/kotlin/exposed/examples/cache/domain/repository/UserEventCacheRepository.kt)
- Verification test: [`UserEventCacheRepositoryTest.kt`](src/test/kotlin/exposed/examples/cache/domain/repository/UserEventCacheRepositoryTest.kt)

### Cache Invalidation (UserCredentials)

`UserCredentialsCacheRepository` applies a Read-Only cache strategy and provides an API for explicitly invalidating cache for a specific list of IDs.

- Related file: [`domain/repository/UserCredentialsCacheRepository.kt`](src/main/kotlin/exposed/examples/cache/domain/repository/UserCredentialsCacheRepository.kt)
- Verification test: [`UserCredentialsCacheRepositoryTest.kt`](src/test/kotlin/exposed/examples/cache/domain/repository/UserCredentialsCacheRepositoryTest.kt)

---

## Next Module

- [`../02-cache-strategies-coroutines/README.md`](../02-cache-strategies-coroutines/README.md)
