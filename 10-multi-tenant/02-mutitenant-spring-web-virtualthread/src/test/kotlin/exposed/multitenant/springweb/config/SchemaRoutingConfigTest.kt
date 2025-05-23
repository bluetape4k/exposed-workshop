package exposed.multitenant.springweb.config

import exposed.multitenant.springweb.AbstractMultitenantTest
import exposed.multitenant.springweb.domain.model.MovieSchema.ActorTable
import exposed.multitenant.springweb.tenant.TenantContext
import exposed.multitenant.springweb.tenant.Tenants
import exposed.shared.repository.toActorDTO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class SchemaRoutingConfigTest: AbstractMultitenantTest() {

    companion object: KLoggingChannel()

    @Test
    fun `get actors in korean`() {
        ScopedValue.where(TenantContext.CURRENT_TENANT, Tenants.Tenant.KOREAN).run {
            val actors = ActorTable.selectAll().map { it.toActorDTO() }

            actors.forEach {
                log.debug { "Korean Actor: $it" }
            }
        }
    }

    @Test
    fun `get actors in english`() {
        ScopedValue.where(TenantContext.CURRENT_TENANT, Tenants.Tenant.ENGLISH).run {
            val actors = ActorTable.selectAll().map { it.toActorDTO() }

            actors.forEach {
                log.debug { "English Actor: $it" }
            }
        }
    }
}
