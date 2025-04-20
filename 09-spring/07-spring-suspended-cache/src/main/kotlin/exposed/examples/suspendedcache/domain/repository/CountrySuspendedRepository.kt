package exposed.examples.suspendedcache.domain.repository

import exposed.examples.suspendedcache.domain.CountryDTO

interface CountrySuspendedRepository {

    suspend fun findByCode(code: String): CountryDTO?

    suspend fun update(countryDTO: CountryDTO): Int

    suspend fun evictCacheAll()
}
