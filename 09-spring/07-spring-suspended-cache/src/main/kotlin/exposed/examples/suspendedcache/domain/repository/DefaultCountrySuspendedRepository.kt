package exposed.examples.suspendedcache.domain.repository

import exposed.examples.suspendedcache.domain.CountryDTO
import exposed.examples.suspendedcache.domain.CountryTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class DefaultCountrySuspendedRepository: CountrySuspendedRepository {

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
