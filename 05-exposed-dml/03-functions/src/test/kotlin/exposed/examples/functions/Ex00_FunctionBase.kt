package exposed.examples.functions

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withDb
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table.Dual
import org.jetbrains.exposed.sql.Transaction
import java.math.BigDecimal
import java.math.RoundingMode

typealias SqlFunction<T> = org.jetbrains.exposed.sql.Function<T>

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

    protected fun withTable(testDB: TestDB, body: Transaction.(TestDB) -> Unit) {
        withDb(testDB) {
            body(it)
        }
    }

    protected infix fun <T> SqlFunction<T>.shouldExpressionEqualTo(expected: T) {
        val result = Dual.select(this).first()[this]

        if (expected is BigDecimal && result is BigDecimal) {
            result.setScale(expected.scale(), RoundingMode.HALF_UP) shouldBeEqualTo expected
        } else {
            result shouldBeEqualTo expected
        }
    }
}
