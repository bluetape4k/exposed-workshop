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
 * Order event copied from an OLTP boundary into an embedded DuckDB analytics file.
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
 * Aggregated analytical row returned to the application after DuckDB execution.
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
 * File-backed DuckDB table used by this embedded analytics example.
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
 * Owns the root DuckDB connection used by the workshop database.
 *
 * DuckDB keeps one in-memory catalog per plain `jdbc:duckdb:` connection. This
 * file-backed workshop keeps a root connection open and gives Exposed duplicate
 * connections so separate transactions observe the same local database.
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
         * Opens a file-backed DuckDB session and shares it through duplicated
         * transaction connections.
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
 * Opens a file-backed DuckDB analytics session.
 */
fun openDuckDbAnalyticsSession(path: Path): DuckDbAnalyticsSession =
    DuckDbAnalyticsSession.file(path)

/**
 * Creates the workshop schema in the embedded DuckDB file.
 */
suspend fun createDuckDbAnalyticsSchema(db: Database) {
    suspendTransaction(db) {
        SchemaUtils.create(DuckDbOrderEvents)
    }
}

/**
 * Inserts deterministic fixture events into the embedded DuckDB file.
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
 * Builds the daily category sales query that DuckDB executes locally.
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
 * Renders the analytical SQL so the README can show the exact query shape.
 */
fun generateDailyCategorySalesSql(db: Database): String =
    transaction(db) {
        buildDailyCategorySalesQuery().prepareSQL(this, prepared = false)
    }

/**
 * Executes the aggregate query inside DuckDB and returns materialized rows.
 */
suspend fun projectDailyCategorySales(db: Database): List<DailyCategorySales> =
    suspendTransaction(db) {
        buildDailyCategorySalesQuery().map(::toDailyCategorySales)
    }

/**
 * Exposes DuckDB aggregate results as a coroutine Flow.
 *
 * `queryFlow` materializes rows inside the Exposed transaction first. The Flow
 * boundary is useful for application pipelines, not a claim of JDBC row-by-row
 * streaming.
 */
fun streamDailyCategorySales(
    db: Database,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
): Flow<DailyCategorySales> =
    queryFlow(db, dispatcher) {
        buildDailyCategorySalesQuery().map(::toDailyCategorySales)
    }

/**
 * Counts events in a separate Exposed transaction on the same file-backed DuckDB database.
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
