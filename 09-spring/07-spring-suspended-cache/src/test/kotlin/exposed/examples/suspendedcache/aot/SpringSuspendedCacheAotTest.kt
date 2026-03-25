package exposed.examples.suspendedcache.aot

import exposed.examples.suspendedcache.SpringSuspendedCacheApplication
import exposed.examples.suspendedcache.config.ExposedConfig
import exposed.examples.suspendedcache.config.LettuceSuspendedCacheConfig
import exposed.examples.suspendedcache.config.SuspendedRepositoryConfig
import exposed.examples.suspendedcache.domain.repository.CountrySuspendedRepository
import exposed.examples.suspendedcache.lettuce.LettuceSuspendedCacheManager
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.util.function.Supplier
import javax.sql.DataSource

/**
 * Spring Boot AoT 컴파일 호환성을 [ApplicationContextRunner]로 검증합니다.
 *
 * Suspended Cache + Coroutines 기반 구성이 AoT 환경에서 올바르게 동작하는지 확인합니다:
 * - [ExposedConfig]: Exposed 데이터베이스 구성 (`DatabaseConfig`, `Database` 빈)
 * - [LettuceSuspendedCacheConfig]: Lettuce 캐시 구성 (`LettuceSuspendedCacheManager` 빈)
 * - [SuspendedRepositoryConfig]: Repository 빈 구성 (`CountrySuspendedRepository` 빈)
 */
class SpringSuspendedCacheAotTest {

    companion object: KLoggingChannel() {
        // Testcontainers Redis 싱글턴 (첫 접근 시 컨테이너 시작)
        private val redisServer = SpringSuspendedCacheApplication.redisServer
    }

    /**
     * Exposed DB 구성을 검증하는 컨텍스트 러너.
     *
     * [DataSourceAutoConfiguration]이 DataSource를 자동 구성하고,
     * [ExposedConfig]가 [DatabaseConfig]와 [Database] 빈을 생성합니다.
     */
    private val exposedContextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration::class.java))
        .withUserConfiguration(ExposedConfig::class.java)
        .withPropertyValues(
            "spring.datasource.url=jdbc:h2:mem:ssc-aot-test;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
        )

    /**
     * Lettuce Suspended 캐시 구성을 검증하는 컨텍스트 러너.
     *
     * [RedisClient] 빈을 직접 등록하고 [LettuceSuspendedCacheConfig]가
     * [LettuceSuspendedCacheManager] 빈을 생성합니다.
     */
    private val cacheContextRunner = ApplicationContextRunner()
        .withUserConfiguration(LettuceSuspendedCacheConfig::class.java)
        .withBean(
            RedisClient::class.java,
            Supplier { RedisClient.create(RedisURI.create(redisServer.url)) }
        )

    /**
     * Repository 구성을 검증하는 컨텍스트 러너.
     *
     * [LettuceSuspendedCacheManager]와 [SuspendedRepositoryConfig]를 함께 로드하여
     * Repository 빈이 올바르게 구성되는지 확인합니다.
     */
    private val repositoryContextRunner = ApplicationContextRunner()
        .withUserConfiguration(LettuceSuspendedCacheConfig::class.java, SuspendedRepositoryConfig::class.java)
        .withBean(
            RedisClient::class.java,
            Supplier { RedisClient.create(RedisURI.create(redisServer.url)) }
        )

    // ─── Exposed DB 구성 테스트 ───────────────────────────────────────────────

    @Test
    fun `DataSource 빈이 자동 구성되어야 한다`() {
        exposedContextRunner.run { context ->
            context.getBean<DataSource>().shouldNotBeNull()
        }
    }

    @Test
    fun `DatabaseConfig 빈이 생성되어야 한다`() {
        exposedContextRunner.run { context ->
            context.getBean<DatabaseConfig>().shouldNotBeNull()
        }
    }

    @Test
    fun `Exposed Database 빈이 생성되어야 한다`() {
        exposedContextRunner.run { context ->
            context.getBean<Database>().shouldNotBeNull()
        }
    }

    // ─── Lettuce Suspended Cache 구성 테스트 ─────────────────────────────────

    @Test
    fun `LettuceSuspendedCacheManager 빈이 생성되어야 한다`() {
        cacheContextRunner.run { context ->
            context.getBean<LettuceSuspendedCacheManager>().shouldNotBeNull()
        }
    }

    // ─── Repository 구성 테스트 ───────────────────────────────────────────────

    @Test
    fun `defaultCountrySuspendedRepository 빈이 생성되어야 한다`() {
        repositoryContextRunner.run { context ->
            context.getBean<CountrySuspendedRepository>("defaultCountrySuspendedRepository").shouldNotBeNull()
        }
    }

    @Test
    fun `cachedCountrySuspendedRepository 빈이 생성되어야 한다`() {
        repositoryContextRunner.run { context ->
            context.getBean<CountrySuspendedRepository>("cachedCountrySuspendedRepository").shouldNotBeNull()
        }
    }
}
