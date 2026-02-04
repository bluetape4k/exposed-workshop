package exposed.examples.suspendedcache.domain.repository

import exposed.examples.suspendedcache.domain.CountryRecord

interface CountrySuspendedRepository {

    suspend fun findByCode(code: String): CountryRecord?

    suspend fun update(countryRecord: CountryRecord): Int

    suspend fun evictCacheAll()
}
