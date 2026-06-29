package exposed.examples.starrocks.olap

import io.bluetape4k.exposed.starrocks.StarRocksConnectionOptions
import io.bluetape4k.exposed.starrocks.StarRocksTable
import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.Serializable
import java.math.BigDecimal

/**
 * Typed application profile for the StarRocks OLAP workshop.
 *
 * The profile makes the JDBC boundary explicit without opening a StarRocks
 * connection during local structural tests.
 */
data class StarRocksAnalyticsProfile(
    val host: String = "localhost",
    val port: Int = 9030,
    val catalog: String = "default_catalog",
    val database: String = "analytics",
    val password: String = "",
    val extraProperties: Map<String, String> = emptyMap(),
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }

    init {
        host.requireNotBlank("host")
        port.requireInRange(1, 65535, "port")
        catalog.requireNotBlank("catalog")
        database.requireNotBlank("database")
        extraProperties.forEach { (key, value) ->
            key.requireNotBlank("extraProperties key")
            value.requireNotBlank("extraProperties[$key]")
        }
    }

    /**
     * Returns the StarRocks Connector/J URL that a real validation run uses.
     */
    fun jdbcUrl(): String =
        "jdbc:starrocks://$host:$port/$catalog.$database"

    /**
     * Converts extra driver properties to the public bluetape4k StarRocks API.
     */
    fun toConnectionOptions(): StarRocksConnectionOptions =
        StarRocksConnectionOptions(extraProperties = extraProperties)

    /**
     * Exposes the driver property boundary for README and test assertions.
     */
    fun jdbcPropertyPreview(user: String): Map<String, String> {
        user.requireNotBlank("user")

        return linkedMapOf(
            "user" to user,
            "password" to password,
        ) + extraProperties
    }
}

/**
 * Returns the default local StarRocks analytics profile.
 */
fun defaultStarRocksAnalyticsProfile(): StarRocksAnalyticsProfile =
    StarRocksAnalyticsProfile()

/**
 * Order event projected from OLTP storage into an OLAP rollup.
 */
data class OrderEvent(
    val orderId: Long,
    val region: String,
    val eventDate: String,
    val revenue: BigDecimal,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }

    init {
        region.requireNotBlank("region")
        eventDate.requireNotBlank("eventDate")
    }
}

/**
 * Daily regional revenue rollup ready for a StarRocks fact table.
 */
data class RegionalRevenueRollup(
    val region: String,
    val eventDate: String,
    val orderCount: Long,
    val grossRevenue: BigDecimal,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Local OLTP-style fixture table used for deterministic projection tests.
 */
object LocalOrderEvents: org.jetbrains.exposed.v1.core.Table("local_order_events") {
    val orderId = long("order_id")
    val region = varchar("region", 32)
    val eventDate = varchar("event_date", 10)
    val revenue = decimal("revenue", precision = 12, scale = 2)

    override val primaryKey: PrimaryKey = PrimaryKey(orderId)
}

/**
 * StarRocks target table shape for the projected OLAP rollup.
 */
object StarRocksRegionalSalesRollups: StarRocksTable("starrocks_regional_sales_rollups") {
    val region = varchar("region", 32)
    val eventDate = varchar("event_date", 10)
    val orderCount = long("order_count")
    val grossRevenue = decimal("gross_revenue", precision = 14, scale = 2)
}

/**
 * Creates a local H2 database for SQL rendering and aggregation tests.
 */
fun createLocalProjectionDatabase(name: String = "starrocks_projection"): Database {
    name.requireNotBlank("name")

    return Database.connect(
        url = "jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        driver = "org.h2.Driver",
    )
}

/**
 * Generates the StarRocks rollup DDL locally without executing it.
 */
fun buildStarRocksRollupDdl(): String =
    transaction(createLocalProjectionDatabase("starrocks_rollup_ddl")) {
        StarRocksRegionalSalesRollups.createStatement().single()
    }

/**
 * Inserts deterministic local order events for projection tests.
 */
fun seedLocalOrderEvents(db: Database, events: List<OrderEvent>) {
    events.requireNotEmpty("events")

    transaction(db) {
        SchemaUtils.createMissingTablesAndColumns(LocalOrderEvents)
        LocalOrderEvents.batchInsert(events) { event ->
            this[LocalOrderEvents.orderId] = event.orderId
            this[LocalOrderEvents.region] = event.region
            this[LocalOrderEvents.eventDate] = event.eventDate
            this[LocalOrderEvents.revenue] = event.revenue
        }
    }
}

/**
 * Builds the daily regional revenue query used before real StarRocks validation.
 */
fun buildDailyRegionalRevenueQuery(): Query {
    val orderCount = LocalOrderEvents.orderId.count()
    val grossRevenue = LocalOrderEvents.revenue.sum()

    return LocalOrderEvents
        .select(
            LocalOrderEvents.region,
            LocalOrderEvents.eventDate,
            orderCount,
            grossRevenue,
        )
        .groupBy(LocalOrderEvents.region, LocalOrderEvents.eventDate)
        .orderBy(
            LocalOrderEvents.region to SortOrder.ASC,
            LocalOrderEvents.eventDate to SortOrder.ASC,
        )
}

/**
 * Generates the rollup SQL locally so readers can inspect projection shape.
 */
fun generateDailyRegionalRevenueSql(): String =
    transaction(createLocalProjectionDatabase("starrocks_rollup_sql")) {
        buildDailyRegionalRevenueQuery().prepareSQL(this, prepared = false)
    }

/**
 * Projects local order events into daily regional revenue rollups.
 */
fun projectDailyRegionalRevenue(db: Database): List<RegionalRevenueRollup> {
    val orderCount = LocalOrderEvents.orderId.count()
    val grossRevenue = LocalOrderEvents.revenue.sum()

    return transaction(db) {
        LocalOrderEvents
            .select(
                LocalOrderEvents.region,
                LocalOrderEvents.eventDate,
                orderCount,
                grossRevenue,
            )
            .groupBy(LocalOrderEvents.region, LocalOrderEvents.eventDate)
            .orderBy(
                LocalOrderEvents.region to SortOrder.ASC,
                LocalOrderEvents.eventDate to SortOrder.ASC,
            )
            .map { row -> row.toRegionalRevenueRollup(orderCount, grossRevenue) }
    }
}

private fun ResultRow.toRegionalRevenueRollup(
    orderCount: org.jetbrains.exposed.v1.core.Expression<Long>,
    grossRevenue: org.jetbrains.exposed.v1.core.Expression<BigDecimal?>,
): RegionalRevenueRollup =
    RegionalRevenueRollup(
        region = this[LocalOrderEvents.region],
        eventDate = this[LocalOrderEvents.eventDate],
        orderCount = this[orderCount],
        grossRevenue = this[grossRevenue] ?: BigDecimal.ZERO.setScale(2),
    )
