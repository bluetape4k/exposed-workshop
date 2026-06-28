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
 * Event table used by the BigQuery dry-run workshop.
 *
 * The table models a small analytical event stream. It is used only to let
 * Exposed generate SQL before the SQL is sent to BigQuery as a dry-run request.
 */
object Events: Table("events") {
    val eventId = long("event_id")
    val region = varchar("region", 32)
    val eventType = varchar("event_type", 64)
    val revenue = decimal("revenue", precision = 12, scale = 2)
    val occurredAt = long("occurred_at_epoch_ms")
}

/**
 * Builds the regional revenue read-model query.
 *
 * The query groups billable events by region and keeps the generated SQL stable
 * enough for workshop assertions without depending on a live warehouse.
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
 * Returns the default BigQuery dry-run options used by the workshop.
 *
 * The options cap billed bytes, attach a workshop label, use batch priority,
 * and select the US location. `BigQueryContext.validateQuery` turns the request
 * into a dry run before it reaches the REST client.
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
 * Validates the regional revenue query with BigQuery dry-run semantics.
 *
 * The supplied [context] owns the BigQuery REST client. Tests provide a mocked
 * client, so the default workshop command validates request construction
 * without credentials, network access, or billable query execution.
 */
fun validateRegionalRevenueDryRun(
    context: BigQueryContext,
    options: BigQueryQueryOptions = defaultDryRunOptions(),
    minimumRevenue: BigDecimal = BigDecimal("10.00"),
): QueryResponse =
    context.validateQuery(buildRegionalRevenueQuery(minimumRevenue), options)
