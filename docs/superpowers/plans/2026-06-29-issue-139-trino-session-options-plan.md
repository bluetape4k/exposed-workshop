# Issue #139 Trino Session Options Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans or inline execution with TDD. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a credential-free `02-trino-session-options` workshop module that teaches typed Trino JDBC/session options and local pushdown request-shape verification.

**Architecture:** The module exposes small Kotlin helpers for a validated Trino analytical profile, conversion to `TrinoConnectionOptions`, Exposed SQL generation, and `EXPLAIN` request creation. Tests run on H2 for SQL generation and inspect public typed options plus generated request strings.

**Tech Stack:** Kotlin 2.4, Exposed 1.3.0, `bluetape4k-exposed-trino`, H2, JUnit 5, bluetape4k assertions, CairoSVG for README diagram rendering.

---

## Task 1: Register dependency and module scaffold

**Files:**
- Modify: `gradle/libs.versions.toml`
- Create: `13-ecosystem-integrations/02-trino-session-options/build.gradle.kts`
- Create: `13-ecosystem-integrations/02-trino-session-options/src/test/resources/junit-platform.properties`
- Create: `13-ecosystem-integrations/02-trino-session-options/src/test/resources/logback-test.xml`

- [ ] Add `exposed-trino = { module = "io.github.bluetape4k.exposed:bluetape4k-exposed-trino" }` near other Exposed aliases.
- [ ] Create the module build file with `implementation(libs.exposed.trino)`, `implementation(libs.jetbrains.exposed.core)`, `implementation(libs.jetbrains.exposed.jdbc)`, `runtimeOnly(libs.h2.v2)`, and `testImplementation(libs.bluetape4k.junit5)`.
- [ ] Add standard JUnit/logback test resources matching `01-bigquery-dry-run`.
- [ ] Verify `./gradlew projects --quiet | grep ':02-trino-session-options'`.

## Task 2: RED tests for typed Trino options and request shape

**Files:**
- Create: `13-ecosystem-integrations/02-trino-session-options/src/test/kotlin/exposed/examples/trino/options/TrinoSessionOptionsWorkshopTest.kt`

- [ ] Write failing tests for:
  - default analytical profile maps to `TrinoConnectionOptions` fields.
  - profile preview contains stable Trino JDBC property names and values.
  - unsafe blank catalog/schema/source/tag/session property values are rejected.
  - generated analytical SQL and `EXPLAIN` request preserve predicate, projection, order, and top-N shape.
- [ ] Run `./gradlew :02-trino-session-options:test --no-daemon` and confirm it fails because production symbols are unresolved.

## Task 3: Implement minimal workshop helpers

**Files:**
- Create: `13-ecosystem-integrations/02-trino-session-options/src/main/kotlin/exposed/examples/trino/options/TrinoSessionOptionsWorkshop.kt`

- [ ] Add `TrinoWorkshopConnectionProfile` with validation and `toConnectionOptions()`.
- [ ] Add `jdbcPropertyPreview(user: String)` for stable local assertions and README inspection.
- [ ] Add `WarehouseOrders` Exposed table and `buildRegionalTopOrdersQuery(minimumRevenue)`.
- [ ] Add `generateRegionalTopOrdersSql()` using an H2 SQL-generation transaction and `prepareSQL(prepared = false)`.
- [ ] Add `buildExplainRequest(sql)` that wraps generated SQL in an `EXPLAIN` statement.
- [ ] Add English KDoc for public functions/classes.
- [ ] Run `./gradlew :02-trino-session-options:test --no-daemon` and confirm green.

## Task 4: README and diagram

**Files:**
- Create: `13-ecosystem-integrations/02-trino-session-options/README.md`
- Create: `13-ecosystem-integrations/02-trino-session-options/README.ko.md`
- Create: `docs/images/readme-diagrams/13-trino-session-options-sequence-01.svg`
- Create: `docs/images/readme-diagrams/13-trino-session-options-sequence-01.png`
- Modify: `13-ecosystem-integrations/README.md`
- Modify: `13-ecosystem-integrations/README.ko.md`
- Modify: `README.md`
- Modify: `README.ko.md`

- [ ] Write English/Korean README pair with language switch.
- [ ] State local-only validation versus real Trino connector validation.
- [ ] Embed the PNG diagram and keep diagram labels English.
- [ ] Draw a best-practices-style sequence diagram: application profile, typed options, Exposed SQL generation, `EXPLAIN` request, and real Trino as opt-in.
- [ ] Validate SVG XML, geometry/endpoint audits where applicable, CairoSVG render, and full-size PNG inspection.
- [ ] Mark #139 Ready in chapter README pair and link the module from root README pair.

## Task 5: CI registration and verification

**Files:**
- Modify: `.github/workflows/examples.yml`
- Create: `docs/review/2026-06-29-issue-139-trino-session-options-code-review.md`
- Create: `docs/lessons/2026-06-29-issue-139-trino-session-options.md`

- [ ] Add `:02-trino-session-options:build` to the selected Examples workflow.
- [ ] Run `actionlint .github/workflows/examples.yml`.
- [ ] Run `./gradlew :02-trino-session-options:test --no-daemon`.
- [ ] Run `./gradlew :02-trino-session-options:build --no-daemon`.
- [ ] Run `./gradlew projects --quiet`.
- [ ] Run `git diff --check`.
- [ ] Record 7-tier current-session review with P0=0/P1=0.
- [ ] Commit with Lore trailers, push, create PR closing #139 and referencing #137.
