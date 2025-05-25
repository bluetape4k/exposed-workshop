package exposed.examples.suspendedcache.config

import exposed.examples.suspendedcache.AbstractSpringSuspendedCacheApplicationTest
import exposed.examples.suspendedcache.domain.CountryTable
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Test

class ExposedConfigTest: AbstractSpringSuspendedCacheApplicationTest() {

    companion object: KLoggingChannel()

    @Test
    fun `Schema 가 자동 생성되어야 한다`() = runSuspendIO {
        newSuspendedTransaction {
            CountryTable.exists().shouldBeTrue()
        }
    }

    @Test
    fun `CountryCodeTable 이 생성되고 데이터가 입력되어 있어야 한다`() = runSuspendIO {
        newSuspendedTransaction {
            CountryTable.selectAll().count() shouldBeGreaterThan 0
        }
    }
}
