# 이슈 #240 JDBC Lettuce 캐시 예제 구현 계획

## 계획 상태

- 대상 이슈: [#240](https://github.com/bluetape4k/exposed-workshop/issues/240)
- 구현 branch: `feat/issue-240-jdbc-lettuce-cache`
- 설계 명세: `docs/superpowers/specs/2026-08-28-issue-240-jdbc-lettuce-cache-design.md`
- 명세 리뷰: `docs/superpowers/reviews/2026-08-28-issue-240-jdbc-lettuce-cache-spec-review.md`
- 설계·명세 승인: 완료
- 계획 승인: 대기
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

골격 직후 `./gradlew :08-cache-strategies-lettuce:test
--no-daemon --no-configuration-cache`를 실행해 등록·configuration 오류를
먼저 고정한다.

### 3. TDD RED: 관찰 가능한 테스트 계약을 먼저 작성

production repository를 작성하기 전에 다음 테스트와 공용 fixture를 만든다.
테스트가 기대 API 또는 아직 없는 repository를 참조해 처음에는 실패하는지
확인하고, 실패 로그를 기록한 뒤 구현 단계로 넘어간다. 테스트 파일의 실제
package는 기존 chapter convention을 따른다.

- `11-high-performance/08-cache-strategies-lettuce/src/test/kotlin/**/ProductLettuceCacheTest.kt`
  - H2 `ProductTable`, `ProductRecord`, sync/suspend fixture
  - explicit Jackson3 codec round-trip과 동일 namespace 구성
  - `get` miss→DB→cache, 두 번째 hit, 없는 ID의 `null`
  - `getAll`의 MGET 결과 map과 miss별 loader/SET 비용 관찰
  - `put`/`putAll` write-through, 양수 `batchSize`와 잘못된 batchSize
  - 단건·복수·pattern·clear invalidation 및 다른 namespace 보존
  - `findAll` 결과 반환과 cache-only warm
  - DB writer/loader 오류와 Redis write 오류의 전파 경계

- `11-high-performance/08-cache-strategies-lettuce/src/test/kotlin/**/ProductSuspendedLettuceCacheTest.kt`
  - sync와 같은 table/record/codec/namespace를 사용하는 suspend API
  - `get`, `getAll`, `put`, `putAll`, cancellation의 동일 계약

- `11-high-performance/08-cache-strategies-lettuce/src/test/kotlin/**/RedisLettuceCacheIntegrationTest.kt`
  - `@Tag("redis")`, `RedisServer.Launcher.redis`, `LettuceClients.clientOf`
  - `workshop:products:<unique-test-suffix>` namespace와 별도 namespace 격리
  - `finally`의 provider cleanup: literal prefix `SCAN MATCH` + `UNLINK`;
    `FLUSHDB`/`FLUSHALL` 금지
  - Redis GET/MGET/cache-only SET/warm 오류 시 DB fallback 또는 결과 보존
  - Redis 중단→fallback→재시작→같은 client의 auto-reconnect와 cache fill
  - repository close-before-use, 반복 close, repository→client shutdown 순서
  - `ShutdownQueue` 등록과 공유 `DEFAULT_CLIENT_RESOURCES` 비소유권
  - 뒤의 batch chunk/pipeline 실패에서 앞선 DB commit·partial state·예외 보존

Redis 오류는 provider가 실제로 제공하는 loader/writer seam 또는 통제된
Lettuce/Testcontainers fixture로 주입한다. provider source를 우회하는 새
production abstraction은 만들지 않는다. query/command counter는 작은 고정
입력에서 `getAll`의 한 번 MGET·miss별 fallback과 `putAll` chunk/pipeline 경계를
관찰하는 데만 사용하고 성능 수치로 확장하지 않는다.

### 4. TDD GREEN: provider subclass와 공용 모델 구현

다음 production 파일을 추가한다(실제 package와 파일 분리는 기존 module
layout 확인 후 결정하되 책임은 유지한다).

- `11-high-performance/08-cache-strategies-lettuce/src/main/kotlin/**/ProductLettuceCacheWorkshop.kt`
  - `ProductTable`은 `LongIdTable` 기반으로 정의하고 `ProductRecord`는
    `Serializable` data class로 둔다.
  - `ProductJdbcLettuceRepository`는
    `AbstractJdbcLettuceRepository<Long, ProductRecord>`를 직접 상속한다.
  - `ProductSuspendedJdbcLettuceRepository`는
    `AbstractSuspendedJdbcLettuceRepository<Long, ProductRecord>`를 직접 상속한다.
  - 두 repository의 `table`, `ResultRow.toEntity`, `UpdateStatement.updateEntity`,
    `BatchInsertStatement.insertEntity`, `extractId` mapping을 동일하게 유지한다.
  - 기본 config는 `LettuceCacheConfig.READ_WRITE_THROUGH.copy(nearCacheEnabled = false,
    keyPrefix = "workshop:products")`이며 codec은 생성 시
    `ExposedLettuceCodecs.jackson3(ProductRecord::class.java)`를 명시한다.
  - `get`, `getAll`, `put`, `putAll`, `findAll`, `invalidate`,
    `invalidateByPattern` 호출은 provider API의 기본 인자·반환 의미를 그대로
    노출한다. `getAll`은 ID→record map, pattern은 내부 allow-list suffix만
    받는다.
  - repository는 외부 `RedisClient`를 닫지 않는다. fixture가
    `ShutdownQueue`/명시적 shutdown을 소유하고 repository를 먼저 닫는다.

GREEN 단계에서는 테스트가 요구한 최소 동작만 구현하고 near-cache, retry loop,
manager, endpoint를 추가하지 않는다. `CancellationException`은 일반 fallback으로
변환하지 않고 provider coroutine 경계를 보존한다.

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
   - 기존 Redisson의 TTL/near-cache/transaction boundary와 source/provider
     근거가 있는 비교표
   - R2DBC는 `exposed-r2dbc-workshop`에서 별도 구현한다는 경계
4. chapter index를 갱신한다.
   - `11-high-performance/README.md`
   - `11-high-performance/README.ko.md`
5. diagram skill 규칙에 따라 다음 source-equivalent pair를 생성한다.
   - `docs/images/readme-diagrams/11-high-performance-08-lettuce-cache-architecture.svg`
   - `docs/images/readme-diagrams/11-high-performance-08-lettuce-cache-architecture.png`
   - `docs/images/readme-diagrams/11-high-performance-08-lettuce-cache-sequence.svg`
   - `docs/images/readme-diagrams/11-high-performance-08-lettuce-cache-sequence.png`
   - 독자-facing label이 있는 경우 EN/KO SVG/PNG를 각각 제공하고, CairoSVG
     render·pair audit·full-size visual inspection을 수행한다. README에는 raw
     Mermaid를 남기지 않는다.

### 6. CI/selector 등록

- `.github/scripts/select-changed-examples.sh`의 `ALL_TASKS`와 chapter 11 path
  mapping에 `:08-cache-strategies-lettuce:build`를 추가한다.
- `.github/workflows/examples.yml`의 push/pull_request path filter에
  `11-high-performance/08-cache-strategies-lettuce/**`를 추가한다.
- 기존 `ci.yml` 전체 test/nightly H2 기본 경로를 Redis opt-in과 섞지 않는다.
- selector를 직접 실행해 새 module task가 선택되고, unrelated path에는 선택되지
  않는지 확인한다.

### 7. 검증·증적·통합 준비

변경 순서에 따라 다음 검증을 순차 실행하고 출력·경로·commit SHA를 기록한다.

1. `git diff --check`
2. `./gradlew projects --no-daemon --no-configuration-cache`
3. `./gradlew :08-cache-strategies-lettuce:test --no-daemon --no-configuration-cache`
4. `./gradlew :08-cache-strategies-lettuce:compileKotlin
   :08-cache-strategies-lettuce:compileTestKotlin --no-daemon
   --no-configuration-cache`
5. `./gradlew :08-cache-strategies-lettuce:detekt --no-daemon
   --no-configuration-cache` (repository task가 제공하는 경우)
6. Redis runtime 가능 여부를 `colima status`, `docker context show`, `docker info`
   로 확인한다. 관리된 `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`를 우선 사용한다.
7. Docker가 준비되면
   `./gradlew :08-cache-strategies-lettuce:test -PincludeRedisIntegration=true
   --no-daemon --no-configuration-cache`를 실행한다. Docker가 없으면 opt-in
   결과를 실패/미검증으로 기록하고 완료를 주장하지 않는다.
8. README EN/KO 목차·명령·제약 비교, diagram pair/audit, selector/workflow
   path를 다시 읽는다.
9. `git status`, `git diff --stat`, `git diff --check`와 module 전체 diff를
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
- Redis opt-in targeted test가 통과하거나, Docker 불가 사실과 미검증 범위가
  최종 DoD에 명시된다.
- 한 명의 개발자 lead lane이 module-runtime → integration-tests →
  documentation-assets → ci-registration 순서로 component evidence를 남긴다.
- 최종 branch diff를 독립 code review와 verifier가 읽은 뒤에만 PR/merge 승인
  단계로 전환한다.
