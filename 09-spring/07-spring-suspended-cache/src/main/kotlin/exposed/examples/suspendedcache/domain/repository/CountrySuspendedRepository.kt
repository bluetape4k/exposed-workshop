package exposed.examples.suspendedcache.domain.repository

import exposed.examples.suspendedcache.domain.CountryRecord

/**
 * 코루틴 환경에서 국가 정보를 조회/수정하고 캐시를 제어하는 리포지토리 계약입니다.
 */
interface CountrySuspendedRepository {

    /**
     * 국가 코드를 기준으로 국가 정보를 조회합니다.
     */
    suspend fun findByCode(code: String): CountryRecord?

    /**
     * 국가 정보를 수정하고 반영된 행 수를 반환합니다.
     */
    suspend fun update(countryRecord: CountryRecord): Int

    /**
     * 관련 캐시를 전체 무효화합니다.
     */
    suspend fun evictCacheAll()
}
