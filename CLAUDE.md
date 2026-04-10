# CLAUDE.md

Kotlin Exposed workshop — Kotlin 2.3 / Java 21 / Spring Boot 3.x / Gradle multi-module project.

## Build & Test

```bash
./gradlew clean build
./gradlew :05-exposed-dml:01-dml:test
./gradlew detekt
./bin/repo-status
./bin/repo-diff
./bin/repo-test-summary -- ./gradlew :MODULE:test
```

Module paths use the last directory name as the Gradle project path.
Examples:
- `:04-exposed-repository:test`
- `:01-multitenant-spring-web:test`

`settings.gradle.kts` maps each leaf module directory to the project path, so use the leaf module name directly rather than the full section path.

Recommended low-token workflow:
`repo-status` → `repo-diff` → inspect only relevant files → `repo-test-summary`

## Module Structure

| Module | Description |
|---|---|
| `00-shared/exposed-shared-tests` | Shared test utilities used by all modules |
| `01-spring-boot` | Spring MVC + WebFlux examples |
| `02-alternatives-to-jpa` | R2DBC, Vert.x, Hibernate Reactive |
| `03-exposed-basic` | Basic DSL / DAO patterns |
| `04-exposed-ddl` | Connection setup and schema definition |
| `05-exposed-dml` | SELECT / INSERT / UPDATE / DELETE, transactions |
| `06-advanced` | JSON, encryption, custom types, money |
| `07-jpa` | JPA to Exposed migration |
| `08-coroutines` | Coroutines, virtual threads |
| `09-spring` | Spring transactions, cache, repository |
| `10-multi-tenant` | Schema-based multi-tenancy |
| `11-high-performance` | Cache strategies, routing datasource, benchmarks |

## Architecture Highlights

### Dependency Management
- BOMs: `bluetape4k_bom`, `exposed_bom`, `kotlinx_coroutines_bom`, `spring_boot_dependencies`
- Version catalog source: `buildSrc/src/main/kotlin/Libs.kt`
- Parallel test execution is disabled with `maxParallelUsages = 1`

### Exposed Patterns
- DSL: `object Table` with `transaction { }`
- DAO: `Entity` / `EntityClass`
- Coroutines: `newSuspendedTransaction { }`, `suspendedTransactionAsync { }`
- Imports: `org.jetbrains.exposed.v1.*`

### Test Infrastructure
`00-shared/exposed-shared-tests` provides the common DB testing setup.

- `TestDB` enum: H2, PostgreSQL, MySQL V8, MariaDB
- Default DB matrix is enabled; `USE_FAST_DB=true` reduces it to H2 only
- `AbstractExposedTest` + `enableDialects()` choose target databases
- `WithTables` / `WithTablesSuspending` manage table lifecycle
- For Exposed-related DB tests, prefer `withTables` from `bluetape4k-exposed-jdbc-tests` or `bluetape4k-exposed-r2dbc-tests`
- For benchmarks, use real database connections with `HikariCP` or `r2dbc-pool` instead of test helpers

## Coding Rules

- Kotlin only; do not introduce Java
- Prefer extension functions when they improve clarity
- Use 4 spaces for indentation
- Package names: lowercase dot-separated
- Types: `PascalCase`
- Functions / variables: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Public APIs should use Korean KDoc
- Logging should use a `KLogging` companion object

## Commit Style

Use Korean commit messages with Conventional Commit prefixes:
- `feat:`
- `fix:`
- `refactor:`
- `build:`
- `docs:`
- `chore:`
- `test:`
- `perf:`

## Key Libraries

| Library | Purpose |
|---|---|
| `exposed-core/jdbc/dao/java-time` | Exposed core |
| `bluetape4k-exposed` | Exposed extension utilities |
| `bluetape4k-junit5`, `bluetape4k-testcontainers` | Test support |
| `kluent` | Kotlin assertions |
| `mockk` | Kotlin mocking |
| `datafaker`, `random-beans` | Test data generation |
| `kotlinx-atomicfu` | Atomic operations / compiler plugin |

## Compiler Options

Global opt-ins:
- `ExperimentalCoroutinesApi`
- `FlowPreview`
- `DelicateCoroutinesApi`

Additional compiler options:
- `-Xcontext-parameters`
- `--enable-preview`
