package exposed.examples.duckdb.analytics

import io.bluetape4k.exposed.duckdb.DuckDBDatabase
import io.bluetape4k.exposed.duckdb.queryFlow
import io.bluetape4k.exposed.duckdb.suspendTransaction
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.duckdb.DuckDBConnection
import java.io.Serializable
import java.math.BigDecimal
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

/**
 * OLTP 경계에서 embedded DuckDB 분석 파일로 복사되는 주문 이벤트이다.
 */
data class DuckDbOrderEvent(
    val orderId: Long,
    val region: String,
    val category: String,
    val eventDate: String,
    val amount: BigDecimal,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }

    init {
        region.requireNotBlank("region")
        category.requireNotBlank("category")
        eventDate.requireNotBlank("eventDate")
    }
}

/**
 * DuckDB 실행 후 애플리케이션에 반환되는 집계 분석 행이다.
 */
data class DailyCategorySales(
    val region: String,
    val category: String,
    val eventDate: String,
    val orderCount: Long,
    val grossAmount: BigDecimal,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * 이 embedded analytics 예제에서 사용하는 파일 기반 DuckDB 테이블이다.
 */
object DuckDbOrderEvents: org.jetbrains.exposed.v1.core.Table("duckdb_order_events") {
    val orderId = long("order_id")
    val region = varchar("region", 32)
    val category = varchar("category", 48)
    val eventDate = varchar("event_date", 10)
    val amount = decimal("amount", precision = 14, scale = 2)

    override val primaryKey: PrimaryKey = PrimaryKey(orderId)
}

/**
 * 워크숍 데이터베이스에서 사용하는 root DuckDB 연결을 소유한다.
 *
 * DuckDB는 일반 `jdbc:duckdb:` 연결마다 하나의 인메모리 catalog를 유지한다. 이
 * 파일 기반 워크숍은 root 연결을 열린 상태로 유지하고 Exposed에 중복
 * 연결을 제공해서 별도 트랜잭션에서도 같은 로컬 데이터베이스를 보게 한다.
 */
class DuckDbAnalyticsSession private constructor(
    val db: Database,
    private val rootConnection: DuckDBConnection,
): AutoCloseable {

    override fun close() {
        rootConnection.close()
    }

    companion object {
        /**
         * 파일 기반 DuckDB 세션을 열고 중복
         * 트랜잭션 연결로 공유한다.
         */
        fun file(path: Path): DuckDbAnalyticsSession {
            val databasePath = path.toString().requireNotBlank("path")

            Class.forName(DuckDBDatabase.DRIVER)

            val rootConnection = DriverManager
                .getConnection("jdbc:duckdb:$databasePath") as DuckDBConnection
            val db = Database.connect(
                getNewConnection = { WorkshopDuckDbConnectionWrapper(rootConnection.duplicate()) }
            )

            return DuckDbAnalyticsSession(db, rootConnection)
        }
    }
}

/**
 * 파일 기반 DuckDB 분석 세션을 연다.
 */
fun openDuckDbAnalyticsSession(path: Path): DuckDbAnalyticsSession =
    DuckDbAnalyticsSession.file(path)

/**
 * embedded DuckDB 파일에 워크숍 스키마를 생성한다.
 */
suspend fun createDuckDbAnalyticsSchema(db: Database) {
    suspendTransaction(db) {
        SchemaUtils.create(DuckDbOrderEvents)
    }
}

/**
 * embedded DuckDB 파일에 결정적인 픽스처 이벤트를 삽입한다.
 */
suspend fun seedDuckDbOrderEvents(db: Database, events: List<DuckDbOrderEvent>) {
    events.requireNotEmpty("events")

    suspendTransaction(db) {
        SchemaUtils.create(DuckDbOrderEvents)
        DuckDbOrderEvents.batchInsert(events) { event ->
            this[DuckDbOrderEvents.orderId] = event.orderId
            this[DuckDbOrderEvents.region] = event.region
            this[DuckDbOrderEvents.category] = event.category
            this[DuckDbOrderEvents.eventDate] = event.eventDate
            this[DuckDbOrderEvents.amount] = event.amount
        }
    }
}

/**
 * DuckDB가 로컬에서 실행할 일별 카테고리 매출 쿼리를 구성한다.
 */
fun buildDailyCategorySalesQuery(): Query {
    val orderCount = DuckDbOrderEvents.orderId.count()
    val grossAmount = DuckDbOrderEvents.amount.sum()

    return DuckDbOrderEvents
        .select(
            DuckDbOrderEvents.region,
            DuckDbOrderEvents.category,
            DuckDbOrderEvents.eventDate,
            orderCount,
            grossAmount,
        )
        .groupBy(
            DuckDbOrderEvents.region,
            DuckDbOrderEvents.category,
            DuckDbOrderEvents.eventDate,
        )
        .orderBy(
            DuckDbOrderEvents.region to SortOrder.ASC,
            DuckDbOrderEvents.category to SortOrder.ASC,
            DuckDbOrderEvents.eventDate to SortOrder.ASC,
        )
}

/**
 * README가 정확한 쿼리 형태를 보여 줄 수 있도록 분석 SQL을 렌더링한다.
 */
fun generateDailyCategorySalesSql(db: Database): String =
    transaction(db) {
        buildDailyCategorySalesQuery().prepareSQL(this, prepared = false)
    }

/**
 * DuckDB 안에서 집계 쿼리를 실행하고 materialized row를 반환한다.
 */
suspend fun projectDailyCategorySales(db: Database): List<DailyCategorySales> =
    suspendTransaction(db) {
        buildDailyCategorySalesQuery().map(::toDailyCategorySales)
    }

/**
 * DuckDB 집계 결과를 코루틴 Flow로 노출한다.
 *
 * `queryFlow`는 먼저 Exposed 트랜잭션 내부에서 row를 materialize한다. Flow
 * 경계는 애플리케이션 파이프라인에 유용하지만 JDBC row-by-row
 * 스트리밍을 보장한다는 뜻은 아니다.
 */
fun streamDailyCategorySales(
    db: Database,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
): Flow<DailyCategorySales> =
    queryFlow(db, dispatcher) {
        buildDailyCategorySalesQuery().map(::toDailyCategorySales)
    }

/**
 * 같은 파일 기반 DuckDB 데이터베이스에서 별도 Exposed 트랜잭션으로 이벤트 수를 계산한다.
 */
suspend fun countPersistedOrderEvents(db: Database): Long =
    suspendTransaction(db) {
        DuckDbOrderEvents.selectAll().count()
    }

private fun toDailyCategorySales(row: ResultRow): DailyCategorySales {
    val orderCount = DuckDbOrderEvents.orderId.count()
    val grossAmount = DuckDbOrderEvents.amount.sum()

    return DailyCategorySales(
        region = row[DuckDbOrderEvents.region],
        category = row[DuckDbOrderEvents.category],
        eventDate = row[DuckDbOrderEvents.eventDate],
        orderCount = row[orderCount],
        grossAmount = row[grossAmount] ?: BigDecimal.ZERO,
    )
}

@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
private class WorkshopDuckDbConnectionWrapper(private val conn: Connection): Connection by conn {

    override fun prepareStatement(sql: String, autoGeneratedKeys: Int): PreparedStatement =
        conn.prepareStatement(sql)

    override fun prepareStatement(sql: String, columnIndexes: IntArray): PreparedStatement =
        conn.prepareStatement(sql)

    override fun prepareStatement(sql: String, columnNames: Array<out String>): PreparedStatement =
        conn.prepareStatement(sql)
}
