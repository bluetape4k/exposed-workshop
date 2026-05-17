package exposed.examples.ktor.architecture.routes

import exposed.examples.ktor.architecture.ktorArchitectureModule
import exposed.examples.ktor.architecture.model.CreateCustomerRequest
import exposed.examples.ktor.architecture.model.CustomerResponse
import exposed.examples.ktor.architecture.model.CustomersResponse
import exposed.examples.ktor.architecture.model.ErrorResponse
import exposed.examples.ktor.architecture.persistence.CustomerPersistence
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.codec.Base58
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerRoutesTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `create customer then read by id and list through routes`() = testApplicationWithPersistence {
        val created = createCustomer("Alice", "alice@example.com")
        created.name shouldBeEqualTo "Alice"
        created.email shouldBeEqualTo "alice@example.com"

        val found = json.decodeFromString<CustomerResponse>(
            client.get("/customers/${created.id}").bodyAsText()
        )
        found shouldBeEqualTo created

        val list = json.decodeFromString<CustomersResponse>(
            client.get("/customers").bodyAsText()
        )
        list.customers shouldHaveSize 1
        list.customers.first() shouldBeEqualTo created
    }

    @Test
    fun `unknown customer returns not found`() = testApplicationWithPersistence {
        val response = client.get("/customers/999")
        response.status shouldBeEqualTo HttpStatusCode.NotFound

        val error = json.decodeFromString<ErrorResponse>(response.bodyAsText())
        error.code shouldBeEqualTo "NOT_FOUND"
    }

    @Test
    fun `invalid id returns bad request`() = testApplicationWithPersistence {
        val response = client.get("/customers/not-a-number")
        response.status shouldBeEqualTo HttpStatusCode.BadRequest

        val error = json.decodeFromString<ErrorResponse>(response.bodyAsText())
        error.code shouldBeEqualTo "VALIDATION_FAILED"
    }

    @Test
    fun `validation failures return bad request`() = testApplicationWithPersistence {
        val blankName = postJson("/customers", CreateCustomerRequest(" ", "alice@example.com"))
        blankName.status shouldBeEqualTo HttpStatusCode.BadRequest
        json.decodeFromString<ErrorResponse>(blankName.bodyAsText()).message shouldBeEqualTo "name must not be blank"

        val invalidEmail = postJson("/customers", CreateCustomerRequest("Alice", "alice.example.com"))
        invalidEmail.status shouldBeEqualTo HttpStatusCode.BadRequest
        json.decodeFromString<ErrorResponse>(invalidEmail.bodyAsText()).message shouldBeEqualTo "email must contain @"
    }

    @Test
    fun `malformed json returns sanitized bad request`() = testApplicationWithPersistence {
        val response = client.post("/customers") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":""")
        }

        response.status shouldBeEqualTo HttpStatusCode.BadRequest
        val error = json.decodeFromString<ErrorResponse>(response.bodyAsText())
        error.code shouldBeEqualTo "BAD_REQUEST"
        error.message shouldBeEqualTo "Malformed request body"
    }

    @Test
    fun `oversized request returns payload too large`() = testApplicationWithPersistence {
        val largeName = "A".repeat(65 * 1024)
        val response = client.post("/customers") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateCustomerRequest(largeName, "alice@example.com")))
        }

        response.status shouldBeEqualTo HttpStatusCode.PayloadTooLarge
        val error = json.decodeFromString<ErrorResponse>(response.bodyAsText())
        error.code shouldBeEqualTo "PAYLOAD_TOO_LARGE"
    }

    private suspend fun ApplicationTestBuilder.createCustomer(
        name: String,
        email: String,
    ): CustomerResponse {
        val response = postJson("/customers", CreateCustomerRequest(name, email))
        response.status shouldBeEqualTo HttpStatusCode.Created
        return json.decodeFromString(response.bodyAsText())
    }

    private suspend inline fun <reified T> ApplicationTestBuilder.postJson(
        path: String,
        body: T,
    ) = client.post(path) {
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(body))
    }

    private fun testApplicationWithPersistence(
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) = testApplication {
        val persistence = CustomerPersistence.inMemory("route_${Base58.randomString(8)}")

        application {
            ktorArchitectureModule(persistence)
        }

        test()
    }
}
