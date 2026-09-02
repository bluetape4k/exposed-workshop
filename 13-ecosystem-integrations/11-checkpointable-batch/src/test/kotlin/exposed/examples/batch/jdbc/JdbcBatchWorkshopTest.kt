package exposed.examples.batch.jdbc

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.batch.api.BatchProcessor
import io.bluetape4k.batch.api.BatchReport
import io.bluetape4k.batch.api.BatchStatus
import io.bluetape4k.batch.api.BatchWriter
import io.bluetape4k.batch.api.SkipPolicy
import io.bluetape4k.batch.CheckpointJson
import io.bluetape4k.batch.jdbc.ExposedJdbcBatchJobRepository
import io.bluetape4k.batch.jdbc.tables.BatchJobExecutionTable
import io.bluetape4k.batch.jdbc.tables.BatchStepExecutionTable
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.workflow.api.RetryPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

class JdbcBatchWorkshopTest {

    @Test
    fun `normal execution persists transformed rows and completes`() = runSuspendIO {
        val database = preparedDatabase("normal", 1..8)

        val report = runCheckpointableJdbcBatch(
            database,
            JdbcBatchOptions(chunkSize = 3),
        )

        report shouldBeInstanceOf BatchReport.Success::class
        report.stepReports.single().status shouldBeEqualTo BatchStatus.COMPLETED
        report.stepReports.single().readCount shouldBeEqualTo 8L
        report.stepReports.single().writeCount shouldBeEqualTo 8L
        report.stepReports.single().skipCount shouldBeEqualTo 0L
        report.stepReports.single().checkpoint shouldBeEqualTo 8L
        targetRows(database).size shouldBeEqualTo 8
        targetRows(database).single { it.sourceId == 1L } shouldBeEqualTo JdbcTargetRecord(
            sourceId = 1L,
            sourceName = "ITEM-1",
            transformedValue = 2,
        )
    }

    @Test
    fun `failed execution is surfaced without hiding the provider checkpoint limitation`() = runSuspendIO {
        val database = preparedDatabase("restart", 1..8)
        val options = JdbcBatchOptions(
            jobName = "checkpointable-jdbc-restart",
            chunkSize = 3,
        )
        val failingWriter = FailOnceWriter(jdbcTargetWriter(database))

        val firstReport = checkpointableJdbcBatchJob(database, options, writer = failingWriter).run()

        firstReport shouldBeInstanceOf BatchReport.Failure::class
        firstReport.stepReports.single().status shouldBeEqualTo BatchStatus.FAILED
        firstReport.stepReports.single().writeCount shouldBeEqualTo 3L
        failingWriter.attempts shouldBeEqualTo 2
        targetRows(database).map { it.sourceId } shouldBeEqualTo listOf(1L, 2L, 3L)
        jobStatus(database) shouldBeEqualTo BatchStatus.FAILED
        stepStatus(database) shouldBeEqualTo BatchStatus.FAILED
    }

    @Test
    fun `processor errors are skipped and successful records are written`() = runSuspendIO {
        val database = preparedDatabase("skip", 1..10)
        val processor = BatchProcessor<JdbcSourceRecord, JdbcTargetRecord> { source ->
            if (source.value % 2 == 0) {
                throw IllegalArgumentException("even value")
            }
            defaultJdbcProcessor.process(source)
        }

        val report = checkpointableJdbcBatchJob(
            database = database,
            options = JdbcBatchOptions(
                jobName = "checkpointable-jdbc-skip",
                chunkSize = 4,
                skipPolicy = SkipPolicy.ALL,
            ),
            processor = processor,
        ).run()

        report shouldBeInstanceOf BatchReport.PartiallyCompleted::class
        report.stepReports.single().status shouldBeEqualTo BatchStatus.COMPLETED_WITH_SKIPS
        report.stepReports.single().skipCount shouldBeEqualTo 5L
        report.stepReports.single().writeCount shouldBeEqualTo 5L
        targetRows(database).map { it.sourceId } shouldBeEqualTo listOf(1L, 3L, 5L, 7L, 9L)
    }

