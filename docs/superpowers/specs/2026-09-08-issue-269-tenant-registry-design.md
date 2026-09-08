# Issue 269 Tenant JDBC Registry 전환 설계

## 문제와 근거

두 Spring MVC 예제가 `TenantDatabaseRegistry`와 `TenantDatabaseEntry`를 각각 복사해 tenant별 Hikari pool, Exposed `Database`, 부분 초기화 정리, 종료를 중복 구현한다. 현재 consumer 기준은 다음과 같다.

- `10-multi-tenant/05-database-per-tenant-spring-web/src/main/kotlin/exposed/multitenant/database/tenant/TenantDatabaseRegistry.kt`
- `10-multi-tenant/06-spring-security-tenant-authorization-spring-web/src/main/kotlin/exposed/multitenant/security/tenant/TenantDatabaseRegistry.kt`
- 두 모듈의 `DatabaseConfiguration.kt`, `TenantTransaction.kt`, `InventorySeeder.kt`, 통합 테스트
- upstream `bluetape4k-exposed#836`의 병합 commit `dd099f85377d798ef3e2754dc64064a125e4669b`
- 공용 non-Spring support module `00-shared/tenant-jdbc-support`

upstream 개발 버전 `io.github.bluetape4k.exposed:bluetape4k-exposed-tenant-jdbc:2.1.0-20260907.153611-1`은 Kotlin package `io.bluetape4k.exposed.tenant.jdbc`의 `TenantJdbcResourceRegistry.create`, `resourceFor`, `databaseFor`, `dataSourceFor`, `configuredTenants`, `close`를 제공한다. registry가 `Database.connect` 등록 해제와 disposer 호출을 역순으로 담당하며, close는 idempotent하다.

## 경계와 선택

각 consumer의 `TenantDataSourceProperties` wrapper만 Spring
`@ConfigurationProperties` binding 경계로 남긴다. 순수
`TenantJdbcSettings`와 `validate`, Hikari factory, tenant key normalization,
missing/duplicate 검증, provider registry factory/disposer wiring은
`00-shared/tenant-jdbc-support`가 소유한다. `TenantId` parsing,
authentication/authorization, request context, transaction orchestration은
consumer 책임으로 남긴다.

각 `DatabaseConfiguration`은 Spring-bound map을 shared
`toTenantJdbcResourceRegistry`에 전달하고 `TenantId.entries`와 parser/name
function만 제공한다. shared helper는 설정을 검증하고 Hikari factory와
`TenantJdbcResourceRegistry.create` disposer를 연결한다. non-null 반환 이후의
DataSource ownership은 provider로 넘기며 disposer는
`HikariDataSource.close()`만 수행한다. bean destroy method는 provider `close()`를
호출한다.

검토한 대안은 (1) local wrapper로 provider를 감싸는 방식, (2) 두 consumer가
provider type을 직접 주입하는 방식, (3) consumer마다 공용 helper 일부만
복사하는 방식이다. wrapper와 부분 복사는 중복된 lifecycle/configuration
surface를 다시 만들고 public contract가 두 겹이 되므로 제외한다. provider type
직접 주입과 shared non-Spring adapter를 함께 선택해 duplicate registry/entry와
normalization/validation/factory 중복을 제거한다. 두 모듈의 configuration
adapter 코드는 각 모듈의 Spring binding 경계를 명확히 하기 위해 얇게 둔다.

## 실패·호환성 계약

- 누락된 known tenant는 기존의 `Missing tenant datasource configuration: <tenant>` 메시지로 조기에 거부한다.
- `acme`와 ` ACME `처럼 같은 tenant로 normalize되는 key는 map의 마지막 값을 조용히 선택하지 않고 `IllegalArgumentException`으로 조기에 거부한다.
- configuration key가 `TenantId`에 매핑되지 않으면 기존 `TenantId.fromHeader`의 unknown tenant 오류를 유지한다.
- provider lookup의 미등록 key는 `UnknownTenantJdbcResourceException`의 고정 메시지를 사용하며 tenant 식별자를 오류 메시지에 복제하지 않는다.
- H2 URL, username, pool 설정과 `varchar`/transaction routing은 변경하지 않는다.
- provider가 `Database.connect` 후 close/unregister를 담당하므로 consumer가 별도 entry map이나 중복 close를 수행하지 않는다.
- `configuredTenants`는 provider의 read-only property로 사용하고, `databaseFor(tenant)`는 기존 명시적 Exposed transaction 호출을 유지한다.

## 검증과 문서

기존 tenant A/B 격리, seeding, rollback, request context cleanup 테스트를
유지하고 provider type/property 변경에 맞춰 갱신한다. shared support 테스트는
settings validation, Hikari defaults, normalized duplicate/missing, generic
registry factory와 disposer를 직접 검증한다. consumer contract 테스트는 두
예제 모두 `acme`/` ACME ` duplicate를 fail-fast로 확인한다. standalone
lifecycle 테스트는 두 DataSource가 닫히고 두 번째 `close()`가 성공하는지
확인한다. 두 README locale과 shared module README에는 provider가 DataSource와
`Database`를 소유하고 caller가 shutdown 전에 request drain을 수행해야 한다는
경계를 기록한다.

완료 조건은 duplicate registry/entry 삭제, 두 모듈의 개발 버전 dependency/POM 확인, targeted compile/test 통과, `git diff --check`, Kotlin final checklist의 lifecycle·Exposed·문서 항목 PASS다.
