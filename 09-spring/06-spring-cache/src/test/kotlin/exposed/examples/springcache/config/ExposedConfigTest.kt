package exposed.examples.springcache.config

import exposed.examples.springcache.AbstractSpringCacheApplicationTest
import exposed.examples.springcache.domain.CountryTable
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/** Spring Cache 모듈의 Exposed 스키마 자동 생성 및 초기 데이터 적재를 검증합니다. */
class ExposedConfigTest(
    @param:Autowired private val database: Database,
): AbstractSpringCacheApplicationTest() {

    companion object: KLogging()

    @Test
    fun `Schema 가 자동 생성되어야 한다`() {
        transaction(database) {
            CountryTable.exists().shouldBeTrue()
        }
    }

    @Test
    fun `CountryCodeTable 이 생성되고 데이터가 입력되어 있어야 한다`() {
        transaction(database) {
            CountryTable.selectAll().count() shouldBeGreaterThan 0
        }
    }
}
