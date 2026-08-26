# Checkpointable Exposed JDBC Batch Workshop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 이슈 #236에 H2 기본 경로를 가진 checkpointable Exposed JDBC batch workshop을 추가하고, 실패·재시작·skip/retry·timeout·cancellation 계약을 테스트와 bilingual 문서로 고정한다.

**Architecture:** `13-ecosystem-integrations/11-checkpointable-batch` 독립 module이 `BatchJob` DSL, `ExposedJdbcBatchJobRepository`, `ExposedJdbcBatchReader`, `ExposedJdbcBatchWriter`를 직접 조합한다. source/target table과 불변 options는 main source에 두고, 실패를 주입하는 writer는 test source에만 둔다. R2DBC는 이 저장소에 추가하지 않고 `exposed-r2dbc-workshop#205`가 담당한다.

**Tech Stack:** Kotlin 2.3, Java 25, Gradle version catalog, JetBrains Exposed 1.4.0, `bluetape4k-exposed-batch:1.12.1`, Jackson 3 checkpoint serialization, H2, JUnit 5, kotlinx-coroutines-test, SVG/PNG diagrams.

---

## 파일 영향 지도

- Create: `13-ecosystem-integrations/11-checkpointable-batch/build.gradle.kts` — module 의존성, JDK 25 runtime, H2 test runtime.
- Create: `13-ecosystem-integrations/11-checkpointable-batch/src/main/kotlin/exposed/examples/batch/jdbc/JdbcBatchWorkshop.kt` — table, record, options, schema helper, provider 조합 API.
- Create: `13-ecosystem-integrations/11-checkpointable-batch/src/test/kotlin/exposed/examples/batch/jdbc/JdbcBatchWorkshopTest.kt` — H2 deterministic end-to-end 및 실패 주입 테스트.
- Create: `13-ecosystem-integrations/11-checkpointable-batch/src/test/resources/junit-platform.properties` — module 테스트 병렬 실행 제한.
- Create: `13-ecosystem-integrations/11-checkpointable-batch/src/test/resources/logback-test.xml` — module 테스트 로그 설정.
- Create: `13-ecosystem-integrations/11-checkpointable-batch/README.md` — English reader documentation.
- Create: `13-ecosystem-integrations/11-checkpointable-batch/README.ko.md` — Korean source-equivalent documentation.
- Modify: `gradle/libs.versions.toml` — `exposed-batch` alias.
- Modify: `.github/scripts/select-changed-examples.sh` — `:11-checkpointable-batch:build` fixed Examples task.
- Modify: `13-ecosystem-integrations/README.md`, `13-ecosystem-integrations/README.ko.md` — chapter index row and link.
- Modify: `README.md`, `README.ko.md` — root chapter 13 module link.
- Create: `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-architecture-01.svg` and `.png` — English architecture.
- Create: `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-architecture-01.ko.svg` and `.png` — Korean architecture.
- Create: `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-lifecycle-01.svg` and `.png` — English lifecycle.
- Create: `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-lifecycle-01.ko.svg` and `.png` — Korean lifecycle.

## Task 1: Register the dependency and empty module

**Files:**
- Modify: `gradle/libs.versions.toml:92-145`
- Create: `13-ecosystem-integrations/11-checkpointable-batch/build.gradle.kts`

- [ ] **Step 1: Add the central catalog alias.**

Insert the following entry beside the existing `exposed-*` aliases and do not add a version:

```toml
exposed-batch = { module = "io.github.bluetape4k.exposed:bluetape4k-exposed-batch" }
```

- [ ] **Step 2: Add the module build file.**

Create the exact Gradle file below so provider compile-only contracts and the JDK 25 runtime are explicit:

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
    testRuntimeClasspath {
        exclude(group = "io.github.bluetape4k", module = "bluetape4k-virtualthread-jdk21")
    }
}

dependencies {
    implementation(libs.exposed.batch)
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.bluetape4k.jackson3)
    implementation(libs.bluetape4k.virtualthread.jdk25)

    runtimeOnly(libs.h2.v2)

    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 3: Prove automatic project discovery before adding source.**

Run:

```bash
./gradlew projects --no-daemon
```

Expected: the project list contains `:11-checkpointable-batch` without a new `settings.gradle.kts` include.

