package exposed.examples.starrocks.olap

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldContain
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale

class StarRocksOlapWorkshopTest {

    @Test
    fun `analytics profile keeps the StarRocks JDBC boundary typed`() {
        val profile = defaultStarRocksAnalyticsProfile()
        val options = profile.toConnectionOptions()

        profile.jdbcUrl() shouldBeEqualTo
            "jdbc:starrocks://localhost:9030/default_catalog.analytics"
        profile.jdbcPropertyPreview(user = "analyst") shouldBeEqualTo mapOf(
            "user" to "analyst",
            "password" to "",
        )
        options.extraProperties shouldBeEqualTo emptyMap()
    }

    @Test
    fun `invalid analytics profile values fail before a JDBC connection is opened`() {
        assertFailsWith<IllegalArgumentException> {
            StarRocksAnalyticsProfile(host = "")
        }
        assertFailsWith<IllegalArgumentException> {
            StarRocksAnalyticsProfile(port = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            StarRocksAnalyticsProfile(catalog = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            StarRocksAnalyticsProfile(database = "")
        }
        assertFailsWith<IllegalArgumentException> {
            defaultStarRocksAnalyticsProfile().jdbcPropertyPreview(user = "")
        }
    }

    @Test
    fun `StarRocks rollup DDL is generated locally without starting StarRocks`() {
        val ddl = buildStarRocksRollupDdl()
        val normalized = ddl.uppercase(Locale.ROOT)

        normalized shouldContain "CREATE TABLE"
        normalized shouldContain "STARROCKS_REGIONAL_SALES_ROLLUPS"
        normalized shouldContain "ENGINE=OLAP"
        ddl shouldContain "\"replication_num\" = \"1\""
        normalized.contains("PRIMARY KEY").shouldBeFalse()
    }

    @Test
    fun `regional revenue SQL keeps projection aggregation and grouping visible`() {
        val sql = generateDailyRegionalRevenueSql()
        val normalized = sql.uppercase(Locale.ROOT)

        normalized shouldContain "SELECT"
        normalized shouldContain "LOCAL_ORDER_EVENTS"
        normalized shouldContain "SUM"
        normalized shouldContain "COUNT"
        normalized shouldContain "GROUP BY"
        normalized shouldContain "ORDER BY"
    }

    @Test
    fun `local projection fixture aggregates order events before StarRocks validation`() {
        val db = createLocalProjectionDatabase("starrocks_projection_${System.nanoTime()}")

        transaction(db) {
            SchemaUtils.create(LocalOrderEvents)
        }
        seedLocalOrderEvents(
            db = db,
            events = listOf(
                OrderEvent(orderId = 1001L, region = "apac", eventDate = "2026-06-29", revenue = BigDecimal("42.50")),
                OrderEvent(orderId = 1002L, region = "apac", eventDate = "2026-06-29", revenue = BigDecimal("17.25")),
                OrderEvent(orderId = 1003L, region = "emea", eventDate = "2026-06-29", revenue = BigDecimal("31.00")),
            ),
        )

        projectDailyRegionalRevenue(db) shouldBeEqualTo listOf(
            RegionalRevenueRollup(
                region = "apac",
                eventDate = "2026-06-29",
                orderCount = 2L,
                grossRevenue = BigDecimal("59.75"),
            ),
            RegionalRevenueRollup(
                region = "emea",
                eventDate = "2026-06-29",
                orderCount = 1L,
                grossRevenue = BigDecimal("31.00"),
            ),
        )
    }
}
