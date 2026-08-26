package exposed.examples.druid.queryonly

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.exposed.druid.DruidAvaticaSerialization
import io.bluetape4k.exposed.druid.DruidColumnMetadata
import io.bluetape4k.exposed.druid.DruidJdbc
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.ResultSet

class DruidQueryOnlyWorkshopTest {

    private val profile = DruidQueryProfile(
        avaticaEndpoint = "http://druid.test:8888/druid/v2/sql/avatica/",
        datasource = "wikipedia",
        schema = "druid",
        contextProperties = mapOf("sqlTimeZone" to "Etc/UTC"),
    )

    @BeforeEach
    fun setUp() {
        mockkObject(DruidJdbc)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(DruidJdbc)
    }

    @Test
    fun `profile maps Druid connection options without opening a connection`() {
        val options = profile.toConnectionOptions()

        options.avaticaEndpoint shouldBeEqualTo profile.avaticaEndpoint
        options.transparentReconnection shouldBeEqualTo true
        options.serialization shouldBeEqualTo DruidAvaticaSerialization.JSON
        options.jdbcUrl() shouldContain "transparent_reconnection=true"
        options.toProperties().getProperty("sqlTimeZone") shouldBeEqualTo "Etc/UTC"
    }

    @Test
    fun `default profile points to the local Druid Avatica endpoint`() {
        defaultDruidQueryProfile() shouldBeEqualTo DruidQueryProfile()
    }

    @Test
    fun `blank profile values fail before JDBC calls`() {
        assertFailsWith<IllegalArgumentException> { DruidQueryProfile(datasource = "") }
        assertFailsWith<IllegalArgumentException> { DruidQueryProfile(schema = " ") }
        assertFailsWith<IllegalArgumentException> {
            DruidQueryProfile(contextProperties = mapOf("" to "Etc/UTC"))
        }
        assertFailsWith<IllegalArgumentException> {
            DruidQueryProfile(avaticaEndpoint = "http://druid.test:8888/wrong/").toConnectionOptions()
        }
    }

    @Test
    fun `sync query delegates query-only SQL and returns mapped rows`() {
        val mapper = slot<(ResultSet) -> Long>()
        every { DruidJdbc.query<Long>(any(), any(), capture(mapper)) } returns listOf(7L)

        queryDatasourceRowCount(profile) shouldBeEqualTo listOf(7L)

        val resultSet = mockk<ResultSet>()
        every { resultSet.getLong("row_count") } returns 7L
        mapper.captured(resultSet) shouldBeEqualTo 7L

        verify(exactly = 1) {
            DruidJdbc.query<Long>(
                sql = "SELECT COUNT(*) AS row_count FROM \"wikipedia\"",
                options = profile.toConnectionOptions(),
                mapper = any(),
            )
        }
    }

    @Test
    fun `suspend query delegates the supplied dispatcher`() = runSuspendIO {
        val mapper = slot<(ResultSet) -> Long>()
        coEvery { DruidJdbc.querySuspend<Long>(any(), any(), any(), capture(mapper)) } returns listOf(11L)

        queryDatasourceRowCountSuspend(profile, dispatcher = Dispatchers.Unconfined) shouldBeEqualTo listOf(11L)

        val resultSet = mockk<ResultSet>()
        every { resultSet.getLong("row_count") } returns 11L
        mapper.captured(resultSet) shouldBeEqualTo 11L

        coVerify(exactly = 1) {
            DruidJdbc.querySuspend<Long>(
                sql = "SELECT COUNT(*) AS row_count FROM \"wikipedia\"",
                options = profile.toConnectionOptions(),
                dispatcher = Dispatchers.Unconfined,
                mapper = any(),
            )
        }
    }

    @Test
    fun `metadata query preserves datasource schema and options`() {
        val metadata = listOf(
            DruidColumnMetadata(
                tableSchema = "druid",
                tableName = "wikipedia",
                columnName = "country",
                dataType = "VARCHAR",
                ordinalPosition = 1,
                isNullable = "YES",
            ),
        )
        every { DruidJdbc.listColumns(any(), any(), any()) } returns metadata

        listDatasourceColumns(profile) shouldBeEqualTo metadata

        verify(exactly = 1) {
            DruidJdbc.listColumns(
                datasource = "wikipedia",
                schema = "druid",
                options = profile.toConnectionOptions(),
            )
        }
    }

    @Test
    fun `unsafe datasource identifiers fail before provider invocation`() {
        assertFailsWith<IllegalArgumentException> {
            buildDatasourceCountQuery("wikipedia; DROP TABLE users")
        }
    }

    @Test
    fun `provider rejects blank and DML SQL before opening a connection`() {
        unmockkObject(DruidJdbc)

        assertFailsWith<IllegalArgumentException> {
            DruidJdbc.query<Long>("", profile.toConnectionOptions()) { 0L }
        }
        assertFailsWith<IllegalArgumentException> {
            DruidJdbc.query<Long>("INSERT INTO wikipedia VALUES (1)", profile.toConnectionOptions()) { 0L }
        }
    }
}
