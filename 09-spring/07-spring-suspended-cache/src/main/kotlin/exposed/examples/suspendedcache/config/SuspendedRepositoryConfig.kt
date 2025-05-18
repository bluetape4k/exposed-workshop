package exposed.examples.suspendedcache.config

import exposed.examples.suspendedcache.domain.repository.CachedCountrySuspendedRepository
import exposed.examples.suspendedcache.domain.repository.CountrySuspendedRepository
import exposed.examples.suspendedcache.domain.repository.DefaultCountrySuspendedRepository
import exposed.examples.suspendedcache.lettuce.LettuceSuspendedCacheManager
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SuspendedRepositoryConfig(
    private val suspendedCacheManager: LettuceSuspendedCacheManager,
) {

    companion object: KLoggingChannel() 

    @Bean(name = ["countrySuspendedRepository", "defaultCountrySuspendedRepository"])
    fun countrySuspendedRepository(): CountrySuspendedRepository {
        return DefaultCountrySuspendedRepository()
    }

    @Bean(name = ["cachedCountrySuspendedRepository"])
    fun cachedCountrySuspendedRepository(countrySuspendedRepository: CountrySuspendedRepository): CountrySuspendedRepository {
        return CachedCountrySuspendedRepository(
            delegate = countrySuspendedRepository,
            cacheManager = suspendedCacheManager,
        )
    }
}