    @Test
    fun `writer failure retries with bounded backoff`() = runSuspendIO {
        val database = preparedDatabase("retry", 1..3)
        val retryingWriter = FailFirstWriter(jdbcTargetWriter(database))

        val report = checkpointableJdbcBatchJob(
            database = database,
            options = JdbcBatchOptions(
                jobName = "checkpointable-jdbc-retry",
                retryPolicy = RetryPolicy(maxAttempts = 2, delay = 1.milliseconds),
            ),
            writer = retryingWriter,
        ).run()

        report shouldBeInstanceOf BatchReport.Success::class
        report.stepReports.single().status shouldBeEqualTo BatchStatus.COMPLETED
        retryingWriter.attempts shouldBeEqualTo 2
        targetRows(database).size shouldBeEqualTo 3
    }

    @Test
    fun `commit timeout skips one timed out chunk without partial target rows`() = runSuspendIO {
        val database = preparedDatabase("timeout", 1..3)
        val slowWriter = SlowWriter(jdbcTargetWriter(database), 50.milliseconds)

        val report = checkpointableJdbcBatchJob(
            database = database,
            options = JdbcBatchOptions(
                jobName = "checkpointable-jdbc-timeout",
                chunkSize = 3,
                commitTimeout = 5.milliseconds,
                skipPolicy = SkipPolicy.maxSkips(3),
            ),
            writer = slowWriter,
        ).run()

        report shouldBeInstanceOf BatchReport.PartiallyCompleted::class
        report.stepReports.single().status shouldBeEqualTo BatchStatus.COMPLETED_WITH_SKIPS
        report.stepReports.single().skipCount shouldBeEqualTo 3L
        report.stepReports.single().writeCount shouldBeEqualTo 0L
        targetRows(database).size shouldBeEqualTo 0
    }

    @Test
    fun `cancellation persists STOPPED and restart resumes after the saved checkpoint`() = runSuspendIO {
        val database = preparedDatabase("cancel", 1..8)
        val firstWriteCompleted = CompletableDeferred<Unit>()
        val secondWriteStarted = CompletableDeferred<Unit>()
        val blockingWriter = CancellationWriter(
            delegate = jdbcTargetWriter(database),
            firstWriteCompleted = firstWriteCompleted,
            secondWriteStarted = secondWriteStarted,
        )
        val options = JdbcBatchOptions(jobName = "checkpointable-jdbc-cancel", chunkSize = 3)
        val running = async {
            checkpointableJdbcBatchJob(database, options, writer = blockingWriter).run()
        }

        firstWriteCompleted.await()
        secondWriteStarted.await()
        withTimeout(5.seconds) {
            while (stepCheckpoint(database) == null) {
                delay(1.milliseconds)
            }
        }

        running.cancel()
        assertFailsWith<CancellationException> { running.await() }

        jobStatus(database) shouldBeEqualTo BatchStatus.STOPPED
        stepStatus(database) shouldBeEqualTo BatchStatus.STOPPED
        stepCheckpoint(database)?.contains("\"className\":\"java.lang.Long\"") shouldBeEqualTo true
        stepCheckpoint(database)?.contains("\"payload\":\"3\"") shouldBeEqualTo true

        val restarted = runCheckpointableJdbcBatch(database, options)

        restarted shouldBeInstanceOf BatchReport.Success::class
        restarted.stepReports.single().status shouldBeEqualTo BatchStatus.COMPLETED
        targetRows(database).map { it.sourceId } shouldBeEqualTo (1L..8L).toList()
    }

    @Test
    fun `schema creates provider metadata and idempotent target primary key`() = runSuspendIO {
        val database = h2Database("schema")

        createJdbcBatchSchema(database)

        val tableCounts = transaction(database) {
            jdbcBatchMetadataTables.associate { table ->
                table.tableName to table.selectAll().count()
            }
        }
        tableCounts.keys shouldBeEqualTo jdbcBatchMetadataTables.map { it.tableName }.toSet()
        JdbcBatchTargetTable.primaryKey.columns.single() shouldBeEqualTo JdbcBatchTargetTable.sourceId
    }

