package exposed.examples.custom.entities

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.params.provider.Arguments

abstract class AbstractCustomIdTableTest: JdbcExposedTestBase() {

    companion object: KLogging() {
        const val GET_TESTDB_AND_ENTITY_COUNT = "getTestDBAndEntityCount"

        @JvmStatic
        fun getTestDBAndEntityCount(): List<Arguments> {
            val recordCounts = listOf(100, 300)

            return TestDB.enabledDialects().map { testDB ->
                recordCounts.map { entityCount ->
                    Arguments.of(testDB, entityCount)
                }
            }.flatten()
        }
    }
}
