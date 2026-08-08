# Bluetape 1.4.0 예제 공통 기능 재사용 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: 각 작업을 시작하기 전에 해당 작업의 테스트를 먼저 작성하고, RED 증적을 남긴 뒤 최소 구현으로 GREEN을 만든다.

**Goal:** `bluetape4k-dependencies 1.4.0`이 제공하는 Exposed cache/Ktor/DDD API를 workshop 예제에 적용하고, 반복되는 Ktor JSON test helper를 `exposed-shared-tests`로 통합한다.

**Architecture:** 캐시 예제는 Bluetape Caffeine repository를 persistence/cache 경계로 사용하고, Ktor health/readiness contributor는 repository consistency probe를 노출한다. DDD 예제는 `AbstractAggregateRoot<Long>`와 `DomainEvent<Long>`를 도메인 경계에 사용한다. 공통 Ktor client helper는 기존 test-only shared module의 `src/main`에 한 번만 둔다. tenant context는 예제 간 차이를 보존하고 `bluetape4k-projects` 승격 issue로 분리한다.

**Tech Stack:** Kotlin 2.3, Java 21, Gradle version catalog, Exposed 1.12.1, Bluetape4k dependencies 1.4.0, Ktor, kotlinx.coroutines, JUnit 5, H2.

---

## Task 1: 1.4.0 catalog와 영향 모듈 의존성 고정

**Files:**
- `gradle/libs.versions.toml`
- `00-shared/exposed-shared-tests/build.gradle.kts`
- `11-high-performance/05-cache-strategies-ktor/build.gradle.kts`
- `11-high-performance/06-cache-strategies-coroutines-ktor/build.gradle.kts`
- `10-multi-tenant/07-multitenant-ktor/build.gradle.kts`
- `11-high-performance/07-routing-datasource-ktor/build.gradle.kts`
- `13-ecosystem-integrations/07-ddd-aggregate-repository/build.gradle.kts`

- `libs.exposed.cache`와 `libs.exposed.jdbc.caffeine` alias를 1.4.0 catalog module coordinate에 추가한다.
- shared helper가 컴파일할 수 있도록 Ktor test-host/client negotiation/serialization JSON dependency를 shared module에 `api`로 추가한다.
- 네 Ktor consumer에 `testImplementation(project(":exposed-shared-tests"))`를 추가하고 기존 중복 helper에만 필요한 직접 test dependency는 유지 여부를 compile로 확인한다.
- cache 두 모듈에 Caffeine/Exposed Ktor alias를 추가한다. DDD 모듈에는 `bluetape4k.idgenerators`와 Exposed `IdTable`에 필요한 alias를 추가한다.
- 수정 직후 `./gradlew projects`로 catalog alias와 project path를 확인한다.

## Task 2: 공통 Ktor JSON client helper를 RED/GREEN으로 통합

**Files:**
- `00-shared/exposed-shared-tests/src/test/kotlin/exposed/shared/tests/KtorTestSupportTest.kt`
- `00-shared/exposed-shared-tests/src/main/kotlin/exposed/shared/tests/KtorTestSupport.kt`
- 네 Ktor consumer의 `src/test/kotlin/**/Application*Test.kt`

- 먼저 네 consumer 테스트에서 private `createJsonClient` 구현과 로컬 Ktor `ContentNegotiation` import를 제거하고 shared extension import를 사용하도록 바꾼다. 이 시점에 helper source가 없으므로 `:exposed-shared-tests:test` 또는 affected test compile이 실패하는 RED를 기록한다.
- `KtorTestSupport.kt`에 `ApplicationTestBuilder.createJsonClient()`를 추가한다. `Json { ignoreUnknownKeys = true }` 설정은 기존 네 구현과 동일하게 유지한다.
- shared test에서 최소 Ktor test application에 JSON client를 사용해 응답을 decode하는 GREEN 테스트를 추가한다.
- 네 consumer 테스트를 다시 실행해 private duplicate가 제거되고 모든 호출자가 하나의 shared 구현을 사용함을 확인한다.

## Task 3: 동기 cache-aside/read-through/write-through 예제를 Bluetape Caffeine repository로 교체

**Files:**
- `11-high-performance/05-cache-strategies-ktor/src/main/kotlin/exposed/examples/ktor/cache/**`
- `11-high-performance/05-cache-strategies-ktor/src/test/kotlin/exposed/examples/ktor/cache/**`

