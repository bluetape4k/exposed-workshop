package exposed.examples.suspendedcache.domain.repository

import exposed.examples.suspendedcache.domain.CountryRecord
import exposed.examples.suspendedcache.domain.CountryTable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

@Suppress("DEPRECATION")
class DefaultCountrySuspendedRepository: CountrySuspendedRepository {

    companion object: KLoggingChannel()

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

    override suspend fun update(countryRecord: CountryRecord): Int = newSuspendedTransaction {
        CountryTable.update({ CountryTable.code eq countryRecord.code }) {
            it[name] = countryRecord.name
            it[description] = countryRecord.description
        }
    }

    override suspend fun evictCacheAll() {
        // Nothing to do.
    }
}
