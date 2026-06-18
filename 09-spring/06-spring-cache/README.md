# 09 Spring: Spring Cache (06)

English | [한국어](./README.ko.md)

This module adds Spring Cache to a country lookup repository backed by Exposed. It uses Redis through `RedisCacheManager`, LZ4+Fory serialization, `@Cacheable`, and `@CacheEvict`; the tests check cache hit/miss behavior, invalidation, and transaction-aware cache updates.

## Learning Goals

- Connect Spring Cache to Redis with `@EnableCaching` + `RedisCacheManager` configuration.
- Design cache keys explicitly with `@Cacheable(key = "'country:' + #code")`.
- Prevent stale data by immediately invalidating cache on changes with `@CacheEvict`.
- Understand the benefit of not opening a transaction on cache hits when combining `transaction { }` blocks with `@Cacheable`.

## Prerequisites

- [`../04-exposed-repository/README.md`](../04-exposed-repository/README.md)
- Spring Cache abstraction basics

## Architecture

![06 spring cache Class Structure diagram](../../docs/images/readme-diagrams/09-spring-06-spring-cache-class-01.png)

## Key Concepts

### Cache Configuration (LZ4+Fory Serialization, TTL 10min)

```kotlin
@Configuration
@EnableCaching
class LettuceCacheConfig {

    @Bean
    fun redisCacheConfiguration(): RedisCacheConfiguration =
        RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(RedisBinarySerializers.LZ4Fory)  // Compressed serialization
            )
            .entryTtl(Duration.ofMinutes(10))  // Default TTL

    @Bean
    fun cacheManager(
        connectionFactory: RedisConnectionFactory,
        cacheConfiguration: RedisCacheConfiguration,
    ): CacheManager = RedisCacheManager.builder(connectionFactory)
        .transactionAware()      // Reflect cache after transaction commit
        .cacheDefaults(cacheConfiguration)
        .build()
}
```

### Repository Cache Declaration

```kotlin
@Component
@CacheConfig(cacheNames = [COUNTRY_CACHE_NAME])   // Common cache name configuration
class CountryRepository(private val cacheManager: CacheManager) {

    companion object {
        const val COUNTRY_CACHE_NAME = "cache:code:country"
    }

    // Only opens transaction {} for DB query on cache miss
    @Cacheable(key = "'country:' + #code")
    fun findByCode(code: String): CountryRecord? {
        return transaction {
            CountryTable.selectAll()
                .where { CountryTable.code eq code }
                .singleOrNull()
                ?.let { CountryRecord(code = it[CountryTable.code], name = it[CountryTable.name]) }
        }
    }

    // Immediately invalidate cache entry after DB update
    @Transactional
    @CacheEvict(key = "'country:' + #countryRecord.code")
    fun update(countryRecord: CountryRecord): Int =
        CountryTable.update({ CountryTable.code eq countryRecord.code }) {
            it[name] = countryRecord.name
            it[description] = countryRecord.description
        }

    // Clear all cache entries
    @CacheEvict(cacheNames = [COUNTRY_CACHE_NAME], allEntries = true)
    fun evictCacheAll() { /* Handled by Spring AOP */ }
}
```

## Cache Flow

![Cache Flow diagram](../../docs/images/readme-diagrams/09-spring-06-spring-cache-architecture-02.png)

## Domain Model

```kotlin
object CountryTable: IntIdTable("countries") {
    val code = char("code", 2).uniqueIndex()   // ISO 2-letter country code
    val name = varchar("name", 50)
    val description = text("description").nullable()
}

data class CountryRecord(
    val code: String,
    val name: String,
    val description: String? = null,
): Serializable   // Implements Serializable for Redis serialization
```

## Cache Hit/Miss Sequence

![Cache Hit/Miss Sequence diagram](../../docs/images/readme-diagrams/09-spring-06-spring-cache-sequence-03.png)

## Cache Key Design Principles

| Scenario     | Key Pattern              | Example                              |
|-------------|--------------------------|--------------------------------------|
| Single lookup | `'cacheName:' + #param` | `'country:' + #code` → `country:KR` |
| Full lookup   | `'cacheName:all'`       | `'country:all'`                      |
| Composite key | `#a + ':' + #b`         | `#userId + ':' + #orderId`           |

## How to Run

```bash
# Redis Testcontainer starts automatically
./gradlew :06-spring-cache:test

# Test log summary
./bin/repo-test-summary -- ./gradlew :06-spring-cache:test
```

## Practice Checklist

- Verify no DB query logs on the second consecutive `findByCode("KR")` call
- Validate that updated data is returned when calling `findByCode()` after `update()`
- Confirm all keys are deleted from Redis after `evictCacheAll()`
- Verify that rolled-back transaction cache is not reflected when `RedisCacheManager.transactionAware()` is configured

## Performance & Stability Checkpoints

- Adjust TTL to match data freshness requirements (SLA) -- the default 10 minutes may not suit all domains
- Consider implementing `CacheErrorHandler` so `@Cacheable` does not throw exceptions on Redis failures
- Without `transactionAware()`, data may remain in cache even after rollback

## Next Module

- [`../07-spring-suspended-cache/README.md`](../07-spring-suspended-cache/README.md)
