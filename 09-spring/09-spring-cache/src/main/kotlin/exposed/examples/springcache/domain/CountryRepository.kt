package exposed.examples.springcache.domain

import exposed.examples.springcache.domain.CountryRepository.Companion.COUNTRY_CACHE_NAME
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@CacheConfig(cacheNames = [COUNTRY_CACHE_NAME])
class CountryRepository(private val cacheManager: CacheManager) {

    companion object: KLogging() {
        const val COUNTRY_CACHE_NAME = "cache:code:country"
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'country:' + #code")
    fun findByCode(code: String): CountryDTO? {
        log.debug { "----> Loading country with code[$code] and caching in redis ..." }

        val row = CountryTable.selectAll().where { CountryTable.code eq code }.singleOrNull() ?: return null

        return CountryDTO(
            code = row[CountryTable.code],
            name = row[CountryTable.name],
            description = row[CountryTable.description]
        )
    }

    @Transactional
    @CacheEvict(key = "'country:' + #countryDTO.code")
    fun update(countryDTO: CountryDTO): Int {
        log.debug { "----> Updating country with code[${countryDTO.code}] ..." }
        return CountryTable.update({ CountryTable.code eq countryDTO.code }) {
            it[name] = countryDTO.name
            it[description] = countryDTO.description
        }
    }

    @CacheEvict(cacheNames = [COUNTRY_CACHE_NAME], allEntries = true)
    fun evictCacheAll() {
        log.debug { "----> Evicting all country cache ..." }
    }
}