- 테스트에 library repository 기반의 전략 동작, cache consistency readiness, `/health`와 `/ready` 응답, application stop 뒤 `TransactionManager.defaultDatabase` 복원을 먼저 명시하고 RED를 실행한다.
- `Users`를 `IdTable<String>`로 매핑하고 `CachedUser` persistence record를 만든다. `ExposedUserRepository`는 `AbstractJdbcCaffeineRepository<String, CachedUser>`를 상속하며 `findByIdFromDb`, `insertEntity`, `updateEntity`, `BatchInsertStatement` 매핑과 기존 database-read counter를 연결한다.
- service의 `ConcurrentHashMap` cache와 수동 lock/cache path를 제거한다. cache-aside는 repository cache의 read/populate API, read-through는 repository `get`, write-through는 configured `put`, invalidation은 repository `invalidate`를 사용한다. 기존 response DTO, route, hit/miss/read/cache-size stats는 유지한다.
- module lifecycle에서 공통 lifecycle lock 안에 `TransactionManager.defaultDatabase`의 이전 값을 저장하고, repository `close()`가 bounded/idempotent하게 끝난 뒤 현재 default가 자신이 설치한 DB인지 확인한다. identity가 다르면 fail-closed로 복원을 덮어쓰지 않고 소유권 오류를 증적한다. 정상 경로는 이전 default 복원 후 datasource 종료 순서를 지킨다.
- `installBluetape4kExposedKtor(installStatusPages=false, jdbcDatabase, jdbcBlockingDispatcher=Dispatchers.IO, healthPath="/health", readinessPath="/ready")`와 `ExposedKtorCacheContributor.jdbcRepository`를 사용한다. loopback/demo 전용 route는 allowlisted 상태만 반환하고 readiness `UP`/`DOWN`/timeout과 민감정보 redaction을 테스트한다.
- 1.4.0 `put`의 cache-first 동작을 전제로 DB write/취소 예외 시 service 보상 `invalidate`와 후속 DB read를 먼저 테스트한다. DB read count, strategy 결과, invalidation, readiness consistency, lifecycle restoration을 GREEN으로 확인한다.

## Task 4: suspend cache 예제를 cancellation-safe repository로 교체

**Files:**
- `11-high-performance/06-cache-strategies-coroutines-ktor/src/main/kotlin/exposed/examples/ktor/cache/**`
- `11-high-performance/06-cache-strategies-coroutines-ktor/src/test/kotlin/exposed/examples/ktor/cache/**`

- 기존 concurrent coalescing, write-through, invalidation, cancellation 테스트를 library `AbstractSuspendedJdbcCaffeineRepository` contract와 `/health`/`/ready` lifecycle 요구로 확장한 뒤 RED를 실행한다.
- `Products`를 `IdTable<String>`로 매핑하고 repository mapping/DB read counter를 구현한다. `LocalCacheConfig`의 write mode와 repository `get`/`put`/`invalidate`를 사용해 service의 map 및 key별 `Mutex`를 제거한다. persistence seed SKU allowlist 밖의 key는 library loader에 전달하기 전에 fail-closed로 거부한다.
- hit/miss/in-flight 통계는 cache 구현을 다시 만들지 않는 관측용 상태로만 유지한다. coalescing 자체는 Bluetape repository에 맡기고, library 내부 mutex map의 제거를 가정하지 않는다. cancellation 시 `CancellationException` 재전파와 후속 load 가능성을 검증한다.
- repository adapter의 DB operation과 lifecycle은 ownership guard 안에서 실행한다. `ApplicationStopped`에서 repository close → default database 소유권 확인 및 복원 → datasource close 순서를 보장하고 Ktor contributor readiness를 연결한다. write-through DB 예외 뒤 cache 보상 invalidate와 readiness `DOWN`도 검증한다.
- H2에서 동시 8개 read의 database read 1회, 후속 hit, unknown-key churn rejection, cancellation 후 후속 read, readiness, lifecycle 복원을 GREEN으로 확인한다.

## Task 5: DDD aggregate와 event를 Bluetape core API로 교체

**Files:**
- `13-ecosystem-integrations/07-ddd-aggregate-repository/src/main/kotlin/exposed/examples/ddd/aggregate/DddAggregateWorkshop.kt`
- `13-ecosystem-integrations/07-ddd-aggregate-repository/src/test/kotlin/exposed/examples/ddd/aggregate/DddAggregateWorkshopTest.kt`

- 테스트에서 non-null Snowflake aggregate ID, `DomainEvent<Long>` event contract, 성공 저장 뒤 event clear, rollback 뒤 pending event 보존을 먼저 표현하고 RED를 실행한다. `AbstractAggregateRoot` 자체는 DB rollback을 제공하지 않으므로 repository commit-after-clear 책임을 명시한다.
- `PurchaseOrder`를 `AbstractAggregateRoot<Long>`로 바꾸고 `Snowflakers.Global.nextId()`로 ID를 만든다. `OrderDomainEvent`는 `DomainEvent<Long>`와 기존 sequence/order number/payload 필드를 함께 유지하고 시간을 `Instant`로 보유한다.
- repository insert는 `EntityID(aggregate.id, WorkshopDddOrders)`를 명시하고, transaction 성공 뒤에만 `clearDomainEvents()`를 호출한다. 실패 hook에서는 event buffer를 건드리지 않는다.
- 기존 table schema, route가 아닌 aggregate command API, event ordering, rollback 및 invariant 테스트를 보존하고 README의 repository 예시를 새 contract에 맞춘다.

## Task 6: 독자 문서와 공통화 근거 동기화

