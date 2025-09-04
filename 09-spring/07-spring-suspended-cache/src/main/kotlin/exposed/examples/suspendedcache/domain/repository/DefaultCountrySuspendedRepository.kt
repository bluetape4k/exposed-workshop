package exposed.examples.suspendedcache.domain.repository

import exposed.examples.suspendedcache.domain.CountryDTO
import exposed.examples.suspendedcache.domain.CountryTable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

@Suppress("DEPRECATION")
class DefaultCountrySuspendedRepository: CountrySuspendedRepository {

    companion object: KLoggingChannel()

    override suspend fun findByCode(code: String): CountryDTO? = newSuspendedTransaction {
        CountryTable.selectAll()
            .where { CountryTable.code eq code }
            .singleOrNull()
            ?.let {
                CountryDTO(
                    code = it[CountryTable.code],
                    name = it[CountryTable.name],
                    description = it[CountryTable.description]
                )
            }
    }

    override suspend fun update(countryDTO: CountryDTO): Int = newSuspendedTransaction {
        CountryTable.update({ CountryTable.code eq countryDTO.code }) {
            it[name] = countryDTO.name
            it[description] = countryDTO.description
        }
    }

    override suspend fun evictCacheAll() {
        // Nothing to do.
    }
}
