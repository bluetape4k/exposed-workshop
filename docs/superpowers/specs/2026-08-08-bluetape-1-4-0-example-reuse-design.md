# Bluetape 1.4.0 예제 공통 기능 재사용 설계

## 목표

`bluetape4k-dependencies 1.4.0`이 제공하는 `bluetape4k-exposed 1.12.1`의
cache, Ktor health/readiness, DDD aggregate API를 exposed-workshop 예제에
적용한다. 예제가 직접 만든 캐시 저장소·이벤트 버퍼를 Bluetape API로
교체하고, 서로 반복되는 Ktor 테스트 헬퍼는 기존 `00-shared` 테스트 모듈로
통합한다. 여러 멀티테넌시 예제에 반복되는 테넌트 컨텍스트 요구는 라이브
GitHub 중복 확인 후 별도 library 승격 issue로 남긴다.

## 현재 근거

- 기준 저장소는 `codex/exposed-workshop-1.4.0-reuse` worktree이며
  `origin/develop`의 `25698d8c`에서 시작한다.
- 원래 `develop` checkout에는 다이어그램 SVG/PNG 207개가 dirty 상태로
  존재했으며, 별도 worktree를 사용해 그 변경을 보존한다.
- `bluetape4k-dependencies 1.4.0`의 catalog에는
  `bluetape4k-exposed 1.12.1`, `bluetape4k-exposed-cache`,
  `bluetape4k-exposed-jdbc-caffeine`, `bluetape4k-exposed-jdbc-lettuce`,
  `bluetape4k-exposed-ktor`가 등록되어 있다.
- 기준선에서 다음 네 모듈 테스트는 `USE_FAST_DB=true`와 Gradle
  `--no-daemon`으로 `rc=0`, `BUILD SUCCESSFUL`이었다.
  `exposed-shared-tests`, `05-cache-strategies-ktor`,
  `06-cache-strategies-coroutines-ktor`, `07-ddd-aggregate-repository`.
- `11-high-performance/05`와 `/06`은 각각 `ConcurrentHashMap` 기반 캐시,
  hit/miss 카운터, DB read 카운터, 수동 health endpoint를 구현한다.
- `13-ecosystem-integrations/07`은 `PurchaseOrder`의 자체 pending event
  buffer와 `OrderDomainEvent`를 구현한다. Bluetape의
  `AbstractAggregateRoot`/`DomainEvent`는 ID 일치 검증과 drain/clear 계약을
  제공하지만 DB rollback은 제공하지 않으므로, commit 이후 clear와 예외 시
  buffer 보존은 workshop repository가 책임져야 한다.
- `00-shared/exposed-shared-tests` 외에 production shared 모듈은 없으며,
  다음 네 Ktor 테스트에 동일한 JSON client helper가 중복되어 있다.
  `11-high-performance/05-cache-strategies-ktor`,
  `11-high-performance/06-cache-strategies-coroutines-ktor`,
  `10-multi-tenant/07-multitenant-ktor`,
  `11-high-performance/07-routing-datasource-ktor`.
- 현재 open issue `bluetape4k/exposed-workshop#166`은 FastFory cache 도입
  대기 건으로, 이번 Caffeine repository 전환과 중복되지 않는다.

## 설계 선택

### 캐시 예제

권장안은 두 예제의 저장소 경계를 `AbstractJdbcCaffeineRepository`와
`AbstractSuspendedJdbcCaffeineRepository`로 교체하는 것이다.

- `Users`/`Products` 테이블을 `IdTable<String>`으로 바꾸고 기존 string ID와
  endpoint response shape는 유지한다.
- 동기 저장소는 `findByIdFromDb`, `insertEntity`, `updateEntity`에 기존 DB
  read counter를 연결하고, service는 library `get`, `put`, `invalidate`와
  `LocalCacheConfig`/`CacheWriteMode.WRITE_THROUGH`를 사용한다. library
  `put`은 cache를 먼저 갱신하므로 DB write 예외·취소 시 service가 즉시
  `invalidate`를 보상 호출하고 readiness를 `DOWN`으로 관측할 수 있게 한다.
