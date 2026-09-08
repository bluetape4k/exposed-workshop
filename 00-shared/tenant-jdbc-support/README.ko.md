# Tenant JDBC Support

[English](./README.md) | 한국어

`tenant-jdbc-support`는 database-per-tenant Spring MVC 예제가 함께 사용하는
Spring 비의존 지원 모듈입니다. 각 consumer가 Spring
`@ConfigurationProperties` binding을 소유하되, 동일해야 하는 순수 JDBC 설정과
provider wiring은 한 곳에서 관리합니다.

## 책임

- `TenantJdbcSettings`가 JDBC/H2 lifecycle 옵션을 검증하고 설정된
  `HikariDataSource`를 만듭니다.
- `normalizeTenantJdbcSettings`가 consumer tenant key를 도메인 key로 매핑하고,
  정규화 후 중복과 누락된 expected tenant를 거부합니다.
- `toTenantJdbcResourceRegistry`가 설정을 검증하고 정규화된 map을 배포된
  `TenantJdbcResourceRegistry` factory와 disposer에 연결합니다.

이 helper는 Spring configuration 모듈이 아닙니다. consumer는
`@ConfigurationProperties` wrapper와 tenant parser를 소유하고, factory가
반환한 뒤의 `DataSource`와 Exposed `Database`는 provider가 소유합니다.

## 의존성

provider API는 워크샵 catalog가 선택한 변경 가능한 개발 build를 exact version으로
고정합니다.

```kotlin
api(libs.exposed.tenant.jdbc.snapshot)
```

version directory는 `2.1.0-SNAPSHOT`으로 유지되며, catalog는 Sonatype
metadata를 통해 배포된 `2.1.0-20260907.153611-1` build를 선택합니다.

## 예제

```kotlin
@ConfigurationProperties(prefix = "app")
data class TenantDataSourceProperties(
    val tenants: Map<String, TenantJdbcSettings> = emptyMap(),
)

val registry = properties.tenants.toTenantJdbcResourceRegistry(
    parseTenant = TenantId::fromHeader,
    expectedTenants = TenantId.entries,
    tenantName = TenantId::headerValue,
)
```

`acme`와 ` ACME `처럼 key가 같은 tenant로 정규화되면 registry를 만들기 전에
실패합니다. expected tenant가 하나라도 빠져도 동일하게 실패합니다.
`close()`는 provider registry의 idempotent lifecycle 경계이며, consumer는
Spring이 bean을 destroy하기 전에 진행 중인 요청을 drain해야 합니다.

## 테스트

```bash
./gradlew :tenant-jdbc-support:test --rerun-tasks --no-build-cache --no-daemon
```

테스트는 설정 검증, Hikari 기본값, 정규화된 중복 key, tenant 누락, provider가
소유한 datasource 정리를 검증합니다.
