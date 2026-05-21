package exposed.examples.spring.architecture

import exposed.examples.spring.architecture.service.CustomerService
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SpringArchitectureApplicationTest(
    @param:Autowired private val mockMvc: MockMvc,
    @param:Autowired private val customerService: CustomerService,
) {

    @BeforeEach
    fun resetCustomers() {
        customerService.deleteAll()
    }

    @Test
    fun `index and health endpoints describe the application`() {
        mockMvc.get("/")
            .andExpect {
                status { isOk() }
                jsonPath("$.service", equalTo("spring-application-architecture"))
            }

        mockMvc.get("/health")
            .andExpect {
                status { isOk() }
                jsonPath("$.status", equalTo("UP"))
            }
    }

    @Test
    fun `customer routes create list and find customers`() {
        mockMvc.post("/customers") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":" Alice ","email":"ALICE@EXAMPLE.COM "}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id", equalTo(1))
            jsonPath("$.name", equalTo("Alice"))
            jsonPath("$.email", equalTo("alice@example.com"))
        }

        mockMvc.get("/customers")
            .andExpect {
                status { isOk() }
                jsonPath("$.customers", hasSize<Any>(1))
                jsonPath("$.customers[0].name", equalTo("Alice"))
            }

        mockMvc.get("/customers/1")
            .andExpect {
                status { isOk() }
                jsonPath("$.email", equalTo("alice@example.com"))
            }
    }

    @Test
    fun `validation and not found errors are sanitized`() {
        mockMvc.post("/customers") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"","email":"alice@example.com"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code", equalTo("VALIDATION_ERROR"))
        }

        mockMvc.get("/customers/999")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.code", equalTo("NOT_FOUND"))
                jsonPath("$.message", equalTo("Resource was not found"))
            }
    }

    @Test
    fun `malformed json is reported without parser details`() {
        mockMvc.post("/customers") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code", equalTo("MALFORMED_JSON"))
            jsonPath("$.message", equalTo("Request body is not valid JSON"))
        }
    }
}
