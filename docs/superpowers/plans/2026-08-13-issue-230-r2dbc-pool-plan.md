# Issue #230 R2DBC 커넥션 풀 헬퍼 통합 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `test-driven-development` and `verification-before-completion` to execute each task. Steps use checkbox syntax for tracking.

**Goal:** Ktor Exposed 예제의 수동 R2DBC pool builder를 `bluetape4k-r2dbc:1.12.1` helper로 교체하면서 caller-owned lifecycle과 문서 계약을 보존한다.

**Architecture:** 같은 `ConnectionFactoryOptions`를 `connectionFactoryOptionsOf(url)`로 한 번 만들고 `connectionPoolOf(options)`에 전달한다. `KtorExposedIntegrationResources`가 pool을 계속 소유·정리하고 route/helper 계층은 변경하지 않는다.

**Tech Stack:** Kotlin 2.4, Ktor 3.5.x, Exposed 1.4.0, R2DBC H2, `bluetape4k-r2dbc` 1.12.1, JUnit 5, bluetape4k assertions.

---

### Task 1: versionless R2DBC dependency boundary

**Files:**
- Modify: `gradle/libs.versions.toml:116-135`
- Modify: `13-ecosystem-integrations/05-ktor-exposed-integration/build.gradle.kts:20-45`

- [ ] Add `bluetape4k-r2dbc = { module = "io.github.bluetape4k:bluetape4k-r2dbc" }` beside the other core aliases.
- [ ] Add `implementation(libs.bluetape4k.r2dbc)` to the Ktor Exposed module.
- [ ] Run `./gradlew :05-ktor-exposed-integration:dependencies --configuration runtimeClasspath` and confirm `io.github.bluetape4k:bluetape4k-r2dbc:1.12.1` is selected by the `bluetape4k-dependencies:1.4.0` BOM.

### Task 2: add the failing helper-contract test

**Files:**
- Modify: `13-ecosystem-integrations/05-ktor-exposed-integration/src/test/kotlin/exposed/examples/ktor/exposedintegration/KtorExposedIntegrationApplicationTest.kt`

- [ ] Add one test that creates `KtorExposedIntegrationResources.create("pool-helper")`, calls the existing readiness route, and asserts `r2dbc` is `HealthResponse.UP`.
- [ ] In the same test, call `resources.close()` twice after the application block and assert no exception escapes; this locks idempotent caller-owned cleanup without mocking `ConnectionPool` internals.
- [ ] Run `./gradlew :05-ktor-exposed-integration:test --tests '*pool helper*'` before changing production code and record the expected failure caused by the still-missing dependency/helper migration.

### Task 3: replace direct R2DBC builder calls

**Files:**
- Modify: `13-ecosystem-integrations/05-ktor-exposed-integration/src/main/kotlin/exposed/examples/ktor/exposedintegration/KtorExposedIntegrationApplication.kt:25-128`

- [ ] Replace imports with `io.bluetape4k.r2dbc.pool.connectionFactoryOptionsOf` and `io.bluetape4k.r2dbc.pool.connectionPoolOf`.
- [ ] Replace the four-line builder chain with:

```kotlin
val r2dbcOptions = connectionFactoryOptionsOf(r2dbcUrl)
val r2dbcPool = connectionPoolOf(r2dbcOptions) {
    maxSize = 2
    initialSize = 1
}
```

- [ ] Keep `R2dbcDatabaseConfig { connectionFactoryOptions = r2dbcOptions }` unchanged.
- [ ] Keep `close()` cleanup order and `ApplicationStopped` subscription unchanged unless the failing test proves a lifecycle defect; do not add suspend cleanup or catch cancellation.
- [ ] Run the new focused test and the complete module test; both must pass.

### Task 4: synchronize bilingual README

**Files:**
- Modify: `13-ecosystem-integrations/05-ktor-exposed-integration/README.ko.md`
- Modify: `13-ecosystem-integrations/05-ktor-exposed-integration/README.md`

- [ ] Add a short R2DBC pool section showing the helper call and stating that the application owns disposal.
- [ ] Preserve code tokens, route names, diagram path, and existing chapter-12 boundary.
- [ ] Verify section/title/table parity and paired links between the two locale files.

### Task 5: targeted verification and cleanup

- [ ] Run `./gradlew :05-ktor-exposed-integration:test` sequentially with fresh output.
- [ ] Run `./gradlew :05-ktor-exposed-integration:detekt` or the repository’s module-equivalent static-analysis task.
- [ ] Run `git diff --check` and `rg -n 'ConnectionPoolConfiguration|ConnectionFactories' 13-ecosystem-integrations/05-ktor-exposed-integration/src` to prove direct builder removal.
- [ ] Read back both README files and record `SPW-01..05`, `KT-FIN-01..11`, and performance/stability scan evidence.
- [ ] Commit with a Korean Lore message containing `Constraint`, `Rejected`, `Confidence`, `Scope-risk`, `Directive`, `Tested`, and `Not-tested` trailers.

