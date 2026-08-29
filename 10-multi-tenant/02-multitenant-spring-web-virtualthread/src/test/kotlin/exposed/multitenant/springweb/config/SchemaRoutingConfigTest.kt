package exposed.multitenant.springweb.config

import exposed.multitenant.springweb.AbstractMultitenantTest
import exposed.multitenant.springweb.domain.model.MovieSchema.ActorTable
import exposed.multitenant.springweb.tenant.TenantContexts
import exposed.multitenant.springweb.tenant.Tenants
import exposed.shared.repository.model.toActorRecord
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldHaveSize
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import org.springframework.transaction.annotation.Transactional

/**
 * 가상 스레드(Java 25 `ScopedValue`) 기반 멀티테넌시에서 테넌트별 스키마 라우팅 및 데이터 격리를 검증합니다.
 */
@Transactional
class SchemaRoutingConfigTest: AbstractMultitenantTest() {

    companion object: KLoggingChannel()

    @EnabledOnJre(JRE.JAVA_25)
    @Test
    fun `get actors in korean`() {
        TenantContexts.withTenant(Tenants.Tenant.KOREAN) {
            val actors = ActorTable.selectAll().map { it.toActorRecord() }
            actors shouldHaveSize 9

            actors.forEach {
                log.debug { "Korean Actor: $it" }
            }
        }
    }

    @EnabledOnJre(JRE.JAVA_25)
    @Test
    fun `get actors in english`() {
        TenantContexts.withTenant(Tenants.Tenant.ENGLISH) {
            val actors = ActorTable.selectAll().map { it.toActorRecord() }
            actors shouldHaveSize 9

            actors.forEach {
                log.debug { "English Actor: $it" }
            }
        }
    }
}