- suspend 저장소는 library의 per-key `Mutex`와 cancellation-safe
  transaction path를 사용하고, coalescing의 외부 계약은 `databaseReads == 1`로
  좁힌다. 기존 DTO의 `cacheHits`, `cacheMisses`, `inFlightLoads` 필드는 response
  호환성을 위해 남기되 library loader의 내부 관측값이라고 주장하지 않으며,
  해당 정확한 수치는 수용 기준에서 제외한다. 1.4.0 library 내부 mutex map의
  수명은 library 계약으로 취급하며, workshop이 lock을 재구현하거나 “lock 제거”를
  주장하지 않는다. demo는 persistence가 seed한 SKU allowlist만 cache loader에
  전달해 미지의 고카디널리티 key가 library loader에 유입되지 않도록 fail-closed
  한다. 실제 cancellation 뒤 `CancellationException` 재전파와 후속 조회 성공만
  검증한다.
- Ktor module은 실제 1.12.1 overload를 사용한다. 각 application은
  `Bluetape4kExposedKtorConfig(jdbcDatabase = database,
  jdbcBlockingDispatcher = Dispatchers.IO, installStatusPages = false,
  installHealthRoutes = true, healthPath = "/health", readinessPath = "/ready")`와
  `ExposedKtorCacheReadinessConfig`를 함께 넘긴다. readiness contributor는
  `jdbcRepository("users"/"products") { repository.validateConsistency() }`와
  `custom("*-write") { writeFailureLatch ? DOWN : UP }`를 분리해 구성한다.
  전자는 library worker report를, 후자는 `WRITE_THROUGH` DB 예외 뒤 service가
  기록한 O(1) application-owned latch를 관측한다. 성공한 다음 write는 latch를
  reset한다. 기존 수동 `/health` route와 그 전용 `HealthResponse`는 제거한다.
  health는 library가 반환하는 allowlisted 상태만 노출하고 SQL/URL/credential/cache
  key/exception detail을 포함하지 않는 loopback/demo 전용 route로 둔다. `main`의
  embedded server는 `host = "127.0.0.1"`로 고정하고, readiness는 library의
  `UP`/`DOWN`/timeout HTTP status를 함께 검증한다.
- JDBC repository base가 implicit `transaction {}`를 사용하므로
  `TransactionManager.defaultDatabase`는 각 demo module의 application-local
  owner가 명시적으로 관리한다. 새 production shared module을 만들지 않는
  대신 각 module이 JVM 내 자신의 `Any` lifecycle lock과 owner token을 갖고,
  같은 module에서 서로 다른 두 database lifecycle이 동시에 소유권을 얻으려
  하면 fail-fast한다. 시작/종료는 그 lock 안에서 수행한다. 시작 실패 시 이미
  등록된 database도 `TransactionManager.closeAndUnregister(database)`로
  해제하고 datasource·owner를 독립적으로 정리한다. 정상 종료는 repository
  `close()` → 현재 default가 자신이 설치한 DB인지 확인 → 이전 default 복원 →
  `closeAndUnregister(database)` → datasource close → owner release 순서이며,
  owner mismatch에서는 복원을 덮어쓰지 않고도 unregister/datasource/owner
  cleanup을 시도한다. module 간 JVM-global exclusion은 범위 밖이며 이를
  보장한다고 주장하지 않는다.

대안 1은 공통 production cache service를 새 shared 모듈에 만드는 방식이다.
이는 교육용 cache-aside/read-through/write-through 차이를 감추고 새 모듈
등록·docs·CI 범위를 늘리므로 선택하지 않는다. 대안 2는 기존 map/cache
구현을 유지한 채 dependency만 추가하는 방식이다. 이는 1.12.1의 핵심
재사용 목표와 library lifecycle 검증을 달성하지 못하므로 선택하지 않는다.

### DDD aggregate

`PurchaseOrder`는 `AbstractAggregateRoot<Long>`를 상속하고, domain event는
`DomainEvent<Long>`를 구현한다.

- 신규 aggregate ID는 이미 catalog에 있는 Bluetape `Snowflakers.Global`로
  생성하여 DB auto-increment 의존성을 제거한다.
- `PurchaseOrder.id`는 생성 시점부터 non-null이며, Exposed insert는
  `EntityID(aggregate.id, WorkshopDddOrders)`를 명시한다.
- event의 시간은 `Instant`로 보유하고, 교육용 event table의 epoch millis와
  sequence/order number/payload 필드는 유지한다.
