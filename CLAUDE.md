# CLAUDE.md — exposed-workshop

Kotlin Exposed ORM 워크샵 — Kotlin 2.3 / Java 21 / Spring Boot 3.x / Gradle multi-module.

## Build & Test

```bash
./gradlew clean build
./gradlew :05-exposed-dml:01-dml:test
./gradlew detekt
repo-status
repo-diff
repo-test-summary -- ./gradlew :MODULE:test
```

모듈 경로는 `settings.gradle.kts` 의 leaf 디렉토리 이름을 직접 사용:
- `:04-exposed-repository:test`
- `:01-multitenant-spring-web:test`

## Module Structure

| Module | Description |
|---|---|
| `00-shared/exposed-shared-tests` | Shared test utilities |
| `01-spring-boot` | Spring MVC + WebFlux examples |
| `02-alternatives-to-jpa` | R2DBC, Vert.x, Hibernate Reactive |
| `03-exposed-basic` | Basic DSL / DAO patterns |
| `04-exposed-ddl` | Connection setup, schema definition |
| `05-exposed-dml` | SELECT / INSERT / UPDATE / DELETE, transactions |
| `06-advanced` | JSON, encryption, custom types, money |
| `07-jpa` | JPA to Exposed migration |
| `08-coroutines` | Coroutines, virtual threads |
| `09-spring` | Spring transactions, cache, repository |
| `10-multi-tenant` | Schema-based multi-tenancy |
| `11-high-performance` | Cache strategies, routing datasource, benchmarks |

## Exposed 패턴

- DSL: `object Table` + `transaction { }`
- DAO: `Entity` / `EntityClass`
- Coroutines: `newSuspendedTransaction { }`, `suspendedTransactionAsync { }`
- Import: `org.jetbrains.exposed.v1.*`

## 테스트 인프라 (`00-shared/exposed-shared-tests`)

- `TestDB` enum: H2, PostgreSQL, MySQL V8, MariaDB
- `USE_FAST_DB=true` → H2 only (빠른 개발 반복)
- `AbstractExposedTest` + `enableDialects()` → 대상 DB 선택
- `WithTables` / `WithTablesSuspending` — 테이블 라이프사이클 관리
- 병렬 테스트: `maxParallelUsages = 1` 직렬 실행 보장

## 의존성 관리

BOM: `bluetape4k_bom`, `exposed_bom`, `kotlinx_coroutines_bom`, `spring_boot_dependencies`
버전: `buildSrc/src/main/kotlin/Libs.kt`

## 컴파일러 옵션

opt-in: `ExperimentalCoroutinesApi`, `FlowPreview`, `DelicateCoroutinesApi`
추가: `-Xcontext-parameters`, `--enable-preview`
