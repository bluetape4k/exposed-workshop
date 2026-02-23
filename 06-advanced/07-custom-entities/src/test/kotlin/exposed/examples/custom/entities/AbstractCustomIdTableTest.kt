package exposed.examples.custom.entities

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.params.provider.Arguments

/**
 * 커스텀 ID 엔티티 테스트에서 공통으로 사용하는 테스트 DB/레코드 개수 조합을 제공한다.
 */
abstract class AbstractCustomIdTableTest: AbstractExposedTest() {

    companion object: KLogging() {
        const val GET_TESTDB_AND_ENTITY_COUNT = "getTestDBAndEntityCount"

        @JvmStatic
        fun getTestDBAndEntityCount(): List<Arguments> {
            val recordCounts = listOf(50, 500)

            return TestDB.enabledDialects().flatMap { testDB ->
                recordCounts.map { entityCount ->
                    Arguments.of(testDB, entityCount)
                }
            }
        }
    }
}
