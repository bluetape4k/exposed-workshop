package exposed.examples.ktor.observability.config

import io.bluetape4k.ktor.observability.Bluetape4kKtorObservabilityConfig
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import nl.altindag.log.LogCaptor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtorPluginsProviderTest {

    private lateinit var logCaptor: LogCaptor

    @BeforeAll
    fun setUpLogger() {
        logCaptor = LogCaptor.forRoot()
        logCaptor.setLogLevelToInfo()
    }

    @AfterAll
    fun tearDownLogger() {
        logCaptor.clearLogs()
        logCaptor.close()
    }

    @Test
    fun `provider 기본 설정은 correlation 과 call logging 을 켜고 telemetry 를 끈다`() {
        val config = Bluetape4kKtorObservabilityConfig()

        assertTrue(config.installCorrelationId)
        assertTrue(config.installCallLogging)
        assertEquals(false, config.installMicrometerMetrics)
        assertEquals(null, config.meterRegistry)
        assertEquals(null, config.tracing)
    }

    @Test
    fun `provider 설치 후 generic exception 은 구조화 오류와 correlation id 를 유지한다`() = testApplication {
        application {
            installKtorPlugins()
            routing {
                get("/boom") { error("boom") }
            }
        }

        val response = client.get("/boom") {
            header(REQUEST_ID_HEADER, "internal-trace")
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertTrue(response.bodyAsText().contains("\"requestId\":\"internal-trace\""))
        assertEquals("internal-trace", response.headers[REQUEST_ID_HEADER])
    }

    @Test
    fun `provider 설치는 CancellationException 을 소비하지 않는다`() {
        testApplication {
            application {
                installKtorPlugins()
                routing {
                    get("/cancel") { throw CancellationException("cancelled") }
                }
            }

            val response = client.get("/cancel")
            val body = response.bodyAsText()

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertTrue(body.contains("CancellationException"))
            assertTrue(!body.contains("\"code\":\"INTERNAL_ERROR\""))
        }
    }

    @Test
    fun `provider call logging 은 sanitized correlation id 를 기록한다`() = testApplication {
        application {
            installKtorPlugins()
            routing { get("/ok") { call.respondText("ok") } }
        }

        client.get("/ok") { header(REQUEST_ID_HEADER, "trace:with spaces") }

        val logs = logCaptor.getLogs()
        assertTrue(logs.any { it.contains("correlationId=tracewithspaces") })
    }
}
