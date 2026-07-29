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
 * StarRocks OLAP 워크숍에서 사용하는 타입화된 애플리케이션 프로필이다.
 *
 * 이 프로필은 StarRocks 연결을 열지 않고도 JDBC 경계를 명시한다.
 * 로컬 구조 테스트 중에는 실제 연결을 만들지 않는다.
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
     * 실제 검증 실행에서 사용하는 StarRocks Connector/J URL을 반환한다.
     */
    fun jdbcUrl(): String =
        "jdbc:starrocks://$host:$port/$catalog.$database"

    /**
     * 추가 driver property를 공개 bluetape4k StarRocks API로 변환한다.
     */
    fun toConnectionOptions(): StarRocksConnectionOptions =
        StarRocksConnectionOptions(extraProperties = extraProperties)

    /**
     * README와 테스트 assertion을 위해 driver property 경계를 노출한다.
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
 * 기본 로컬 StarRocks 분석 프로필을 반환한다.
 */
fun defaultStarRocksAnalyticsProfile(): StarRocksAnalyticsProfile =
    StarRocksAnalyticsProfile()

/**
 * OLTP 스토리지에서 OLAP rollup으로 투영되는 주문 이벤트이다.
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
 * StarRocks fact table에 적재할 수 있는 일별 지역 매출 rollup이다.
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
 * 결정적인 프로젝션 테스트에 사용하는 로컬 OLTP 스타일 픽스처 테이블이다.
 */
object LocalOrderEvents: org.jetbrains.exposed.v1.core.Table("local_order_events") {
    val orderId = long("order_id")
    val region = varchar("region", 32)
    val eventDate = varchar("event_date", 10)
    val revenue = decimal("revenue", precision = 12, scale = 2)

    override val primaryKey: PrimaryKey = PrimaryKey(orderId)
}

/**
 * 프로젝션된 OLAP rollup을 위한 StarRocks 대상 테이블 형태이다.
 */
object StarRocksRegionalSalesRollups: StarRocksTable("starrocks_regional_sales_rollups") {
    val region = varchar("region", 32)
    val eventDate = varchar("event_date", 10)
    val orderCount = long("order_count")
    val grossRevenue = decimal("gross_revenue", precision = 14, scale = 2)
}

/**
 * SQL 렌더링과 집계 테스트를 위한 로컬 H2 데이터베이스를 생성한다.
 */
fun createLocalProjectionDatabase(name: String = "starrocks_projection"): Database {
    name.requireNotBlank("name")

    return Database.connect(
        url = "jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        driver = "org.h2.Driver",
    )
}

/**
 * StarRocks rollup DDL을 실행하지 않고 로컬에서 생성한다.
 */
fun buildStarRocksRollupDdl(): String =
    transaction(createLocalProjectionDatabase("starrocks_rollup_ddl")) {
        StarRocksRegionalSalesRollups.createStatement().single()
    }

/**
 * 프로젝션 테스트를 위한 결정적인 로컬 주문 이벤트를 삽입한다.
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
 * 실제 StarRocks 검증 전에 사용하는 일별 지역 매출 쿼리를 구성한다.
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
 * 독자가 프로젝션 형태를 확인할 수 있도록 rollup SQL을 로컬에서 생성한다.
 */
fun generateDailyRegionalRevenueSql(): String =
    transaction(createLocalProjectionDatabase("starrocks_rollup_sql")) {
        buildDailyRegionalRevenueQuery().prepareSQL(this, prepared = false)
    }

/**
 * 로컬 주문 이벤트를 일별 지역 매출 rollup으로 프로젝션한다.
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
