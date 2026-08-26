package exposed.examples.druid.queryonly

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

@EnabledIfEnvironmentVariable(named = "EXPOSED_DRUID_SMOKE", matches = "true")
class DruidQueryOnlySmokeTest {

    @Test
    fun `opt-in Druid endpoint serves query and metadata`() = runSuspendIO {
        val profile = DruidQueryProfile(
            avaticaEndpoint = envOrDefault(
                "EXPOSED_DRUID_AVATICA_ENDPOINT",
                "http://localhost:8888/druid/v2/sql/avatica/",
            ),
            datasource = envOrDefault("EXPOSED_DRUID_DATASOURCE", "wikipedia"),
            schema = envOrDefault("EXPOSED_DRUID_SCHEMA", "druid"),
            user = System.getenv("EXPOSED_DRUID_USER")?.takeIf(String::isNotBlank),
            password = System.getenv("EXPOSED_DRUID_PASSWORD")?.takeIf(String::isNotBlank),
        )

        queryDatasourceRowCountSuspend(profile).size shouldBeEqualTo 1
        listDatasourceColumns(profile).isNotEmpty().shouldBeTrue()
    }

    private fun envOrDefault(name: String, fallback: String): String =
        System.getenv(name)?.takeIf(String::isNotBlank) ?: fallback
}
