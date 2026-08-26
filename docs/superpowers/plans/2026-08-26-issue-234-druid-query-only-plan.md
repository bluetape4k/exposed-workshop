# Apache Druid Query-Only Exposed 예제 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use inline execution in this session. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 중앙 `bluetape4k-dependencies:1.4.0` catalog alias와 `bluetape4k-exposed:1.12.1` provider API를 사용하는 외부 서비스 없는 Apache Druid query-only workshop 모듈을 추가한다.

**Architecture:** `DruidQueryProfile`이 검증된 연결 설정을 `DruidConnectionOptions`로 변환하고, 얇은 workshop 함수가 `DruidJdbc.query`, `querySuspend`, `listColumns`를 직접 호출한다. 기본 테스트는 MockK로 provider object를 대체하며, 실제 Druid는 `EXPOSED_DRUID_SMOKE=true`인 별도 opt-in 테스트에서만 접근한다.

**Tech Stack:** Kotlin 2.3, Java 25 toolchain, Gradle 9.6, bluetape4k dependency BOM 1.4.0, bluetape4k-exposed Druid provider 1.12.1, JUnit 5, MockK, kotlinx.coroutines, CairoSVG.

---

## 파일 구조

- Modify: `gradle/libs.versions.toml` — BOM 기반 `libs.exposed.druid` alias.
- Create: `13-ecosystem-integrations/10-druid-query-only/build.gradle.kts` — Druid provider와 기존 test convention.
- Create: `13-ecosystem-integrations/10-druid-query-only/src/main/kotlin/exposed/examples/druid/queryonly/DruidQueryOnlyWorkshop.kt` — profile, query-only SQL 생성, sync/suspend/metadata facade.
- Create: `13-ecosystem-integrations/10-druid-query-only/src/test/kotlin/exposed/examples/druid/queryonly/DruidQueryOnlyWorkshopTest.kt` — 외부 서비스 없는 RED→GREEN 회귀 테스트.
- Create: `13-ecosystem-integrations/10-druid-query-only/src/test/kotlin/exposed/examples/druid/queryonly/DruidQueryOnlySmokeTest.kt` — 환경 변수로만 활성화되는 실제 endpoint smoke test.
- Create: `13-ecosystem-integrations/10-druid-query-only/README.md` — English 사용법과 제약.
- Create: `13-ecosystem-integrations/10-druid-query-only/README.ko.md` — Korean source-equivalent 사용법과 제약.
- Modify: `13-ecosystem-integrations/README.md` — chapter 표에 issue #234 행.
- Modify: `13-ecosystem-integrations/README.ko.md` — Korean chapter 표에 issue #234 행.
- Modify: `.github/scripts/select-changed-examples.sh` — `:10-druid-query-only:build` weekly Examples task.
- Create: `docs/images/readme-diagrams/13-druid-query-only-architecture-01.svg` — English architecture source.
- Create: `docs/images/readme-diagrams/13-druid-query-only-architecture-01.png` — English rendered asset.
- Create: `docs/images/readme-diagrams/13-druid-query-only-architecture-01.ko.svg` — Korean architecture source.
- Create: `docs/images/readme-diagrams/13-druid-query-only-architecture-01.ko.png` — Korean rendered asset.
- Create: `docs/images/readme-diagrams/13-druid-query-only-architecture-01.semantic.json` — source-backed semantic ledger per diagram contract.
- Create: `docs/lessons/2026-08-26-issue-234-druid-query-only.md` — Korean lesson and follow-up evidence.
- Create: `docs/review/2026-08-26-issue-234-druid-query-only.md` — six-lens serial review receipt and DoD evidence.

## Task 1: Catalog alias and module discovery

**Files:**
- Modify: `gradle/libs.versions.toml` near the existing `exposed-bigquery` and `exposed-trino` aliases.
- Create: `13-ecosystem-integrations/10-druid-query-only/build.gradle.kts`.

