# 캐시 전략 - Coroutines (02-cache-strategies-coroutines)

[English](./README.md) | 한국어

`01-cache-strategies`의 코루틴/논블로킹 버전입니다. WebFlux + Netty + Coroutines 환경에서 `suspend` 기반 캐시 접근 패턴을 실습합니다.

## 학습 목표

- `suspend` 기반 캐시/DB 접근 패턴을 익힌다.
- 이벤트 루프 친화적인 캐시 처리 모델을 구현한다.
- 동시 연결이 많은 상황에서 안정성을 검증한다.

## 선수 지식

- [`../08-coroutines/README.md`](../08-coroutines/README.md)
- [`../01-cache-strategies/README.md`](../01-cache-strategies/README.md)

---

## 개요

`AbstractSuspendedJdbcRedissonRepository`를 상속하면 `suspend` 함수 형태로 캐시 전략을 적용할 수 있습니다. 내부적으로
`newSuspendedTransaction`을 사용해 Exposed DB 접근을 코루틴 컨텍스트에서 처리합니다. Netty 이벤트 루프를 블로킹하지 않으므로 고동시성 환경에서도 스레드 풀 고갈이 발생하지 않습니다.

---

## 캐시 전략 아키텍처

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
        WT_DB -->|완료| WT_Cache
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

## 클래스 구조

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

## 요청 처리 흐름 — Write-Behind 비동기 이벤트 적재 (코루틴)

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
    R ->> NC: put events (즉시)
    R ->> RD: put events (즉시)
    RD -->> Ctrl: 저장 완료
    Ctrl -->> C: 200 OK
    Note over RD, Q: 코루틴 비동기 배치 플러시
    RD ->> Q: enqueue dirty entries
    Q ->> DB: newSuspendedTransaction { batchInsert }
    DB -->> Q: commit
    Q -->> RD: flush 완료
    C ->> Ctrl: GET /user-events/{id} (suspend)
    Ctrl ->> R: findById(id) [suspend]
    R ->> NC: get(id)
    NC -->> R: hit
    R -->> Ctrl: UserEventRecord
    Ctrl -->> C: 200 OK + body
```

---

## 요청 처리 흐름 — Read-Through + Write-Through (코루틴 User)

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
    Ctrl -->> C: 200 OK (첫 번째 조회)
    C ->> Ctrl: GET /users/{id}
    Ctrl ->> R: findById(id) [suspend]
    R ->> NC: get(id)
    NC -->> Ctrl: hit → UserRecord
    Ctrl -->> C: 200 OK (캐시 적중)
    C ->> Ctrl: POST /users (save)
    Ctrl ->> R: saveAll([user]) [suspend]
    R ->> DB: newSuspendedTransaction { UPDATE }
    R ->> RD: put(id, updated)
    R ->> NC: put(id, updated)
    R -->> Ctrl: 저장 완료
    Ctrl -->> C: 200 OK
```

---

## 주요 설정

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

### NettyConfig 주요 설정

| 항목                    | 값                                 | 설명                         |
|-----------------------|-----------------------------------|----------------------------|
| `SO_BACKLOG`          | 8,000                             | 대기 연결 큐 크기                 |
| `SO_KEEPALIVE`        | true                              | TCP keepalive 활성화          |
| `ReadTimeoutHandler`  | 10s                               | 읽기 타임아웃                    |
| `WriteTimeoutHandler` | 10s                               | 쓰기 타임아웃                    |
| `maxConnections`      | 8,000                             | ConnectionProvider 최대 연결 수 |
| `maxIdleTime`         | 30s                               | 유휴 연결 유지 시간                |
| `loopResources`       | `availableProcessors * 8` (최소 64) | 이벤트 루프 스레드 수               |

---

## 주요 구성 요소

