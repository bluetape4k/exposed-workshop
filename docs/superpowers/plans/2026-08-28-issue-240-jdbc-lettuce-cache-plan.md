# 이슈 #240 JDBC Lettuce 캐시 예제 구현 계획

## 계획 상태

- 대상 이슈: [#240](https://github.com/bluetape4k/exposed-workshop/issues/240)
- 구현 branch: `feat/issue-240-jdbc-lettuce-cache`
- 설계 명세: `docs/superpowers/specs/2026-08-28-issue-240-jdbc-lettuce-cache-design.md`
- 명세 리뷰: `docs/superpowers/reviews/2026-08-28-issue-240-jdbc-lettuce-cache-spec-review.md`
- 설계·명세 승인: 완료
- 계획 승인: 사용자 승인 완료, 구현 진행 중
- 작업 방식: 한 명의 개발자가 하나의 branch에서 컴포넌트를 의존성 순서대로
  직렬 구현한다. 독립 검토는 별도 reviewer lane으로 수행하되 코드 소유권은
  lead lane 하나로 유지한다.

## 목표와 고정 경계

`11-high-performance`에 기존 `bluetape4k-exposed-jdbc-lettuce` provider를
직접 사용하는 작은 JDBC 예제를 추가한다. 동일한 `ProductTable`/`ProductRecord`,
명시적인 `ExposedLettuceCodecs.jackson3(...)`, `READ_WRITE_THROUGH`, 고정
namespace를 sync/suspend repository에 함께 적용하고, H2 기본 경로와 Redis
opt-in 경로를 분리한다.

다음은 구현하지 않는다.

- R2DBC repository 또는 `exposed-r2dbc-workshop` 외 저장소의 코드
- near-cache wrapper, provider 수정, 새로운 cache manager 또는 service endpoint
- custom column/schema migration, Fory codec 재구현, 운영 Redis ACL/TLS/secret
- Redis와 JDBC를 묶는 2PC/exactly-once 또는 처리량·SLO 주장

## 순차 실행 단계

### 1. 실행 전 현재 계약 재확인

1. 중앙 BOM과 현재 `gradle/libs.versions.toml`의
   `exposed-jdbc-lettuce` alias, provider 버전, `AbstractJdbcLettuceRepository`,
   `AbstractSuspendedJdbcLettuceRepository`, `ExposedLettuceCodecs.jackson3`의
   실제 시그니처를 다시 읽는다.
2. 기존 chapter 11 모듈의 Gradle convention, `AbstractExposedTest`,
   `TestDB.H2`, `RedisServer.Launcher.redis`, `ShutdownQueue`,
   `maxParallelUsages = 1` 사용법을 확인한다.
3. `settings.gradle.kts`, Examples selector, `examples.yml`, chapter README의
   동적 등록·path filter·실행 명령을 현재 HEAD 기준으로 확인한다.
4. 기준 검증으로 `./gradlew projects --no-daemon --no-configuration-cache`와
   기존 `:12-javers-exposed-audit:test`를 실행하고 결과를 기록한다.

### 2. 최소 Gradle/module 골격

다음 파일을 새로 추가하거나 수정한다.

- `11-high-performance/08-cache-strategies-lettuce/build.gradle.kts`
- `gradle/libs.versions.toml`
- `11-high-performance/08-cache-strategies-lettuce/src/test/resources/junit-platform.properties`
- `11-high-performance/08-cache-strategies-lettuce/src/test/resources/logback-test.xml`

`gradle/libs.versions.toml`에는 중앙 BOM 버전을 재정의하지 않는
`exposed-jdbc-lettuce` versionless alias만 추가한다. module은 repository의
기존 Kotlin/Exposed/JUnit/Testcontainers convention을 재사용하고, production
provider는 `implementation`, 공용 테스트와 H2/Testcontainers는 기존 규칙에
맞는 test configuration으로 선언한다. 새 dependency와 local version pin은
추가하지 않는다.

module test task에는 다음 계약을 고정한다.

- `@Tag("redis")` 테스트는 기본 `test`/`build`에서 제외한다.
- `-PincludeRedisIntegration=true`일 때만 Redis tag를 포함한다.
- opt-in에서 Docker/Testcontainers가 준비되지 않으면 skip하지 않고 실패한다.
- 기본 H2 경로는 module compile/test와 Examples `build`에서 계속 실행된다.

새 `junit-platform.properties`는 공유 Redis transport 장애를 주입하는 모든
클래스를 `same_thread`로 직렬 실행하고, 테스트별 `@Timeout`과 별도의 Redis
readiness deadline을 함께 사용한다. client는 테스트 전용 `RedisURI`에 짧은
connect/command timeout을 명시한다. 골격 직후
`./gradlew :08-cache-strategies-lettuce:test --no-daemon
--no-configuration-cache`를 실행해 등록·configuration 오류를 먼저 고정한다.

### 3. TDD RED: 관찰 가능한 테스트 계약을 먼저 작성

production repository를 작성하기 전에 다음 테스트와 공용 fixture를 만든다.
테스트가 기대 API 또는 아직 없는 repository를 참조해 처음에는 실패하는지
확인하고, 실패 로그를 기록한 뒤 구현 단계로 넘어간다. 테스트 파일의 실제
package는 기존 chapter convention을 따른다.

- `11-high-performance/08-cache-strategies-lettuce/src/test/kotlin/exposed/examples/cache/lettuce/ProductLettuceCacheH2Test.kt`
  - Redis connection을 건드리지 않는 H2-only DB mapping, seed/update, count 검증;
    `cache`, `get`, `put`을 호출하지 않아 기본 실행이 Redis에 의존하지 않음을
    고정한다.
  - `LongIdTable` auto-increment 신규 insert를 기대하지 않고, seed된 기존 ID의
    DB update mapping은 Redis 통합 테스트의 write-through oracle과 분리한다.
  - explicit Jackson3 codec 생성, 잘못된 `batchSize`의 즉시 검증

- `11-high-performance/08-cache-strategies-lettuce/src/test/kotlin/exposed/examples/cache/lettuce/ProductJdbcLettuceRepositoryIntegrationTest.kt`
  - `@Tag("redis")` sync read-through/write-through, `get`, `getAll`, `put`,
    `putAll`, `findAll`, `invalidate`, `invalidateAll`, `invalidateByPattern`,
    `clear`의 전체 계약
  - hit/miss, MGET 1회·miss별 DB load/SET, chunk별 writer/pipeline 비용
  - 고정 prefix와 별도 namespace 격리, pattern allow-list 외 입력 거부

- `11-high-performance/08-cache-strategies-lettuce/src/test/kotlin/exposed/examples/cache/lettuce/ProductSuspendedJdbcLettuceRepositoryIntegrationTest.kt`
  - `@Tag("redis")` suspend read/write 및 sync와 같은 table/record/codec/
    namespace를 사용하는 `get`, `getAll`, `put`, `putAll`, `findAll`,
    `invalidate`, `invalidateAll`, `invalidateByPattern`, `clear` parity
  - `Job.cancelAndJoin()`으로 Redis await와 DB loader 중 취소를 주입하고
    `CancellationException` 재전파, JDBC transaction cleanup, close를 단언

sync/suspend parity oracle은 다음 표로 고정한다.

| 연산 | sync oracle | suspend oracle |
| --- | --- | --- |
| `get`/`getAll` | hit, miss, null 누락, MGET·miss fill 비용 | 동일 값·누락 map·동일 namespace와 비용 경계 |
| `put`/`putAll` | 기존 seed row UPDATE, Redis SET/pipeline, batch 오류 | 동일 DB/cache 상태·예외·chunk 경계 |
| `findAll` | 조건·정렬 결과와 cache-only warm | 동일 결과·warm 및 `CancellationException` 재전파 |
| `invalidate`/`invalidateAll` | DB 유지, 해당 key만 제거 | 동일 DB 유지·key 제거 |
| `invalidateByPattern`/`clear` | `product-*` allow-list와 namespace 격리 | 동일 allow-list·namespace 격리 |

- `11-high-performance/08-cache-strategies-lettuce/src/test/kotlin/exposed/examples/cache/lettuce/RedisLettuceCacheFixture.kt`
  - `RedisServer.Launcher.redis`, 테스트 전용 `RedisURI`/짧은 timeout,
    `LettuceClients.clientOf`, readiness polling과 deadline
  - `workshop:products:<unique-test-suffix>` namespace와 별도 namespace 격리
  - 테스트별 `@Timeout`와 `finally`의 provider cleanup: literal prefix `SCAN MATCH` + `UNLINK`;
    `FLUSHDB`/`FLUSHALL` 금지
  - fixture가 `ShutdownQueue`에 client를 한 번만 등록하는 단일 소유자 규칙;
    테스트에서 명시적 `client.shutdown()`을 중복 호출하지 않음

- `11-high-performance/08-cache-strategies-lettuce/src/test/kotlin/exposed/examples/cache/lettuce/RedisLettuceCacheFailureIntegrationTest.kt`
  - `@Tag("redis")` Redis GET/MGET/cache-only SET/warm 오류 시 DB fallback 또는
    결과 보존, write 오류 전파
  - 공유 Redis endpoint에서 기존 transport를 `CLIENT KILL`로 끊은 뒤
    fallback→readiness polling→같은 client의 auto-reconnect와 cache fill을
    deadline 안에 확인
  - 두 번째 `putAll` chunk의 writer 또는 pipeline만 결정적으로 실패시키고
    첫 번째 DB commit, partial Redis state, 원래 예외를 정확히 단언
  - 별도 `StringCodec` connection으로 malformed JSON을 같은 key에 기록해
    decode 실패가 DB fallback으로 처리되는지 확인한다. 유효하지만 변조된
    payload는 DB 재검증 없이 반환될 수 있음을 보안 경고 테스트/README에
    기록하고 authorization/business truth에 사용하지 않는다고 명시한다.
  - repository close-before-use와 반복 close가 예외 없이 끝나고, repository
    close 후 JVM 종료 시점의 `ShutdownQueue` client 소유권만 남는지 확인

Redis 오류는 (1) 공유 endpoint를 유지하는 `CLIENT KILL`로 transport 오류, (2)
별도 raw `StringCodec` connection으로 malformed payload, (3) provider writer가
사용하는 통제된 Redis connection/pipeline 실패 fixture로 주입한다. provider source를
우회하는 새 production abstraction은 만들지 않는다. query/command counter는
작은 고정 입력에서 `getAll`의 한 번 MGET·miss별 fallback과 `putAll` chunk/pipeline
경계를 관찰하는 데만 사용하고 성능 수치로 확장하지 않는다.

### 4. TDD GREEN: provider subclass와 공용 모델 구현

다음 production 파일을 추가한다(실제 package와 파일 분리는 기존 module
layout 확인 후 결정하되 책임은 유지한다).

- `11-high-performance/08-cache-strategies-lettuce/src/main/kotlin/exposed/examples/cache/lettuce/ProductLettuceCacheModels.kt`
  - `ProductTable`은 `LongIdTable("lettuce_products")` 기반으로 정의하고
    `ProductRecord`는 한국어 KDoc이 있는 `Serializable` data class로 둔다.
    `ProductRecord`의 companion에는 private `const val serialVersionUID: Long = 1L`을
    둔다. public KDoc은 README의 import/API 예제와 같은 이름을 사용한다.
- `11-high-performance/08-cache-strategies-lettuce/src/main/kotlin/exposed/examples/cache/lettuce/ProductJdbcLettuceRepository.kt`
  - public constructor를 다음으로 고정한다:
    `ProductJdbcLettuceRepository(client: RedisClient, config: LettuceCacheConfig = ProductLettuceCacheConfig.default(), valueCodec: RedisCodec<String, ProductRecord> = ExposedLettuceCodecs.jackson3(ProductRecord::class.java))`.
  - `AbstractJdbcLettuceRepository<Long, ProductRecord>`를 직접 상속하고
    `table`, `ResultRow.toEntity`, `UpdateStatement.updateEntity`,
    `BatchInsertStatement.insertEntity`, `extractId` mapping을 구현한다.
- `11-high-performance/08-cache-strategies-lettuce/src/main/kotlin/exposed/examples/cache/lettuce/ProductSuspendedJdbcLettuceRepository.kt`
  - public constructor를 sync와 동일한 `client`, `config`, `valueCodec` 순서와
    기본값으로 고정한다.
  - `AbstractSuspendedJdbcLettuceRepository<Long, ProductRecord>`를 직접 상속하고
    sync와 같은 mapping 및 `suspendedTransactionAsync(Dispatchers.IO)` 경계를
    사용한다.
- `11-high-performance/08-cache-strategies-lettuce/src/main/kotlin/exposed/examples/cache/lettuce/ProductLettuceCacheConfig.kt`
  - `ProductLettuceCacheConfig.default()`는
    `LettuceCacheConfig.READ_WRITE_THROUGH.copy(nearCacheEnabled = false,
    keyPrefix = "workshop:products")`를 반환한다.
  - 테스트 suffix는 fixture 내부에서만 `workshop:products:<unique-test-suffix>`로
    만들며 production README/API가 임의 prefix를 받지 않도록 한다.
  - repository는 keyPrefix가 `workshop:products` namespace 또는 fixture suffix
    아래인지 검증한다. `serializeKey`는 `product-<id>`로 고정하고
    `invalidateByPattern`은 allow-list `product-*`만 허용하며 외부 wildcard를
    직접 전달하는 호출은 `IllegalArgumentException`으로 거부한다.

모든 public class와 model에는 한국어 KDoc과 실제 README import/example과
일치하는 API를 제공한다. GREEN 단계에서는 테스트가 요구한 최소 동작만 구현하고
near-cache, retry loop, manager, endpoint를 추가하지 않는다. `CancellationException`은
일반 fallback으로 변환하지 않고 provider coroutine 경계를 보존한다.

### 5. 테스트 보강·cleanup·문서화

1. RED에서 기록한 실패를 GREEN 구현으로 다시 실행하고, 테스트 이름과 assertion이
   명세의 관찰값(DB row, 반환 map, 예외, namespace key)에만 의존하는지 정리한다.
2. `use`/`try-finally`와 `ShutdownQueue`를 이용해 client·repository lifecycle을
   명확히 하고, 테스트별 prefix cleanup이 다른 테스트의 key를 삭제하지 않는지
   확인한다.
3. module README를 추가한다.
   - `11-high-performance/08-cache-strategies-lettuce/README.md`
   - `11-high-performance/08-cache-strategies-lettuce/README.ko.md`
   - 목표, 학습 순서, sync/suspend 동일 계약, H2 기본 명령,
     `-PincludeRedisIntegration=true` opt-in 명령
   - hit/miss, MGET miss 비용, chunk/pipeline partial success, fallback/write
     failure, close/reconnect, near-cache remote-only 제한
   - Redis cache는 신뢰된 인프라의 read optimization일 뿐이며 authorization이나
     business truth가 아니다. malformed/변조 payload, ACL/TLS/secret manager
     운영 경계를 명시한다.
   - 기존 Redisson의 TTL/near-cache/transaction boundary와 source/provider
     근거가 있는 비교표
   - R2DBC는 `exposed-r2dbc-workshop`에서 별도 구현한다는 경계
4. chapter index를 갱신한다.
   - `11-high-performance/README.md`
   - `11-high-performance/README.ko.md`
5. diagram skill 규칙에 따라 다음 source-equivalent pair를 생성한다.
   - `docs/images/readme-diagrams/11-high-performance-08-lettuce-cache-architecture.en.svg`
   - `docs/images/readme-diagrams/11-high-performance-08-lettuce-cache-architecture.en.png`
   - `docs/images/readme-diagrams/11-high-performance-08-lettuce-cache-architecture.ko.svg`
   - `docs/images/readme-diagrams/11-high-performance-08-lettuce-cache-architecture.ko.png`
   - `docs/images/readme-diagrams/11-high-performance-08-lettuce-cache-sequence.en.svg`
   - `docs/images/readme-diagrams/11-high-performance-08-lettuce-cache-sequence.en.png`
   - `docs/images/readme-diagrams/11-high-performance-08-lettuce-cache-sequence.ko.svg`
   - `docs/images/readme-diagrams/11-high-performance-08-lettuce-cache-sequence.ko.png`
   - 독자-facing label은 EN/KO source-equivalent로 각각 제공하고, CairoSVG
     render·pair audit·full-size visual inspection을 수행한다. README에는 raw
     Mermaid를 남기지 않는다.

### 6. CI/selector 등록

- `.github/scripts/select-changed-examples.sh`의 `ALL_TASKS`와 chapter 11 path
  mapping에 `:08-cache-strategies-lettuce:build`를 추가한다.
- `.github/workflows/examples.yml`의 push/pull_request path filter에
  `11-high-performance/08-cache-strategies-lettuce/**`를 추가한다.
- 기존 `ci.yml` 전체 test/nightly H2 기본 경로를 Redis opt-in과 섞지 않는다.
- Redis 통합 회귀는 전용/manual 또는 Redis-enabled nightly job에서
  `-PincludeRedisIntegration=true`로 실행한다. Redis opt-in fresh pass가 없으면
  merge-ready로 전환하지 않는다.
- selector를 직접 실행해 새 module task가 선택되고, unrelated path에는 선택되지
  않는지 확인한다.

### 7. 검증·증적·통합 준비

변경 순서에 따라 다음 검증을 순차 실행하고 출력·경로·commit SHA를 기록한다.

1. `git diff --check`
2. `./gradlew projects --no-daemon --no-configuration-cache`
3. `./gradlew :08-cache-strategies-lettuce:test --no-daemon --no-configuration-cache`
4. `./gradlew :08-cache-strategies-lettuce:build --no-daemon --no-configuration-cache`
5. `./gradlew :08-cache-strategies-lettuce:compileKotlin
   :08-cache-strategies-lettuce:compileTestKotlin --no-daemon
   --no-configuration-cache`
6. `./gradlew :08-cache-strategies-lettuce:detekt --no-daemon
   --no-configuration-cache`
7. Redis runtime 가능 여부를 `colima status`, `docker context show`, `docker info`
   로 확인한다. 관리된 `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`를 우선 사용한다.
8. Docker가 준비되면
   `./gradlew :08-cache-strategies-lettuce:test -PincludeRedisIntegration=true
   --no-daemon --no-configuration-cache`를 실행한다. Docker가 없으면 opt-in
   결과를 실패/미검증으로 기록하고 merge-ready를 주장하지 않는다.
9. README EN/KO 목차·명령·제약 비교, diagram pair/audit, selector/workflow
   path를 다시 읽는다.
10. `git status`, `git diff --stat`, `git diff --check`와 module 전체 diff를
   확인하고 workflow component/check evidence에 연결한다.

## 위험·롤백

- provider API 또는 catalog 해석이 현재 HEAD와 다르면 API를 복제하거나 새
  abstraction을 만들지 않고 해당 module 변경을 되돌린 뒤 명세를 갱신한다.
- Redis integration 실패는 H2 기본 성공으로 덮지 않는다. Docker를 사용할 수
  있는 환경에서 opt-in 명령을 재실행하기 전까지 상태를 `PENDING`으로 둔다.
- provider initialization `client.connect` 실패에는 DB-only fallback을 붙이지
  않는다. Redis command read fallback과 write failure 전파를 분리 유지한다.
- README/diagram/selector/catalog 변경은 module 구현과 같은 branch에서 함께
  제거할 수 있도록 작은 commit 단위로 유지한다.
- 테스트 cleanup은 literal prefix의 `SCAN`/`UNLINK`와 TTL만 사용하며 공유 Redis의
  `FLUSHDB`/`FLUSHALL`은 금지한다.

## 완료 조건

- 모든 수용 기준이 코드·테스트·README·diagram·CI 증적으로 추적된다.
- H2 기본 targeted test, compile/static analysis, detekt, selector/path 검증이
  fresh evidence로 통과한다.
- Redis opt-in targeted test가 fresh pass한다. Docker 불가 또는 opt-in 실패 시
  결과를 숨기지 않고 `PENDING`으로 유지하며 merge-ready/PR 보고를 보류한다.
- H2-only test는 Redis 없이 통과하고, Redis-tagged test는 opt-in에서만 실행된다.
- 정확한 package/import/constructor, `serialVersionUID`, public KDoc,
  explicit Jackson3 codec와 sync/suspend 전체 API parity가 README·테스트·소스에
  일치한다.
- Redis cache trust boundary, malformed/변조 payload 위험, ACL/TLS/secret
  manager 경고, 단일 client owner와 repository→client 종료 순서가 문서·테스트에
  남는다.
- 한 명의 개발자 lead lane이 module-runtime → integration-tests →
  documentation-assets → ci-registration 순서로 component evidence를 남긴다.
- 최종 branch diff를 독립 code review와 verifier가 읽은 뒤에만 PR/merge 승인
  단계로 전환한다.
