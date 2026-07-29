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
 * 애플리케이션에서 사용하는 검증된 Trino 분석 연결 프로필이다.
 *
 * 이 프로필은 catalog, schema, source, tag, session property를 애플리케이션 코드에서 타입으로 보존하고,
 * bluetape4k-exposed의
 * [TrinoConnectionOptions]로 변환한다.
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
     * 이 워크숍 프로필을 공개 bluetape4k Trino options API로 변환한다.
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
     * JDBC property 이름을 로컬에서만 확인하는 안정적인 preview를 반환한다.
     *
     * 실제 driver property 변환은 라이브러리 내부의 `TrinoConnectionOptions`가 책임진다.
     * 이 preview는 워크숍 assertion과 README 예제를 위한 것이며,
     * 사용자가 JDBC 연결을 열지 않고도 property 경계를 확인할 수 있게 한다.
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
 * Trino 워크숍에서 사용하는 기본 분석 프로필을 반환한다.
 */
fun defaultTrinoAnalyticsProfile(): TrinoWorkshopConnectionProfile =
    TrinoWorkshopConnectionProfile()

/**
 * 푸시다운에 적합한 분석 쿼리를 생성하는 데 사용하는 주문 테이블이다.
 */
object WarehouseOrders: Table("warehouse_orders") {
    val orderId = long("order_id")
    val region = varchar("region", 32)
    val customerTier = varchar("customer_tier", 32)
    val revenue = decimal("revenue", precision = 12, scale = 2)
}

/**
 * Trino EXPLAIN 검사에 적합한 top-N 분석 쿼리 형태를 구성한다.
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
 * Trino cluster에 접속하지 않고 로컬에서 SQL을 생성한다.
 *
 * H2는 Exposed에 SQL 생성을 위한 JDBC 트랜잭션 컨텍스트를 제공하는 용도로만 사용된다.
 * 반환된 SQL은 실행하지 않는다.
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
 * 생성된 SQL을 Trino EXPLAIN 요청으로 감싼다.
 *
 * 실제 푸시다운 지원 여부는 커넥터별로 다르다. 워크숍은 요청이
 * 안정적인 predicate, projection, ordering, top-N 신호를 유지하는지 검증한다.
 * 실제 Trino catalog는 이 신호를 `EXPLAIN`으로 검사할 수 있다.
 */
fun buildExplainRequest(sql: String): String {
    sql.requireNotBlank("sql")
    return "EXPLAIN\n$sql"
}