| 파일/영역                                                 | 설명                                     |
|-------------------------------------------------------|----------------------------------------|
| `domain/repository/UserCacheRepository.kt`            | Suspended Read-Through + Write-Through |
| `domain/repository/UserCredentialsCacheRepository.kt` | Suspended Read-Only Cache              |
| `domain/repository/UserEventCacheRepository.kt`       | Suspended Write-Behind                 |
| `config/NettyConfig.kt`                               | Netty 이벤트 루프 및 연결 풀 튜닝                 |
| `config/RedissonConfig.kt`                            | Redisson 클라이언트 설정                      |
| `controller/*Controller.kt`                           | `suspend` 엔드포인트                        |

---

## 테스트 방법

```bash
# 단위/통합 테스트 실행 (Testcontainers가 Redis를 자동 시작)
./gradlew :11-high-performance:02-cache-strategies-coroutines:test

# 애플리케이션 실행
./gradlew :11-high-performance:02-cache-strategies-coroutines:bootRun
```

### API 엔드포인트 (WebFlux / suspend)

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

## 실습 체크리스트

- `suspend` 경로에서 캐시 적중/미스 동작 검증
- `flatMapMerge`로 100개 병렬 조회 후 두 번째 조회 시간이 더 짧음을 `measureTimeMillis`로 측정
- 대량 이벤트 적재 시 비동기 반영 지연을 `untilSuspending`으로 관측
- WebTestClient 기반 통합 테스트로 회귀 방지

---

## 운영 체크포인트

- 이벤트 루프 블로킹 호출 금지 (`runBlocking` 사용 금지)
- 비동기 반영 지연이 허용 가능한 도메인인지 사전 합의
- 코루틴 취소/타임아웃 시 데이터 정합성 검증
- `ReactorResourceFactory`를 글로벌 리소스에서 분리해 테스트 간 간섭 방지

---

## 복잡한 시나리오

### 코루틴 Read-Through + Write-Through 흐름 (User)

`UserCacheRepository`(suspend 버전)는 `newSuspendedTransaction` 안에서 캐시 미스 시 DB를 조회하고 Redis에 적재합니다.

- 관련 파일: [`domain/repository/UserCacheRepository.kt`](src/main/kotlin/exposed/examples/cache/coroutines/domain/repository/UserCacheRepository.kt)
- 검증 테스트: [
  `UserCacheRepositoryTest.kt`](src/test/kotlin/exposed/examples/cache/coroutines/domain/repository/UserCacheRepositoryTest.kt)

### 코루틴 Write-Behind 대량 이벤트 비동기 반영 (UserEvent)

`UserEventCacheRepository`(suspend 버전)는 이벤트를 Redis에 선반영 후 코루틴 기반으로 DB에 일괄 저장합니다. 500개 bulk insert 후 Awaitility +
`untilSuspending`으로 비동기 DB 반영 완료를 대기합니다.

- 관련 파일: [`domain/repository/UserEventCacheRepository.kt`](src/main/kotlin/exposed/examples/cache/coroutines/domain/repository/UserEventCacheRepository.kt)
- 검증 테스트: [
  `UserEventCacheRepositoryTest.kt`](src/test/kotlin/exposed/examples/cache/coroutines/domain/repository/UserEventCacheRepositoryTest.kt)

### 코루틴 캐시 무효화 (UserCredentials)

`UserCredentialsCacheRepository`(suspend 버전)는 Read-Only 캐시와 ID 기반 명시적 무효화를 코루틴 환경에서 제공합니다.

- 관련 파일: [`domain/repository/UserCredentialsCacheRepository.kt`](src/main/kotlin/exposed/examples/cache/coroutines/domain/repository/UserCredentialsCacheRepository.kt)
- 검증 테스트: [
  `UserCredentialsCacheRepositoryTest.kt`](src/test/kotlin/exposed/examples/cache/coroutines/domain/repository/UserCredentialsCacheRepositoryTest.kt)

---

## 다음 모듈

- [`../03-routing-datasource/README.md`](../03-routing-datasource/README.md)