- repository는 저장 성공 뒤 `clearDomainEvents()`를 호출한다. transaction
  rollback이나 예외에서는 buffer를 지우지 않아 재시도 semantics를 보존한다.
- 기존 명령 guard, event row persistence, approve 중복 방지, rollback 테스트
  의 외부 동작은 유지한다.

대안 1은 자체 event buffer를 유지하고 일부 helper만 호출하는 방식이다.
이는 library aggregate contract를 실제로 사용하는 것이 아니므로 선택하지
않는다. 대안 2는 `UUID` aggregate ID로 바꾸는 방식이다. 현재 테이블·테스트가
`Long`을 전제로 하고 Bluetape ID generator 재사용이라는 목표에도 맞지 않아
선택하지 않는다.

### shared 통합과 승격 issue

기존 shared 테스트 모듈에 `ApplicationTestBuilder.createJsonClient()` 확장
헬퍼를 추가하고 네 모듈의 private helper를 제거한다. 각 consumer가 명시적으로
shared test fixture를 참조하게 하며, 이 변경은 테스트 지원 코드만 통합한다.
교육용 production plugin·datasource는 shared에 넣지 않는다.

다음 테넌트 컨텍스트 구현은 교육용 차이를 보존하되, live duplicate check 후
`bluetape4k-projects`에 프레임워크 중립 primitive 승격 issue를 등록한다.

1.4.0 suspended Caffeine repository의 key별 mutex lifecycle은 workshop에서
재구현하지 않고 `bluetape4k-exposed`에 별도 개선 issue 후보로 등록한다. 이
issue는 release 버전의 known limitation과 bounded-key demo 완화책을 함께
기록한다.

- ThreadLocal/ScopedValue 기반 context;
- coroutine `CoroutineContext.Element` 및 `withTenant` 범위;
- 중첩 범위 복원, cancellation, thread reuse에 대한 leakage 방지;
- context가 없으면 기본적으로 거부하고, 인증된 caller identity에 tenant를
  결합하는 정책;
- tenant를 cache key/isolation boundary에 포함하는 규칙과, caller가 정책을
  명시적으로 주입할 수 있는 API.

Issue target은 cross-framework 설계이므로 `bluetape4k-projects`로 권장한다.
동일 issue가 발견되면 생성하지 않고 검색 URL/번호를 evidence로 남긴다.

## 경계와 비목표

- `bluetape4k-dependencies` 또는 `bluetape4k-exposed` 소스는 수정하지 않는다.
- 새 production shared 모듈을 추가하지 않는다.
- Ktor endpoint path와 response DTO, DDD event table schema를 불필요하게
  변경하지 않는다.
- PR 생성, push, merge, release, tag, branch 삭제는 범위 밖이다.
- `#166`을 수정하거나 별도 FastFory issue를 생성하지 않는다.
- suspended Caffeine repository의 key별 mutex lifecycle 개선은 workshop 내부
  재구현으로 숨기지 않고 `bluetape4k-exposed` 승격 issue로 별도 기록한다.
- 테넌트 예제의 모든 구현을 하나의 교육용 class로 강제 통합하지 않는다.

## 실패 모드와 대응

1. **Default database 누수:** 여러 `testApplication` 실행 뒤 이전
   `TransactionManager.defaultDatabase`가 덮어써질 수 있다. module-local
   lifecycle lock, owner token, 설치 DB identity check,
   `closeAndUnregister`, 시작 실패/두 번째 owner 거부/중첩 stop/반복 실행
   테스트로 소유권과 실패 cleanup을 증명한다. module 간 동시성은 범위 밖으로
   명시한다.
2. **Cache/DB 불일치:** 1.4.0 write-through는 cache-first이므로 DB 예외나
   취소 뒤 stale entry가 남을 수 있다. service 보상 invalidate, 후속 DB
   read, application-owned failure latch를 통한 readiness `DOWN`, 성공 write
   뒤 latch reset, repository idempotent close와 bounded close evidence를
   남긴다.
3. **Coroutine cancellation 계약:** in-flight load 취소 시 library 내부
   mutex 수명을 workshop이 추정하지 않는다. 실제 job cancellation에서
   `CancellationException`을 재전파하고 후속 재조회를 성공시키는 테스트를
   둔다.
4. **DDD event 조기 clear:** persist 중 예외 전에 event buffer를 지우면
   재시도가 event를 잃는다. library buffer는 in-memory 계약으로만 사용하고,
   성공 commit 이후에만 `clearDomainEvents()`를 호출하며 rollback 테스트에서
   pending event를 검증한다.
