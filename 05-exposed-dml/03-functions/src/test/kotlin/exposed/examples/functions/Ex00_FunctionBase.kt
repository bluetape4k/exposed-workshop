package exposed.examples.functions

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withDb
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import java.math.BigDecimal
import java.math.RoundingMode

typealias SqlFunction<T> = org.jetbrains.exposed.v1.core.Function<T>

/**
 * SQL 함수 예제의 공통 베이스 클래스.
 *
 * 함수 테스트에서 공통으로 사용하는 테이블 정의, 헬퍼 함수, 검증 유틸리티를 제공합니다.
 */
abstract class Ex00_FunctionBase: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS faketable (
     *      id SERIAL PRIMARY KEY
     * )
     * ```
     */
    private object FakeTestTable: IntIdTable("fakeTable")

    protected fun withTable(testDB: TestDB, body: JdbcTransaction.(TestDB) -> Unit) {
        withDb(testDB) {
            body(it)
        }
    }

    protected infix fun <T> SqlFunction<T>.shouldExpressionEqualTo(expected: T) {
        val result = Table.Dual.select(this).first()[this]

        if (expected is BigDecimal && result is BigDecimal) {
            result.setScale(expected.scale(), RoundingMode.HALF_UP) shouldBeEqualTo expected
        } else {
            result shouldBeEqualTo expected
        }
    }
}
