package exposed.multitenant.springweb.aot

import exposed.multitenant.springweb.config.ExposedMultitenantConfig
import exposed.multitenant.springweb.tenant.TenantAwareDataSource
import io.bluetape4k.logging.KLogging
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
 * Virtual Threads 기반 멀티테넌트 Spring MVC 환경에서 [ExposedMultitenantConfig]의 Bean들이
 * `h2` 프로파일 환경에서 올바르게 구성되는지 경량 컨텍스트로 확인합니다.
 */
class MultitenantVirtualThreadAotTest {

    companion object : KLogging()

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
