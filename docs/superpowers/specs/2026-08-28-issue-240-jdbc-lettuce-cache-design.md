# JDBC Lettuce 캐시 전략 예제 설계

## 문서 상태

- 대상 이슈: [#240](https://github.com/bluetape4k/exposed-workshop/issues/240)
- 대상 저장소: `bluetape4k/exposed-workshop`
- 대상 모듈: `11-high-performance/08-cache-strategies-lettuce`
- 변경 유형: Type A Full Feature
- 승인 상태: 설계 방향·명세 리뷰 완료, 구현 계획 승인 대기
- 구현 범위: Exposed JDBC + H2 업무 데이터베이스와 Redis Lettuce 캐시
- 제외 범위: R2DBC 구현, 별도 서비스/API, 스키마 migration 도구, 운영 비밀값

이 문서는 이슈 #240의 승인된 설계를 구현 전에 고정한다. 한 명의 개발자가
설계·구현·검증·문서화를 순차적으로 수행하므로, 새 추상화나 병렬 실행 경로를
추가하지 않고 provider가 이미 제공하는 계약을 직접 보여주는 작은 모듈을 선택한다.

## 목표와 독자

독자는 기존 11장 Redisson 예제와 비교하면서 다음을 한 모듈에서 확인할 수 있어야
한다.

1. `AbstractJdbcLettuceRepository`와
   `AbstractSuspendedJdbcLettuceRepository`가 같은 Exposed `IdTable`/record를
   각각 동기·서스펜드 JDBC 경로로 연결하는 방법
2. 명시적인 `ExposedLettuceCodecs.jackson3(...)`를 사용한 값 codec과
   `LettuceCacheConfig.keyPrefix` 기반 키 namespace
3. `READ_WRITE_THROUGH`의 read-through, write-through, batch read/write,
   invalidation 동작
4. Redis GET/MGET 또는 cache warm 중 오류가 나도 데이터베이스를 원본으로
   유지하는 fallback 경계와, write-through 쓰기 실패를 성공으로 오인하지 않는
   경계
5. Redisson 예제의 TTL·near-cache·transaction boundary와 Lettuce provider의
   실제 동작 차이

README와 KDoc은 독자에게 노출되는 부분을 한국어로 작성하고, `README.md`와
`README.ko.md`는 같은 순서와 정보를 유지한다. R2DBC 예제는 이 저장소에 추가하지
않으며 `exposed-r2dbc-workshop`의 후속 이슈에서 별도로 다룬다.

## 현재 근거와 제약

구현 전에 확인한 현재 저장소와 provider의 사실은 다음과 같다.

| 근거 | 확인한 사실 | 설계에 미치는 영향 |
| --- | --- | --- |
| `settings.gradle.kts` | 장(chapter) 디렉터리의 즉시 하위 디렉터리를 동적으로 module로 등록한다. | 새 module의 별도 `include`는 추가하지 않는다. |
| `gradle/libs.versions.toml` | Exposed core/jdbc와 Lettuce·Redis·Testcontainers alias가 있고, JDBC Lettuce provider alias는 없다. | `exposed-jdbc-lettuce`를 versionless catalog alias로 추가한다. 버전은 중앙 BOM에 맡긴다. |
| 중앙 `bluetape4k-dependencies/gradle/libs.versions.toml` | `io.github.bluetape4k.exposed:bluetape4k-exposed-jdbc-lettuce` alias가 중앙 BOM 버전을 따른다. | 로컬 catalog에 provider 버전을 중복 선언하지 않는다. 최종 해석 버전은 Gradle dependency insight로 확인한다. |
| `bluetape4k-exposed/.../AbstractJdbcLettuceRepository.kt` | `table`, `ResultRow.toEntity`, `UpdateStatement.updateEntity`, `BatchInsertStatement.insertEntity`를 하위 타입이 구현하고, `extractId`는 `findAll` 사용 시 제공해야 한다. | 예제 record/table과 네 개의 매핑 hook을 한 곳에서 보여준다. |
| `.../AbstractSuspendedJdbcLettuceRepository.kt` | DB 접근은 `suspendedTransactionAsync(Dispatchers.IO)`를 사용하고, cache/near-cache 자원을 `close()`에서 해제한다. | 동기 저장소와 동일한 record/table을 사용하되 suspend API와 취소 경계를 별도 검증한다. |
| `.../ExposedLettuceCodecs.kt` | 기본 codec은 거부되며 `ExposedLettuceCodecs.jackson3(Class<E>)` 같은 명시 codec이 필요하다. | implicit codec이나 #166 Fory codec을 사용하지 않는다. |
| `.../ExposedLettuceLoadedMap.kt` | `get`/`getAll`의 Redis 오류는 loader DB fallback으로 전환하고, cache-only SET 오류는 경고 후 DB 값을 반환한다. `set`/`putAll`의 WRITE_THROUGH DB writer 오류는 전파된다. | 읽기·warm fallback과 쓰기 성공 계약을 서로 섞어 설명하지 않는다. |
| `.../LettuceCacheConfig.kt` | `READ_WRITE_THROUGH`, `READ_ONLY`, `WRITE_BEHIND` preset과 양수 TTL·배치·queue 및 비공백 namespace 검증이 있다. | 예제 기본 경로는 `READ_WRITE_THROUGH.copy(keyPrefix = ...)`로 고정한다. |
| 기존 `11-high-performance/01~07` | Redisson Spring/WebFlux/Ktor와 routing/benchmark 예제가 이미 존재한다. | Lettuce 예제는 서비스 프레임워크를 새로 끌어오지 않는 plain Kotlin/Exposed 테스트 중심 module로 둔다. |
| `.github/scripts/select-changed-examples.sh`, `.github/workflows/examples.yml` | 변경 예제 task 목록과 path filter가 일부 chapter 11 module만 명시한다. | 새 module task와 path filter를 함께 등록해 선택 실행이 누락되지 않게 한다. |

이슈 본문이 언급하는 provider 버전 문자열은 작업 시점 중앙 catalog보다 우선하지
않는다. 구현·검증 단계에서 실제 `runtimeClasspath` 해석 결과와 provider API를
읽어, 문서가 현재 빌드와 일치하는지 확인한다.

## 선택한 구조

### 독립적인 module 08

`11-high-performance/08-cache-strategies-lettuce`를 새 module로 추가한다. 기존
01/02 Redisson module을 확장하지 않는 이유는 다음과 같다.

- Spring MVC/WebFlux 설정과 Lettuce JDBC provider 생명주기를 섞으면 어느 계층이
  Redis client와 transaction을 소유하는지 흐려진다.
- 05/06 Ktor 예제는 라우팅과 request handler 학습이 중심이며, 이슈 #240의
  provider contract를 가장 작은 표면적으로 보여주지 못한다.
- 독립 module이면 새로운 dependency와 CI task를 한 명의 개발자가 추적하기 쉽고,
  동일한 `ProductTable`/`ProductRecord`에 동기·서스펜드 repository를 나란히
  배치할 수 있다.

### 업무 모델과 repository

모듈은 다음 최소 모델을 사용한다.

```text
ProductTable : LongIdTable
  name  : varchar
  price : decimal

ProductRecord : Serializable
  id    : Long
  name  : String
  price : BigDecimal
```

`ProductTable`과 `ProductRecord`는 동기·서스펜드 repository가 공유한다. 두
repository는 각자 provider 추상 클래스를 상속하지만 table, ID 직렬화, row 변환,
update/insert mapping은 의미적으로 동일해야 한다. `extractId`는
`ProductRecord.id`를 직접 반환한다.

두 repository의 기본 구성은 다음과 같다.

```kotlin
val config = LettuceCacheConfig.READ_WRITE_THROUGH.copy(
    keyPrefix = "workshop:products",
    ttl = Duration.ofMinutes(10),
    nearCacheEnabled = false,
)
val codec = ExposedLettuceCodecs.jackson3(ProductRecord::class.java)
```

`keyPrefix`는 provider가 `${keyPrefix}:${serializedId}`로 만드는 Redis key의
namespace다. 다른 repository가 같은 Redis를 사용해도 `workshop:products` 밖의
키를 삭제하지 않도록 `invalidateByPattern`, `clear`의 범위를 문서와 테스트로
고정한다. Codec은 저장 payload의 계약이므로 생략하거나 Fory/LZ4 기본값으로
대체하지 않는다. 이 모듈은 `nearCacheEnabled = false`인 remote-cache profile만
실행한다. 현재 `AbstractSuspendedJdbcLettuceRepository`는 내부
`LettuceSuspendNearCache`를 만들 때 repository의 Jackson3 codec을 전달하지 않고
provider 기본 codec을 사용할 수 있으며, pattern invalidation도 local front를
함께 지우지 않는다. 이를 workshop에서 우회하는 새 provider wrapper를 만들지
않고, near-cache는 Redisson과의 비교·제한 문서로만 다룬다.

### 호출자에게 고정하는 API 표면

구현 클래스 이름은 `ProductJdbcLettuceRepository`와
`ProductSuspendedJdbcLettuceRepository`로 고정한다. 두 클래스는 같은
`ProductTable`/`ProductRecord`와 `config`/`codec`을 받고 다음 공통 연산을 노출한다.

```kotlin
sync.get(id)                         // cache miss → JDBC loader → cache fill
sync.getAll(listOf(id1, id2))        // 결과는 ID→ProductRecord map
sync.put(id, product)                // DB writer → Redis SET
sync.putAll(productsById, batchSize = 2)
sync.findAll(where = { ProductTable.price greaterEq minPrice })
sync.invalidate(id)
sync.invalidateByPattern("product-*")

runSuspendIO {
    suspended.get(id)
    suspended.getAll(listOf(id1, id2))
    suspended.put(id, product)
}
```

`findAll`은 `JdbcCacheRepository`의 기본 인자(`limit`, `offset`, `sortBy`,
`sortOrder`, `where`)를 그대로 사용하고, 결과를 반환한 뒤 cache-only `warmAll`을
시도한다. `getAll`은 입력 순서나 중복을 보장하지 않고 ID를 key로 하는 map만
반환한다. `putAll`은 ID→record map과 양수 `batchSize`를 요구하며, 단일 bulk
upsert 또는 원자적 일괄 성공을 약속하지 않는다. `invalidateByPattern`의 인자는
전체 Redis glob이 아니라 provider가 `keyPrefix` 뒤에 붙이는 내부 suffix이며,
이 예제는 코드에 고정한 allow-list만 전달한다. 문서 예제는 sync와 suspend에서
같은 codec/namespace를 구성하고, `use`/`try-finally`로 repository를 먼저 닫는다.

### 데이터 흐름과 소유권

```text
caller
  ├─ sync ProductRepository.get/getAll/put/putAll
  │    └─ AbstractJdbcLettuceRepository
  │         ├─ Redis Lettuce map (keyPrefix + Jackson3 codec)
  │         └─ Exposed JDBC transaction → ProductTable (source of truth)
  └─ suspend ProductSuspendedRepository.get/getAll/put/putAll
       └─ AbstractSuspendedJdbcLettuceRepository
            ├─ suspended Lettuce map (remote-only profile)
            └─ suspendedTransactionAsync(IO) → ProductTable
```

repository는 자신이 만든 cache map을 소유하고 `close()`에서 map과 near-cache를
해제한다. 이번 실행 profile에서는 near-cache가 비활성화되어 원격 map만 생성된다.
`RedisClient`를 workshop 전역 singleton으로 감추거나 별도 manager에
위임하지 않는다. 호출자는 `RedisClient`와 repository의 종료 순서를 명시하고,
테스트는 `try/finally` 또는 `use`로 repository를 먼저 닫는다. `LettuceClients.clientOf`
는 client를 만들 뿐 자동 종료를 등록하지 않으므로 fixture가
`ShutdownQueue.register { client.shutdown() }`를 등록하거나 명시적으로
`client.shutdown()`을 호출한다. 공유 `LettuceClients.DEFAULT_CLIENT_RESOURCES`는
개별 테스트가 닫지 않고 JVM 수명 동안 한 책임자가 유지한다. Redis client 자체의
소유권은 테스트 fixture가 가지며, repository가 외부 client를 종료하지 않는다는
provider 계약을 보존한다. 종료 검증은 repository → client 순서, close-before-use,
반복 `close()`를 포함하며, provider의 `cache`가 lazy이고 `close()`에서 초기화될 수
있다는 현재 동작도 제한 사항으로 기록한다.

## 트랜잭션·일관성·장애 경계

### Read-through

1. `get(id)` 또는 `getAll(ids)`가 Redis hit이면 codec으로 복원한 값을 반환한다.
2. miss 또는 GET/MGET 연결 오류이면 provider loader가 같은 ID를 Exposed JDBC
   transaction에서 조회한다.
3. DB 값이 있으면 cache-only SET을 시도하지만, SET 오류는 경고로 관찰하고
   이미 읽은 DB 값을 호출자에게 반환한다.
4. DB에 행이 없으면 `null` 또는 결과 map에서 누락된 key를 반환한다.

따라서 읽기 fallback은 이미 provider cache map이 정상 생성된 뒤 Redis GET/MGET
명령이 실패할 때 “DB 읽기는 계속된다”는 계약이다. repository 생성 또는 lazy
cache 초기화의 `client.connect(...)` 자체가 실패한 경우에는 provider에 DB-only
전환 경로가 없으므로 fallback을 주장하지 않고 초기화 오류를 전파한다. 이는
Redis 복구 후 같은 client가 Lettuce 기본 auto-reconnect로 다시 명령을 수행하는
경로와 별개다. 이 계약은 write-through 쓰기 성공을 보장하지 않는다.

### Write-through와 batch

- `put`은 `WriteMode.WRITE_THROUGH`에서 DB writer를 먼저 호출한 뒤 Redis SET을
  수행한다. DB writer가 실패하면 예외를 호출자에게 전파하고 성공으로 기록하지
  않는다.
- `putAll(entities, batchSize)`는 provider writer와 Redis pipeline을 batchSize
  단위로 호출한다. Redis는 pipeline이지만 provider DB writer는 각 chunk에서
  기존 행을 조회한 뒤 행별 `UPDATE`/`INSERT` mapping을 적용할 수 있다. 따라서
  이 예제에서 “batch write”는 DB가 단일 bulk upsert를 수행한다는 뜻이 아니다.
  batchSize가 0 이하이면 provider의 즉시 검증 오류를 그대로 노출한다.
- 각 chunk의 DB writer transaction은 독립적으로 commit된다. 뒤의 chunk 또는
  해당 chunk의 Redis pipeline이 실패해도 앞선 chunk의 DB commit은 롤백되지
  않으므로 partial success와 DB/cache 일시 불일치를 허용한다. DB commit 뒤
  Redis SET/pipeline이 실패한 경우에도 DB를 되돌리지 않으며, 다음 read miss와
  명시적 invalidation/TTL 만료로 cache를 다시 채우는 복구 경계를 문서화한다.
- `findAll(where)`는 DB에서 조건 결과를 읽은 뒤 `warmAll`로 cache-only 적재한다.
  warm 실패는 결과 목록을 폐기하지 않고 경고 후 목록을 반환한다.
- 이 예제의 transaction boundary는 DB writer의 Exposed transaction이다. Redis
  SET과 DB commit을 분산 트랜잭션으로 묶거나 exactly-once를 주장하지 않는다.

### 초기화·재연결·retry

- Lettuce client는 `LettuceClients.clientOf(...)`의 현재 `ClientOptions` 기본
  auto-reconnect와 5초 connect timeout을 사용한다. 이 예제는 별도의 command
  retry loop를 추가하지 않는다.
- 읽기 GET/MGET transport 오류는 한 번 관찰한 뒤 즉시 DB loader fallback을
  수행한다. read retry 횟수·간격·deadline을 새로 정의하지 않는다.
- 공유 Redis endpoint에서 기존 transport를 `CLIENT KILL`로 끊은 뒤 DB fallback을
  확인하고, 같은 client가 readiness deadline 안에 다시 연결되어 cache SET/GET을
  수행하는 bounded smoke를 Redis 통합 테스트에 포함한다. provider map 초기화
  전 연결 실패와 active near-cache 오류는 이 경로의 대상이 아니다.
- `READ_WRITE_THROUGH`의 DB writer 설정에 포함된 provider retry가 있더라도
  이 모듈은 그 내부 횟수·간격을 재구현하거나 성공으로 재해석하지 않는다. Redis
  SET/pipeline 실패는 provider 예외를 전파하고 DB commit을 되돌리지 않는다.
- fallback/writer 오류 로그는 payload·key를 남기지 않고 `operation`,
  `entryCount`, `cacheType`, `errorType`만 구조화된 필드로 기록한다. metrics나
  health-check을 제공한다고 주장하지 않으며 로그는 보조 관찰 수단이다.

### Provider batch 비용과 검증 범위

`getAll(ids)`의 provider API에는 `batchSize` 인자가 없다. 구현은 한 번의 Redis
`MGET` 뒤에 miss마다 DB loader와 cache-only `SET`을 순차 호출한다. 따라서 이
예제의 batch read 계약은 “Redis 조회 round-trip은 한 번, miss 수에 비례한
fallback은 허용”으로 고정한다. 구현 계획과 테스트는 세부 miss 수에 따른
DB loader 호출 수와 Redis fill 명령 수를 관찰해 N+1 비용을 숨기지 않는다.

`putAll`/`warmAll`은 provider가 제공하는 chunk/pipeline 경계를 그대로 사용한다.
작은 고정 입력으로 query/command counter를 확인하는 bounded smoke 검증만
수행하고, 동시 호출 처리량·운영 latency·SLO를 이 workshop의 성능 근거로
추정하지 않는다. `high-performance` 장의 제목이 provider의 모든 경로가
bulk 또는 lock-free라는 주장을 의미하지 않도록 README에 이 제한을 반복한다.

### Invalidation과 namespace

`invalidate(id)`, `invalidateAll(ids)`는 Redis cache entry만 제거하며 DB 행은
유지한다. 이후 `get`은 DB에서 다시 읽는다. `invalidateByPattern`과 `clear`는
`keyPrefix` 하위만 대상으로 한다. 구현은 provider의 `SCAN` + `UNLINK`를 그대로
사용하므로 동시 쓰기 중 일부 key가 다음 scan으로 넘어가거나 누락될 수 있고,
`COUNT`는 삭제 상한이 아닌 scan hint다. 이를 원자적 namespace purge로 설명하지
않는다. 테스트는 두 namespace를 동시에 만들고 한 repository의
clear/invalidation이 다른 namespace를 건드리지 않는지 확인한다.

### Suspend와 취소

서스펜드 repository의 DB 구간은 `suspendedTransactionAsync(Dispatchers.IO)`를
사용한다. `CancellationException`은 일반 Redis/DB 오류로 삼키지 않고 호출자에게
다시 전파해야 한다. `close()`는 AutoCloseable 경계에서만 필요한 blocking
정리를 수행하며 일반 조회 경로에 `runBlocking`을 넣지 않는다.

## 비교 기준: Redisson과 Lettuce

README의 비교 표는 다음 사실만 다룬다.

| 항목 | 기존 Redisson 예제 | 이번 Lettuce 예제 |
| --- | --- | --- |
| provider 표면 | Redisson map/cache repository | `AbstractJdbcLettuceRepository`와 `AbstractSuspendedJdbcLettuceRepository` |
| read path | provider loader와 near-cache 설정 | Lettuce loaded map의 GET/MGET 및 DB loader fallback |
| write path | 기존 module의 write-through/write-behind 예제 | `READ_WRITE_THROUGH`의 DB writer 선행 + Redis SET/pipeline |
| TTL | Redisson config에 선언된 TTL | `LettuceCacheConfig.ttl`로 Redis entry SET 시 적용 |
| near-cache | 기존 Redisson coroutine 예제의 near-cache 전략 | 이번 모듈은 `nearCacheEnabled = false` remote-cache profile만 실행한다. suspend near-cache의 기본 codec·pattern invalidation 한계를 구현 범위 밖 제한으로 명시한다. |
| transaction boundary | 각 예제가 소유한 Exposed transaction | DB writer/loader의 JDBC 또는 `suspendedTransactionAsync(IO)`; Redis와 2PC 아님 |
| 장애 fallback | 기존 예제의 문서·구현을 별도 확인 | GET/MGET/warm read 경로의 DB fallback을 테스트로 고정, write failure는 전파 |

provider 구현이 near-cache preset의 모든 동작을 보장하지 않는 경우에는 실제
`cacheMode`/near-cache source를 읽고 README에 제한을 적는다. 단순히 preset 이름만
보고 local cache invalidation 또는 RESP3 tracking을 보장한다고 쓰지 않는다. 이
명세에서는 그 불일치를 해결하기 위해 near-cache를 기본 DoD에서 제외하고,
provider가 repository codec과 local invalidation을 함께 보장하는 후속 이슈가
생길 때만 별도 설계한다.

## 테스트 계약

기본 테스트는 H2와 공용 테스트 fixture를 사용해 DB 부분을 결정론적으로 검증한다.
Redis가 필요한 테스트 클래스에는 JUnit `@Tag("redis")`를 붙이고, 모듈의
`test` task는 기본적으로 해당 tag를 제외한다. 정확한 opt-in은
`-PincludeRedisIntegration=true`이며, `./gradlew :08-cache-strategies-lettuce:test
-PincludeRedisIntegration=true`가 Redis 통합 명령이다. Docker가 없으면 opt-in
실행은 skip하지 않고 Testcontainers 오류로 실패시켜 검증 공백을 숨기지 않는다.
각 테스트는 고정 문서 prefix와 분리된 `workshop:products:<unique-test-suffix>`를
사용하고, provider의 `clear()` 및 `SCAN MATCH <prefix>:*` 정리를 `finally`에서
수행한다. Redis fixture와 JUnit 병렬 사용량은 `maxParallelUsages = 1`로 직렬화한다.
Redis가 필요한 테스트는 `RedisServer.Launcher.redis`와
`LettuceClients.clientOf(...)`를 사용하며 fixture가 `ShutdownQueue`에 client
종료를 등록한다.

| 테스트 묶음 | 검증 내용 | 환경 |
| --- | --- | --- |
| codec/namespace | `jackson3` round-trip, 같은 ID의 namespace 분리, implicit codec 거부 | H2 + Redis |
| sync read-through | 첫 `get` miss→DB→cache, 두 번째 hit, 존재하지 않는 ID의 `null` | H2 + Redis |
| suspend read-through | 동일한 hit/miss와 `getAll` 결과를 suspend API에서 확인 | H2 + Redis |
| batch read/write | sync/suspend `getAll`은 MGET 후 miss별 loader/SET fallback을, `putAll`은 chunk 단위 DB writer와 Redis pipeline을 사용하고 query/command 비용을 숨기지 않음 | H2 + Redis |
| write-through | `put`/`putAll` 직후 cache 우회 DB 조회가 새 값을 반환 | H2 + Redis |
| invalidation | 단건·복수·pattern·clear 이후 DB 재조회와 namespace 보존 | H2 + Redis |
| findAll warm | 조건 조회 결과를 반환하고 이후 ID 조회가 warm cache를 사용 | H2 + Redis |
| Redis GET/MGET fallback | Redis 명령 실패를 통제한 client/fixture에서 DB 값 또는 누락 결과를 반환 | H2 + 장애 Redis 경계 |
| cache-only SET/warm failure | SET 실패가 DB에서 읽은 결과를 오류로 바꾸지 않는지 확인 | H2 + 장애 Redis 경계 |
| write failure boundary | DB writer/Redis write 실패가 성공으로 위장되지 않고 예외/상태를 보존하는지 확인 | H2 + Redis |
| close lifecycle | remote-cache profile의 sync/suspend repository가 close-before-use와 반복 `close()`에서도 예외 없이 종료되고, repository → client 순서와 fixture shutdown 책임을 지킴 | Redis |
| cancellation | suspend read/write 중 취소가 `CancellationException`으로 전파되고 임의 fallback으로 변환되지 않음 | Redis + coroutine test |
| reconnect recovery | 기존 transport를 `CLIENT KILL`로 끊은 동안 DB fallback, 동일 endpoint에서 같은 client의 재연결·재조회·cache fill | Redis + Testcontainers |
| partial batch failure | 뒤의 DB chunk 또는 Redis pipeline 실패가 앞선 commit을 되돌리지 않고 partial state와 예외를 보존 | H2 + Redis 장애 경계 |
| near-cache safety | provider 기본 codec 및 local pattern invalidation 한계 때문에 near-cache 실행을 DoD에서 제외했음을 문서·config로 확인 | N/A: remote-only profile |

테스트는 Redis key 문자열과 로그 전체를 성공 기준으로 삼지 않는다. 관찰 가능한
DB 행, 반환 map, cache 재조회 결과, 예외 타입, namespace별 key 존재 여부를
단언한다. 기본 `test`/`build`/CI는 `@Tag("redis")`를 제외한 H2-only 경로이고,
opt-in 명령에서 Docker가 없으면 실패한다. 외부 Docker가 없는 환경에서 기본
경로만 실행한 경우에는 Redis 동작을 검증하지 못한 사실을 최종 보고의 검증
공백으로 남긴다. 로그 assertion은 `operation`, `entryCount`, `cacheType`,
`errorType`와 redaction만 보조 확인한다.

## Gradle·CI 통합

1. `gradle/libs.versions.toml`에 다음 versionless alias를 추가한다.

   ```toml
   exposed-jdbc-lettuce = { module = "io.github.bluetape4k.exposed:bluetape4k-exposed-jdbc-lettuce" }
   ```

2. 새 module은 `org.jetbrains.exposed.v1.*` imports, Kotlin 2.3/Java 21,
   `implementation(libs.exposed.jdbc.lettuce)`, Exposed core/jdbc, Redis/Lettuce
   client, coroutine, shared test와 H2/Testcontainers 의존성을 repository convention에
   맞춰 선언한다. 새 외부 dependency나 버전 pin은 추가하지 않는다.
3. 새 module의 `test` task에 다음 opt-in 규칙을 고정한다. 기본 실행과 CI는
   `@Tag("redis")`를 제외하고, `-PincludeRedisIntegration=true`일 때만 Redis
   tag를 포함한다. 기본 검증은 `./gradlew :08-cache-strategies-lettuce:test`,
   Redis 검증은 앞 절의 opt-in 명령으로 구분한다.
4. `.github/scripts/select-changed-examples.sh`의 고정 task 목록과 chapter 11
   path mapping에 `:08-cache-strategies-lettuce:build`를 추가한다. 현재 Examples
   workflow의 선택 task 계약이 `build`이므로 selector는 기본 H2 경로만 실행하며,
   Redis opt-in은 별도 명령으로만 실행한다.
5. `.github/workflows/examples.yml`의 push/pull_request path filter에
   `11-high-performance/08-cache-strategies-lettuce/**`를 추가하고 selector가
   해당 module task를 반환하는지 검사한다.
6. repository의 `ci.yml` 전체 `./gradlew test`와 Examples `build`는 tag 제외
   기본 경로를 유지하고, nightly H2 전체 테스트도 같은 기본 계약을 사용한다.
   CI/Kover artifact에서 이 module의 H2 결과가 누락되지 않는지 확인하며, Redis
   opt-in 명령은 수동/전용 환경에서만 실행한다. 이를 문서에 명시해 기본 CI가
   Redis 의존성을 조용히 생략하거나 예기치 않게 실행하지 않도록 한다.
7. `11-high-performance/README.md`와 `README.ko.md`에 module 표, 실행 명령,
   학습 순서와 비교 설명을 source-equivalent로 추가한다. 기존 diagram asset을
   참조하는 chapter 문서와 새 module 문서의 경로가 모두 실제 파일을 가리켜야 한다.

## 문서와 시각 자료 계약

새 module에는 다음 두 문서를 같은 목차와 정보를 가진 source-equivalent pair로
추가한다.

- `11-high-performance/08-cache-strategies-lettuce/README.md`
- `11-high-performance/08-cache-strategies-lettuce/README.ko.md`

README는 목표, dependency alias, `ProductTable`/`ProductRecord`, sync/suspend
repository 사용법, codec/namespace, hit/miss와 batch, invalidation, Redis failure
fallback, Redisson 비교, R2DBC 제외 범위, 테스트 명령과 통합 제한을 설명한다. raw
Mermaid block은 남기지 않는다.

다음 편집 가능한 SVG와 CairoSVG 렌더링 PNG를
`docs/images/readme-diagrams/`에 둔다.

- `11-high-performance-lettuce-architecture-01.svg/.png`
- `11-high-performance-lettuce-architecture-01.ko.svg/.png`
- `11-high-performance-lettuce-sequence-01.svg/.png`
- `11-high-performance-lettuce-sequence-01.ko.svg/.png`

architecture diagram은 caller, sync/suspend repository, Redis loaded map, explicit
codec, Exposed JDBC/H2 source of truth와 namespace를 보여준다. sequence diagram은
cache miss→DB loader→cache SET, cache hit, Redis failure→DB fallback,
write-through의 DB/Redis 순서를 보여준다. 모든 독자-facing 문구는 영문·한국어
asset이 의미상 일치해야 하고, PNG를 full-size로 눈으로 확인한 뒤 diagram endpoint/
sequence-style/pair audit를 수행한다.

## 실패 모드와 완화

| 실패 모드 | 관찰 가능한 계약 | 완화/테스트 |
| --- | --- | --- |
| Redis GET/MGET 연결·명령 실패 | loader가 DB에서 읽은 값 또는 빈 결과를 반환 | 통제된 장애 client/fixture에서 DB 값과 예외 비전파를 단언 |
| provider cache 초기화/`client.connect` 실패 | DB-only fallback을 주장하지 않고 초기화 예외를 전파 | close-before-use와 생성 실패 경계를 별도 lifecycle 테스트·README에 기록 |
| Redis transport 단절 후 복구 | `CLIENT KILL` 중 DB fallback, 동일 endpoint에서 동일 client가 auto-reconnect 후 cache 경로를 회복 | shared-endpoint bounded smoke; custom retry loop 없음 |
| DB loader 실패 | Redis miss가 임의의 빈 성공으로 바뀌지 않음 | H2 transaction 오류를 주입해 원래 예외를 관찰 |
| cache-only SET 또는 warm pipeline 실패 | 이미 확보한 DB read 결과는 호출자에게 유지 | `findAll`/fallback 테스트와 경고 로그 확인 |
| write-through DB writer/Redis SET 실패 | write API가 성공한 것처럼 반환하지 않음 | 예외 타입과 DB/cache 상태를 분리 단언 |
| 후속 batch chunk/Redis pipeline 실패 | 앞선 chunk commit은 유지되고 partial state·stale cache 가능성을 숨기지 않음 | 후속 chunk 실패를 주입하고 commit/예외/재조회 결과를 단언 |
| namespace 충돌·과도한 clear | 다른 prefix의 key가 삭제되지 않음 | 두 prefix를 만들고 pattern/clear 후 교차 조회 |
| 외부 입력이 섞인 prefix/pattern | SCAN glob 범위가 의도보다 넓어질 수 있음 | prefix는 코드에 고정한 literal이고 pattern 인자는 내부 allow-list만 사용한다. 사용자 입력을 직접 전달하는 API는 예제에서 제공하지 않는다. |
| codec 변경 또는 implicit codec | 저장 payload를 복원하지 못하거나 provider가 즉시 거부 | explicit Jackson3 생성 및 round-trip/초기화 실패 테스트 |
| Redis payload 변조 또는 운영 endpoint 오용 | cache hit이 DB 검증 없이 반환될 수 있음 | 테스트 Redis와 고유 namespace만 사용한다. 운영에서는 ACL/TLS/secret manager를 적용하고 cache를 authorization/business truth로 사용하지 않는다. |
| repository/client 종료 순서 오류 | connection/scheduler/near-cache가 남거나 double close 오류 | fixture 소유권, `ShutdownQueue`/명시 shutdown, close-before-use 및 반복 close 테스트 |
| coroutine cancellation | 취소가 일반 예외로 삼켜지거나 DB fallback으로 변환되지 않음 | `CancellationException` 재전파 테스트 |
| SCAN 기반 namespace 정리 | 동시 쓰기 중 누락 가능성과 비원자성을 원자 purge로 오인 | `COUNT`를 hint로 문서화하고 고정 prefix만 `SCAN MATCH`/`UNLINK` |
| Testcontainers/Docker 불가 | Redis opt-in 실행이 skip되어 통합 검증을 완료했다고 오인 | 기본 H2 경로와 Redis 실패를 분리 보고; opt-in은 실패 상태로 남김 |

## 범위 밖과 후속 작업

- R2DBC Lettuce repository와 coroutine context 전파는
  `exposed-r2dbc-workshop`에서 별도 이슈로 구현한다.
- Lettuce suspend near-cache의 explicit codec 전달·namespace 생성·pattern
  invalidation 보완은 현재 provider 계약을 우회하지 않고 별도 후속 이슈로
  다룬다. 이번 모듈은 remote-cache profile만 검증한다.
- Spring Boot/Ktor endpoint, cache manager, 새로운 schema/migration, custom
  column type은 추가하지 않는다.
- #166의 Fory codec을 반복하지 않고, 운영용 Redis ACL/TLS/secret 관리도 다루지
  않는다.
- write-behind의 durable queue/재처리 보장은 provider의 별도 예제 또는 후속
  이슈로 남긴다. 이번 DoD는 `READ_WRITE_THROUGH`에 집중한다.
- provider가 near-cache 장애 fallback·local invalidation·cache 초기화 실패의
  DB-only 전환을 제공하지 않는 한, remote-only profile 밖의 graceful degradation은
  후속 범위로 남긴다.

## 수용 기준과 DoD

- [ ] 새 module이 동적으로 등록되고 `:08-cache-strategies-lettuce:test`가
      선택 실행된다.
- [ ] 중앙 BOM이 관리하는 `exposed-jdbc-lettuce` alias가 실제 provider API와
      함께 해석된다.
- [ ] 같은 `ProductTable`/`ProductRecord`에 sync/suspend repository가 연결되고
      동일한 key namespace·Jackson3 codec을 사용한다.
- [ ] hit/miss, MGET+miss별 fallback, chunked write/pipeline, write-through,
      invalidation, namespace 격리가 H2+Redis 테스트로 고정된다.
- [ ] Redis read/MGET 및 cache warm 장애 fallback과 write failure 경계가
      코드·테스트·README에 명시된다.
- [ ] Redis integration이 opt-in 경계로 실행되고 Docker 불가 시 검증 공백이
      아니라 실패로 드러나며, 기본 H2 경로와 분리 보고된다.
- [ ] `-PincludeRedisIntegration=true`와 `@Tag("redis")` 규칙, 기본
      `test`/Examples `build`/CI/nightly 범위가 README·Gradle·workflow 근거와
      일치한다.
- [ ] Redis transport 단절 중 DB fallback과 동일 endpoint에서 같은 client의
      auto-reconnect 경로가 bounded smoke로 검증된다. provider 초기화 실패는
      DB-only fallback으로 포장하지 않는다.
- [ ] batch chunk 독립 commit, partial success, stale cache 및 후속 실패가
      테스트·README에 명시된다.
- [ ] repository → client shutdown, `ShutdownQueue`/공유 resources 소유권,
      고유 test namespace·직렬 실행·안전한 cleanup이 lifecycle 검증에 포함된다.
- [ ] remote-only profile을 통해 suspend near-cache의 기본 codec·local
      invalidation 한계를 과장하지 않는다.
- [ ] batch read miss 수와 batch write chunk 비용을 관찰하는 bounded
      query/command 검증이 있고 운영 SLO를 추정하지 않는다.
- [ ] Redisson TTL/near-cache/transaction 비교가 source/provider 근거와 함께
      EN/KO README에 기록된다.
- [ ] architecture/sequence SVG·PNG 및 EN/KO source-equivalent pair가 생성되고
      diagram audit와 full-size visual inspection을 통과한다.
- [ ] module 및 chapter README, CI selector/path filter, catalog alias가 서로
      일치한다.
- [ ] `git diff --check`, targeted test, compile/static analysis, detekt와
      변경 경로 검증이 모두 fresh evidence로 남는다.
- [ ] 한 명의 개발자가 한 branch에서 순차적으로 구현하며, R2DBC나 별도 service를
      추가하지 않는다.

## 롤백과 재실행

구현 중 provider API 또는 Redis fixture가 현재 catalog와 맞지 않으면 새 공통
추상화를 만들지 않고 해당 module 변경을 되돌린다. catalog alias·CI path·README
변경은 module 구현과 같은 branch에서 함께 제거할 수 있어야 한다. Redis 통합이
필요한 재실행에서는 `SCAN MATCH <literal-prefix>:*` 후 `UNLINK`를 사용하거나
TTL 만료를 기다리며, 공유 DB의 `FLUSHDB`/`FLUSHALL`은 금지한다. schema migration은
없으며 dependency·selector·README 변경도 같은 rollback 목록에 포함한다. 기본 H2
검증만 실행한 경우 완료를 선언하지 않고 상태를 `PENDING`으로 남긴 뒤 Docker를
사용할 수 있는 환경에서 `-PincludeRedisIntegration=true` targeted command를
재실행한다.