- [ ] **Step 4: Commit the registration slice.**

```bash
git add gradle/libs.versions.toml 13-ecosystem-integrations/11-checkpointable-batch/build.gradle.kts
git commit -m "JDBC batch workshop 의존성 경계를 등록한다" -m "Constraint: 버전은 bluetape4k-dependencies BOM이 관리한다.
Rejected: provider 버전 로컬 고정 | 중앙 catalog와 중복된다.
Confidence: high
Scope-risk: narrow
Directive: R2DBC alias와 module을 이 저장소에 추가하지 않는다.
Tested: ./gradlew projects --no-daemon
Not-tested: 아직 source와 module test는 없음
"
```

## Task 2: Write the first failing behavior test

**Files:**
- Create: `13-ecosystem-integrations/11-checkpointable-batch/src/test/kotlin/exposed/examples/batch/jdbc/JdbcBatchWorkshopTest.kt`

- [ ] **Step 1: Write the normal execution expectation before production source.**

Create the test file with this first test. The referenced production symbols are intentionally absent at this point:

```kotlin
package exposed.examples.batch.jdbc

import io.bluetape4k.batch.api.BatchReport
import io.bluetape4k.batch.api.BatchStatus
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Test

class JdbcBatchWorkshopTest {

    @Test
    fun `normal execution persists transformed rows and completes`() = runTest {
        val database = Database.connect(
            url = "jdbc:h2:mem:checkpointable-jdbc-red;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver",
        )

        createJdbcBatchSchema(database)
        seed(database, 1..8)

        val report = runCheckpointableJdbcBatch(
            database,
            JdbcBatchOptions(chunkSize = 3),
        )

        report shouldBeInstanceOf BatchReport.Success::class
        report.stepReports.single().status shouldBeEqualTo BatchStatus.COMPLETED
        report.stepReports.single().writeCount shouldBeEqualTo 8L
        targetRows(database).single { it.sourceId == 1L }.transformedValue shouldBeEqualTo 2
    }
}
```

- [ ] **Step 2: Run the test and record the correct RED failure.**

Run:

```bash
./gradlew :11-checkpointable-batch:test --tests '*JdbcBatchWorkshopTest.normal execution persists transformed rows and completes' --no-daemon
```

Expected: compilation fails with unresolved references for `createJdbcBatchSchema`, `seed`, `runCheckpointableJdbcBatch`, `JdbcBatchOptions`, and `targetRows`; this confirms the test is exercising the new feature rather than existing behavior.

## Task 3: Define the JDBC workshop contract

**Files:**
- Create: `13-ecosystem-integrations/11-checkpointable-batch/src/main/kotlin/exposed/examples/batch/jdbc/JdbcBatchWorkshop.kt`

- [ ] **Step 1: Write the public types and schema objects.**

Use the following names and contracts; keep KDoc in Korean and imports under `org.jetbrains.exposed.v1.*`:

