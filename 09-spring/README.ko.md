# 09 Spring Integration

[English](./README.md) | 한국어

Spring Boot 환경에서 Exposed를 안정적으로 운영하기 위한 통합 패턴을 다루는 챕터입니다. 자동 설정 기반 연결부터 선언적 트랜잭션, Repository 패턴(동기/코루틴), Spring Cache 통합까지 단계적으로 학습할 수 있는 예제를 제공합니다.

## 챕터 목표

- Spring 트랜잭션과 Exposed 트랜잭션 경계를 정렬해 일관된 데이터 흐름을 설계한다.
- 동기(`Spring MVC`)와 비동기(`Spring WebFlux + 코루틴`) Repository 레이어 표준 패턴을 정립한다.
- 캐시 통합 시 일관성과 성능의 균형을 맞추는 전략을 확인한다.

## 선수 지식

- Spring Boot 기본 (`DataSource`, `@Transactional`, `@Cacheable`)
- `05-exposed-dml` 챕터 내용 (DSL/DAO 기초)
- `08-coroutines` 챕터 내용 (코루틴 모듈 학습 시)

## 포함 모듈

| 모듈                                                                               | 설명                                 | 핵심 기술                                             |
|----------------------------------------------------------------------------------|------------------------------------|---------------------------------------------------|
| [`01-springboot-autoconfigure`](01-springboot-autoconfigure/README.ko.md)           | Spring Boot 자동 설정 기반 Exposed 통합    | `ExposedAutoConfiguration`, `DatabaseInitializer` |
| [`02-transactiontemplate`](02-transactiontemplate/README.ko.md)                     | `TransactionTemplate` 프로그래밍 트랜잭션   | `TransactionTemplate`, `TransactionOperations`    |
| [`03-spring-transaction`](03-spring-transaction/README.ko.md)                       | `@Transactional` 선언적 트랜잭션          | `SpringTransactionManager`, `@Transactional`      |
| [`04-exposed-repository`](04-exposed-repository/README.ko.md)                       | 동기 Repository 패턴 (Spring MVC)      | `JdbcRepository`, DSL/DAO 혼용                      |
| [`05-exposed-repository-coroutines`](05-exposed-repository-coroutines/README.ko.md) | 코루틴 Repository 패턴 (Spring WebFlux) | `newSuspendedTransaction`, suspend fun            |
| [`06-spring-cache`](06-spring-cache/README.ko.md)                                   | Spring Cache + Redis 동기 캐시         | `@Cacheable`, `@CacheEvict`, `RedisCacheManager`  |
| [`07-spring-suspended-cache`](07-spring-suspended-cache/README.ko.md)               | 코루틴 기반 Redis 캐시                    | `LettuceSuspendedCache`, 데코레이터 패턴                 |

## 전체 아키텍처 흐름

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
flowchart TD
    subgraph AutoConfig["01 AutoConfiguration"]
        AC[ExposedAutoConfiguration] --> STM[SpringTransactionManager]
        AC --> DI[DatabaseInitializer]
    end
    subgraph TxControl["02-03 트랜잭션 제어"]
        TT[TransactionTemplate] --> STM
        AT["@Transactional"] --> STM
    end
    subgraph Repo["04-05 Repository 패턴"]
        MVC[MovieExposedRepository\nActorExposedRepository\nSpring MVC] --> AT
        WF[MovieExposedRepository\nActorExposedRepository\nWebFlux Coroutine] --> NST[newSuspendedTransaction]
    end
    subgraph Cache["06-07 캐시 통합"]
        SC[CountryRepository\n@Cacheable/@CacheEvict] --> Redis[(Redis)]
        CC[CachedCountrySuspendedRepository\nLettuceSuspendedCache] --> Redis
        SC --> AT
        CC --> NST
    end
    STM --> DB[(Database)]
    NST --> DB

    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef purple fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef pink fill:#FCE4EC,stroke:#F48FB1,color:#AD1457
    classDef teal fill:#E0F2F1,stroke:#80CBC4,color:#00695C

    class AC,DI blue
    class STM purple
    class TT,AT green
    class MVC,WF teal
    class NST teal
    class SC,CC pink
    class Redis,DB orange
```

## 핵심 패턴 요약

### SpringTransactionManager 등록

```kotlin
@Configuration
@EnableTransactionManagement
class DataSourceConfig: TransactionManagementConfigurer {

    @Bean
    override fun annotationDrivenTransactionManager(): TransactionManager =
        SpringTransactionManager(dataSource(), DatabaseConfig {
            useNestedTransactions = true
        })
}
```

### Repository 패턴 (동기)

```kotlin
@Repository
class MovieExposedRepository: JdbcRepository<Long, MovieRecord> {
    override val table = MovieTable
    override fun ResultRow.toEntity() = toMovieRecord()