**Files:**
- `00-shared/exposed-shared-tests/README.md`
- `00-shared/exposed-shared-tests/README.ko.md`
- `11-high-performance/05-cache-strategies-ktor/README.md`
- `11-high-performance/05-cache-strategies-ktor/README.ko.md`
- `11-high-performance/06-cache-strategies-coroutines-ktor/README.md`
- `11-high-performance/06-cache-strategies-coroutines-ktor/README.ko.md`
- `13-ecosystem-integrations/07-ddd-aggregate-repository/README.md`
- `13-ecosystem-integrations/07-ddd-aggregate-repository/README.ko.md`

- shared README 양쪽에 `createJsonClient` 사용법과 네 consumer 재사용 경계를 source-equivalent로 추가한다.
- cache README 양쪽에 Bluetape repository, health/readiness, lifecycle 책임을 반영하고 coroutine README의 coalescing 설명을 library 책임으로 수정한다.
- DDD README 양쪽의 자체 event buffer/nullable ID 예시를 `AbstractAggregateRoot`, `DomainEvent`, Snowflake ID, commit 후 clear semantics로 갱신한다.
- 기존 PNG/SVG architecture diagram은 구조가 바뀌지 않는 범위에서 재사용하며, diagram source-equivalent 규칙을 깨는 raw Mermaid를 추가하지 않는다.

## Task 7: library 승격 issue 중복 확인 및 등록

**Files:**
- 임시 issue body: `/tmp/bluetape4k-projects-tenant-context-issue.md`, `/tmp/bluetape4k-exposed-caffeine-mutex-issue.md`

- 코드와 targeted tests가 GREEN이 된 뒤 `bluetape4k-projects`의 tenant context와 `bluetape4k-exposed`의 suspended Caffeine mutex lifecycle 관련 모든 issue를 각각 다시 검색한다.
- label, milestone, assignee(`debop`)와 default branch를 live GitHub에서 확인한다.
- 중복이 없을 때만, 승인된 issue side effect 범위와 sanitized 한국어 body를 확인한 뒤 issue를 생성한다. tenant body에는 네 workshop 구현의 반복 근거, context 없음 거부, 인증 identity 결합, cross-tenant isolation/cache key, framework-neutral API, nested/cancellation/thread reuse acceptance를 포함한다. Caffeine body에는 release 1.12.1의 key mutex retention 근거, bounded-key demo 완화책, cancellation/high-cardinality acceptance를 포함한다.
- `gh issue view`로 생성된 issue title/body/labels/assignee/state를 read-back하고 번호/URL을 workflow evidence에 기록한다. 중복이면 새 issue를 만들지 않고 기존 번호와 검색 근거를 기록한다.

## Task 8: 검증, 독립 리뷰, DoD 증적

- TDD RED/GREEN 명령과 결과를 각 workflow component evidence에 첨부한다.
- 다음 targeted command를 fresh 실행한다.
  ```bash
  USE_FAST_DB=true ./gradlew :exposed-shared-tests:test :05-cache-strategies-ktor:test :06-cache-strategies-coroutines-ktor:test :07-ddd-aggregate-repository:test --no-daemon --console=plain
  ./gradlew :05-cache-strategies-ktor:compileTestKotlin :06-cache-strategies-coroutines-ktor:compileTestKotlin :07-ddd-aggregate-repository:compileTestKotlin --no-daemon --console=plain
  ./gradlew detekt --no-daemon --console=plain
  git diff --check
  ```
- 네 shared-helper consumer compile/test와 기존 관련 module 테스트를 추가 확인한다. H2 fast 외에 `USE_FAST_DB=false` 또는 repository canonical `-PuseDB=H2,POSTGRESQL`을 `--max-workers=1`과 `cleanTest --no-build-cache`로 시도하고, Testcontainers/Docker 미가용이면 정확한 N/A/blocked evidence와 잔여 리소스 판정을 기록한다.
- six perspective review 결과를 performance, stability/Ops, security, architecture, developer/API, user/caller 렌즈로 수집하고 P0/P1은 0으로 닫는다. P2/P3는 수정하거나 rationale과 후속 issue를 남긴다.
- `bluetape-flow.py` checklist의 required checks, component evidence, lane completion, main-lane completion을 갱신한다. PR/push/merge/release는 승인 범위 밖이므로 CG-11~18은 concrete N/A로 남긴다.
- 최종 DoD에 changed paths, 보존한 원본 dirty 다이어그램, test/detekt/diff evidence, issue read-back, known gaps, `DONE`/`PENDING`/`BLOCKED`를 포함한다.

## 실행 순서와 중단 조건

Task 1 → 2 → 3 → 4 → 5는 의존성 순서대로 실행한다. Task 6은 각 구현이 GREEN이 된 뒤 수행하고, Task 7은 issue 중복을 재확인할 수 있는 최종 코드 상태 이후에만 외부 side effect를 허용한다. Task 8은 모든 변경 후 수행한다. 승인된 범위를 벗어나는 public API 변경, PR/merge/push/release, destructive cleanup, 또는 동일 issue 중복 판단이 불확실한 경우에는 해당 branch를 중단하고 evidence를 남긴다.
