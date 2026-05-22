package exposed.examples.spring.httpoutbox

import exposed.examples.spring.httpoutbox.service.PaymentService
import exposed.examples.spring.httpoutbox.service.ScenarioPaymentGateway
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest(
    classes = [
        SpringHttpOutboxApplication::class,
        SpringHttpOutboxApplicationTest.TestPaymentGatewayConfiguration::class,
    ]
)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpringHttpOutboxApplicationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var service: PaymentService

    @Autowired
    private lateinit var gateway: ScenarioPaymentGateway

    @BeforeEach
    fun resetState() {
        service.deleteAll()
        gateway.reset()
    }

    @Test
    fun `index and health endpoints respond`() {
        mockMvc.get("/")
            .andExpect {
                status { isOk() }
                jsonPath("$.service", equalTo("spring-http-outbox-idempotency"))
            }

        mockMvc.get("/health")
            .andExpect {
                status { isOk() }
                jsonPath("$.status", equalTo("UP"))
            }
    }

    @Test
    fun `payment routes cover success duplicate permanent and retry paths`() {
        gateway.succeed("ok-key", "ext-ok")
        mockMvc.post("/payments") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"orderId":" order-ok ","amountCents":2500,"idempotencyKey":" ok-key "}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.status", equalTo("SUCCEEDED"))
            jsonPath("$.externalId", equalTo("ext-ok"))
        }

        mockMvc.post("/payments") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"orderId":"order-ok","amountCents":2500,"idempotencyKey":"ok-key"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.duplicate", equalTo(true))
        }

        gateway.retryableThenSuccess("retry-key", "ext-retry")
        val retryId = mockMvc.post("/payments") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"orderId":"order-retry","amountCents":1000,"idempotencyKey":"retry-key"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.status", equalTo("RETRYABLE_FAILED"))
        }.andReturn().response.contentAsString.substringAfter("\"id\":").substringBefore(",").toLong()

        mockMvc.post("/payments/$retryId/retry")
            .andExpect {
                status { isOk() }
                jsonPath("$.status", equalTo("SUCCEEDED"))
                jsonPath("$.attempts", equalTo(2))
            }

        gateway.permanent("bad-key")
        mockMvc.post("/payments") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"orderId":"order-bad","amountCents":1000,"idempotencyKey":"bad-key"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.status", equalTo("PERMANENT_FAILED"))
        }

        mockMvc.get("/payments")
            .andExpect {
                status { isOk() }
                jsonPath("$.payments", hasSize<Any>(3))
            }
    }

    @Test
    fun `validation and not found errors are sanitized`() {
        mockMvc.post("/payments") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"orderId":"","amountCents":1000,"idempotencyKey":"key"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code", equalTo("VALIDATION_ERROR"))
        }

        mockMvc.get("/payments/999")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.code", equalTo("NOT_FOUND"))
            }
    }

    @TestConfiguration(proxyBeanMethods = false)
    internal class TestPaymentGatewayConfiguration {
        @Bean
        @Primary
        fun scenarioPaymentGateway(): ScenarioPaymentGateway =
            ScenarioPaymentGateway()
    }
}
