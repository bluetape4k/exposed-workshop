# Issue 269 Tenant JDBC Registry 전환 Implementation Plan

> **For agentic workers:** 승인된 설계를 기준으로 아래 작업을 순서대로 실행한다. 각 단계는 RED/GREEN 증거와 정확한 파일 범위를 남긴다.

**Goal:** 두 Spring MVC multi-tenant 예제가 published `TenantJdbcResourceRegistry`를 사용하고 중복 registry/entry lifecycle 구현을 제거한다.

**Architecture:** 각 Spring configuration adapter는 `@ConfigurationProperties`로
binding한 map과 `TenantId` parser만 공용 non-Spring
`tenant-jdbc-support`에 전달한다. shared helper가 normalized map, duplicate/missing
검증, Hikari factory, provider registry/disposer wiring을 소유한다. provider가
`DataSource`/Exposed `Database` registration과 close를 소유하고, consumer service는
`databaseFor`와 read-only `configuredTenants`만 사용한다.

**Tech Stack:** Kotlin 2.4, Spring Boot 4.1, Exposed 1.4, HikariCP, H2, Gradle version catalog, `io.github.bluetape4k.exposed:bluetape4k-exposed-tenant-jdbc:2.1.0-20260907.153611-1` (published 개발 버전의 timestamped build).

---

### Task 1: Provider dependency와 configuration adapter 계약 고정

**Files:**
- Modify: `gradle/libs.versions.toml`
- Add: `00-shared/tenant-jdbc-support/build.gradle.kts`
- Add: `00-shared/tenant-jdbc-support/src/main/kotlin/exposed/shared/tenant/jdbc/TenantJdbcSettings.kt`
- Add: `00-shared/tenant-jdbc-support/src/main/kotlin/exposed/shared/tenant/jdbc/TenantJdbcRegistrySupport.kt`
- Add: `00-shared/tenant-jdbc-support/src/test/kotlin/exposed/shared/tenant/jdbc/TenantJdbcSupportTest.kt`
- Modify: `10-multi-tenant/05-database-per-tenant-spring-web/build.gradle.kts`
- Modify: `10-multi-tenant/06-spring-security-tenant-authorization-spring-web/build.gradle.kts`
- Modify: 각 모듈 `src/main/kotlin/.../config/DatabaseConfiguration.kt`
- Delete: 각 모듈 `src/main/kotlin/.../tenant/TenantDatabaseRegistry.kt`

- [x] 두 모듈 build script에 catalog alias를 추가하고 `implementation`으로 선언한다. alias version은 published 개발 버전의 exact timestamped build `2.1.0-20260907.153611-1`로 고정한다.
- [x] 각 `DatabaseConfiguration`의 registry bean 반환형을 `TenantJdbcResourceRegistry<TenantId>`로 바꾸고 다음 형태의 factory를 사용한다.

```kotlin
val configured = properties.tenants.mapKeys { (key, _) -> TenantId.fromHeader(key) }
val missing = TenantId.entries.filterNot(configured::containsKey)
require(missing.isEmpty()) { "Missing tenant datasource configuration: ..." }
TenantJdbcResourceRegistry.create(
    tenants = TenantId.entries,
    dataSourceFactory = { tenant -> configured.getValue(tenant).toHikariDataSource("tenant-${tenant.headerValue}") },
    disposeDataSource = { _, dataSource -> dataSource.close() },
)
```

- [x] Spring bean `destroyMethod = "close"`를 유지하고 provider API 외의 local entry map/close helper를 삭제한다.

### Task 2: RED — consumer 호출부와 lifecycle 계약을 provider API로 갱신

**Files:**
- Modify: 두 모듈 `TenantTransaction.kt`, `InventorySeeder.kt`
- Modify: 두 모듈 통합 테스트

- [x] `configuredTenants()` 호출을 `configuredTenants` property로 바꾸고 `TenantJdbcResourceRegistry<TenantId>` import를 사용한다.
- [x] registry builder 테스트가 provider return type으로 컴파일되지 않는 RED 상태를 확인한다.
- [x] missing/unknown/H2 validation 테스트의 오류 assertion을 현재 consumer/provider 계약에 맞춰 명시한다.
- [x] standalone close 테스트에 두 번째 `close()` 호출을 추가하고 Hikari pool close 및 provider의 transaction-manager unregister 계약을 검증한다.

### Review follow-up: shared non-Spring support와 duplicate fail-fast