```kotlin
package exposed.examples.batch.jdbc

import io.bluetape4k.batch.BatchDefaults
import io.bluetape4k.batch.api.BatchProcessor
import io.bluetape4k.batch.api.BatchReader
import io.bluetape4k.batch.api.BatchReport
import io.bluetape4k.batch.api.BatchWriter
import io.bluetape4k.batch.api.SkipPolicy
import io.bluetape4k.batch.core.BatchJob
import io.bluetape4k.batch.core.dsl.batchJob
import io.bluetape4k.batch.internal.CheckpointJson
import io.bluetape4k.batch.jdbc.ExposedJdbcBatchJobRepository
import io.bluetape4k.batch.jdbc.ExposedJdbcBatchReader
import io.bluetape4k.batch.jdbc.ExposedJdbcBatchWriter
import io.bluetape4k.batch.jdbc.tables.BatchJobExecutionTable
import io.bluetape4k.batch.jdbc.tables.BatchStepExecutionTable
import io.bluetape4k.workflow.api.RetryPolicy
import kotlin.time.Duration
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object JdbcBatchSourceTable : Table("jdbc_batch_source") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)
    val value = integer("value")

    override val primaryKey = PrimaryKey(id)
}

object JdbcBatchTargetTable : Table("jdbc_batch_target") {
    val sourceId = long("source_id")
    val sourceName = varchar("source_name", 255)
    val transformedValue = integer("transformed_value")

    override val primaryKey = PrimaryKey(sourceId)
}

data class JdbcSourceRecord(val id: Long, val name: String, val value: Int)

data class JdbcTargetRecord(val sourceId: Long, val sourceName: String, val transformedValue: Int)

data class JdbcBatchOptions(
    val jobName: String = "checkpointable-jdbc-batch",
    val parameters: Map<String, Any> = mapOf("dataset" to "workshop"),
    val chunkSize: Int = 3,
    val pageSize: Int = chunkSize,
    val skipPolicy: SkipPolicy = SkipPolicy.NONE,
    val retryPolicy: RetryPolicy = RetryPolicy.NONE,
    val commitTimeout: Duration = BatchDefaults.COMMIT_TIMEOUT,
) {
    init {
        require(jobName.isNotBlank()) { "jobName must not be blank" }
        require(chunkSize > 0) { "chunkSize must be positive" }
        require(pageSize > 0) { "pageSize must be positive" }
    }
}

val jdbcBatchMetadataTables: List<Table> = listOf(
    BatchJobExecutionTable,
    BatchStepExecutionTable,
    JdbcBatchSourceTable,
    JdbcBatchTargetTable,
)

fun createJdbcBatchSchema(database: Database) {
    transaction(database) {
        SchemaUtils.create(*jdbcBatchMetadataTables.toTypedArray())
    }
}
```

- [ ] **Step 2: Add the provider-backed reader, writer, and job builder.**

Implement these exact boundaries in the same file:

```kotlin
private val defaultJdbcProcessor = BatchProcessor<JdbcSourceRecord, JdbcTargetRecord> { source ->
    JdbcTargetRecord(
        sourceId = source.id,
        sourceName = source.name.uppercase(),
        transformedValue = source.value * 2,
    )
}

private fun jdbcSourceReader(database: Database, pageSize: Int): BatchReader<JdbcSourceRecord> =
    ExposedJdbcBatchReader(
        database = database,
        table = JdbcBatchSourceTable,
        keyColumn = JdbcBatchSourceTable.id,
        pageSize = pageSize,
        rowMapper = { row ->
            JdbcSourceRecord(
                id = row[JdbcBatchSourceTable.id],
                name = row[JdbcBatchSourceTable.name],
                value = row[JdbcBatchSourceTable.value],
            )
        },
        keyExtractor = JdbcSourceRecord::id,
        keyClass = Long::class,
    )

private fun jdbcTargetWriter(database: Database): BatchWriter<JdbcTargetRecord> =
    ExposedJdbcBatchWriter(database, JdbcBatchTargetTable) { target ->
        this[JdbcBatchTargetTable.sourceId] = target.sourceId
        this[JdbcBatchTargetTable.sourceName] = target.sourceName
        this[JdbcBatchTargetTable.transformedValue] = target.transformedValue
    }

fun checkpointableJdbcBatchJob(
    database: Database,
    options: JdbcBatchOptions = JdbcBatchOptions(),
    processor: BatchProcessor<JdbcSourceRecord, JdbcTargetRecord> = defaultJdbcProcessor,
    writer: BatchWriter<JdbcTargetRecord> = jdbcTargetWriter(database),
): BatchJob = batchJob(options.jobName) {
    repository(ExposedJdbcBatchJobRepository(database, CheckpointJson.jackson3()))
    params(options.parameters)
    step<JdbcSourceRecord, JdbcTargetRecord>("transform-and-write") {
        reader(jdbcSourceReader(database, options.pageSize))
        processor(processor)
        writer(writer)
        chunkSize(options.chunkSize)
        skipPolicy(options.skipPolicy)
        retryPolicy(options.retryPolicy)
        commitTimeout(options.commitTimeout)
    }
}

suspend fun runCheckpointableJdbcBatch(
    database: Database,
    options: JdbcBatchOptions = JdbcBatchOptions(),
): BatchReport = checkpointableJdbcBatchJob(database, options).run()
```

- [ ] **Step 3: Compile the contract before writing integration assertions.**

Run:

```bash
./gradlew :11-checkpointable-batch:compileKotlin --no-daemon
```

Expected: `BUILD SUCCESSFUL`; if provider compile-only dependencies are missing, add only the existing catalog aliases listed in Task 1.

