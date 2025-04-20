package exposed.examples.springcache.config

import exposed.examples.springcache.AbstractSpringCacheApplicationTest
import exposed.examples.springcache.domain.CountryTable
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test

class ExposedConfigTest: AbstractSpringCacheApplicationTest() {

    companion object: KLogging()

    @Test
    fun `Schema 가 자동 생성되어야 한다`() {
        transaction {
            CountryTable.exists().shouldBeTrue()
        }
    }

    @Test
    fun `CountryCodeTable 이 생성되고 데이터가 입력되어 있어야 한다`() {
        transaction {
            CountryTable.selectAll().count() shouldBeGreaterThan 0
        }
    }
}
