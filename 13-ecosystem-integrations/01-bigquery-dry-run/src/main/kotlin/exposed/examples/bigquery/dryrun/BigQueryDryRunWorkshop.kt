package exposed.examples.bigquery.dryrun

import com.google.api.services.bigquery.model.QueryResponse
import io.bluetape4k.exposed.bigquery.BigQueryContext
import io.bluetape4k.exposed.bigquery.BigQueryQueryOptions
import io.bluetape4k.exposed.bigquery.BigQueryQueryPriority
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.select
import java.math.BigDecimal

/**
 * BigQuery 드라이런 워크숍에서 사용하는 이벤트 테이블이다.
 *
 * 이 테이블은 작은 분석 이벤트 스트림을 모델링한다. 용도는
 * SQL을 BigQuery 드라이런 요청으로 보내기 전에 Exposed가 SQL을 생성하게 하는 데 한정된다.
 */
object Events: Table("events") {
    val eventId = long("event_id")
    val region = varchar("region", 32)
    val eventType = varchar("event_type", 64)
    val revenue = decimal("revenue", precision = 12, scale = 2)
    val occurredAt = long("occurred_at_epoch_ms")
}

/**
 * 지역별 매출 조회 모델 쿼리를 구성한다.
 *
 * 이 쿼리는 과금 대상 이벤트를 지역별로 그룹화하고 생성 SQL을 안정적으로 유지한다.
 * 그래서 실제 warehouse에 의존하지 않고도 워크숍 assertion을 수행할 수 있다.
 */
fun buildRegionalRevenueQuery(
    minimumRevenue: BigDecimal = BigDecimal("10.00"),
): Query =
    Events
        .select(Events.region, Events.eventId.count())
        .where { Events.revenue greaterEq minimumRevenue }
        .groupBy(Events.region)
        .orderBy(Events.region)

/**
 * 워크숍에서 사용하는 기본 BigQuery 드라이런 옵션을 반환한다.
 *
 * 이 옵션은 과금 바이트 상한을 설정하고, 워크숍 label을 붙이며, batch priority를 사용하고,
 * US location을 선택한다. `BigQueryContext.validateQuery`는 요청을
 * REST client에 도달하기 전에 드라이런으로 전환한다.
 */
fun defaultDryRunOptions(): BigQueryQueryOptions =
    BigQueryQueryOptions(
        maximumBytesBilled = 1_000_000L,
        labels = mapOf("workshop" to "bigquery-dry-run"),
        priority = BigQueryQueryPriority.BATCH,
        location = "US",
        timeoutMs = 5_000L,
    )

/**
 * BigQuery 드라이런 의미론으로 지역별 매출 쿼리를 검증한다.
 *
 * 전달된 [context]가 BigQuery REST client를 소유한다. 테스트는 모의
 * client를 제공하므로 기본 워크숍 명령은 요청 구성을 검증한다.
 * 인증 정보, 네트워크 접근, 과금 쿼리 실행 없이 검증이 끝난다.
 */
fun validateRegionalRevenueDryRun(
    context: BigQueryContext,
    options: BigQueryQueryOptions = defaultDryRunOptions(),
    minimumRevenue: BigDecimal = BigDecimal("10.00"),
): QueryResponse =
    context.validateQuery(buildRegionalRevenueQuery(minimumRevenue), options)
