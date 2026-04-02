package exposed.examples.springcache.domain

import exposed.examples.springcache.domain.CountryRepository.Companion.COUNTRY_CACHE_NAME
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@CacheConfig(cacheNames = [COUNTRY_CACHE_NAME])
/**
 * 국가 코드 기준 조회/수정과 캐시 무효화를 제공하는 동기 리포지토리입니다.
 */
class CountryRepository(private val cacheManager: CacheManager) {

    companion object: KLogging() {
        const val COUNTRY_CACHE_NAME = "cache:code:country"
    }

    /**
     * 국가 코드로 국가 정보를 조회합니다.
     *
     * 캐시에 값이 없으면 DB에서 읽어 캐시에 저장하고, 있으면 캐시 값을 반환합니다.
     */
    @Cacheable(key = "'country:' + #code")
    fun findByCode(code: String): CountryRecord? {
        log.debug { "----> Loading country with code[$code] and caching in redis ..." }

        // @Transactional 을 사용하지 않고, transaction {} 블록을 사용하여 DB에 접근합니다.
        // 캐시에 이미 값이 있다면, Transaction을 사용하지 않고 캐시에서 값을 반환하도록 합니다.
        return transaction {
            val row = CountryTable.selectAll().where { CountryTable.code eq code }.singleOrNull()
                ?: return@transaction null

            CountryRecord(
                code = row[CountryTable.code],
                name = row[CountryTable.name],
                description = row[CountryTable.description]
            )
        }
    }

    /**
     * 국가 정보를 수정하고 해당 국가 캐시를 즉시 무효화합니다.
     */
    @Transactional
    @CacheEvict(key = "'country:' + #countryRecord.code")
    fun update(countryRecord: CountryRecord): Int {
        log.debug { "----> Updating country with code[${countryRecord.code}] ..." }

        return CountryTable.update({ CountryTable.code eq countryRecord.code }) {
            it[name] = countryRecord.name
            it[description] = countryRecord.description
        }
    }

    /**
     * 국가 캐시 엔트리를 모두 제거합니다.
     */
    @CacheEvict(cacheNames = [COUNTRY_CACHE_NAME], allEntries = true)
    fun evictCacheAll() {
        log.debug { "----> Evicting all country cache ..." }
    }
}
