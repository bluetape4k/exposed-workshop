package exposed.examples.trino.options

import io.bluetape4k.exposed.trino.TrinoConnectionOptions
import io.bluetape4k.support.requireNotBlank
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.Serializable
import java.math.BigDecimal

/**
 * Validated application-facing Trino analytical connection profile.
 *
 * The profile keeps catalog, schema, source, tags, and session properties typed
 * in application code before converting them to bluetape4k-exposed
 * [TrinoConnectionOptions].
 */
data class TrinoWorkshopConnectionProfile(
    val catalog: String = "hive",
    val schema: String = "analytics",
    val source: String = "exposed-workshop",
    val clientTags: List<String> = listOf("exposed", "analytics", "workshop"),
    val sessionProperties: Map<String, String> = mapOf(
        "join_distribution_type" to "AUTOMATIC",
        "query_max_execution_time" to "5m",
    ),
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }

    init {
        catalog.requireNotBlank("catalog")
        schema.requireNotBlank("schema")
        source.requireNotBlank("source")
        clientTags.forEachIndexed { index, tag ->
            tag.requireNotBlank("clientTags[$index]")
        }
        sessionProperties.forEach { (key, value) ->
            key.requireNotBlank("sessionProperties key")
            value.requireNotBlank("sessionProperties[$key]")
        }
    }

    /**
     * Converts this workshop profile to the public bluetape4k Trino options API.
     */
    fun toConnectionOptions(): TrinoConnectionOptions =
        TrinoConnectionOptions(
            explicitPrepare = false,
            encoding = "json+zstd",
            validateConnection = true,
            source = source,
            clientTags = clientTags,
            sessionProperties = sessionProperties,
        )

    /**
     * Returns a stable, local-only preview of the JDBC property names.
     *
     * `TrinoConnectionOptions` owns the actual driver property conversion inside
     * the library. This preview is for workshop assertions and README examples,
     * so users can see the property boundary without opening a JDBC connection.
     */
    fun jdbcPropertyPreview(user: String): Map<String, String> {
        user.requireNotBlank("user")

        return linkedMapOf(
            "user" to user,
            "explicitPrepare" to "false",
            "encoding" to "json+zstd",
            "validateConnection" to "true",
            "source" to source,
            "clientTags" to clientTags.joinToString(","),
            "sessionProperties" to sessionProperties.entries.joinToString(",") { (key, value) -> "$key=$value" },
        )
    }
}

/**
 * Returns the default analytical profile used by the Trino workshop.
 */
fun defaultTrinoAnalyticsProfile(): TrinoWorkshopConnectionProfile =
    TrinoWorkshopConnectionProfile()

/**
 * Order table used to generate a pushdown-friendly analytical query.
 */
object WarehouseOrders: Table("warehouse_orders") {
    val orderId = long("order_id")
    val region = varchar("region", 32)
    val customerTier = varchar("customer_tier", 32)
    val revenue = decimal("revenue", precision = 12, scale = 2)
}

/**
 * Builds a top-N analytical query shape suitable for Trino EXPLAIN inspection.
 */
fun buildRegionalTopOrdersQuery(
    minimumRevenue: BigDecimal = BigDecimal("25.00"),
): Query =
    WarehouseOrders
        .select(WarehouseOrders.orderId, WarehouseOrders.region, WarehouseOrders.revenue)
        .where { WarehouseOrders.revenue greaterEq minimumRevenue }
        .orderBy(WarehouseOrders.revenue to SortOrder.DESC)
        .limit(10)

/**
 * Generates SQL locally without contacting a Trino cluster.
 *
 * H2 is used only to give Exposed a JDBC transaction context for SQL
 * generation. The returned SQL is not executed.
 */
fun generateRegionalTopOrdersSql(
    minimumRevenue: BigDecimal = BigDecimal("25.00"),
): String {
    val db = Database.connect(
        url = "jdbc:h2:mem:trino_session_options_sql;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        driver = "org.h2.Driver",
    )

    return transaction(db) {
        buildRegionalTopOrdersQuery(minimumRevenue).prepareSQL(this, prepared = false)
    }
}

/**
 * Wraps generated SQL in a Trino EXPLAIN request.
 *
 * Real pushdown support is connector-specific. The workshop verifies that the
 * request keeps stable predicate, projection, ordering, and top-N signals that
 * a real Trino catalog can inspect with `EXPLAIN`.
 */
fun buildExplainRequest(sql: String): String {
    sql.requireNotBlank("sql")
    return "EXPLAIN\n$sql"
}