- [ ] **Step 1: Add the BOM-backed alias and minimal build file.**

Add the alias:

```toml
exposed-druid = { module = "io.github.bluetape4k.exposed:bluetape4k-exposed-druid" }
```

Create:

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(libs.exposed.druid)

    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 2: Prove Gradle discovers the new project before adding Kotlin code.**

Run from the linked worktree:

```bash
./gradlew projects --no-daemon --no-configuration-cache
```

Expected: the project list contains `:10-druid-query-only` and dependency resolution uses the root `bluetape4k-dependencies:1.4.0` BOM without a hard-coded Druid version.

- [ ] **Step 3: Commit the catalog/module scaffold.**

```bash
git add gradle/libs.versions.toml 13-ecosystem-integrations/10-druid-query-only/build.gradle.kts
git commit -m "Druid query-only 모듈의 중앙 의존성 경계를 연결한다" -m "BOM alias와 최소 Gradle 모듈로 provider 재사용 경계를 먼저 고정한다.\n\nConstraint: 중앙 bluetape4k-dependencies:1.4.0 catalog를 사용한다.\nRejected: 모듈별 provider 버전 고정 | catalog train과 중복되고 drift 위험이 있다.\nConfidence: high\nScope-risk: narrow\nDirective: Druid provider 의존성은 libs.exposed.druid alias를 통해서만 추가한다.\nTested: ./gradlew projects --no-daemon --no-configuration-cache\nNot-tested: Kotlin source와 module test는 다음 TDD 단계에서 검증한다."
```

## Task 2: RED tests for the query-only contract

**Files:**
- Create: `13-ecosystem-integrations/10-druid-query-only/src/test/kotlin/exposed/examples/druid/queryonly/DruidQueryOnlyWorkshopTest.kt`.

- [ ] **Step 1: Write the failing test suite before production code.**

Create the test file with these exact behaviors and MockK boundaries:

