package exposed.examples.trino.options

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale

class TrinoSessionOptionsWorkshopTest {

    @Test
    fun `default analytical profile maps to typed Trino connection options`() {
        val profile = defaultTrinoAnalyticsProfile()
        val options = profile.toConnectionOptions()

        options.explicitPrepare shouldBeEqualTo false
        options.encoding shouldBeEqualTo "json+zstd"
        options.validateConnection shouldBeEqualTo true
        options.source shouldBeEqualTo "exposed-workshop"
        options.clientTags shouldBeEqualTo listOf("exposed", "analytics", "workshop")
        options.sessionProperties shouldBeEqualTo mapOf(
            "join_distribution_type" to "AUTOMATIC",
            "query_max_execution_time" to "5m",
        )
    }

    @Test
    fun `profile exposes a stable JDBC property preview without raw strings in caller code`() {
        val properties = defaultTrinoAnalyticsProfile().jdbcPropertyPreview(user = "analyst")

        properties["user"] shouldBeEqualTo "analyst"
        properties["explicitPrepare"] shouldBeEqualTo "false"
        properties["encoding"] shouldBeEqualTo "json+zstd"
        properties["validateConnection"] shouldBeEqualTo "true"
        properties["source"] shouldBeEqualTo "exposed-workshop"
        properties["clientTags"] shouldBeEqualTo "exposed,analytics,workshop"
        properties["sessionProperties"] shouldBeEqualTo
            "join_distribution_type=AUTOMATIC,query_max_execution_time=5m"
    }

    @Test
    fun `unsafe Trino option values fail before a JDBC connection is attempted`() {
        assertFailsWith<IllegalArgumentException> {
            TrinoWorkshopConnectionProfile(catalog = "")
        }
        assertFailsWith<IllegalArgumentException> {
            TrinoWorkshopConnectionProfile(schema = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            TrinoWorkshopConnectionProfile(source = "")
        }
        assertFailsWith<IllegalArgumentException> {
            TrinoWorkshopConnectionProfile(clientTags = listOf("analytics", ""))
        }
        assertFailsWith<IllegalArgumentException> {
            TrinoWorkshopConnectionProfile(sessionProperties = mapOf("" to "AUTOMATIC"))
        }
    }

    @Test
    fun `generated SQL can be wrapped in an EXPLAIN request for pushdown inspection`() {
        val sql = generateRegionalTopOrdersSql(minimumRevenue = BigDecimal("25.00"))
        val explain = buildExplainRequest(sql)
        val normalized = explain.uppercase(Locale.ROOT)

        normalized shouldContain "EXPLAIN"
        normalized shouldContain "SELECT"
        normalized shouldContain "FROM WAREHOUSE_ORDERS"
        normalized shouldContain "WHERE"
        normalized shouldContain "ORDER BY"
        normalized shouldContain "LIMIT 10"
    }
}
