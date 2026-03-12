package exposed.shared.tests

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [WithTables] 및 [WithTablesSuspending] 헬퍼의 동작을 검증하는 테스트 클래스.
 *
 * 테스트 전 지정한 테이블을 자동 생성하고, 테스트 완료 후 자동으로 삭제(drop)하는
 * 패턴이 JDBC 방식과 Coroutine(suspend) 방식 모두에서 올바르게 동작하는지 확인한다.
 */
class WithTablesTest {

    companion object: KLogging()

    private object TestTable: IntIdTable("test_with_tables") {
        val name = varchar("name", 255)
    }

    /** JDBC 방식의 [withTables] 헬퍼 동작을 검증하는 중첩 테스트. */
    @Nested
    inner class Jdbc: AbstractExposedTest() {

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `withTables should create and drop tables`(testDB: TestDB) {
            withTables(testDB, TestTable) {
                TestTable.insert {
                    it[name] = "test"
                }
                commit()

                val count = TestTable.selectAll().count()
                count shouldBeEqualTo 1L
            }
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `withTables should pass testDB to statement`(testDB: TestDB) {
            withTables(testDB, TestTable) { db ->
                db shouldBeEqualTo testDB
            }
        }
    }

    /** Coroutine(suspend) 방식의 [withTablesSuspending] 헬퍼 동작을 검증하는 중첩 테스트. */
    @Nested
    inner class Coroutines: AbstractExposedTest() {

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `withSuspendedTables should create and drop tables`(testDB: TestDB) = runSuspendIO {
            withTablesSuspending(testDB, TestTable) {
                TestTable.insert {
                    it[name] = "test"
                }
                commit()

                val count = TestTable.selectAll().count()
                count shouldBeEqualTo 1L
            }
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `withSuspendedTables should pass testDB to statement`(testDB: TestDB) = runSuspendIO {
            withTablesSuspending(testDB, TestTable) { db ->
                db shouldBeEqualTo testDB
            }
        }
    }
}