```kotlin
package exposed.examples.druid.queryonly

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.exposed.druid.DruidColumnMetadata
import io.bluetape4k.exposed.druid.DruidJdbc
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DruidQueryOnlyWorkshopTest {

    private val profile = DruidQueryProfile(
        avaticaEndpoint = "http://druid.test:8888/druid/v2/sql/avatica/",
        datasource = "wikipedia",
        schema = "druid",
        contextProperties = mapOf("sqlTimeZone" to "Etc/UTC"),
    )

    @BeforeEach
    fun setUp() {
        mockkObject(DruidJdbc)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(DruidJdbc)
    }

    @Test
    fun `profile maps Druid connection options without opening a connection`() {
        val options = profile.toConnectionOptions()

        options.avaticaEndpoint shouldBeEqualTo profile.avaticaEndpoint
        options.transparentReconnection shouldBeEqualTo true
        options.jdbcUrl() shouldContain "transparent_reconnection=true"
        options.toProperties().getProperty("sqlTimeZone") shouldBeEqualTo "Etc/UTC"
    }

    @Test
    fun `blank profile values fail before JDBC calls`() {
        assertFailsWith<IllegalArgumentException> { DruidQueryProfile(datasource = "") }
        assertFailsWith<IllegalArgumentException> { DruidQueryProfile(schema = " ") }
        assertFailsWith<IllegalArgumentException> {
            DruidQueryProfile(contextProperties = mapOf("" to "Etc/UTC"))
        }
        assertFailsWith<IllegalArgumentException> {
            DruidQueryProfile(avaticaEndpoint = "http://druid.test:8888/wrong/").toConnectionOptions()
        }
    }

    @Test
    fun `sync query delegates query-only SQL and returns mapped rows`() {
        every { DruidJdbc.query<Long>(any(), any(), any()) } returns listOf(7L)

        queryDatasourceRowCount(profile) shouldBeEqualTo listOf(7L)

        verify(exactly = 1) {
            DruidJdbc.query<Long>(
                sql = "SELECT COUNT(*) AS row_count FROM \"wikipedia\"",
                options = profile.toConnectionOptions(),
                mapper = any(),
            )
        }
    }

    @Test
    fun `suspend query delegates the supplied dispatcher`() = runSuspendIO {
        coEvery { DruidJdbc.querySuspend<Long>(any(), any(), any(), any()) } returns listOf(11L)

        queryDatasourceRowCountSuspend(profile, dispatcher = Dispatchers.Unconfined) shouldBeEqualTo listOf(11L)

        coVerify(exactly = 1) {
            DruidJdbc.querySuspend<Long>(
                sql = "SELECT COUNT(*) AS row_count FROM \"wikipedia\"",
                options = profile.toConnectionOptions(),
                dispatcher = Dispatchers.Unconfined,
                mapper = any(),
            )
        }
    }

    @Test
    fun `metadata query preserves datasource schema and options`() {
        val metadata = listOf(
            DruidColumnMetadata(
                tableSchema = "druid",
                tableName = "wikipedia",
                columnName = "country",
                dataType = "VARCHAR",
                ordinalPosition = 1,
                isNullable = "YES",
            ),
        )
        every { DruidJdbc.listColumns(any(), any(), any()) } returns metadata

        listDatasourceColumns(profile) shouldBeEqualTo metadata

        verify(exactly = 1) {
            DruidJdbc.listColumns(
                datasource = "wikipedia",
                schema = "druid",
                options = profile.toConnectionOptions(),
            )
        }
    }

    @Test
    fun `unsafe datasource identifiers fail before provider invocation`() {
        assertFailsWith<IllegalArgumentException> {
            buildDatasourceCountQuery("wikipedia; DROP TABLE users")
        }
    }

    @Test
    fun `provider rejects blank and DML SQL before opening a connection`() {
        unmockkObject(DruidJdbc)

        assertFailsWith<IllegalArgumentException> {
            DruidJdbc.query<Long>("", profile.toConnectionOptions()) { 0L }
        }
        assertFailsWith<IllegalArgumentException> {
            DruidJdbc.query<Long>("INSERT INTO wikipedia VALUES (1)", profile.toConnectionOptions()) { 0L }
        }
    }
}
```

- [ ] **Step 2: Run the new test to prove the intended RED state.**

```bash
./gradlew :10-druid-query-only:test --tests '*DruidQueryOnlyWorkshopTest' --no-daemon --no-configuration-cache
```

Expected: compilation/test failure because `DruidQueryProfile`, `queryDatasourceRowCount`, `queryDatasourceRowCountSuspend`, `listDatasourceColumns`, and `buildDatasourceCountQuery` do not exist yet. A dependency-resolution failure is not an acceptable RED result; fix the catalog/module setup first.

## Task 3: GREEN implementation of the minimal provider facade

**Files:**
- Create: `13-ecosystem-integrations/10-druid-query-only/src/main/kotlin/exposed/examples/druid/queryonly/DruidQueryOnlyWorkshop.kt`.

- [ ] **Step 1: Implement the profile and safe query builder.**

Use this implementation shape and Korean KDoc:

