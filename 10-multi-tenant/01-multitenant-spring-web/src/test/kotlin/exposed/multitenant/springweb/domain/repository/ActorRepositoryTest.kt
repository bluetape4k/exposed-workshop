package exposed.multitenant.springweb.domain.repository

import exposed.multitenant.springweb.AbstractMultitenantTest
import exposed.multitenant.springweb.tenant.TenantContext
import exposed.multitenant.springweb.tenant.Tenants.Tenant
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

/** 멀티테넌트 환경에서 테넌트별 스키마 격리를 검증하는 `ActorExposedRepository` 통합 테스트. */
class ActorRepositoryTest(
    @param:Autowired private val actorRepo: ActorExposedRepository,
): AbstractMultitenantTest() {

    companion object: KLogging()

    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenant::class)
    fun `테넌트별 모든 배우 조회`(tenant: Tenant) {
        TenantContext.withTenant(tenant) {
            val actors = actorRepo.searchActors(emptyMap())
            log.debug { "tenant=${tenant.id}, actors.size=${actors.size}" }
            actors shouldHaveSize 9
        }
    }

    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenant::class)
    fun `테넌트별 firstName으로 배우 검색`(tenant: Tenant) {
        val firstName = when (tenant) {
            Tenant.ENGLISH -> "Johnny"
            Tenant.KOREAN  -> "조니"
        }
        TenantContext.withTenant(tenant) {
            val actors = actorRepo.searchActors(mapOf("firstName" to firstName))
            actors.shouldNotBeEmpty()
            actors.forEach { log.debug { "tenant=${tenant.id}, actor=$it" } }
            actors.all { it.firstName == firstName }.shouldBeEqualTo(true)
        }
    }

    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenant::class)
    fun `테넌트별 lastName으로 배우 검색`(tenant: Tenant) {
        val lastName = when (tenant) {
            Tenant.ENGLISH -> "Depp"
            Tenant.KOREAN  -> "뎁"
        }
        TenantContext.withTenant(tenant) {
            val actors = actorRepo.searchActors(mapOf("lastName" to lastName))
            actors.shouldNotBeEmpty()
            actors.forEach { log.debug { "tenant=${tenant.id}, actor=$it" } }
        }
    }

    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenant::class)
    fun `잘못된 birthday 파라미터는 무시되고 모든 배우를 반환한다`(tenant: Tenant) {
        TenantContext.withTenant(tenant) {
            val actors = actorRepo.searchActors(mapOf("birthday" to "not-a-date"))
            actors shouldHaveSize 9
        }
    }

    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenant::class)
    fun `id=2 배우 조회 시 테넌트에 맞는 이름을 반환한다`(tenant: Tenant) {
        val expectedFirstName = when (tenant) {
            Tenant.ENGLISH -> "Brad"
            Tenant.KOREAN  -> "브래드"
        }
        TenantContext.withTenant(tenant) {
            val actors = actorRepo.searchActors(mapOf("id" to "2"))
            actors shouldHaveSize 1
            actors.first().firstName shouldBeEqualTo expectedFirstName
        }
    }

    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenant::class)
    fun `존재하지 않는 배우 id 조회 시 null을 반환한다`(tenant: Tenant) {
        TenantContext.withTenant(tenant) {
            val actor = actorRepo.findByIdOrNull(-1L)
            actor.shouldBeNull()
        }
    }

}
