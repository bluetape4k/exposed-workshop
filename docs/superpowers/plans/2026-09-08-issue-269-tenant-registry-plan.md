# Issue 269 Tenant JDBC Registry 전환 Implementation Plan

> **For agentic workers:** 승인된 설계를 기준으로 아래 작업을 순서대로 실행한다. 각 단계는 RED/GREEN 증거와 정확한 파일 범위를 남긴다.

**Goal:** 두 Spring MVC multi-tenant 예제가 published `TenantJdbcResourceRegistry`를 사용하고 중복 registry/entry lifecycle 구현을 제거한다.

**Architecture:** Spring configuration adapter가 `TenantId.entries`와 검증된 Hikari factory를 provider registry에 전달한다. provider가 `DataSource`/Exposed `Database` registration과 close를 소유하고, consumer service는 `databaseFor`와 read-only `configuredTenants`만 사용한다.

**Tech Stack:** Kotlin 2.4, Spring Boot 4.1, Exposed 1.4, HikariCP, H2, Gradle version catalog, `io.github.bluetape4k.exposed:bluetape4k-exposed-tenant-jdbc:2.1.0-SNAPSHOT`.

---

### Task 1: Provider dependency와 configuration adapter 계약 고정

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `10-multi-tenant/05-database-per-tenant-spring-web/build.gradle.kts`
- Modify: `10-multi-tenant/06-spring-security-tenant-authorization-spring-web/build.gradle.kts`
- Modify: 각 모듈 `src/main/kotlin/.../config/DatabaseConfiguration.kt`
- Delete: 각 모듈 `src/main/kotlin/.../tenant/TenantDatabaseRegistry.kt`

- [ ] 두 모듈 build script에 catalog alias `libs.exposed.tenant.jdbc.snapshot`을 추가하고 `implementation`으로 선언한다. alias version은 published `2.1.0-SNAPSHOT`으로 고정한다.
- [ ] 각 `DatabaseConfiguration`의 registry bean 반환형을 `TenantJdbcResourceRegistry<TenantId>`로 바꾸고 다음 형태의 factory를 사용한다.

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

- [ ] Spring bean `destroyMethod = "close"`를 유지하고 provider API 외의 local entry map/close helper를 삭제한다.

### Task 2: RED — consumer 호출부와 lifecycle 계약을 provider API로 갱신

**Files:**
- Modify: 두 모듈 `TenantTransaction.kt`, `InventorySeeder.kt`
- Modify: 두 모듈 통합 테스트

- [ ] `configuredTenants()` 호출을 `configuredTenants` property로 바꾸고 `TenantJdbcResourceRegistry<TenantId>` import를 사용한다.
- [ ] registry builder 테스트가 provider return type으로 컴파일되지 않는 RED 상태를 확인한다.
- [ ] missing/unknown/H2 validation 테스트의 오류 assertion을 현재 consumer/provider 계약에 맞춰 명시한다.
- [ ] standalone close 테스트에 두 번째 `close()` 호출을 추가하고 Hikari pool close 및 transaction-manager unregister를 검증한다.

### Task 3: GREEN — provider wiring과 테스트를 최소 구현으로 통과

**Files:**
- Modify: Task 1–2 파일
- Modify: 두 README locale

- [ ] Task 2 RED를 통과시키는 최소 adapter/import 변경만 적용한다. authentication, tenant parsing, repository 동작은 변경하지 않는다.
- [ ] README English/Korean에 provider ownership, explicit `transaction(databaseFor(tenant))`, shutdown/drain 경계를 동일한 구조로 기록한다.
- [ ] 변경된 Kotlin 테스트가 bluetape4k assertion idiom과 descriptive name을 사용하도록 확인한다.

### Task 4: 검증·정리·commit

- [ ] 순서대로 실행한다.

```bash
./gradlew :05-database-per-tenant-spring-web:test --tests '*DatabasePerTenantApplicationTest' --no-build-cache
./gradlew :06-spring-security-tenant-authorization-spring-web:test --tests '*TenantSecurityApplicationTest' --no-build-cache
./gradlew :05-database-per-tenant-spring-web:dependencies --configuration runtimeClasspath
./gradlew :06-spring-security-tenant-authorization-spring-web:dependencies --configuration runtimeClasspath
./gradlew :05-database-per-tenant-spring-web:compileKotlin :06-spring-security-tenant-authorization-spring-web:compileKotlin
git diff --check
```

- [ ] dependency output에서 `bluetape4k-exposed-tenant-jdbc:2.1.0-SNAPSHOT`과 불필요한 Spring/runtime leakage가 없는지 확인한다.
- [ ] `rg 'class TenantDatabaseRegistry|class TenantDatabaseEntry|MessageDigest'`로 중복 구현이 남지 않았음을 확인한다.
- [ ] 설계/계획 SPW-01..05, Kotlin KT-FIN-01..11, workflow CG-01..10 증거를 기록하고 Lore commit protocol로 한국어 commit을 만든다.