```kotlin
package exposed.examples.druid.queryonly

import io.bluetape4k.exposed.druid.DruidColumnMetadata
import io.bluetape4k.exposed.druid.DruidConnectionOptions
import io.bluetape4k.exposed.druid.DruidJdbc
import io.bluetape4k.exposed.druid.DruidAvaticaSerialization
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.Serializable

private const val DEFAULT_DRUID_AVATICA_ENDPOINT = "http://localhost:8888/druid/v2/sql/avatica/"
private val DRUID_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")

/** Apache Druid query-only 예제에서 사용하는 검증된 연결 profile이다. */
data class DruidQueryProfile(
    val avaticaEndpoint: String = DEFAULT_DRUID_AVATICA_ENDPOINT,
    val datasource: String = "wikipedia",
    val schema: String = "druid",
    val transparentReconnection: Boolean = true,
    val serialization: DruidAvaticaSerialization = DruidAvaticaSerialization.JSON,
    val user: String? = null,
    val password: String? = null,
    val contextProperties: Map<String, String> = mapOf("sqlTimeZone" to "Etc/UTC"),
) : Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }

    init {
        datasource.requireNotBlank("datasource")
        schema.requireNotBlank("schema")
        user?.requireNotBlank("user")
        password?.requireNotBlank("password")
        contextProperties.forEach { (key, value) ->
            key.requireNotBlank("contextProperties key")
            value.requireNotBlank("contextProperties value for '$key'")
        }
    }

    /** profile을 provider의 `DruidConnectionOptions`로 변환한다. */
    fun toConnectionOptions(): DruidConnectionOptions =
        DruidConnectionOptions(
            avaticaEndpoint = avaticaEndpoint,
            transparentReconnection = transparentReconnection,
            serialization = serialization,
            user = user,
            password = password,
            contextProperties = contextProperties,
        )
}

/** 기본 local Avatica endpoint를 사용하는 Druid query profile을 반환한다. */
fun defaultDruidQueryProfile(): DruidQueryProfile = DruidQueryProfile()

/** Druid datasource count 조회에 사용하는 읽기 전용 SQL을 생성한다. */
internal fun buildDatasourceCountQuery(datasource: String): String {
    datasource.requireNotBlank("datasource")
    require(DRUID_IDENTIFIER.matches(datasource)) {
        "datasource must be a simple Druid identifier: $datasource"
    }
    return "SELECT COUNT(*) AS row_count FROM \"$datasource\""
}

/** Druid datasource의 row count를 동기 query로 조회한다. */
fun queryDatasourceRowCount(profile: DruidQueryProfile): List<Long> =
    DruidJdbc.query(
        sql = buildDatasourceCountQuery(profile.datasource),
        options = profile.toConnectionOptions(),
    ) { resultSet -> resultSet.getLong("row_count") }

/** Druid datasource의 row count를 지정한 dispatcher에서 suspend query로 조회한다. */
suspend fun queryDatasourceRowCountSuspend(
    profile: DruidQueryProfile,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
): List<Long> =
    DruidJdbc.querySuspend(
        sql = buildDatasourceCountQuery(profile.datasource),
        options = profile.toConnectionOptions(),
        dispatcher = dispatcher,
    ) { resultSet -> resultSet.getLong("row_count") }

/** Druid `INFORMATION_SCHEMA.COLUMNS`에서 profile datasource의 column metadata를 조회한다. */
fun listDatasourceColumns(profile: DruidQueryProfile): List<DruidColumnMetadata> =
    DruidJdbc.listColumns(
        datasource = profile.datasource,
        schema = profile.schema,
        options = profile.toConnectionOptions(),
    )
```

- [ ] **Step 2: Run the targeted test to prove GREEN.**

```bash
./gradlew :10-druid-query-only:test --tests '*DruidQueryOnlyWorkshopTest' --no-daemon --no-configuration-cache
```

Expected: all workshop tests pass, including provider blank/DML rejection without a network connection.

- [ ] **Step 3: Refactor only after GREEN.**

Check that no `!!`, mutable global state, new dependency, `Database.connect`, Exposed table/DAO, or raw credential literal was added. Re-run the same targeted test after any naming/KDoc-only cleanup.

- [ ] **Step 4: Commit the implementation and regression tests.**

```bash
git add 13-ecosystem-integrations/10-druid-query-only/src/main 13-ecosystem-integrations/10-druid-query-only/src/test
git commit -m "Apache Druid query-only 조회 경로를 검증 가능한 예제로 고정한다" -m "타입화한 profile과 sync/suspend/metadata facade를 provider API 위에 최소 범위로 추가한다.\n\nConstraint: Druid 모듈은 query-only 계약을 지키고 기본 테스트는 외부 서비스에 의존하지 않는다.\nRejected: Exposed Database/DAO와 로컬 Druid 서버 | provider의 지원 범위를 벗어나고 테스트가 비결정적이다.\nConfidence: high\nScope-risk: moderate\nDirective: 새로운 Druid 호출은 profile과 provider query-only API를 통해서만 노출한다.\nTested: targeted DruidQueryOnlyWorkshopTest RED→GREEN\nNot-tested: opt-in smoke와 정적/문서/다이어그램 검사는 다음 단계에서 수행한다."
```

