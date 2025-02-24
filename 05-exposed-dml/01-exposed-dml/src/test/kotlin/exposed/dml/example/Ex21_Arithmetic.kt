package exposed.dml.example

import exposed.shared.dml.DMLTestData
import exposed.shared.dml.DMLTestData.withCitiesAndUsers
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withDb
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.sql.DivideOp
import org.jetbrains.exposed.sql.DivideOp.Companion.withScale
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.SqlExpressionBuilder.div
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.times
import org.jetbrains.exposed.sql.Table.Dual
import org.jetbrains.exposed.sql.decimalLiteral
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex21_Arithmetic: AbstractExposedTest() {

    companion object: KLogging()


    /**
     * 컬럼 값을 산술 연산자를 사용하여 계산합니다.
     *
     * ```sql
     * -- Postgres
     * SELECT userdata."value",
     *        (((userdata."value" - 5) * 2) / 2)
     *   FROM userdata
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `operator precedence of minus, plus, div times`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, _, userData ->

            val calculatedColumn: DivideOp<Int, Int> = ((DMLTestData.UserData.value - 5) * 2) / 2

            userData
                .select(DMLTestData.UserData.value, calculatedColumn)
                .forEach {
                    val value = it[DMLTestData.UserData.value]
                    val actualResult = it[calculatedColumn]
                    val expectedResult = ((value - 5) * 2) / 2
                    actualResult shouldBeEqualTo expectedResult
                }
        }
    }

    /**
     * `Expression.build { ten / three }`
     *
     * ```sql
     * SELECT (10 / 3)
     *
     * ```
     *
     * `withScale(2)`
     *
     * ```sql
     * SELECT (10.0 / 3)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `big decimal division with scale and without`(testDB: TestDB) {
        withDb(testDB) {
            val ten = decimalLiteral(10.toBigDecimal())
            val three = decimalLiteral(3.toBigDecimal())

            // SELECT (10 / 3)
            val divTenToThreeWithoutScale = Expression.build { ten / three }
            val resultWithoutScale = Dual
                .select(divTenToThreeWithoutScale)
                .single()[divTenToThreeWithoutScale]

            resultWithoutScale shouldBeEqualTo 3.toBigDecimal()

            // SELECT (10.0 / 3)
            val divTenToThreeWithScale = divTenToThreeWithoutScale.withScale(2)
            val resultWithScale = Dual
                .select(divTenToThreeWithScale)
                .single()[divTenToThreeWithScale]

            resultWithScale shouldBeEqualTo 3.33.toBigDecimal()
        }
    }
}