    @Transactional(readOnly = true)
    fun searchMovies(params: Map<String, String?>): List<MovieRecord> { ... }
}
```

### Repository 패턴 (코루틴)

```kotlin
@Repository
class MovieExposedRepository: JdbcRepository<Long, MovieRecord> {

    suspend fun create(movie: MovieRecord): MovieRecord = /* newSuspendedTransaction 내부 */
        MovieTable.insertAndGetId { ... }.let { movie.copy(id = it.value) }
}
```

### 캐시 계층 데코레이터 (코루틴)

```kotlin
class CachedCountrySuspendedRepository(
    private val delegate: CountrySuspendedRepository,
    private val cacheManager: LettuceSuspendedCacheManager,
): CountrySuspendedRepository {

    override suspend fun findByCode(code: String): CountryRecord? =
        cache.get(code) ?: delegate.findByCode(code)?.apply { cache.put(code, this) }

    override suspend fun update(countryRecord: CountryRecord): Int {
        cache.evict(countryRecord.code)
        return delegate.update(countryRecord)
    }
}
```

## 권장 학습 순서

```
01-springboot-autoconfigure
        ↓
02-transactiontemplate
        ↓
03-spring-transaction
        ↓
04-exposed-repository ──────────────→ 05-exposed-repository-coroutines
        ↓                                          ↓
06-spring-cache ──────────────────→ 07-spring-suspended-cache
```

## 실행 방법

```bash
# 전체 챕터 테스트
./gradlew :09-spring:01-springboot-autoconfigure:test \
          :09-spring:02-transactiontemplate:test \
          :09-spring:03-spring-transaction:test \
          :09-spring:04-exposed-repository:test \
          :09-spring:05-exposed-repository-coroutines:test \
          :09-spring:06-spring-cache:test \
          :09-spring:07-spring-suspended-cache:test

# 개별 모듈 테스트
./gradlew :09-spring:04-exposed-repository:test

# 테스트 로그 요약
./bin/repo-test-summary -- ./gradlew :09-spring:04-exposed-repository:test
```

## 테스트 포인트

- 트랜잭션 전파/롤백 규칙이 의도대로 작동하는지 검증한다.
- 캐시 적중/미스/무효화 시나리오를 각각 검증한다.
- 코루틴 경로와 동기 경로의 결과 일관성을 비교한다.
- `@Transactional`이 suspend 함수에 적용되지 않음을 확인하고 대안이 올바르게 동작하는지 점검한다.

## 주요 트레이드오프

| 선택                        | 장점               | 단점             |
|---------------------------|------------------|----------------|
| `@Transactional`          | 선언적, 간결          | suspend 함수 미지원 |
| `TransactionTemplate`     | 세밀한 경계 제어        | 코드 복잡도 증가      |
| `newSuspendedTransaction` | 코루틴 네이티브         | 명시적 감싸기 필요     |
| Spring Cache `@Cacheable` | 선언적, 간결          | suspend 함수 미지원 |
| `LettuceSuspendedCache`   | suspend 지원, 논블로킹 | 수동 캐시 로직 작성    |

## 성능·안정성 체크포인트

- `DataSourceTransactionManagerAutoConfiguration`을 반드시 제외해 트랜잭션 매니저 충돌 방지
- Exposed JDBC는 블로킹 드라이버이므로 WebFlux 이벤트 루프에서 직접 호출 금지 — `newSuspendedTransaction` 필수
- 캐시 무효화 누락으로 stale 데이터가 노출되지 않도록 갱신/삭제 시 반드시 `@CacheEvict` 또는 `cache.evict()` 적용
- 커넥션 풀(`HikariCP`) 크기를 동시 트랜잭션 수에 맞게 조정

## 복잡한 시나리오

### Spring 트랜잭션과 Exposed 트랜잭션 경계 정렬

`@Transactional`로 감싼 Spring 트랜잭션 내에서 Exposed DSL 쿼리를 실행할 때,
`SpringTransactionManager`가 두 트랜잭션 경계를 같은 커넥션으로 통합합니다.
`useNestedTransactions = true` 설정으로 SAVEPOINT 기반 중첩 트랜잭션도 지원합니다.

- 관련 모듈: [`03-spring-transaction`](03-spring-transaction/)

### 코루틴 기반 Suspended Cache 데코레이터

`CachedCountrySuspendedRepository`는 `DefaultCountrySuspendedRepository`를 감싸는 데코레이터로, Redis `LettuceSuspendedCache`와
`newSuspendedTransaction` DB 접근을 조합해 캐시 히트 시 DB 트랜잭션을 열지 않는 최적화된 구조를 제공합니다.

- 관련 모듈: [`07-spring-suspended-cache`](07-spring-suspended-cache/)

## 다음 챕터

- [`../10-multi-tenant/README.ko.md`](../10-multi-tenant/README.ko.md): 테넌트 분리 아키텍처를 실전 형태로 확장합니다.