## Task 4: Explicit opt-in smoke test

**Files:**
- Create: `13-ecosystem-integrations/10-druid-query-only/src/test/kotlin/exposed/examples/druid/queryonly/DruidQueryOnlySmokeTest.kt`.

- [ ] **Step 1: Add the disabled-by-default smoke test.**

```kotlin
package exposed.examples.druid.queryonly

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

@EnabledIfEnvironmentVariable(named = "EXPOSED_DRUID_SMOKE", matches = "true")
class DruidQueryOnlySmokeTest {

    @Test
    fun `opt-in Druid endpoint serves query and metadata`() = runSuspendIO {
        val profile = DruidQueryProfile(
            avaticaEndpoint = envOrDefault("EXPOSED_DRUID_AVATICA_ENDPOINT", "http://localhost:8888/druid/v2/sql/avatica/"),
            datasource = envOrDefault("EXPOSED_DRUID_DATASOURCE", "wikipedia"),
            schema = envOrDefault("EXPOSED_DRUID_SCHEMA", "druid"),
            user = System.getenv("EXPOSED_DRUID_USER")?.takeIf(String::isNotBlank),
            password = System.getenv("EXPOSED_DRUID_PASSWORD")?.takeIf(String::isNotBlank),
        )

        queryDatasourceRowCountSuspend(profile).size shouldBeEqualTo 1
        listDatasourceColumns(profile).isNotEmpty().shouldBeTrue()
    }

    private fun envOrDefault(name: String, fallback: String): String =
        System.getenv(name)?.takeIf(String::isNotBlank) ?: fallback
}
```

- [ ] **Step 2: Prove the smoke test is skipped without opt-in.**

```bash
./gradlew :10-druid-query-only:test --tests '*DruidQueryOnlySmokeTest' --no-daemon --no-configuration-cache
```

Expected: JUnit reports the test skipped/disabled and the task succeeds without opening an endpoint or reading credentials.

- [ ] **Step 3: Run the complete module test without opt-in.**

```bash
./gradlew :10-druid-query-only:test --no-daemon --no-configuration-cache
```

Expected: deterministic workshop tests pass and the smoke class remains disabled.

## Task 5: Bilingual README and chapter/workflow registration

**Files:**
- Create: `13-ecosystem-integrations/10-druid-query-only/README.md`.
- Create: `13-ecosystem-integrations/10-druid-query-only/README.ko.md`.
- Modify: `13-ecosystem-integrations/README.md`.
- Modify: `13-ecosystem-integrations/README.ko.md`.
- Modify: `.github/scripts/select-changed-examples.sh`.

- [ ] **Step 1: Write source-equivalent README sections.**

Both README files must contain the same sections in the same order: purpose, architecture image, `DruidQueryProfile`/`DruidConnectionOptions` snippet, sync query, suspend query, `listColumns`, supported query-only SQL (`SELECT`/`WITH`/`EXPLAIN`/`DESCRIBE`/`SHOW`), excluded DDL/DML/DAO/repository/migration/`Database`/dialect, deterministic test command, and opt-in smoke command with endpoint/user/password environment variables. English prose is in `README.md`; Korean prose is in `README.ko.md`; code and identifiers remain unchanged.

The English image reference is:

```markdown
![Apache Druid query-only architecture](../../docs/images/readme-diagrams/13-druid-query-only-architecture-01.png)
```

The Korean image reference is:

