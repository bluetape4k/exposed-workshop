package exposed.multitenant.springweb.config

import exposed.multitenant.springweb.AbstractMultitenantTest
import exposed.multitenant.springweb.domain.model.MovieSchema.ActorTable
import exposed.multitenant.springweb.tenant.TenantContext
import exposed.multitenant.springweb.tenant.Tenants
import exposed.shared.repository.model.toActorRecord
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test
import org.springframework.test.annotation.Commit
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Commit
class SchemaRoutingConfigTest: AbstractMultitenantTest() {

    companion object: KLogging()

    @Test
    fun `get actors in korean`() {
        TenantContext.withTenant(Tenants.Tenant.KOREAN) {
            val actors = ActorTable.selectAll().map { it.toActorRecord() }
            actors shouldHaveSize 9

            actors.forEach {
                log.debug { "Korean Actor: $it" }
            }
        }
    }

    @Test
    fun `get actors in english`() {
        TenantContext.withTenant(Tenants.Tenant.ENGLISH) {
            val actors = ActorTable.selectAll().map { it.toActorRecord() }
            actors shouldHaveSize 9

            actors.forEach {
                log.debug { "Korean Actor: $it" }
            }
        }
    }
}
