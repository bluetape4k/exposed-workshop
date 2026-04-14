# 09 Spring: Spring Cache (06)

[English](./README.md) | 한국어

Spring Cache 추상화와 Exposed를 통합하는 모듈입니다.
`@Cacheable`, `@CacheEvict` 어노테이션으로 Redis 기반 캐시를 선언적으로 적용하고, 캐시 히트/미스/무효화 흐름과 DB 정합성 관리 전략을 학습합니다.

## 학습 목표

- `@EnableCaching` + `RedisCacheManager` 구성으로 Spring Cache를 Redis에 연결한다.
- `@Cacheable(key = "'country:' + #code")` 로 캐시 키를 명시적으로 설계한다.
- `@CacheEvict` 로 변경 시 캐시를 즉시 무효화해 stale 데이터를 방지한다.
- `transaction { }` 블록과 `@Cacheable` 을 조합할 때 캐시 히트 시 트랜잭션을 열지 않는 이점을 이해한다.

## 선수 지식

- [`../04-exposed-repository/README.ko.md`](../04-exposed-repository/README.ko.md)
- Spring Cache 추상화 기본 개념

## 아키텍처

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
classDiagram
    class LettuceCacheConfig {
        <<@Configuration @EnableCaching>>
        +redisCacheConfiguration() RedisCacheConfiguration
        +cacheManager(factory, config) CacheManager
    }
    class RedisCacheManager {
        +transactionAware: true
        +defaultTtl: 10min
        +serializer: LZ4Fory
    }
    class CountryRepository {
        <<@Component @CacheConfig>>
        +COUNTRY_CACHE_NAME: "cache:code:country"
        +findByCode(code) CountryRecord?
        +update(countryRecord) Int
        +evictCacheAll()
    }
    class CountryRecord {
        +code: String
        +name: String
        +description: String?
    }
    class CountryTable {
        <<IntIdTable: countries>>
        +code: Column~String~
        +name: Column~String~
        +description: Column~String?~
    }

    LettuceCacheConfig --> RedisCacheManager : creates
    CountryRepository --> RedisCacheManager : uses
    CountryRepository --> CountryTable : queries
    CountryRepository --> CountryRecord : returns

    style LettuceCacheConfig fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style RedisCacheManager fill:#FFEBEE,stroke:#EF9A9A,color:#C62828
    style CountryRepository fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style CountryRecord fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style CountryTable fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
```

## 핵심 개념

### Cache 설정 (LZ4+Fory 직렬화, TTL 10분)

```kotlin
@Configuration
@EnableCaching
class LettuceCacheConfig {

    @Bean
    fun redisCacheConfiguration(): RedisCacheConfiguration =
        RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(RedisBinarySerializers.LZ4Fory)  // 압축 직렬화
            )
            .entryTtl(Duration.ofMinutes(10))  // 기본 TTL

    @Bean
    fun cacheManager(
        connectionFactory: RedisConnectionFactory,
        cacheConfiguration: RedisCacheConfiguration,
    ): CacheManager = RedisCacheManager.builder(connectionFactory)
        .transactionAware()      // 트랜잭션 커밋 후 캐시 반영
        .cacheDefaults(cacheConfiguration)
        .build()
}
```

### Repository 캐시 선언

```kotlin
@Component
@CacheConfig(cacheNames = [COUNTRY_CACHE_NAME])   // 캐시 이름 공통 설정
class CountryRepository(private val cacheManager: CacheManager) {

    companion object {
        const val COUNTRY_CACHE_NAME = "cache:code:country"
    }

    // 캐시 미스 시만 transaction {} 열어 DB 조회
    @Cacheable(key = "'country:' + #code")
    fun findByCode(code: String): CountryRecord? {
        return transaction {
            CountryTable.selectAll()
                .where { CountryTable.code eq code }
                .singleOrNull()
                ?.let { CountryRecord(code = it[CountryTable.code], name = it[CountryTable.name]) }
        }
    }

    // DB 갱신 후 해당 캐시 엔트리 즉시 무효화
    @Transactional
    @CacheEvict(key = "'country:' + #countryRecord.code")
    fun update(countryRecord: CountryRecord): Int =
        CountryTable.update({ CountryTable.code eq countryRecord.code }) {
            it[name] = countryRecord.name
            it[description] = countryRecord.description
        }

