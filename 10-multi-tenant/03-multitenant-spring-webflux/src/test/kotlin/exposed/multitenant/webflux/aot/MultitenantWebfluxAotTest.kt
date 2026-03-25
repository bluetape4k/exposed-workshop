package exposed.multitenant.webflux.aot

import exposed.multitenant.webflux.config.ExposedMultitenantConfig
import exposed.multitenant.webflux.tenant.TenantAwareDataSource
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import javax.sql.DataSource

/**
 * Spring Boot AoT 컴파일 호환성을 [ApplicationContextRunner]로 검증합니다.
 *
 * Spring WebFlux + Coroutines 기반 멀티테넌트 환경에서 [ExposedMultitenantConfig]의 Bean들이
 * `h2` 프로파일 환경에서 올바르게 구성되는지 경량 컨텍스트로 확인합니다.
 */
class MultitenantWebfluxAotTest {

    companion object : KLoggingChannel()

    /**
     * h2 프로파일을 활성화하여 [ExposedMultitenantConfig]만 로드하는 경량 컨텍스트 러너.
     */
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(ExposedMultitenantConfig::class.java)
        .withPropertyValues("spring.profiles.active=h2")

    @Test
    fun `TenantAwareDataSource 빈이 생성되어야 한다`() {
        contextRunner.run { context ->
            context.getBean<TenantAwareDataSource>().shouldNotBeNull()
        }
    }

    @Test
    fun `Primary DataSource 빈이 생성되어야 한다`() {
        contextRunner.run { context ->
            context.getBean<DataSource>().shouldNotBeNull()
        }
    }

    @Test
    fun `DatabaseConfig 빈이 생성되어야 한다`() {
        contextRunner.run { context ->
            context.getBean<DatabaseConfig>().shouldNotBeNull()
        }
    }

    @Test
    fun `Exposed Database 빈이 생성되어야 한다`() {
        contextRunner.run { context ->
            context.getBean<Database>().shouldNotBeNull()
        }
    }
}
