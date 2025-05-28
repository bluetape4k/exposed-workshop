package exposed.examples.dml

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withDb
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.javatime.CurrentDate
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.jdbc.select
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 더미 테이블인 "DUAL" 테이블을 이용하는 예제입니다.
 */
class Ex99_Dual: JdbcExposedTestBase() {

    companion object: KLogging()

    /**
     * 실제 테이블들을 지정하지 않아도 간단한 쿼리 구문을 실행 할 수 있습니다.
     *
     * ```sql
     * SELECT 1
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testDualTable(testDB: TestDB) {
        withDb(testDB) {
            val resultColumn = intLiteral(1)
            // SELECT 1
            val result: Int = Table.Dual.select(resultColumn).single()[resultColumn]
            result shouldBeEqualTo 1
        }
    }

    /**
     * 현재 날짜를 조회하는 쿼리를 실행합니다.
     *
     * ```sql
     * SELECT CURRENT_DATE
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `dual table with CurrentDate`(testDB: TestDB) {
        withDb(testDB) {
            val today: LocalDate = Table.Dual.select(CurrentDate).single()[CurrentDate]
            today shouldBeEqualTo LocalDate.now()
        }
    }

    /**
     * 현재 날짜를 조회하는 쿼리를 실행합니다.
     *
     * ```sql
     * SELECT CURRENT_TIMESTAMP
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `dual table with CurrentDateTime`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_H2 }

        withDb(testDB) {
            val now: LocalDateTime = Table.Dual.select(CurrentDateTime).single()[CurrentDateTime]
            now shouldBeLessOrEqualTo LocalDateTime.now()
        }
    }
}