## Task 4: Add the deterministic H2 test harness and turn the first test green

**Files:**
- Create: `13-ecosystem-integrations/11-checkpointable-batch/src/test/kotlin/exposed/examples/batch/jdbc/JdbcBatchWorkshopTest.kt`
- Create: `13-ecosystem-integrations/11-checkpointable-batch/src/test/resources/junit-platform.properties`
- Create: `13-ecosystem-integrations/11-checkpointable-batch/src/test/resources/logback-test.xml`

- [ ] **Step 1: Create isolated H2 helpers and seed/query functions.**

Use a unique in-memory URL per test and create all four metadata/data tables before each scenario:

```kotlin
private fun h2Database(name: String): Database = Database.connect(
    url = "jdbc:h2:mem:checkpointable-jdbc-$name;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    driver = "org.h2.Driver",
)

private fun seed(database: Database, values: IntRange) {
    transaction(database) {
        JdbcBatchSourceTable.batchInsert(values.toList()) { value ->
            this[JdbcBatchSourceTable.name] = "item-$value"
            this[JdbcBatchSourceTable.value] = value
        }
    }
}

private fun targetRows(database: Database): List<JdbcTargetRecord> = transaction(database) {
    JdbcBatchTargetTable.selectAll()
        .orderBy(JdbcBatchTargetTable.sourceId)
        .map { row ->
            JdbcTargetRecord(
                sourceId = row[JdbcBatchTargetTable.sourceId],
                sourceName = row[JdbcBatchTargetTable.sourceName],
                transformedValue = row[JdbcBatchTargetTable.transformedValue],
            )
        }
}
```

Use the repository's existing `bluetape4k.assertions` matchers and `runTest`/`runSuspendIO` convention already used by nearby modules.

- [ ] **Step 2: Write the normal execution test first.**

The test must call `createJdbcBatchSchema`, seed `1..8`, run `runCheckpointableJdbcBatch` with `chunkSize = 3`, then assert `BatchReport.Success`, `COMPLETED`, read/write counts of `8`, zero skips, eight target rows, and the target transformation (`ITEM-1`, `2`).

- [ ] **Step 3: Run the normal test before implementation changes.**

Run:

```bash
./gradlew :11-checkpointable-batch:test --tests '*JdbcBatchWorkshopTest.normal execution*' --no-daemon
```

Expected: the test compiles and passes against the Task 3 implementation; any failure is fixed in the narrowest source/test file before adding the next scenario.

- [ ] **Step 4: Add test resources and commit the green baseline.**

`junit-platform.properties` must set `junit.jupiter.execution.parallel.enabled=false`. `logback-test.xml` must keep the same concise console pattern as neighboring chapter 13 modules. Commit only after the targeted test and `git diff --check` pass.

## Task 5: Verify restart, skip, retry, timeout, and STOPPED semantics

**Files:**
- Modify: `13-ecosystem-integrations/11-checkpointable-batch/src/test/kotlin/exposed/examples/batch/jdbc/JdbcBatchWorkshopTest.kt`

- [ ] **Step 1: Add a failing writer and prove keyset restart.**

Create a test-only `FailOnceWriter` that delegates the real `ExposedJdbcBatchWriter`, succeeds for the first chunk, throws once for the second chunk, then delegates. Run the same `JdbcBatchOptions.parameters` twice. Assert first report `BatchReport.Failure`, second report `BatchReport.Success`, target source IDs are exactly `1..8` once, and the second job did not rewrite the first checkpointed chunk.

- [ ] **Step 2: Add processor skip coverage.**

Inject `BatchProcessor` that throws `IllegalArgumentException("even value")` for even values, set `SkipPolicy.ALL`, run ten rows, and assert `BatchReport.PartiallyCompleted`, `skipCount == 5`, `writeCount == 5`, and target IDs contain only odd source IDs.

- [ ] **Step 3: Add retry/backoff coverage.**

Create a `FlakyWriter` that throws on its first call and delegates on its second call. Configure `RetryPolicy(maxAttempts = 2, delay = 1.milliseconds)` and assert two attempts plus a successful report. Keep the delay bounded to avoid a slow default test.

- [ ] **Step 4: Add commit-timeout coverage.**