```markdown
![Apache Druid query-only 아키텍처](../../docs/images/readme-diagrams/13-druid-query-only-architecture-01.ko.png)
```

The smoke command must use placeholders, never a real credential:

```bash
EXPOSED_DRUID_SMOKE=true \\
EXPOSED_DRUID_AVATICA_ENDPOINT='https://<router>/druid/v2/sql/avatica/' \\
EXPOSED_DRUID_DATASOURCE='<datasource>' \\
./gradlew :10-druid-query-only:test --tests '*DruidQueryOnlySmokeTest'
```

- [ ] **Step 2: Register the chapter row and weekly task.**

Add the issue #234 row after the DuckDB row in both chapter tables with status `Ready`, path `10-druid-query-only`, task `:10-druid-query-only:build`, title `Apache Druid Query-Only Exposed`, and lane `Database platform adapters`. Add `:10-druid-query-only:build` to `ALL_TASKS` in `.github/scripts/select-changed-examples.sh`; keep the existing dynamic `13-ecosystem-integrations/*/**` mapping unchanged.

- [ ] **Step 3: Verify registration and README parity.**

Run:

```bash
./gradlew projects --no-daemon --no-configuration-cache
bash .github/scripts/select-changed-examples.sh HEAD~1..HEAD
git diff --check
```

Expected: project discovery lists `:10-druid-query-only`; the changed-examples script includes `:10-druid-query-only:build` when the module path is in the diff; and diff check is clean.

## Task 6: Architecture SVG/PNG assets

**Files:**
- Create: `docs/images/readme-diagrams/13-druid-query-only-architecture-01.svg`.
- Create: `docs/images/readme-diagrams/13-druid-query-only-architecture-01.ko.svg`.
- Create/render: matching `.png` files.
- Create: `docs/images/readme-diagrams/13-druid-query-only-architecture-01.semantic.json`.

- [ ] **Step 1: Record the semantic ledger before drawing.**

Use this source-backed model:

```json
{
  "kind": "architecture",
  "source": {
    "question": "타입화한 Druid profile과 query-only facade가 Avatica endpoint까지 어떤 책임 경계로 이어지는가?",
    "revision": "a357c6cb",
    "paths": [
      "docs/superpowers/specs/2026-08-26-issue-234-druid-query-only-design.md",
      "13-ecosystem-integrations/10-druid-query-only/src/main/kotlin/exposed/examples/druid/queryonly/DruidQueryOnlyWorkshop.kt",
      "13-ecosystem-integrations/10-druid-query-only/README.md"
    ]
  },
  "nodes": [
    {"id":"profile","label":"DruidQueryProfile","source":"DruidQueryOnlyWorkshop.kt"},
    {"id":"options","label":"DruidConnectionOptions","source":"DruidQueryOnlyWorkshop.kt"},
    {"id":"jdbc","label":"DruidJdbc query / querySuspend / listColumns","source":"DruidQueryOnlyWorkshop.kt"},
    {"id":"local","label":"Deterministic MockK tests","source":"DruidQueryOnlyWorkshopTest.kt"},
    {"id":"smoke","label":"Explicit EXPOSED_DRUID_SMOKE opt-in","source":"DruidQueryOnlySmokeTest.kt"},
    {"id":"druid","label":"Apache Druid Router/Broker Avatica","source":"README.md"}
  ],
  "edges": [
    {"id":"profile-options","from":"profile","to":"options","kind":"dependency","source":"DruidQueryOnlyWorkshop.kt"},
    {"id":"options-jdbc","from":"options","to":"jdbc","kind":"configuration","source":"DruidQueryOnlyWorkshop.kt"},
    {"id":"jdbc-local","from":"jdbc","to":"local","kind":"verification","source":"DruidQueryOnlyWorkshopTest.kt"},
    {"id":"jdbc-smoke","from":"jdbc","to":"smoke","kind":"opt-in","source":"DruidQueryOnlySmokeTest.kt"},
    {"id":"jdbc-druid","from":"jdbc","to":"druid","kind":"query","source":"README.md"}
  ],
  "behavior": {"branches": 1, "loops": 0},
  "repairs": []
}
```