    @Test
    fun `provider metadata CHECK constraint remains valid across JDBC sessions`() = runSuspendIO {
        val database = h2Database("check-constraint")
        createJdbcBatchSchema(database)

        val execution = ExposedJdbcBatchJobRepository(database, CheckpointJson.jackson3())
            .findOrCreateJobExecution("check-constraint-job", emptyMap())

        execution.status shouldBeEqualTo BatchStatus.RUNNING
    }

    @Test
    fun `options reject invalid execution boundaries`() {
        assertFailsWith<IllegalArgumentException> { JdbcBatchOptions(jobName = " ") }
        assertFailsWith<IllegalArgumentException> { JdbcBatchOptions(chunkSize = 0) }
        assertFailsWith<IllegalArgumentException> { JdbcBatchOptions(pageSize = 0) }
    }

    private fun preparedDatabase(name: String, values: IntRange): Database = h2Database(name).also { database ->
        createJdbcBatchSchema(database)
        seed(database, values)
    }

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
        JdbcBatchTargetTable.selectAll().map { row ->
            JdbcTargetRecord(
                sourceId = row[JdbcBatchTargetTable.sourceId],
                sourceName = row[JdbcBatchTargetTable.sourceName],
                transformedValue = row[JdbcBatchTargetTable.transformedValue],
            )
        }.sortedBy { it.sourceId }
    }

    private fun jobStatus(database: Database): BatchStatus = transaction(database) {
        BatchJobExecutionTable.selectAll().single()[BatchJobExecutionTable.status]
    }

    private fun stepStatus(database: Database): BatchStatus = transaction(database) {
        BatchStepExecutionTable.selectAll().single()[BatchStepExecutionTable.status]
    }

    private fun stepCheckpoint(database: Database): String? = transaction(database) {
        BatchStepExecutionTable.selectAll().single()[BatchStepExecutionTable.checkpoint]
    }

    private class FailOnceWriter(
        private val delegate: BatchWriter<JdbcTargetRecord>,
    ) : BatchWriter<JdbcTargetRecord> {
        var attempts: Int = 0
            private set

        override suspend fun open() = delegate.open()

        override suspend fun write(items: List<JdbcTargetRecord>) {
            attempts++
            if (attempts == 2) {
                throw IllegalStateException("fail the second chunk once")
            }
            delegate.write(items)
        }

        override suspend fun close() = delegate.close()
    }

    private class FailFirstWriter(
        private val delegate: BatchWriter<JdbcTargetRecord>,
    ) : BatchWriter<JdbcTargetRecord> {
        var attempts: Int = 0
            private set

        override suspend fun open() = delegate.open()

        override suspend fun write(items: List<JdbcTargetRecord>) {
            attempts++
            if (attempts == 1) {
                throw IllegalStateException("fail the first write")
            }
            delegate.write(items)
        }

        override suspend fun close() = delegate.close()
    }

    private class SlowWriter(
        private val delegate: BatchWriter<JdbcTargetRecord>,
        private val writeDelay: Duration,
    ) : BatchWriter<JdbcTargetRecord> {
        override suspend fun open() = delegate.open()

        override suspend fun write(items: List<JdbcTargetRecord>) {
            delay(writeDelay)
            delegate.write(items)
        }

        override suspend fun close() = delegate.close()
    }

    private class CancellationWriter(
        private val delegate: BatchWriter<JdbcTargetRecord>,
        private val firstWriteCompleted: CompletableDeferred<Unit>,
        private val secondWriteStarted: CompletableDeferred<Unit>,
    ) : BatchWriter<JdbcTargetRecord> {
        private var calls = 0

        override suspend fun open() = delegate.open()

        override suspend fun write(items: List<JdbcTargetRecord>) {
            calls++
            if (calls == 1) {
                delegate.write(items)
                firstWriteCompleted.complete(Unit)
            } else {
                secondWriteStarted.complete(Unit)
                awaitCancellation()
            }
        }

        override suspend fun close() = delegate.close()
    }
}
