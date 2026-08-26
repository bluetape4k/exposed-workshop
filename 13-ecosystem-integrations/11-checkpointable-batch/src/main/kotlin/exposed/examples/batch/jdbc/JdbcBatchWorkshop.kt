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
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/** JDBC batch workshop 입력 테이블입니다. */
object JdbcBatchSourceTable : Table("jdbc_batch_source") {
    /** 증가하는 keyset 읽기 기준입니다. */
    val id = long("id").autoIncrement()

    /** 원본 이름입니다. */
    val name = varchar("name", 255)

    /** 변환할 정수 값입니다. */
    val value = integer("value")

    override val primaryKey = PrimaryKey(id)
}

/** JDBC batch workshop 출력 테이블입니다. */
object JdbcBatchTargetTable : Table("jdbc_batch_target") {
    /** 재시작 시 중복을 드러내는 source key입니다. */
    val sourceId = long("source_id")

    /** 대문자로 변환한 원본 이름입니다. */
    val sourceName = varchar("source_name", 255)

    /** 두 배로 변환한 정수 값입니다. */
    val transformedValue = integer("transformed_value")

    override val primaryKey = PrimaryKey(sourceId)
}

/** 입력 테이블에서 읽는 불변 레코드입니다. */
data class JdbcSourceRecord(
    val id: Long,
    val name: String,
    val value: Int,
)

/** 출력 테이블에 쓰는 불변 레코드입니다. */
data class JdbcTargetRecord(
    val sourceId: Long,
    val sourceName: String,
    val transformedValue: Int,
)

/** JDBC batch 예제의 실행 옵션입니다. */
data class JdbcBatchOptions(
    /** 재시작 식별에 사용하는 job 이름입니다. */
    val jobName: String = "checkpointable-jdbc-batch",
    /** 같은 job 실행을 재사용할 때 사용하는 파라미터입니다. */
    val parameters: Map<String, Any> = mapOf("dataset" to "workshop"),
    /** 한 번에 writer로 전달할 아이템 수입니다. */
    val chunkSize: Int = 3,
    /** keyset reader가 한 번에 조회할 page 크기입니다. */
    val pageSize: Int = chunkSize,
    /** processor 또는 writer 예외를 허용할 정책입니다. */
    val skipPolicy: SkipPolicy = SkipPolicy.NONE,
    /** writer 실패 재시도 정책입니다. */
    val retryPolicy: RetryPolicy = RetryPolicy.NONE,
    /** writer 한 chunk의 commit timeout입니다. */
    val commitTimeout: Duration = BatchDefaults.COMMIT_TIMEOUT,
) {
    init {
        require(jobName.isNotBlank()) { "jobName must not be blank" }
        require(chunkSize > 0) { "chunkSize must be positive" }
        require(pageSize > 0) { "pageSize must be positive" }
    }
}

/** 예제가 생성하는 provider metadata와 source/target 테이블입니다. */
val jdbcBatchMetadataTables: List<Table> = listOf(
    BatchJobExecutionTable,
    BatchStepExecutionTable,
    JdbcBatchSourceTable,
    JdbcBatchTargetTable,
)

/** H2 또는 호출자가 지정한 JDBC database에 batch schema를 생성합니다. */
fun createJdbcBatchSchema(database: Database) {
    transaction(database) {
        SchemaUtils.create(*jdbcBatchMetadataTables.toTypedArray())
    }
}

/** 기본 workshop processor입니다. */
val defaultJdbcProcessor = BatchProcessor<JdbcSourceRecord, JdbcTargetRecord> { source ->
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

/** 기본 target table writer입니다. */
fun jdbcTargetWriter(database: Database): BatchWriter<JdbcTargetRecord> =
    ExposedJdbcBatchWriter(database, JdbcBatchTargetTable) { target ->
        this[JdbcBatchTargetTable.sourceId] = target.sourceId
        this[JdbcBatchTargetTable.sourceName] = target.sourceName
        this[JdbcBatchTargetTable.transformedValue] = target.transformedValue
    }

/** provider DSL을 직접 조합한 checkpointable JDBC batch job을 만듭니다. */
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

/** 기본 JDBC batch job을 실행합니다. */
suspend fun runCheckpointableJdbcBatch(
    database: Database,
    options: JdbcBatchOptions = JdbcBatchOptions(),
): BatchReport = checkpointableJdbcBatchJob(database, options).run()