- [ ] **Step 2: Draw and render one SVG at a time.**

Use the repository architecture family `docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.png` as the palette/layout reference. Use orthogonal rounded connectors, typed primary/secondary markers, Korean or English fonts from the diagram contract, and no invented technology logos. Keep the six source-backed cards and five edges; explain solid query edges and dashed test/opt-in edges in an in-image legend.

- [ ] **Step 3: Validate, render, and audit each asset.**

Run for each SVG:

```bash
xmllint --noout docs/images/readme-diagrams/13-druid-query-only-architecture-01.svg
cairosvg docs/images/readme-diagrams/13-druid-query-only-architecture-01.svg -o docs/images/readme-diagrams/13-druid-query-only-architecture-01.png -s 2
python3 "${CODEX_HOME:-$HOME/.codex}/skills/bluetape-diagram/scripts/diagram-semantic-audit.py" --repo-root . --json docs/images/readme-diagrams/13-druid-query-only-architecture-01.semantic.json
python3 "${CODEX_HOME:-$HOME/.codex}/skills/bluetape-diagram/scripts/diagram-svg-text-normalize.py" docs/images/readme-diagrams/13-druid-query-only-architecture-01.svg
python3 "${CODEX_HOME:-$HOME/.codex}/skills/bluetape-diagram/scripts/diagram-connector-audit.py" docs/images/readme-diagrams/13-druid-query-only-architecture-01.svg
python3 "${CODEX_HOME:-$HOME/.codex}/skills/bluetape-diagram/scripts/diagram-arrowhead-audit.py" docs/images/readme-diagrams/13-druid-query-only-architecture-01.svg
python3 "${CODEX_HOME:-$HOME/.codex}/skills/bluetape-diagram/scripts/diagram-geometry-audit.py" --fail-diagonal docs/images/readme-diagrams/13-druid-query-only-architecture-01.svg
python3 "${CODEX_HOME:-$HOME/.codex}/skills/bluetape-diagram/scripts/diagram-endpoint-audit.py" docs/images/readme-diagrams/13-druid-query-only-architecture-01.svg
python3 "${CODEX_HOME:-$HOME/.codex}/skills/bluetape-diagram/scripts/diagram-mixed-corner-audit.py" docs/images/readme-diagrams/13-druid-query-only-architecture-01.svg
python3 "${CODEX_HOME:-$HOME/.codex}/skills/bluetape-diagram/scripts/diagram-visual-audit.py" --require-opaque docs/images/readme-diagrams/13-druid-query-only-architecture-01.png
```

Repeat with `.ko.svg` and `.ko.png`; then run:

```bash
python3 "${CODEX_HOME:-$HOME/.codex}/skills/bluetape-diagram/scripts/diagram-asset-pair-audit.py" --asset-dir docs/images/readme-diagrams --readme 13-ecosystem-integrations/10-druid-query-only/README.md --require-all-referenced
python3 "${CODEX_HOME:-$HOME/.codex}/skills/bluetape-diagram/scripts/diagram-asset-pair-audit.py" --asset-dir docs/images/readme-diagrams --readme 13-ecosystem-integrations/10-druid-query-only/README.ko.md --require-all-referenced
```

Open both final PNGs at full size and record dimensions, opaque background, balanced margins, readable labels, perpendicular endpoints, rounded corners, zero crossing/card intrusion, marker direction, and zero audit failures in the review receipt.

## Task 7: Serial six-lens review, lesson, and final verification

**Files:**
- Create: `docs/review/2026-08-26-issue-234-druid-query-only.md`.
- Create: `docs/lessons/2026-08-26-issue-234-druid-query-only.md`.

- [ ] **Step 1: Run the required serial review lenses.**

Record evidence for each lens in the review receipt:

