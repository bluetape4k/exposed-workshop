# 캐시 전략 (Caching Strategies)

Redisson + Exposed 를 활용하여 다양한 캐시 전략을 구현한 예제입니다. 가장 많이 사용하는 **Cache Aside** 전략은
`09-Spring/06-spring-cache` 에 있으며, 이 모듈에서는 **Read Through**, **Write Through**, **Write Behind** 전략을 Redisson의
`MapCache`를 활용하여 구현합니다.

## 기술 스택

| 구분         | 기술                                             |
|------------|------------------------------------------------|
| Framework  | Spring Boot (Web MVC) + Tomcat Virtual Threads |
| ORM        | Exposed (DAO + DSL)                            |
| Cache      | Redisson (`MapCache`, Near Cache 포함)           |
| Serializer | Fory / Kryo5                                   |
| Compressor | LZ4 / Snappy / Zstd                            |
| Near Cache | Caffeine                                       |
| DB         | H2 (기본) / MySQL / PostgreSQL (Testcontainers)  |
| Test       | JUnit 5, Kluent, Awaitility                    |

## 프로젝트 구조

```
src/main/kotlin/exposed/examples/cache/
├── CacheStrategyApplication.kt          # Spring Boot 애플리케이션 진입점
├── config/
│   ├── ExposedConfig.kt                 # Exposed Database 설정
│   ├── RedissonConfig.kt                # Redisson 클라이언트 설정
│   └── TomcatVirtualThreadConfig.kt     # Tomcat Virtual Thread 설정
├── controller/
│   ├── IndexController.kt               # 헬스체크 등 기본 엔드포인트
│   ├── UserController.kt                # User CRUD (Read/Write Through)
│   ├── UserCredentialsController.kt     # UserCredentials 조회 (Read-Only)
│   └── UserEventController.kt           # UserEvent 저장 (Write Behind)
├── domain/
│   ├── model/
│   │   ├── User.kt                      # UserTable, UserEntity, UserRecord
│   │   ├── UserCredentials.kt           # UserCredentialsTable, UserCredentialsRecord
│   │   └── UserEvent.kt                 # UserEventTable, UserEventRecord
│   └── repository/
│       ├── UserCacheRepository.kt               # Read/Write Through 캐시 저장소
│       ├── UserCredentialsCacheRepository.kt     # Read-Only 캐시 저장소
│       └── UserEventCacheRepository.kt           # Write Behind 캐시 저장소
└── utils/
    └── DataFakers.kt                    # 테스트 데이터 생성 유틸
```

## 캐시 전략별 상세 설명

### Read Through

```
Client ← Cache ← DB
```

- 클라이언트가 데이터를 요청하면, 먼저 **캐시에서 조회**합니다.
- 캐시에 데이터가 없으면(Cache Miss) **DB에서 읽어온 후 캐시에 저장**하고 결과를 반환합니다.
- 이후 동일 키 요청 시 캐시에서 바로 반환하므로 DB 부하를 줄일 수 있습니다.
- **적용 예시**: `UserCacheRepository`, `UserCredentialsCacheRepository`

```kotlin
// Read Through - 캐시에 없으면 DB에서 조회 후 캐시에 저장
@Repository
class UserCacheRepository(redissonClient: RedissonClient): AbstractExposedCacheRepository<UserRecord, Long>(
    redissonClient = redissonClient,
    cacheName = "exposed:users",
    config = RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(deleteFromDBOnInvalidate = true)
)
```

### Write Through

```
Client → Cache → DB (동기)
```

- 클라이언트가 데이터를 저장/수정하면, **캐시에 먼저 저장**한 뒤 **즉시 DB에도 동기적으로 반영**합니다.
- 데이터 일관성이 보장되지만, 쓰기 지연 시간(latency)이 증가할 수 있습니다.
- 캐시 Invalidate 시 `deleteFromDBOnInvalidate = true` 옵션으로 DB 데이터도 함께 삭제할 수 있습니다.
- **적용 예시**: `UserCacheRepository` (`put()` 호출 시)

```kotlin
// Write Through - 캐시에 저장하면 자동으로 DB에도 동기 저장
repository.put(updatedUser)

// DB에서 직접 읽어 검증 가능
val userFromDB = repository.findByIdFromDb(userId)
```

### Write Behind (Write Back)

```
Client → Cache ──(비동기)──→ DB
```

- 클라이언트가 데이터를 저장하면, **캐시에만 즉시 저장**합니다.
- DB 저장은 **비동기적으로 일정 간격 또는 배치(batch)로 수행**됩니다.
- 쓰기 성능이 매우 뛰어나며, 대량의 이벤트/로그 데이터 적재에 적합합니다.
- 캐시 장애 시 데이터 유실 가능성이 있으므로, 유실 허용 가능한 데이터에 사용해야 합니다.
- **적용 예시**: `UserEventCacheRepository`

```kotlin
// Write Behind - 캐시에 즉시 저장, DB에는 비동기 배치 저장
@Repository
class UserEventCacheRepository(redissonClient: RedissonClient):
    AbstractSuspendedExposedCacheRepository<UserEventRecord, Long>(
        redissonClient = redissonClient,
        cacheName = "exposed:user-events",
        config = RedisCacheConfig.WRITE_BEHIND_WITH_NEAR_CACHE,
    )
```

### Read-Only Cache

```
Client ← Cache ← DB (읽기 전용, 쓰기 불가)
```