Create a `SlowWriter` that calls `delay(50.milliseconds)` before delegating. Configure `commitTimeout = 5.milliseconds`, `RetryPolicy.NONE`, and `SkipPolicy.maxSkips(3)`. Assert `BatchReport.PartiallyCompleted`, `skipCount == 3`, `writeCount == 0`, and no target row from the timed-out chunk.

- [ ] **Step 5: Add cancellation and STOPPED restart coverage.**

Create a writer that delegates and signals a `CompletableDeferred<Unit>` after the first write, then suspends on the second write. Wait until `BatchStepExecutionTable.checkpoint` is non-null, cancel the running job, assert `CancellationException` is rethrown, query metadata for `STOPPED`, then run a fresh job with the normal writer and assert the final target source IDs are `1..8` exactly once.

- [ ] **Step 6: Add schema contract assertions.**

Query `BatchJobExecutionTable`, `BatchStepExecutionTable`, `JdbcBatchSourceTable`, and `JdbcBatchTargetTable` through Exposed metadata and assert all required tables exist and target `sourceId` is the primary key used by the idempotent workshop output.

- [ ] **Step 7: Run the complete module test and commit the behavior slice.**

Run:

```bash
USE_FAST_DB=true ./gradlew :11-checkpointable-batch:test --no-daemon
```

Expected: all module tests pass without Docker, network, or credentials. Commit with a Korean Lore message that records the H2 evidence and any provider behavior not tested locally.

## Task 6: Write bilingual module documentation and diagrams

**Files:**
- Create: `13-ecosystem-integrations/11-checkpointable-batch/README.md`
- Create: `13-ecosystem-integrations/11-checkpointable-batch/README.ko.md`
- Create: `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-architecture-01.svg`
- Create: `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-architecture-01.png`
- Create: `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-architecture-01.ko.svg`
- Create: `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-architecture-01.ko.png`
- Create: `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-lifecycle-01.svg`
- Create: `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-lifecycle-01.png`
- Create: `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-lifecycle-01.ko.svg`
- Create: `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-lifecycle-01.ko.png`

- [ ] **Step 1: Draft the English README from the public source.**

Include the module link to `README.ko.md`, architecture PNG, purpose, provider coordinates, schema table names, `checkpointableJdbcBatchJob` snippet, restart ordering, failure/skip/retry/timeout/cancellation behavior, at-least-once and idempotency caveat, H2 test command, optional PostgreSQL JDBC boundary, and explicit statement that R2DBC belongs to `exposed-r2dbc-workshop#205`.

- [ ] **Step 2: Write `README.ko.md` as source-equivalent Korean.**

Keep the same headings, code blocks, list order, links, image filenames, counts, commands, and exclusions. Preserve API names, identifiers, URLs, versions, and exact `BatchStatus` tokens.

- [ ] **Step 3: Create the four SVG sources.**

Architecture labels: `JdbcBatchWorkshop`, `BatchJob`, `BatchStepRunner`, JDBC keyset reader, chunk writer, checkpoint repository tables, H2, and restart boundary. Lifecycle labels: `RUNNING`, `FAILED`, `STOPPED`, `COMPLETED`, `COMPLETED_WITH_SKIPS`, and the ordered `write → onChunkCommitted → saveCheckpoint` transition. English and Korean assets must be source-equivalent.

- [ ] **Step 4: Render and inspect PNGs.**

Use the repository's `bluetape-diagram` renderer/audit workflow to render each SVG at scale 2, inspect the PNGs at README size, and run XML/semantic/connector/arrowhead/geometry/visual/asset-pair audits. README files embed PNG only; SVG links remain available through the image target.

- [ ] **Step 5: Run documentation checks.**

Run:

```bash
git diff --check
node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs \
  13-ecosystem-integrations/11-checkpointable-batch/README.ko.md
```

Expected: no whitespace errors and zero unexplained terminology findings.

## Task 7: Register chapter, root README, and Examples workflow

**Files:**
- Modify: `13-ecosystem-integrations/README.md`
- Modify: `13-ecosystem-integrations/README.ko.md`
- Modify: `README.md`
- Modify: `README.ko.md`
- Modify: `.github/scripts/select-changed-examples.sh`

- [ ] **Step 1: Add chapter 13 rows after the Druid entry.**

