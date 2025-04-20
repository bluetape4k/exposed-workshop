package exposed.multitenant.springweb.controller

import exposed.multitenant.springweb.AbstractMultitenantTest
import exposed.multitenant.springweb.domain.dtos.ActorDTO
import exposed.multitenant.springweb.tenant.TenantFilter
import exposed.multitenant.springweb.tenant.Tenants
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList

class ActorControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractMultitenantTest() {

    companion object: KLogging()

    @ParameterizedTest(name = "Tenant: {0}")
    @EnumSource(Tenants.Tenant::class)
    fun `get all actors by tenant`(tenant: Tenants.Tenant) {

        val actors = client.get()
            .uri("/actors")
            .header(TenantFilter.TENANT_HEADER, tenant.id)
            .exchange()
            .expectStatus().isOk
            .expectBodyList<ActorDTO>()
            .returnResult().responseBody!!

        actors.forEach {
            log.debug { "Tenant: ${tenant.id}, Actor: $it" }
        }

        actors shouldHaveSize 9

        when (tenant) {
            Tenants.Tenant.KOREAN ->
                actors.any { it.firstName == "조니" }.shouldBeTrue()
            Tenants.Tenant.ENGLISH ->
                actors.any { it.firstName == "Johnny" }.shouldBeTrue()
        }
    }
}
