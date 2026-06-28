package exposed.examples.bigquery.dryrun

import com.google.api.services.bigquery.Bigquery
import com.google.api.services.bigquery.Bigquery.Jobs
import com.google.api.services.bigquery.model.ErrorProto
import com.google.api.services.bigquery.model.QueryRequest
import com.google.api.services.bigquery.model.QueryResponse
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.exposed.bigquery.BigQueryContext
import io.bluetape4k.exposed.bigquery.BigQueryQueryException
import io.bluetape4k.exposed.bigquery.BigQueryQueryPriority
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import java.util.*

class BigQueryDryRunWorkshopTest {

    @Test
    fun `generated query dry run maps query job options without credentials`() {
        val fixture = bigQueryFixture(QueryResponse().setJobComplete(true))
        val context = BigQueryContext.create(
            bigquery = fixture.bigquery,
            projectId = PROJECT_ID,
            datasetId = DATASET_ID,
        )

        val response = validateRegionalRevenueDryRun(context)

        response.jobComplete shouldBeEqualTo true

        val request = fixture.request.captured
        request.dryRun shouldBeEqualTo true
        request.useLegacySql shouldBeEqualTo false
        request.defaultDataset.projectId shouldBeEqualTo PROJECT_ID
        request.defaultDataset.datasetId shouldBeEqualTo DATASET_ID
        request.maximumBytesBilled shouldBeEqualTo MAX_BYTES_BILLED
        request.labels shouldBeEqualTo mapOf("workshop" to "bigquery-dry-run")
        request.get("priority") shouldBeEqualTo BigQueryQueryPriority.BATCH.apiValue
        request.location shouldBeEqualTo LOCATION
        request.timeoutMs shouldBeEqualTo TIMEOUT_MS

        val sql = request.query.uppercase(Locale.ROOT)
        sql shouldContain "SELECT"
        sql shouldContain "FROM EVENTS"
        sql shouldContain "GROUP BY"
        sql shouldContain "ORDER BY"
    }

    @Test
    fun `dry run surfaces BigQuery validation errors without execution`() {
        val fixture = bigQueryFixture(
            QueryResponse().setErrors(
                listOf(ErrorProto().setReason("invalidQuery").setMessage("Unknown column region_name"))
            )
        )
        val context = BigQueryContext.create(
            bigquery = fixture.bigquery,
            projectId = PROJECT_ID,
            datasetId = DATASET_ID,
        )

        val error = assertFailsWith<BigQueryQueryException> {
            validateRegionalRevenueDryRun(context)
        }

        error.message.orEmpty() shouldContain "Unknown column region_name"
        fixture.request.isCaptured.shouldBeTrue()
        fixture.request.captured.dryRun shouldBeEqualTo true
    }

    private fun bigQueryFixture(response: QueryResponse): BigQueryFixture {
        val request = slot<QueryRequest>()
        val queryCall = mockk<Jobs.Query>(relaxed = true) {
            every { execute() } returns response
        }
        val jobs = mockk<Jobs>(relaxed = true) {
            every { query(PROJECT_ID, capture(request)) } returns queryCall
        }
        val bigquery = mockk<Bigquery>(relaxed = true) {
            every { jobs() } returns jobs
        }
        return BigQueryFixture(bigquery, request)
    }

    private data class BigQueryFixture(
        val bigquery: Bigquery,
        val request: io.mockk.CapturingSlot<QueryRequest>,
    )

    private companion object {
        private const val PROJECT_ID = "analytics-project"
        private const val DATASET_ID = "workshop_events"
        private const val LOCATION = "US"
        private const val MAX_BYTES_BILLED = 1_000_000L
        private const val TIMEOUT_MS = 5_000L
    }
}
