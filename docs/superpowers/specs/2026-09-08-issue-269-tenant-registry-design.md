# Issue 269 Tenant JDBC Registry 전환 설계

## 문제와 근거

두 Spring MVC 예제가 `TenantDatabaseRegistry`와 `TenantDatabaseEntry`를 각각 복사해 tenant별 Hikari pool, Exposed `Database`, 부분 초기화 정리, 종료를 중복 구현한다. 현재 consumer 기준은 다음과 같다.

- `10-multi-tenant/05-database-per-tenant-spring-web/src/main/kotlin/exposed/multitenant/database/tenant/TenantDatabaseRegistry.kt`
- `10-multi-tenant/06-spring-security-tenant-authorization-spring-web/src/main/kotlin/exposed/multitenant/security/tenant/TenantDatabaseRegistry.kt`
- 두 모듈의 `DatabaseConfiguration.kt`, `TenantTransaction.kt`, `InventorySeeder.kt`, 통합 테스트
- upstream `bluetape4k-exposed#836`의 병합 commit `dd099f85377d798ef3e2754dc64064a125e4669b`

upstream snapshot `io.github.bluetape4k.exposed:bluetape4k-exposed-tenant-jdbc:2.1.0-SNAPSHOT`은 `TenantJdbcResourceRegistry.create`, `resourceFor`, `databaseFor`, `dataSourceFor`, `configuredTenants`, `close`를 제공한다. registry가 `Database.connect` 등록 해제와 disposer 호출을 역순으로 담당하며, close는 idempotent하다.

## 경계와 선택

`TenantDataSourceProperties`와 `TenantJdbcProperties`는 Spring `@ConfigurationProperties` binding 및 예제별 H2 URL 검증을 위해 각 모듈에 남긴다. `TenantId` parsing, missing/unknown configuration 오류, authentication/authorization, request context, transaction orchestration도 consumer 책임으로 남긴다.

각 `DatabaseConfiguration`은 `TenantId.entries`를 provider에 전달하고, 이미 검증한 property로 HikariDataSource를 만든다. non-null 반환 이후의 DataSource ownership은 provider로 넘기며 disposer는 `HikariDataSource.close()`만 수행한다. bean destroy method는 provider `close()`를 호출한다.

검토한 대안은 (1) local wrapper로 provider를 감싸는 방식, (2) 두 consumer가 provider type을 직접 주입하는 방식이다. wrapper는 중복된 lifecycle surface를 다시 만들고 public contract가 두 겹이 되므로 제외한다. provider type 직접 주입은 duplicate registry/entry를 제거하고 lifecycle 단일화를 보장하므로 선택한다. 두 모듈의 configuration adapter 코드는 각 모듈의 Spring binding 경계를 명확히 하기 위해 별도로 둔다.

## 실패·호환성 계약

- 누락된 known tenant는 기존의 `Missing tenant datasource configuration: <tenant>` 메시지로 조기에 거부한다.
- configuration key가 `TenantId`에 매핑되지 않으면 기존 `TenantId.fromHeader`의 unknown tenant 오류를 유지한다.
- provider lookup의 미등록 key는 `UnknownTenantJdbcResourceException`의 고정 메시지를 사용하며 tenant 식별자를 오류 메시지에 복제하지 않는다.
- H2 URL, username, pool 설정과 `varchar`/transaction routing은 변경하지 않는다.
- provider가 `Database.connect` 후 close/unregister를 담당하므로 consumer가 별도 entry map이나 중복 close를 수행하지 않는다.
- `configuredTenants`는 provider의 read-only property로 사용하고, `databaseFor(tenant)`는 기존 명시적 Exposed transaction 호출을 유지한다.

## 검증과 문서

기존 tenant A/B 격리, seeding, rollback, request context cleanup 테스트를 유지하고 provider type/property 변경에 맞춰 갱신한다. standalone lifecycle 테스트는 두 DataSource가 닫히고 두 번째 `close()`가 성공하는지 확인한다. 두 README locale에는 provider가 DataSource와 `Database`를 소유하고 caller가 shutdown 전에 request drain을 수행해야 한다는 경계를 기록한다.

완료 조건은 duplicate registry/entry 삭제, 두 모듈의 snapshot dependency/POM 확인, targeted compile/test 통과, `git diff --check`, Kotlin final checklist의 lifecycle·Exposed·문서 항목 PASS다.
