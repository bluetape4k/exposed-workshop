package exposed.examples.duckdb.analytics

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.junit5.coroutines.runSuspendIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.math.BigDecimal
import java.nio.file.Path
import java.util.Locale

class DuckDbEmbeddedAnalyticsWorkshopTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `file backed DuckDB keeps rows across Exposed transactions`() = runSuspendIO {
        val dbPath = tempDir.resolve("analytics.duckdb")
        openDuckDbAnalyticsSession(dbPath).use { session ->
            val db = session.db

            createDuckDbAnalyticsSchema(db)
            seedDuckDbOrderEvents(db, sampleEvents())

            countPersistedOrderEvents(db) shouldBeEqualTo 4L
        }
    }

    @Test
    fun `embedded DuckDB aggregates daily category sales`() = runSuspendIO {
        openDuckDbAnalyticsSession(tempDir.resolve("sales.duckdb")).use { session ->
            val db = session.db
            seedDuckDbOrderEvents(db, sampleEvents())

            projectDailyCategorySales(db) shouldBeEqualTo listOf(
                DailyCategorySales(
                    region = "apac",
                    category = "book",
                    eventDate = "2026-06-29",
                    orderCount = 2L,
                    grossAmount = BigDecimal("52.50"),
                ),
                DailyCategorySales(
                    region = "apac",
                    category = "tool",
                    eventDate = "2026-06-29",
                    orderCount = 1L,
                    grossAmount = BigDecimal("19.25"),
                ),
                DailyCategorySales(
                    region = "emea",
                    category = "book",
                    eventDate = "2026-06-30",
                    orderCount = 1L,
                    grossAmount = BigDecimal("31.00"),
                ),
            )
        }
    }

    @Test
    fun `queryFlow exposes materialized DuckDB analytics rows as a coroutine pipeline`() = runSuspendIO {
        openDuckDbAnalyticsSession(tempDir.resolve("flow.duckdb")).use { session ->
            val db = session.db
            seedDuckDbOrderEvents(db, sampleEvents())

            val rows = streamDailyCategorySales(db, Dispatchers.Unconfined).toList()

            rows.map { it.region to it.category } shouldBeEqualTo listOf(
                "apac" to "book",
                "apac" to "tool",
                "emea" to "book",
            )
        }
    }

    @Test
    fun `rendered SQL keeps aggregation grouping and ordering visible`() = runSuspendIO {
        openDuckDbAnalyticsSession(tempDir.resolve("sql.duckdb")).use { session ->
            val db = session.db
            createDuckDbAnalyticsSchema(db)

            val normalized = generateDailyCategorySalesSql(db).uppercase(Locale.ROOT)

            normalized shouldContain "SELECT"
            normalized shouldContain "DUCKDB_ORDER_EVENTS"
            normalized shouldContain "COUNT"
            normalized shouldContain "SUM"
            normalized shouldContain "GROUP BY"
            normalized shouldContain "ORDER BY"
        }
    }

    @Test
    fun `invalid events fail before the DuckDB insert boundary`() {
        assertFailsWith<IllegalArgumentException> {
            DuckDbOrderEvent(
                orderId = 1L,
                region = "",
                category = "book",
                eventDate = "2026-06-29",
                amount = BigDecimal("10.00"),
            )
        }
    }

    @Test
    fun `empty fixture input fails before touching DuckDB`() = runSuspendIO {
        openDuckDbAnalyticsSession(tempDir.resolve("empty.duckdb")).use { session ->
            val db = session.db

            assertFailsWith<IllegalArgumentException> {
                seedDuckDbOrderEvents(db, emptyList())
            }
        }
    }

    @Test
    fun `created schema can be queried with Exposed DSL`() = runSuspendIO {
        openDuckDbAnalyticsSession(tempDir.resolve("schema.duckdb")).use { session ->
            val db = session.db
            createDuckDbAnalyticsSchema(db)

            io.bluetape4k.exposed.duckdb.suspendTransaction(db) {
                DuckDbOrderEvents.selectAll().count()
            } shouldBeEqualTo 0L
        }
    }

    private fun sampleEvents(): List<DuckDbOrderEvent> =
        listOf(
            DuckDbOrderEvent(
                orderId = 1001L,
                region = "apac",
                category = "book",
                eventDate = "2026-06-29",
                amount = BigDecimal("42.50"),
            ),
            DuckDbOrderEvent(
                orderId = 1002L,
                region = "apac",
                category = "book",
                eventDate = "2026-06-29",
                amount = BigDecimal("10.00"),
            ),
            DuckDbOrderEvent(
                orderId = 1003L,
                region = "apac",
                category = "tool",
                eventDate = "2026-06-29",
                amount = BigDecimal("19.25"),
            ),
            DuckDbOrderEvent(
                orderId = 1004L,
                region = "emea",
                category = "book",
                eventDate = "2026-06-30",
                amount = BigDecimal("31.00"),
            ),
        )
}