- DB 데이터를 캐시에 적재하여 읽기만 수행합니다.
- `doUpdateEntity`, `doInsertEntity`를 구현하지 않으므로 캐시를 통한 쓰기가 불가능합니다.
- 변경이 거의 없는 마스터 데이터나 인증 정보 캐시에 적합합니다.
- **적용 예시**: `UserCredentialsCacheRepository`

## Near Cache

모든 캐시 전략에 **Near Cache (Caffeine 기반)
** 가 적용되어 있어, 로컬 메모리에서 먼저 조회 후 Redis를 참조합니다. 이를 통해 Redis 네트워크 왕복 시간(RTT)을 줄이고 읽기 성능을 극대화합니다.

```
Client ← [Near Cache (Caffeine)] ← [Redis Cache] ← DB
```

## REST API 엔드포인트

### UserController (`/users`) - Read/Write Through

| Method   | Path                     | 설명                        |
|----------|--------------------------|---------------------------|
| `GET`    | `/users`                 | 전체 사용자 조회 (limit 파라미터 지원) |
| `GET`    | `/users/{id}`            | 단일 사용자 조회 (Read Through)  |
| `GET`    | `/users/all?ids=1,2,3`   | 복수 사용자 일괄 조회              |
| `POST`   | `/users`                 | 사용자 저장/수정 (Write Through) |
| `DELETE` | `/users/invalidate?ids=` | 지정 ID 캐시 무효화              |
| `DELETE` | `/users/invalidate/all`  | 전체 캐시 무효화                 |

### UserCredentialsController (`/user-credentials`) - Read-Only

| Method   | Path                                | 설명            |
|----------|-------------------------------------|---------------|
| `GET`    | `/user-credentials`                 | 전체 인증정보 조회    |
| `GET`    | `/user-credentials/{id}`            | 단일 인증정보 조회    |
| `GET`    | `/user-credentials/all?ids=`        | 복수 인증정보 일괄 조회 |
| `DELETE` | `/user-credentials/invalidate?ids=` | 지정 ID 캐시 무효화  |
| `DELETE` | `/user-credentials/invalidate/all`  | 전체 캐시 무효화     |

### UserCredentialsController (`/user-credentials`) - Read-Only

| Method   | Path                                            | 설명                                |
|----------|-------------------------------------------------|-----------------------------------|
| `GET`    | `/user-credentials/{id}`                        | 단일 사용자 인증 정보 조회 (Read-Only Cache) |
| `DELETE` | `/user-credentials/invalidate?ids=`             | 지정 ID 캐시 무효화                      |
| `DELETE` | `/user-credentials/invalidate/all`              | 전체 캐시 무효화                         |
| `DELETE` | `/user-credentials/invalidate/pattern?pattern=` | 패턴 매칭 캐시 무효화                      |

### UserEventController (`/user-events`) - Write Behind

| Method | Path                | 설명                       |
|--------|---------------------|--------------------------|
| `POST` | `/user-events`      | 단일 이벤트 저장 (Write Behind) |
| `POST` | `/user-events/bulk` | 대량 이벤트 일괄 저장             |

## 주요 설정

### Tomcat Virtual Threads

Java 21+ 환경에서 Tomcat의 요청 처리 스레드를 Virtual Thread로 교체하여, 블로킹 I/O(DB, Redis) 호출 시에도 높은 동시성을 확보합니다.

```kotlin
@Bean
fun protocolHandlerVirtualThreadExecutorCustomizer(): TomcatProtocolHandlerCustomizer<*> {
    return TomcatProtocolHandlerCustomizer<ProtocolHandler> { protocolHandler ->
        protocolHandler.executor = Executors.newVirtualThreadPerTaskExecutor()
    }
}
```

### Redisson Client

Testcontainers를 통해 Redis 서버를 자동 시작하며, Connection Pool 및 Retry 정책이 설정되어 있습니다.

### Exposed Database

`DatabaseConfig`에서 엔티티 캐시 크기(`maxEntitiesToStoreInCachePerEntity = 1000`), 중첩 트랜잭션(
`useNestedTransactions = true`), 격리 수준(`TRANSACTION_READ_COMMITTED`)을 설정합니다.

## 테스트

```bash
# 전체 테스트 실행
./gradlew :11-high-performance:01-cache-strategies:test
```

### 주요 테스트 시나리오

- **Read Through**: DB 데이터를 캐시에서 읽어오는 속도가 DB 직접 조회보다 빠른지 검증
- **Write Through**: 캐시를 통해 저장한 데이터가 DB에 동기 반영되었는지 검증
- **Write Behind**: 10,000건의 이벤트를 캐시에 저장 후 비동기적으로 DB에 모두 반영되는지 검증 (Awaitility 활용)
- **Cache Invalidation**: 캐시 무효화 후 DB에서 다시 읽어오는지 검증

## 참고

- [Cache Strategies with Redisson, Exposed](https://speakerdeck.com/debop/cache-strategies-with-redisson-and-exposed)
- [캐시 전략들 by Perplexity](https://www.perplexity.ai/search/kaesi-jeonryagdeulyi-teugjinge-JAF35te5SnWTUBsQg5JGSg)
- [Caching patterns](https://docs.aws.amazon.com/whitepapers/latest/database-caching-strategies-using-redis/caching-patterns.html)
- [A Hitchhiker's Guide to Caching](https://hazelcast.com/blog/a-hitchhikers-guide-to-caching-patterns/)
- [Understanding Cache Strategies](https://www.linkedin.com/pulse/decoding-cache-chronicles-understanding-strategies-aside-gopal-kb9kf/)