**Files:**
- Add: `00-shared/tenant-jdbc-support/README.md`, `README.ko.md`
- Modify: `00-shared/README.md`, `README.ko.md`
- Modify: 두 consumer `build.gradle.kts`
- Modify: 두 consumer `DatabaseConfiguration.kt`
- Modify: 두 consumer 통합 테스트

- [x] `TenantJdbcSettings`, `validate`, Hikari factory를 Spring 비의존 shared module로 추출한다.
- [x] generic `normalizeTenantJdbcSettings`와 `toTenantJdbcResourceRegistry`가 normalized duplicate/missing을 fail-fast하고 provider factory/disposer를 단일 wiring으로 수행하게 한다.
- [x] `acme`와 ` ACME ` duplicate 설정을 shared helper와 두 consumer contract test에서 `IllegalArgumentException`으로 고정한다.
- [x] 각 consumer에는 `@ConfigurationProperties` wrapper와 tenant parser만 남기고, exact provider dependency를 shared `api`와 consumer catalog alias로 유지한다.
- [x] shared module README 및 00-shared index, 두 consumer README locale을 실제 ownership/lifecycle 구조에 맞춰 동기화한다.

### Task 3: GREEN — provider wiring과 테스트를 최소 구현으로 통과

**Files:**
- Modify: Task 1–2 파일
- Modify: 두 README locale

- [x] Task 2 RED를 통과시키는 최소 adapter/import 변경만 적용한다. authentication, tenant parsing, repository 동작은 변경하지 않는다.
- [x] Review follow-up RED/GREEN에서 shared helper와 consumer adapter의 책임을 분리하고, 기존 authentication/context/transaction 동작은 변경하지 않는다.
- [x] README English/Korean에 provider ownership, explicit `transaction(databaseFor(tenant))`, shutdown/drain 경계를 동일한 구조로 기록한다.
- [x] 변경된 Kotlin 테스트가 bluetape4k assertion idiom과 descriptive name을 사용하도록 확인한다.

### Task 4: 검증·정리·commit

- [x] 순서대로 실행한다.

```bash
./gradlew :05-database-per-tenant-spring-web:test --tests '*DatabasePerTenantApplicationTest' --no-build-cache
./gradlew :06-spring-security-tenant-authorization-spring-web:test --tests '*TenantSecurityApplicationTest' --no-build-cache
./gradlew :05-database-per-tenant-spring-web:dependencies --configuration runtimeClasspath
./gradlew :06-spring-security-tenant-authorization-spring-web:dependencies --configuration runtimeClasspath
./gradlew :05-database-per-tenant-spring-web:compileKotlin :06-spring-security-tenant-authorization-spring-web:compileKotlin
git diff --check
```

- [x] dependency output에서 `bluetape4k-exposed-tenant-jdbc:2.1.0-20260907.153611-1`과 불필요한 Spring/runtime leakage가 없는지 확인한다.
- [x] shared module dependency insight에서 exact tenant JDBC provider와 Exposed BOM resolution을 확인하고 Spring starter가 shared production classpath로 새지 않는지 확인한다.
- [x] `rg 'class TenantDatabaseRegistry|class TenantDatabaseEntry|MessageDigest'`로 중복 구현이 남지 않았음을 확인한다.
- [x] 설계/계획 SPW-01..05, Kotlin KT-FIN-01..11, workflow CG-01..10 증거를 기록하고 Lore commit protocol로 한국어 commit을 만든다.

## 실행 결과

- RED: provider import와 adapter가 없는 상태에서 `compileTestKotlin`이
  `toTenantJdbcResourceRegistry` unresolved reference로 실패했다.
- GREEN: 두 모듈 `compileKotlin`과 `compileTestKotlin`이 통과했다.
- 테스트: shared support 4개, 05 모듈 15개, 06 모듈 33개가 통과했다.
- 정적 검증: shared support와 두 consumer `detekt`, `git diff --check`, duplicate
  registry/property scan이 통과했다.
- 의존성: 두 runtime graph가 exact
  `io.github.bluetape4k.exposed:bluetape4k-exposed-tenant-jdbc:2.1.0-20260907.153611-1`
  을 선택했다. Exposed BOM은 `1.5.0`, consumer의 `exposed-jdbc`는 기존
  compatibility constraint에 따라 `1.4.0`으로 resolve됐다.
- 문서: English/Korean README lifecycle 문구와 개발 버전 dependency 설명을
  동기화했고, Korean terminology audit은 5개 파일에서 finding 0으로 통과했다.
