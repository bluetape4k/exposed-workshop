package exposed.examples.routing.config

import com.zaxxer.hikari.HikariDataSource
import exposed.examples.routing.datasource.ContextAwareRoutingKeyResolver
import exposed.examples.routing.datasource.DataSourceRegistry
import exposed.examples.routing.datasource.DynamicRoutingDataSource
import exposed.examples.routing.datasource.InMemoryDataSourceRegistry
import exposed.examples.routing.datasource.RoutingKeyResolver
import exposed.examples.routing.domain.RoutingMarkerRepository
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

/**
 * `routing.datasource` 설정을 읽어 동적 라우팅 DataSource를 구성합니다.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RoutingDataSourceProperties::class)
class RoutingDataSourceConfig {

    /**
     * 런타임 라우팅에 사용할 레지스트리를 생성합니다.
     */
    @Bean
    fun dataSourceRegistry(properties: RoutingDataSourceProperties): DataSourceRegistry {
        val registry = InMemoryDataSourceRegistry()

        properties.tenants.forEach { (tenantId, tenant) ->
            val rw = tenant.rw.toHikariDataSource()
            val ro = (tenant.ro ?: tenant.rw).toHikariDataSource()

            registry.register("$tenantId:rw", rw)
            registry.register("$tenantId:ro", ro)
        }
        return registry
    }

    /**
     * tenant/read-only 기반 라우팅 키 해석기를 생성합니다.
     */
    @Bean
    fun routingKeyResolver(properties: RoutingDataSourceProperties): RoutingKeyResolver =
        ContextAwareRoutingKeyResolver(defaultTenant = properties.defaultTenant)

    /**
     * 애플리케이션 기본 [DataSource]를 동적 라우팅 구현으로 교체합니다.
     */
    @Bean
    @Primary
    fun routingDataSource(registry: DataSourceRegistry, resolver: RoutingKeyResolver): DataSource =
        DynamicRoutingDataSource(registry, resolver)

    /**
     * Exposed가 사용할 [Database]를 라우팅 데이터소스에 연결합니다.
     */
    @Bean
    fun exposedDatabase(dataSource: DataSource): Database = Database.connect(dataSource)

    /**
     * 라우팅 데이터소스 검증용 Exposed 저장소를 제공합니다.
     */
    @Bean
    fun routingMarkerRepository(database: Database): RoutingMarkerRepository =
        RoutingMarkerRepository(database)
}

/**
 * 라우팅 데이터소스 구성 루트 프로퍼티입니다.
 */
@ConfigurationProperties(prefix = "routing.datasource")
class RoutingDataSourceProperties {

    /**
     * tenant 컨텍스트가 비어 있을 때 사용할 기본 tenant 식별자입니다.
     */
    var defaultTenant: String = "default"

    /**
     * tenant 식별자별 read-write/read-only 데이터소스 설정입니다.
     */
    var tenants: MutableMap<String, TenantDataSourceProperties> = linkedMapOf()
}

/**
 * 단일 tenant의 read-write/read-only 데이터소스 설정입니다.
 */
class TenantDataSourceProperties {

    /**
     * read-write(기본) 데이터소스 설정입니다.
     */
    lateinit var rw: DataSourceNodeProperties

    /**
     * read-only 데이터소스 설정입니다. 지정하지 않으면 `rw`를 재사용합니다.
     */
    var ro: DataSourceNodeProperties? = null
}

/**
 * 실제 JDBC 연결 노드 1개의 설정 값입니다.
 */
class DataSourceNodeProperties {

    /**
     * JDBC URL입니다.
     */
    lateinit var url: String

    /**
     * DB 로그인 계정입니다.
     */
    var username: String = "sa"

    /**
     * DB 로그인 비밀번호입니다.
     */
    var password: String = ""

    /**
     * JDBC 드라이버 클래스 이름입니다.
     */
    var driverClassName: String = "org.h2.Driver"
}

private fun DataSourceNodeProperties.toHikariDataSource(): DataSource =
    HikariDataSource().also {
        it.jdbcUrl = url
        it.username = username
        it.password = password
        it.driverClassName = driverClassName
        it.maximumPoolSize = 4
        it.minimumIdle = 1
    }
