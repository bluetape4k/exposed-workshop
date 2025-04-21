package exposed.multitenant.webflux.config

import exposed.multitenant.webflux.AbstractMultitenantTest
import exposed.multitenant.webflux.domain.model.toActorDTO
import exposed.multitenant.webflux.domain.repository.ActorExposedRepository
import exposed.multitenant.webflux.tenant.Tenants
import exposed.multitenant.webflux.tenant.newSuspendedTransactionWithTenant
import exposed.shared.repository.MovieSchema.ActorTable
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.uninitialized
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

class ExposedConfigTest: AbstractMultitenantTest() {

    companion object: KLogging()

    @Autowired
    private val actorRepository: ActorExposedRepository = uninitialized()

    @Test
    fun `context loading`() {
        actorRepository.shouldNotBeNull()
    }

    @ParameterizedTest
    @EnumSource(Tenants.Tenant::class)
    fun `load all actors by tenant`(tenant: Tenants.Tenant) = runSuspendIO {
        newSuspendedTransactionWithTenant(tenant) {
            val actors = ActorTable.selectAll().map { it.toActorDTO() }
            actors.shouldNotBeEmpty()

            actors.forEach { actor ->
                log.debug { "tenant:${tenant.id}, Actor: $actor" }
            }
        }
    }
}
