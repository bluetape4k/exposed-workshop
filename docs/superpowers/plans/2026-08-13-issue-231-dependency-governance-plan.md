# Issue #231 중앙 버전 권위 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `test-driven-development` for any executable regression added and `verification-before-completion` before commit/PR. Steps use checkbox syntax for tracking.

**Goal:** `bluetape4k-dependencies:1.4.0` release catalog와 충돌하는 workshop-local direct pins를 제거하고, resolved graph에서 중앙 버전을 선택하게 한다.

**Architecture:** root BOM import는 유지하고 `gradle/libs.versions.toml`의 shared version keys만 release authority로 조정한다. 소비 모듈은 선언 변경 없이 dependencyInsight와 기존 테스트로 compatibility를 확인한다.

**Tech Stack:** Gradle 9.6 wrapper, version catalog, Spring dependency-management, Bluetape4k dependency BOM 1.4.0, Kotlin/JVM modules.

---

### Task 1: freeze release/current evidence

**Files:**
- Create: `docs/superpowers/research/2026-08-13-issue-231-dependency-authority.md`

- [x] Record the official release tag URL, current catalog lines, and the full direct-pin version pairs.
- [x] Record baseline commands and selected outputs for representative consumers of Ktor, Caffeine, Fory, Jackson 2/3, HikariCP, PostgreSQL JDBC, Netty, Redisson, Vert.x, Hibernate, Micrometer, and cache/test libraries.
- [x] Record unchanged exceptions (`mariadb-java-client`, `r2dbc-postgresql`, v1 H2, `hibernate-validator`, `springmockk`, `jackson-annotations`, `r2dbc-pool`, no direct `fory` key) with their evidence.

### Task 2: add a red catalog-governance check

**Files:**
- Create: `gradle/dependency-governance.sh`

- [x] Add a small read-only shell check that parses every aligned key/value pair from `gradle/libs.versions.toml` and exits non-zero when a value drifts below the release authority.
- [x] The script must print only key, expected, actual, and a remediation hint; it must not edit files or invoke network access.
- [x] Guard the imported `bluetape4k-dependencies` version as `1.4.0` so a future BOM change cannot silently pass the fixed authority snapshot.
- [x] Run it before catalog edits and record the expected failures for every stale key.

### Task 3: align the catalog values

**Files:**
- Modify: `gradle/libs.versions.toml:12,29,33,40,47,49,51,53,78`

- [x] Change only the aligned keys listed in the design spec; keep documented compatibility exceptions unchanged.
- [x] Keep `bluetape4k-dependencies = "1.4.0"` unchanged and do not add versions to versionless Bluetape aliases.
- [x] Run `./gradlew projects` and the governance script to prove catalog parsing and expected values.

### Task 4: prove resolved dependency convergence

- [x] Run the following sequentially and capture selected versions:

```bash
./gradlew :05-ktor-exposed-integration:dependencyInsight --dependency io.ktor:ktor-server-core --configuration runtimeClasspath
./gradlew :01-cache-strategies:dependencyInsight --dependency com.github.ben-manes.caffeine:caffeine --configuration runtimeClasspath
./gradlew :06-spring-cache:dependencyInsight --dependency org.apache.fory:fory-kotlin --configuration runtimeClasspath
./gradlew :08-exposed-jackson:dependencyInsight --dependency com.fasterxml.jackson.core:jackson-databind --configuration testRuntimeClasspath
./gradlew :11-exposed-jackson3:dependencyInsight --dependency tools.jackson.core:jackson-databind --configuration testRuntimeClasspath
./gradlew :01-connection:dependencyInsight --dependency com.zaxxer:HikariCP --configuration testRuntimeClasspath
./gradlew :01-dml:dependencyInsight --dependency org.postgresql:postgresql --configuration testRuntimeClasspath
./gradlew :vertx-sqlclient-example:dependencyInsight --dependency io.vertx:vertx-core --configuration testRuntimeClasspath
./gradlew :hibernate-reactive-example:dependencyInsight --dependency org.hibernate.reactive:hibernate-reactive-core --configuration runtimeClasspath
./gradlew :01-cache-strategies:dependencyInsight --dependency io.micrometer:micrometer-core --configuration runtimeClasspath
```

- [x] Confirm no representative output contains the old downgrade arrows.
- [x] Run Netty and Redisson insight on an actually consuming configuration; if
  a module has no matching dependency, record N/A rather than manufacturing a
  result.

### Task 5: run compatibility tests

- [x] Sequentially run the representative Ktor, cache/Fory, Jackson 2/3,
  connection, Vert.x, Hibernate Reactive, Modulith, and benchmark tests.
- [x] Run `./gradlew compileKotlin detekt` or the repository’s equivalent aggregate
  tasks and classify any pre-existing failure with raw evidence.
- [x] Run `git diff --check` and inspect the final catalog diff for unrelated keys.

### Task 6: document the governance rule and close the lane

**Files:**
- Modify: `README.md`
- Modify: `README.ko.md`
- Create: `docs/lessons/2026-08-13-issue-231-dependency-authority.md`

- [x] Add source-equivalent English/Korean catalog authority guidance under the
  Tech Stack section without changing diagrams.
- [x] Write the Korean lesson with context, decision, verification, surprise,
  and a future guard referencing the governance script.
- [x] Read back all artifacts and record `SPW-01..05`, `KT-FIN-01..11` (N/A for
  production Kotlin), and P0/P1 review results.
- [ ] Commit with a Korean Lore message containing all required trailers.
