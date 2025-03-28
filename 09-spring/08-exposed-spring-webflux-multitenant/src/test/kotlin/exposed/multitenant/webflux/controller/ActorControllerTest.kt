package exposed.multitenant.webflux.controller

import exposed.multitenant.webflux.AbstractMultitenantTest
import exposed.multitenant.webflux.domain.dtos.ActorDTO
import exposed.multitenant.webflux.tenant.TenantFilter
import exposed.multitenant.webflux.tenant.Tenants
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList

class ActorControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractMultitenantTest() {

    companion object: KLogging()

    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenants.Tenant::class)
    fun `get all actors by tenant`(tenant: Tenants.Tenant) {
        val actors = client
            .get()
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

    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenants.Tenant::class)
    fun `get actor by id with tenant`(tenant: Tenants.Tenant) {
        val actor = client
            .get()
            .uri("/actors/2")
            .header(TenantFilter.TENANT_HEADER, tenant.id)
            .exchange()
            .expectStatus().isOk
            .expectBody<ActorDTO>()
            .returnResult().responseBody!!

        log.debug { "Tenant: ${tenant.id}, Actor: $actor" }

        when (tenant) {
            Tenants.Tenant.KOREAN -> actor.firstName shouldBeEqualTo "브래드"
            Tenants.Tenant.ENGLISH -> actor.firstName shouldBeEqualTo "Brad"
        }
    }
}
