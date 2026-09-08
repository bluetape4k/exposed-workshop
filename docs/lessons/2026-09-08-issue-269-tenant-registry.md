# Issue #269 tenant JDBC registry 전환 교훈

## 맥락

두 Spring MVC multi-tenant 예제가 `TenantDatabaseRegistry`와
`TenantDatabaseEntry`를 각각 구현해 Hikari `DataSource`, Exposed `Database`,
부분 초기화 정리, 종료를 중복 관리하고 있었다. upstream에 공용
`bluetape4k-exposed-tenant-jdbc` 개발 버전이 배포되어 consumer 전환을 진행했다.

## 결정

각 `DatabaseConfiguration`은 `TenantId.entries`와 검증된 Hikari factory를
`TenantJdbcResourceRegistry.create`에 전달하고, provider가 `Database.connect`,
등록 해제, disposer 호출, 역순 종료를 소유하도록 바꿨다. consumer는
`databaseFor`와 `configuredTenants`만 사용하며, Spring bean의 `destroyMethod`
`close`는 provider의 idempotent close 계약에 맡긴다. mutable 개발 버전은
catalog에서 exact build
`2.1.0-20260907.153611-1`로 고정했다.

## 리뷰 보완: 공용 설정·factory 경계

초기 전환 뒤에도 두 `DatabaseConfiguration`에 normalized map, Hikari 설정 검증,
provider factory/disposer wiring이 남아 있으면 lifecycle 중복이 사라져도
configuration contract가 다시 갈라진다. 그래서 `00-shared/tenant-jdbc-support`를
추가하고 다음 순서로 책임을 고정했다.

- `TenantJdbcSettings`가 순수 설정값, H2 `DB_CLOSE_DELAY=-1` 검증, Hikari 기본값과
  factory를 소유한다.
- `normalizeTenantJdbcSettings`가 parser를 호출한 뒤 명시적으로 `containsKey`를
  검사한다. `acme`와 ` ACME `가 같은 normalized key이면 마지막 값을 덮어쓰지
  않고 `IllegalArgumentException`을 발생시킨다. expected tenant 누락도 같은
  helper에서 fail-fast한다.
- `toTenantJdbcResourceRegistry`가 설정 검증과 provider
  `TenantJdbcResourceRegistry.create`/disposer 연결을 단일화한다. 각 consumer에는
  Spring `@ConfigurationProperties` wrapper와 `TenantId` parser/name function만
  남는다.

공용 모듈은 exact timestamped tenant JDBC provider를 `api`로 소비하지만 Spring
의존성은 갖지 않는다. 따라서 Spring binding은 예제에 남고 provider가 factory
반환 이후 `DataSource`와 Exposed `Database` lifecycle을 계속 소유한다.

## 결과와 검증

- 두 local registry/entry 구현을 삭제했다.
- 두 모듈의 integration test와 lifecycle test를 provider type 기준으로
  갱신했다. standalone registry는 `close()`를 두 번 호출하고 datasource가
  닫힌 뒤 lookup이 고정된 closed 예외로 실패하는지 확인한다.
- shared support test와 두 consumer contract test에서 normalized duplicate
  tenant 설정(`acme`/` ACME `)이 동일한 예외 메시지로 fail-fast하는지 확인한다.
- `./gradlew :tenant-jdbc-support:test --rerun-tasks --no-build-cache --no-daemon`
  결과: 4 passing.
- `./gradlew :05-database-per-tenant-spring-web:test --rerun-tasks --no-build-cache --no-daemon`
  결과: 15 passing.
- `./gradlew :06-spring-security-tenant-authorization-spring-web:test --rerun-tasks --no-build-cache --no-daemon`
  결과: 33 passing.
- shared support와 두 모듈 `compileKotlin` 및 `compileTestKotlin`, targeted
  `detekt`, `git diff --check`가 통과했다.
- runtime dependency graph에서 shared support와 두 consumer 모두
  `io.github.bluetape4k.exposed:bluetape4k-exposed-tenant-jdbc:2.1.0-20260907.153611-1`
  을 확인했다.

## 예상 밖의 실패

첫 RED compile에서 provider artifact는 정상적으로 resolve됐지만 import package를
artifact 좌표의 `io.github...`로 추정해 unresolved reference가 발생했다. jar와
`javap`로 공개 package가 `io.bluetape4k.exposed.tenant.jdbc`임을 확인하고,
production/test import를 수정한 뒤 GREEN compile을 다시 실행했다.

## 다음 작업을 위한 guard

upstream Kotlin API를 개발 버전으로 소비할 때는 좌표만 보지 말고 resolved jar의
package와 public signature를 먼저 확인한다. mutable 개발 버전 대신 Central
metadata의 timestamped build를 catalog에 고정하고, consumer lifecycle 문서에는
resource ownership과 shutdown 전 request drain 경계를 함께 기록한다.

## Writer DoD

- SPW-01: PASS — issue 설계/계획, provider jar, Gradle graph, 대상 모듈을
  근거로 audience와 범위를 고정했다.
- SPW-02: PASS — 맥락, 결정, 결과, 실패, future guard, 검증 명령을 기록했다.
- SPW-03: PASS — Korean technical register를 적용하고 API/좌표/명령/예외 문구를
  보존했다.
- SPW-04: PASS — exact build, package, 테스트 결과를 현재 worktree와
  대조했다.
- SPW-05: PASS — Markdown을 다시 읽고 제목·목록·코드 토큰과 locale 문서를
  확인했다.
- KO-01..KO-07: PASS — 사실·식별자·용어·reader-facing 표면을 검토하고
  변경된 Korean README에 terminology audit을 실행했다.
