# Issue #139 Trino session options 구현 계획

> **Agentic worker용:** REQUIRED SUB-SKILL: superpowers:executing-plans 또는 inline TDD 실행을
> 사용한다. 단계 추적에는 checkbox(`- [ ]`) syntax를 사용한다.

**목표:** Typed Trino JDBC/session option과 local pushdown request-shape verification을 가르치는
credential-free `02-trino-session-options` workshop module을 추가한다.

**아키텍처:** Module은 validated Trino analytical profile, `TrinoConnectionOptions` 변환,
Exposed SQL generation, `EXPLAIN` request creation을 위한 작은 Kotlin helper를 노출한다. Test는
SQL generation을 위해 H2에서 실행되고 public typed option 및 generated request string을 검사한다.

**기술 스택:** Kotlin 2.4, Exposed 1.3.0, `bluetape4k-exposed-trino`, H2, JUnit 5,
bluetape4k assertions, README diagram rendering용 CairoSVG.

---

## 작업 1: Dependency 및 module scaffold 등록

**파일:**
- 수정: `gradle/libs.versions.toml`
- 생성: `13-ecosystem-integrations/02-trino-session-options/build.gradle.kts`
- 생성: `13-ecosystem-integrations/02-trino-session-options/src/test/resources/junit-platform.properties`
- 생성: `13-ecosystem-integrations/02-trino-session-options/src/test/resources/logback-test.xml`

- [ ] 추가: `exposed-trino = { module = "io.github.bluetape4k.exposed:bluetape4k-exposed-trino" }`를 다른 Exposed alias 근처에 둔다.
- [ ] 생성: `implementation(libs.exposed.trino)`, `implementation(libs.jetbrains.exposed.core)`,
  `implementation(libs.jetbrains.exposed.jdbc)`, `runtimeOnly(libs.h2.v2)`,
  `testImplementation(libs.bluetape4k.junit5)`를 사용하는 module build file.
- [ ] 추가: `01-bigquery-dry-run`과 맞는 standard JUnit/logback test resource.
- [ ] 검증: `./gradlew projects --quiet | grep ':02-trino-session-options'`.

## 작업 2: Typed Trino option 및 request shape용 RED test

**파일:**
- 생성: `13-ecosystem-integrations/02-trino-session-options/src/test/kotlin/exposed/examples/trino/options/TrinoSessionOptionsWorkshopTest.kt`

- [ ] 작성: 다음을 기대하는 failing test.
  - Default analytical profile이 `TrinoConnectionOptions` field로 mapping된다.
  - Profile preview가 stable Trino JDBC property name/value를 포함한다.
  - Unsafe blank catalog/schema/source/tag/session property value는 거부된다.
  - Generated analytical SQL과 `EXPLAIN` request는 predicate, projection, order, top-N shape를
    보존한다.
- [ ] 실행: `./gradlew :02-trino-session-options:test --no-daemon`을 실행하고 production symbol이
  unresolved라 실패하는지 확인한다.

## 작업 3: 최소 workshop helper 구현

**파일:**
- 생성: `13-ecosystem-integrations/02-trino-session-options/src/main/kotlin/exposed/examples/trino/options/TrinoSessionOptionsWorkshop.kt`

- [ ] 추가: validation과 `toConnectionOptions()`가 있는 `TrinoWorkshopConnectionProfile`.
- [ ] 추가: stable local assertion 및 README inspection용 `jdbcPropertyPreview(user: String)`.
- [ ] 추가: `WarehouseOrders` Exposed table 및 `buildRegionalTopOrdersQuery(minimumRevenue)`.
- [ ] 추가: H2 SQL-generation transaction과 `prepareSQL(prepared = false)`를 사용하는
  `generateRegionalTopOrdersSql()`.
- [ ] 추가: Generated SQL을 `EXPLAIN` statement로 감싸는 `buildExplainRequest(sql)`.
- [ ] 추가: Public function/class용 한국어 KDoc.
- [ ] 실행: `./gradlew :02-trino-session-options:test --no-daemon`을 실행하고 green을 확인한다.

## 작업 4: README와 diagram

**파일:**
- 생성: `13-ecosystem-integrations/02-trino-session-options/README.md`
- 생성: `13-ecosystem-integrations/02-trino-session-options/README.ko.md`
- 생성: `docs/images/readme-diagrams/13-trino-session-options-sequence-01.svg`
- 생성: `docs/images/readme-diagrams/13-trino-session-options-sequence-01.png`
- 수정: `13-ecosystem-integrations/README.md`
- 수정: `13-ecosystem-integrations/README.ko.md`
- 수정: `README.md`
- 수정: `README.ko.md`

- [ ] 작성: Language switch가 있는 English/Korean README pair.
- [ ] 명시: Local-only validation과 real Trino connector validation의 차이.
- [ ] embed: PNG diagram을 넣고 diagram label은 English로 유지한다.
- [ ] 그리기: application profile, typed option, Exposed SQL generation, `EXPLAIN` request, opt-in
  real Trino를 보여 주는 best-practices-style sequence diagram.
- [ ] validate: 적용 가능한 SVG XML, geometry/endpoint audit, CairoSVG render, full-size PNG
  inspection.
- [ ] 표시: Chapter README pair에서 #139를 Ready로 표시하고 root README pair에서 module을 link한다.

## 작업 5: CI 등록과 검증

**파일:**
- 수정: `.github/workflows/examples.yml`
- 생성: `docs/review/2026-06-29-issue-139-trino-session-options-code-review.md`
- 생성: `docs/lessons/2026-06-29-issue-139-trino-session-options.md`

- [ ] 추가: Selected Examples workflow에 `:02-trino-session-options:build`.
- [ ] 실행: `actionlint .github/workflows/examples.yml`.
- [ ] 실행: `./gradlew :02-trino-session-options:test --no-daemon`.
- [ ] 실행: `./gradlew :02-trino-session-options:build --no-daemon`.
- [ ] 실행: `./gradlew projects --quiet`.
- [ ] 실행: `git diff --check`.
- [ ] 기록: P0=0/P1=0인 7-tier current-session review.
- [ ] commit: Lore trailer로 commit하고 push한 뒤 #139를 close하고 #137을 reference하는 PR을 만든다.