    // 전체 캐시 비우기
    @CacheEvict(cacheNames = [COUNTRY_CACHE_NAME], allEntries = true)
    fun evictCacheAll() { /* Spring AOP가 처리 */ }
}
```

## 캐시 흐름

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
flowchart TD
    A[클라이언트 findByCode 호출] --> B{Redis 캐시 히트?}
    B -- 예 --> C[캐시에서 CountryRecord 반환]
    B -- 아니오 --> D[transaction 블록 시작]
    D --> E[CountryTable.selectAll WHERE code = ?]
    E --> F[DB에서 CountryRecord 조회]
    F --> G[Redis에 캐시 저장\ncache:code:country:KR]
    G --> H[CountryRecord 반환]

    I[클라이언트 update 호출] --> J[@Transactional 시작]
    J --> K[CountryTable.update]
    K --> L[트랜잭션 커밋]
    L --> M[@CacheEvict 실행\ncache:code:country:KR 삭제]

    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef pink fill:#FCE4EC,stroke:#F48FB1,color:#AD1457
    classDef yellow fill:#FFFDE7,stroke:#FFF176,color:#F57F17
    classDef teal fill:#E0F2F1,stroke:#80CBC4,color:#00695C

    class A,I blue
    class B,D yellow
    class C,H green
    class E,F orange
    class G,M pink
    class J,K,L teal
```

## 도메인 모델

```kotlin
object CountryTable: IntIdTable("countries") {
    val code = char("code", 2).uniqueIndex()   // ISO 2자리 국가 코드
    val name = varchar("name", 50)
    val description = text("description").nullable()
}

data class CountryRecord(
    val code: String,
    val name: String,
    val description: String? = null,
): Serializable   // Redis 직렬화를 위해 Serializable 구현
```

## 캐시 히트/미스 시퀀스

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"', 'actorBkg': '#E3F2FD', 'actorBorder': '#90CAF9', 'actorTextColor': '#1565C0', 'actorLineColor': '#90CAF9', 'activationBkgColor': '#E8F5E9', 'activationBorderColor': '#A5D6A7', 'labelBoxBkgColor': '#FFF3E0', 'labelBoxBorderColor': '#FFCC80', 'labelTextColor': '#E65100', 'loopTextColor': '#6A1B9A', 'noteBkgColor': '#F3E5F5', 'noteBorderColor': '#CE93D8', 'noteTextColor': '#6A1B9A', 'signalColor': '#1565C0', 'signalTextColor': '#1565C0'}}}%%
sequenceDiagram
    participant Client
    participant CountryRepository as CountryRepository (@Cacheable)
    participant CacheManager as RedisCacheManager
    participant DB

    Client->>CountryRepository: findByCode("KR")
    CountryRepository->>CacheManager: get("country:KR")
    alt 캐시 히트
        CacheManager-->>CountryRepository: CountryRecord
        CountryRepository-->>Client: CountryRecord (DB 조회 없음)
    else 캐시 미스
        CacheManager-->>CountryRepository: null
        CountryRepository->>DB: transaction { SELECT WHERE code='KR' }
        DB-->>CountryRepository: CountryRecord
        CountryRepository->>CacheManager: put("country:KR", CountryRecord)
        CountryRepository-->>Client: CountryRecord
    end

    Client->>CountryRepository: update(countryRecord)
    Note over CountryRepository: @Transactional 시작
    CountryRepository->>DB: CountryTable.update(...)
    DB-->>CountryRepository: 갱신 행 수
    Note over CountryRepository: 트랜잭션 커밋 후 @CacheEvict 실행
    CountryRepository->>CacheManager: evict("country:KR")
    CountryRepository-->>Client: 갱신 행 수
```

## 캐시 키 설계 원칙

| 상황    | 키 패턴             | 예시                                  |
|-------|------------------|-------------------------------------|
| 단건 조회 | `'캐시명:' + #파라미터` | `'country:' + #code` → `country:KR` |
| 전체 조회 | `'캐시명:all'`      | `'country:all'`                     |
| 복합 키  | `#a + ':' + #b`  | `#userId + ':' + #orderId`          |

## 실행 방법

```bash
# Redis Testcontainer를 자동으로 기동합니다
./gradlew :09-spring:06-spring-cache:test

# 테스트 로그 요약
./bin/repo-test-summary -- ./gradlew :09-spring:06-spring-cache:test
```

## 실습 체크리스트

- `findByCode("KR")` 두 번 연속 호출 시 두 번째 호출에서 DB 쿼리 로그가 없는지 확인
- `update()` 후 `findByCode()` 재호출 시 갱신된 데이터가 반환되는지 검증
- `evictCacheAll()` 후 모든 키가 Redis에서 삭제되는지 확인
- `RedisCacheManager.transactionAware()` 설정 시 롤백된 트랜잭션의 캐시가 반영되지 않음을 검증

## 성능·안정성 체크포인트

- TTL을 데이터 신선도 요구(SLA)에 맞게 조정 — 기본 10분이 모든 도메인에 적합하지 않음
- Redis 장애 시 `@Cacheable`이 예외를 던지지 않도록 `CacheErrorHandler` 구현 고려
- `transactionAware()` 미적용 시 롤백 후에도 캐시에 데이터가 남을 수 있음

## 다음 모듈

- [`../07-spring-suspended-cache/README.ko.md`](../07-spring-suspended-cache/README.ko.md)
