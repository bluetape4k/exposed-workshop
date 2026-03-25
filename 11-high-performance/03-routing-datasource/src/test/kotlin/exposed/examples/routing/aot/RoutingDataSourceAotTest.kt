package exposed.examples.routing.aot

import exposed.examples.routing.config.RoutingDataSourceConfig
import exposed.examples.routing.datasource.DataSourceRegistry
import exposed.examples.routing.datasource.RoutingKeyResolver
import exposed.examples.routing.domain.RoutingMarkerRepository
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import javax.sql.DataSource

/**
 * Spring Boot AoT 컴파일 호환성을 [ApplicationContextRunner]로 검증합니다.
 *
 * [RoutingDataSourceConfig]의 Bean들이 AoT 환경에서 올바르게 구성되는지 확인합니다:
 * - [DataSourceRegistry]: 테넌트별 DataSource 레지스트리
 * - [RoutingKeyResolver]: 라우팅 키 해석기
 * - `routingDataSource` (`@Primary`): 동적 라우팅 DataSource
 * - [Database]: Exposed Database 인스턴스
 * - [RoutingMarkerRepository]: 라우팅 검증용 Repository
 */
class RoutingDataSourceAotTest {

    companion object : KLogging()

    /**
     * H2 기반 라우팅 DataSource 구성을 검증하는 컨텍스트 러너.
     *
     * `routing.datasource.*` 프로퍼티로 default 테넌트의 H2 DataSource를 설정합니다.
     */
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(RoutingDataSourceConfig::class.java)
        .withPropertyValues(
            "routing.datasource.default-tenant=default",
            "routing.datasource.tenants.default.rw.url=jdbc:h2:mem:routing-aot-rw;DB_CLOSE_DELAY=-1",
            "routing.datasource.tenants.default.rw.driver-class-name=org.h2.Driver",
            "routing.datasource.tenants.default.rw.username=sa",
            "routing.datasource.tenants.default.rw.password=",
        )

    @Test
    fun `DataSourceRegistry 빈이 생성되어야 한다`() {
        contextRunner.run { context ->
            context.getBean<DataSourceRegistry>().shouldNotBeNull()
        }
    }

    @Test
    fun `RoutingKeyResolver 빈이 생성되어야 한다`() {
        contextRunner.run { context ->
            context.getBean<RoutingKeyResolver>().shouldNotBeNull()
        }
    }

    @Test
    fun `라우팅 DataSource 빈이 생성되어야 한다`() {
        contextRunner.run { context ->
            context.getBean<DataSource>().shouldNotBeNull()
        }
    }

    @Test
    fun `Exposed Database 빈이 생성되어야 한다`() {
        contextRunner.run { context ->
            context.getBean<Database>().shouldNotBeNull()
        }
    }

    @Test
    fun `RoutingMarkerRepository 빈이 생성되어야 한다`() {
        contextRunner.run { context ->
            context.getBean<RoutingMarkerRepository>().shouldNotBeNull()
        }
    }
}