1. API/architecture — only the profile and three provider methods are exposed; no unsupported Exposed layer.
2. Kotlin — immutable data, `requireNotBlank`, null-safe optional credentials, Korean KDoc, dispatcher parameter, no `!!`.
3. Tests — targeted RED failure was observed; targeted GREEN and complete module tests pass; smoke is disabled by default.
4. Security/operations — no checked-in credentials or real endpoint; identifier validation; explicit smoke environment gate.
5. Documentation/diagram — README locale parity, PNG-only links, SVG/PNG pair and all diagram audits pass.
6. Scope/workflow — chapter row, task selector, issue acceptance criteria, and query-only exclusions are present.

- [ ] **Step 2: Run standard module and static checks in serialized order.**

```bash
./gradlew :10-druid-query-only:test --no-daemon --no-configuration-cache
./gradlew :10-druid-query-only:detekt --no-daemon --no-configuration-cache
./gradlew :10-druid-query-only:build --no-daemon --no-configuration-cache
git diff --check
git status --short --branch
```

Expected: each Gradle task exits 0, no test failure, no detekt finding, and only the planned files are changed. Do not run heavy Gradle tasks in parallel.

- [ ] **Step 3: Write the Korean lesson.**

Document the durable decisions: provider-first query-only scope, MockK deterministic default, environment-gated smoke, central catalog alias, and the diagram/README parity contract. Link the design and implementation plan; list no unregistered follow-up.

- [ ] **Step 4: Commit docs, diagrams, workflow registration, and review evidence.**

```bash
git add gradle/libs.versions.toml 13-ecosystem-integrations .github/scripts/select-changed-examples.sh docs/images/readme-diagrams docs/review/2026-08-26-issue-234-druid-query-only.md docs/lessons/2026-08-26-issue-234-druid-query-only.md
git commit -m "Apache Druid 예제의 문서와 자동 검증 경계를 완성한다" -m "영어/한국어 README, architecture 자산, chapter/workflow 등록과 여섯 렌즈 검토를 함께 고정한다.\n\nConstraint: 외부 서비스는 opt-in이고 README는 PNG와 source-equivalent locale을 제공해야 한다.\nRejected: Mermaid 원문 embed와 단일 locale raster | 저장소의 diagram/문서 계약을 위반한다.\nConfidence: high\nScope-risk: moderate\nDirective: smoke 환경과 provider query-only 계약을 운영 endpoint나 DDL/DML 예제로 확장하지 않는다.\nTested: module test, detekt, build, diagram audits, README asset-pair audits, git diff --check\nNot-tested: 실제 Druid smoke는 명시적 환경 변수가 없으므로 의도적으로 실행하지 않는다."
```

- [ ] **Step 5: Prepare PR evidence without merging.**

Refresh `AGENTS.md`, workflow references, issue metadata, and PR template immediately before creating the PR. Push `feat/issue-234-druid-query-only`, create/update a Korean PR targeting `develop`, verify the live PR body ends with `## DoD Status`, and re-read exact head/CI/threads. Stop at merge-ready; merging requires a separate fresh explicit approval tied to the live PR head.

## Plan self-review

- Spec coverage: catalog alias (Task 1), provider API and query-only boundary (Tasks 2–3), deterministic and opt-in tests (Task 4), bilingual docs/chapter/workflow (Task 5), architecture assets and audits (Task 6), six review lenses/lesson/DoD (Task 7).
- Placeholder scan: no `TBD`, `TODO`, or vague implementation step is used; every code change has an exact path, code shape, command, and expected result.
- Type consistency: `DruidQueryProfile`, `toConnectionOptions`, `buildDatasourceCountQuery`, `queryDatasourceRowCount`, `queryDatasourceRowCountSuspend`, and `listDatasourceColumns` names are identical in tests, implementation, README plan, and smoke test.
- Single-owner constraint: tasks are sequential, heavy Gradle checks are serialized, and no subagent lane is required.

