package exposed.examples.suspendedcache.domain.repository

import exposed.examples.suspendedcache.domain.CountryRecord
import exposed.examples.suspendedcache.domain.CountryTable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

@Suppress("DEPRECATION")
/**
 * Exposed 코루틴 트랜잭션으로 국가 정보를 직접 조회/수정하는 기본 구현체입니다.
 */
class DefaultCountrySuspendedRepository: CountrySuspendedRepository {

    companion object: KLoggingChannel()

    /**
     * 국가 코드로 국가 정보를 조회합니다.
     */
    override suspend fun findByCode(code: String): CountryRecord? = newSuspendedTransaction {
        CountryTable.selectAll()
            .where { CountryTable.code eq code }
            .singleOrNull()
            ?.let {
                CountryRecord(
                    code = it[CountryTable.code],
                    name = it[CountryTable.name],
                    description = it[CountryTable.description]
                )
            }
    }

    /**
     * 국가 정보를 수정하고 갱신된 행 수를 반환합니다.
     */
    override suspend fun update(countryRecord: CountryRecord): Int = newSuspendedTransaction {
        CountryTable.update({ CountryTable.code eq countryRecord.code }) {
            it[name] = countryRecord.name
            it[description] = countryRecord.description
        }
    }

    /**
     * 기본 구현은 별도 캐시를 사용하지 않으므로 수행할 작업이 없습니다.
     */
    override suspend fun evictCacheAll() {
        // Nothing to do.
    }
}
