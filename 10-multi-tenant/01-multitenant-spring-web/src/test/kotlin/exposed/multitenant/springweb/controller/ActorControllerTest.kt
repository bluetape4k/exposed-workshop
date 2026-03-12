package exposed.multitenant.springweb.controller

import exposed.multitenant.springweb.AbstractMultitenantTest
import exposed.multitenant.springweb.domain.dtos.ActorRecord
import exposed.multitenant.springweb.tenant.TenantFilter
import exposed.multitenant.springweb.tenant.Tenants.Tenant
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.test.web.reactive.server.returnResult

/** 멀티테넌트 Spring MVC 환경에서 테넌트 헤더에 따라 Actor REST API가 올바른 스키마 데이터를 반환함을 검증합니다. */
class ActorControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractMultitenantTest() {

    companion object: KLogging()

    @ParameterizedTest(name = "Tenant: {0}")
    @EnumSource(Tenant::class)
    fun `get all actors by tenant`(tenant: Tenant) = runSuspendIO {

        val actors = client
            .get()
            .uri("/actors")
            .header(TenantFilter.TENANT_HEADER, tenant.id)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBodyList<ActorRecord>()
            .returnResult().responseBody
            .shouldNotBeNull()

        actors.forEach {
            log.debug { "Tenant: ${tenant.id}, Actor: $it" }
        }

        actors shouldHaveSize 9

        val expectedFirstName = mapOf(
            Tenant.KOREAN to "조니",
            Tenant.ENGLISH to "Johnny"
        )
        actors.any { it.firstName == expectedFirstName[tenant] }.shouldBeTrue()
    }


    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenant::class)
    fun `get actor by id with tenant`(tenant: Tenant) = runSuspendIO {
        val actor = client
            .get()
            .uri("/actors/2")
            .header(TenantFilter.TENANT_HEADER, tenant.id)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<ActorRecord>().responseBody
            .awaitSingle()

        log.debug { "Tenant: ${tenant.id}, Actor: $actor" }

        val expectedFirstName = mapOf(
            Tenant.KOREAN to "브래드",
            Tenant.ENGLISH to "Brad"
        )
        actor.firstName shouldBeEqualTo expectedFirstName[tenant]
    }

    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenant::class)
    fun `존재하지 않는 actor id 조회 시 null 을 반환한다`(tenant: Tenant) = runSuspendIO {
        val actor = client
            .get()
            .uri("/actors/-1")
            .header(TenantFilter.TENANT_HEADER, tenant.id)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<ActorRecord>().responseBody
            .awaitFirstOrNull()

        actor.shouldBeNull()
    }

    @Test
    fun `잘못된 tenant header 로 요청하면 서버 에러가 발생한다`() {
        client
            .get()
            .uri("/actors")
            .header(TenantFilter.TENANT_HEADER, "invalid-tenant")
            .exchange()
            .expectStatus().is5xxServerError
    }
}
