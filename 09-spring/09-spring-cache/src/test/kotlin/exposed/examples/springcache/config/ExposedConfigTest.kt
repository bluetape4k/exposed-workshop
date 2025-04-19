package exposed.examples.springcache.config

import exposed.examples.springcache.AbstractSpringCacheApplicationTest
import exposed.examples.springcache.domain.CountryTable
import exposed.examples.springcache.domain.DataPopulator
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.uninitialized
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.spring.DatabaseInitializer
import org.jetbrains.exposed.spring.SpringTransactionManager
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ExposedConfigTest: AbstractSpringCacheApplicationTest() {

    companion object: KLogging()

    @Autowired
    private val springTransactionManager: SpringTransactionManager = uninitialized()

    @Autowired
    private val databaseInitializer: DatabaseInitializer = uninitialized()

    @Autowired
    private val dataPopulator: DataPopulator = uninitialized()

    @Autowired
    private val database: Database = uninitialized()

    @Test
    fun `Schema 가 자동 생성되어야 한다`() {
        transaction {
            CountryTable.exists().shouldBeTrue()
        }
    }

    @Test
    fun `DataPopulator 가 정상적으로 동작해야 한다`() {
        database.shouldNotBeNull()
        databaseInitializer.shouldNotBeNull()
        dataPopulator.shouldNotBeNull()
    }

    @Test
    fun `CountryCodeTable 이 생성되고 데이터가 입력되어 있어야 한다`() {
        transaction {
            CountryTable.exists().shouldBeTrue()
            CountryTable.selectAll().count() shouldBeGreaterThan 0
        }
    }
}