5. **ID insert 충돌:** explicit Snowflake ID와 Exposed `EntityID` 매핑이
   어긋나면 aggregate row와 event foreign key가 분리될 수 있다. 신규 생성,
   rehydrate, approve, rollback을 모두 실제 H2 transaction으로 검증한다.
6. **Shared test classpath 확장:** Ktor client dependency를 shared test
   module에 잘못 노출하면 모든 consumer compile surface가 커진다. helper를
   `testFixtures` 성격의 기존 shared module 안에 두고 affected modules만
   compile/test하여 의존성 범위를 확인한다.
7. **Library mutex key churn:** 1.4.0 suspended Caffeine repository의
   내부 key별 mutex 수명은 workshop이 조정할 수 없다. seed SKU allowlist와
   unknown-key rejection으로 예제 입력 cardinality를 bounded하게 유지하고,
   실제 library 개선 후보는 `bluetape4k-exposed` issue로 중복 확인 후
   등록한다.

## 호환성 및 rollback

기존 HTTP route/DTO와 DB table 이름은 유지한다. cache implementation 변경은
loopback/demo 전용 in-memory 상태가 재시작 시 사라지는 현재 교육용 특성을
유지한다. 문제가
발생하면 각 cache module에서 Caffeine repository wiring을 이전 local map
구현으로 되돌릴 수 있고, DDD module에서는 event table/API를 유지한 채
aggregate adapter만 되돌릴 수 있다. catalog alias 추가가 compile을 깨우면
1.4.0이 이미 제공하는 정확한 alias명을 다시 확인하고 해당 build file만
수정한다.

## 검증 가능한 수용 기준

1. 05/06 cache module이 `libs.exposed.jdbc.caffeine` 및 필요 시
   `libs.exposed.ktor`를 사용하고, 직접 만든 `ConcurrentHashMap` cache path가
   production service에서 제거된다.
2. 기존 cache 전략·read counter·write-through·invalidate 테스트와 실제 library
   repository를 통한 동시성 coalescing(`databaseReads == 1`)이 통과한다. suspend
   DTO의 hit/miss/in-flight 필드는 호환성을 유지하지만 내부 library mutex의
   정확한 수치를 수용 기준으로 삼지 않는다.
3. 실제 `Bluetape4kExposedKtorConfig` +
   `ExposedKtorCacheReadinessConfig` overload가 library health/readiness route를
   설치한다. repository consistency contributor와 write-failure latch contributor가
   각각 `UP`/`DOWN`/timeout으로 변환하고, 기존 수동 health route가 없으며,
   allowlisted redacted response를 반환한다. application stop 뒤
   `closeAndUnregister`와 소유권 검증을 통과한 경우에만 default database가
   이전 값으로 돌아온다. 시작 실패·두 번째 owner·중첩 stop·반복 실행 cleanup도
   테스트한다.
4. DDD aggregate가 `AbstractAggregateRoot<Long>`/`DomainEvent<Long>`를
   사용하며, success commit만 event를 clear하고 rollback은 pending event를
   보존한다.
5. 중복 Ktor JSON test helper가 shared module의 한 구현으로 통합되고, 네
   consumer 모듈에서 private duplicate가 사라진다.
6. tenant-context와 suspended Caffeine mutex lifecycle 후보 issue는 각각
   target repository의 live duplicate check와 post-create read-back을 갖추고,
   context 없음 거부·인증 identity 결합·nested/cancellation/thread reuse·
   cross-tenant isolation acceptance 또는 bounded key/cancellation acceptance를
   포함한 sanitized 한국어 body를 사용하거나, 중복 issue 번호와 그 이유를
   증적으로 남긴다.
7. 영향 모듈 compile/test, `detekt`, `git diff --check`, README EN/KO parity
   검사가 fresh evidence로 통과한다.

## 완료 정의

변경 파일 범위가 승인된 모듈과 docs로 제한되고, Type A checklist의 해당
행이 fresh evidence로 채워지며, P0/P1 finding이 0이다. PR/merge를 만들지
않는 현재 범위에서는 CG-11~CG-18을 concrete N/A로 기록하고, issue side
effect가 수행된 경우 live URL/read-back을 DoD에 포함한다.