Add issue #236, path `13-ecosystem-integrations/11-checkpointable-batch`, task `:11-checkpointable-batch:build`, title `Checkpointable Exposed JDBC Batch Workshop`, and lane `JDBC batch execution` to both chapter tables. Korean row text must preserve the same path/task/link and translate only reader-facing prose.

- [ ] **Step 2: Add root README module links.**

Add matching English/Korean module headings after the Druid section, linking to both module README files and stating that the workshop demonstrates JDBC checkpoint restart and chunk lifecycle.

- [ ] **Step 3: Add the fixed Examples task.**

Insert `:11-checkpointable-batch:build` into `ALL_TASKS` in `.github/scripts/select-changed-examples.sh`. Do not add a new path case; the existing `13-ecosystem-integrations/*/**` dynamic mapping selects the module.

- [ ] **Step 4: Prove path selection and workflow registration.**

Run:

```bash
FORCE_ALL=true .github/scripts/select-changed-examples.sh
git diff --name-only HEAD~1..HEAD | .github/scripts/select-changed-examples.sh HEAD~1..HEAD
```

Expected: the fixed task list contains `:11-checkpointable-batch:build`, and a module-only diff selects only that task rather than silently dropping the module. Run `git diff --check` again.

## Task 8: Full verification and workflow receipt evidence

**Files:**
- Modify: workflow receipt under `/Users/debop/work/bluetape4k/exposed-workshop/.bluetape/runs/20260826T160113Z-76d748ca/`
- No source file changes unless a verification failure requires a narrow repair.

- [ ] **Step 1: Run targeted and static checks.**

Run sequentially:

```bash
USE_FAST_DB=true ./gradlew :11-checkpointable-batch:test --no-daemon
./gradlew :11-checkpointable-batch:build --no-daemon
./gradlew detekt --no-daemon
git diff --check
```

Expected: all commands exit 0; no network or credential requirement is introduced.

- [ ] **Step 2: Re-read implementation and docs against the design.**

Verify provider class names, package imports, table names, status tokens, README locale parity, diagram filenames, issue links, R2DBC exclusion, and workflow task registration. Run the Korean terminology audit again after any prose repair.

- [ ] **Step 3: Attach workflow checks.**

Record evidence for `spec-plan`, `module-test`, `static-check`, `docs-parity`, and `workflow-registration` with exact paths and command output. Attach component evidence only after all five checks have fresh results.

- [ ] **Step 4: Run workflow completion check.**

Use `bluetape-flow.py completion-check` with the Type A run's explicit `--state-root` and owner file. Repair any missing evidence before considering the run complete.

- [ ] **Step 5: Commit the final implementation slice.**

Use the Lore commit protocol in Korean. Include `Tested:` with the exact Gradle, static, docs, and path-selection evidence and `Not-tested:` only for explicitly opt-in PostgreSQL or remote CI evidence.

- [ ] **Step 6: Prepare merge-ready report, not a merge.**

Create/update the Korean PR body with `Summary`, `Background`, `What This Solves`, `Work Done`, `Validation`, `Review Notes`, `Metadata`, and final `## DoD Status`. Read back assignee, milestone, labels, closing token, exact head SHA, CI checks, review threads, and mergeability. Stop at `PENDING` for a fresh explicit user approval tied to that exact head; never enable auto-merge.

## Plan self-review

- Spec coverage: Tasks 1–3 cover module/API/dependency boundaries; Task 4 covers H2 deterministic success; Task 5 covers all provider lifecycle behaviors and schema; Task 6 covers bilingual docs/diagrams; Task 7 covers chapter/root/CI registration; Task 8 covers workflow evidence and merge gate.
- Placeholder scan: no `TODO`, `TBD`, `<module>`, or unspecified command remains in the task steps; all Gradle task names and file paths are concrete.
- Type consistency: `JdbcBatchOptions`, `JdbcSourceRecord`, `JdbcTargetRecord`, `JdbcBatchSourceTable`, `JdbcBatchTargetTable`, `checkpointableJdbcBatchJob`, `runCheckpointableJdbcBatch`, and `createJdbcBatchSchema` are used consistently across source, tests, docs, and acceptance checks.
- Rollback/rerun: each task commits a narrow slice; a failed test is repaired in its owning file, and all H2 tests use unique in-memory names so reruns do not reuse state.
