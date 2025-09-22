package exposed.multitenant.springweb.controller

import exposed.multitenant.springweb.AbstractMultitenantTest
import exposed.multitenant.springweb.domain.dtos.ActorDTO
import exposed.multitenant.springweb.tenant.TenantFilter
import exposed.multitenant.springweb.tenant.Tenants
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

class ActorControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractMultitenantTest() {

    companion object: KLogging()

    @ParameterizedTest(name = "Tenant: {0}")
    @EnumSource(Tenants.Tenant::class)
    fun `get all actors by tenant`(tenant: Tenants.Tenant) = runSuspendIO {

        val actors = client.get()
            .uri("/actors")
            .header(TenantFilter.TENANT_HEADER, tenant.id)
            .exchange()
            .expectStatus().isOk
            .returnResult<ActorDTO>().responseBody
            .asFlow()
            .toList()

        actors.forEach {
            log.debug { "Tenant: ${tenant.id}, Actor: $it" }
        }

        actors shouldHaveSize 9

        val expectedFirstName = mapOf(
            Tenants.Tenant.KOREAN to "조니",
            Tenants.Tenant.ENGLISH to "Johnny"
        )
        actors.any { it.firstName == expectedFirstName[tenant] }.shouldBeTrue()
    }
}
